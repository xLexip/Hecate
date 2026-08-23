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

package dev.lexip.hecate.ui

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lexip.hecate.R
import dev.lexip.hecate.data.AdaptiveThreshold
import dev.lexip.hecate.ui.components.GitHubStarPromptCard
import dev.lexip.hecate.ui.components.MainSwitchPreferenceCard
import dev.lexip.hecate.ui.components.SetupRequiredCard
import dev.lexip.hecate.ui.components.ThreeDotMenu
import dev.lexip.hecate.ui.components.preferences.CustomThresholdDialog
import dev.lexip.hecate.ui.components.preferences.DetailPreferenceCard
import dev.lexip.hecate.ui.components.preferences.ProgressDetailCard
import dev.lexip.hecate.ui.components.preferences.SliderDetailCard
import dev.lexip.hecate.ui.components.preferences.TimePickerPreferenceDialog
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors
import java.util.Calendar
import kotlinx.coroutines.launch

private val ScreenHorizontalMargin = 20.dp
private val horizontalOffsetPadding = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
	uiState: MainUiState,
	currentSensorLux: Float,
	isDeviceCovered: Boolean,
	isBatterySaverActive: Boolean,
	hasWriteSecureSettingsPermission: Boolean,
	packageName: String,
	callbacks: MainScreenCallbacks
) {
	val haptic = LocalHapticFeedback.current
	val scrollState = rememberScrollState()
	val snackbarHostState = remember { SnackbarHostState() }
	val coroutineScope = rememberCoroutineScope()
	var isLargeTitleVisible by remember { mutableStateOf(true) }
	var showCustomDialog by remember { mutableStateOf(false) }
	var showNightStartPicker by remember { mutableStateOf(false) }
	var showNightEndPicker by remember { mutableStateOf(false) }
	var isAdvancedSettingsExpanded by remember {
		mutableStateOf(
			(uiState.stayDarkAtNightEnabled || uiState.wallpaperSyncEnabled) &&
					uiState.adaptiveThemeEnabled
		)
	}
	var autoScrollAdvancedSettingsTransition by remember { mutableStateOf(false) }
	val advancedSettingsTransition = updateTransition(
		targetState = isAdvancedSettingsExpanded,
		label = "Advanced settings visibility"
	)
	val setupShakeKey = remember { mutableIntStateOf(0) }
	val textShakeKey = remember { mutableIntStateOf(0) }
	val wallpaperButtonsShakeKey = remember { mutableIntStateOf(0) }
	val textOffset = rememberShakeOffset(
		key = textShakeKey.intValue,
		offsets = listOf(-3f, 3f, -2f, 2f, -1f, 1f, -0.5f, 0.5f, 0f),
		durationMillis = 80
	)
	val wallpaperButtonsOffset = rememberShakeOffset(
		key = wallpaperButtonsShakeKey.intValue,
		offsets = listOf(-4f, 4f, -3f, 3f, -1.5f, 1.5f, 0f),
		durationMillis = 60
	)

	AdvancedSettingsAutoScrollEffect(
		enabled = autoScrollAdvancedSettingsTransition,
		transition = advancedSettingsTransition,
		scrollState = scrollState,
		onFinished = { autoScrollAdvancedSettingsTransition = false }
	)

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			MainScreenTopBar(
				showCollapsedTitle = !isLargeTitleVisible,
				uiState = uiState,
				packageName = packageName,
				onShowCustomThresholdDialog = { showCustomDialog = true }
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.consumeWindowInsets(innerPadding)
				.padding(horizontal = ScreenHorizontalMargin)
				.verticalScroll(scrollState),
			verticalArrangement = Arrangement.spacedBy(29.dp)
		) {
			MainHeader(
				textOffset = textOffset,
				onLargeTitleVisibilityChanged = { isLargeTitleVisible = it }
			)
			StatusWarnings(
				showBatterySaverWarning = isBatterySaverActive && uiState.adaptiveThemeEnabled,
				showDeviceCoveredWarning = isDeviceCovered && uiState.adaptiveThemeEnabled
			)
			ServiceControls(
				uiState = uiState,
				hasPermission = hasWriteSecureSettingsPermission,
				setupShakeKey = setupShakeKey.intValue,
				haptic = haptic,
				onMissingPermission = { setupShakeKey.intValue += 1 },
				onServiceToggleRequested = callbacks.onServiceToggleRequested,
				onServiceDisabled = {
					isAdvancedSettingsExpanded = false
					autoScrollAdvancedSettingsTransition = false
				}
			)
			ThresholdAndAdvancedSettings(
				uiState = uiState,
				currentSensorLux = currentSensorLux,
				wallpaperButtonsOffset = wallpaperButtonsOffset,
				transition = advancedSettingsTransition,
				haptic = haptic,
				callbacks = callbacks,
				onTextShake = { textShakeKey.intValue += 1 },
				onWallpaperButtonsShake = { wallpaperButtonsShakeKey.intValue += 1 },
				onExpand = {
					autoScrollAdvancedSettingsTransition = true
					isAdvancedSettingsExpanded = true
				},
				onCollapse = {
					autoScrollAdvancedSettingsTransition = true
					isAdvancedSettingsExpanded = false
				},
				onShowNightStartPicker = { showNightStartPicker = true },
				onShowNightEndPicker = { showNightEndPicker = true }
			)
			if (uiState.showGitHubStarPrompt &&
				!isBatterySaverActive &&
				!isDeviceCovered
			) {
				val dismissMessage = stringResource(
					R.string.github_star_prompt_dismiss_description
				)
				val cancelLabel = stringResource(R.string.action_cancel)
				GitHubStarPromptCard(
					onImpression = callbacks.onGitHubStarPromptImpression,
					onDismiss = {
						callbacks.onDismissGitHubStarPrompt()
						coroutineScope.launch {
							val result = snackbarHostState.showSnackbar(
								message = dismissMessage,
								actionLabel = cancelLabel,
								withDismissAction = true
							)
							if (result == SnackbarResult.ActionPerformed) {
								callbacks.onUndoGitHubStarPromptDismissal()
							}
						}
					},
					onOpenGitHub = callbacks.onOpenGitHubRepository
				)
			}
			Spacer(modifier = Modifier.padding(bottom = 4.dp))
		}
	}

	MainScreenDialogs(
		uiState = uiState,
		showCustomDialog = showCustomDialog,
		showNightStartPicker = showNightStartPicker,
		showNightEndPicker = showNightEndPicker,
		callbacks = callbacks,
		onDismissCustomDialog = { showCustomDialog = false },
		onDismissNightStartPicker = { showNightStartPicker = false },
		onDismissNightEndPicker = { showNightEndPicker = false }
	)
}

