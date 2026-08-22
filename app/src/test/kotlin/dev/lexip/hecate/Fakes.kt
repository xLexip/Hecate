/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate

import android.net.Uri
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.data.UserPreferences
import dev.lexip.hecate.data.UserPreferencesDataSource
import dev.lexip.hecate.services.AdaptiveThemeServiceController
import dev.lexip.hecate.ui.setup.SetupEnvironmentProvider
import dev.lexip.hecate.ui.setup.SetupEnvironmentSnapshot
import dev.lexip.hecate.ui.setup.SetupGrantResult
import dev.lexip.hecate.ui.setup.SetupPermissionGrantController
import dev.lexip.hecate.ui.setup.SetupPermissionListenerRegistration
import dev.lexip.hecate.util.InstallMetadataProvider
import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.WallpaperPlatform
import dev.lexip.hecate.util.WallpaperImagePreparer
import dev.lexip.hecate.util.WallpaperSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesDataSource(
	initial: UserPreferences = UserPreferences(
		adaptiveThemeEnabled = false,
		adaptiveThemeThresholdLux = AdaptiveThreshold.DAYLIGHT.lux
	)
) : UserPreferencesDataSource {
	private val state = MutableStateFlow(initial)
	override val userPreferencesFlow = state.asStateFlow()

	var thresholdDefaultCalls = 0
	var nightDefaultCalls = 0
	var setupCompletedWrites = 0
	var rejectNightWindow = false

	val current: UserPreferences
		get() = state.value

	fun emit(value: UserPreferences) {
		state.value = value
	}

	override suspend fun fetchInitialPreferences(): UserPreferences = current

	override suspend fun ensureAdaptiveThemeThresholdDefault(default: Float) {
		thresholdDefaultCalls++
	}

	override suspend fun ensureNightDefaults(defaultStartMinutes: Int, defaultEndMinutes: Int) {
		nightDefaultCalls++
	}

	override suspend fun updateAdaptiveThemeEnabled(enabled: Boolean) {
		state.value = current.copy(adaptiveThemeEnabled = enabled)
	}

	override suspend fun updateAdaptiveThemeThresholdLux(lux: Float) {
		state.value = current.copy(
			adaptiveThemeThresholdLux = lux,
			customAdaptiveThemeThresholdLux = null
		)
	}

	override suspend fun updateCustomAdaptiveThemeThresholdLux(lux: Float) {
		state.value = current.copy(
			adaptiveThemeThresholdLux = lux,
			customAdaptiveThemeThresholdLux = lux
		)
	}

	override suspend fun updateSetupCompleted(completed: Boolean) {
		setupCompletedWrites++
		state.value = current.copy(hasSetupCompleted = completed)
	}

	override suspend fun updateStayDarkAtNightEnabled(enabled: Boolean) {
		state.value = current.copy(stayDarkAtNightEnabled = enabled)
	}

	override suspend fun updateNightWindow(startMinutes: Int, endMinutes: Int): Boolean {
		if (rejectNightWindow) return false
		state.value = current.copy(
			nightStartMinutes = startMinutes,
			nightEndMinutes = endMinutes
		)
		return true
	}

	override suspend fun updateWallpaperSyncEnabled(enabled: Boolean) {
		state.value = current.copy(wallpaperSyncEnabled = enabled)
	}

	override suspend fun updateDayWallpaperUri(uri: String?) {
		state.value = current.copy(dayWallpaperUri = uri)
	}

	override suspend fun updateNightWallpaperUri(uri: String?) {
		state.value = current.copy(nightWallpaperUri = uri)
	}

	override suspend fun updateWallpaperStorageVersion(version: Int) {
		state.value = current.copy(wallpaperStorageVersion = version)
	}

	override suspend fun recordGitHubStarPromptImpression(epochDay: Long) {
		val isNewDay = current.githubStarPromptLastImpressionEpochDay != epochDay
		state.value = current.copy(
			githubStarPromptImpressionCount = current.githubStarPromptImpressionCount +
					if (isNewDay) 1 else 0,
			githubStarPromptLastImpressionEpochDay = epochDay
		)
	}

	override suspend fun updateGitHubStarPromptDismissed(dismissed: Boolean) {
		state.value = current.copy(githubStarPromptDismissed = dismissed)
	}

	override suspend fun updateReviewPromptLastRequestEpochDay(epochDay: Long) {
		state.value = current.copy(reviewPromptLastRequestEpochDay = epochDay)
	}
}

