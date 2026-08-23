/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.util

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

private const val DAY_WALLPAPER_URI = "content://wallpaper/day"
private const val NIGHT_WALLPAPER_URI = "content://wallpaper/night"
private const val SHORT_DAY_URI = "content://day"
private const val SHORT_NIGHT_URI = "content://night"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37])
class WallpaperHandlerTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun selectsDayAndNightUrisForTheirMatchingThemes() {
		val openedUris = mutableListOf<String>()
		val handler = handler(
			openInputStream = { uri ->
				openedUris += uri.toString()
				ByteArrayInputStream(byteArrayOf(1))
			}
		)

		assertTrue(
			handler.applyWallpaperForTheme(
				isDark = false,
				dayUriStr = DAY_WALLPAPER_URI,
				nightUriStr = NIGHT_WALLPAPER_URI
			)
		)
		assertTrue(
			handler.applyWallpaperForTheme(
				isDark = true,
				dayUriStr = DAY_WALLPAPER_URI,
				nightUriStr = NIGHT_WALLPAPER_URI
			)
		)

		assertEquals(
			listOf(DAY_WALLPAPER_URI, NIGHT_WALLPAPER_URI),
			openedUris
		)
	}

	@Test
	fun missingTargetUriSkipsPlatformIo() {
		var openCalls = 0
		val handler = handler(
			openInputStream = {
				openCalls++
				ByteArrayInputStream(byteArrayOf(1))
			}
		)

		assertFalse(handler.applyWallpaperForTheme(false, null, NIGHT_WALLPAPER_URI))
		assertFalse(handler.applyWallpaperForTheme(true, DAY_WALLPAPER_URI, ""))
		assertEquals(0, openCalls)
	}

	@Test
	fun nullStreamAndPlatformExceptionsReturnFailure() {
		val nullStreamHandler = handler(openInputStream = { null })
		val openFailureHandler = handler(
			openInputStream = { throw SecurityException("denied") }
		)
		val setFailureHandler = handler(
			setStream = { _, _ -> throw IllegalStateException("set failed") }
		)

		assertFalse(nullStreamHandler.applyWallpaperForTheme(false, SHORT_DAY_URI, SHORT_NIGHT_URI))
		assertFalse(openFailureHandler.applyWallpaperForTheme(false, SHORT_DAY_URI, SHORT_NIGHT_URI))
		assertFalse(setFailureHandler.applyWallpaperForTheme(false, SHORT_DAY_URI, SHORT_NIGHT_URI))
	}

	@Test
	fun successfulApplicationUsesSystemAndLockFlagsAndClosesStream() {
		val stream = CloseTrackingInputStream()
		var appliedFlags = 0
		val handler = handler(
			openInputStream = { stream },
			setStream = { _, flags -> appliedFlags = flags }
		)

		val succeeded = handler.applyWallpaperForTheme(
			isDark = false,
			dayUriStr = DAY_WALLPAPER_URI,
			nightUriStr = NIGHT_WALLPAPER_URI
		)

		assertTrue(succeeded)
		assertEquals(
			WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
			appliedFlags
		)
		assertTrue(stream.closed)
	}

	@Test
	fun blurredLockScreenApplicationUsesSeparateSharpAndBlurredStreams() {
		val openedUris = mutableListOf<String>()
		val appliedFlags = mutableListOf<Int>()
		val handler = handler(
			openInputStream = { uri ->
				openedUris += uri.toString()
				ByteArrayInputStream(byteArrayOf(1))
			},
			setStream = { _, flags -> appliedFlags += flags }
		)

		assertTrue(
			handler.applyWallpaperForTheme(
				isDark = false,
				dayUriStr = DAY_WALLPAPER_URI,
				nightUriStr = NIGHT_WALLPAPER_URI,
				lockScreenWallpaperBlurEnabled = true
			)
		)

		assertEquals(DAY_WALLPAPER_URI, openedUris.first())
		assertTrue(openedUris.last().endsWith("day_wallpaper_lock_blur.jpg"))
		assertEquals(
			listOf(WallpaperManager.FLAG_SYSTEM, WallpaperManager.FLAG_LOCK),
			appliedFlags
		)
	}

	@Test
	fun missingBlurredWallpaperFallsBackToSharpLockWallpaper() {
		val openedUris = mutableListOf<String>()
		val appliedFlags = mutableListOf<Int>()
		val handler = handler(
			openInputStream = { uri ->
				openedUris += uri.toString()
				if (uri.toString().endsWith("_lock_blur.jpg")) null
				else ByteArrayInputStream(byteArrayOf(1))
			},
			setStream = { _, flags -> appliedFlags += flags }
		)

		assertTrue(
			handler.applyWallpaperForTheme(
				isDark = false,
				dayUriStr = DAY_WALLPAPER_URI,
				nightUriStr = NIGHT_WALLPAPER_URI,
				lockScreenWallpaperBlurEnabled = true
			)
		)

		assertEquals(DAY_WALLPAPER_URI, openedUris.first())
		assertTrue(openedUris[1].endsWith("day_wallpaper_lock_blur.jpg"))
		assertEquals(DAY_WALLPAPER_URI, openedUris.last())
		assertEquals(
			listOf(WallpaperManager.FLAG_SYSTEM, WallpaperManager.FLAG_LOCK),
			appliedFlags
		)
	}

	@Test
	fun delegatesLiveWallpaperAndPersistablePermissionChecks() {
		val persisted = mutableListOf<Uri>()
		val handler = handler(
			liveWallpaperActive = { true },
			persistReadPermission = { persisted += it }
		)
		val uri = Uri.parse(DAY_WALLPAPER_URI)

		assertTrue(handler.isLiveWallpaperActive())
		handler.takePersistableReadPermission(uri)

		assertEquals(listOf(uri), persisted)
	}

	private fun handler(
		liveWallpaperActive: () -> Boolean = { false },
		persistReadPermission: (Uri) -> Unit = {},
		openInputStream: (Uri) -> ByteArrayInputStream? = {
			ByteArrayInputStream(byteArrayOf(1))
		},
		setStream: (java.io.InputStream, Int) -> Unit = { _, _ -> }
	) = WallpaperHandler(
		context = context,
		liveWallpaperActive = liveWallpaperActive,
		persistReadPermission = persistReadPermission,
		openInputStream = openInputStream,
		setStream = setStream
	)
}

private class CloseTrackingInputStream : ByteArrayInputStream(byteArrayOf(1)) {
	var closed = false

	override fun close() {
		closed = true
		super.close()
	}
}