@Composable
private fun rememberShakeOffset(
	key: Int,
	offsets: List<Float>,
	durationMillis: Int
): Float {
	val animation = remember { Animatable(0f) }
	LaunchedEffect(key) {
		if (key > 0) {
			offsets.forEach { offset ->
				animation.animateTo(offset, animationSpec = tween(durationMillis))
			}
		}
	}
	return animation.value
}

@Composable
private fun AdvancedSettingsAutoScrollEffect(
	enabled: Boolean,
	transition: Transition<Boolean>,
	scrollState: androidx.compose.foundation.ScrollState,
	onFinished: () -> Unit
) {
	LaunchedEffect(enabled) {
		if (enabled) {
			withFrameNanos { }
			snapshotFlow {
				Triple(
					transition.isRunning,
					transition.currentState == transition.targetState,
					scrollState.maxValue
				)
			}.collect { (isRunning, isSettled, maxScrollValue) ->
				scrollState.scrollTo(maxScrollValue)
				if (!isRunning && isSettled) {
					scrollState.scrollTo(scrollState.maxValue)
					onFinished()
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenTopBar(
	showCollapsedTitle: Boolean,
	uiState: MainUiState,
	packageName: String,
	onShowCustomThresholdDialog: () -> Unit
) {
	TopAppBar(
		modifier = Modifier
			.padding(start = ScreenHorizontalMargin - 8.dp)
			.padding(top = 12.dp),
		colors = hecateTopAppBarColors(),
		title = {
			AnimatedVisibility(
				visible = showCollapsedTitle,
				enter = fadeIn(animationSpec = tween(180)) +
						slideInHorizontally(
							initialOffsetX = { fullWidth -> -fullWidth / 2 },
							animationSpec = spring(
								dampingRatio = Spring.DampingRatioMediumBouncy,
								stiffness = Spring.StiffnessMediumLow
							)
						),
				exit = fadeOut(animationSpec = tween(120)) +
						slideOutHorizontally(
							targetOffsetX = { fullWidth -> -fullWidth / 3 },
							animationSpec = tween(160)
						)
			) {
				Text(
					text = stringResource(id = R.string.app_name),
					style = MaterialTheme.typography.titleLarge
				)
			}
		},
		actions = {
			ThreeDotMenu(
				isAdaptiveThemeEnabled = uiState.adaptiveThemeEnabled,
				packageName = packageName,
				isInstalledFromPlayStore = uiState.isInstalledFromPlayStore,
				onShowCustomThresholdDialog = onShowCustomThresholdDialog
			)
		}
	)
}

@Composable
private fun MainHeader(
	textOffset: Float,
	onLargeTitleVisibilityChanged: (Boolean) -> Unit
) {
	Text(
		modifier = Modifier
			.padding(horizontal = horizontalOffsetPadding)
			.padding(top = 12.dp)
			.onGloballyPositioned { coordinates ->
				onLargeTitleVisibilityChanged(coordinates.boundsInWindow().bottom > 0f)
			},
		text = stringResource(id = R.string.app_name),
		style = MaterialTheme.typography.displaySmall
	)
	Column {
		Text(
			modifier = Modifier.padding(horizontal = horizontalOffsetPadding),
			text = stringResource(id = R.string.description_service_purpose),
			style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
		)
		Spacer(modifier = Modifier.padding(top = 8.dp))
		Text(
			modifier = Modifier
				.padding(horizontal = horizontalOffsetPadding)
				.offset { IntOffset(textOffset.dp.roundToPx(), 0) },
			text = stringResource(id = R.string.description_switching_conditions),
			style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)
		)
	}
}

@Composable
private fun StatusWarnings(
	showBatterySaverWarning: Boolean,
	showDeviceCoveredWarning: Boolean
) {
	AnimatedVisibility(
		visible = showBatterySaverWarning,
		enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
		exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
	) {
		StatusWarningCard(
			title = stringResource(id = R.string.battery_saver_title),
			message = stringResource(id = R.string.battery_saver_message),
			isError = false
		)
	}
	AnimatedVisibility(
		visible = showDeviceCoveredWarning && !showBatterySaverWarning,
		enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
		exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
	) {
		StatusWarningCard(
			title = stringResource(id = R.string.device_covered_title),
			message = stringResource(id = R.string.device_covered_message),
			isError = true
		)
	}
}

@Composable
private fun StatusWarningCard(title: String, message: String, isError: Boolean) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = if (isError) {
				MaterialTheme.colorScheme.errorContainer
			} else {
				MaterialTheme.colorScheme.tertiaryContainer
			},
			contentColor = if (isError) {
				MaterialTheme.colorScheme.onErrorContainer
			} else {
				MaterialTheme.colorScheme.onTertiaryContainer
			}
		),
		shape = RoundedCornerShape(20.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(text = title, style = MaterialTheme.typography.titleMedium)
			Spacer(modifier = Modifier.padding(top = 4.dp))
			Text(text = message, style = MaterialTheme.typography.bodyMedium)
		}
	}
}

@Composable
private fun ServiceControls(
	uiState: MainUiState,
	hasPermission: Boolean,
	setupShakeKey: Int,
	haptic: HapticFeedback,
	onMissingPermission: () -> Unit,
	onServiceToggleRequested: (Boolean, Boolean) -> Boolean,
	onServiceDisabled: () -> Unit
) {
	if (!hasPermission) {
		SetupRequiredCard(
			modifier = Modifier.fillMaxWidth(),
			title = stringResource(id = R.string.setup_required_title),
			message = stringResource(
				id = R.string.setup_required_message,
				stringResource(id = R.string.app_name)
			),
			onLaunchSetup = { onServiceToggleRequested(true, false) },
			shakeKey = setupShakeKey
		)
	}
	MainSwitchPreferenceCard(
		text = stringResource(
			id = R.string.action_use_adaptive_theme,
			stringResource(id = R.string.app_name)
		),
		isChecked = uiState.adaptiveThemeEnabled,
		onCheckedChange = { checked ->
			handleServiceToggle(
				checked = checked,
				hasPermission = hasPermission,
				haptic = haptic,
				onMissingPermission = onMissingPermission,
				onServiceToggleRequested = onServiceToggleRequested,
				onServiceDisabled = onServiceDisabled
			)
		}
	)
}

private fun handleServiceToggle(
	checked: Boolean,
	hasPermission: Boolean,
	haptic: HapticFeedback,
	onMissingPermission: () -> Unit,
	onServiceToggleRequested: (Boolean, Boolean) -> Boolean,
	onServiceDisabled: () -> Unit
) {
	if (checked && !hasPermission) {
		onMissingPermission()
		haptic.performHapticFeedback(HapticFeedbackType.Reject)
		return
	}
	if (!onServiceToggleRequested(checked, hasPermission)) {
		haptic.performHapticFeedback(HapticFeedbackType.Reject)
		return
	}
	if (!checked) {
		onServiceDisabled()
	}
	haptic.performHapticFeedback(
		if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
	)
}

@Composable
private fun ThresholdAndAdvancedSettings(
	uiState: MainUiState,
	currentSensorLux: Float,
	wallpaperButtonsOffset: Float,
	transition: Transition<Boolean>,
	haptic: HapticFeedback,
	callbacks: MainScreenCallbacks,
	onTextShake: () -> Unit,
	onWallpaperButtonsShake: () -> Unit,
	onExpand: () -> Unit,
	onCollapse: () -> Unit,
	onShowNightStartPicker: () -> Unit,
	onShowNightEndPicker: () -> Unit
) {
	val customLabel = stringResource(id = R.string.adaptive_threshold_custom)
	val currentThresholdIndex =
		AdaptiveThreshold.fromLux(uiState.adaptiveThemeThresholdLux).ordinal
	val labels = AdaptiveThreshold.entries.mapIndexed { index, threshold ->
		if (uiState.customAdaptiveThemeThresholdLux != null && index == currentThresholdIndex) {
			customLabel
		} else {
			stringResource(id = threshold.labelRes)
		}
	}
	val baseLux = AdaptiveThreshold.entries.map { it.lux }
	val lux = uiState.customAdaptiveThemeThresholdLux?.let { customLux ->
		baseLux.mapIndexed { index, value ->
			if (index == currentThresholdIndex) customLux else value
		}
	} ?: baseLux

	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		ThresholdCards(
			uiState = uiState,
			currentSensorLux = currentSensorLux,
			currentThresholdIndex = currentThresholdIndex,
			labels = labels,
			lux = lux,
			isSystemDark = isSystemInDarkTheme(),
			transition = transition,
			callbacks = callbacks,
			onTextShake = onTextShake
		)
		CollapsedAdvancedSettingsControl(
			transition = transition,
			enabled = uiState.adaptiveThemeEnabled,
			showActiveFeatureIndicator =
				uiState.stayDarkAtNightEnabled || uiState.wallpaperSyncEnabled,
			haptic = haptic,
			onExpand = onExpand,
			onCheckReviewPrompt = callbacks.onCheckReviewPrompt
		)
		ExpandedAdvancedSettings(
			uiState = uiState,
			wallpaperButtonsOffset = wallpaperButtonsOffset,
			transition = transition,
			haptic = haptic,
			callbacks = callbacks,
			onWallpaperButtonsShake = onWallpaperButtonsShake,
			onCollapse = onCollapse,
			onShowNightStartPicker = onShowNightStartPicker,
			onShowNightEndPicker = onShowNightEndPicker
		)
	}
}

