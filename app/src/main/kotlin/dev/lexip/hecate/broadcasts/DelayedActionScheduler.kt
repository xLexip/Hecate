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

import android.os.Handler
import android.os.Looper

internal fun interface ScheduledAction {
	fun cancel()
}

internal fun interface DelayedActionScheduler {
	fun schedule(delayMillis: Long, action: () -> Unit): ScheduledAction
}

internal class MainThreadDelayedActionScheduler(
	private val handler: Handler = Handler(Looper.getMainLooper())
) : DelayedActionScheduler {
	override fun schedule(delayMillis: Long, action: () -> Unit): ScheduledAction {
		val runnable = Runnable(action)
		handler.postDelayed(runnable, delayMillis)
		return ScheduledAction { handler.removeCallbacks(runnable) }
	}
}
