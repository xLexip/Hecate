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

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

private const val TAG = "WallpaperPreprocessor"
private const val WALLPAPER_DIRECTORY = "wallpaper_sources"
private const val JPEG_QUALITY = 90
private const val MAX_PREPARED_PIXELS = 8_000_000L
private const val MAX_DECODE_PIXELS = 16_000_000L
private const val LOCK_SCREEN_BLUR_RADIUS = 32
private const val LOCK_SCREEN_BLUR_PASSES = 3

internal enum class WallpaperSlot {
	DAY,
	NIGHT
}

/** Prepares a selected image and its lock-screen variant for later wallpaper changes. */
internal fun interface WallpaperImagePreparer {
	@Throws(IOException::class)
	fun prepare(sourceUri: Uri, sourceStream: InputStream, slot: WallpaperSlot): Uri
}

internal data class WallpaperMigrationResult(
	val dayWallpaperUri: String?,
	val nightWallpaperUri: String?,
	val failed: Boolean = false
)

private data class SlotMigrationResult(
	val uri: String?,
	val failed: Boolean = false
)

internal fun interface WallpaperStorageMigrator {
	fun migrate(dayWallpaperUri: String?, nightWallpaperUri: String?): WallpaperMigrationResult
}

internal class WallpaperImagePreprocessor(
	context: Context,
	private val openInputStream: (Uri) -> InputStream? =
		context.applicationContext.contentResolver::openInputStream
) :
	WallpaperImagePreparer,
	WallpaperStorageMigrator {
	private val appContext = context.applicationContext
	private val wallpaperManager = WallpaperManager.getInstance(appContext)
	private val outputDirectory = File(appContext.filesDir, WALLPAPER_DIRECTORY)

	override fun prepare(sourceUri: Uri, sourceStream: InputStream, slot: WallpaperSlot): Uri {
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			throw IOException("Could not create wallpaper cache directory")
		}
		val localSource = File(outputDirectory, "${slot.name.lowercase()}_wallpaper_input.tmp")
		return try {
			FileOutputStream(localSource).use(sourceStream::copyTo)
			if (localSource.length() == 0L) {
				throw IOException("Wallpaper source was empty: $sourceUri")
			}
			prepareLocalSource(localSource, sourceUri, slot)
		} finally {
			localSource.delete()
		}
	}

	override fun migrate(
		dayWallpaperUri: String?,
		nightWallpaperUri: String?
	): WallpaperMigrationResult {
		if (dayWallpaperUri == null && nightWallpaperUri == null) {
			return WallpaperMigrationResult(null, null)
		}
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			Log.w(TAG, "Could not create wallpaper cache directory for blur migration")
			return WallpaperMigrationResult(dayWallpaperUri, nightWallpaperUri, failed = true)
		}

		val day = migrateWallpaper(WallpaperSlot.DAY, dayWallpaperUri)
		val night = migrateWallpaper(WallpaperSlot.NIGHT, nightWallpaperUri)
		return WallpaperMigrationResult(
			dayWallpaperUri = day.uri,
			nightWallpaperUri = night.uri,
			failed = day.failed || night.failed
		)
	}

	private fun migrateWallpaper(slot: WallpaperSlot, uriString: String?): SlotMigrationResult {
		if (uriString.isNullOrBlank()) return SlotMigrationResult(uri = null)
		val uri = runCatching { Uri.parse(uriString) }.getOrNull()
			?: return SlotMigrationResult(uriString, failed = true)

		if (uri.scheme.equals("file", ignoreCase = true)) {
			val source = appPrivateWallpaperFile(uri) ?: return SlotMigrationResult(uriString, failed = true)
			val blurReady = prepareMissingVariant(slot, source)
			return SlotMigrationResult(uriString, failed = !blurReady)
		}

		if (!uri.scheme.equals("content", ignoreCase = true)) {
			return SlotMigrationResult(uriString, failed = true)
		}

		return try {
			val storedUri = openInputStream(uri)?.use { sourceStream ->
				prepare(uri, sourceStream, slot)
			} ?: return SlotMigrationResult(uriString, failed = true)
			Log.i(TAG, "Migrated legacy ${slot.name.lowercase()} wallpaper into app storage")
			SlotMigrationResult(storedUri.toString())
		} catch (e: Exception) {
			Log.w(TAG, "Failed to migrate legacy ${slot.name.lowercase()} wallpaper", e)
			SlotMigrationResult(uriString, failed = true)
		}
	}

	private fun prepareMissingVariant(slot: WallpaperSlot, source: File): Boolean {
		val destination = blurredLockScreenWallpaperFile(slot)
		if (destination.isReadableBitmap()) return true
		try {
			val bitmap = decodeBoundedBitmap(source)
			if (bitmap == null) {
				Log.w(TAG, "Could not decode existing ${slot.name.lowercase()} wallpaper for blur migration")
				return false
			}
			val blurred = bitmap.stronglyBlurForLockScreen()
			try {
				writeBitmap(blurred, destination)
				Log.i(TAG, "Created missing ${slot.name.lowercase()} blurred lock wallpaper")
			} finally {
				bitmap.recycle()
				if (blurred !== bitmap) blurred.recycle()
			}
		} catch (e: Exception) {
			Log.w(TAG, "Failed to migrate ${slot.name.lowercase()} blurred lock wallpaper", e)
		}
		return destination.isReadableBitmap()
	}

	private fun decodeBoundedBitmap(source: File): Bitmap? {
		val bounds = BitmapFactory.Options().also { options ->
			options.inJustDecodeBounds = true
			BitmapFactory.decodeFile(source.absolutePath, options)
		}
		if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

		val target = targetDimensions().fitPixelBudget()
		val decodeOptions = BitmapFactory.Options().apply {
			inSampleSize = calculateDecodeSampleSize(
				sourceWidth = bounds.outWidth,
				sourceHeight = bounds.outHeight,
				targetWidth = target.width,
				targetHeight = target.height,
				maxDecodePixels = MAX_PREPARED_PIXELS
			)
		}
		return BitmapFactory.decodeFile(source.absolutePath, decodeOptions)
	}

	private fun appPrivateWallpaperFile(uri: Uri): File? {
		val path = uri.path ?: return null
		return runCatching {
			val source = File(path).canonicalFile
			val filesDirectory = appContext.filesDir.canonicalFile
			val isInsideFilesDirectory = source.path.startsWith(
				filesDirectory.path + File.separator,
				ignoreCase = true
			)
			source.takeIf { isInsideFilesDirectory && it.isFile && it.length() > 0L }
		}.getOrNull()
	}

	private fun prepareLocalSource(localSource: File, sourceUri: Uri, slot: WallpaperSlot): Uri {
		val orientation = readExifOrientation(localSource)
		val bounds = BitmapFactory.Options().also { options ->
			options.inJustDecodeBounds = true
			BitmapFactory.decodeFile(localSource.absolutePath, options)
		}
		if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
			throw IOException("Wallpaper source is not a supported image: $sourceUri")
		}

		val sourceWidth = if (orientation.rotatesDimensions) bounds.outHeight else bounds.outWidth
		val sourceHeight = if (orientation.rotatesDimensions) bounds.outWidth else bounds.outHeight
		val target = targetDimensions().fitPixelBudget()
		val decodeOptions = BitmapFactory.Options().apply {
			inSampleSize = calculateDecodeSampleSize(
				sourceWidth = sourceWidth,
				sourceHeight = sourceHeight,
				targetWidth = target.width,
				targetHeight = target.height,
				maxDecodePixels = MAX_DECODE_PIXELS
			)
		}
		val decoded = BitmapFactory.decodeFile(localSource.absolutePath, decodeOptions)
			?: throw IOException("Could not decode wallpaper source: $sourceUri")

		val oriented = decoded.applyExifOrientation(orientation)
		if (oriented !== decoded) decoded.recycle()
		val scaled = oriented.scaleDownToFill(target.width, target.height)
		if (scaled !== oriented) oriented.recycle()

		val destination = wallpaperFile(slot)
		val blurredDestination = blurredLockScreenWallpaperFile(slot)
		val blurred = scaled.stronglyBlurForLockScreen()
		try {
			writeBitmap(scaled, destination)
			writeBitmap(blurred, blurredDestination)
			Log.i(
				TAG,
				"Prepared ${slot.name.lowercase()} wallpaper and blurred lock variant " +
					"at ${scaled.width}x${scaled.height}"
			)
			return Uri.fromFile(destination)
		} finally {
			scaled.recycle()
			if (blurred !== scaled) blurred.recycle()
		}
	}

	private fun writeBitmap(bitmap: Bitmap, destination: File) {
		val temporaryDestination = File(outputDirectory, "${destination.name}.tmp")
		try {
			FileOutputStream(temporaryDestination).use { output ->
				check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
					"Could not encode prepared wallpaper"
				}
			}
			if (destination.exists() && !destination.delete()) {
				throw IOException("Could not replace prepared wallpaper")
			}
			if (!temporaryDestination.renameTo(destination)) {
				throw IOException("Could not finalize prepared wallpaper")
			}
		} finally {
			if (temporaryDestination.exists()) temporaryDestination.delete()
		}
	}

	private fun wallpaperFile(slot: WallpaperSlot): File =
		File(outputDirectory, "${slot.name.lowercase()}_wallpaper.jpg")

	private fun blurredLockScreenWallpaperFile(slot: WallpaperSlot): File =
		File(outputDirectory, "${slot.name.lowercase()}_wallpaper_lock_blur.jpg")

	private fun targetDimensions(): Dimensions {
		val metrics = appContext.resources.displayMetrics
		return Dimensions(
			width = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: metrics.widthPixels,
			height = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: metrics.heightPixels
		)
	}

	private fun readExifOrientation(source: File): Int = try {
		FileInputStream(source).use { stream ->
			ExifInterface(stream).getAttributeInt(
				ExifInterface.TAG_ORIENTATION,
				ExifInterface.ORIENTATION_NORMAL
			)
		}
	} catch (_: IOException) {
		ExifInterface.ORIENTATION_NORMAL
	}
}