@Composable
private fun ThresholdCards(
	uiState: MainUiState,
	currentSensorLux: Float,
	currentThresholdIndex: Int,
	labels: List<String>,
	lux: List<Float>,
	isSystemDark: Boolean,
	transition: Transition<Boolean>,
	callbacks: MainScreenCallbacks,
	onTextShake: () -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		SliderDetailCard(
			title = stringResource(id = R.string.title_brightness_threshold),
			valueIndex = currentThresholdIndex,
			steps = labels.size,
			labels = labels,
			lux = lux,
			onValueChange = { index ->
				callbacks.onThresholdSelected(index, lux[index])
				if ((currentSensorLux > lux[index]) == isSystemDark) {
					onTextShake()
				}
			},
			enabled = uiState.adaptiveThemeEnabled,
			firstCard = true,
			lastCard = false
		)
		ProgressDetailCard(
			title = stringResource(id = R.string.title_current_brightness),
			currentLux = currentSensorLux,
			luxSteps = lux,
			enabled = uiState.adaptiveThemeEnabled,
			firstCard = false,
			lastCard = !transition.currentState && !transition.targetState
		)
	}
}

@Composable
private fun CollapsedAdvancedSettingsControl(
	transition: Transition<Boolean>,
	enabled: Boolean,
	showActiveFeatureIndicator: Boolean,
	haptic: HapticFeedback,
	onExpand: () -> Unit,
	onCheckReviewPrompt: () -> Unit
) {
	transition.AnimatedVisibility(
		visible = { expanded -> !expanded },
		enter = fadeIn(animationSpec = tween(220)) +
				expandVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)),
		exit = fadeOut(animationSpec = tween(150)) +
				shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 8.dp),
			horizontalArrangement = Arrangement.Center
		) {
			AssistChip(
				onClick = {
					onExpand()
					haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
					onCheckReviewPrompt()
				},
				enabled = enabled,
				shape = RoundedCornerShape(20.dp),
				label = {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(text = stringResource(id = R.string.action_advanced_settings))
						if (showActiveFeatureIndicator) {
							Badge(
								modifier = Modifier.padding(start = 6.dp),
								containerColor = MaterialTheme.colorScheme.primary
							)
						}
					}
				},
				leadingIcon = {
					Icon(
						imageVector = Icons.Filled.KeyboardArrowDown,
						contentDescription = null
					)
				}
			)
		}
	}
}

