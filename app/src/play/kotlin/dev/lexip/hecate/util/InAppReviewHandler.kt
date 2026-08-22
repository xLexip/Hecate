/*
 * Copyright (C) 2025 xLexip <https://lexip.dev>
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

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import dev.lexip.hecate.BuildConfig
import dev.lexip.hecate.R
import dev.lexip.hecate.logging.Logger

object InAppReviewHandler {

	private const val TAG = "InAppReviewHandler"

	fun triggerReview(activity: Activity, onLaunchStarted: () -> Unit) {

		val manager = if (BuildConfig.DEBUG) {
			FakeReviewManager(activity)
		} else {
			ReviewManagerFactory.create(activity)
		}

		val request = manager.requestReviewFlow()
		request.addOnCompleteListener { task ->
			if (task.isSuccessful) {
				val reviewInfo = task.result
				val flow = manager.launchReviewFlow(activity, reviewInfo)
				onLaunchStarted()
				flow.addOnCompleteListener { flowTask ->
					if (BuildConfig.DEBUG) {
						Toast.makeText(
							activity,
							R.string.debug_review_flow_completed,
							Toast.LENGTH_SHORT
						)
							.show()
					}
					if (flowTask.isSuccessful) {
						Logger.logInAppReviewFlowCompleted(activity)
					} else {
						val exception = flowTask.exception
						Log.e(TAG, "Review flow failed", exception)
						if (exception != null) Logger.logException(exception)
					}
				}
			} else {
				val exception = task.exception
				Log.e(TAG, "Review manager request failed", exception)
				if (exception != null) Logger.logException(exception)
			}
		}
	}
}
