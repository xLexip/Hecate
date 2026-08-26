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

package dev.lexip.hecate.services

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightModeChangeTrackerTest {
	@Test
	fun `reports only effective light and dark mode changes`() {
		val tracker = NightModeChangeTracker()
		tracker.initialize(uiMode(Configuration.UI_MODE_NIGHT_NO))

		assertNull(
			tracker.onConfigurationChanged(
				Configuration.UI_MODE_TYPE_CAR or Configuration.UI_MODE_NIGHT_NO
			)
		)
		assertEquals(true, tracker.onConfigurationChanged(uiMode(Configuration.UI_MODE_NIGHT_YES)))
		assertNull(tracker.onConfigurationChanged(uiMode(Configuration.UI_MODE_NIGHT_YES)))
		assertEquals(false, tracker.onConfigurationChanged(uiMode(Configuration.UI_MODE_NIGHT_NO)))
	}

	@Test
	fun `first configuration establishes a baseline without reporting a change`() {
		val tracker = NightModeChangeTracker()

		assertNull(tracker.onConfigurationChanged(uiMode(Configuration.UI_MODE_NIGHT_YES)))
	}

	@Test
	fun `reinitialization does not replace the existing baseline`() {
		val tracker = NightModeChangeTracker()
		tracker.initialize(uiMode(Configuration.UI_MODE_NIGHT_NO))
		tracker.initialize(uiMode(Configuration.UI_MODE_NIGHT_YES))

		assertEquals(true, tracker.onConfigurationChanged(uiMode(Configuration.UI_MODE_NIGHT_YES)))
	}

	private fun uiMode(nightMode: Int): Int =
		Configuration.UI_MODE_TYPE_NORMAL or nightMode
}