@Composable
private fun ExpandedAdvancedSettings(
	uiState: MainUiState,
	wallpaperButtonsOffset: Float,
	transition: Transition<Boolean>,
	haptic: HapticFeedback,
	callbacks: MainScreenCallbacks,
	onWallpaperButtonsShake: () -> Unit,
	onCollapse: () -> Unit,
	onShowNightStartPicker: () -> Unit,
	onShowNightEndPicker: () -> Unit
) {
	transition.AnimatedVisibility(
		visible = { expanded -> expanded },
		enter = fadeIn(animationSpec = tween(220)) +
				expandVertically(animationSpec = tween(360, easing = FastOutSlowInEasing)),
		exit = fadeOut(animationSpec = tween(150)) +
				shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing))
	) {
		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			NightWindowPreference(
				uiState = uiState,
				onStayDarkAtNightChanged = callbacks.onStayDarkAtNightChanged,
				onShowNightStartPicker = onShowNightStartPicker,
				onShowNightEndPicker = onShowNightEndPicker
			)
			WallpaperSyncPreference(
				uiState = uiState,
				wallpaperButtonsOffset = wallpaperButtonsOffset,
				haptic = haptic,
				callbacks = callbacks,
				onWallpaperButtonsShake = onWallpaperButtonsShake
			)
			LockScreenWallpaperBlurPreference(
				uiState = uiState,
				onLockScreenWallpaperBlurChanged = callbacks.onLockScreenWallpaperBlurChanged
			)
			AssistChip(
				modifier = Modifier.align(Alignment.CenterHorizontally),
				onClick = {
					onCollapse()
					haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
				},
				enabled = uiState.adaptiveThemeEnabled,
				shape = RoundedCornerShape(20.dp),
				label = { Text(text = stringResource(id = R.string.action_collapse)) },
				leadingIcon = {
					Icon(
						imageVector = Icons.Filled.KeyboardArrowUp,
						contentDescription = null
					)
				}
			)
		}
	}
}

