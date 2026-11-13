package com.example.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseProvider
import com.example.data.entity.DetectedNotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class UpiNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullContent = "$title $text $bigText".trim()

        // Check if supported UPI or payment app
        val appName = when {
            pkg.contains("paisa") || pkg.contains("nbu") -> "Google Pay"
            pkg.contains("phonepe") -> "PhonePe"
            pkg.contains("paytm") -> "Paytm"
            pkg.contains("npci") || pkg.contains("bhim") -> "BHIM UPI"
            pkg.contains("bank") || pkg.contains("sms") || pkg.contains("mms") -> "Bank SMS"
            else -> null
        } ?: return

        val parsed = parseUpiText(appName, pkg, fullContent)
        if (parsed != null) {
            val database = DatabaseProvider.getDatabase(applicationContext)
            scope.launch {
                val id = database.detectedNotificationDao().insertNotification(parsed)
                UpiPopupNotificationManager.showUpiPopupNotification(
                    context = applicationContext,
                    notification = parsed.copy(id = id)
                )
            }
        }
    }

    companion object {
        private const val TAG = "UpiNotificationParser"

        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(pkgName)
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun parseUpiText(
            appName: String,
            packageName: String,
            text: String
        ): DetectedNotificationEntity? {
            try {
                // Regex for currency amounts: ₹250, Rs. 250, INR 250, 250.00
                val amountPattern = Pattern.compile(
                    "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:Rs|INR|₹)",
                    Pattern.CASE_INSENSITIVE
                )
                val matcher = amountPattern.matcher(text)
                if (!matcher.find()) return null

                val rawAmountStr = (matcher.group(1) ?: matcher.group(2))?.replace(",", "") ?: return null
                val amount = rawAmountStr.toDoubleOrNull() ?: return null
                if (amount <= 0.0) return null

                // Direction: Debit vs Credit
                val lower = text.lowercase()
                val isDebit = lower.contains("paid") || lower.contains("sent") ||
                        lower.contains("debited") || lower.contains("spent") ||
                        lower.contains("transferred to") || lower.contains("debit")

                val isCredit = lower.contains("received") || lower.contains("credited") ||
                        lower.contains("refund") || lower.contains("credit")

                val direction = if (isCredit && !isDebit) "CREDIT" else "DEBIT"

                // Recipient / Sender extraction
                var party = ""
                val toPattern = Pattern.compile("(?:to|paid to|sent to)\\s+([A-Za-z0-9\\s&]+?)(?:\\s+on|\\s+via|\\s+using|\\.|,|$)", Pattern.CASE_INSENSITIVE)
                val fromPattern = Pattern.compile("(?:from|received from)\\s+([A-Za-z0-9\\s&]+?)(?:\\s+on|\\s+via|\\s+using|\\.|,|$)", Pattern.CASE_INSENSITIVE)

                val partyMatcher = if (direction == "DEBIT") toPattern.matcher(text) else fromPattern.matcher(text)
                if (partyMatcher.find()) {
                    party = partyMatcher.group(1)?.trim() ?: ""
                }

                // UPI Ref / UTR extraction
                var upiRef = ""
                val refPattern = Pattern.compile("(?:UPI Ref(?:\\s*No)?|UTR|Ref No|Txn ID)[:\\s]+([0-9A-Za-z]+)", Pattern.CASE_INSENSITIVE)
                val refMatcher = refPattern.matcher(text)
                if (refMatcher.find()) {
                    upiRef = refMatcher.group(1)?.trim() ?: ""
                }

                return DetectedNotificationEntity(
                    appName = appName,
                    packageName = packageName,
                    amount = amount,
                    direction = direction,
                    recipientOrSender = party,
                    upiRef = upiRef,
                    timestamp = System.currentTimeMillis(),
                    rawText = text,
                    status = "PENDING_CONFIRMATION"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing notification text", e)
                return null
            }
        }
    }
}
