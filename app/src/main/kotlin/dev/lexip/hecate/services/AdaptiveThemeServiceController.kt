/*
 * Copyright (C) 2024-2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0
 *
 * Please see the License for specific terms regarding permissions and limitations.
 */

package dev.lexip.hecate.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

interface AdaptiveThemeServiceController {
	fun start(
		enableMonitoring: Boolean = false,
		evaluateImmediately: Boolean = false
	)
	fun stop()
}

class AndroidAdaptiveThemeServiceController(
	private val context: Context
) : AdaptiveThemeServiceController {

	override fun start(enableMonitoring: Boolean, evaluateImmediately: Boolean) {
		val intent = Intent(context, BroadcastReceiverService::class.java)
		if (enableMonitoring) {
			intent.putExtra(EXTRA_ENABLE_MONITORING, true)
		}
		if (evaluateImmediately) {
			intent.putExtra(EXTRA_EVALUATE_IMMEDIATELY, true)
		}
		ContextCompat.startForegroundService(context, intent)
	}

	override fun stop() {
		context.stopService(Intent(context, BroadcastReceiverService::class.java))
	}
}