@Composable
private fun NightWindowPreference(
	uiState: MainUiState,
	onStayDarkAtNightChanged: (Boolean) -> Unit,
	onShowNightStartPicker: () -> Unit,
	onShowNightEndPicker: () -> Unit
) {
	val context = LocalContext.current
	DetailPreferenceCard(
		title = stringResource(id = R.string.title_night_dark_lock),
		enabled = uiState.adaptiveThemeEnabled,
		firstCard = false,
		lastCard = false,
		toggleableValue = uiState.stayDarkAtNightEnabled,
		onToggle = onStayDarkAtNightChanged
	) {
		PreferenceDescriptionSwitch(
			description = stringResource(id = R.string.description_night_dark_lock),
			checked = uiState.stayDarkAtNightEnabled,
			enabled = uiState.adaptiveThemeEnabled,
			onCheckedChange = null
		)
		if (uiState.stayDarkAtNightEnabled && uiState.adaptiveThemeEnabled) {
			NightWindowButtons(
				startText = formatMinutesAsLocalTime(context, uiState.nightStartMinutes),
				endText = formatMinutesAsLocalTime(context, uiState.nightEndMinutes),
				onShowNightStartPicker = onShowNightStartPicker,
				onShowNightEndPicker = onShowNightEndPicker
			)
		}
	}
}

