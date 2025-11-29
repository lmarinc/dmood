package com.dmood.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dmood.app.MainActivity
import com.dmood.app.R

object ReminderNotificationHelper {

    private const val DAILY_CHANNEL_ID = "daily_reminder_channel"
    private const val WEEKLY_CHANNEL_ID = "weekly_summary_channel"

    fun showDailyReminder(context: Context) {
        if (!hasPermission(context)) return
        ensureChannels(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DAILY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recordatorio diario")
            .setContentText("No registraste ninguna decisión hoy. Anótala antes de dormir.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }

    fun showWeeklySummaryReminder(context: Context, summaryLabel: String) {
        if (!hasPermission(context)) return
        ensureChannels(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WEEKLY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo resumen semanal disponible")
            .setContentText("Tu resumen de $summaryLabel está listo para revisarlo.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(1002, notification)
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dailyChannel = NotificationChannel(
            DAILY_CHANNEL_ID,
            "Recordatorios diarios",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos para registrar tus decisiones del día"
        }

        val weeklyChannel = NotificationChannel(
            WEEKLY_CHANNEL_ID,
            "Resumen semanal",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Aviso cuando un nuevo resumen semanal esté disponible"
        }

        manager.createNotificationChannel(dailyChannel)
        manager.createNotificationChannel(weeklyChannel)
    }

    private fun hasPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
