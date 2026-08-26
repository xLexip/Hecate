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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs the expensive WallpaperManager transaction outside the main thread.
 * Requests received while a change is pending are coalesced to the newest target.
 */
internal class WallpaperUpdateScheduler(
	private val scope: CoroutineScope,
	private val dispatcher: CoroutineDispatcher,
	private val applyWallpaper: (Boolean, String?, String?, Boolean) -> Boolean
) {
	private val lock = Any()
	private var pendingRequest: WallpaperRequest? = null
	private var activeRequest: WallpaperRequest? = null
	private var lastSuccessfulRequest: WallpaperRequest? = null
	private var worker: Job? = null

	fun schedule(
		isDark: Boolean,
		dayUri: String?,
		nightUri: String?,
		lockScreenWallpaperBlurEnabled: Boolean
	) {
		val request = WallpaperRequest(isDark, dayUri, nightUri, lockScreenWallpaperBlurEnabled)
		synchronized(lock) {
			if (request == pendingRequest ||
				request == activeRequest ||
				request == lastSuccessfulRequest
			) return
			pendingRequest = request
			if (worker?.isActive != true) {
				worker = scope.launch(dispatcher) { drainRequests() }
			}
		}
	}

	fun schedule(isDark: Boolean, dayUri: String?, nightUri: String?) =
		schedule(isDark, dayUri, nightUri, lockScreenWallpaperBlurEnabled = false)

	private suspend fun drainRequests() {
		while (true) {
			val request = synchronized(lock) {
				pendingRequest.also {
					pendingRequest = null
					activeRequest = it
				}
			} ?: return
			var succeeded = false
			var shouldStop = false
			try {
				succeeded = applyWallpaper(
					request.isDark,
					request.dayUri,
					request.nightUri,
					request.lockScreenWallpaperBlurEnabled
				)
			} finally {
				synchronized(lock) {
					activeRequest = null
					if (succeeded) lastSuccessfulRequest = request
					if (pendingRequest == null) {
						worker = null
						shouldStop = true
					}
				}
			}
			if (shouldStop) return
		}
	}
}

private data class WallpaperRequest(
	val isDark: Boolean,
	val dayUri: String?,
	val nightUri: String?,
	val lockScreenWallpaperBlurEnabled: Boolean
)
