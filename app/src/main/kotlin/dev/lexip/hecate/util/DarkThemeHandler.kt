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
class DarkThemeHandler(context: Context) {
    private val appContext = context.applicationContext ?: context
    private val contentResolver = appContext.contentResolver
    private val uiModeManager = requireNotNull(
        appContext.getSystemService(UiModeManager::class.java)
    ) {
        "UiModeManager is unavailable"
    }

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
     * @return The result of attempting to change the system theme.
     */
    @Synchronized
    fun setDarkTheme(
        enable: Boolean,
        screenOnProximityResult: ScreenOnProximityResult
    ): DarkThemeChangeResult {
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
            return DarkThemeChangeResult(succeeded = true, changed = false)
        }

        val succeeded = try {
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

        Logger.logThemeSwitched(
            context = appContext,
            targetMode = plan.targetMode,
            succeeded = succeeded,
            screenOnProximityResult = screenOnProximityResult
        )

        return DarkThemeChangeResult(
            succeeded = succeeded,
            changed = succeeded && plan.refreshUi
        )
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
}

internal fun isNightConfigurationEnabled(uiMode: Int): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

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
