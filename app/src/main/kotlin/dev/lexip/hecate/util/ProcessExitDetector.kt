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

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import dev.lexip.hecate.logging.Logger

private const val TAG = "ProcessExitDetector"
private const val PREFS_NAME = "process_exit_tracker"
private const val KEY_LAST_LOGGED_TIMESTAMP = "last_logged_timestamp"

object ProcessExitDetector {

    fun checkAndLogProcessExitReasons(context: Context) {
        try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    ?: return

            val exitReasons = activityManager.getHistoricalProcessExitReasons(
                context.packageName,
                0, // 0 gets all recent PIDs
                10 // max results
            ).toList()

            if (exitReasons.isEmpty()) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastLoggedTimestamp = prefs.getLong(KEY_LAST_LOGGED_TIMESTAMP, 0L)

            // Exit reasons are sorted most recent first
            val newExitReasons = exitReasons.filter { it.timestamp > lastLoggedTimestamp }
            if (newExitReasons.isEmpty()) return

            // Update stored timestamp
            val newestTimestamp = newExitReasons.maxOf { it.timestamp }
            prefs.edit { putLong(KEY_LAST_LOGGED_TIMESTAMP, newestTimestamp) }

            for (exitInfo in newExitReasons) {
                processExitInfo(context, exitInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect historical process exit reasons", e)
        }
    }

    private fun processExitInfo(context: Context, exitInfo: ApplicationExitInfo) {
        val reason = exitInfo.reason
        val reasonName = exitReasonToString(reason)
        val description = exitInfo.description ?: "None"
        val importanceName = importanceToString(exitInfo.importance)
        val pssMb = exitInfo.pss / 1024L

        Log.i(
            TAG,
            "Historical Process Exit: reason=$reasonName ($reason), importance=$importanceName, " +
                    "pss=${pssMb}MB, pid=${exitInfo.pid}, timestamp=${exitInfo.timestamp}, description=\"$description\""
        )

        if (isUnexpectedKill(reason)) {
            Log.w(
                TAG,
                "Unexpected process kill detected! Reason: $reasonName, Importance: $importanceName, Desc: $description"
            )
            Logger.logProcessExit(context, reasonName, description, pssMb, importanceName)
        }
    }

    private fun isUnexpectedKill(reason: Int): Boolean {
        return when (reason) {
            ApplicationExitInfo.REASON_LOW_MEMORY,
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
            ApplicationExitInfo.REASON_SIGNALED,
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
            ApplicationExitInfo.REASON_DEPENDENCY_DIED,
            ApplicationExitInfo.REASON_FREEZER,
            ApplicationExitInfo.REASON_UNKNOWN,
            ApplicationExitInfo.REASON_OTHER -> true

            else -> false
        }
    }

    private fun exitReasonToString(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
            ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
            ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
            ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "PACKAGE_STATE_CHANGE"
            ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "PACKAGE_UPDATED"
            ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
            ApplicationExitInfo.REASON_OTHER -> "OTHER"
            else -> "UNKNOWN_$reason"
        }
    }

    private fun importanceToString(importance: Int): String {
        return when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> "TOP_SLEEPING"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERCEPTIBLE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "CANT_SAVE_STATE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "GONE"
            else -> "IMPORTANCE_$importance"
        }
    }
}
