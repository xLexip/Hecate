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

internal const val GITHUB_STAR_PROMPT_MIN_INSTALL_DAYS = 14L
internal const val SUPPORT_PROMPT_SEPARATION_DAYS = 14L
internal const val REVIEW_PROMPT_MIN_INSTALL_DAYS = 2L

internal data class GitHubStarPromptEligibility(
	val hasSetupCompleted: Boolean,
	val adaptiveThemeEnabled: Boolean,
	val daysSinceFirstInstall: Long,
	val dismissed: Boolean,
	val lastReviewRequestEpochDay: Long,
	val todayEpochDay: Long
)

internal data class ReviewPromptEligibility(
	val isPlayStoreInstall: Boolean,
	val serviceEnabledAtStart: Boolean,
	val daysSinceFirstInstall: Long,
	val requestedInSession: Boolean,
	val githubPromptRenderedInSession: Boolean,
	val lastReviewRequestEpochDay: Long,
	val lastGitHubImpressionEpochDay: Long,
	val todayEpochDay: Long
)

internal fun shouldShowGitHubStarPrompt(input: GitHubStarPromptEligibility): Boolean {
	if (!input.hasSetupCompleted || !input.adaptiveThemeEnabled || input.dismissed) return false
	if (input.daysSinceFirstInstall < GITHUB_STAR_PROMPT_MIN_INSTALL_DAYS) return false

	if (!hasElapsed(
			input.todayEpochDay,
			input.lastReviewRequestEpochDay,
			SUPPORT_PROMPT_SEPARATION_DAYS
		)
	) return false

	return true
}

internal fun shouldRequestReview(input: ReviewPromptEligibility): Boolean {
	if (!input.isPlayStoreInstall ||
		!input.serviceEnabledAtStart ||
		input.requestedInSession ||
		input.githubPromptRenderedInSession
	) {
		return false
	}
	if (input.daysSinceFirstInstall < REVIEW_PROMPT_MIN_INSTALL_DAYS) return false
	if (!hasElapsed(
			input.todayEpochDay,
			input.lastReviewRequestEpochDay,
			SUPPORT_PROMPT_SEPARATION_DAYS
		)
	) return false
	if (!hasElapsed(
			input.todayEpochDay,
			input.lastGitHubImpressionEpochDay,
			SUPPORT_PROMPT_SEPARATION_DAYS
		)
	) return false

	return true
}

private fun hasElapsed(todayEpochDay: Long, previousEpochDay: Long, requiredDays: Long): Boolean {
	if (previousEpochDay == NO_SUPPORT_PROMPT_EPOCH_DAY) return true
	return todayEpochDay - previousEpochDay >= requiredDays
}
