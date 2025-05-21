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

package dev.lexip.hecate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SwitchPreferenceCard(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
	Card(
		modifier = Modifier
			.fillMaxWidth(),
		shape = RoundedCornerShape(percent = 100),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
		onClick = { onCheckedChange(!isChecked) }
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 20.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				text = text
			)
			Switch(
				checked = isChecked,
				onCheckedChange = onCheckedChange,
				thumbContent = if (isChecked) {
					{
						Icon(
							imageVector = Icons.Filled.Check,
							contentDescription = null,
							modifier = Modifier.size(SwitchDefaults.IconSize),
						)
					}
				} else {
					{
						Icon(
							imageVector = Icons.Filled.Clear,
							contentDescription = null,
							modifier = Modifier.size(SwitchDefaults.IconSize),
						)
					}
				}
			)
		}
	}
}