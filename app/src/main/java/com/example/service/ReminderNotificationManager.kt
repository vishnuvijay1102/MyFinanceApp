package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.ui.components.formatCurrency
import java.util.Calendar

object ReminderNotificationManager {

    private const val CHANNEL_ID = "myfinance_reminders_channel"
    private const val CHANNEL_NAME = "Financial Reminders"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for EMIs, Credit Cards, AutoPay, Insurance, and Bills"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        id: Int,
        title: String,
        amount: Double,
        dueDateFormatted: String,
        type: String
    ) {
        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = formatCurrency(amount)
        val typeLabel = when (type) {
            "CREDIT_CARD" -> "Credit Card Bill"
            "EMI" -> "EMI Installment"
            "AUTOPAY" -> "AutoPay Mandate"
            "INSURANCE" -> "Insurance Renewal"
            "LOAN" -> "Loan Repayment"
            else -> "Bill / Recharge"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$typeLabel: $title")
            .setContentText("Amount $formattedAmt due on $dueDateFormatted")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$typeLabel reminder for $title. Amount: $formattedAmt. Due Date: $dueDateFormatted. Tap to open MyFinance."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(id, builder.build())
    }

    fun scheduleReminderNotification(
        context: Context,
        id: Int,
        title: String,
        amount: Double,
        dueDateMillis: Long,
        dueDateFormatted: String,
        type: String
    ) {
        initNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val receiverIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_AMOUNT, amount)
            putExtra(ReminderReceiver.EXTRA_DUE_DATE, dueDateFormatted)
            putExtra(ReminderReceiver.EXTRA_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule at 9:00 AM on the due date (or 15 seconds from now if already today/past)
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        val triggerAt = if (targetCal.timeInMillis > now) targetCal.timeInMillis else now + 10_000L

        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
