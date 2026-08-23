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

internal const val THEME_VERIFICATION_INTERVAL_MS = 250L
internal const val THEME_VERIFICATION_MAX_ATTEMPTS = 20

internal data class ThemeVerificationResult(
	val succeeded: Boolean,
	val attempt: Int,
	val effectiveUiMode: Int
) {
	val elapsedMillis: Long
		get() = attempt * THEME_VERIFICATION_INTERVAL_MS
}

internal class ThemeChangeVerifier(
	private val delayedActionScheduler: DelayedActionScheduler,
	private val effectiveUiModeProvider: () -> Int
) {
	private var nextGeneration = 0L
	private var activeGeneration: Long? = null
	private var pendingAction: ScheduledAction? = null

	val isVerificationInProgress: Boolean
		@Synchronized get() = activeGeneration != null

	@Synchronized
	fun start(
		expectedDark: Boolean,
		onComplete: (ThemeVerificationResult) -> Unit
	): Boolean {
		if (activeGeneration != null) return false

		val generation = ++nextGeneration
		activeGeneration = generation
		scheduleAttempt(generation, expectedDark, attempt = 1, onComplete)
		return true
	}

	@Synchronized
	fun cancel() {
		nextGeneration += 1
		activeGeneration = null
		pendingAction?.cancel()
		pendingAction = null
	}

	private fun scheduleAttempt(
		generation: Long,
		expectedDark: Boolean,
		attempt: Int,
		onComplete: (ThemeVerificationResult) -> Unit
	) {
		pendingAction = delayedActionScheduler.schedule(THEME_VERIFICATION_INTERVAL_MS) {
			val effectiveUiMode = synchronized(this) {
				if (activeGeneration != generation) return@schedule
				effectiveUiModeProvider()
			}
			val succeeded = doesNightConfigurationMatchTarget(effectiveUiMode, expectedDark)
			if (!succeeded && attempt < THEME_VERIFICATION_MAX_ATTEMPTS) {
				synchronized(this) {
					if (activeGeneration != generation) return@synchronized
					scheduleAttempt(generation, expectedDark, attempt + 1, onComplete)
				}
				return@schedule
			}

			val shouldComplete = synchronized(this) {
				if (activeGeneration != generation) {
					false
				} else {
					activeGeneration = null
					pendingAction = null
					true
				}
			}
			if (shouldComplete) {
				onComplete(
					ThemeVerificationResult(
						succeeded = succeeded,
						attempt = attempt,
						effectiveUiMode = effectiveUiMode
					)
				)
			}
		}
	}
}
