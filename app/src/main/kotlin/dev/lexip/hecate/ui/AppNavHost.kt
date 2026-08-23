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

package dev.lexip.hecate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dev.lexip.hecate.Application
import dev.lexip.hecate.R
import dev.lexip.hecate.data.UserPreferencesRepository
import dev.lexip.hecate.ui.navigation.DeepLinks
import dev.lexip.hecate.ui.navigation.MainRoute
import dev.lexip.hecate.ui.navigation.NavTransitions.fadeEnter
import dev.lexip.hecate.ui.navigation.NavTransitions.fadeExit
import dev.lexip.hecate.ui.navigation.NavTransitions.slideInFromEnd
import dev.lexip.hecate.ui.navigation.NavTransitions.slideInFromStart
import dev.lexip.hecate.ui.navigation.NavTransitions.slideOutToEnd
import dev.lexip.hecate.ui.navigation.NavTransitions.slideOutToStart
import dev.lexip.hecate.ui.navigation.NavigationEvent
import dev.lexip.hecate.ui.navigation.NavigationManager
import dev.lexip.hecate.ui.navigation.SetupGraph
import dev.lexip.hecate.ui.navigation.SetupRoute
import dev.lexip.hecate.ui.setup.SetupViewModel
import dev.lexip.hecate.ui.setup.SetupViewModelFactory
import dev.lexip.hecate.ui.setup.screens.A_DeveloperModeScreen
import dev.lexip.hecate.ui.setup.screens.B_ConnectUsbScreen
import dev.lexip.hecate.ui.setup.screens.C_GrantPermissionScreen
import dev.lexip.hecate.ui.setup.shareText

@Composable
fun AppNavHost(
	mainViewModel: MainViewModel,
	uiState: MainUiState,
	navigationManager: NavigationManager
) {
	val navController = rememberNavController()
	val context = LocalContext.current
	val shareSetupTitle = stringResource(R.string.title_share_setup)

	// Handle navigation events from ViewModel via NavigationManager
	LaunchedEffect(Unit) {
		navigationManager.navigationEvents.collect { event ->
			when (event) {
				is NavigationEvent.ToSetup -> {
					navController.navigate(SetupGraph)
				}

				is NavigationEvent.Back -> {
					navController.popBackStack()
				}

				is NavigationEvent.ToMainClearingSetup -> {
					navController.popBackStack(MainRoute, inclusive = false)
				}

				is NavigationEvent.ToSetupStep -> {
					// Navigate to specific setup step within the graph
					when (event.step) {
						SetupRoute.DeveloperMode -> navController.navigate(SetupRoute.DeveloperMode) {
							popUpTo(SetupRoute.DeveloperMode) { inclusive = true }
						}

						SetupRoute.ConnectUsb -> navController.navigate(SetupRoute.ConnectUsb)
						SetupRoute.GrantPermission -> navController.navigate(SetupRoute.GrantPermission)
					}
				}
			}
		}
	}

	// Handle legacy UI events from MainViewModel
	LaunchedEffect(Unit) {
		mainViewModel.uiEvents.collect { event ->
			when (event) {
				is NavigateToSetup -> {
					navController.navigate(SetupGraph)
				}

				is CopyToClipboard -> {
					// Let UI handle copying to clipboard if needed
				}

				is RequestInAppReview -> {
					// Handled by MainScreen
				}
			}
		}
	}

	NavHost(
		navController = navController,
		startDestination = MainRoute,
		enterTransition = { fadeEnter() },
		exitTransition = { fadeExit() }
	) {
		// Main screen
		composable<MainRoute>(
			deepLinks = listOf(
				navDeepLink<MainRoute>(basePath = DeepLinks.MAIN)
			)
		) {
			MainScreen(
				uiState = uiState,
				mainViewModel = mainViewModel
			)
		}

		// Setup flow nested graph
		setupNavGraph(
			context = context,
			shareSetupTitle = shareSetupTitle,
			navigationManager = navigationManager,
			navController = navController
		)
	}
}

/**
 * Setup navigation graph with shared ViewModel scoped to the graph.
 * Includes deep link support for all setup steps.
 */
