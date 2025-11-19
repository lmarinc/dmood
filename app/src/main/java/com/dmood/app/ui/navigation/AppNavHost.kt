package com.dmood.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dmood.app.ui.DmoodViewModelFactory
import com.dmood.app.ui.screen.decision.DecisionEditorScreen
import com.dmood.app.ui.screen.decision.DecisionEditorViewModel
import com.dmood.app.ui.screen.home.HomeScreen
import com.dmood.app.ui.screen.onboarding.OnboardingScreen
import com.dmood.app.ui.screen.settings.SettingsScreen
import com.dmood.app.ui.screen.summary.WeeklySummaryScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onAddDecisionClick = {
                    navController.navigate(Screen.DecisionEditor.buildRoute())
                },
                onOpenSummaryClick = {
                    navController.navigate(Screen.WeeklySummary.route)
                },
                onDecisionClick = { decisionId ->
                    navController.navigate(Screen.DecisionEditor.buildRoute(decisionId))
                }
            )
        }
        composable(
            route = Screen.DecisionEditor.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.DecisionEditor.decisionIdArg) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val decisionId = backStackEntry.arguments
                ?.getLong(Screen.DecisionEditor.decisionIdArg)
                ?.takeIf { it != -1L }
            val editorViewModel: DecisionEditorViewModel = viewModel(factory = DmoodViewModelFactory)

            LaunchedEffect(decisionId) {
                if (decisionId != null) {
                    editorViewModel.loadDecision(decisionId)
                }
            }

            DecisionEditorScreen(
                onClose = {
                    navController.popBackStack()
                },
                viewModel = editorViewModel
            )
        }
        composable(Screen.WeeklySummary.route) {
            WeeklySummaryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