@Composable
private fun NightWindowButtons(
	startText: String,
	endText: String,
	onShowNightStartPicker: () -> Unit,
	onShowNightEndPicker: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		OutlinedButton(modifier = Modifier.weight(1f), onClick = onShowNightStartPicker) {
			Text(text = stringResource(id = R.string.action_night_from_time, startText))
		}
		OutlinedButton(modifier = Modifier.weight(1f), onClick = onShowNightEndPicker) {
			Text(text = stringResource(id = R.string.action_night_to_time, endText))
		}
	}
}

@Composable
private fun WallpaperSyncPreference(
	uiState: MainUiState,
	wallpaperButtonsOffset: Float,
	haptic: HapticFeedback,
	callbacks: MainScreenCallbacks,
	onWallpaperButtonsShake: () -> Unit
) {
	val isDaySet = !uiState.dayWallpaperUri.isNullOrEmpty()
	val isNightSet = !uiState.nightWallpaperUri.isNullOrEmpty()
	val bothWallpapersSet = isDaySet && isNightSet
	val requestToggle = { enabled: Boolean ->
		requestWallpaperSyncToggle(
			enabled = enabled,
			bothWallpapersSet = bothWallpapersSet,
			haptic = haptic,
			onWallpaperButtonsShake = onWallpaperButtonsShake,
			onToggleRequested = callbacks.onWallpaperSyncToggleRequested
		)
	}

	DetailPreferenceCard(
		title = stringResource(id = R.string.title_device_wallpaper_sync),
		enabled = uiState.adaptiveThemeEnabled,
		firstCard = false,
		lastCard = false,
		toggleableValue = uiState.wallpaperSyncEnabled,
		onToggle = requestToggle,
		titleTrailingContent = { BetaLabel() }
	) {
		PreferenceDescriptionSwitch(
			description = stringResource(id = R.string.description_device_wallpaper_sync),
			checked = uiState.wallpaperSyncEnabled,
			enabled = uiState.adaptiveThemeEnabled,
			onCheckedChange = requestToggle,
			switchTopPadding = 20.dp
		)
		WallpaperSelectionButtons(
			isDaySet = isDaySet,
			isNightSet = isNightSet,
			enabled = uiState.adaptiveThemeEnabled,
			offset = wallpaperButtonsOffset,
			onSelectDayWallpaper = callbacks.onSelectDayWallpaper,
			onSelectNightWallpaper = callbacks.onSelectNightWallpaper
		)
	}
}

@Composable
private fun LockScreenWallpaperBlurPreference(
	uiState: MainUiState,
	onLockScreenWallpaperBlurChanged: (Boolean) -> Unit
) {
	DetailPreferenceCard(
		title = stringResource(id = R.string.title_lock_screen_wallpaper_blur),
		enabled = uiState.adaptiveThemeEnabled && uiState.wallpaperSyncEnabled,
		firstCard = false,
		lastCard = true,
		toggleableValue = uiState.lockScreenWallpaperBlurEnabled,
		onToggle = onLockScreenWallpaperBlurChanged,
		cardTrailingContent = {
			PreferenceSwitch(
				checked = uiState.lockScreenWallpaperBlurEnabled,
				enabled = uiState.adaptiveThemeEnabled && uiState.wallpaperSyncEnabled,
				onCheckedChange = onLockScreenWallpaperBlurChanged,
				modifier = Modifier.padding(start = 14.dp, end = 4.dp)
			)
		}
	) {
		Text(
			modifier = Modifier.padding(top = 4.dp),
			text = stringResource(id = R.string.description_lock_screen_wallpaper_blur),
			style = MaterialTheme.typography.bodyMedium
		)
	}
}

