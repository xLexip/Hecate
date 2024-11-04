/*
 * Copyright (C) 2024 xLexip <https://lexip.dev>
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserPreferences(
	val serviceEnabled: Boolean
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

	private val className: String = this::class.java.simpleName

	private object PreferencesKeys {
		val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
	}

	val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			// dataStore.data throws an IOException when an error is encountered when reading data
			if (exception is IOException) {
				Log.e(className, "Error reading user preferences.", exception)
				emit(emptyPreferences())
			} else {
				throw exception
			}
		}.map { preferences ->
			mapUserPreferences(preferences)
		}

	suspend fun fetchInitialPreferences() =
		mapUserPreferences(dataStore.data.first().toPreferences())

	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		// Get our show completed value, defaulting to false if not set:
		val serviceEnabled = preferences[PreferencesKeys.SERVICE_ENABLED] == true
		return UserPreferences(serviceEnabled)
	}

	suspend fun updateServiceEnabled(enabled: Boolean) {
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.SERVICE_ENABLED] = enabled
		}
	}

}