internal fun blurredLockScreenWallpaperUri(context: Context, slot: WallpaperSlot): Uri =
	Uri.fromFile(File(context.filesDir, WALLPAPER_DIRECTORY).resolve("${slot.name.lowercase()}_wallpaper_lock_blur.jpg"))

private data class Dimensions(val width: Int, val height: Int)

private fun Dimensions.fitPixelBudget(): Dimensions {
	val pixels = width.toLong() * height.toLong()
	if (pixels <= MAX_PREPARED_PIXELS) return this
	val scale = kotlin.math.sqrt(MAX_PREPARED_PIXELS.toDouble() / pixels)
	return Dimensions(
		width = max(1, (width * scale).toInt()),
		height = max(1, (height * scale).toInt())
	)
}

private val Int.rotatesDimensions: Boolean
	get() = this == ExifInterface.ORIENTATION_ROTATE_90 ||
		this == ExifInterface.ORIENTATION_ROTATE_270 ||
		this == ExifInterface.ORIENTATION_TRANSPOSE ||
		this == ExifInterface.ORIENTATION_TRANSVERSE

internal fun calculateDecodeSampleSize(
	sourceWidth: Int,
	sourceHeight: Int,
	targetWidth: Int,
	targetHeight: Int,
	maxDecodePixels: Long
): Int {
	var sampleSize = 1
	while (
		sourceWidth / (sampleSize * 2) >= targetWidth &&
		sourceHeight / (sampleSize * 2) >= targetHeight
	) {
		sampleSize *= 2
	}

	while (
		decodedPixelCount(
			sourceWidth = sourceWidth,
			sourceHeight = sourceHeight,
			sampleSize = sampleSize
		) > maxDecodePixels
	) {
		sampleSize *= 2
	}
	return sampleSize
}

