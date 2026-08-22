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

import dev.lexip.hecate.util.DelayedActionScheduler
import dev.lexip.hecate.util.MinuteProvider
import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.ScheduledAction
import dev.lexip.hecate.util.ScreenOnProximityResult
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.ThemeController
import dev.lexip.hecate.util.ThemeDecisionPolicy
import dev.lexip.hecate.util.ThemeSwitchSkipReason

internal const val PROXIMITY_GRACE_PERIOD_MS = 400L
internal const val SENSOR_READING_TIMEOUT_MS = 1_000L

private val NO_OP_SKIP_REPORTER: (ThemeSwitchSkipReason, ScreenOnProximityResult) -> Unit =
	{ _, _ -> }

internal class ScreenOnCoordinator(
	private val proximitySensor: ProximitySensorReader,
	private val lightSensor: SensorReader,
	private val themeController: ThemeController,
	private val minuteProvider: MinuteProvider,
	private val delayedActionScheduler: DelayedActionScheduler,
	var adaptiveThemeThresholdLux: Float,
	var stayDarkAtNightEnabled: Boolean,
	var nightStartMinutes: Int,
	var nightEndMinutes: Int
) {
	private var proximityReadingPending = false
	private var lightReadingPending = false
	private var nextCycleId = 0L
	private var activeCycleId: Long? = null
	private var proximityReadingCount = 0
	private var proximityInitialTimeout: ScheduledAction? = null
	private var proximityGraceTimeout: ScheduledAction? = null
	private var lightReadingTimeout: ScheduledAction? = null
	private var skipReporter = NO_OP_SKIP_REPORTER

	fun onScreenOn(
		onThemeSwitchSkipped: (
			reason: ThemeSwitchSkipReason,
			screenOnProximityResult: ScreenOnProximityResult
		) -> Unit = NO_OP_SKIP_REPORTER
	) {
		if (activeCycleId != null) return

		val cycleId = ++nextCycleId
		activeCycleId = cycleId
		skipReporter = onThemeSwitchSkipped

		if (!proximitySensor.hasProximitySensor) {
			readLightAndApplyTheme(cycleId, ScreenOnProximityResult.SENSOR_UNAVAILABLE)
			return
		}

		proximityReadingPending = true
		proximityReadingCount = 0
		val registered = proximitySensor.startListening(
			callback = { distance ->
				onProximityReading(cycleId, distance)
			}
		)
		if (!registered && isCycleActive(cycleId) && proximityReadingPending) {
			proximityReadingPending = false
			reportSkipped(
				cycleId,
				ThemeSwitchSkipReason.PROXIMITY_REGISTRATION_FAILED,
				ScreenOnProximityResult.REGISTRATION_FAILED
			)
			return
		}
		if (isCycleActive(cycleId) &&
			proximityReadingPending &&
			proximityReadingCount == 0
		) {
			proximityInitialTimeout = delayedActionScheduler.schedule(
				SENSOR_READING_TIMEOUT_MS
			) {
				if (!isCycleActive(cycleId) || !proximityReadingPending) return@schedule
				stopProximityListening()
				reportSkipped(
					cycleId,
					ThemeSwitchSkipReason.PROXIMITY_READING_TIMEOUT,
					ScreenOnProximityResult.READING_TIMEOUT
				)
			}
		}
	}

	fun cancelPendingEvaluation() {
		activeCycleId = null
		cancelTimeouts()
		if (proximityReadingPending) proximitySensor.stopListening()
		if (lightReadingPending) lightSensor.stopListening()
		proximityReadingPending = false
		lightReadingPending = false
		proximityReadingCount = 0
		skipReporter = NO_OP_SKIP_REPORTER
	}

	private fun onProximityReading(cycleId: Long, distance: Float) {
		if (!isCycleActive(cycleId) || !proximityReadingPending) return
		proximityInitialTimeout?.cancel()
		proximityInitialTimeout = null

		if (!distance.isFinite() ||
			!proximitySensor.maximumRange.isFinite() ||
			proximitySensor.maximumRange <= 0f
		) {
			stopProximityListening()
			reportSkipped(
				cycleId,
				ThemeSwitchSkipReason.PROXIMITY_INVALID_READING,
				ScreenOnProximityResult.INVALID_READING
			)
			return
		}

		proximityReadingCount += 1
		val isCovered = distance < proximitySensor.maximumRange
		if (!isCovered) {
			val result = if (proximityReadingCount == 1) {
				ScreenOnProximityResult.UNCOVERED_INITIAL
			} else {
				ScreenOnProximityResult.UNCOVERED_AFTER_WAIT
			}
			stopProximityListening()
			readLightAndApplyTheme(cycleId, result)
			return
		}

		if (proximityReadingCount == 1) {
			proximityGraceTimeout = delayedActionScheduler.schedule(PROXIMITY_GRACE_PERIOD_MS) {
				if (!isCycleActive(cycleId) || !proximityReadingPending) return@schedule
				stopProximityListening()
				reportSkipped(
					cycleId,
					ThemeSwitchSkipReason.PROXIMITY_COVERED,
					ScreenOnProximityResult.COVERED_AFTER_GRACE
				)
			}
		}
	}

	private fun readLightAndApplyTheme(
		cycleId: Long,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		if (!isCycleActive(cycleId) || lightReadingPending) return
		lightReadingPending = true
		val registered = lightSensor.startListening(
			callback = { lightValue ->
				if (!isCycleActive(cycleId) || !lightReadingPending) return@startListening
				stopLightListening()
				if (!lightValue.isFinite()) {
					reportSkipped(
						cycleId,
						ThemeSwitchSkipReason.LIGHT_INVALID_READING,
						screenOnProximityResult
					)
					return@startListening
				}
				try {
					themeController.setDarkTheme(
						ThemeDecisionPolicy.shouldUseDarkTheme(
							lightValue = lightValue,
							thresholdLux = adaptiveThemeThresholdLux,
							stayDarkAtNightEnabled = stayDarkAtNightEnabled,
							nightStartMinutes = nightStartMinutes,
							nightEndMinutes = nightEndMinutes,
							nowMinutes = minuteProvider.currentMinutes()
						),
						screenOnProximityResult
					)
				} finally {
					finishCycle(cycleId)
				}
			}
		)
		if (!registered && isCycleActive(cycleId) && lightReadingPending) {
			lightReadingPending = false
			reportSkipped(
				cycleId,
				ThemeSwitchSkipReason.LIGHT_REGISTRATION_FAILED,
				screenOnProximityResult
			)
			return
		}
		if (isCycleActive(cycleId) && lightReadingPending) {
			lightReadingTimeout = delayedActionScheduler.schedule(SENSOR_READING_TIMEOUT_MS) {
				if (!isCycleActive(cycleId) || !lightReadingPending) return@schedule
				stopLightListening()
				reportSkipped(
					cycleId,
					ThemeSwitchSkipReason.LIGHT_READING_TIMEOUT,
					screenOnProximityResult
				)
			}
		}
	}

	private fun stopProximityListening() {
		proximityInitialTimeout?.cancel()
		proximityInitialTimeout = null
		proximityGraceTimeout?.cancel()
		proximityGraceTimeout = null
		if (proximityReadingPending) proximitySensor.stopListening()
		proximityReadingPending = false
	}

	private fun stopLightListening() {
		lightReadingTimeout?.cancel()
		lightReadingTimeout = null
		if (lightReadingPending) lightSensor.stopListening()
		lightReadingPending = false
	}

	private fun reportSkipped(
		cycleId: Long,
		reason: ThemeSwitchSkipReason,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		if (!isCycleActive(cycleId)) return
		val reporter = skipReporter
		finishCycle(cycleId)
		reporter(reason, screenOnProximityResult)
	}

	private fun finishCycle(cycleId: Long) {
		if (!isCycleActive(cycleId)) return
		cancelTimeouts()
		activeCycleId = null
		proximityReadingCount = 0
		skipReporter = NO_OP_SKIP_REPORTER
	}

	private fun cancelTimeouts() {
		proximityInitialTimeout?.cancel()
		proximityInitialTimeout = null
		proximityGraceTimeout?.cancel()
		proximityGraceTimeout = null
		lightReadingTimeout?.cancel()
		lightReadingTimeout = null
	}

	private fun isCycleActive(cycleId: Long): Boolean = activeCycleId == cycleId
}
