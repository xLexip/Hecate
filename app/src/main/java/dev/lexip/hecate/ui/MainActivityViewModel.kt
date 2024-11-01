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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainActivityUiState(
	val isAdaptiveThemeEnabled: Boolean = false
)

class MainActivityViewModel : ViewModel() {
	private val _uiState = MutableStateFlow(MainActivityUiState())
	val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

	fun toggleAdaptiveTheme() {
		_uiState.update {
			it.copy(
				isAdaptiveThemeEnabled = !it.isAdaptiveThemeEnabled
			)
		}
	}
}