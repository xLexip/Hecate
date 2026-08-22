/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

private const val TAG = "LightSensorManager"

class LightSensorManager(context: Context) : SensorEventListener, SensorReader {

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
	private var callback: ((Float) -> Unit)? = null

	override fun startListening(
		callback: (Float) -> Unit,
		sensorDelay: Int
	): Boolean {
		val sensor = lightSensor ?: return false
		this.callback = callback
		val registered = sensorManager.registerListener(this, sensor, sensorDelay)
		if (!registered) {
			this.callback = null
		}
		return registered
	}

	override fun stopListening() {
		callback = null
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		val lightValue = event.values[0]
		callback?.invoke(lightValue)
		Log.d(TAG, "Light sensor value: $lightValue lx")
	}

	override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
		// No-op: accuracy changes are not relevant for current light sensor usage
	}

}
