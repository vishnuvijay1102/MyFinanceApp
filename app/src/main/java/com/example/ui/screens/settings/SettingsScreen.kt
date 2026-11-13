package com.example.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AuditLogEntity
import com.example.service.ReminderNotificationManager
import com.example.service.UpiNotificationListenerService
import com.example.ui.components.CompactTextField
import com.example.ui.components.GlassCard
import com.example.ui.components.TintedIconBadge
import com.example.ui.components.formatDate
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.TealAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auditLogs by viewModel.recentAuditLogs.collectAsStateWithLifecycle()

    var isNotificationServiceEnabled by remember {
        mutableStateOf(UpiNotificationListenerService.isNotificationServiceEnabled(context))
    }

    var showSimulateModal by remember { mutableStateOf(false) }
    var showAuditHistoryModal by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showSimulateModal = true
        } else {
            Toast.makeText(
                context,
                "Allow notifications so UPI alerts can pop up on your home screen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 108.dp)
        ) {
        item {
            Text(
                text = "Settings & Tools",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Automation, data protection & audit logs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 5: Automatic UPI Notification Detection
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = IndigoAccent,
                gradientColors = listOf(IndigoAccent, CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TintedIconBadge(
                                icon = Icons.Default.NotificationsActive,
                                tint = IndigoAccent,
                                contentDescription = "Notification Listener",
                                size = 36.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "UPI Notification Detection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (isNotificationServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (isNotificationServiceEnabled) Color(0xFF6EE7B7) else Color(0xFFFDE68A)
                        )
                    }

                    Text(
                        text = if (isNotificationServiceEnabled)
                            "Active: Detected UPI payments pop up directly on your home screen with Confirm & Ignore actions."
                        else
                            "Inactive: Enable notification access so UPI payments pop up on your home screen automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                UpiNotificationListenerService.openNotificationAccessSettings(context)
                                isNotificationServiceEnabled = UpiNotificationListenerService.isNotificationServiceEnabled(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isNotificationServiceEnabled) "Review Access" else "Enable Access")
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !ReminderNotificationManager.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showSimulateModal = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f).testTag("simulate_notification_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Test")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test / Sim")
                        }
                    }
                }
            }
        }

        // Section 14: History & Audit System
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = VioletAccent,
                gradientColors = listOf(VioletAccent, RoseAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TintedIconBadge(
                            icon = Icons.Default.History,
                            tint = VioletAccent,
                            contentDescription = "Audit History",
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Financial Audit Trail",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tracks all modifications, balance updates, and record deletions chronologically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Button(
                        onClick = { showAuditHistoryModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent, contentColor = Color.White),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View Audit Logs (${auditLogs.size})")
                    }
                }
            }
        }

        // Section 16: Backup & Export
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = SkyAccent,
                gradientColors = listOf(TealAccent, CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TintedIconBadge(
                            icon = Icons.Default.FileDownload,
                            tint = TealAccent,
                            contentDescription = "Export CSV",
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Export & Backup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Export your entire financial transaction history as a standardized CSV spreadsheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Button(
                        onClick = {
                            viewModel.exportCsv { file ->
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Transactions CSV"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Color.White),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Export to CSV")
                    }
                }
            }
        }

        // Section 18: Security & Privacy
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = GoldAccent,
                gradientColors = listOf(CharcoalInk, IndigoAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TintedIconBadge(
                            icon = Icons.Default.Lock,
                            tint = GoldAccent,
                            contentDescription = "Security",
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Privacy Guarantee",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = "• 100% Offline-First: All financial records are stored strictly in local SQLite.\n" +
                                "• Zero External Calls: No accounts, no servers, no third-party tracking.\n" +
                                "• Zero Credential Access: The app never asks for UPI PIN, passwords, or full card details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Clear All Data Action
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = RoseAccent,
                gradientColors = listOf(RoseAccent, OrangeAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Data Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Wipe all transactions, accounts, and reminders to start with a clean ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Button(
                        onClick = { showClearConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Financial Data")
                    }
                }
            }
        }
    }
    }

    // Modal to Test / Simulate Notification
    if (showSimulateModal) {
        SimulateNotificationDialog(
            onSimulate = { app, amt, dir, party ->
                viewModel.simulateDetectedNotification(app, amt, dir, party)
                Toast.makeText(
                    context,
                    "Home-screen UPI pop-up sent",
                    Toast.LENGTH_SHORT
                ).show()
                showSimulateModal = false
            },
            onDismiss = { showSimulateModal = false }
        )
    }

    // Modal to View Audit History
    if (showAuditHistoryModal) {
        AuditLogsDialog(
            logs = auditLogs,
            onDismiss = { showAuditHistoryModal = false }
        )
    }

    // Clear Confirmation
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Data") },
            text = { Text("Are you sure you want to delete all financial data? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SimulateNotificationDialog(
    onSimulate: (app: String, amount: Double, direction: String, party: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedApp by remember { mutableStateOf("Google Pay") }
    var amountText by remember { mutableStateOf("350") }
    var direction by remember { mutableStateOf("DEBIT") }
    var partyText by remember { mutableStateOf("Swiggy Indiranagar") }

    val apps = listOf("Google Pay", "PhonePe", "Paytm", "BHIM UPI")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simulate UPI Notification", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                Text("App Source", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    apps.forEach { app ->
                        Button(
                            onClick = { selectedApp = app },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedApp == app) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                app.split(" ").first(),
                                color = if (selectedApp == app) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { direction = "DEBIT" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (direction == "DEBIT") ExpenseRed else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Debit (Spent)", color = if (direction == "DEBIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { direction = "CREDIT" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (direction == "CREDIT") IncomeGreen else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Credit (Received)", color = if (direction == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = partyText,
                    onValueChange = { partyText = it },
                    label = "Merchant / Sender Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onSimulate(selectedApp, amt, direction, partyText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = Color.White)
                    ) {
                        Text("Inject Notification")
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsDialog(
    logs: List<AuditLogEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Audit History Logs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                if (logs.isEmpty()) {
                    Text("No audit events recorded yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            GlassCard(
                                shape = RoundedCornerShape(12.dp),
                                accentTint = VioletAccent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${log.action} on ${log.tableName}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = when (log.action) {
                                                "DELETE" -> Color(0xFFFDA4AF)
                                                "UPDATE" -> Color(0xFFFDE68A)
                                                else -> Color(0xFF6EE7B7)
                                            }
                                        )
                                        Text(
                                            text = formatDate(log.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
