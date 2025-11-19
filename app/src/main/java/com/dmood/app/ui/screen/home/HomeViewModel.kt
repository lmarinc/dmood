package com.dmood.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val todayDecisions: List<Decision> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val decisionRepository: DecisionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadTodayDecisions()
    }

    fun loadTodayDecisions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val now = System.currentTimeMillis()
                val decisions = decisionRepository.getByDay(now)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    todayDecisions = decisions,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar las decisiones de hoy."
                )
            }
        }
    }
}
