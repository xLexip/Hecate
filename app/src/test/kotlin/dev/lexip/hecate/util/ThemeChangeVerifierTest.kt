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

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeChangeVerifierTest {
	@Test
	fun `completes after the target configuration is observed`() {
		val scheduler = FakeVerificationScheduler()
		var effectiveUiMode = lightUiMode()
		var result: ThemeVerificationResult? = null
		val verifier = ThemeChangeVerifier(scheduler) { effectiveUiMode }

		assertTrue(verifier.start(expectedDark = true) { result = it })
		scheduler.advanceBy(THEME_VERIFICATION_INTERVAL_MS - 1)
		assertNull(result)

		effectiveUiMode = darkUiMode()
		scheduler.advanceBy(1)

		assertEquals(
			ThemeVerificationResult(
				succeeded = true,
				attempt = 1,
				effectiveUiMode = darkUiMode()
			),
			result
		)
		assertFalse(verifier.isVerificationInProgress)
	}

	@Test
	fun `times out after the maximum verification window`() {
		val scheduler = FakeVerificationScheduler()
		var result: ThemeVerificationResult? = null
		val verifier = ThemeChangeVerifier(scheduler) { lightUiMode() }

		verifier.start(expectedDark = true) { result = it }
		scheduler.advanceBy(
			THEME_VERIFICATION_INTERVAL_MS * THEME_VERIFICATION_MAX_ATTEMPTS
		)

		assertEquals(false, result?.succeeded)
		assertEquals(THEME_VERIFICATION_MAX_ATTEMPTS, result?.attempt)
		assertEquals(5_000L, result?.elapsedMillis)
		assertFalse(verifier.isVerificationInProgress)
	}

	@Test
	fun `cancel ignores a pending and stale verification callback`() {
		val scheduler = FakeVerificationScheduler()
		var callbackCount = 0
		val verifier = ThemeChangeVerifier(scheduler) { darkUiMode() }

		verifier.start(expectedDark = true) { callbackCount += 1 }
		verifier.cancel()
		scheduler.runCancelledTasks()

		assertEquals(0, callbackCount)
		assertFalse(verifier.isVerificationInProgress)
	}

	@Test
	fun `rejects a parallel verification`() {
		val scheduler = FakeVerificationScheduler()
		val verifier = ThemeChangeVerifier(scheduler) { darkUiMode() }

		assertTrue(verifier.start(expectedDark = true) { })
		assertFalse(verifier.start(expectedDark = false) { })
		assertEquals(1, scheduler.activeTaskCount)
	}

	private fun darkUiMode(): Int =
		Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES

	private fun lightUiMode(): Int =
		Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO
}

private class FakeVerificationScheduler : DelayedActionScheduler {
	private data class Task(
		val dueAt: Long,
		val action: () -> Unit,
		var cancelled: Boolean = false
	)

	private val tasks = mutableListOf<Task>()
	private var now = 0L

	val activeTaskCount: Int
		get() = tasks.count { !it.cancelled }

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

	fun runCancelledTasks() {
		val cancelledTasks = tasks.filter { it.cancelled }
		tasks.removeAll(cancelledTasks)
		cancelledTasks.forEach { it.action() }
	}
}
