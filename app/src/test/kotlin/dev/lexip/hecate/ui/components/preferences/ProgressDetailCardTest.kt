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

package dev.lexip.hecate.ui.components.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressDetailCardTest {
	@Test
	fun activeSegmentStartsAtFirstNonZeroThreshold() {
		val luxSteps = listOf(0f, 1f, 10f, 100f)

		assertEquals(-1, computeActiveSegmentIndex(luxSteps, 0f))
		assertEquals(0, computeActiveSegmentIndex(luxSteps, 1f))
	}
}
