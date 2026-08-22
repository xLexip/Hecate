/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_WALLPAPER_URI = "content://wallpaper/day"
private const val NIGHT_WALLPAPER_URI = "content://wallpaper/night"

class AdaptiveAppearanceHandlerTest {
	@Test
	fun `applies matching wallpaper after successful theme transition`() {
		var appliedTheme: Boolean? = null
		var appliedDayUri: String? = null
		var appliedNightUri: String? = null
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { isDark, dayUri, nightUri ->
				appliedTheme = isDark
				appliedDayUri = dayUri
				appliedNightUri = nightUri
			}
		)
		handler.configureWallpaperSync(
			enabled = true,
			dayWallpaperUri = DAY_WALLPAPER_URI,
			nightWallpaperUri = NIGHT_WALLPAPER_URI
		)

		val result = applyAndCaptureResult(
			handler = handler,
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertTrue(result.succeeded)
		assertTrue(result.changed)
		assertEquals(true, appliedTheme)
		assertEquals(DAY_WALLPAPER_URI, appliedDayUri)
		assertEquals(NIGHT_WALLPAPER_URI, appliedNightUri)
	}

	@Test
	fun `does not apply wallpaper when theme is already correct`() {
		var wallpaperApplied = false
		val handler = createHandler(
			themeResult = DarkThemeChangeResult(succeeded = true, changed = false),
			onWallpaperApplied = { wallpaperApplied = true }
		)

		val result = applyAndCaptureResult(
			handler = handler,
			useDarkTheme = false,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertTrue(result.succeeded)
		assertFalse(result.changed)
		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when theme change fails`() {
		var wallpaperApplied = false
		val handler = createHandler(
			themeResult = DarkThemeChangeResult(succeeded = false, changed = false),
			onWallpaperApplied = { wallpaperApplied = true }
		)

		val result = applyAndCaptureResult(
			handler = handler,
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertFalse(result.succeeded)
		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when sync is disabled`() {
		var wallpaperApplied = false
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { _, _, _ ->
				wallpaperApplied = true
			}
		)
		handler.configureWallpaperSync(
			enabled = false,
			dayWallpaperUri = DAY_WALLPAPER_URI,
			nightWallpaperUri = NIGHT_WALLPAPER_URI
		)

		handler.applyAppearance(
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertFalse(wallpaperApplied)
	}

	@Test
	fun `does not apply wallpaper when sync configuration is incomplete`() {
		var wallpaperApplied = false
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { _, _, _ ->
				wallpaperApplied = true
			}
		)
		handler.configureWallpaperSync(
			enabled = true,
			dayWallpaperUri = DAY_WALLPAPER_URI,
			nightWallpaperUri = null
		)

		handler.applyAppearance(
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertFalse(wallpaperApplied)
	}

	@Test
	fun `applies day wallpaper for a light theme transition`() {
		var appliedTheme: Boolean? = null
		val lightHandler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { isDark, _, _ ->
				appliedTheme = isDark
			}
		)
		lightHandler.configureWallpaperSync(
			enabled = true,
			dayWallpaperUri = DAY_WALLPAPER_URI,
			nightWallpaperUri = NIGHT_WALLPAPER_URI
		)

		lightHandler.applyAppearance(
			useDarkTheme = false,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertEquals(false, appliedTheme)
	}

	@Test
	fun `uses the latest wallpaper configuration`() {
		var appliedUris: Pair<String?, String?>? = null
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { _, dayUri, nightUri ->
				appliedUris = dayUri to nightUri
			}
		)
		handler.configureWallpaperSync(true, "content://old/day", "content://old/night")
		handler.configureWallpaperSync(true, "content://new/day", "content://new/night")

		handler.applyAppearance(
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertEquals(
			"content://new/day" to "content://new/night",
			appliedUris
		)
	}

	@Test
	fun `scheduling wallpaper does not change successful theme result`() {
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete ->
				onComplete(DarkThemeChangeResult(succeeded = true, changed = true))
			},
			scheduleWallpaperForTheme = { _, _, _ -> }
		)
		handler.configureWallpaperSync(true, "content://day", "content://night")

		val result = applyAndCaptureResult(
			handler = handler,
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_INITIAL
		)

		assertTrue(result.succeeded)
		assertTrue(result.changed)
	}

	@Test
	fun `waits for verified theme completion before applying wallpaper`() {
		var completeThemeChange: ((DarkThemeChangeResult) -> Unit)? = null
		var wallpaperApplied = false
		val handler = AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete -> completeThemeChange = onComplete },
			scheduleWallpaperForTheme = { _, _, _ -> wallpaperApplied = true }
		)
		handler.configureWallpaperSync(true, DAY_WALLPAPER_URI, NIGHT_WALLPAPER_URI)

		handler.applyAppearance(
			useDarkTheme = true,
			screenOnProximityResult = ScreenOnProximityResult.UNCOVERED_AFTER_WAIT
		)

		assertFalse(wallpaperApplied)
		completeThemeChange?.invoke(DarkThemeChangeResult(succeeded = true, changed = true))
		assertTrue(wallpaperApplied)
	}

	private fun createHandler(
		themeResult: DarkThemeChangeResult,
		onWallpaperApplied: () -> Unit
	): AdaptiveAppearanceHandler {
		return AdaptiveAppearanceHandler(
			setDarkTheme = { _, _, onComplete -> onComplete(themeResult) },
			scheduleWallpaperForTheme = { _, _, _ ->
				onWallpaperApplied()
			}
		).apply {
			configureWallpaperSync(
				enabled = true,
				dayWallpaperUri = DAY_WALLPAPER_URI,
				nightWallpaperUri = NIGHT_WALLPAPER_URI
			)
		}
	}

	private fun applyAndCaptureResult(
		handler: AdaptiveAppearanceHandler,
		useDarkTheme: Boolean,
		screenOnProximityResult: ScreenOnProximityResult
	): DarkThemeChangeResult {
		var capturedResult: DarkThemeChangeResult? = null
		handler.applyAppearance(
			useDarkTheme = useDarkTheme,
			screenOnProximityResult = screenOnProximityResult,
			onComplete = { capturedResult = it }
		)
		return requireNotNull(capturedResult)
	}
}
