/*
 * Copyright (C) 2025-2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.services

import dev.lexip.hecate.data.UserPreferencesDataSource

enum class QuickSettingsState {
	UNAVAILABLE,
	INACTIVE,
	ACTIVE
}

object QuickSettingsStatePolicy {
	fun from(hasPermission: Boolean, adaptiveThemeEnabled: Boolean): QuickSettingsState {
		if (!hasPermission) return QuickSettingsState.UNAVAILABLE
		return if (adaptiveThemeEnabled) QuickSettingsState.ACTIVE else QuickSettingsState.INACTIVE
	}
}

sealed interface QuickSettingsToggleResult {
	data object Enabled : QuickSettingsToggleResult
	data object Disabled : QuickSettingsToggleResult
	data class StartFailed(val cause: Exception) : QuickSettingsToggleResult
}

class QuickSettingsToggleCoordinator(
	private val preferences: UserPreferencesDataSource,
	private val serviceController: AdaptiveThemeServiceController
) {
	suspend fun toggle(currentlyEnabled: Boolean): QuickSettingsToggleResult {
		val enable = !currentlyEnabled
		if (enable) {
			try {
				serviceController.start(
					enableMonitoring = true,
					evaluateImmediately = true
				)
			} catch (exception: Exception) {
				return QuickSettingsToggleResult.StartFailed(exception)
			}
			preferences.ensureAdaptiveThemeThresholdDefault()
			preferences.updateAdaptiveThemeEnabled(true)
			return QuickSettingsToggleResult.Enabled
		}

		serviceController.stop()
		preferences.updateAdaptiveThemeEnabled(false)
		return QuickSettingsToggleResult.Disabled
	}
}
