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

package dev.lexip.hecate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.components.SwitchPreferenceCard
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	uiState: AdaptiveThemeUiState,
	updateAdaptiveThemeEnabled: (Boolean) -> Unit
) {
	val scrollBehavior =
		TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
	val horizontalOffsetPadding = 8.dp
	Scaffold(
		modifier = Modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			LargeTopAppBar(
				modifier = Modifier
					.padding(horizontal = horizontalOffsetPadding)
					.padding(top = 22.dp, bottom = 12.dp),
				colors = hecateTopAppBarColors(),
				title = {
					Text(
						text = stringResource(id = R.string.app_name),
						style = MaterialTheme.typography.displaySmall,
						fontWeight = FontWeight.Medium
					)
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(32.dp)

		) {
			Text(
				modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
				text = stringResource(id = R.string.description_adaptive_theme),
				style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
			)
			SwitchPreferenceCard(
				text = stringResource(
					id = R.string.action_use_adaptive_theme
				),
				isChecked = uiState.adaptiveThemeEnabled,
				onCheckedChange = { checked -> updateAdaptiveThemeEnabled(checked) }

			)
		}
	}
}