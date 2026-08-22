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

import android.hardware.SensorManager

interface SensorReader {
	fun startListening(
		callback: (Float) -> Unit,
		sensorDelay: Int = SensorManager.SENSOR_DELAY_FASTEST
	): Boolean

	fun stopListening()
}

interface ProximitySensorReader : SensorReader {
	val hasProximitySensor: Boolean
	val maximumRange: Float
}
