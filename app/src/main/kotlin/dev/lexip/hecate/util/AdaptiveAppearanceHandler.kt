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

package dev.lexip.hecate.util

import android.content.Context


data class DarkThemeChangeResult(
	val succeeded: Boolean,
	val changed: Boolean
)

private data class WallpaperSyncConfiguration(
	val enabled: Boolean = false,
	val dayWallpaperUri: String? = null,
	val nightWallpaperUri: String? = null
)

class AdaptiveAppearanceHandler internal constructor(
	private val setDarkTheme: (Boolean, ScreenOnProximityResult) -> DarkThemeChangeResult,
	private val scheduleWallpaperForTheme: (Boolean, String?, String?) -> Unit
) {
	@Volatile
	private var wallpaperSyncConfiguration = WallpaperSyncConfiguration()

	fun configureWallpaperSync(
		enabled: Boolean,
		dayWallpaperUri: String?,
		nightWallpaperUri: String?
	) {
		wallpaperSyncConfiguration = WallpaperSyncConfiguration(
			enabled = enabled,
			dayWallpaperUri = dayWallpaperUri,
			nightWallpaperUri = nightWallpaperUri
		)
	}

	fun applyAppearance(
		useDarkTheme: Boolean,
		screenOnProximityResult: ScreenOnProximityResult
	): DarkThemeChangeResult {
		val result = setDarkTheme(useDarkTheme, screenOnProximityResult)
		val wallpaperConfig = wallpaperSyncConfiguration

		if (result.succeeded && result.changed &&
			wallpaperConfig.enabled &&
			!wallpaperConfig.dayWallpaperUri.isNullOrEmpty() &&
			!wallpaperConfig.nightWallpaperUri.isNullOrEmpty()
		) {
			scheduleWallpaperForTheme(
				useDarkTheme,
				wallpaperConfig.dayWallpaperUri,
				wallpaperConfig.nightWallpaperUri
			)
		}

		return result
	}
}
