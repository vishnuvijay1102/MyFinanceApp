package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.entity.DetectedNotificationEntity
import com.example.ui.components.formatCurrency

object UpiPopupNotificationManager {

    private const val CHANNEL_ID = "myfinance_upi_popup_channel"
    private const val CHANNEL_NAME = "UPI Transaction Pop-ups"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up pop-up alerts on the home screen when a UPI payment is detected"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showUpiPopupNotification(
        context: Context,
        notification: DetectedNotificationEntity
    ) {
        initChannel(context)
        val popupId = (notification.id.toInt() + 10_000)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            popupId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmIntent = Intent(context, UpiActionReceiver::class.java).apply {
            action = UpiActionReceiver.ACTION_CONFIRM_UPI
            putExtra(UpiActionReceiver.EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(UpiActionReceiver.EXTRA_POPUP_ID, popupId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            popupId + 1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ignoreIntent = Intent(context, UpiActionReceiver::class.java).apply {
            action = UpiActionReceiver.ACTION_IGNORE_UPI
            putExtra(UpiActionReceiver.EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(UpiActionReceiver.EXTRA_POPUP_ID, popupId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            popupId + 2,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountFormatted = formatCurrency(notification.amount)
        val isCredit = notification.direction.equals("CREDIT", ignoreCase = true)
        val title = if (isCredit) {
            "UPI Received • $amountFormatted"
        } else {
            "UPI Paid • $amountFormatted"
        }
        val partyPart = if (notification.recipientOrSender.isNotBlank()) {
            " • ${notification.recipientOrSender}"
        } else {
            ""
        }
        val refPart = if (notification.upiRef.isNotBlank()) {
            "\nRef: ${notification.upiRef}"
        } else {
            ""
        }
        val shortBody = "${notification.appName}$partyPart"
        val bigBody = "${notification.appName}$partyPart$refPart\nTap Confirm & Save to log this transaction to MyFinance."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Confirm & Save", confirmPendingIntent)
            .addAction(0, "Ignore", ignorePendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(popupId, builder.build())
    }
}
