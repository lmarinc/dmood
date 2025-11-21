package com.dmood.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dmood.app.di.DmoodServiceLocator
import com.dmood.app.ui.screen.decision.DecisionEditorViewModel
import com.dmood.app.ui.screen.home.HomeViewModel
import com.dmood.app.ui.screen.onboarding.OnboardingViewModel
import com.dmood.app.ui.screen.settings.SettingsViewModel
import com.dmood.app.ui.screen.summary.WeeklySummaryViewModel

object DmoodViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val locator = DmoodServiceLocator

        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    decisionRepository = locator.decisionRepository,
                    userPreferencesRepository = locator.userPreferencesRepository,
                    calculateWeeklyScheduleUseCase = locator.calculateWeeklyScheduleUseCase
                ) as T
            }

            modelClass.isAssignableFrom(DecisionEditorViewModel::class.java) -> {
                DecisionEditorViewModel(
                    decisionRepository = locator.decisionRepository,
                    validateDecisionUseCase = locator.validateDecisionUseCase,
                    calculateDecisionToneUseCase = locator.calculateDecisionToneUseCase
                ) as T
            }

            modelClass.isAssignableFrom(WeeklySummaryViewModel::class.java) -> {
                WeeklySummaryViewModel(
                    decisionRepository = locator.decisionRepository,
                    buildWeeklySummaryUseCase = locator.buildWeeklySummaryUseCase,
                    extractWeeklyHighlightsUseCase = locator.extractWeeklyHighlightsUseCase,
                    userPreferencesRepository = locator.userPreferencesRepository,
                    calculateWeeklyScheduleUseCase = locator.calculateWeeklyScheduleUseCase,
                    generateInsightRulesUseCase = locator.generateInsightRulesUseCase
                ) as T
            }

            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(
                    userPreferencesRepository = locator.userPreferencesRepository
                ) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    userPreferencesRepository = locator.userPreferencesRepository
                ) as T
            }

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}
