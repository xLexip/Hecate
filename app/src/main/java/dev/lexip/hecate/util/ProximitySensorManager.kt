/*
 * Copyright (C) 2024 xLexip <https://lexip.dev>
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

private const val TAG = "ProximitySensorManager"

class ProximitySensorManager(private val context: Context) : SensorEventListener {

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
	private lateinit var callback: (Float) -> Unit

	fun startListening(callback: (Float) -> Unit) {
		this.callback = callback
		proximitySensor?.let {
			sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
		}
	}

	fun stopListening() {
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		val distance = event.values[0]
		this.callback.invoke(distance)
		Log.d(TAG, "Proximity sensor distance: $distance cm")
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
		return
	}

}