private fun decodedPixelCount(
	sourceWidth: Int,
	sourceHeight: Int,
	sampleSize: Int
): Long =
	(sourceWidth / sampleSize).toLong() * (sourceHeight / sampleSize).toLong()

private fun Bitmap.scaleDownToFill(targetWidth: Int, targetHeight: Int): Bitmap {
	val scale = minOf(1f, max(targetWidth.toFloat() / width, targetHeight.toFloat() / height))
	if (scale >= 1f) return this
	val scaledWidth = max(1, (width * scale).toInt())
	val scaledHeight = max(1, (height * scale).toInt())
	return scale(scaledWidth, scaledHeight, filter = true)
}

/**
 * Creates a full-resolution, three-pass box blur. Multiple box passes closely approximate a
 * Gaussian blur while avoiding the visibly blocky result of downscaling and enlarging an image.
 */
internal fun Bitmap.stronglyBlurForLockScreen(): Bitmap {
	if (width <= 1 || height <= 1) return this

	var source = IntArray(width * height)
	val working = IntArray(source.size)
	getPixels(source, 0, width, 0, 0, width, height)
	repeat(LOCK_SCREEN_BLUR_PASSES) {
		boxBlurHorizontally(source, working, width, height, LOCK_SCREEN_BLUR_RADIUS)
		boxBlurVertically(working, source, width, height, LOCK_SCREEN_BLUR_RADIUS)
	}
	return Bitmap.createBitmap(source, width, height, Bitmap.Config.ARGB_8888)
}

