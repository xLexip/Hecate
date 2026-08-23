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

package dev.lexip.hecate.broadcasts

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dev.lexip.hecate.FakeProximitySensorReader
import dev.lexip.hecate.FakeSensorReader
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37])
class ScreenOnReceiverTest {

	@Test
	fun screenOnBroadcastReadsLightAndAppliesAppearance() {
		val proximity = FakeProximitySensorReader(hasProximitySensor = false)
		val light = FakeSensorReader()
		var requestedTheme: Boolean? = null
		val receiver = ScreenOnReceiver(
			proximitySensorManager = proximity,
			lightSensorManager = light,
			themeController = { enabled, _ -> requestedTheme = enabled },
			adaptiveThemeThresholdLux = 100f,
			stayDarkAtNightEnabled = false,
			nightStartMinutes = 21 * 60,
			nightEndMinutes = 6 * 60,
			minuteProvider = { 12 * 60 }
		)

		receiver.onReceive(
			ApplicationProvider.getApplicationContext(),
			Intent(Intent.ACTION_SCREEN_ON)
		)
		light.emit(25f)

		assertEquals(1, light.startCalls)
		assertEquals(1, light.stopCalls)
		assertEquals(true, requestedTheme)
	}

	@Test
	fun unrelatedBroadcastIsIgnored() {
		val proximity = FakeProximitySensorReader()
		val light = FakeSensorReader()
		var themeCalls = 0
		val receiver = ScreenOnReceiver(
			proximitySensorManager = proximity,
			lightSensorManager = light,
			themeController = { _, _ -> themeCalls++ },
			adaptiveThemeThresholdLux = 100f,
			stayDarkAtNightEnabled = false,
			nightStartMinutes = 21 * 60,
			nightEndMinutes = 6 * 60,
			minuteProvider = { 12 * 60 }
		)

		receiver.onReceive(
			ApplicationProvider.getApplicationContext(),
			Intent(Intent.ACTION_BATTERY_CHANGED)
		)

		assertEquals(0, proximity.startCalls)
		assertEquals(0, light.startCalls)
		assertEquals(0, themeCalls)
	}
}
