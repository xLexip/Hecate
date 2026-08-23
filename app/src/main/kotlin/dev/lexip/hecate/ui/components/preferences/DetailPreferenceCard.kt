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

package dev.lexip.hecate.ui.components.preferences

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun DetailPreferenceCard(
	title: String,
	enabled: Boolean = true,
	firstCard: Boolean = false,
	lastCard: Boolean = false,
	toggleableValue: Boolean? = null,
	onToggle: ((Boolean) -> Unit)? = null,
	titleTrailingContent: (@Composable RowScope.() -> Unit)? = null,
	cardTrailingContent: (@Composable () -> Unit)? = null,
	content: @Composable () -> Unit
) {
	val largeRadius = 20.dp
	val smallRadius = 4.dp
	val shape = RoundedCornerShape(
		topStart = if (firstCard) largeRadius else smallRadius,
		topEnd = if (firstCard) largeRadius else smallRadius,
		bottomStart = if (lastCard) largeRadius else smallRadius,
		bottomEnd = if (lastCard) largeRadius else smallRadius,
	)

	// Animate based on enabled state
	val animatedAlpha = animateFloatAsState(
		targetValue = if (enabled) 1f else 0.38f,
		animationSpec = tween(durationMillis = 250)
	)

	val toggleValue = toggleableValue
	val onToggleValue = onToggle
	val isToggleable = toggleValue != null && onToggleValue != null

	Card(
		modifier = if (isToggleable) {
			Modifier.toggleable(
				value = toggleValue,
				enabled = enabled,
				role = Role.Switch,
				onValueChange = { onToggleValue(it) }
			)
		} else {
			Modifier
		},
		shape = shape,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp)
				.animateContentSize(
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioMediumBouncy,
						stiffness = Spring.StiffnessMediumLow
					)
				)
				.alpha(animatedAlpha.value),
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(modifier = Modifier.weight(1f)) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = title,
						modifier = Modifier.weight(1f),
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					titleTrailingContent?.invoke(this)
				}

				content()
			}
			cardTrailingContent?.invoke()
		}
	}
}
