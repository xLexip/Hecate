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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperUpdateSchedulerTest {
	@Test
	fun `coalesces pending wallpaper updates to the latest target`() = runTest {
		val appliedRequests = mutableListOf<Pair<Boolean, Boolean>>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { isDark, _, _, lockScreenBlurEnabled ->
				appliedRequests += isDark to lockScreenBlurEnabled
				true
			}
		)

		scheduler.schedule(isDark = false, dayUri = "day-1", nightUri = "night-1")
		scheduler.schedule(
			isDark = true,
			dayUri = "day-2",
			nightUri = "night-2",
			lockScreenWallpaperBlurEnabled = true
		)
		advanceUntilIdle()

		assertEquals(listOf(true to true), appliedRequests)
	}

	@Test
	fun `starts a new worker after the previous update completed`() = runTest {
		val appliedThemes = mutableListOf<Boolean>()
		val scheduler = WallpaperUpdateScheduler(
			scope = this,
			dispatcher = StandardTestDispatcher(testScheduler),
			applyWallpaper = { isDark, _, _, _ ->
				appliedThemes += isDark
				true
			}
		)

		scheduler.schedule(isDark = false, dayUri = "day", nightUri = "night")
		advanceUntilIdle()
		scheduler.schedule(isDark = true, dayUri = "day", nightUri = "night")
		advanceUntilIdle()

		assertEquals(listOf(false, true), appliedThemes)
	}
}
