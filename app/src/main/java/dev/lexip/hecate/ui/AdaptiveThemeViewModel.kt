package dev.lexip.hecate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class AdaptiveThemeUiState(
	val isAdaptiveThemeEnabled: Boolean = false
)

class AdaptiveThemeViewModel(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(AdaptiveThemeUiState())
	val uiState: StateFlow<AdaptiveThemeUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				_uiState.value = AdaptiveThemeUiState(
					isAdaptiveThemeEnabled = userPreferences.serviceEnabled
				)
			}
		}
	}

	fun updateServiceEnabled(enabled: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.updateServiceEnabled(enabled)
		}
	}

}

class AdaptiveThemeViewModelFactory(
	private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(AdaptiveThemeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return AdaptiveThemeViewModel(userPreferencesRepository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}