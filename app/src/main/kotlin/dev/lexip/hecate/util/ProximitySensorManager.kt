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

private const val TAG = "ProximitySensorManager"

class ProximitySensorManager(context: Context) : SensorEventListener, ProximitySensorReader {

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
	private var callback: ((Float) -> Unit)? = null

	override val hasProximitySensor: Boolean
		get() = proximitySensor != null
	override val maximumRange: Float
		get() = proximitySensor?.maximumRange ?: 0f

	override fun startListening(
		callback: (Float) -> Unit,
		sensorDelay: Int
	): Boolean {
		val sensor = proximitySensor
		if (sensor == null) {
			Log.w(
				TAG,
				"Proximity sensor not available on this device; startListening() will be a no-op."
			)
			return false
		}

		this.callback = callback
		Log.d(TAG, "Registering proximity sensor listener...")
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
		val distance = event.values[0]
		callback?.invoke(distance)
		Log.d(TAG, "Proximity sensor distance: $distance cm")
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
		// No-op: accuracy changes are not relevant for current proximity sensor usage
	}

}
