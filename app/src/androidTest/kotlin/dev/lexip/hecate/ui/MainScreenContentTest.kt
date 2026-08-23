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

package dev.lexip.hecate.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.theme.HecateTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DAY_WALLPAPER_URI = "content://wallpaper/day"

@RunWith(AndroidJUnit4::class)
class MainScreenContentTest {

	@get:Rule
	val composeRule = createComposeRule()

	private val context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Before
	fun enableAccessibilityValidation() {
		composeRule.mainClock.autoAdvance = true
		composeRule.enableAccessibilityChecks()
	}

	@Test
	fun setupRequiredInactiveStateShowsSetupAndRequestsSetupFromSwitch() {
		var toggleRequest: Pair<Boolean, Boolean>? = null
		setMainContent(
			uiState = MainUiState(),
			hasPermission = false,
			callbacks = callbacks(
				onToggle = { checked, hasPermission ->
					toggleRequest = checked to hasPermission
					false
				}
			)
		)

		composeRule.onNodeWithText(context.getString(R.string.setup_required_title))
			.assertIsDisplayed()
		composeRule.onNodeWithText(adaptiveThemeAction()).performClick()

		assertEquals(null, toggleRequest)
	}

	@Test
	fun activeStateExposesThresholdAndAdvancedSettings() {
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				adaptiveThemeThresholdLux = 100f
			),
			hasPermission = true
		)

		composeRule.onNodeWithText(context.getString(R.string.title_brightness_threshold))
			.assertExists()
		composeRule.onNodeWithText(context.getString(R.string.action_advanced_settings))
			.assertExists()
	}

	@Test
	fun githubStarPromptForwardsOpenDismissAndUndoActions() {
		var impressionCalls = 0
		var openCalls = 0
		var dismissCalls = 0
		var undoCalls = 0
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				showGitHubStarPrompt = true
			),
			hasPermission = true,
			callbacks = callbacks(
				onGitHubImpression = { impressionCalls++ },
				onOpenGitHub = { openCalls++ },
				onDismissGitHub = { dismissCalls++ },
				onUndoGitHubDismissal = { undoCalls++ }
			)
		)
		composeRule.waitForIdle()
		assertEquals(1, impressionCalls)

		clickTextAfterScroll(context.getString(R.string.github_star_prompt_action))
		assertEquals(1, openCalls)

		clickTextAfterScroll(context.getString(R.string.github_star_prompt_dismiss_description))
		assertEquals(1, dismissCalls)

		composeRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
		assertEquals(1, undoCalls)
	}

	@Test
	fun customThresholdAndNightLockStateAreRendered() {
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				adaptiveThemeThresholdLux = 321f,
				customAdaptiveThemeThresholdLux = 321f,
				stayDarkAtNightEnabled = true,
				nightStartMinutes = 22 * 60,
				nightEndMinutes = 5 * 60
			),
			hasPermission = true
		)

		composeRule.onNodeWithText(context.getString(R.string.adaptive_threshold_custom))
			.assertExists()
		composeRule.onNodeWithText(context.getString(R.string.title_night_dark_lock))
			.assertExists()
	}

	@Test
	fun permissionGrantedSwitchForwardsCallback() {
		var toggleRequest: Pair<Boolean, Boolean>? = null
		setMainContent(
			uiState = MainUiState(),
			hasPermission = true,
			callbacks = callbacks(
				onToggle = { checked, hasPermission ->
					toggleRequest = checked to hasPermission
					true
				}
			)
		)

		composeRule.onNodeWithText(adaptiveThemeAction()).performClick()

		assertEquals(true to true, toggleRequest)
	}

	@Test
	fun advancedSettingsExpandAndCollapseAndRequestReviewOnce() {
		var reviewCalls = 0
		val advancedSettingsText = context.getString(R.string.action_advanced_settings)
		val collapseText = context.getString(R.string.action_collapse)
		setMainContent(
			uiState = MainUiState(adaptiveThemeEnabled = true),
			hasPermission = true,
			callbacks = callbacks(onReview = { reviewCalls++ })
		)

		expandAdvancedSettings()
		scrollToText(collapseText)
		composeRule.onNodeWithText(collapseText)
			.assertIsDisplayed()
			.performClick()
		composeRule.waitForIdle()

		scrollToText(advancedSettingsText)
		composeRule.onNodeWithText(advancedSettingsText)
			.assertIsDisplayed()
		composeRule.onAllNodesWithText(collapseText)
			.assertCountEquals(0)

		assertEquals(1, reviewCalls)
	}

	@Test
	fun successfulServiceDisableCollapsesAdvancedSettings() {
		setMainContent(
			uiState = MainUiState(adaptiveThemeEnabled = true),
			hasPermission = true,
			callbacks = callbacks(onToggle = { _, _ -> true })
		)
		expandAdvancedSettings()

		clickTextAfterScroll(adaptiveThemeAction())

		scrollToText(context.getString(R.string.action_advanced_settings))
		composeRule.onNodeWithText(context.getString(R.string.action_advanced_settings))
			.assertIsDisplayed()
		composeRule.onAllNodesWithText(context.getString(R.string.action_collapse))
			.assertCountEquals(0)
	}

	@Test
	fun rejectedServiceDisableKeepsAdvancedSettingsExpanded() {
		setMainContent(
			uiState = MainUiState(adaptiveThemeEnabled = true),
			hasPermission = true,
			callbacks = callbacks(onToggle = { _, _ -> false })
		)
		expandAdvancedSettings()

		clickTextAfterScroll(adaptiveThemeAction())

		scrollToText(context.getString(R.string.action_collapse))
		composeRule.onNodeWithText(context.getString(R.string.action_collapse))
			.assertIsDisplayed()
	}

	@Test
	fun enabledWallpaperSyncInitiallyExpandsAndShowsStatusAndPickerActions() {
		var daySelectionCalls = 0
		var nightSelectionCalls = 0
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				wallpaperSyncEnabled = true,
				dayWallpaperUri = DAY_WALLPAPER_URI,
				nightWallpaperUri = null
			),
			hasPermission = true,
			callbacks = callbacks(
				onSelectDay = { daySelectionCalls++ },
				onSelectNight = { nightSelectionCalls++ }
			)
		)

		scrollToText(context.getString(R.string.title_device_wallpaper_sync))
		composeRule.onNodeWithText(context.getString(R.string.title_device_wallpaper_sync))
			.assertIsDisplayed()
		scrollToText(context.getString(R.string.label_beta))
		composeRule.onNodeWithText(context.getString(R.string.label_beta))
			.assertIsDisplayed()
		clickTextAfterScroll(wallpaperButtonText(day = true, isSet = true))
		clickTextAfterScroll(wallpaperButtonText(day = false, isSet = false))

		assertEquals(1, daySelectionCalls)
		assertEquals(1, nightSelectionCalls)
	}

	@Test
	fun wallpaperSyncRejectsIncompleteSelection() {
		val toggleRequests = mutableListOf<Boolean>()
		setMainContent(
			uiState = MainUiState(adaptiveThemeEnabled = true),
			hasPermission = true,
			callbacks = callbacks(onWallpaperToggle = toggleRequests::add)
		)
		expandAdvancedSettings()
		clickTextAfterScroll(context.getString(R.string.title_device_wallpaper_sync))

		assertEquals(emptyList<Boolean>(), toggleRequests)
	}

	@Test
	fun wallpaperSyncEnablesWhenBothImagesAreSelected() {
		val toggleRequests = mutableListOf<Boolean>()
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				dayWallpaperUri = DAY_WALLPAPER_URI,
				nightWallpaperUri = "content://wallpaper/night"
			),
			hasPermission = true,
			callbacks = callbacks(onWallpaperToggle = toggleRequests::add)
		)
		expandAdvancedSettings()
		clickTextAfterScroll(context.getString(R.string.title_device_wallpaper_sync))

		assertEquals(listOf(true), toggleRequests)
	}

	@Test
	fun wallpaperSyncDispatchesDisable() {
		val toggleRequests = mutableListOf<Boolean>()
		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				wallpaperSyncEnabled = true,
				dayWallpaperUri = DAY_WALLPAPER_URI,
				nightWallpaperUri = "content://wallpaper/night"
			),
			hasPermission = true,
			callbacks = callbacks(onWallpaperToggle = toggleRequests::add)
		)
		clickTextAfterScroll(context.getString(R.string.title_device_wallpaper_sync))

		assertEquals(listOf(false), toggleRequests)
	}

	@Test
	fun lockScreenBlurStaysVisibleWithoutWallpaperSyncAndDispatchesWhenEnabled() {
		val blurRequests = mutableListOf<Boolean>()
		setMainContent(
			uiState = MainUiState(adaptiveThemeEnabled = true),
			hasPermission = true,
			callbacks = callbacks(onLockScreenBlur = blurRequests::add)
		)
		expandAdvancedSettings()
		scrollToText(context.getString(R.string.title_lock_screen_wallpaper_blur))
		composeRule.onNodeWithText(context.getString(R.string.title_lock_screen_wallpaper_blur))
			.assertIsDisplayed()
		assertEquals(emptyList<Boolean>(), blurRequests)

		setMainContent(
			uiState = MainUiState(
				adaptiveThemeEnabled = true,
				wallpaperSyncEnabled = true
			),
			hasPermission = true,
			callbacks = callbacks(onLockScreenBlur = blurRequests::add)
		)
		clickTextAfterScroll(context.getString(R.string.title_lock_screen_wallpaper_blur))
		assertEquals(listOf(true), blurRequests)
	}

	@Test
	fun liveWallpaperWarningDispatchesConfirm() {
		var confirmCalls = 0
		var dismissCalls = 0
		setMainContent(
			uiState = MainUiState(showLiveWallpaperWarningDialog = true),
			hasPermission = true,
			callbacks = callbacks(
				onConfirmLiveWallpaper = { confirmCalls++ },
				onDismissLiveWallpaper = { dismissCalls++ }
			)
		)

		composeRule.onNodeWithText(context.getString(R.string.live_wallpaper_warning_title))
			.assertIsDisplayed()
		composeRule.onNodeWithText(context.getString(R.string.action_continue)).performClick()
		assertEquals(1, confirmCalls)
		assertEquals(0, dismissCalls)
	}

	@Test
	fun liveWallpaperWarningDispatchesDismiss() {
		var confirmCalls = 0
		var dismissCalls = 0
		setMainContent(
			uiState = MainUiState(showLiveWallpaperWarningDialog = true),
			hasPermission = true,
			callbacks = callbacks(
				onConfirmLiveWallpaper = { confirmCalls++ },
				onDismissLiveWallpaper = { dismissCalls++ }
			)
		)
		composeRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
		assertEquals(0, confirmCalls)
		assertEquals(1, dismissCalls)
	}

	private fun setMainContent(
		uiState: MainUiState,
		hasPermission: Boolean,
		callbacks: MainScreenCallbacks = callbacks()
	) {
		composeRule.setContent {
			HecateTheme {
				MainScreenContent(
					uiState = uiState,
					currentSensorLux = 25f,
					isDeviceCovered = false,
					isBatterySaverActive = false,
					hasWriteSecureSettingsPermission = hasPermission,
					packageName = context.packageName,
					callbacks = callbacks
				)
			}
		}
	}

	private fun callbacks(
		onToggle: (Boolean, Boolean) -> Boolean = { _, _ -> true },
		onReview: () -> Unit = {},
		onWallpaperToggle: (Boolean) -> Unit = {},
		onLockScreenBlur: (Boolean) -> Unit = {},
		onSelectDay: () -> Unit = {},
		onSelectNight: () -> Unit = {},
		onConfirmLiveWallpaper: () -> Unit = {},
		onDismissLiveWallpaper: () -> Unit = {},
		onGitHubImpression: () -> Unit = {},
		onOpenGitHub: () -> Unit = {},
		onDismissGitHub: () -> Unit = {},
		onUndoGitHubDismissal: () -> Unit = {}
	): MainScreenCallbacks = MainScreenCallbacks(
		onServiceToggleRequested = onToggle,
		onThresholdSelected = { _, _ -> },
		onCheckReviewPrompt = onReview,
		onStayDarkAtNightChanged = {},
		onWallpaperSyncToggleRequested = onWallpaperToggle,
		onLockScreenWallpaperBlurChanged = onLockScreenBlur,
		onSelectDayWallpaper = onSelectDay,
		onSelectNightWallpaper = onSelectNight,
		onConfirmLiveWallpaper = onConfirmLiveWallpaper,
		onDismissLiveWallpaperWarning = onDismissLiveWallpaper,
		onCustomThresholdConfirmed = {},
		onNightWindowChanged = { _, _, _ -> },
		onGitHubStarPromptImpression = onGitHubImpression,
		onOpenGitHubRepository = onOpenGitHub,
		onDismissGitHubStarPrompt = onDismissGitHub,
		onUndoGitHubStarPromptDismissal = onUndoGitHubDismissal
	)

	private fun adaptiveThemeAction(): String = context.getString(
		R.string.action_use_adaptive_theme,
		context.getString(R.string.app_name)
	)

	private fun expandAdvancedSettings() {
		clickTextAfterScroll(context.getString(R.string.action_advanced_settings))
		composeRule.waitForIdle()
		waitForText(context.getString(R.string.action_collapse))
	}

	private fun clickTextAfterScroll(text: String) {
		scrollToText(text)
		composeRule.onNodeWithText(text).performClick()
	}

	private fun scrollToText(text: String) {
		waitForText(text)
		composeRule.onNodeWithText(text).performScrollTo()
	}

	private fun waitForText(text: String) {
		composeRule.waitUntil(timeoutMillis = 5_000) {
			composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
		}
	}

	private fun wallpaperButtonText(day: Boolean, isSet: Boolean): String {
		val label = context.getString(
			if (day) R.string.action_select_day_wallpaper
			else R.string.action_select_night_wallpaper
		)
		val status = context.getString(
			if (isSet) R.string.wallpaper_status_set
			else R.string.wallpaper_status_not_set
		)
		return "$label\n$status"
	}
}
