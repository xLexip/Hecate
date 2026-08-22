/*
 * Copyright (C) 2024-2026 xLexip <https://lexip.dev>
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

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings.Secure
import android.util.Log
import dev.lexip.hecate.logging.Logger

private const val TAG = "DarkThemeHandler"
private const val SECURE_SETTINGS_KEY = "ui_night_mode"
private const val NIGHT_MODE_UNSET = -1

/**
 * Handler for managing the system dark theme.
 */
class DarkThemeHandler internal constructor(
    context: Context,
    delayedActionScheduler: DelayedActionScheduler
) {
    constructor(context: Context) : this(context, MainThreadDelayedActionScheduler())

    private val appContext = context.applicationContext ?: context
    private val contentResolver = appContext.contentResolver
    private val uiModeManager = requireNotNull(
        appContext.getSystemService(UiModeManager::class.java)
    ) {
        "UiModeManager is unavailable"
    }
    private val themeChangeVerifier = ThemeChangeVerifier(
        delayedActionScheduler = delayedActionScheduler,
        effectiveUiModeProvider = { appContext.resources.configuration.uiMode }
    )

    /**
     * @return True if the system dark theme is enabled, false otherwise.
     */
    fun isDarkThemeEnabled(): Boolean {
        val enabled = isNightConfigurationEnabled(appContext.resources.configuration.uiMode)
        Log.d(TAG, "Device dark theme enabled: $enabled")
        return enabled
    }

    /**
     * Set the system dark theme based on the given parameter.
     * @param enable True to enable dark theme, false to disable.
     * @param onComplete Called once the effective system configuration has been verified.
     */
    @Synchronized
    fun setDarkTheme(
        enable: Boolean,
        screenOnProximityResult: ScreenOnProximityResult,
        onComplete: (DarkThemeChangeResult) -> Unit = {}
    ) {
        if (themeChangeVerifier.isVerificationInProgress) {
            Log.w(TAG, "Theme verification already in progress")
            Logger.logThemeSwitchSkipped(
                context = appContext,
                reason = ThemeSwitchSkipReason.THEME_TRANSITION_IN_PROGRESS,
                screenOnProximityResult = screenOnProximityResult
            )
            onComplete(DarkThemeChangeResult(succeeded = false, changed = false))
            return
        }

        val isCurrentlyDark = isDarkThemeEnabled()
        val configuredMode = Secure.getInt(
            contentResolver,
            SECURE_SETTINGS_KEY,
            NIGHT_MODE_UNSET
        )
        val plan = createNightModeUpdatePlan(
            isCurrentlyDark = isCurrentlyDark,
            configuredMode = configuredMode,
            enable = enable
        )
        if (!plan.writeSetting && !plan.refreshUi) {
            onComplete(DarkThemeChangeResult(succeeded = true, changed = false))
            return
        }

        val requestSucceeded = try {
            if (plan.refreshUi &&
                uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
            ) {
                false
            } else {
                Log.i(TAG, "Setting dark theme to target mode: ${plan.targetMode} (transitioning=${plan.refreshUi})")
                val settingUpdated = !plan.writeSetting ||
                    Secure.putInt(contentResolver, SECURE_SETTINGS_KEY, plan.targetMode)
                if (settingUpdated) {
                    if (plan.refreshUi) refreshUi()
                    true
                } else {
                    Log.w(TAG, "Secure.putInt reported failure when changing dark theme")
                    false
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while changing dark theme", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception while changing dark theme", e)
            false
        }
        if (!requestSucceeded) {
            if (plan.refreshUi) {
                completeFailedThemeChange(
                    targetMode = plan.targetMode,
                    screenOnProximityResult = screenOnProximityResult,
                    onComplete = onComplete
                )
            } else {
                onComplete(DarkThemeChangeResult(succeeded = false, changed = false))
            }
            return
        }
        if (!plan.refreshUi) {
            onComplete(DarkThemeChangeResult(succeeded = true, changed = false))
            return
        }

        val verificationStarted = themeChangeVerifier.start(enable) { verification ->
            val result = DarkThemeChangeResult(
                succeeded = verification.succeeded,
                changed = verification.succeeded
            )
            if (verification.succeeded) {
                Log.i(
                    TAG,
                    "Theme change verified for target mode ${plan.targetMode} " +
                        "on attempt ${verification.attempt}"
                )
            } else {
                Log.w(
                    TAG,
                    "Theme change did not reach target mode ${plan.targetMode} " +
                        "after ${verification.elapsedMillis} ms"
                )
            }
            try {
                Logger.logThemeSwitched(
                    context = appContext,
                    targetMode = plan.targetMode,
                    succeeded = verification.succeeded,
                    verificationAttempt = verification.attempt,
                    verificationElapsedMs = verification.elapsedMillis,
                    effectiveUiMode = verification.effectiveUiMode,
                    screenOnProximityResult = screenOnProximityResult
                )
            } finally {
                onComplete(result)
            }
        }
        if (!verificationStarted) {
            Logger.logThemeSwitchSkipped(
                context = appContext,
                reason = ThemeSwitchSkipReason.THEME_TRANSITION_IN_PROGRESS,
                screenOnProximityResult = screenOnProximityResult
            )
            onComplete(DarkThemeChangeResult(succeeded = false, changed = false))
        }
    }

    fun cancelPendingVerification() {
        themeChangeVerifier.cancel()
    }

    /**
     * Refreshes the Android UI by briefly enabling and disabling car mode.
     * This compatibility workaround is required because changing the secure setting alone
     * does not reliably apply the new theme to the entire system UI.
     * @since API 29 (Android 10)
     * @see <a href="https://developer.android.com/reference/android/app/UiModeManager#setNightMode(int)">UiModeManager.setNightMode(int)</a>
     */
    @SuppressLint("WrongConstant")
    private fun refreshUi() {
        Log.d(TAG, "Refreshing system UI after dark theme change...")
        uiModeManager.enableCarMode(0)
        uiModeManager.disableCarMode(0)
    }

    private fun completeFailedThemeChange(
        targetMode: Int,
        screenOnProximityResult: ScreenOnProximityResult,
        onComplete: (DarkThemeChangeResult) -> Unit
    ) {
        try {
            Logger.logThemeSwitched(
                context = appContext,
                targetMode = targetMode,
                succeeded = false,
                verificationAttempt = 0,
                verificationElapsedMs = 0,
                effectiveUiMode = appContext.resources.configuration.uiMode,
                screenOnProximityResult = screenOnProximityResult
            )
        } finally {
            onComplete(DarkThemeChangeResult(succeeded = false, changed = false))
        }
    }
}

internal fun isNightConfigurationEnabled(uiMode: Int): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

internal fun doesNightConfigurationMatchTarget(uiMode: Int, expectedDark: Boolean): Boolean {
    val expectedNightMode = if (expectedDark) {
        Configuration.UI_MODE_NIGHT_YES
    } else {
        Configuration.UI_MODE_NIGHT_NO
    }
    return uiMode and Configuration.UI_MODE_NIGHT_MASK == expectedNightMode
}

internal data class NightModeUpdatePlan(
    val targetMode: Int,
    val writeSetting: Boolean,
    val refreshUi: Boolean
)

internal fun createNightModeUpdatePlan(
    isCurrentlyDark: Boolean,
    configuredMode: Int,
    enable: Boolean
): NightModeUpdatePlan {
    val targetMode = if (enable) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
    return NightModeUpdatePlan(
        targetMode = targetMode,
        writeSetting = configuredMode != targetMode,
        refreshUi = isCurrentlyDark != enable
    )
}
