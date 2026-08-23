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

package dev.lexip.hecate.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.screens.A_DeveloperModeScreen
import dev.lexip.hecate.ui.setup.screens.B_ConnectUsbScreen
import dev.lexip.hecate.ui.setup.screens.C_GrantPermissionScreen
import dev.lexip.hecate.ui.setup.components.ForExpertsSectionCard
import dev.lexip.hecate.ui.theme.HecateTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreensTest {

	@get:Rule
	val composeRule = createComposeRule()

	private val context
		get() = InstrumentationRegistry.getInstrumentation().targetContext

	@Before
	fun enableAccessibilityValidation() {
		composeRule.enableAccessibilityChecks()
	}

	@Test
	fun developerStepDisablesContinueWhileRequirementsAreIncomplete() {
		setDeveloperScreen(SetupUiState())
		composeRule.onNodeWithText(context.getString(R.string.action_continue))
			.assertIsNotEnabled()
	}

	@Test
	fun developerStepContinueInvokesCallback() {
		var nextCalls = 0
		setDeveloperScreen(
			state = SetupUiState(
				isDeveloperOptionsEnabled = true,
				isUsbDebuggingEnabled = true
			),
			onNext = { nextCalls++ }
		)

		composeRule.onNodeWithText(context.getString(R.string.action_continue)).performClick()

		assertEquals(1, nextCalls)
	}

	@Test
	fun connectStepOffersSkipWhenUsbIsNotConnected() {
		var nextCalls = 0
		setConnectScreen(SetupUiState(), onNext = { nextCalls++ })
		composeRule.onNodeWithText(context.getString(R.string.action_skip)).performClick()
		assertEquals(1, nextCalls)
	}

	@Test
	fun connectStepOffersContinueWhenUsbIsConnected() {
		var nextCalls = 0
		setConnectScreen(
			SetupUiState(isUsbConnected = true),
			onNext = { nextCalls++ }
		)
		composeRule.onNodeWithText(context.getString(R.string.action_continue))
			.assertIsEnabled()
			.performClick()
		assertEquals(1, nextCalls)
	}

	@Test
	fun connectStepOffersShizukuInstallCardWhenShizukuIsNotInstalled() {
		var installCalls = 0
		setConnectScreen(
			state = SetupUiState(isShizukuInstalled = false),
			onInstallShizuku = { installCalls++ }
		)

		composeRule.onNodeWithText(context.getString(R.string.setup_shizuku_install_action))
			.performScrollTo()
			.assertIsDisplayed()
			.performClick()

		assertEquals(1, installCalls)
	}

	@Test
	fun connectStepUsesShizukuCardForGrantWhenShizukuIsInstalled() {
		var grantCalls = 0
		var installCalls = 0
		setConnectScreen(
			state = SetupUiState(isShizukuInstalled = true),
			onGrantViaShizuku = { grantCalls++ },
			onInstallShizuku = { installCalls++ }
		)

		composeRule.onNodeWithText(context.getString(R.string.setup_shizuku_action))
			.performScrollTo()
			.assertIsDisplayed()
			.performClick()

		assertEquals(1, grantCalls)
		assertEquals(0, installCalls)
	}

	@Test
	fun grantStepDisablesFinishWithoutPermission() {
		setGrantScreen(SetupUiState())
		composeRule.onNodeWithText(context.getString(R.string.action_finish))
			.assertIsNotEnabled()
	}

	@Test
	fun grantStepEnablesFinishAfterPermissionAndInvokesCallback() {
		var finishCalls = 0
		setGrantScreen(
			SetupUiState(hasWriteSecureSettings = true),
			onFinish = { finishCalls++ }
		)
		composeRule.onNodeWithText(context.getString(R.string.action_finish))
			.assertIsEnabled()
			.performClick()
		assertEquals(1, finishCalls)
	}

	@Test
	fun expertSectionReportsExpansionStateAndOnlyCallsExpansionHookWhenOpening() {
		var expansionCalls = 0
		composeRule.setContent {
			HecateTheme {
				ForExpertsSectionCard(onExpansionStarted = { expansionCalls++ })
			}
		}
		val section = composeRule.onNodeWithText(
			context.getString(R.string.setup_alternative_methods)
		)

		section.assert(
			SemanticsMatcher.expectValue(
				SemanticsProperties.StateDescription,
				context.getString(R.string.state_collapsed)
			)
		)
		section.performClick()
		composeRule.waitForIdle()
		section.assert(
			SemanticsMatcher.expectValue(
				SemanticsProperties.StateDescription,
				context.getString(R.string.state_expanded)
			)
		)
		section.performClick()
		composeRule.waitForIdle()

		section.assert(
			SemanticsMatcher.expectValue(
				SemanticsProperties.StateDescription,
				context.getString(R.string.state_collapsed)
			)
		)
		assertEquals(1, expansionCalls)
	}

	@Test
	fun expertSectionDoesNotOfferShizukuAction() {
		composeRule.setContent {
			HecateTheme {
				ForExpertsSectionCard()
			}
		}

		composeRule.onNodeWithText(context.getString(R.string.setup_alternative_methods))
			.performClick()
		composeRule.waitForIdle()

		assertTrue(
			composeRule.onAllNodesWithText(context.getString(R.string.setup_shizuku_action))
				.fetchSemanticsNodes()
				.isEmpty()
		)
	}

	@Test
	fun expandingAlternativeMethodsAutoScrollsExpertActionsIntoView() {
		composeRule.setContent {
			HecateTheme {
				Box(
					modifier = Modifier
						.width(360.dp)
						.height(500.dp)
				) {
					B_ConnectUsbScreen(
						uiState = SetupUiState(),
						onGrantViaShizuku = {},
						onNext = {},
						onBack = {},
						onShareExpertCommand = {},
						onUseRoot = {},
						onInstallShizuku = {}
					)
				}
			}
		}

		composeRule.onNodeWithText(context.getString(R.string.setup_alternative_methods))
			.performScrollTo()
			.performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText(context.getString(R.string.setup_action_adb_command))
			.assertIsDisplayed()
	}

	private fun setDeveloperScreen(
		state: SetupUiState,
		onNext: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				A_DeveloperModeScreen(
					uiState = state,
					onGrantViaShizuku = {},
					onNext = onNext,
					onExit = {},
					onOpenSettings = {},
					onOpenDeveloperSettings = {}
				)
			}
		}
	}

	private fun setConnectScreen(
		state: SetupUiState,
		onNext: () -> Unit = {},
		onGrantViaShizuku: () -> Unit = {},
		onInstallShizuku: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				B_ConnectUsbScreen(
					uiState = state,
					onGrantViaShizuku = onGrantViaShizuku,
					onNext = onNext,
					onBack = {},
					onShareExpertCommand = {},
					onUseRoot = {},
					onInstallShizuku = onInstallShizuku
				)
			}
		}
	}

	private fun setGrantScreen(
		state: SetupUiState,
		onFinish: () -> Unit = {}
	) {
		composeRule.setContent {
			HecateTheme {
				C_GrantPermissionScreen(
					uiState = state,
					onShareSetupUrl = {},
					onShareExpertCommand = {},
					onFinish = onFinish,
					onBack = {},
					onUseRoot = {}
				)
			}
		}
	}
}
