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

package dev.lexip.hecate.ui.setup.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
internal fun ShizukuOptionCard(
	isVisible: Boolean,
	@StringRes actionRes: Int = R.string.setup_shizuku_action,
	onClick: () -> Unit,
) {
	if (!isVisible) return

	val haptic = LocalHapticFeedback.current

	ElevatedCard(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.secondaryContainer,
		)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(id = R.string.setup_shizuku_title),
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSecondaryContainer
			)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = stringResource(id = R.string.setup_shizuku_description),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSecondaryContainer
			)
			Spacer(modifier = Modifier.height(8.dp))
			OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = {
					haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
					onClick()
				}
			) {
				Text(
					text = stringResource(id = actionRes),
					textAlign = TextAlign.Center
				)
			}
		}
	}
}
