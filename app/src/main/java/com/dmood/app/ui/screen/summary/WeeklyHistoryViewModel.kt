package com.dmood.app.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.repository.DecisionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WeeklyHistoryItem(val startDate: LocalDate, val endDate: LocalDate)

data class WeeklyHistoryUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeeklyHistoryItem> = emptyList()
)

class WeeklyHistoryViewModel(
    private val decisionRepository: DecisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(WeeklyHistoryUiState())
    val uiState: StateFlow<WeeklyHistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = WeeklyHistoryUiState(isLoading = true)

            val earliestTimestamp = decisionRepository.getEarliestDecisionTimestamp()
            if (earliestTimestamp == null) {
                _uiState.value = WeeklyHistoryUiState(isLoading = false, weeks = emptyList())
                return@launch
            }
            val weekStart = userPreferencesRepository.weekStartDayFlow.first()
            val today = LocalDate.now()
            val anchor = today.with(TemporalAdjusters.previousOrSame(weekStart))
            val earliestDate = Instant.ofEpochMilli(earliestTimestamp).atZone(zoneId).toLocalDate()
            val firstAnchor = earliestDate.with(TemporalAdjusters.previousOrSame(weekStart))

            val history = buildList {
                var cursor = anchor
                while (!cursor.isBefore(firstAnchor)) {
                    val start = cursor.minusDays(7)
                    val end = cursor.minusDays(1)
                    add(WeeklyHistoryItem(startDate = start, endDate = end))
                    cursor = cursor.minusWeeks(1)
                }
            }

            _uiState.value = WeeklyHistoryUiState(
                isLoading = false,
                weeks = history
            )
        }
    }
}
