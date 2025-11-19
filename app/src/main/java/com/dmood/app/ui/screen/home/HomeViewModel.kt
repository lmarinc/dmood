package com.dmood.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val decisions: List<Decision> = emptyList(),
    val errorMessage: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val userName: String? = null
)

class HomeViewModel(
    private val decisionRepository: DecisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val zoneId: ZoneId = ZoneId.systemDefault()

    init {
        observeUserName()
        loadTodayDecisions()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(userName = name)
            }
        }
    }

    fun loadDecisionsForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                selectedDate = date
            )

            try {
                val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val decisions = decisionRepository
                    .getByDay(startOfDay)
                    .sortedByDescending { it.timestamp }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    decisions = decisions,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar las decisiones"
                )
            }
        }
    }

    fun loadTodayDecisions() {
        loadDecisionsForDate(LocalDate.now())
    }

    fun refreshCurrentDay() {
        loadDecisionsForDate(_uiState.value.selectedDate)
    }

    fun goToPreviousDay() {
        val previousDate = _uiState.value.selectedDate.minusDays(1)
        loadDecisionsForDate(previousDate)
    }

    fun goToNextDay() {
        val today = LocalDate.now()
        val nextDate = _uiState.value.selectedDate.plusDays(1)
        if (nextDate.isAfter(today)) return
        loadDecisionsForDate(nextDate)
    }
}
