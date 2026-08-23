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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.setup.SetupUiState
import dev.lexip.hecate.ui.setup.components.ForExpertsSectionCard
import dev.lexip.hecate.ui.setup.components.SetupWaitingCard
import dev.lexip.hecate.ui.setup.components.StepNavigationRow


@Composable
fun C_GrantPermissionScreen(
	uiState: SetupUiState,
	onShareSetupUrl: () -> Unit,
	onShareExpertCommand: () -> Unit,
	onFinish: () -> Unit,
	onBack: () -> Unit,
	onUseRoot: () -> Unit,
) {
	val haptic = LocalHapticFeedback.current


	// Haptic feedback when permission granted
	LaunchedEffect(uiState.hasWriteSecureSettings) {
		if (uiState.hasWriteSecureSettings) {
			haptic.performHapticFeedback(HapticFeedbackType.Confirm)
		}
	}

	SetupScreenScaffold(
		currentStepIndex = 2,
		totalSteps = 3
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Column(
				modifier = Modifier
					.weight(1f)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(24.dp)
			) {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(
						text = stringResource(id = R.string.setup_grant_title),
						style = MaterialTheme.typography.headlineMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						text = stringResource(id = R.string.setup_grant_body),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				SetupWebsiteCard(onShareSetupUrl = onShareSetupUrl)

				// Show USB not connected waiting card if USB is not connected (e.g. step 2 was skipped)
				if (!uiState.isUsbConnected) {
					SetupWaitingCard(
						title = stringResource(id = R.string.setup_usb_not_connected),
						isWaiting = true
					)
				}

				// Show permission waiting card
				SetupWaitingCard(
					title = if (uiState.hasWriteSecureSettings)
						stringResource(id = R.string.setup_permission_granted)
					else
						stringResource(id = R.string.setup_permission_not_granted),
					isWaiting = !uiState.hasWriteSecureSettings
				)

				ForExpertsSectionCard(
					onUseRoot = onUseRoot,
					onShareADBCommand = onShareExpertCommand
				)
			}

			// Step 3: Back button (left) + Finish button (right, disabled until permission granted)
			StepNavigationRow(
				leftTextRes = R.string.action_back,
				onLeft = onBack,
				rightTextRes = R.string.action_finish,
				onRight = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onFinish()
				},
				rightEnabled = uiState.hasWriteSecureSettings,
				rightIsPrimary = true
			)
		}
	}
}

@Composable
private fun SetupWebsiteCard(onShareSetupUrl: () -> Unit) {
	val haptic = LocalHapticFeedback.current

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(id = R.string.setup_website_url),
				style = MaterialTheme.typography.displaySmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(16.dp))
			OutlinedButton(
				border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onShareSetupUrl()
				}
			) {
				Text(
					text = stringResource(id = R.string.action_share_url),
				)
			}
		}
	}
}