private fun NavGraphBuilder.setupNavGraph(
	context: android.content.Context,
	shareSetupTitle: String,
	navigationManager: NavigationManager,
	navController: NavHostController
) {
	navigation<SetupGraph>(
		startDestination = SetupRoute.DeveloperMode,
		deepLinks = listOf(
			navDeepLink<SetupGraph>(basePath = DeepLinks.SETUP)
		)
	) {
		// Developer Mode Step
		composable<SetupRoute.DeveloperMode>(
			deepLinks = listOf(
				navDeepLink<SetupRoute.DeveloperMode>(basePath = DeepLinks.SETUP_DEVELOPER)
			),
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) { backStackEntry ->
			// Scope ViewModel to the SetupGraph to share state across steps
			val graphEntry = remember(backStackEntry) {
				navController.getBackStackEntry(SetupGraph)
			}
			val setupViewModel = rememberSetupViewModel(
				backStackEntry = graphEntry,
				context = context,
				navigationManager = navigationManager
			)
			val setupUiState by setupViewModel.uiState.collectAsState()

			A_DeveloperModeScreen(
				uiState = setupUiState,
				onGrantViaShizuku = setupViewModel::onGrantViaShizukuRequested,
				onNext = setupViewModel::navigateToNextStep,
				onExit = setupViewModel::exitSetup,
				onOpenSettings = setupViewModel::openDeviceInfoSettings,
				onOpenDeveloperSettings = setupViewModel::openDeveloperSettings
			)
		}

		// Connect USB Step
		composable<SetupRoute.ConnectUsb>(
			deepLinks = listOf(
				navDeepLink<SetupRoute.ConnectUsb>(basePath = DeepLinks.SETUP_USB)
			),
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) { backStackEntry ->
			// Scope ViewModel to the SetupGraph
			val graphEntry = remember(backStackEntry) {
				navController.getBackStackEntry(SetupGraph)
			}
			val setupViewModel = rememberSetupViewModel(
				backStackEntry = graphEntry,
				context = context,
				navigationManager = navigationManager
			)
			val setupUiState by setupViewModel.uiState.collectAsState()

			B_ConnectUsbScreen(
				uiState = setupUiState,
				onGrantViaShizuku = setupViewModel::onGrantViaShizukuRequested,
				onNext = setupViewModel::navigateToNextStep,
				onBack = setupViewModel::navigateBack,
				onShareExpertCommand = setupViewModel::shareAdbCommand,
				onUseRoot = setupViewModel::onGrantViaRootRequested,
				onInstallShizuku = setupViewModel::installShizuku
			)
		}

		// Grant Permission Step
		composable<SetupRoute.GrantPermission>(
			deepLinks = listOf(
				navDeepLink<SetupRoute.GrantPermission>(basePath = DeepLinks.SETUP_PERMISSION)
			),
			enterTransition = { slideInFromEnd() },
			exitTransition = { slideOutToStart() },
			popEnterTransition = { slideInFromStart() },
			popExitTransition = { slideOutToEnd() }
		) { backStackEntry ->
			// Scope ViewModel to the SetupGraph
			val graphEntry = remember(backStackEntry) {
				navController.getBackStackEntry(SetupGraph)
			}
			val setupViewModel = rememberSetupViewModel(
				backStackEntry = graphEntry,
				context = context,
				navigationManager = navigationManager
			)
			val setupUiState by setupViewModel.uiState.collectAsState()

			C_GrantPermissionScreen(
				uiState = setupUiState,
				onShareSetupUrl = {
					context.shareText(
						"https://lexip.dev/setup",
						shareSetupTitle
					)
				},
				onShareExpertCommand = setupViewModel::shareAdbCommand,
				onFinish = setupViewModel::checkPermissionAndComplete,
				onBack = setupViewModel::navigateBack,
				onUseRoot = setupViewModel::onGrantViaRootRequested
			)
		}
	}
}

/**
 * Creates and remembers a SetupViewModel scoped to the setup navigation graph.
 * Shared across all setup steps in the graph and manages environment monitoring (USB, etc.).
 */
@Composable
private fun rememberSetupViewModel(
	backStackEntry: NavBackStackEntry,
	context: android.content.Context,
	navigationManager: NavigationManager
): SetupViewModel {
	val application = context.applicationContext as Application
	val dataStore = application.userPreferencesDataStore

	// Use the backStackEntry as ViewModelStoreOwner to scope VM to the setup graph
	val viewModel: SetupViewModel = viewModel(
		viewModelStoreOwner = backStackEntry,
		factory = SetupViewModelFactory(
			application = application,
			userPreferencesRepository = UserPreferencesRepository(dataStore),
			navigationManager = navigationManager
		)
	)


	return viewModel
}

