package com.dmood.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dmood.app.ui.screen.decision.DecisionEditorScreen
import com.dmood.app.ui.screen.faq.FaqScreen
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
                    navController.navigate(Screen.DecisionEditor.createRoute(null))
                },
                onOpenSummaryClick = {
                    navController.navigate(Screen.WeeklySummary.route)
                },
                onDecisionClick = { decisionId ->
                    navController.navigate(Screen.DecisionEditor.createRoute(decisionId))
                }
            )
        }
        composable(
            route = Screen.DecisionEditor.route,
            arguments = listOf(
                navArgument("decisionId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { entry ->
            val decisionId = entry.arguments?.getLong("decisionId") ?: 0L
            DecisionEditorScreen(
                decisionId = decisionId,
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
        composable(Screen.Faq.route) {
            FaqScreen()
        }
    }
}
