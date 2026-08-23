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

package dev.lexip.hecate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.lexip.hecate.R

@Composable
fun GitHubStarPromptCard(
	onImpression: () -> Unit,
	onDismiss: () -> Unit,
	onOpenGitHub: () -> Unit
) {
	LaunchedEffect(Unit) {
		onImpression()
	}

	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(24.dp),
		color = MaterialTheme.colorScheme.surfaceContainerLow,
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
	) {
		Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
			Row(verticalAlignment = Alignment.Top) {
				Icon(
					modifier = Modifier
						.padding(top = 4.dp)
						.size(24.dp),
					imageVector = Icons.Outlined.Star,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary
				)
				Spacer(modifier = Modifier.width(12.dp))
				Column(
					modifier = Modifier
						.weight(1f)
						.padding(top = 2.dp)
				) {
					Text(
						text = stringResource(R.string.title_support_project),
						style = MaterialTheme.typography.titleMedium
					)
					Text(
						modifier = Modifier.padding(top = 4.dp),
						text = stringResource(R.string.github_star_prompt_description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End
			) {
				TextButton(onClick = onDismiss) {
					Text(text = stringResource(R.string.github_star_prompt_dismiss_description))
				}
				TextButton(onClick = onOpenGitHub) {
					Text(text = stringResource(R.string.github_star_prompt_action))
				}
			}
		}
	}
}
