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

enum class ScreenOnProximityResult(val analyticsValue: String) {
	SENSOR_UNAVAILABLE("sensor_unavailable"),
	UNCOVERED_INITIAL("uncovered_initial"),
	UNCOVERED_AFTER_WAIT("uncovered_after_wait"),
	COVERED_AFTER_GRACE("covered_after_grace"),
	REGISTRATION_FAILED("registration_failed"),
	READING_TIMEOUT("reading_timeout"),
	INVALID_READING("invalid_reading")
}

enum class ThemeSwitchSkipReason(val analyticsValue: String) {
	PROXIMITY_COVERED("proximity_covered"),
	PROXIMITY_REGISTRATION_FAILED("proximity_registration_failed"),
	PROXIMITY_READING_TIMEOUT("proximity_reading_timeout"),
	PROXIMITY_INVALID_READING("proximity_invalid_reading"),
	LIGHT_REGISTRATION_FAILED("light_registration_failed"),
	LIGHT_READING_TIMEOUT("light_reading_timeout"),
	LIGHT_INVALID_READING("light_invalid_reading")
}

/** Boundary around the privileged system-theme implementation. */
fun interface ThemeController {
	fun setDarkTheme(enabled: Boolean, screenOnProximityResult: ScreenOnProximityResult)
}

class DarkThemeController(
	private val handler: DarkThemeHandler
) : ThemeController {
	override fun setDarkTheme(
		enabled: Boolean,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		handler.setDarkTheme(enabled, screenOnProximityResult)
	}
}

class AdaptiveAppearanceController(
	private val handler: AdaptiveAppearanceHandler
) : ThemeController {
	override fun setDarkTheme(
		enabled: Boolean,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		handler.applyAppearance(enabled, screenOnProximityResult)
	}
}
