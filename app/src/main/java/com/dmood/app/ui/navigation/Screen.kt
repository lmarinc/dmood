package com.dmood.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object DecisionEditor : Screen("decision_editor?decisionId={decisionId}") {
        fun createRoute(decisionId: Long? = null): String {
            val targetId = decisionId ?: 0L
            return "decision_editor?decisionId=$targetId"
        }
    }
    object WeeklySummary : Screen("weekly_summary")
    object WeeklyHistory : Screen("weekly_history")
    object Settings : Screen("settings")
    object Faq : Screen("faq")
}
