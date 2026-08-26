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

package dev.lexip.hecate.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.lexip.hecate.logging.Logger
import dev.lexip.hecate.util.AdaptiveAppearanceController
import dev.lexip.hecate.util.AdaptiveAppearanceHandler
import dev.lexip.hecate.util.DelayedActionScheduler
import dev.lexip.hecate.util.LightSensorManager
import dev.lexip.hecate.util.MainThreadDelayedActionScheduler
import dev.lexip.hecate.util.MinuteProvider
import dev.lexip.hecate.util.ProximitySensorManager
import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.SystemMinuteProvider
import dev.lexip.hecate.util.ThemeController

internal interface ScreenOnReceiverSettings {
	var adaptiveThemeThresholdLux: Float
	var stayDarkAtNightEnabled: Boolean
	var nightStartMinutes: Int
	var nightEndMinutes: Int
}

/**
 * Adaptive theme switching logic. Executes when the screen is turned on.
 * The theme is switched based on the environment brightness and proximity sensor values.
 */
class ScreenOnReceiver internal constructor(
	proximitySensorManager: ProximitySensorReader,
	lightSensorManager: SensorReader,
	themeController: ThemeController,
	adaptiveThemeThresholdLux: Float,
	stayDarkAtNightEnabled: Boolean,
	nightStartMinutes: Int,
	nightEndMinutes: Int,
	minuteProvider: MinuteProvider = SystemMinuteProvider,
	delayedActionScheduler: DelayedActionScheduler = MainThreadDelayedActionScheduler()
) : BroadcastReceiver(), ScreenOnReceiverSettings {

	constructor(
		proximitySensorManager: ProximitySensorManager,
		lightSensorManager: LightSensorManager,
		adaptiveAppearanceHandler: AdaptiveAppearanceHandler,
		adaptiveThemeThresholdLux: Float,
		stayDarkAtNightEnabled: Boolean,
		nightStartMinutes: Int,
		nightEndMinutes: Int
	) : this(
		proximitySensorManager = proximitySensorManager,
		lightSensorManager = lightSensorManager,
		themeController = AdaptiveAppearanceController(adaptiveAppearanceHandler),
		adaptiveThemeThresholdLux = adaptiveThemeThresholdLux,
		stayDarkAtNightEnabled = stayDarkAtNightEnabled,
		nightStartMinutes = nightStartMinutes,
		nightEndMinutes = nightEndMinutes
	)

	private val coordinator = ScreenOnCoordinator(
		proximitySensor = proximitySensorManager,
		lightSensor = lightSensorManager,
		themeController = themeController,
		minuteProvider = minuteProvider,
		delayedActionScheduler = delayedActionScheduler,
		adaptiveThemeThresholdLux = adaptiveThemeThresholdLux,
		stayDarkAtNightEnabled = stayDarkAtNightEnabled,
		nightStartMinutes = nightStartMinutes,
		nightEndMinutes = nightEndMinutes
	)

	override var adaptiveThemeThresholdLux: Float
		get() = coordinator.adaptiveThemeThresholdLux
		set(value) {
			coordinator.adaptiveThemeThresholdLux = value
		}

	override var stayDarkAtNightEnabled: Boolean
		get() = coordinator.stayDarkAtNightEnabled
		set(value) {
			coordinator.stayDarkAtNightEnabled = value
		}

	override var nightStartMinutes: Int
		get() = coordinator.nightStartMinutes
		set(value) {
			coordinator.nightStartMinutes = value
		}

	override var nightEndMinutes: Int
		get() = coordinator.nightEndMinutes
		set(value) {
			coordinator.nightEndMinutes = value
		}

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_SCREEN_ON) {
			evaluateNow(context.applicationContext)
		}
	}

	fun evaluateNow(
		context: Context,
		syncWallpaperWhenThemeUnchanged: Boolean = false
	) {
		coordinator.onScreenOn(syncWallpaperWhenThemeUnchanged) { reason, screenOnProximityResult ->
			Logger.logThemeSwitchSkipped(
				context = context.applicationContext,
				reason = reason,
				screenOnProximityResult = screenOnProximityResult
			)
		}
	}

	fun cancelPendingEvaluation() {
		coordinator.cancelPendingEvaluation()
	}
}
