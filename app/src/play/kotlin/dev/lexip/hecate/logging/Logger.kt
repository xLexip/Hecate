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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.lexip.hecate.util.ScreenOnProximityResult
import dev.lexip.hecate.util.ThemeSwitchSkipReason

object Logger {

	private fun analytics(context: Context): FirebaseAnalytics =
		FirebaseAnalytics.getInstance(context)

	private inline fun ifAllowed(block: () -> Unit) {
		if (LoggerGate.allowed()) block()
	}

	fun logException(e: Throwable) {
		ifAllowed {
			FirebaseCrashlytics.getInstance().recordException(e)
		}
	}

	fun logServiceEnabled(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("adaptive_service_enabled") {
				param("source", source)
			}
		}
	}

	fun logServiceDisabled(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("adaptive_service_disabled") {
				param("source", source)
			}
		}
	}

	fun logBrightnessThresholdChanged(
		context: Context,
		oldLux: Float,
		newLux: Float
	) {
		if (oldLux == newLux) return
		ifAllowed {
			analytics(context).logEvent("brightness_threshold_changed") {
				param("old_lux", oldLux.toLong())
				param("new_lux", newLux.toLong())
			}
		}
	}

	fun logQuickSettingsTileAdded(context: Context) {
		ifAllowed {
			analytics(context).logEvent("qs_tile_added") { }
		}
	}

	fun logThemeSwitched(
		context: Context,
		targetMode: Int,
		succeeded: Boolean,
		verificationAttempt: Int,
		verificationElapsedMs: Long,
		effectiveUiMode: Int,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		ifAllowed {
			analytics(context).logEvent("theme_switched") {
				param("target_mode", targetMode.toLong())
				param("succeeded", if (succeeded) 1L else 0L)
				param("verification_attempt", verificationAttempt.toLong())
				param("verification_elapsed_ms", verificationElapsedMs)
				param("effective_ui_mode", effectiveUiMode.toLong())
				param(
					"screen_on_proximity_result",
					screenOnProximityResult.analyticsValue
				)
			}
		}
	}

	fun logThemeSwitchSkipped(
		context: Context,
		reason: ThemeSwitchSkipReason,
		screenOnProximityResult: ScreenOnProximityResult
	) {
		ifAllowed {
			analytics(context).logEvent("theme_switch_skipped") {
				param("reason", reason.analyticsValue)
				param(
					"screen_on_proximity_result",
					screenOnProximityResult.analyticsValue
				)
			}
		}
	}

	fun logOverflowMenuItemClicked(context: Context, menuItem: String) {
		ifAllowed {
			analytics(context).logEvent("overflow_menu_item_clicked") {
				param("menu_item", menuItem)
			}
		}
	}

	fun logShareLinkClicked(context: Context, source: String) {
		ifAllowed {
			analytics(context).logEvent("share_link_clicked") {
				param("source", source)
			}
		}
	}

	fun logGitHubStarPromptAction(context: Context, action: String) {
		ifAllowed {
			analytics(context).logEvent("github_star_prompt") {
				param("action", action)
			}
		}
	}

	fun logSetupStarted(context: Context, hasShizuku: Boolean) {
		ifAllowed {
			analytics(context).logEvent("setup_started") {
				param("has_shizuku", if (hasShizuku) 1L else 0L)
			}
		}
	}

	fun logSetupStepOneCompleted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("setup_step_one_completed") { }
		}
	}

	fun logSetupStepTwoCompleted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("setup_step_two_completed") { }
		}
	}

	fun logSetupComplete(context: Context, source: String? = null) {
		ifAllowed {
			analytics(context).logEvent("setup_finished") {
				if (source != null) param("source", source)
			}
		}
	}

	fun logInAppUpdateInstalled(context: Context) {
		ifAllowed {
			analytics(context).logEvent("in_app_update_installed") { }
		}
	}

	fun logUnexpectedShizukuError(
		context: Context,
		operation: String,
		stage: String,
		throwable: Throwable,
		binderReady: Boolean,
		packageName: String? = null
	) {
		ifAllowed {
			analytics(context).logEvent("shizuku_unexpected_error") {
				param("operation", operation)
				param("stage", stage)
				param("exception_type", throwable.javaClass.simpleName)
				param("message", throwable.message ?: "no_message")
				param("binder_ready", if (binderReady) 1L else 0L)
				if (packageName != null) param("package_name", packageName)
			}
		}
	}

	fun logShizukuGrantResult(
		context: Context,
		result: dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult,
		packageName: String
	) {
		ifAllowed {
			analytics(context).logEvent("shizuku_grant_result") {
				val (resultType, exitCode) = when (result) {
					is dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult.Success -> "success" to null
					is dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult.ServiceNotRunning -> "service_not_running" to null
					is dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult.NotAuthorized -> "not_authorized" to null
					is dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult.ShellCommandFailed -> "shell_command_failed" to result.exitCode
					is dev.lexip.hecate.util.shizuku.ShizukuManager.GrantResult.Unexpected -> "unexpected" to null
				}
				param("result_type", resultType)
				exitCode?.let { param("exit_code", it.toLong()) }
				param("package_name", packageName)
			}
		}
	}

	fun logInAppReviewFlowCompleted(context: Context) {
		ifAllowed {
			analytics(context).logEvent("review_flow_completed") { }
		}
	}

	fun logStayDarkAtNightToggled(context: Context, enabled: Boolean, source: String) {
		ifAllowed {
			analytics(context).logEvent("stay_dark_at_night_toggled") {
				param("enabled", if (enabled) 1L else 0L)
				param("source", source)
			}
		}
	}

	fun logProcessExit(
		context: Context,
		reasonName: String,
		description: String,
		pssMb: Long,
		importanceName: String
	) {
		ifAllowed {
			analytics(context).logEvent("process_killed_by_os") {
				param("exit_reason", reasonName)
				param("description", description.take(100))
				param("pss_mb", pssMb)
				param("importance", importanceName)
			}
		}
	}

	fun logWallpaperSyncToggled(context: Context, enabled: Boolean, source: String = "ui") {
		ifAllowed {
			analytics(context).logEvent("wallpaper_sync_toggled") {
				param("enabled", if (enabled) 1L else 0L)
				param("source", source)
			}
		}
	}

	fun logLockScreenWallpaperBlurToggled(context: Context, enabled: Boolean) {
		ifAllowed {
			analytics(context).logEvent("lock_wallpaper_blur_toggled") {
				param("enabled", if (enabled) 1L else 0L)
			}
		}
	}

	fun logWallpaperPicked(context: Context, mode: String) {
		ifAllowed {
			analytics(context).logEvent("wallpaper_picked") {
				param("mode", mode)
			}
		}
	}

	fun logWallpaperSwitched(context: Context, isDark: Boolean, succeeded: Boolean) {
		ifAllowed {
			analytics(context).logEvent("wallpaper_switched") {
				param("is_dark", if (isDark) 1L else 0L)
				param("succeeded", if (succeeded) 1L else 0L)
			}
		}
	}
}

