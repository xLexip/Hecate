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

package dev.lexip.hecate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.ui.theme.HecateTheme
import dev.lexip.hecate.util.DarkThemeHandler

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		installSplashScreen()
		enableEdgeToEdge()

		setContent {
			val dataStore = (this.applicationContext as HecateApplication).userPreferencesDataStore
			val adaptiveThemeViewModel: AdaptiveThemeViewModel = viewModel(
				factory = AdaptiveThemeViewModelFactory(
					this.application as HecateApplication,
					UserPreferencesRepository(dataStore),
					DarkThemeHandler(applicationContext)
				)
			)
			val state by adaptiveThemeViewModel.uiState.collectAsState()
			HecateTheme {
				AdaptiveThemeScreen(state, adaptiveThemeViewModel::updateAdaptiveThemeEnabled)
			}
		}

	}
}

