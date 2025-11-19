package com.dmood.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object DecisionEditor : Screen("decision_editor") {
        const val decisionIdArg = "decisionId"
        val routeWithArgs = "${route}?$decisionIdArg={${decisionIdArg}}"

        fun buildRoute(decisionId: Long? = null): String {
            return decisionId?.let { "${route}?$decisionIdArg=$it" } ?: route
        }
    }
    object WeeklySummary : Screen("weekly_summary")
    object Settings : Screen("settings")
}
