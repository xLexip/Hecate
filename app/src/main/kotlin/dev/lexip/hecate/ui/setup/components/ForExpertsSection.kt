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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
internal fun ForExpertsSectionCard(
	onUseRoot: (() -> Unit)? = null,
	onShareADBCommand: (() -> Unit)? = null,
	onExpansionStarted: (() -> Unit)? = null,
) {
	val haptic = LocalHapticFeedback.current
	var expanded by remember { mutableStateOf(false) }
	val expansionStateDescription = stringResource(
		id = if (expanded) R.string.state_expanded else R.string.state_collapsed
	)

	val toggleExpanded = {
		expanded = !expanded
		if (expanded) {
			onExpansionStarted?.invoke()
		}
	}

	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(
			modifier = Modifier
				.padding(16.dp)
				.animateContentSize()
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.semantics {
						stateDescription = expansionStateDescription
					}
					.clickable(
						role = Role.Button,
						onClick = toggleExpanded
					),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = stringResource(id = R.string.setup_alternative_methods),
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.weight(1f)
				)
				val rotation by animateFloatAsState(
					targetValue = if (expanded) 180f else 0f,
					animationSpec = tween(durationMillis = 300)
				)
				Icon(
					imageVector = Icons.Outlined.ExpandMore,
					contentDescription = null,
					modifier = Modifier
						.size(48.dp)
						.padding(12.dp)
						.rotate(rotation)
				)
			}

			AnimatedVisibility(
				visible = expanded,
				enter = fadeIn(animationSpec = tween(200)) + expandVertically(
					animationSpec = tween(
						300
					)
				),
				exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
					animationSpec = tween(
						200
					)
				)
			) {
				Column(modifier = Modifier.fillMaxWidth()) {
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = stringResource(id = R.string.setup_alternatives),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(12.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						OutlinedButton(
							onClick = {
								haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
								onUseRoot?.invoke()
							},
							modifier = Modifier.weight(1f)
						) {
							Text(text = stringResource(id = R.string.setup_action_use_root))
						}
						OutlinedButton(
							onClick = {
								haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
								onShareADBCommand?.invoke()
							},
							modifier = Modifier.weight(1f)
						) {
							Text(text = stringResource(id = R.string.setup_action_adb_command))
						}
					}
				}
			}
		}
	}
}
