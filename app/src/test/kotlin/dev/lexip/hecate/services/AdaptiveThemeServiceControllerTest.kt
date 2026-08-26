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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AdaptiveThemeServiceControllerTest {
	private val application = ApplicationProvider.getApplicationContext<Application>()

	@Test
	fun `quick settings start requests monitoring and immediate evaluation`() {
		AndroidAdaptiveThemeServiceController(application).start(
			enableMonitoring = true,
			evaluateImmediately = true
		)

		val intent = shadowOf(application).nextStartedService

		assertTrue(intent.getBooleanExtra(EXTRA_ENABLE_MONITORING, false))
		assertTrue(intent.getBooleanExtra(EXTRA_EVALUATE_IMMEDIATELY, false))
	}

	@Test
	fun `ordinary start does not request immediate evaluation`() {
		AndroidAdaptiveThemeServiceController(application).start()

		val intent = shadowOf(application).nextStartedService

		assertFalse(intent.getBooleanExtra(EXTRA_ENABLE_MONITORING, false))
		assertFalse(intent.getBooleanExtra(EXTRA_EVALUATE_IMMEDIATELY, false))
	}
}
