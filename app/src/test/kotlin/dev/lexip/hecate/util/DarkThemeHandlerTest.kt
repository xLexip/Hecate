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

import android.app.UiModeManager
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkThemeHandlerTest {
	@Test
	fun `detects the effective dark theme from the current configuration`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES

		assertTrue(isNightConfigurationEnabled(uiMode))
	}

	@Test
	fun `detects the effective light theme from the current configuration`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO

		assertFalse(isNightConfigurationEnabled(uiMode))
	}

	@Test
	fun `does not treat an undefined night mode as dark`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_UNDEFINED

		assertFalse(isNightConfigurationEnabled(uiMode))
	}

	@Test
	fun `matches an effective dark configuration to a dark target`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES

		assertTrue(doesNightConfigurationMatchTarget(uiMode, expectedDark = true))
		assertFalse(doesNightConfigurationMatchTarget(uiMode, expectedDark = false))
	}

	@Test
	fun `matches an effective light configuration to a light target`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_NO

		assertTrue(doesNightConfigurationMatchTarget(uiMode, expectedDark = false))
		assertFalse(doesNightConfigurationMatchTarget(uiMode, expectedDark = true))
	}

	@Test
	fun `does not match an undefined night configuration to either target`() {
		val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_UNDEFINED

		assertFalse(doesNightConfigurationMatchTarget(uiMode, expectedDark = false))
		assertFalse(doesNightConfigurationMatchTarget(uiMode, expectedDark = true))
	}

	@Test
	fun `skips work when configured and effective modes match the target`() {
		val plan = createNightModeUpdatePlan(
			isCurrentlyDark = true,
			configuredMode = UiModeManager.MODE_NIGHT_YES,
			enable = true
		)

		assertFalse(plan.writeSetting)
		assertFalse(plan.refreshUi)
	}

	@Test
	fun `updates policy without refreshing when only configured mode differs`() {
		val plan = createNightModeUpdatePlan(
			isCurrentlyDark = true,
			configuredMode = UiModeManager.MODE_NIGHT_AUTO,
			enable = true
		)

		assertEquals(UiModeManager.MODE_NIGHT_YES, plan.targetMode)
		assertTrue(plan.writeSetting)
		assertFalse(plan.refreshUi)
	}

	@Test
	fun `refreshes without rewriting when only effective mode differs`() {
		val plan = createNightModeUpdatePlan(
			isCurrentlyDark = false,
			configuredMode = UiModeManager.MODE_NIGHT_YES,
			enable = true
		)

		assertFalse(plan.writeSetting)
		assertTrue(plan.refreshUi)
	}

	@Test
	fun `writes and refreshes when configured and effective modes differ`() {
		val plan = createNightModeUpdatePlan(
			isCurrentlyDark = true,
			configuredMode = UiModeManager.MODE_NIGHT_YES,
			enable = false
		)

		assertEquals(UiModeManager.MODE_NIGHT_NO, plan.targetMode)
		assertTrue(plan.writeSetting)
		assertTrue(plan.refreshUi)
	}
}
