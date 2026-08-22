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

package dev.lexip.hecate.ui

import dev.lexip.hecate.data.NO_SUPPORT_PROMPT_EPOCH_DAY
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TODAY = 20_000L

class SupportPromptPolicyTest {

	@Test
	fun githubPromptRequiresCompletedSetupAndEnabledServiceAfterTwoWeeks() {
		assertFalse(shouldShowGitHubStarPrompt(githubInput(hasSetupCompleted = false)))
		assertFalse(shouldShowGitHubStarPrompt(githubInput(adaptiveThemeEnabled = false)))
		assertFalse(shouldShowGitHubStarPrompt(githubInput(daysSinceFirstInstall = 13)))
		assertTrue(shouldShowGitHubStarPrompt(githubInput(daysSinceFirstInstall = 14)))
	}

	@Test
	fun githubPromptPersistsUntilInteractionAndWaitsFourteenDaysAfterReview() {
		assertFalse(shouldShowGitHubStarPrompt(githubInput(dismissed = true)))
		assertFalse(
			shouldShowGitHubStarPrompt(
				githubInput(lastReviewRequestEpochDay = TODAY - 13)
			)
		)
		assertTrue(
			shouldShowGitHubStarPrompt(
				githubInput(lastReviewRequestEpochDay = TODAY - 14)
			)
		)
	}

	@Test
	fun githubPromptRemainsEligibleInFollowingSessionsWithoutInteraction() {
		assertTrue(shouldShowGitHubStarPrompt(githubInput()))
	}

	@Test
	fun reviewPromptRequiresPlayInstallAndSeparatesSupportAsks() {
		assertFalse(shouldRequestReview(reviewInput(isPlayStoreInstall = false)))
		assertFalse(shouldRequestReview(reviewInput(serviceEnabledAtStart = false)))
		assertFalse(shouldRequestReview(reviewInput(daysSinceFirstInstall = 1)))
		assertFalse(shouldRequestReview(reviewInput(requestedInSession = true)))
		assertFalse(shouldRequestReview(reviewInput(githubPromptRenderedInSession = true)))
		assertFalse(
			shouldRequestReview(reviewInput(lastReviewRequestEpochDay = TODAY - 13))
		)
		assertFalse(
			shouldRequestReview(reviewInput(lastGitHubImpressionEpochDay = TODAY - 13))
		)
		assertTrue(
			shouldRequestReview(
				reviewInput(
					lastReviewRequestEpochDay = TODAY - 14,
					lastGitHubImpressionEpochDay = TODAY - 14
				)
			)
		)
	}

	private fun githubInput(
		hasSetupCompleted: Boolean = true,
		adaptiveThemeEnabled: Boolean = true,
		daysSinceFirstInstall: Long = 14,
		dismissed: Boolean = false,
		lastReviewRequestEpochDay: Long = NO_SUPPORT_PROMPT_EPOCH_DAY
	) = GitHubStarPromptEligibility(
		hasSetupCompleted = hasSetupCompleted,
		adaptiveThemeEnabled = adaptiveThemeEnabled,
		daysSinceFirstInstall = daysSinceFirstInstall,
		dismissed = dismissed,
		lastReviewRequestEpochDay = lastReviewRequestEpochDay,
		todayEpochDay = TODAY
	)

	private fun reviewInput(
		isPlayStoreInstall: Boolean = true,
		serviceEnabledAtStart: Boolean = true,
		daysSinceFirstInstall: Long = 2,
		requestedInSession: Boolean = false,
		githubPromptRenderedInSession: Boolean = false,
		lastReviewRequestEpochDay: Long = NO_SUPPORT_PROMPT_EPOCH_DAY,
		lastGitHubImpressionEpochDay: Long = NO_SUPPORT_PROMPT_EPOCH_DAY
	) = ReviewPromptEligibility(
		isPlayStoreInstall = isPlayStoreInstall,
		serviceEnabledAtStart = serviceEnabledAtStart,
		daysSinceFirstInstall = daysSinceFirstInstall,
		requestedInSession = requestedInSession,
		githubPromptRenderedInSession = githubPromptRenderedInSession,
		lastReviewRequestEpochDay = lastReviewRequestEpochDay,
		lastGitHubImpressionEpochDay = lastGitHubImpressionEpochDay,
		todayEpochDay = TODAY
	)
}
