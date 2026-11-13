package com.example.ui.screens.records

import android.Manifest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AutoPayEntity
import com.example.data.entity.CreditCardEntity
import com.example.data.entity.EmiEntity
import com.example.data.entity.InsuranceEntity
import com.example.data.entity.LoanEntity
import com.example.data.entity.RechargeEntity
import com.example.service.ReminderNotificationManager
import com.example.ui.components.CompactTextField
import com.example.ui.components.DatePickerField
import com.example.ui.components.GlassCard
import com.example.ui.components.TintedIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
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
import java.util.Calendar

enum class RecordTab(val title: String) {
    LOANS("Loans & Lent"),
    CARDS("Credit Cards"),
    EMIS("EMIs"),
    AUTOPAY("AutoPay"),
    INSURANCE("Insurance"),
    UTILITIES("Utilities")
}

private data class PendingReminderPayload(
    val id: Int,
    val title: String,
    val amount: Double,
    val dueDateMillis: Long,
    val dueDateFormatted: String,
    val type: String
)

private sealed interface RecordDeleteTarget {
    val label: String
    data class Loan(val item: LoanEntity) : RecordDeleteTarget {
        override val label: String = "${item.personOrOrg} (${item.loanType})"
    }
    data class CreditCard(val item: CreditCardEntity) : RecordDeleteTarget {
        override val label: String = item.cardNickname
    }
    data class Emi(val item: EmiEntity) : RecordDeleteTarget {
        override val label: String = item.loanName
    }
    data class AutoPay(val item: AutoPayEntity) : RecordDeleteTarget {
        override val label: String = item.merchantName
    }
    data class Insurance(val item: InsuranceEntity) : RecordDeleteTarget {
        override val label: String = item.policyName
    }
    data class Recharge(val item: RechargeEntity) : RecordDeleteTarget {
        override val label: String = item.serviceProvider
    }
}

