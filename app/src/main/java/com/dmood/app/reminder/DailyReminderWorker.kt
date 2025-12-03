package com.dmood.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dmood.app.di.DmoodServiceLocator
import java.time.LocalDate
import java.time.ZoneId

/**
 * Worker que se ejecuta cuando el DailyReminderReceiver lanza un OneTimeWorkRequest.
 *
 * Responsabilidad:
 * - Comprobar si el recordatorio diario está activado.
 * - Comprobar si hoy hay decisiones registradas.
 * - Si no hay decisiones, mostrar una notificación de recordatorio.
 *
 * NO programa la siguiente alarma; eso lo hace el DailyReminderReceiver.
 */
class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val decisionRepository = DmoodServiceLocator.decisionRepository
    private val userPreferencesRepository = DmoodServiceLocator.userPreferencesRepository
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun doWork(): Result {
        // Si el usuario ha desactivado el recordatorio, no hacemos nada.
        if (!userPreferencesRepository.isDailyReminderEnabled()) {
            return Result.success()
        }

        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return try {
            val decisions = decisionRepository.getByRange(start, end)

            // Solo lanzamos notificación si no hay decisiones registradas hoy.
            if (decisions.isEmpty()) {
                // Usa aquí tu helper real de notificaciones.
                // Si en tu proyecto se llama ReminderNotificationHelper, deja esta línea.
                ReminderNotificationHelper.showDailyReminder(applicationContext)
                // Si usas el helper que te propuse antes, sería:
                // NotificationHelper.showDailyReminderNotification(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            // Para un recordatorio de usuario, normalmente no compensa reintentar en bucle;
            // pero si quieres mantener el comportamiento actual, puedes devolver retry.
            Result.retry()
        }
    }
}
