package com.dmood.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.domain.model.CategoryType
import com.dmood.app.domain.model.Decision
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val decisions: List<Decision> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val userName: String? = null,
    val isDeleteMode: Boolean = false,
    val selectedForDeletion: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val categoryFilter: CategoryType? = null,
    val cardLayout: CardLayoutMode = CardLayoutMode.COZY,
    val minAvailableDate: LocalDate = LocalDate.now(),
    val weekStartDay: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
    val nextSummaryDate: LocalDate = LocalDate.now(),
    val isSummaryAvailable: Boolean = false,
    val developerModeEnabled: Boolean = false
)

enum class CardLayoutMode(val label: String, val description: String) {
    COMPACT(label = "Pequeño", description = "Muestra más decisiones"),
    COZY(label = "Medio", description = "Equilibrio entre cantidad y espacio"),
    ROOMY(label = "Tarjeta", description = "Más aire y jerarquía visual")
}

class HomeViewModel(
    private val decisionRepository: DecisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private var firstUseDate: LocalDate = LocalDate.now()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observeUserName()
        observePreferences()
        observeDeveloperMode()
        ensureFirstUseDate()
        loadMinAvailableDate()
        observeDecisionsForCurrentDay()
        refreshWeeklySchedule()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            userPreferencesRepository.userNameFlow.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    private fun loadMinAvailableDate() {
        viewModelScope.launch {
            val earliestTimestamp = try {
                decisionRepository.getEarliestDecisionTimestamp()
            } catch (e: Exception) {
                null
            }

            val minDate = earliestTimestamp?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
            } ?: LocalDate.now()

            earliestTimestamp?.let { userPreferencesRepository.updateFirstUseDateIfEarlier(it) }
            earliestTimestamp?.let { firstUseDate = Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

            _uiState.update { it.copy(minAvailableDate = minDate) }
            refreshWeeklySchedule()
        }
    }

    private fun observeDecisionsForCurrentDay() {
        val date = _uiState.value.selectedDate
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            decisionRepository.getDecisionsForDayFlow(startMillis, endMillis)
                .collect { decisions ->
                    _uiState.update { state ->
                        val validIds = decisions.map { it.id }.toSet()
                        state.copy(
                            decisions = decisions,
                            isLoading = false,
                            selectedForDeletion = state.selectedForDeletion.intersect(validIds)
                        )
                    }
                }
        }
    }

    fun goToPreviousDay() {
        val current = _uiState.value
        val candidate = current.selectedDate.minusDays(1)
        if (candidate.isBefore(current.minAvailableDate)) return
        updateSelectedDate(-1)
    }

    fun goToNextDay() {
        val today = LocalDate.now()
        val nextDate = _uiState.value.selectedDate.plusDays(1)
        if (nextDate.isAfter(today)) return
        updateSelectedDate(+1)
    }

    private fun updateSelectedDate(offsetDays: Long) {
        val newDate = _uiState.value.selectedDate.plusDays(offsetDays)
        _uiState.update {
            it.copy(
                selectedDate = newDate,
                isLoading = true,
                selectedForDeletion = emptySet(),
                isDeleteMode = false
            )
        }
        observeDecisionsForCurrentDay()
    }

    // ------- MODO BORRADO MÚLTIPLE -------

    fun toggleDeleteMode() {
        val isToday = _uiState.value.selectedDate == LocalDate.now()
        if (!isToday) {
            _uiState.update { it.copy(isDeleteMode = false, selectedForDeletion = emptySet()) }
            return
        }
        _uiState.update { state ->
            if (state.isDeleteMode) {
                state.copy(isDeleteMode = false, selectedForDeletion = emptySet())
            } else {
                state.copy(isDeleteMode = true)
            }
        }
    }

    fun toggleDecisionSelection(decisionId: Long) {
        _uiState.update { state ->
            if (!state.isDeleteMode || state.selectedDate != LocalDate.now()) return@update state

            val newSet = state.selectedForDeletion.toMutableSet()
            if (newSet.contains(decisionId)) {
                newSet.remove(decisionId)
            } else {
                newSet.add(decisionId)
            }
            state.copy(selectedForDeletion = newSet)
        }
    }

    fun deleteSelectedDecisions() {
        val ids = _uiState.value.selectedForDeletion.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            ids.forEach { id ->
                val decision = decisionRepository.getById(id) ?: return@forEach
                decisionRepository.delete(decision)
            }
            _uiState.update {
                it.copy(
                    isDeleteMode = false,
                    selectedForDeletion = emptySet()
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateCategoryFilter(categoryType: CategoryType?) {
        _uiState.update { it.copy(categoryFilter = categoryType) }
    }

    fun updateCardLayout(mode: CardLayoutMode) {
        _uiState.update { it.copy(cardLayout = mode) }
        viewModelScope.launch {
            userPreferencesRepository.setCardLayout(mode.name)
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.cardLayoutFlow.collect { stored ->
                val resolved = runCatching { CardLayoutMode.valueOf(stored) }
                    .getOrDefault(CardLayoutMode.COZY)
                _uiState.update { it.copy(cardLayout = resolved) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weekStartDayFlow.collect { weekStart ->
                _uiState.update { it.copy(weekStartDay = weekStart) }
                refreshWeeklySchedule()
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.firstUseLocalDate(zoneId).collect { stored ->
                stored?.let {
                    firstUseDate = it
                    refreshWeeklySchedule()
                }
            }
        }
    }

    private fun observeDeveloperMode() {
        viewModelScope.launch {
            userPreferencesRepository.developerModeFlow.collect { enabled ->
                _uiState.update { it.copy(developerModeEnabled = enabled) }
                refreshWeeklySchedule()
            }
        }
    }

    private fun ensureFirstUseDate() {
        viewModelScope.launch {
            val stored = userPreferencesRepository.ensureFirstUseDate()
            firstUseDate = Instant.ofEpochMilli(stored).atZone(zoneId).toLocalDate()
            refreshWeeklySchedule()
        }
    }

    private fun refreshWeeklySchedule() {
        val weekStart = _uiState.value.weekStartDay
        val devMode = _uiState.value.developerModeEnabled
        val schedule = calculateWeeklyScheduleUseCase(
            firstUseDate = firstUseDate,
            weekStartDay = weekStart,
            today = LocalDate.now()
        )
        _uiState.update {
            it.copy(
                nextSummaryDate = schedule.nextSummaryDate,
                isSummaryAvailable = schedule.isSummaryAvailable || devMode
            )
        }
    }
}
