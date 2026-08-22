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

import dev.lexip.hecate.util.ProximitySensorReader
import dev.lexip.hecate.util.ScreenOnProximityResult
import dev.lexip.hecate.util.SensorReader
import dev.lexip.hecate.util.ThemeSwitchSkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenOnCoordinatorTest {
	@Test
	fun noProximitySensorReadsLightAndAppliesThemeOnce() {
		val proximity = FakeProximitySensor(hasProximitySensor = false)
		val light = FakeSensor()
		var requestedTheme: Pair<Boolean, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light) { enabled, result ->
			requestedTheme = enabled to result
		}

		coordinator.onScreenOn()
		light.emit(25f)
		light.emit(10_000f)

		assertEquals(1, light.stopCount)
		assertEquals(true to ScreenOnProximityResult.SENSOR_UNAVAILABLE, requestedTheme)
	}

	@Test
	fun coveredDeviceWaitsForUncoveredEventBeforeApplyingTheme() {
		val proximity = FakeProximitySensor(maximumRange = 5f)
		val light = FakeSensor()
		val scheduler = FakeDelayedActionScheduler()
		var requestedTheme: Pair<Boolean, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light, scheduler) { enabled, result ->
			requestedTheme = enabled to result
		}

		coordinator.onScreenOn()
		proximity.emit(0f)
		scheduler.advanceBy(PROXIMITY_GRACE_PERIOD_MS - 1)
		proximity.emit(5f)
		light.emit(100f)

		assertEquals(1, proximity.stopCount)
		assertEquals(1, light.startCount)
		assertEquals(true to ScreenOnProximityResult.UNCOVERED_AFTER_WAIT, requestedTheme)
	}

	@Test
	fun coveredDeviceSkipsThemeAfterGracePeriod() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor()
		val scheduler = FakeDelayedActionScheduler()
		var skipped: Pair<ThemeSwitchSkipReason, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light, scheduler) { _, _ -> }

		coordinator.onScreenOn { reason, result -> skipped = reason to result }
		proximity.emit(0f)
		scheduler.advanceBy(PROXIMITY_GRACE_PERIOD_MS)

		assertEquals(1, proximity.stopCount)
		assertEquals(0, light.startCount)
		assertEquals(
			ThemeSwitchSkipReason.PROXIMITY_COVERED to
				ScreenOnProximityResult.COVERED_AFTER_GRACE,
			skipped
		)
	}

	@Test
	fun maximumRangeDefinesUncoveredState() {
		val proximity = FakeProximitySensor(maximumRange = 1f)
		val light = FakeSensor()
		var requestedResult: ScreenOnProximityResult? = null
		val coordinator = coordinator(proximity, light) { _, result ->
			requestedResult = result
		}

		coordinator.onScreenOn()
		proximity.emit(1f)
		light.emit(100f)

		assertEquals(ScreenOnProximityResult.UNCOVERED_INITIAL, requestedResult)
	}

	@Test
	fun proximityRegistrationFailureIsReported() {
		val proximity = FakeProximitySensor(registrationSucceeds = false)
		val light = FakeSensor()
		var skipped: Pair<ThemeSwitchSkipReason, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light) { _, _ -> }

		coordinator.onScreenOn { reason, result -> skipped = reason to result }

		assertEquals(
			ThemeSwitchSkipReason.PROXIMITY_REGISTRATION_FAILED to
				ScreenOnProximityResult.REGISTRATION_FAILED,
			skipped
		)
		assertEquals(0, light.startCount)
	}

	@Test
	fun missingInitialProximityReadingTimesOut() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor()
		val scheduler = FakeDelayedActionScheduler()
		var skipped: Pair<ThemeSwitchSkipReason, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light, scheduler) { _, _ -> }

		coordinator.onScreenOn { reason, result -> skipped = reason to result }
		scheduler.advanceBy(SENSOR_READING_TIMEOUT_MS)

		assertEquals(1, proximity.stopCount)
		assertEquals(
			ThemeSwitchSkipReason.PROXIMITY_READING_TIMEOUT to
				ScreenOnProximityResult.READING_TIMEOUT,
			skipped
		)
	}

	@Test
	fun lightRegistrationFailurePreservesProximityResult() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor(registrationSucceeds = false)
		var skipped: Pair<ThemeSwitchSkipReason, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light) { _, _ -> }

		coordinator.onScreenOn { reason, result -> skipped = reason to result }
		proximity.emit(proximity.maximumRange)

		assertEquals(
			ThemeSwitchSkipReason.LIGHT_REGISTRATION_FAILED to
				ScreenOnProximityResult.UNCOVERED_INITIAL,
			skipped
		)
	}

	@Test
	fun missingLightReadingTimesOut() {
		val proximity = FakeProximitySensor(hasProximitySensor = false)
		val light = FakeSensor()
		val scheduler = FakeDelayedActionScheduler()
		var skipped: Pair<ThemeSwitchSkipReason, ScreenOnProximityResult>? = null
		val coordinator = coordinator(proximity, light, scheduler) { _, _ -> }

		coordinator.onScreenOn { reason, result -> skipped = reason to result }
		scheduler.advanceBy(SENSOR_READING_TIMEOUT_MS)

		assertEquals(1, light.stopCount)
		assertEquals(
			ThemeSwitchSkipReason.LIGHT_READING_TIMEOUT to
				ScreenOnProximityResult.SENSOR_UNAVAILABLE,
			skipped
		)
	}

	@Test
	fun cancelStopsSensorsAndIgnoresLateCallbacks() {
		val proximity = FakeProximitySensor()
		val light = FakeSensor()
		var requestedTheme: Boolean? = null
		val coordinator = coordinator(proximity, light) { enabled, _ -> requestedTheme = enabled }

		coordinator.onScreenOn()
		proximity.emit(0f)
		coordinator.cancelPendingEvaluation()
		proximity.emitStale(proximity.maximumRange)

		assertEquals(1, proximity.stopCount)
		assertEquals(0, light.startCount)
		assertNull(requestedTheme)
	}

	private fun coordinator(
		proximity: FakeProximitySensor,
		light: FakeSensor,
		scheduler: FakeDelayedActionScheduler = FakeDelayedActionScheduler(),
		onThemeRequested: (Boolean, ScreenOnProximityResult) -> Unit
	) = ScreenOnCoordinator(
		proximitySensor = proximity,
		lightSensor = light,
		themeController = onThemeRequested,
		minuteProvider = { 12 * 60 },
		delayedActionScheduler = scheduler,
		adaptiveThemeThresholdLux = 100f,
		stayDarkAtNightEnabled = false,
		nightStartMinutes = 21 * 60,
		nightEndMinutes = 6 * 60
	)
}

