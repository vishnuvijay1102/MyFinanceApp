package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.data.database.DatabaseProvider
import com.example.data.repository.FinanceRepository
import com.example.ui.components.formatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class UpiActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1L)
        val popupId = intent.getIntExtra(EXTRA_POPUP_ID, -1)
        if (notifId <= 0L) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (popupId > 0) {
            manager?.cancel(popupId)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = DatabaseProvider.getDatabase(appContext)
                val repository = FinanceRepository(database, appContext)
                val detected = database.detectedNotificationDao().getNotificationById(notifId)

                when (intent.action) {
                    ACTION_CONFIRM_UPI -> {
                        if (detected != null && detected.status == "PENDING_CONFIRMATION") {
                            val firstAccountId = database.accountDao().getAllActiveAccounts().firstOrNull()?.firstOrNull()?.id
                            repository.confirmDetectedNotification(
                                notification = detected,
                                category = "Others",
                                sourceAccountId = firstAccountId
                            )
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    appContext,
                                    "Saved ${formatCurrency(detected.amount)} ${detected.direction.lowercase()} to MyFinance",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    ACTION_IGNORE_UPI -> {
                        repository.updateNotificationStatus(notifId, "IGNORED")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM_UPI = "com.example.ACTION_CONFIRM_UPI"
        const val ACTION_IGNORE_UPI = "com.example.ACTION_IGNORE_UPI"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_POPUP_ID = "extra_popup_id"
    }
}
