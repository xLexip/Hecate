/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.Application
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferences
import dev.lexip.hecate.data.UserPreferencesDataSource
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.services.AdaptiveThemeServiceController
import dev.lexip.hecate.services.AndroidAdaptiveThemeServiceController
import dev.lexip.hecate.util.AndroidInstallMetadataProvider
import dev.lexip.hecate.util.InstallMetadataProvider
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.ProximitySensorManager
import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.WallpaperHandler
import dev.lexip.hecate.util.WallpaperImagePreparer
import dev.lexip.hecate.util.WallpaperImagePreprocessor
import dev.lexip.hecate.util.WallpaperPlatform
import dev.lexip.hecate.util.WallpaperSlot
import dev.lexip.hecate.util.WallpaperStorageMigrator
import dev.lexip.hecate.util.CURRENT_WALLPAPER_STORAGE_VERSION
import dev.lexip.hecate.util.isNightConfigurationEnabled
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.time.LocalDate

private const val TAG = "MainViewModel"

sealed interface UiEvent

data class CopyToClipboard(val text: String) : UiEvent
data object NavigateToSetup : UiEvent
data object RequestInAppReview : UiEvent

data class MainUiState(
	val adaptiveThemeEnabled: Boolean = false,
	val adaptiveThemeThresholdLux: Float = 1000f,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val hasSetupCompleted: Boolean = false,
	val isDeviceCovered: Boolean = false,
	val isBatterySaverActive: Boolean = false,
	val isShizukuInstalled: Boolean = false,
	val isInstalledFromPlayStore: Boolean = false,
	val stayDarkAtNightEnabled: Boolean = false,
	val nightStartMinutes: Int = 21 * 60,
	val nightEndMinutes: Int = 6 * 60,
	val wallpaperSyncEnabled: Boolean = false,
	val lockScreenWallpaperBlurEnabled: Boolean = false,
	val dayWallpaperUri: String? = null,
	val nightWallpaperUri: String? = null,
	val showLiveWallpaperWarningDialog: Boolean = false,
	val showGitHubStarPrompt: Boolean = false
)

