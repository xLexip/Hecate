/*
 * Copyright (C) 2024-2025 xLexip <https://lexip.dev>
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

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.InputStream
import java.lang.ref.WeakReference

import dev.lexip.hecate.logging.Logger

private const val TAG = "WallpaperHandler"

internal interface WallpaperPlatform {
	fun isLiveWallpaperActive(): Boolean
	fun takePersistableReadPermission(uri: Uri)
	fun applyWallpaperForTheme(
		isDark: Boolean,
		dayUriStr: String?,
		nightUriStr: String?,
		lockScreenWallpaperBlurEnabled: Boolean
	): Boolean
}

/**
 * Handler for theme-synchronized wallpaper swapping.
 */
internal class WallpaperHandler internal constructor(
	context: Context,
	private val liveWallpaperActive: () -> Boolean,
	private val persistReadPermission: (Uri) -> Unit,
	private val openInputStream: (Uri) -> InputStream?,
	private val setStream: (InputStream, Int) -> Unit
) : WallpaperPlatform {
	constructor(context: Context) : this(
		context = context,
		liveWallpaperActive = {
			WallpaperManager.getInstance(context.applicationContext).wallpaperInfo != null
		},
		persistReadPermission = { uri ->
			context.applicationContext.contentResolver.takePersistableUriPermission(
				uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION
			)
		},
		openInputStream = context.applicationContext.contentResolver::openInputStream,
		setStream = { stream, flags ->
			WallpaperManager.getInstance(context.applicationContext).setStream(
				stream,
				null,
				true,
				flags
			)
		}
	)

	private val contextRef = WeakReference(context.applicationContext)

	/**
	 * Checks whether a live wallpaper is currently active.
	 */
	override fun isLiveWallpaperActive(): Boolean {
		val isLive = liveWallpaperActive()
		Log.d(TAG, "Live wallpaper active: $isLive")
		return isLive
	}

	override fun takePersistableReadPermission(uri: Uri) {
		persistReadPermission(uri)
	}

	/**
	 * Applies the corresponding theme wallpaper based on dark theme status.
	 * @param isDark True if target theme is dark, false for light.
	 * @param dayUriStr Content URI string for day wallpaper.
	 * @param nightUriStr Content URI string for night wallpaper.
	 * @param lockScreenWallpaperBlurEnabled Whether to use a pre-rendered blurred lock variant.
	 * @return true if wallpaper was successfully applied, false otherwise.
	 */
	override fun applyWallpaperForTheme(
		isDark: Boolean,
		dayUriStr: String?,
		nightUriStr: String?,
		lockScreenWallpaperBlurEnabled: Boolean
	): Boolean {
		val context = contextRef.get() ?: return false

		val targetUriStr = if (isDark) nightUriStr else dayUriStr
		if (targetUriStr.isNullOrEmpty()) {
			Log.w(TAG, "Target wallpaper URI is missing (isDark=$isDark). Skipping wallpaper swap.")
			return false
		}

		val succeeded = if (lockScreenWallpaperBlurEnabled) {
			val slot = if (isDark) WallpaperSlot.NIGHT else WallpaperSlot.DAY
			val systemSucceeded = setWallpaper(
				targetUriStr,
				WallpaperManager.FLAG_SYSTEM
			)
			val blurredLockSucceeded = systemSucceeded && setWallpaper(
				blurredLockScreenWallpaperUri(context, slot).toString(),
				WallpaperManager.FLAG_LOCK
			)
			val lockSucceeded = if (systemSucceeded && !blurredLockSucceeded) {
				Log.w(TAG, "Blurred lock wallpaper unavailable; applying the sharp wallpaper to the lock screen.")
				setWallpaper(targetUriStr, WallpaperManager.FLAG_LOCK)
			} else {
				blurredLockSucceeded
			}
			systemSucceeded && lockSucceeded
		} else {
			setWallpaper(
				targetUriStr,
				WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
			)
		}

		if (succeeded) {
			Log.i(TAG, "Successfully set ${if (isDark) "night" else "day"} wallpaper from URI.")
		}
		Logger.logWallpaperSwitched(context, isDark = isDark, succeeded = succeeded)
		return succeeded
	}

	internal fun applyWallpaperForTheme(
		isDark: Boolean,
		dayUriStr: String?,
		nightUriStr: String?
	): Boolean = applyWallpaperForTheme(
		isDark = isDark,
		dayUriStr = dayUriStr,
		nightUriStr = nightUriStr,
		lockScreenWallpaperBlurEnabled = false
	)

	private fun setWallpaper(uriString: String, flags: Int): Boolean = try {
		val stream = openInputStream(uriString.toUri())
		if (stream == null) {
			Log.w(TAG, "Could not open InputStream for URI: $uriString")
			false
		} else {
			stream.use { setStream(it, flags) }
			true
		}
	} catch (e: Exception) {
		Log.w(TAG, "Failed to set wallpaper from URI: $uriString", e)
		false
	}
}
