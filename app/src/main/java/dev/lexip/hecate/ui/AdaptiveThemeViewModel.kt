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

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.lexip.hecate.HecateApplication
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.services.BroadcastReceiverService
import dev.lexip.hecate.util.DarkThemeHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AdaptiveThemeViewModel"

data class AdaptiveThemeUiState(
	val adaptiveThemeEnabled: Boolean = false
)

class AdaptiveThemeViewModel(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	private var darkThemeHandler: DarkThemeHandler
) : ViewModel() {

	private val _uiState = MutableStateFlow(AdaptiveThemeUiState())
	val uiState: StateFlow<AdaptiveThemeUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			userPreferencesRepository.userPreferencesFlow.collect { userPreferences ->
				_uiState.value = AdaptiveThemeUiState(
					adaptiveThemeEnabled = userPreferences.adaptiveThemeEnabled
				)
			}
		}
	}

	fun updateAdaptiveThemeEnabled(enable: Boolean) {
		// TODO #30: Check for android.permission.WRITE_SECURE_SETTINGS
		viewModelScope.launch {
			userPreferencesRepository.updateAdaptiveThemeEnabled(enable)
			if (enable) startBroadcastReceiverService() else stopBroadcastReceiverService()
			updateAdaptiveThemeThresholdLux(500f)
		}
	}

	private fun startBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		application.applicationContext.startService(intent)
	}

	private fun stopBroadcastReceiverService() {
		val intent = Intent(application.applicationContext, BroadcastReceiverService::class.java)
		application.applicationContext.stopService(intent)
	}
	
	private suspend fun updateAdaptiveThemeThresholdLux(lux: Float) {
		userPreferencesRepository.updateAdaptiveThemeThresholdLux(lux)
	}

}

class AdaptiveThemeViewModelFactory(
	private val application: HecateApplication,
	private val userPreferencesRepository: UserPreferencesRepository,
	private val darkThemeHandler: DarkThemeHandler
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(AdaptiveThemeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return AdaptiveThemeViewModel(
				application,
				userPreferencesRepository,
				darkThemeHandler
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}