class MainViewModel internal constructor(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesDataSource,
	private val lightSensorManager: SensorReader =
		LightSensorManager(application.applicationContext),
	private val proximitySensorManager: ProximitySensorReader =
		ProximitySensorManager(application.applicationContext),
	private val serviceController: AdaptiveThemeServiceController =
		AndroidAdaptiveThemeServiceController(application.applicationContext),
	private val installMetadataProvider: InstallMetadataProvider =
		AndroidInstallMetadataProvider(application.applicationContext),
	private val wallpaperPlatform: WallpaperPlatform =
		WallpaperHandler(application.applicationContext),
	private val wallpaperImagePreparer: WallpaperImagePreparer =
		WallpaperImagePreprocessor(application.applicationContext),
	private val wallpaperStorageMigrator: WallpaperStorageMigrator =
		WallpaperImagePreprocessor(application.applicationContext),
	private val openWallpaperInputStream: (Uri) -> InputStream? =
		application.applicationContext.contentResolver::openInputStream,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
	private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
	private val todayEpochDay: () -> Long = { LocalDate.now().toEpochDay() }
) : ViewModel() {

	private val _uiState = MutableStateFlow(MainUiState())
	val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

	fun setShizukuInstalled(installed: Boolean) {
		if (_uiState.value.isShizukuInstalled == installed) return
		_uiState.value = _uiState.value.copy(isShizukuInstalled = installed)
	}

	fun isAdaptiveThemeEnabled(): Boolean = _uiState.value.adaptiveThemeEnabled

	// One-shot UI events
	private val _uiEvents = MutableSharedFlow<UiEvent>(
		replay = 0,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val uiEvents = _uiEvents.asSharedFlow()

	// Light Sensor
	private var isListeningToSensor = false
	private var isUiActive = false

	private val _currentSensorLux = MutableStateFlow(0f)
	val currentSensorLuxFlow: StateFlow<Float> = _currentSensorLux.asStateFlow()
	val currentSensorLux: Float get() = _currentSensorLux.value

	fun updateCurrentSensorLux(lux: Float) {
		_currentSensorLux.value = lux
	}

	// Proximity Sensor
	private var isListeningToProximity = false
	private var coveredJob: Job? = null
	private var batterySaverReceiver: BroadcastReceiver? = null
	private var isMonitoringBatterySaver = false

	private fun startProximityListening() {
		if (!proximitySensorManager.hasProximitySensor) {
			Log.w(
				TAG,
				"Proximity sensor not available; skipping proximity listening in MainViewModel."
			)
			if (_uiState.value.isDeviceCovered) {
				_uiState.value = _uiState.value.copy(isDeviceCovered = false)
			}
			return
		}

		if (isListeningToProximity) return
		isListeningToProximity = true
		val registered = proximitySensorManager.startListening({ distance: Float ->
			val maximumRange = proximitySensorManager.maximumRange
			val covered = distance.isFinite() &&
				maximumRange.isFinite() &&
				maximumRange > 0f &&
				distance < maximumRange
			if (covered) {
				if (_uiState.value.isDeviceCovered || coveredJob?.isActive == true) return@startListening
				coveredJob = viewModelScope.launch {
					Log.d(TAG, "Proximity covered timer started")
					try {
						delay(1000)
						if (!_uiState.value.isDeviceCovered) {
							_uiState.value = _uiState.value.copy(isDeviceCovered = true)
							Log.d(TAG, "Proximity covered timer fired")
						}
					} finally {
						coveredJob = null
					}
				}
			} else {
				if (coveredJob?.isActive == true) {
					coveredJob?.cancel()
					coveredJob = null
					Log.d(TAG, "Proximity covered timer cancelled")
				}
				if (_uiState.value.isDeviceCovered) {
					_uiState.value = _uiState.value.copy(isDeviceCovered = false)
				}
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_UI)
		if (!registered) {
			isListeningToProximity = false
			Log.w(TAG, "Failed to register proximity sensor listener in MainViewModel.")
		}
	}

	private fun stopProximityListening() {
		if (!isListeningToProximity) return
		isListeningToProximity = false
		proximitySensorManager.stopListening()
		if (coveredJob?.isActive == true) {
			coveredJob?.cancel()
			coveredJob = null
			Log.d(TAG, "Proximity covered timer cancelled")
		}
		if (_uiState.value.isDeviceCovered) {
			_uiState.value = _uiState.value.copy(isDeviceCovered = false)
		}
	}

	private fun updateBatterySaverState(context: Context) {
		val powerManager = context.getSystemService(PowerManager::class.java)
		val isActive = powerManager?.isPowerSaveMode == true
		if (_uiState.value.isBatterySaverActive != isActive) {
			_uiState.value = _uiState.value.copy(isBatterySaverActive = isActive)
		}
	}

	private fun startBatterySaverMonitoring() {
		if (isMonitoringBatterySaver) return
		val context = application.applicationContext
		isMonitoringBatterySaver = true
		updateBatterySaverState(context)

		batterySaverReceiver = object : BroadcastReceiver() {
			override fun onReceive(ctx: Context?, intent: Intent?) {
				if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
					updateBatterySaverState(context)
				}
			}
		}

		ContextCompat.registerReceiver(
			context,
			batterySaverReceiver,
			IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
	}

	private fun stopBatterySaverMonitoring() {
		if (!isMonitoringBatterySaver) return
		isMonitoringBatterySaver = false
		batterySaverReceiver?.let {
			try {
				application.applicationContext.unregisterReceiver(it)
			} catch (_: IllegalArgumentException) {
				// Receiver may already be unregistered.
			}
		}
		batterySaverReceiver = null
		if (_uiState.value.isBatterySaverActive) {
			_uiState.value = _uiState.value.copy(isBatterySaverActive = false)
		}
	}

	fun onUiResumed() {
		isUiActive = true
		updateUiSensorMonitoring()
	}

	fun onUiPaused() {
		isUiActive = false
		stopUiSensorMonitoring()
	}

	private fun updateUiSensorMonitoring() {
		if (isUiActive && _uiState.value.adaptiveThemeEnabled) {
			startLightSensorListening()
			startProximityListening()
			startBatterySaverMonitoring()
		} else {
			stopUiSensorMonitoring()
		}
	}

	private fun stopUiSensorMonitoring() {
		stopLightSensorListening()
		stopProximityListening()
		stopBatterySaverMonitoring()
	}

	private var customThresholdTemp: Float? = null

	// In-app reviews
	private var serviceEnabledAtStart: Boolean? = null
	private var reviewRequestedInSession: Boolean = false
	private var hasChangedBrightnessThresholdInSession: Boolean = false
	private var githubStarPromptImpressionRecordedInSession: Boolean = false
	private var latestUserPreferences: UserPreferences? = null
	private val wallpaperSelectionMutex = Mutex()
	private val wallpaperMigrationCompleted = CompletableDeferred<Unit>()

	init {
		viewModelScope.launch(ioDispatcher) {
			try {
				wallpaperSelectionMutex.withLock {
					migrateWallpaperStorage()
				}
			} finally {
				wallpaperMigrationCompleted.complete(Unit)
			}
		}
		viewModelScope.launch(ioDispatcher) {
			val fromPlayStore = installMetadataProvider.isInstalledFromPlayStore()
			_uiState.value = _uiState.value.copy(isInstalledFromPlayStore = fromPlayStore)
		}

		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				latestUserPreferences = userPreferences
				if (serviceEnabledAtStart == null) {
					serviceEnabledAtStart = userPreferences.adaptiveThemeEnabled
				}
				_uiState.value = _uiState.value.copy(
					adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled,
					adaptiveThemeThresholdLux = userPreferences.adaptiveThemeThresholdLux,
					customAdaptiveThemeThresholdLux = userPreferences.customAdaptiveThemeThresholdLux,
					hasSetupCompleted = userPreferences.hasSetupCompleted,
					stayDarkAtNightEnabled = userPreferences.stayDarkAtNightEnabled,
					nightStartMinutes = userPreferences.nightStartMinutes,
					nightEndMinutes = userPreferences.nightEndMinutes,
					wallpaperSyncEnabled = userPreferences.wallpaperSyncEnabled,
					lockScreenWallpaperBlurEnabled = userPreferences.lockScreenWallpaperBlurEnabled,
					dayWallpaperUri = userPreferences.dayWallpaperUri,
					nightWallpaperUri = userPreferences.nightWallpaperUri,
					showGitHubStarPrompt = shouldShowGitHubStarPrompt(userPreferences)
				)

				updateUiSensorMonitoring()
			}
		}
	}

	private fun shouldShowGitHubStarPrompt(
		userPreferences: UserPreferences
	): Boolean {
		if (githubStarPromptImpressionRecordedInSession) {
			return userPreferences.hasSetupCompleted &&
				userPreferences.adaptiveThemeEnabled &&
				!userPreferences.githubStarPromptDismissed
		}
		return shouldShowGitHubStarPrompt(
			GitHubStarPromptEligibility(
				hasSetupCompleted = userPreferences.hasSetupCompleted,
				adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled,
				daysSinceFirstInstall = installMetadataProvider.daysSinceFirstInstall(),
				dismissed = userPreferences.githubStarPromptDismissed,
				lastReviewRequestEpochDay = userPreferences.reviewPromptLastRequestEpochDay,
				todayEpochDay = todayEpochDay()
			)
		)
	}

	private suspend fun migrateWallpaperStorage() {
		val preferences = userPreferencesRepository.fetchInitialPreferences()
		val migration = wallpaperStorageMigrator.migrate(
			dayWallpaperUri = preferences.dayWallpaperUri,
			nightWallpaperUri = preferences.nightWallpaperUri
		)
		if (migration.failed) {
			userPreferencesRepository.updateWallpaperSyncEnabled(false)
			userPreferencesRepository.updateLockScreenWallpaperBlurEnabled(false)
			userPreferencesRepository.updateDayWallpaperUri(null)
			userPreferencesRepository.updateNightWallpaperUri(null)
			userPreferencesRepository.updateWallpaperStorageVersion(CURRENT_WALLPAPER_STORAGE_VERSION)
			Log.i(TAG, "Cleared wallpaper selections that could not be migrated safely")
			return
		}

		if (migration.dayWallpaperUri != preferences.dayWallpaperUri) {
			userPreferencesRepository.updateDayWallpaperUri(migration.dayWallpaperUri)
		}
		if (migration.nightWallpaperUri != preferences.nightWallpaperUri) {
			userPreferencesRepository.updateNightWallpaperUri(migration.nightWallpaperUri)
		}
		if (preferences.wallpaperStorageVersion < CURRENT_WALLPAPER_STORAGE_VERSION) {
			userPreferencesRepository.updateWallpaperStorageVersion(CURRENT_WALLPAPER_STORAGE_VERSION)
		}
	}

	private fun startLightSensorListening() {
		if (isListeningToSensor) return
		isListeningToSensor = true
		val registered = lightSensorManager.startListening({ lux: Float ->
			viewModelScope.launch {
				updateCurrentSensorLux(lux)
			}
		}, sensorDelay = SensorManager.SENSOR_DELAY_NORMAL)
		if (!registered) {
			isListeningToSensor = false
			Log.w(TAG, "Failed to register light sensor listener in MainViewModel.")
		}
	}

	private fun stopLightSensorListening() {
		if (!isListeningToSensor) return
		isListeningToSensor = false
		lightSensorManager.stopListening()
	}

	override fun onCleared() {
		stopUiSensorMonitoring()
	}

	/**
	 * Toggle adaptive theme service or show setup.
	 * @return true if service was toggled, false if setup is shown.
	 */
	fun onServiceToggleRequested(
		checked: Boolean,
		hasPermission: Boolean
	): Boolean {
		if (checked && !hasPermission) {
			viewModelScope.launch {
				_uiEvents.emit(NavigateToSetup)
			}
			return false
		}
		updateAdaptiveThemeEnabled(checked)
		return true
	}

	private fun updateAdaptiveThemeEnabled(enable: Boolean) {
		val wasEnabled = _uiState.value.adaptiveThemeEnabled
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeEnabled(enable)
			if (enable) {
				startBroadcastReceiverService()
				userPreferencesRepository.ensureAdaptiveThemeThresholdDefault()
				Logger.logServiceEnabled(
					application.applicationContext,
					source = if (wasEnabled) "state_restore" else "ui_toggle"
				)
			} else {
				stopBroadcastReceiverService()
				Logger.logServiceDisabled(
					application.applicationContext,
					source = if (wasEnabled) "ui_toggle" else "state_restore"
				)
			}
		}
	}

	private fun shouldPromptForReview(): Boolean {
		val preferences = latestUserPreferences ?: return false
		return shouldRequestReview(
			ReviewPromptEligibility(
				isPlayStoreInstall = installMetadataProvider.isInstalledFromPlayStore(),
				serviceEnabledAtStart = serviceEnabledAtStart == true,
				daysSinceFirstInstall = installMetadataProvider.daysSinceFirstInstall(),
				requestedInSession = reviewRequestedInSession,
				githubPromptRenderedInSession =
					githubStarPromptImpressionRecordedInSession,
				lastReviewRequestEpochDay = preferences.reviewPromptLastRequestEpochDay,
				lastGitHubImpressionEpochDay =
					preferences.githubStarPromptLastImpressionEpochDay,
				todayEpochDay = todayEpochDay()
			)
		)
	}

	fun checkReviewPrompt() {
		if (shouldPromptForReview()) {
			reviewRequestedInSession = true
			viewModelScope.launch {
				_uiEvents.emit(RequestInAppReview)
			}
		}
	}

	fun recordReviewPromptLaunch() {
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.updateReviewPromptLastRequestEpochDay(todayEpochDay())
		}
	}

	fun recordGitHubStarPromptImpression() {
		if (!_uiState.value.showGitHubStarPrompt ||
			githubStarPromptImpressionRecordedInSession
		) return

		githubStarPromptImpressionRecordedInSession = true
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.recordGitHubStarPromptImpression(todayEpochDay())
			Logger.logGitHubStarPromptAction(
				application.applicationContext,
				action = "impression"
			)
		}
	}

	fun dismissGitHubStarPrompt() {
		_uiState.value = _uiState.value.copy(showGitHubStarPrompt = false)
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.updateGitHubStarPromptDismissed(true)
			Logger.logGitHubStarPromptAction(
				application.applicationContext,
				action = "dismiss"
			)
		}
	}

	fun undoGitHubStarPromptDismissal() {
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.updateGitHubStarPromptDismissed(false)
			Logger.logGitHubStarPromptAction(
				application.applicationContext,
				action = "undo_dismiss"
			)
		}
	}

	fun onGitHubStarPromptOpened() {
		_uiState.value = _uiState.value.copy(showGitHubStarPrompt = false)
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.updateGitHubStarPromptDismissed(true)
			Logger.logGitHubStarPromptAction(
				application.applicationContext,
				action = "open_github"
			)
		}
	}

	fun updateAdaptiveThemeThresholdByIndex(index: Int) {
		val threshold = AdaptiveThreshold.fromIndex(index)
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		val shouldCheckReviewPrompt = hasChangedBrightnessThresholdInSession
		hasChangedBrightnessThresholdInSession = true
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeThresholdLux(threshold.lux)

			Logger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = threshold.lux
			)

			if (shouldCheckReviewPrompt) {
				checkReviewPrompt()
			}
		}
	}

	fun setCustomAdaptiveThemeThreshold(lux: Float) {
		val oldLux = _uiState.value.adaptiveThemeThresholdLux
		viewModelScope.launch {
			userPreferencesRepository.updateCustomAdaptiveThemeThresholdLux(lux)

			Logger.logBrightnessThresholdChanged(
				application.applicationContext,
				oldLux = oldLux,
				newLux = lux
			)
		}
	}

	val isUsingCustomThreshold: Boolean
		get() = _uiState.value.customAdaptiveThemeThresholdLux != null

	fun getDisplayLuxSteps(baseLux: List<Float>): List<Float> {
		val customLux = _uiState.value.customAdaptiveThemeThresholdLux ?: return baseLux
		val index = AdaptiveThreshold.fromLux(customLux).ordinal
		return baseLux.mapIndexed { i, value -> if (i == index) customLux else value }
	}

	fun getDisplayLabels(labels: List<String>, customLabel: String): List<String> {
		return if (isUsingCustomThreshold) {
			labels.mapIndexed { index, label ->
				if (index == getIndexForCurrentLux()) customLabel else label
			}
		} else labels
	}

	fun onSliderValueCommitted(index: Int) {
		if (isUsingCustomThreshold) {
			customThresholdTemp = null
		}
		updateAdaptiveThemeThresholdByIndex(index)
	}

	fun getIndexForCurrentLux(): Int {
		val lux = customThresholdTemp ?: _uiState.value.adaptiveThemeThresholdLux
		return AdaptiveThreshold.fromLux(lux).ordinal
	}

	fun setPendingCustomSliderLux(lux: Float) {
		customThresholdTemp = lux
	}

	fun updateStayDarkAtNightEnabled(enabled: Boolean, source: String = "main_screen_toggle") {
		val wasEnabled = _uiState.value.stayDarkAtNightEnabled
		if (wasEnabled == enabled) return

		viewModelScope.launch {
			if (enabled) {
				// Keep defaults silently on first enable.
				userPreferencesRepository.ensureNightDefaults()
			}
			userPreferencesRepository.updateStayDarkAtNightEnabled(enabled)
			Logger.logStayDarkAtNightToggled(
				application.applicationContext,
				enabled = enabled,
				source = source
			)
		}
	}

	fun updateNightWindow(startMinutes: Int, endMinutes: Int, onRejected: (() -> Unit)? = null) {
		if (startMinutes == endMinutes) {
			onRejected?.invoke()
			return
		}

		viewModelScope.launch {
			if (!userPreferencesRepository.updateNightWindow(startMinutes, endMinutes)) {
				onRejected?.invoke()
			}
		}
	}

	fun onDayWallpaperPicked(uri: Uri) {
		openAndStoreWallpaperSelection(uri, isDayWallpaper = true)
	}

	fun onNightWallpaperPicked(uri: Uri) {
		openAndStoreWallpaperSelection(uri, isDayWallpaper = false)
	}

	private fun openAndStoreWallpaperSelection(uri: Uri, isDayWallpaper: Boolean) {
		val sourceStream = try {
			openWallpaperInputStream(uri)
		} catch (e: Exception) {
			Log.w(TAG, "Failed to open selected wallpaper", e)
			null
		}
		if (sourceStream == null) {
			Log.w(TAG, "Selected wallpaper returned no readable stream: $uri")
			return
		}
		val job = viewModelScope.launch(ioDispatcher) {
			sourceStream.use {
				storeWallpaperSelection(uri, it, isDayWallpaper)
			}
		}
		job.invokeOnCompletion { runCatching(sourceStream::close) }
	}

	private suspend fun storeWallpaperSelection(
		uri: Uri,
		sourceStream: InputStream,
		isDayWallpaper: Boolean
	) {
		wallpaperSelectionMutex.withLock {
			val preferencesBefore = userPreferencesRepository.fetchInitialPreferences()
			try {
				wallpaperPlatform.takePersistableReadPermission(uri)
			} catch (e: Exception) {
				val wallpaperType = if (isDayWallpaper) "day" else "night"
				Log.w(TAG, "Failed to take persistable URI permission for $wallpaperType wallpaper", e)
			}
			val storedUri = try {
				wallpaperImagePreparer.prepare(
					sourceUri = uri,
					sourceStream = sourceStream,
					slot = if (isDayWallpaper) WallpaperSlot.DAY else WallpaperSlot.NIGHT
				)
			} catch (e: Exception) {
				Log.w(TAG, "Failed to prepare selected wallpaper; using the original URI", e)
				uri
			}

			if (isDayWallpaper) {
				userPreferencesRepository.updateDayWallpaperUri(storedUri.toString())
			} else {
				userPreferencesRepository.updateNightWallpaperUri(storedUri.toString())
			}
			Logger.logWallpaperPicked(
				application.applicationContext,
				if (isDayWallpaper) "light" else "dark"
			)

			val preferencesAfter = userPreferencesRepository.fetchInitialPreferences()
			val bothWereSet = !preferencesBefore.dayWallpaperUri.isNullOrEmpty() &&
				!preferencesBefore.nightWallpaperUri.isNullOrEmpty()
			val bothAreSet = !preferencesAfter.dayWallpaperUri.isNullOrEmpty() &&
				!preferencesAfter.nightWallpaperUri.isNullOrEmpty()
			if (!bothWereSet && bothAreSet && !preferencesAfter.wallpaperSyncEnabled) {
				onWallpaperSyncToggleRequested(true)
			}
		}
	}

	fun onWallpaperSyncToggleRequested(enabled: Boolean) {
		if (enabled) {
			if (wallpaperPlatform.isLiveWallpaperActive()) {
				_uiState.value = _uiState.value.copy(showLiveWallpaperWarningDialog = true)
			} else {
				enableWallpaperSync()
			}
		} else {
			disableWallpaperSync()
		}
	}

	fun updateLockScreenWallpaperBlurEnabled(enabled: Boolean) {
		viewModelScope.launch(ioDispatcher) {
			wallpaperMigrationCompleted.await()
			wallpaperSelectionMutex.withLock {
				val preferences = userPreferencesRepository.fetchInitialPreferences()
				if (enabled && (!preferences.wallpaperSyncEnabled ||
					preferences.dayWallpaperUri.isNullOrEmpty() ||
					preferences.nightWallpaperUri.isNullOrEmpty())
				) return@withLock
				if (preferences.lockScreenWallpaperBlurEnabled == enabled) return@withLock

				userPreferencesRepository.updateLockScreenWallpaperBlurEnabled(enabled)
				Logger.logLockScreenWallpaperBlurToggled(
					application.applicationContext,
					enabled = enabled
				)
				applyCurrentWallpaper(
					preferences.copy(
						lockScreenWallpaperBlurEnabled = enabled
					)
				)
			}
		}
	}

	private fun applyCurrentWallpaper(preferences: UserPreferences) {
		if (!preferences.wallpaperSyncEnabled) return
		wallpaperPlatform.applyWallpaperForTheme(
			isDark = isNightConfigurationEnabled(
				application.applicationContext.resources.configuration.uiMode
			),
			dayUriStr = preferences.dayWallpaperUri,
			nightUriStr = preferences.nightWallpaperUri,
			lockScreenWallpaperBlurEnabled = preferences.lockScreenWallpaperBlurEnabled
		)
	}

	fun confirmEnableWithLiveWallpaper() {
		_uiState.value = _uiState.value.copy(showLiveWallpaperWarningDialog = false)
		enableWallpaperSync()
	}

	fun dismissLiveWallpaperWarningDialog() {
		_uiState.value = _uiState.value.copy(showLiveWallpaperWarningDialog = false)
	}

	private fun enableWallpaperSync() {
		viewModelScope.launch(ioDispatcher) {
			wallpaperMigrationCompleted.await()
			wallpaperSelectionMutex.withLock {
				val preferences = userPreferencesRepository.fetchInitialPreferences()
				userPreferencesRepository.updateWallpaperSyncEnabled(true)
				Logger.logWallpaperSyncToggled(application.applicationContext, enabled = true)
				if (!preferences.dayWallpaperUri.isNullOrEmpty() &&
					!preferences.nightWallpaperUri.isNullOrEmpty()
				) {
					applyCurrentWallpaper(preferences.copy(wallpaperSyncEnabled = true))
				}
			}
		}
	}

	private fun disableWallpaperSync() {
		viewModelScope.launch(ioDispatcher) {
			userPreferencesRepository.updateWallpaperSyncEnabled(false)
			Logger.logWallpaperSyncToggled(application.applicationContext, enabled = false)
		}
	}

	private fun startBroadcastReceiverService() {
		serviceController.start()
	}

	private fun stopBroadcastReceiverService() {
		serviceController.stop()
	}
}

class MainViewModelFactory(
	private val application: Application,
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {


	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return MainViewModel(
				application,
				userPreferencesRepository
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}