private fun requestWallpaperSyncToggle(
	enabled: Boolean,
	bothWallpapersSet: Boolean,
	haptic: HapticFeedback,
	onWallpaperButtonsShake: () -> Unit,
	onToggleRequested: (Boolean) -> Unit
) {
	if (enabled && !bothWallpapersSet) {
		onWallpaperButtonsShake()
		haptic.performHapticFeedback(HapticFeedbackType.Reject)
	} else {
		onToggleRequested(enabled)
	}
}

@Composable
private fun PreferenceDescriptionSwitch(
	description: String,
	checked: Boolean,
	enabled: Boolean,
	onCheckedChange: ((Boolean) -> Unit)?,
	switchTopPadding: Dp = 0.dp
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 4.dp),
		verticalAlignment = Alignment.Top,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Text(
			text = description,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.weight(1f)
		)
		PreferenceSwitch(
			checked = checked,
			enabled = enabled,
			onCheckedChange = onCheckedChange,
			modifier = Modifier
				.padding(start = 14.dp, top = switchTopPadding, end = 4.dp)
				.offset(y = (-6).dp)
				.align(Alignment.Top)
		)
	}
}

@Composable
private fun PreferenceSwitch(
	checked: Boolean,
	enabled: Boolean,
	onCheckedChange: ((Boolean) -> Unit)?,
	modifier: Modifier = Modifier
) {
	Switch(
		modifier = modifier,
		checked = checked,
		enabled = enabled,
		onCheckedChange = onCheckedChange,
		thumbContent = {
			Icon(
				imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Clear,
				contentDescription = null,
				modifier = Modifier.size(SwitchDefaults.IconSize)
			)
		}
	)
}

@Composable
private fun BetaLabel() {
	Surface(
		modifier = Modifier.padding(start = 8.dp),
		shape = RoundedCornerShape(50),
		color = MaterialTheme.colorScheme.tertiaryContainer,
		contentColor = MaterialTheme.colorScheme.onTertiaryContainer
	) {
		Text(
			text = stringResource(id = R.string.label_beta),
			modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
			style = MaterialTheme.typography.labelSmall
		)
	}
}

@Composable
private fun WallpaperSelectionButtons(
	isDaySet: Boolean,
	isNightSet: Boolean,
	enabled: Boolean,
	offset: Float,
	onSelectDayWallpaper: () -> Unit,
	onSelectNightWallpaper: () -> Unit
) {
	val dayStatus = stringResource(
		if (isDaySet) R.string.wallpaper_status_set else R.string.wallpaper_status_not_set
	)
	val nightStatus = stringResource(
		if (isNightSet) R.string.wallpaper_status_set else R.string.wallpaper_status_not_set
	)
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 8.dp)
			.offset { IntOffset(offset.dp.roundToPx(), 0) },
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		WallpaperSelectionButton(
			text = "${stringResource(id = R.string.action_select_night_wallpaper)}\n$nightStatus",
			enabled = enabled,
			onClick = onSelectNightWallpaper
		)
		WallpaperSelectionButton(
			text = "${stringResource(id = R.string.action_select_day_wallpaper)}\n$dayStatus",
			enabled = enabled,
			onClick = onSelectDayWallpaper
		)
	}
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.WallpaperSelectionButton(
	text: String,
	enabled: Boolean,
	onClick: () -> Unit
) {
	OutlinedButton(
		modifier = Modifier.weight(1f),
		enabled = enabled,
		onClick = onClick
	) {
		Text(text = text, textAlign = TextAlign.Center)
	}
}

