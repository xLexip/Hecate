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

package dev.lexip.hecate.services

import dev.lexip.hecate.FakeAdaptiveThemeServiceController
import dev.lexip.hecate.FakeUserPreferencesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSettingsToggleCoordinatorTest {

	@Test
	fun stateIsUnavailableWithoutPermission() {
		assertEquals(
			QuickSettingsState.UNAVAILABLE,
			QuickSettingsStatePolicy.from(
				hasPermission = false,
				adaptiveThemeEnabled = true
			)
		)
		assertEquals(
			QuickSettingsState.INACTIVE,
			QuickSettingsStatePolicy.from(
				hasPermission = true,
				adaptiveThemeEnabled = false
			)
		)
		assertEquals(
			QuickSettingsState.ACTIVE,
			QuickSettingsStatePolicy.from(
				hasPermission = true,
				adaptiveThemeEnabled = true
			)
		)
	}

	@Test
	fun enableStartsMonitoringAndPersistsPreference() = runTest {
		val preferences = FakeUserPreferencesDataSource()
		val service = FakeAdaptiveThemeServiceController()
		val coordinator = QuickSettingsToggleCoordinator(preferences, service)

		val result = coordinator.toggle(currentlyEnabled = false)

		assertEquals(QuickSettingsToggleResult.Enabled, result)
		assertEquals(1, service.startCalls)
		assertTrue(service.lastEnableMonitoring)
		assertTrue(service.lastEvaluateImmediately)
		assertEquals(1, preferences.thresholdDefaultCalls)
		assertTrue(preferences.current.adaptiveThemeEnabled)
	}

	@Test
	fun disableStopsServiceAndPersistsPreference() = runTest {
		val preferences = FakeUserPreferencesDataSource().apply {
			updateAdaptiveThemeEnabled(true)
		}
		val service = FakeAdaptiveThemeServiceController()
		val coordinator = QuickSettingsToggleCoordinator(preferences, service)

		val result = coordinator.toggle(currentlyEnabled = true)

		assertEquals(QuickSettingsToggleResult.Disabled, result)
		assertEquals(1, service.stopCalls)
		assertFalse(preferences.current.adaptiveThemeEnabled)
	}

	@Test
	fun serviceStartFailureRollsBackWithoutPersistingEnabledState() = runTest {
		val preferences = FakeUserPreferencesDataSource()
		val service = FakeAdaptiveThemeServiceController().apply {
			startFailure = IllegalStateException("service unavailable")
		}
		val coordinator = QuickSettingsToggleCoordinator(preferences, service)

		val result = coordinator.toggle(currentlyEnabled = false)

		assertTrue(result is QuickSettingsToggleResult.StartFailed)
		assertFalse(preferences.current.adaptiveThemeEnabled)
		assertEquals(0, preferences.thresholdDefaultCalls)
	}
}