private open class FakeSensor(
	private val registrationSucceeds: Boolean = true
) : SensorReader {
	private var callback: ((Float) -> Unit)? = null
	private var lastCallback: ((Float) -> Unit)? = null
	var startCount = 0
	var stopCount = 0

	override fun startListening(callback: (Float) -> Unit, sensorDelay: Int): Boolean {
		startCount += 1
		if (!registrationSucceeds) return false
		this.callback = callback
		lastCallback = callback
		return true
	}

	override fun stopListening() {
		stopCount += 1
		callback = null
	}

	fun emit(value: Float) {
		callback?.invoke(value)
	}

	fun emitStale(value: Float) {
		lastCallback?.invoke(value)
	}
}

private class FakeProximitySensor(
	override val hasProximitySensor: Boolean = true,
	override val maximumRange: Float = 5f,
	registrationSucceeds: Boolean = true
) : FakeSensor(registrationSucceeds), ProximitySensorReader

private class FakeDelayedActionScheduler : DelayedActionScheduler {
	private data class Task(
		val dueAt: Long,
		val action: () -> Unit,
		var cancelled: Boolean = false
	)

	private val tasks = mutableListOf<Task>()
	private var now = 0L

	override fun schedule(delayMillis: Long, action: () -> Unit): ScheduledAction {
		val task = Task(now + delayMillis, action)
		tasks += task
		return ScheduledAction { task.cancelled = true }
	}

	fun advanceBy(millis: Long) {
		val target = now + millis
		while (true) {
			val next = tasks
				.filter { !it.cancelled && it.dueAt <= target }
				.minByOrNull { it.dueAt }
				?: break
			tasks.remove(next)
			now = next.dueAt
			next.action()
		}
		now = target
	}
}