private fun computeNextCreditCardDueMillis(paymentDueDay: Int): Long {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val targetDay = paymentDueDay.coerceIn(1, 31)
    if (todayDay > targetDay) {
        cal.add(Calendar.MONTH, 1)
    }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
    cal.set(Calendar.HOUR_OF_DAY, 9)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
fun RecordsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(RecordTab.LOANS) }

    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val repayments by viewModel.allRepayments.collectAsStateWithLifecycle()
    val cards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val emis by viewModel.allEmis.collectAsStateWithLifecycle()
    val autopays by viewModel.allAutoPays.collectAsStateWithLifecycle()
    val insurances by viewModel.allInsurances.collectAsStateWithLifecycle()
    val recharges by viewModel.allRecharges.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var loanToRepay by remember { mutableStateOf<LoanEntity?>(null) }
    var cardToPay by remember { mutableStateOf<CreditCardEntity?>(null) }
    var recordToDelete by remember { mutableStateOf<RecordDeleteTarget?>(null) }
    var pendingReminder by remember { mutableStateOf<PendingReminderPayload?>(null) }

    val dispatchReminder: (PendingReminderPayload) -> Unit = { payload ->
        ReminderNotificationManager.showReminderNotification(
            context = context,
            id = payload.id,
            title = payload.title,
            amount = payload.amount,
            dueDateFormatted = payload.dueDateFormatted,
            type = payload.type
        )
        ReminderNotificationManager.scheduleReminderNotification(
            context = context,
            id = payload.id,
            title = payload.title,
            amount = payload.amount,
            dueDateMillis = payload.dueDateMillis,
            dueDateFormatted = payload.dueDateFormatted,
            type = payload.type
        )
        Toast.makeText(
            context,
            "Reminder sent & scheduled for ${payload.dueDateFormatted}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) {
            dispatchReminder(pending)
        } else if (!granted) {
            Toast.makeText(
                context,
                "Allow notifications to receive payment reminders",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val triggerReminder: (PendingReminderPayload) -> Unit = { payload ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderNotificationManager.hasNotificationPermission(context)
        ) {
            pendingReminder = payload
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            dispatchReminder(payload)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Financial Records",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                RecordTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 108.dp)
            ) {
                when (selectedTab) {
                    RecordTab.LOANS -> {
                        val lentList = loans.filter { it.loanType.equals("LENT", ignoreCase = true) }
                        val borrowedList = loans.filter { it.loanType.equals("BORROWED", ignoreCase = true) }
                        val repaymentsByLoan = repayments.groupBy { it.loanId }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }

                        item {
                            GlassCard(
                                shape = RoundedCornerShape(18.dp),
                                accentTint = IndigoAccent,
                                gradientColors = listOf(IndigoAccent, CyanAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Total Lent (Receivable)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val totalLent = lentList.sumOf {
                                            maxOf(0.0, it.originalAmount - (repaymentsByLoan[it.id] ?: 0.0))
                                        }
                                        Text(
                                            formatCurrency(totalLent),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF6EE7B7)
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Total Borrowed (Payable)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val totalBorrowed = borrowedList.sumOf {
                                            maxOf(0.0, it.originalAmount - (repaymentsByLoan[it.id] ?: 0.0))
                                        }
                                        Text(
                                            formatCurrency(totalBorrowed),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                }
                            }
                        }

                        if (loans.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.Handshake,
                                    tint = IncomeGreen,
                                    title = "No Loan Records Yet",
                                    message = "Track money lent or borrowed with expected due dates and reminders.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(loans, key = { "loan_${it.id}" }) { loan ->
                                val repaid = repaymentsByLoan[loan.id] ?: 0.0
                                val remaining = maxOf(0.0, loan.originalAmount - repaid)
                                val isLent = loan.loanType.equals("LENT", ignoreCase = true)
                                val accentColor = if (isLent) SkyAccent else WarningAmber

                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = accentColor,
                                    gradientColors = if (isLent) {
                                        listOf(SkyAccent, CyanAccent)
                                    } else {
                                        listOf(GoldAccent, OrangeAccent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TintedIconBadge(
                                                    icon = if (isLent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    tint = accentColor,
                                                    contentDescription = loan.loanType,
                                                    size = 42.dp,
                                                    iconSize = 20.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = loan.personOrOrg,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (isLent) "LENT • Receivable" else "BORROWED • Payable",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = accentColor
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        triggerReminder(
                                                            PendingReminderPayload(
                                                                id = loan.id.toInt() + 6000,
                                                                title = "${loan.loanType}: ${loan.personOrOrg}",
                                                                amount = remaining,
                                                                dueDateMillis = loan.expectedRepaymentDate,
                                                                dueDateFormatted = formatShortDate(loan.expectedRepaymentDate),
                                                                type = "LOAN"
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = "Remind Me",
                                                        tint = accentColor
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { recordToDelete = RecordDeleteTarget.Loan(loan) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_loan_${loan.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Loan",
                                                        tint = Color(0xFFFDA4AF)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "Due Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFCBD5E1)
                                                )
                                                Text(
                                                    formatShortDate(loan.expectedRepaymentDate),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                                Text(
                                                    "Original: ${formatCurrency(loan.originalAmount)} • Repaid: ${formatCurrency(repaid)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFCBD5E1)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatCurrency(remaining),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = if (isLent) Color(0xFF6EE7B7) else Color(0xFFFDA4AF)
                                                )
                                            }
                                        }

                                        if (loan.description.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Purpose: ${loan.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }

                                        if (remaining > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = { loanToRepay = loan },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("Record Repayment")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RecordTab.CARDS -> {
                        if (cards.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.CreditCard,
                                    tint = RoseAccent,
                                    title = "No Credit Cards Yet",
                                    message = "Add your credit cards with monthly due dates and outstanding amounts.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(cards, key = { "card_${it.id}" }) { card ->
                                val dueMillis = computeNextCreditCardDueMillis(card.paymentDueDay)
                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = RoseAccent,
                                    gradientColors = listOf(VioletAccent, RoseAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TintedIconBadge(
                                                    icon = Icons.Default.CreditCard,
                                                    tint = RoseAccent,
                                                    contentDescription = "Credit Card",
                                                    size = 42.dp,
                                                    iconSize = 20.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = card.cardNickname,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = card.bankName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        triggerReminder(
                                                            PendingReminderPayload(
                                                                id = card.id.toInt() + 1000,
                                                                title = card.cardNickname,
                                                                amount = card.currentOutstanding,
                                                                dueDateMillis = dueMillis,
                                                                dueDateFormatted = formatShortDate(dueMillis),
                                                                type = "CREDIT_CARD"
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = "Remind Me",
                                                        tint = Color(0xFF93C5FD)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { recordToDelete = RecordDeleteTarget.CreditCard(card) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_card_${card.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Credit Card",
                                                        tint = Color(0xFFFDA4AF)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Next Due Date", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    "${formatShortDate(dueMillis)} (${card.paymentDueDay}th monthly)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Current Outstanding", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatCurrency(card.currentOutstanding),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color(0xFFFDA4AF)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        val utilization = if (card.creditLimit > 0) {
                                            ((card.currentOutstanding / card.creditLimit) * 100).toInt()
                                        } else {
                                            0
                                        }
                                        Text(
                                            "Credit Utilization: $utilization% (Limit: ${formatCurrency(card.creditLimit)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { cardToPay = card },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Record Card Payment")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RecordTab.EMIS -> {
                        if (emis.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.ReceiptLong,
                                    tint = VioletAccent,
                                    title = "No Active EMIs Tracked",
                                    message = "Add EMIs with installment amounts and exact due dates to get reminders.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(emis, key = { "emi_${it.id}" }) { emi ->
                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = IndigoAccent,
                                    gradientColors = listOf(IndigoAccent, VioletAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TintedIconBadge(
                                                    icon = Icons.Default.ReceiptLong,
                                                    tint = VioletAccent,
                                                    contentDescription = "EMI",
                                                    size = 42.dp,
                                                    iconSize = 20.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        emi.loanName,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        emi.loanProvider,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        triggerReminder(
                                                            PendingReminderPayload(
                                                                id = emi.id.toInt() + 2000,
                                                                title = emi.loanName,
                                                                amount = emi.emiAmount,
                                                                dueDateMillis = emi.nextPaymentDate,
                                                                dueDateFormatted = formatShortDate(emi.nextPaymentDate),
                                                                type = "EMI"
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = "Remind Me",
                                                        tint = Color(0xFF93C5FD)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { recordToDelete = RecordDeleteTarget.Emi(emi) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_emi_${emi.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete EMI",
                                                        tint = Color(0xFFFDA4AF)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Next Due Date", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatShortDate(emi.nextPaymentDate),
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Monthly EMI Amount", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatCurrency(emi.emiAmount),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Installments: ${emi.remainingInstallments} / ${emi.totalInstallments} remaining • Liability: ${formatCurrency(emi.remainingInstallments * emi.emiAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    RecordTab.AUTOPAY -> {
                        if (autopays.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.Subscriptions,
                                    tint = CyanAccent,
                                    title = "No AutoPay Mandates",
                                    message = "Track subscriptions and recurring mandates with debit amounts and due dates.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(autopays, key = { "auto_${it.id}" }) { auto ->
                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = CyanAccent,
                                    gradientColors = listOf(CyanAccent, IndigoAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TintedIconBadge(
                                                icon = Icons.Default.Subscriptions,
                                                tint = CyanAccent,
                                                contentDescription = "AutoPay",
                                                size = 42.dp,
                                                iconSize = 20.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    auto.merchantName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${auto.frequency} • Due: ${formatShortDate(auto.nextPaymentDate)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    formatCurrency(auto.expectedAmount),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    triggerReminder(
                                                        PendingReminderPayload(
                                                            id = auto.id.toInt() + 3000,
                                                            title = auto.merchantName,
                                                            amount = auto.expectedAmount,
                                                            dueDateMillis = auto.nextPaymentDate,
                                                            dueDateFormatted = formatShortDate(auto.nextPaymentDate),
                                                            type = "AUTOPAY"
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Remind Me",
                                                    tint = Color(0xFF93C5FD)
                                                )
                                            }
                                            IconButton(
                                                onClick = { recordToDelete = RecordDeleteTarget.AutoPay(auto) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_autopay_${auto.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete AutoPay",
                                                    tint = Color(0xFFFDA4AF)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RecordTab.INSURANCE -> {
                        if (insurances.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.Shield,
                                    tint = TealAccent,
                                    title = "No Insurance Policies",
                                    message = "Register health, life, vehicle, or term policies with renewal dates and reminders.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(insurances, key = { "ins_${it.id}" }) { ins ->
                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = SkyAccent,
                                    gradientColors = listOf(CyanAccent, IndigoAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TintedIconBadge(
                                                    icon = Icons.Default.Shield,
                                                    tint = TealAccent,
                                                    contentDescription = "Insurance",
                                                    size = 42.dp,
                                                    iconSize = 20.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        ins.policyName,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${ins.insuranceCompany} • ${ins.policyType}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        triggerReminder(
                                                            PendingReminderPayload(
                                                                id = ins.id.toInt() + 4000,
                                                                title = ins.policyName,
                                                                amount = ins.premiumAmount,
                                                                dueDateMillis = ins.renewalDate,
                                                                dueDateFormatted = formatShortDate(ins.renewalDate),
                                                                type = "INSURANCE"
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = "Remind Me",
                                                        tint = Color(0xFF93C5FD)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { recordToDelete = RecordDeleteTarget.Insurance(ins) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_insurance_${ins.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Policy",
                                                        tint = Color(0xFFFDA4AF)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Renewal Due Date", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatShortDate(ins.renewalDate),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Premium Amount", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                                Text(
                                                    formatCurrency(ins.premiumAmount),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RecordTab.UTILITIES -> {
                        if (recharges.isEmpty()) {
                            item {
                                RecordsEmptyStateCard(
                                    icon = Icons.Default.PhoneAndroid,
                                    tint = OrangeAccent,
                                    title = "No Recharges or Utilities",
                                    message = "Add mobile, broadband, electricity, or utility due dates for timely alerts.",
                                    onAddClick = { showAddDialog = true }
                                )
                            }
                        } else {
                            items(recharges, key = { "rec_${it.id}" }) { rec ->
                                GlassCard(
                                    shape = RoundedCornerShape(18.dp),
                                    accentTint = OrangeAccent,
                                    gradientColors = listOf(OrangeAccent, GoldAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TintedIconBadge(
                                                icon = Icons.Default.PhoneAndroid,
                                                tint = OrangeAccent,
                                                contentDescription = rec.serviceType,
                                                size = 42.dp,
                                                iconSize = 20.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    rec.serviceProvider,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "Due: ${formatShortDate(rec.nextRechargeDate)} • ${rec.serviceType}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color(0xFF93C5FD)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    formatCurrency(rec.expectedAmount),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    triggerReminder(
                                                        PendingReminderPayload(
                                                            id = rec.id.toInt() + 5000,
                                                            title = rec.serviceProvider,
                                                            amount = rec.expectedAmount,
                                                            dueDateMillis = rec.nextRechargeDate,
                                                            dueDateFormatted = formatShortDate(rec.nextRechargeDate),
                                                            type = "UTILITY"
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Remind Me",
                                                    tint = Color(0xFF93C5FD)
                                                )
                                            }
                                            IconButton(
                                                onClick = { recordToDelete = RecordDeleteTarget.Recharge(rec) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_utility_${rec.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Utility",
                                                    tint = Color(0xFFFDA4AF)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ObsidianBlack,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_record_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Record")
        }
    }

    if (showAddDialog) {
        when (selectedTab) {
            RecordTab.LOANS -> {
                AddLoanModal(
                    onConfirm = { loan ->
                        viewModel.addLoan(loan)
                        Toast.makeText(context, "Loan saved & reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
            RecordTab.CARDS -> {
                AddCreditCardModal(
                    onConfirm = { card ->
                        viewModel.addCreditCard(card)
                        Toast.makeText(context, "Card saved & monthly reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
            RecordTab.EMIS -> {
                AddEmiModal(
                    onConfirm = { emi ->
                        viewModel.addEmi(emi)
                        Toast.makeText(context, "EMI saved & reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
            RecordTab.AUTOPAY -> {
                AddAutoPayModal(
                    onConfirm = { auto ->
                        viewModel.addAutoPay(auto)
                        Toast.makeText(context, "AutoPay saved & reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
            RecordTab.INSURANCE -> {
                AddInsuranceModal(
                    onConfirm = { ins ->
                        viewModel.addInsurance(ins)
                        Toast.makeText(context, "Policy saved & reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
            RecordTab.UTILITIES -> {
                AddRechargeModal(
                    onConfirm = { rec ->
                        viewModel.addRecharge(rec)
                        Toast.makeText(context, "Utility bill saved & reminder scheduled", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }
        }
    }

    if (loanToRepay != null) {
        val loan = loanToRepay!!
        RecordRepaymentModal(
            loan = loan,
            onConfirm = { amt, notes, method ->
                viewModel.addLoanRepayment(loan.id, amt, notes, method)
                loanToRepay = null
            },
            onDismiss = { loanToRepay = null }
        )
    }

    if (cardToPay != null) {
        val card = cardToPay!!
        RecordCardPaymentModal(
            card = card,
            onConfirm = { amt, notes ->
                viewModel.recordCreditCardPayment(card.id, amt, notes)
                cardToPay = null
            },
            onDismiss = { cardToPay = null }
        )
    }

    if (recordToDelete != null) {
        val target = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete '${target.label}'? This action will be recorded in the audit log.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            is RecordDeleteTarget.Loan -> viewModel.deleteLoan(target.item)
                            is RecordDeleteTarget.CreditCard -> viewModel.deleteCreditCard(target.item)
                            is RecordDeleteTarget.Emi -> viewModel.deleteEmi(target.item)
                            is RecordDeleteTarget.AutoPay -> viewModel.deleteAutoPay(target.item)
                            is RecordDeleteTarget.Insurance -> viewModel.deleteInsurance(target.item)
                            is RecordDeleteTarget.Recharge -> viewModel.deleteRecharge(target.item)
                        }
                        Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecordsEmptyStateCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String,
    onAddClick: () -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(20.dp),
        accentTint = tint,
        gradientColors = listOf(tint, IndigoAccent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TintedIconBadge(
                icon = icon,
                tint = tint,
                contentDescription = title,
                size = 56.dp,
                iconSize = 28.dp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New")
            }
        }
    }
}

@Composable
fun AddLoanModal(
    onConfirm: (LoanEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var loanType by remember { mutableStateOf("LENT") }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 86400000L) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Loan / Borrowing", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { loanType = "LENT" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loanType == "LENT") IncomeGreen else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Lent",
                            color = if (loanType == "LENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = { loanType = "BORROWED" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loanType == "BORROWED") WarningAmber else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Borrowed",
                            color = if (loanType == "BORROWED") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                CompactTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = "Person or Organization*",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Original Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Expected Repayment Due Date*",
                    selectedDateMillis = dueDateMillis,
                    onDateSelected = { dueDateMillis = it }
                )

                CompactTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = "Purpose / Description",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && personName.isNotBlank()) {
                                onConfirm(
                                    LoanEntity(
                                        personOrOrg = personName.trim(),
                                        loanType = loanType,
                                        originalAmount = amt,
                                        expectedRepaymentDate = dueDateMillis,
                                        description = descText.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRepaymentModal(
    loan: LoanEntity,
    onConfirm: (amount: Double, notes: String, method: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Record Repayment", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("Party: ${loan.personOrOrg} (${loan.loanType})", style = MaterialTheme.typography.bodyMedium)

                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Repayment Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Notes (e.g. Partial repayment via UPI)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, notesText.trim(), paymentMethod)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCreditCardModal(
    onConfirm: (CreditCardEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var outstandingText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("5") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Credit Card", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                CompactTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = "Card Nickname* (e.g. Regalia Gold)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = "Bank Name* (e.g. HDFC Bank)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = "Credit Limit (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = outstandingText,
                    onValueChange = { outstandingText = it },
                    label = "Current Outstanding / Bill Amount (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = "Monthly Reminder Day (1 - 31)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = limitText.toDoubleOrNull() ?: 0.0
                            val outstanding = outstandingText.toDoubleOrNull() ?: 0.0
                            val dueDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 31)
                            if (nickname.isNotBlank() && bank.isNotBlank()) {
                                onConfirm(
                                    CreditCardEntity(
                                        cardNickname = nickname.trim(),
                                        bankName = bank.trim(),
                                        creditLimit = limit,
                                        currentOutstanding = outstanding,
                                        paymentDueDay = dueDay
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save Card")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCardPaymentModal(
    card: CreditCardEntity,
    onConfirm: (amount: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(card.currentOutstanding.toString()) }
    var notesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Record Card Bill Payment", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("Card: ${card.cardNickname}", style = MaterialTheme.typography.bodyMedium)

                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Payment Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Notes (Optional)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, notesText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Record Payment")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEmiModal(
    onConfirm: (EmiEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var loanName by remember { mutableStateOf("") }
    var loanProvider by remember { mutableStateOf("") }
    var originalAmountText by remember { mutableStateOf("") }
    var emiAmountText by remember { mutableStateOf("") }
    var totalInstallmentsText by remember { mutableStateOf("") }
    var remainingInstallmentsText by remember { mutableStateOf("") }
    var nextDueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 15L * 86400000L) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Register EMI / Loan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                CompactTextField(
                    value = loanName,
                    onValueChange = { loanName = it },
                    label = "Loan / Asset Name* (e.g. Phone EMI)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = loanProvider,
                    onValueChange = { loanProvider = it },
                    label = "Provider* (e.g. Bajaj Finserv)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = originalAmountText,
                    onValueChange = { originalAmountText = it },
                    label = "Original Loan Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = emiAmountText,
                    onValueChange = { emiAmountText = it },
                    label = "Monthly EMI Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = totalInstallmentsText,
                    onValueChange = { totalInstallmentsText = it.filter { ch -> ch.isDigit() } },
                    label = "Total Installments (e.g. 24)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = remainingInstallmentsText,
                    onValueChange = { remainingInstallmentsText = it.filter { ch -> ch.isDigit() } },
                    label = "Remaining Installments*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Next EMI Due Date*",
                    selectedDateMillis = nextDueMillis,
                    onDateSelected = { nextDueMillis = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val orig = originalAmountText.toDoubleOrNull() ?: 0.0
                            val emiAmt = emiAmountText.toDoubleOrNull() ?: 0.0
                            val totalInst = totalInstallmentsText.toIntOrNull() ?: 12
                            val remInst = remainingInstallmentsText.toIntOrNull() ?: totalInst
                            if (loanName.isNotBlank() && emiAmt > 0) {
                                onConfirm(
                                    EmiEntity(
                                        loanName = loanName.trim(),
                                        loanProvider = loanProvider.trim(),
                                        originalLoanAmount = orig,
                                        emiAmount = emiAmt,
                                        totalInstallments = totalInst,
                                        remainingInstallments = remInst,
                                        nextPaymentDate = nextDueMillis
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save EMI")
                    }
                }
            }
        }
    }
}

@Composable
fun AddAutoPayModal(
    onConfirm: (AutoPayEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var nextDueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 10L * 86400000L) }
    val frequencies = listOf("MONTHLY", "WEEKLY", "QUARTERLY", "YEARLY")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add AutoPay / Mandate", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                CompactTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = "Merchant / Service* (e.g. Spotify)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(frequencies) { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                DatePickerField(
                    label = "Next AutoPay Due Date*",
                    selectedDateMillis = nextDueMillis,
                    onDateSelected = { nextDueMillis = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (merchant.isNotBlank() && amt > 0) {
                                onConfirm(
                                    AutoPayEntity(
                                        merchantName = merchant.trim(),
                                        category = "Bills",
                                        expectedAmount = amt,
                                        frequency = frequency,
                                        nextPaymentDate = nextDueMillis
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save Mandate")
                    }
                }
            }
        }
    }
}

@Composable
fun AddInsuranceModal(
    onConfirm: (InsuranceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var policyName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var policyType by remember { mutableStateOf("HEALTH") }
    var premiumText by remember { mutableStateOf("") }
    var renewalDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 86400000L) }
    val policyTypes = listOf("HEALTH", "LIFE", "VEHICLE", "TERM")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Insurance Policy", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                CompactTextField(
                    value = policyName,
                    onValueChange = { policyName = it },
                    label = "Policy Name* (e.g. Optima Secure)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CompactTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = "Insurance Company*",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Policy Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(policyTypes) { type ->
                        FilterChip(
                            selected = policyType == type,
                            onClick = { policyType = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                CompactTextField(
                    value = premiumText,
                    onValueChange = { premiumText = it },
                    label = "Premium Amount (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Renewal Due Date*",
                    selectedDateMillis = renewalDateMillis,
                    onDateSelected = { renewalDateMillis = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val prem = premiumText.toDoubleOrNull() ?: 0.0
                            if (policyName.isNotBlank() && prem > 0) {
                                onConfirm(
                                    InsuranceEntity(
                                        policyName = policyName.trim(),
                                        insuranceCompany = company.trim(),
                                        policyType = policyType,
                                        premiumAmount = prem,
                                        renewalDate = renewalDateMillis,
                                        expiryDate = renewalDateMillis
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save Policy")
                    }
                }
            }
        }
    }
}

@Composable
fun AddRechargeModal(
    onConfirm: (RechargeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("MOBILE") }
    var amountText by remember { mutableStateOf("") }
    var nextDueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 28L * 86400000L) }
    val serviceTypes = listOf("MOBILE", "BROADBAND", "ELECTRICITY", "WATER", "GAS", "DTH")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Recharge / Utility", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                CompactTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = "Provider* (e.g. Jio / BESCOM)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Service Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(serviceTypes) { st ->
                        FilterChip(
                            selected = serviceType == st,
                            onClick = { serviceType = st },
                            label = { Text(st.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
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

                DatePickerField(
                    label = "Next Due / Recharge Date*",
                    selectedDateMillis = nextDueMillis,
                    onDateSelected = { nextDueMillis = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val daysLeft = maxOf(1, ((nextDueMillis - System.currentTimeMillis()) / 86400000L).toInt())
                            if (provider.isNotBlank() && amt > 0) {
                                onConfirm(
                                    RechargeEntity(
                                        serviceProvider = provider.trim(),
                                        serviceType = serviceType,
                                        expectedAmount = amt,
                                        validityDays = daysLeft,
                                        nextRechargeDate = nextDueMillis,
                                        expiryDate = nextDueMillis
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Save Reminder")
                    }
                }
            }
        }
    }
}
