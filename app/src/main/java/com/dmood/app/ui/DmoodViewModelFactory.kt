package com.dmood.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dmood.app.di.DmoodServiceLocator
import com.dmood.app.ui.screen.decision.DecisionEditorViewModel
import com.dmood.app.ui.screen.home.HomeViewModel

object DmoodViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val locator = DmoodServiceLocator

        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    decisionRepository = locator.decisionRepository
                ) as T
            }

            modelClass.isAssignableFrom(DecisionEditorViewModel::class.java) -> {
                DecisionEditorViewModel(
                    decisionRepository = locator.decisionRepository,
                    validateDecisionUseCase = locator.validateDecisionUseCase,
                    calculateDecisionToneUseCase = locator.calculateDecisionToneUseCase
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