@Composable
private fun MainScreenDialogs(
	uiState: MainUiState,
	showCustomDialog: Boolean,
	showNightStartPicker: Boolean,
	showNightEndPicker: Boolean,
	callbacks: MainScreenCallbacks,
	onDismissCustomDialog: () -> Unit,
	onDismissNightStartPicker: () -> Unit,
	onDismissNightEndPicker: () -> Unit
) {
	val context = LocalContext.current
	CustomThresholdDialog(
		show = showCustomDialog,
		currentLux = uiState.customAdaptiveThemeThresholdLux ?: uiState.adaptiveThemeThresholdLux,
		onConfirm = { luxValue ->
			callbacks.onCustomThresholdConfirmed(luxValue)
			onDismissCustomDialog()
			showThresholdEasterEgg(context, luxValue)
		},
		onDismiss = onDismissCustomDialog
	)
	NightTimePickerDialogs(
		context = context,
		uiState = uiState,
		showNightStartPicker = showNightStartPicker,
		showNightEndPicker = showNightEndPicker,
		onNightWindowChanged = callbacks.onNightWindowChanged,
		onDismissNightStartPicker = onDismissNightStartPicker,
		onDismissNightEndPicker = onDismissNightEndPicker
	)
	if (uiState.showLiveWallpaperWarningDialog) {
		LiveWallpaperWarningDialog(callbacks)
	}
}

private fun showThresholdEasterEgg(context: Context, luxValue: Float) {
	if (luxValue.toInt() == 42) {
		Toast.makeText(
			context,
			context.getString(R.string.easter_egg_answer),
			Toast.LENGTH_LONG
		).show()
	}
}

@Composable
private fun NightTimePickerDialogs(
	context: Context,
	uiState: MainUiState,
	showNightStartPicker: Boolean,
	showNightEndPicker: Boolean,
	onNightWindowChanged: (Int, Int, (() -> Unit)?) -> Unit,
	onDismissNightStartPicker: () -> Unit,
	onDismissNightEndPicker: () -> Unit
) {
	TimePickerPreferenceDialog(
		show = showNightStartPicker,
		title = stringResource(id = R.string.title_night_start_time_picker),
		initialMinutes = uiState.nightStartMinutes,
		onConfirm = { selectedMinutes ->
			onNightWindowChanged(
				selectedMinutes,
				uiState.nightEndMinutes,
				{ showInvalidNightPeriodToast(context) }
			)
			onDismissNightStartPicker()
		},
		onDismiss = onDismissNightStartPicker
	)
	TimePickerPreferenceDialog(
		show = showNightEndPicker,
		title = stringResource(id = R.string.title_night_end_time_picker),
		initialMinutes = uiState.nightEndMinutes,
		onConfirm = { selectedMinutes ->
			onNightWindowChanged(
				uiState.nightStartMinutes,
				selectedMinutes,
				{ showInvalidNightPeriodToast(context) }
			)
			onDismissNightEndPicker()
		},
		onDismiss = onDismissNightEndPicker
	)
}

private fun showInvalidNightPeriodToast(context: Context) {
	Toast.makeText(context, R.string.error_invalid_night_period, Toast.LENGTH_SHORT).show()
}

@Composable
private fun LiveWallpaperWarningDialog(callbacks: MainScreenCallbacks) {
	AlertDialog(
		onDismissRequest = callbacks.onDismissLiveWallpaperWarning,
		title = { Text(text = stringResource(id = R.string.live_wallpaper_warning_title)) },
		text = { Text(text = stringResource(id = R.string.live_wallpaper_warning_message)) },
		confirmButton = {
			TextButton(onClick = callbacks.onConfirmLiveWallpaper) {
				Text(text = stringResource(id = R.string.action_continue))
			}
		},
		dismissButton = {
			TextButton(onClick = callbacks.onDismissLiveWallpaperWarning) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		}
	)
}

private fun formatMinutesAsLocalTime(context: Context, totalMinutes: Int): String {
	val formatter = DateFormat.getTimeFormat(context)
	val calendar = Calendar.getInstance().apply {
		set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
		set(Calendar.MINUTE, totalMinutes % 60)
		set(Calendar.SECOND, 0)
		set(Calendar.MILLISECOND, 0)
	}
	return formatter.format(calendar.time)
}
