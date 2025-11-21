package com.dmood.app.di

import android.content.Context
import androidx.room.Room
import com.dmood.app.data.local.database.AppDatabase
import com.dmood.app.data.preferences.UserPreferencesRepository
import com.dmood.app.data.repository.DecisionRepositoryImpl
import com.dmood.app.domain.repository.DecisionRepository
import com.dmood.app.domain.usecase.BuildWeeklySummaryUseCase
import com.dmood.app.domain.usecase.CalculateDailyMoodUseCase
import com.dmood.app.domain.usecase.CalculateDecisionToneUseCase
import com.dmood.app.domain.usecase.CalculateWeeklyScheduleUseCase
import com.dmood.app.domain.usecase.ExtractWeeklyHighlightsUseCase
import com.dmood.app.domain.usecase.GenerateInsightRulesUseCase
import com.dmood.app.domain.usecase.ValidateDecisionUseCase

object DmoodServiceLocator {

    private lateinit var appContext: Context

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "dmood.db"
        ).build()
    }

    private val decisionDao by lazy { database.decisionDao() }

    val decisionRepository: DecisionRepository by lazy {
        DecisionRepositoryImpl(decisionDao)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val validateDecisionUseCase: ValidateDecisionUseCase by lazy {
        ValidateDecisionUseCase()
    }

    val calculateDecisionToneUseCase: CalculateDecisionToneUseCase by lazy {
        CalculateDecisionToneUseCase()
    }

    val calculateDailyMoodUseCase: CalculateDailyMoodUseCase by lazy {
        CalculateDailyMoodUseCase()
    }

    val calculateWeeklyScheduleUseCase: CalculateWeeklyScheduleUseCase by lazy {
        CalculateWeeklyScheduleUseCase()
    }

    val buildWeeklySummaryUseCase: BuildWeeklySummaryUseCase by lazy {
        BuildWeeklySummaryUseCase(calculateDailyMoodUseCase)
    }

    val extractWeeklyHighlightsUseCase: ExtractWeeklyHighlightsUseCase by lazy {
        ExtractWeeklyHighlightsUseCase()
    }

    val generateInsightRulesUseCase: GenerateInsightRulesUseCase by lazy {
        GenerateInsightRulesUseCase(calculateDailyMoodUseCase)
    }

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }
}