open class FakeSensorReader(
	var registrationSucceeds: Boolean = true
) : SensorReader {
	private var callback: ((Float) -> Unit)? = null
	var startCalls = 0
	var stopCalls = 0

	override fun startListening(callback: (Float) -> Unit, sensorDelay: Int): Boolean {
		startCalls++
		if (!registrationSucceeds) return false
		this.callback = callback
		return true
	}

	override fun stopListening() {
		stopCalls++
		callback = null
	}

	fun emit(value: Float) {
		callback?.invoke(value)
	}
}

class FakeProximitySensorReader(
	override val hasProximitySensor: Boolean = true,
	override val maximumRange: Float = 5f,
	registrationSucceeds: Boolean = true
) : FakeSensorReader(registrationSucceeds), ProximitySensorReader

class FakeAdaptiveThemeServiceController : AdaptiveThemeServiceController {
	var startCalls = 0
	var stopCalls = 0
	var lastEnableMonitoring = false
	var startFailure: Exception? = null

	override fun start(enableMonitoring: Boolean) {
		startCalls++
		lastEnableMonitoring = enableMonitoring
		startFailure?.let { throw it }
	}

	override fun stop() {
		stopCalls++
	}
}

class FakeInstallMetadataProvider(
	var fromPlayStore: Boolean = false,
	var installedDaysAgo: Long = 0
) : InstallMetadataProvider {
	override fun isInstalledFromPlayStore(): Boolean = fromPlayStore
	override fun daysSinceFirstInstall(): Long = installedDaysAgo
}

class FakeWallpaperPlatform : WallpaperPlatform {
	var liveWallpaperActive = false
	var permissionFailure: Exception? = null
	val persistedUris = mutableListOf<Uri>()

	override fun isLiveWallpaperActive(): Boolean = liveWallpaperActive

	override fun takePersistableReadPermission(uri: Uri) {
		permissionFailure?.let { throw it }
		persistedUris += uri
	}

	override fun applyWallpaperForTheme(
		isDark: Boolean,
		dayUriStr: String?,
		nightUriStr: String?
	): Boolean = true
}

internal class FakeWallpaperImagePreparer : WallpaperImagePreparer {
	var failure: Exception? = null
	val prepared = mutableListOf<Pair<Uri, WallpaperSlot>>()
	var preparedUri: Uri? = null

	override fun prepare(source: Uri, slot: WallpaperSlot): Uri {
		failure?.let { throw it }
		prepared += source to slot
		return preparedUri ?: source
	}
}

class FakeSetupEnvironmentProvider(
	var current: SetupEnvironmentSnapshot = SetupEnvironmentSnapshot(
		isShizukuInstalled = false,
		isDeveloperOptionsEnabled = false,
		isUsbDebuggingEnabled = false,
		hasWriteSecureSettings = false,
		isUsbConnected = false
	)
) : SetupEnvironmentProvider {
	override fun snapshot(): SetupEnvironmentSnapshot = current
}

class FakeSetupPermissionGrantController : SetupPermissionGrantController {
	var binderReady = false
	var shizukuPermission = false
	var permissionRequestCalls = 0
	var rootResult: SetupGrantResult = SetupGrantResult.Failed("root unavailable")
	var shizukuResult: SetupGrantResult = SetupGrantResult.Failed("shizuku unavailable")
	var rootGrantCalls = 0
	var shizukuGrantCalls = 0
	var listener: ((Int, Boolean) -> Unit)? = null

	override fun registerShizukuPermissionListener(
		listener: (requestCode: Int, granted: Boolean) -> Unit
	): SetupPermissionListenerRegistration {
		this.listener = listener
		return SetupPermissionListenerRegistration {
			this.listener = null
		}
	}

	override fun isShizukuBinderReady(): Boolean = binderReady

	override fun hasShizukuPermission(context: android.content.Context): Boolean =
		shizukuPermission

	override fun requestShizukuPermission() {
		permissionRequestCalls++
	}

	override suspend fun grantViaShizuku(
		context: android.content.Context,
		packageName: String
	): SetupGrantResult {
		shizukuGrantCalls++
		return shizukuResult
	}

	override suspend fun grantViaRoot(packageName: String): SetupGrantResult {
		rootGrantCalls++
		return rootResult
	}
}
