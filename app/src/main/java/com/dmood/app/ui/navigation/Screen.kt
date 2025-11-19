package com.dmood.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object DecisionEditor : Screen("decision_editor")
    object WeeklySummary : Screen("weekly_summary")
    object Settings : Screen("settings")
}
