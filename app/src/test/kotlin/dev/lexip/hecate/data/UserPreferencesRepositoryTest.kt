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

package dev.lexip.hecate.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

private const val NIGHT_WALLPAPER_URI = "content://wallpaper/night"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37])
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {
	private lateinit var directory: File
	private lateinit var scope: TestScope
	private lateinit var repository: UserPreferencesRepository

	@Before
	fun setUp() {
		directory = Files.createTempDirectory("adaptive-theme-prefs").toFile()
		scope = TestScope(UnconfinedTestDispatcher())
		val dataStore = PreferenceDataStoreFactory.create(
			scope = scope,
			produceFile = { File(directory, "preferences.preferences_pb") }
		)
		repository = UserPreferencesRepository(dataStore)
	}

	@After
	fun tearDown() {
		scope.cancel()
		directory.deleteRecursively()
	}

	@Test
	fun emptyStoreExposesApplicationDefaults() = runTest {
		val preferences = repository.userPreferencesFlow.first()

		assertFalse(preferences.adaptiveThemeEnabled)
		assertEquals(AdaptiveThreshold.DAYLIGHT.lux, preferences.adaptiveThemeThresholdLux)
		assertNull(preferences.customAdaptiveThemeThresholdLux)
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
		assertFalse(preferences.wallpaperSyncEnabled)
		assertNull(preferences.dayWallpaperUri)
		assertNull(preferences.nightWallpaperUri)
		assertFalse(preferences.githubStarPromptDismissed)
		assertEquals(0, preferences.githubStarPromptImpressionCount)
		assertEquals(
			NO_SUPPORT_PROMPT_EPOCH_DAY,
			preferences.githubStarPromptLastImpressionEpochDay
		)
		assertEquals(NO_SUPPORT_PROMPT_EPOCH_DAY, preferences.reviewPromptLastRequestEpochDay)
	}

	@Test
	fun ensureDefaultsDoesNotOverwriteExistingValues() = runTest {
		repository.updateAdaptiveThemeThresholdLux(100f)
		repository.updateNightWindow(20 * 60, 7 * 60)

		repository.ensureAdaptiveThemeThresholdDefault()
		repository.ensureNightDefaults()

		val preferences = repository.userPreferencesFlow.first()
		assertEquals(100f, preferences.adaptiveThemeThresholdLux)
		assertEquals(20 * 60, preferences.nightStartMinutes)
		assertEquals(7 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun presetThresholdClearsCustomThreshold() = runTest {
		repository.updateCustomAdaptiveThemeThresholdLux(42f)
		repository.updateAdaptiveThemeThresholdLux(100f)

		val preferences = repository.userPreferencesFlow.first()
		assertEquals(100f, preferences.adaptiveThemeThresholdLux)
		assertNull(preferences.customAdaptiveThemeThresholdLux)
	}

	@Test
	fun invalidNightWindowIsRejectedWithoutChangingStoredValues() = runTest {
		val updated = repository.updateNightWindow(60, 60)

		assertFalse(updated)
		val preferences = repository.userPreferencesFlow.first()
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun updatesArePersistedTogether() = runTest {
		repository.updateAdaptiveThemeEnabled(true)
		repository.updateSetupCompleted(true)
		repository.updateStayDarkAtNightEnabled(true)
		assertTrue(repository.updateNightWindow(22 * 60, 5 * 60))

		val preferences = repository.fetchInitialPreferences()
		assertTrue(preferences.adaptiveThemeEnabled)
		assertTrue(preferences.hasSetupCompleted)
		assertTrue(preferences.stayDarkAtNightEnabled)
		assertEquals(22 * 60, preferences.nightStartMinutes)
		assertEquals(5 * 60, preferences.nightEndMinutes)
	}

	@Test
	fun wallpaperSettingsArePersistedReplacedAndClearedIndependently() = runTest {
		repository.updateWallpaperSyncEnabled(true)
		repository.updateDayWallpaperUri("content://wallpaper/day-1")
		repository.updateNightWallpaperUri(NIGHT_WALLPAPER_URI)
		repository.updateDayWallpaperUri("content://wallpaper/day-2")

		var preferences = repository.fetchInitialPreferences()
		assertTrue(preferences.wallpaperSyncEnabled)
		assertEquals("content://wallpaper/day-2", preferences.dayWallpaperUri)
		assertEquals(NIGHT_WALLPAPER_URI, preferences.nightWallpaperUri)

		repository.updateDayWallpaperUri(null)
		preferences = repository.fetchInitialPreferences()
		assertNull(preferences.dayWallpaperUri)
		assertEquals(NIGHT_WALLPAPER_URI, preferences.nightWallpaperUri)

		repository.updateNightWallpaperUri(null)
		preferences = repository.fetchInitialPreferences()
		assertNull(preferences.dayWallpaperUri)
		assertNull(preferences.nightWallpaperUri)
	}

	@Test
	fun supportPromptStateIsPersistedAndImpressionsAreCountedOncePerDay() = runTest {
		repository.recordGitHubStarPromptImpression(20_000L)
		repository.recordGitHubStarPromptImpression(20_000L)
		repository.recordGitHubStarPromptImpression(20_001L)
		repository.updateGitHubStarPromptDismissed(true)
		repository.updateReviewPromptLastRequestEpochDay(19_999L)

		val preferences = repository.fetchInitialPreferences()
		assertEquals(2, preferences.githubStarPromptImpressionCount)
		assertEquals(20_001L, preferences.githubStarPromptLastImpressionEpochDay)
		assertTrue(preferences.githubStarPromptDismissed)
		assertEquals(19_999L, preferences.reviewPromptLastRequestEpochDay)
	}

	@Test
	fun readIOExceptionFallsBackToDefaults() = runTest {
		val failingDataStore = object : DataStore<Preferences> {
			override val data: Flow<Preferences> = flow {
				throw IOException("simulated read failure")
			}

			override suspend fun updateData(
				transform: suspend (t: Preferences) -> Preferences
			): Preferences = transform(emptyPreferences())
		}

		val preferences = UserPreferencesRepository(failingDataStore)
			.userPreferencesFlow
			.first()

		assertFalse(preferences.adaptiveThemeEnabled)
		assertEquals(AdaptiveThreshold.DAYLIGHT.lux, preferences.adaptiveThemeThresholdLux)
		assertEquals(21 * 60, preferences.nightStartMinutes)
		assertEquals(6 * 60, preferences.nightEndMinutes)
	}
}
