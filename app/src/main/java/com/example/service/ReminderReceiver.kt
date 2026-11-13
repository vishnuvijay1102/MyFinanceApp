package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Payment"
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val dueDateFormatted = intent.getStringExtra(EXTRA_DUE_DATE) ?: "Today"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "REMINDER"

        ReminderNotificationManager.showReminderNotification(
            context = context,
            id = id,
            title = title,
            amount = amount,
            dueDateFormatted = dueDateFormatted,
            type = type
        )
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_AMOUNT = "reminder_amount"
        const val EXTRA_DUE_DATE = "reminder_due_date"
        const val EXTRA_TYPE = "reminder_type"
    }
}
