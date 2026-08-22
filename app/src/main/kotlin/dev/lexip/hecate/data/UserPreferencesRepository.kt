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

package dev.lexip.hecate.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "UserPreferencesRepository"
private const val DEFAULT_NIGHT_START_MINUTES = 21 * 60
private const val DEFAULT_NIGHT_END_MINUTES = 6 * 60
internal const val NO_SUPPORT_PROMPT_EPOCH_DAY = -1L

data class UserPreferences(
	val adaptiveThemeEnabled: Boolean,
	val adaptiveThemeThresholdLux: Float,
	val customAdaptiveThemeThresholdLux: Float? = null,
	val hasSetupCompleted: Boolean = false,
	val stayDarkAtNightEnabled: Boolean = false,
	val nightStartMinutes: Int = DEFAULT_NIGHT_START_MINUTES,
	val nightEndMinutes: Int = DEFAULT_NIGHT_END_MINUTES,
	val wallpaperSyncEnabled: Boolean = false,
	val dayWallpaperUri: String? = null,
	val nightWallpaperUri: String? = null,
	val wallpaperStorageVersion: Int = 0,
	val githubStarPromptDismissed: Boolean = false,
	val githubStarPromptImpressionCount: Int = 0,
	val githubStarPromptLastImpressionEpochDay: Long = NO_SUPPORT_PROMPT_EPOCH_DAY,
	val reviewPromptLastRequestEpochDay: Long = NO_SUPPORT_PROMPT_EPOCH_DAY
)

interface UserPreferencesDataSource {
	val userPreferencesFlow: Flow<UserPreferences>

	suspend fun fetchInitialPreferences(): UserPreferences
	suspend fun ensureAdaptiveThemeThresholdDefault(default: Float = AdaptiveThreshold.DAYLIGHT.lux)
	suspend fun ensureNightDefaults(
		defaultStartMinutes: Int = DEFAULT_NIGHT_START_MINUTES,
		defaultEndMinutes: Int = DEFAULT_NIGHT_END_MINUTES
	)

	suspend fun updateAdaptiveThemeEnabled(enabled: Boolean)
	suspend fun updateAdaptiveThemeThresholdLux(lux: Float)
	suspend fun updateCustomAdaptiveThemeThresholdLux(lux: Float)
	suspend fun updateSetupCompleted(completed: Boolean)
	suspend fun updateStayDarkAtNightEnabled(enabled: Boolean)
	suspend fun updateNightWindow(startMinutes: Int, endMinutes: Int): Boolean
	suspend fun updateWallpaperSyncEnabled(enabled: Boolean)
	suspend fun updateDayWallpaperUri(uri: String?)
	suspend fun updateNightWallpaperUri(uri: String?)
	suspend fun updateWallpaperStorageVersion(version: Int)
	suspend fun recordGitHubStarPromptImpression(epochDay: Long)
	suspend fun updateGitHubStarPromptDismissed(dismissed: Boolean)
	suspend fun updateReviewPromptLastRequestEpochDay(epochDay: Long)
}

