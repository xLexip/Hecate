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

package dev.lexip.hecate

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

const val USER_PREFERENCES_NAME = "user_preferences"
private val Context.dataStore by preferencesDataStore(USER_PREFERENCES_NAME)

class HecateApplication : Application() {
	/**
	 * Top level data store to ensure it is maintained as a singleton
	 * but still accessible in both the app and service.
	 */
	val userPreferencesDataStore: DataStore<Preferences>
		get() = this.dataStore
}