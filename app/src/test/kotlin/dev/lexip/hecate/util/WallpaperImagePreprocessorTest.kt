/*
 * Copyright (C) 2026 xLexip <https://lexip.dev>
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package dev.lexip.hecate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class WallpaperImagePreprocessorTest {

	@Test
	fun usesTargetDimensionsWhenTheyRequireMoreDownsampling() {
		assertEquals(
			8,
			calculateDecodeSampleSize(
				sourceWidth = 8_000,
				sourceHeight = 6_000,
				targetWidth = 1_000,
				targetHeight = 750,
				maxDecodePixels = 16_000_000L
			)
		)
	}

	@Test
	fun capsOversizedDecodeEvenWhenTargetWouldUseFullResolution() {
		assertEquals(
			2,
			calculateDecodeSampleSize(
				sourceWidth = 12_000,
				sourceHeight = 4_000,
				targetWidth = 12_000,
				targetHeight = 4_000,
				maxDecodePixels = 16_000_000L
			)
		)
	}

	@Test
	fun keepsFullResolutionAtTheDecodePixelBudget() {
		assertEquals(
			1,
			calculateDecodeSampleSize(
				sourceWidth = 8_000,
				sourceHeight = 2_000,
				targetWidth = 8_000,
				targetHeight = 2_000,
				maxDecodePixels = 16_000_000L
			)
		)
	}

}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WallpaperImagePreprocessorLocalCopyTest {
	@Test
	fun migrationCreatesOnlyMissingBlurredCopyFromExistingLocalWallpaper() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val directory = File(context.filesDir, "wallpaper_sources").apply { mkdirs() }
		val sharp = File(directory, "day_wallpaper.jpg")
		val blurred = File(directory, "day_wallpaper_lock_blur.jpg")
		val sourceBitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply {
			setPixels(
				IntArray(120 * 80) { index -> if (index % 120 < 60) Color.BLACK else Color.WHITE },
				0,
				120,
				0,
				0,
				120,
				80
			)
		}
		try {
			FileOutputStream(sharp).use { output ->
				assertTrue(sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
			}
			blurred.delete()
			val originalSharpBytes = sharp.readBytes()
			val preprocessor = WallpaperImagePreprocessor(context)

			val migration = preprocessor.migrate(Uri.fromFile(sharp).toString(), null)

			assertEquals(Uri.fromFile(sharp).toString(), migration.dayWallpaperUri)
			assertTrue(!migration.failed)
			assertTrue(blurred.isFile)
			assertTrue(originalSharpBytes.contentEquals(sharp.readBytes()))
			val blurredBitmap = BitmapFactory.decodeFile(blurred.absolutePath)
			assertEquals(120, blurredBitmap.width)
			assertEquals(80, blurredBitmap.height)
			blurredBitmap.recycle()

			val migrationMarker = 1_700_000_000_000L
			assertTrue(blurred.setLastModified(migrationMarker))
			preprocessor.migrate(Uri.fromFile(sharp).toString(), null)
			assertEquals(migrationMarker, blurred.lastModified())
		} finally {
			sourceBitmap.recycle()
			sharp.delete()
			blurred.delete()
		}
	}

	@Test
	fun migrationCopiesReadableLegacyContentUriIntoAppStorage() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val source = File(context.cacheDir, "legacy_day_wallpaper.jpg")
		val sourceBitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
		val directory = File(context.filesDir, "wallpaper_sources")
		val sharp = File(directory, "day_wallpaper.jpg")
		val blurred = File(directory, "day_wallpaper_lock_blur.jpg")
		try {
			FileOutputStream(source).use { output ->
				assertTrue(sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
			}
			sharp.delete()
			blurred.delete()
			val legacyUri = Uri.parse("content://media/picker/legacy/day")
			val preprocessor = WallpaperImagePreprocessor(context) { uri ->
				if (uri == legacyUri) FileInputStream(source) else null
			}

			val migration = preprocessor.migrate(legacyUri.toString(), null)

			assertTrue(!migration.failed)
			assertEquals(Uri.fromFile(sharp).toString(), migration.dayWallpaperUri)
			assertTrue(sharp.isReadableImage())
			assertTrue(blurred.isReadableImage())
		} finally {
			sourceBitmap.recycle()
			source.delete()
			sharp.delete()
			blurred.delete()
		}
	}

	@Test
	fun lockScreenBlurKeepsResolutionAndBlendsNeighbouringPixels() {
		val source = Bitmap.createBitmap(129, 129, Bitmap.Config.ARGB_8888).apply {
			setPixels(
				IntArray(129 * 129) { index -> if (index % 129 < 64) Color.BLACK else Color.WHITE },
				0,
				129,
				0,
				0,
				129,
				129
			)
		}

		val blurred = source.stronglyBlurForLockScreen()

		assertEquals(129, blurred.width)
		assertEquals(129, blurred.height)
		assertTrue(Color.red(blurred.getPixel(64, 64)) in 1..254)
		assertTrue(Color.red(blurred.getPixel(40, 64)) > 0)
		source.recycle()
		blurred.recycle()
	}

	@Test
	fun selectionCreatesSharpAndBlurredLocalCopies() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val source = File(context.cacheDir, "selected_wallpaper.jpg")
		val sourceBitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
		FileOutputStream(source).use { output ->
			assertTrue(sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
		}
		val directory = File(context.filesDir, "wallpaper_sources")
		val sharp = File(directory, "night_wallpaper.jpg")
		val blurred = File(directory, "night_wallpaper_lock_blur.jpg")
		try {
			val result = FileInputStream(source).use { sourceStream ->
				WallpaperImagePreprocessor(context).prepare(
					sourceUri = Uri.fromFile(source),
					sourceStream = sourceStream,
					slot = WallpaperSlot.NIGHT
				)
			}

			assertEquals(Uri.fromFile(sharp), result)
			assertTrue(sharp.isFile)
			assertTrue(blurred.isFile)
			val sharpBitmap = BitmapFactory.decodeFile(sharp.absolutePath)
			val blurredBitmap = BitmapFactory.decodeFile(blurred.absolutePath)
			assertEquals(120, sharpBitmap.width)
			assertEquals(120, blurredBitmap.width)
			sharpBitmap.recycle()
			blurredBitmap.recycle()
		} finally {
			sourceBitmap.recycle()
			source.delete()
			sharp.delete()
			blurred.delete()
		}
	}
}

private fun File.isReadableImage(): Boolean = BitmapFactory.decodeFile(absolutePath)?.let { bitmap ->
	bitmap.recycle()
	true
} ?: false
