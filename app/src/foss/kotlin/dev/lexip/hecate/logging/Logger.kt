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

package dev.lexip.hecate.logging

import android.content.Context

object Logger {

	fun logException(e: Throwable) {
		// No-op for FOSS build
	}

	fun logServiceEnabled(context: Context, source: String) {
		// No-op for FOSS build
	}

	fun logServiceDisabled(context: Context, source: String) {
		// No-op for FOSS build
	}

	fun logBrightnessThresholdChanged(
		context: Context,
		oldLux: Float,
		newLux: Float
	) {
		// No-op for FOSS build
	}

	fun logQuickSettingsTileAdded(context: Context) {
		// No-op for FOSS build
	}

	fun logThemeSwitched(
		context: Context,
		targetMode: Int,
		succeeded: Boolean
	) {
		// No-op for FOSS build
	}

	fun logOverflowMenuItemClicked(context: Context, menuItem: String) {
		// No-op for FOSS build
	}

	fun logShareLinkClicked(context: Context, source: String) {
		// No-op for FOSS build
	}

	fun logGitHubStarPromptAction(context: Context, action: String) {
		// No-op for FOSS build
	}

	fun logSetupStarted(context: Context, hasShizuku: Boolean) {
		// No-op for FOSS build
	}

	fun logSetupStepOneCompleted(context: Context) {
		// No-op for FOSS build
	}

	fun logSetupStepTwoCompleted(context: Context) {
		// No-op for FOSS build
	}

	fun logSetupComplete(context: Context, source: String? = null) {
		// No-op for FOSS build
	}

	fun logInAppUpdateInstalled(context: Context) {
		// No-op for FOSS build
	}

	fun logUnexpectedShizukuError(
		context: Context,
		operation: String,
		stage: String,
		throwable: Throwable,
		binderReady: Boolean,
		packageName: String? = null
	) {
		// No-op for FOSS build
	}

	fun logShizukuGrantResult(
		context: Context,
		result: dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult,
		packageName: String
	) {
		// No-op for FOSS build
	}

	fun logStayDarkAtNightToggled(context: Context, enabled: Boolean, source: String) {
		// No-op for FOSS build
	}

	fun logProcessExit(
		context: Context,
		reasonName: String,
		description: String,
		pssMb: Long,
		importanceName: String
	) {
		// No-op for FOSS build
	}

	fun logWallpaperSyncToggled(context: Context, enabled: Boolean, source: String = "ui") {
		// No-op for FOSS build
	}

	fun logWallpaperPicked(context: Context, mode: String) {
		// No-op for FOSS build
	}

	fun logWallpaperSwitched(context: Context, isDark: Boolean, succeeded: Boolean) {
		// No-op for FOSS build
	}
}

