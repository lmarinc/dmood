package com.dmood.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dmood.app.data.preferences.UserPreferencesRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    companion object {
        private const val DAILY_REMINDER_WORK_NAME = "daily_reminder_work"
        private const val WEEKLY_SUMMARY_WORK_NAME = "weekly_summary_work"

        // Código de petición para el PendingIntent del recordatorio diario
        private const val DAILY_REMINDER_REQUEST_CODE = 1001

        // HORA “REAL” DEL RECORDATORIO DIARIO (PRODUCCIÓN)
        const val DAILY_REMINDER_HOUR_DEFAULT = 21
        const val DAILY_REMINDER_MINUTE_DEFAULT = 15

        // HORA DE PRUEBA (CAMBIABLE RÁPIDO MIENTRAS DESARROLLAS)
        // Por ejemplo, 13:30
        const val DAILY_REMINDER_HOUR_TEST = 13
        const val DAILY_REMINDER_MINUTE_TEST = 30
    }

    /**
     * Llama a esto cuando cambien las preferencias del usuario
     * (o al arrancar la app) para sincronizar recordatorios.
     */
    suspend fun syncReminders() {
        if (userPreferencesRepository.isDailyReminderEnabled()) {
            // En producción: usar hora por defecto
            scheduleDailyReminder(DAILY_REMINDER_HOUR_DEFAULT, DAILY_REMINDER_MINUTE_DEFAULT)

            // Para PRUEBAS: comenta la línea de arriba y descomenta esta:
            // scheduleDailyReminder(DAILY_REMINDER_HOUR_TEST, DAILY_REMINDER_MINUTE_TEST)
        } else {
            cancelDailyReminder()
        }

        if (userPreferencesRepository.isWeeklyReminderEnabled()) {
            scheduleWeeklySummaryReminder()
        } else {
            cancelWeeklySummaryReminder()
        }
    }

    /**
     * Programa una alarma exacta (o casi) para la próxima vez que sean [hour]:[minute].
     * Cuando salte, se lanzará DailyReminderReceiver, que a su vez encola DailyReminderWorker.
     */
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = nextTriggerAtMillis(hour, minute)

        val pendingIntent = dailyReminderPendingIntent()

        // Para API >= 23 usamos setExactAndAllowWhileIdle, para anteriores setExact
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(dailyReminderPendingIntent())
    }

    /**
     * Seguimos usando WorkManager para el chequeo de resumen semanal (no tiene por qué ser exacto).
     */
    fun scheduleWeeklySummaryReminder() {
        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(24, TimeUnit.HOURS)
            // Si quieres, aquí puedes ajustar initialDelay como antes
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelWeeklySummaryReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK_NAME)
    }

    // ---------- Helpers privados ----------

    private fun dailyReminderPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.dmood.app.action.DAILY_REMINDER"
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE
                else
                    0)

        return PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            flags
        )
    }

    private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            // Si la hora ya ha pasado hoy, apuntamos a mañana
            next = next.plusDays(1)
        }
        return next
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
