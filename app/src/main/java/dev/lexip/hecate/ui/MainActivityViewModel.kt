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