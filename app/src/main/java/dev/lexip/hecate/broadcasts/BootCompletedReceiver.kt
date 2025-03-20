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

package dev.lexip.hecate.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.lexip.hecate.services.BroadcastReceiverService

private const val TAG = "BootCompletedReceiver"

class BootCompletedReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
			Log.i(TAG, "Boot completed, starting broadcast receiver service...")
			val serviceIntent = Intent(context, BroadcastReceiverService::class.java)
			context.startService(serviceIntent)
		}
	}

}