private fun boxBlurHorizontally(
	input: IntArray,
	output: IntArray,
	width: Int,
	height: Int,
	radius: Int
) {
	val windowSize = radius * 2 + 1
	for (y in 0 until height) {
		val rowStart = y * width
		var alpha = 0
		var red = 0
		var green = 0
		var blue = 0
		for (x in -radius..radius) {
			val color = input[rowStart + x.coerceIn(0, width - 1)]
			alpha += color ushr 24
			red += color ushr 16 and 0xff
			green += color ushr 8 and 0xff
			blue += color and 0xff
		}
		for (x in 0 until width) {
			output[rowStart + x] = averageColor(alpha, red, green, blue, windowSize)
			val leaving = input[rowStart + (x - radius).coerceIn(0, width - 1)]
			val entering = input[rowStart + (x + radius + 1).coerceIn(0, width - 1)]
			alpha += (entering ushr 24) - (leaving ushr 24)
			red += (entering ushr 16 and 0xff) - (leaving ushr 16 and 0xff)
			green += (entering ushr 8 and 0xff) - (leaving ushr 8 and 0xff)
			blue += (entering and 0xff) - (leaving and 0xff)
		}
	}
}

private fun File.isReadableBitmap(): Boolean {
	if (!isFile || length() <= 0L) return false
	val bounds = BitmapFactory.Options().also { options ->
		options.inJustDecodeBounds = true
		BitmapFactory.decodeFile(absolutePath, options)
	}
	return bounds.outWidth > 0 && bounds.outHeight > 0
}

private fun boxBlurVertically(
	input: IntArray,
	output: IntArray,
	width: Int,
	height: Int,
	radius: Int
) {
	val windowSize = radius * 2 + 1
	for (x in 0 until width) {
		var alpha = 0
		var red = 0
		var green = 0
		var blue = 0
		for (y in -radius..radius) {
			val color = input[y.coerceIn(0, height - 1) * width + x]
			alpha += color ushr 24
			red += color ushr 16 and 0xff
			green += color ushr 8 and 0xff
			blue += color and 0xff
		}
		for (y in 0 until height) {
			output[y * width + x] = averageColor(alpha, red, green, blue, windowSize)
			val leaving = input[(y - radius).coerceIn(0, height - 1) * width + x]
			val entering = input[(y + radius + 1).coerceIn(0, height - 1) * width + x]
			alpha += (entering ushr 24) - (leaving ushr 24)
			red += (entering ushr 16 and 0xff) - (leaving ushr 16 and 0xff)
			green += (entering ushr 8 and 0xff) - (leaving ushr 8 and 0xff)
			blue += (entering and 0xff) - (leaving and 0xff)
		}
	}
}

private fun averageColor(alpha: Int, red: Int, green: Int, blue: Int, divisor: Int): Int =
	((alpha / divisor) shl 24) or
		((red / divisor) shl 16) or
		((green / divisor) shl 8) or
		(blue / divisor)

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
	val matrix = Matrix().apply {
		when (orientation) {
			ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
			ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
			ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
			ExifInterface.ORIENTATION_TRANSPOSE -> {
				setRotate(90f)
				postScale(-1f, 1f)
			}
			ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
			ExifInterface.ORIENTATION_TRANSVERSE -> {
				setRotate(-90f)
				postScale(-1f, 1f)
			}
			ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
		}
	}
	if (matrix.isIdentity) return this
	return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