class UserPreferencesRepository(
	private val dataStore: DataStore<Preferences>
) : UserPreferencesDataSource {

	private object PreferencesKeys {
		val ADAPTIVE_THEME_ENABLED = booleanPreferencesKey("adaptive_theme_enabled")
		val ADAPTIVE_THEME_THRESHOLD_LUX = floatPreferencesKey("adaptive_theme_threshold_lux")
		val CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX =
			floatPreferencesKey("custom_adaptive_theme_threshold_lux")
		val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
		val STAY_DARK_AT_NIGHT_ENABLED = booleanPreferencesKey("stay_dark_at_night_enabled")
		val NIGHT_START_MINUTES = intPreferencesKey("night_start_minutes")
		val NIGHT_END_MINUTES = intPreferencesKey("night_end_minutes")
		val WALLPAPER_SYNC_ENABLED = booleanPreferencesKey("wallpaper_sync_enabled")
		val DAY_WALLPAPER_URI = stringPreferencesKey("day_wallpaper_uri")
		val NIGHT_WALLPAPER_URI = stringPreferencesKey("night_wallpaper_uri")
		val WALLPAPER_STORAGE_VERSION = intPreferencesKey("wallpaper_storage_version")
		val GITHUB_STAR_PROMPT_DISMISSED = booleanPreferencesKey("github_star_prompt_dismissed")
		val GITHUB_STAR_PROMPT_IMPRESSION_COUNT =
			intPreferencesKey("github_star_prompt_impression_count")
		val GITHUB_STAR_PROMPT_LAST_IMPRESSION_EPOCH_DAY =
			longPreferencesKey("github_star_prompt_last_impression_epoch_day")
		val REVIEW_PROMPT_LAST_REQUEST_EPOCH_DAY =
			longPreferencesKey("review_prompt_last_request_epoch_day")
	}

	override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			// dataStore.data throws an IOException when an error is encountered when reading data
			if (exception is IOException) {
				Log.e(TAG, "Error reading user preferences.", exception)
				emit(emptyPreferences())
			} else {
				throw exception
			}
		}.map { preferences ->
			mapUserPreferences(preferences)
		}

	override suspend fun fetchInitialPreferences() =
		mapUserPreferences(dataStore.data.first().toPreferences())


	override suspend fun ensureAdaptiveThemeThresholdDefault(default: Float) {
		dataStore.edit { preferences ->
			if (preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] == null) {
				preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = default
			}
		}
	}

	override suspend fun ensureNightDefaults(
		defaultStartMinutes: Int,
		defaultEndMinutes: Int
	) {
		dataStore.edit { preferences ->
			if (preferences[PreferencesKeys.NIGHT_START_MINUTES] == null) {
				preferences[PreferencesKeys.NIGHT_START_MINUTES] = defaultStartMinutes
			}
			if (preferences[PreferencesKeys.NIGHT_END_MINUTES] == null) {
				preferences[PreferencesKeys.NIGHT_END_MINUTES] = defaultEndMinutes
			}
		}
	}

	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		val adaptiveThemeEnabled = preferences[PreferencesKeys.ADAPTIVE_THEME_ENABLED] == true
		val adaptiveThemeThresholdLux =
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX]
				?: AdaptiveThreshold.DAYLIGHT.lux
		val customAdaptiveThemeThresholdLux =
			preferences[PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX]
		val hasSetupCompleted =
			preferences[PreferencesKeys.SETUP_COMPLETED] == true
		val stayDarkAtNightEnabled = preferences[PreferencesKeys.STAY_DARK_AT_NIGHT_ENABLED] == true
		val nightStartMinutes =
			preferences[PreferencesKeys.NIGHT_START_MINUTES] ?: DEFAULT_NIGHT_START_MINUTES
		val nightEndMinutes =
			preferences[PreferencesKeys.NIGHT_END_MINUTES] ?: DEFAULT_NIGHT_END_MINUTES
		val wallpaperSyncEnabled = preferences[PreferencesKeys.WALLPAPER_SYNC_ENABLED] == true
		val dayWallpaperUri = preferences[PreferencesKeys.DAY_WALLPAPER_URI]
		val nightWallpaperUri = preferences[PreferencesKeys.NIGHT_WALLPAPER_URI]
		val wallpaperStorageVersion = preferences[PreferencesKeys.WALLPAPER_STORAGE_VERSION] ?: 0
		val githubStarPromptDismissed =
			preferences[PreferencesKeys.GITHUB_STAR_PROMPT_DISMISSED] == true
		val githubStarPromptImpressionCount =
			preferences[PreferencesKeys.GITHUB_STAR_PROMPT_IMPRESSION_COUNT] ?: 0
		val githubStarPromptLastImpressionEpochDay =
			preferences[PreferencesKeys.GITHUB_STAR_PROMPT_LAST_IMPRESSION_EPOCH_DAY]
				?: NO_SUPPORT_PROMPT_EPOCH_DAY
		val reviewPromptLastRequestEpochDay =
			preferences[PreferencesKeys.REVIEW_PROMPT_LAST_REQUEST_EPOCH_DAY]
				?: NO_SUPPORT_PROMPT_EPOCH_DAY
		return UserPreferences(
			adaptiveThemeEnabled = adaptiveThemeEnabled,
			adaptiveThemeThresholdLux = adaptiveThemeThresholdLux,
			customAdaptiveThemeThresholdLux = customAdaptiveThemeThresholdLux,
			hasSetupCompleted = hasSetupCompleted,
			stayDarkAtNightEnabled = stayDarkAtNightEnabled,
			nightStartMinutes = nightStartMinutes,
			nightEndMinutes = nightEndMinutes,
			wallpaperSyncEnabled = wallpaperSyncEnabled,
			dayWallpaperUri = dayWallpaperUri,
			nightWallpaperUri = nightWallpaperUri,
			wallpaperStorageVersion = wallpaperStorageVersion,
			githubStarPromptDismissed = githubStarPromptDismissed,
			githubStarPromptImpressionCount = githubStarPromptImpressionCount,
			githubStarPromptLastImpressionEpochDay = githubStarPromptLastImpressionEpochDay,
			reviewPromptLastRequestEpochDay = reviewPromptLastRequestEpochDay
		)
	}

	override suspend fun updateAdaptiveThemeEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_ENABLED] = enabled
		}
	}

	override suspend fun updateAdaptiveThemeThresholdLux(lux: Float) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = lux
			preferences.remove(PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX)
		}
	}

	override suspend fun updateCustomAdaptiveThemeThresholdLux(lux: Float) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.ADAPTIVE_THEME_THRESHOLD_LUX] = lux
			preferences[PreferencesKeys.CUSTOM_ADAPTIVE_THEME_THRESHOLD_LUX] = lux
		}
	}


	override suspend fun updateSetupCompleted(completed: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.SETUP_COMPLETED] = completed
		}
	}

	override suspend fun updateStayDarkAtNightEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.STAY_DARK_AT_NIGHT_ENABLED] = enabled
		}
	}

	override suspend fun updateWallpaperSyncEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.WALLPAPER_SYNC_ENABLED] = enabled
		}
	}

	override suspend fun updateDayWallpaperUri(uri: String?) {
		dataStore.edit { preferences ->
			if (uri == null) {
				preferences.remove(PreferencesKeys.DAY_WALLPAPER_URI)
			} else {
				preferences[PreferencesKeys.DAY_WALLPAPER_URI] = uri
			}
		}
	}

	override suspend fun updateNightWallpaperUri(uri: String?) {
		dataStore.edit { preferences ->
			if (uri == null) {
				preferences.remove(PreferencesKeys.NIGHT_WALLPAPER_URI)
			} else {
				preferences[PreferencesKeys.NIGHT_WALLPAPER_URI] = uri
			}
		}
	}

	override suspend fun updateWallpaperStorageVersion(version: Int) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.WALLPAPER_STORAGE_VERSION] = version
		}
	}

	override suspend fun recordGitHubStarPromptImpression(epochDay: Long) {
		dataStore.edit { preferences ->
			val lastImpression =
				preferences[PreferencesKeys.GITHUB_STAR_PROMPT_LAST_IMPRESSION_EPOCH_DAY]
			if (lastImpression != epochDay) {
				val impressionCount =
					preferences[PreferencesKeys.GITHUB_STAR_PROMPT_IMPRESSION_COUNT] ?: 0
				preferences[PreferencesKeys.GITHUB_STAR_PROMPT_IMPRESSION_COUNT] =
					impressionCount + 1
			}
			preferences[PreferencesKeys.GITHUB_STAR_PROMPT_LAST_IMPRESSION_EPOCH_DAY] = epochDay
		}
	}

	override suspend fun updateGitHubStarPromptDismissed(dismissed: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.GITHUB_STAR_PROMPT_DISMISSED] = dismissed
		}
	}

	override suspend fun updateReviewPromptLastRequestEpochDay(epochDay: Long) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.REVIEW_PROMPT_LAST_REQUEST_EPOCH_DAY] = epochDay
		}
	}

	/**
	 * @return true when values are valid and persisted, false when rejected.
	 */
	override suspend fun updateNightWindow(startMinutes: Int, endMinutes: Int): Boolean {
		if (!isValidMinute(startMinutes) || !isValidMinute(endMinutes) || startMinutes == endMinutes) {
			Log.w(
				TAG,
				"Rejected invalid night window update: start=$startMinutes, end=$endMinutes"
			)
			return false
		}

		dataStore.edit { preferences ->
			preferences[PreferencesKeys.NIGHT_START_MINUTES] = startMinutes
			preferences[PreferencesKeys.NIGHT_END_MINUTES] = endMinutes
		}
		return true
	}

	private fun isValidMinute(value: Int): Boolean = value in 0 until 24 * 60

}
