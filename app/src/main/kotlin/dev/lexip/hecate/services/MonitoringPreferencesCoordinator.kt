/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.services

import dev.lexip.hecate.broadcasts.ScreenOnReceiverSettings
import dev.lexip.hecate.data.UserPreferences

internal class MonitoringPreferencesCoordinator(
	private val configureWallpaperSync: (
		enabled: Boolean,
		dayWallpaperUri: String?,
		nightWallpaperUri: String?,
		lockScreenWallpaperBlurEnabled: Boolean
	) -> Unit
) {
	fun apply(
		preferences: UserPreferences,
		receiver: ScreenOnReceiverSettings?
	) {
		receiver?.adaptiveThemeThresholdLux = preferences.adaptiveThemeThresholdLux
		receiver?.stayDarkAtNightEnabled = preferences.stayDarkAtNightEnabled
		receiver?.nightStartMinutes = preferences.nightStartMinutes
		receiver?.nightEndMinutes = preferences.nightEndMinutes
		configureWallpaperSync(
			preferences.wallpaperSyncEnabled,
			preferences.dayWallpaperUri,
			preferences.nightWallpaperUri,
			preferences.lockScreenWallpaperBlurEnabled
		)
	}
}
