package com.dmood.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dmood.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // 1) Encolamos el worker que hará la lógica real
        val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // 2) Reprogramamos la siguiente alarma para el día siguiente
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = UserPreferencesRepository(context.applicationContext)
            if (prefs.isDailyReminderEnabled()) {
                val scheduler = ReminderScheduler(context.applicationContext, prefs)
                scheduler.scheduleDailyReminder(
                    ReminderScheduler.DAILY_REMINDER_HOUR_DEFAULT,
                    ReminderScheduler.DAILY_REMINDER_MINUTE_DEFAULT
                )
                // Para pruebas, igual que antes, puedes usar HOUR_TEST / MINUTE_TEST
                // scheduler.scheduleDailyReminder(
                //     ReminderScheduler.DAILY_REMINDER_HOUR_TEST,
                //     ReminderScheduler.DAILY_REMINDER_MINUTE_TEST
                // )
            }
        }
    }
}
