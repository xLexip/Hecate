/*
 * Copyright (C) 2024 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun HecateTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	context: Context = LocalContext.current,
	content: @Composable () -> Unit
) {
	val colorScheme =
		if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

	MaterialTheme(
		colorScheme = colorScheme,
		content = content
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun hecateTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(
	// This represents the top app bar style of the android system settings app in Android 15.
	containerColor = MaterialTheme.colorScheme.surfaceContainer,
	scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
	navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
	titleContentColor = MaterialTheme.colorScheme.onSurface,
	actionIconContentColor = MaterialTheme.colorScheme.onSurface
)