/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui.setup.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.SetupUiState
import dev.lexip.hecate.ui.setup.components.ForExpertsSectionCard
import dev.lexip.hecate.ui.setup.components.SetupFAQCards
import dev.lexip.hecate.ui.setup.components.SetupWaitingCard
import dev.lexip.hecate.ui.setup.components.ShizukuOptionCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow
import kotlinx.coroutines.launch


@Composable
fun B_ConnectUsbScreen(
	uiState: SetupUiState,
	onGrantViaShizuku: () -> Unit,
	onNext: () -> Unit,
	onBack: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onUseRoot: () -> Unit,
	onInstallShizuku: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current
	val scrollState = rememberScrollState()
	val coroutineScope = rememberCoroutineScope()


	// Haptic feedback when USB connected
	val previousUsbConnected = remember { mutableStateOf(uiState.isUsbConnected) }
	LaunchedEffect(uiState.isUsbConnected) {
		if (uiState.isUsbConnected && !previousUsbConnected.value) {
			haptic.performHapticFeedback(HapticFeedbackType.Confirm)
		}
		previousUsbConnected.value = uiState.isUsbConnected
	}


	SetupScreenScaffold(
		currentStepIndex = 1,
		totalSteps = 3
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Column(
				modifier = Modifier
					.weight(1f)
					.verticalScroll(scrollState),
				verticalArrangement = Arrangement.spacedBy(24.dp)
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(
						text = stringResource(id = R.string.setup_connect_title),
						style = MaterialTheme.typography.headlineMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						text = stringResource(id = R.string.setup_connect_description),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				SetupWaitingCard(
					title =
						if (uiState.isUsbConnected) stringResource(id = R.string.setup_usb_connected)
						else stringResource(id = R.string.setup_usb_not_connected),
					isWaiting = !uiState.isUsbConnected
				)

				ShizukuOptionCard(
					isVisible = uiState.isShizukuInstalled,
					onClick = onGrantViaShizuku
				)

				SetupFAQCards()

				ShizukuOptionCard(
					isVisible = !uiState.isShizukuInstalled,
					actionRes = R.string.setup_shizuku_install_action,
					onClick = onInstallShizuku
				)

				ForExpertsSectionCard(
					onUseRoot = onUseRoot,
					onShareADBCommand = onShareExpertCommand,
					onExpansionStarted = {
						coroutineScope.launch {
							var previousMaxValue = -1
							var stableFrameCount = 0
							while (stableFrameCount < 5) {
								withFrameNanos { }
								val currentMaxValue = scrollState.maxValue
								scrollState.scrollTo(currentMaxValue)
								stableFrameCount =
									if (currentMaxValue == previousMaxValue) stableFrameCount + 1
									else 0
								previousMaxValue = currentMaxValue
							}
						}
					}
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Step 2: Back button (left) + Skip/Continue button (right, outlined)
			StepNavigationRow(
				leftTextRes = R.string.action_back,
				onLeft = onBack,
				rightTextRes = if (uiState.isUsbConnected) R.string.action_continue else R.string.action_skip,
				onRight = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onNext()
				},
				rightEnabled = true,
				rightIsPrimary = uiState.isUsbConnected
			)
		}
	}
}
