package com.dmood.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dmood.app.ui.screen.decision.DecisionEditorScreen
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
                    navController.navigate(Screen.DecisionEditor.route)
                },
                onOpenSummaryClick = {
                    navController.navigate(Screen.WeeklySummary.route)
                }
            )
        }
        composable(Screen.DecisionEditor.route) {
            DecisionEditorScreen(
                onClose = {
                    navController.popBackStack()
                }
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
