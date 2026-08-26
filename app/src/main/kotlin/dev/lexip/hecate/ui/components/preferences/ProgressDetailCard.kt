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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.components.SegmentedProgressIndicator
import dev.lexip.hecate.util.formatLux

@Composable
fun ProgressDetailCard(
	title: String,
	currentLux: Float,
	luxSteps: List<Float>,
	enabled: Boolean = true,
	firstCard: Boolean = false,
	lastCard: Boolean = false
) {
	DetailPreferenceCard(
		title = title,
		enabled = enabled,
		firstCard = firstCard,
		lastCard = lastCard
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp)
				.padding(top = 12.dp, bottom = 8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			val segments = (luxSteps.size - 1).coerceAtLeast(1)

			val activeIndex = remember(currentLux, luxSteps) {
				computeActiveSegmentIndex(
					luxSteps,
					currentLux
				).coerceIn(-1, segments - 1)
			}

			val haptic = LocalHapticFeedback.current
			val previousIndex = remember { mutableStateOf<Int?>(null) }
			LaunchedEffect(activeIndex) {
				val prev = previousIndex.value
				if (prev != null && prev != activeIndex) {
					haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
				}
				previousIndex.value = activeIndex
			}

			SegmentedProgressIndicator(
				segments = segments,
				activeIndex = activeIndex,
				enabled = enabled
			)

			// Live lux measurement
			if (enabled) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 4.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Text(
						text = stringResource(id = R.string.title_live_measurement),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = "${currentLux.formatLux()} lx",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.End
					)
				}
			}
		}
	}
}

internal fun computeActiveSegmentIndex(luxSteps: List<Float>, currentLux: Float): Int {
	val n = luxSteps.size
	if (n < 2) return -1
	var idx = -1
	for (i in 0 until n - 1) {
		val upper = luxSteps.getOrNull(i + 1) ?: continue
		if (currentLux >= upper) idx = i
	}
	return idx
}
