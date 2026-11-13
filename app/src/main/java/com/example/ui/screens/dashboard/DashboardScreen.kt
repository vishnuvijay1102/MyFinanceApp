package com.example.ui.screens.dashboard

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.preferences.DashboardMetricCard
import com.example.data.preferences.DashboardSection
import com.example.data.repository.FinancialOverview
import com.example.data.repository.UpcomingReminderItem
import com.example.service.ReminderNotificationManager
import com.example.ui.components.CompactTextField
import com.example.ui.components.DonutPieChart
import com.example.ui.components.GlassCard
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.MetricStatCard
import com.example.ui.components.TintedIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.formatShortDate
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HeroBrandGradient
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overview by viewModel.financialOverview.collectAsStateWithLifecycle()
    val slices by viewModel.expenseCategorySlices.collectAsStateWithLifecycle()
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val recentTxns by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val reminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val dashboardLayout by viewModel.dashboardLayout.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showAddTxnDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var updatingAccountBalance by remember { mutableStateOf<AccountEntity?>(null) }
    var pendingReminder by remember { mutableStateOf<UpcomingReminderItem?>(null) }

    val dispatchReminder: (UpcomingReminderItem) -> Unit = { item ->
        val dueFormatted = formatShortDate(item.dueDate)
        ReminderNotificationManager.showReminderNotification(
            context = context,
            id = item.id.toInt() + 100,
            title = item.title,
            amount = item.amount,
            dueDateFormatted = dueFormatted,
            type = item.type
        )
        ReminderNotificationManager.scheduleReminderNotification(
            context = context,
            id = item.id.toInt() + 100,
            title = item.title,
            amount = item.amount,
            dueDateMillis = item.dueDate,
            dueDateFormatted = dueFormatted,
            type = item.type
        )
        Toast.makeText(
            context,
            "Reminder sent & scheduled for $dueFormatted",
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

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 108.dp)
        ) {
            // Header: App Title & Current Month Indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MyFinance",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "This Month's Financial Summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IndigoAccent.copy(alpha = 0.16f), CyanAccent.copy(alpha = 0.16f))
                                    )
                                )
                                .border(
                                    1.dp,
                                    IndigoAccent.copy(alpha = 0.30f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Monthly View",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoAccent
                            )
                        }

                        IconButton(
                            onClick = { showCustomizeDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("customize_dashboard_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBlack.copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Customize Dashboard",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (dashboardLayout.visibleSections.isEmpty()) {
                item(key = "empty_dashboard_layout") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        accentTint = IndigoAccent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "All dashboard sections are hidden",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Button(
                                onClick = { showCustomizeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = Color.White)
                            ) {
                                Text("Customize Dashboard")
                            }
                        }
                    }
                }
            }

            dashboardLayout.visibleSections.forEach { section ->
                when (section) {
                    DashboardSection.HERO_BALANCE -> {
                        // SECTION A: Total Available Balance Hero Card
                        item(key = "section_hero_balance") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.34f),
                                                CyanAccent.copy(alpha = 0.32f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        ),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .testTag("available_funds_card"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(HeroBrandGradient))
                                        .padding(20.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "TOTAL AVAILABLE BALANCE",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        letterSpacing = 1.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = formatCurrency(overview.totalAvailableFunds),
                                                    style = MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = (-1).sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                                    .padding(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = "Available Balance",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Savings / Current",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatCurrency(overview.totalSavingsBalance)} / ${formatCurrency(overview.totalCurrentBalance)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Monthly Income / Expense",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "+${formatCurrency(overview.periodIncome)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF7DD3FC),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = " / -${formatCurrency(overview.periodExpense)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFFFDA4AF),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardSection.METRIC_GRID -> {
                        if (dashboardLayout.metricCards.isNotEmpty()) {
                            item(key = "section_metric_grid") {
                                val upcomingTotal = reminders.sumOf { it.amount }
                                Column(
                                    modifier = Modifier.animateItem(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    dashboardLayout.metricCards.chunked(2).forEach { rowCards ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowCards.forEach { card ->
                                                DashboardMetricCardTile(
                                                    card = card,
                                                    overview = overview,
                                                    upcomingDuesTotal = upcomingTotal,
                                                    onNavigateToAccounts = onNavigateToAccounts,
                                                    onNavigateToTransactions = onNavigateToTransactions,
                                                    onNavigateToRecords = onNavigateToRecords,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowCards.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardSection.EXPENSE_DONUT -> {
                        // SECTION B1: Expense Analytics Donut Pie Chart
                        item(key = "section_expense_donut") {
                            DonutPieChart(
                                slices = slices,
                                totalAmount = overview.periodExpense,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    DashboardSection.INCOME_EXPENSE_BAR -> {
                        // SECTION B2: Income vs Expense Bar Chart
                        item(key = "section_income_expense_bar") {
                            IncomeExpenseBarChart(
                                income = overview.periodIncome,
                                expense = overview.periodExpense,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    DashboardSection.BANK_ACCOUNTS -> {
            item(key = "section_accounts_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bank Accounts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToAccounts) {
                        Text("Manage")
                    }
                }
            }

            if (accounts.isEmpty()) {
                item(key = "section_accounts_empty") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        shape = RoundedCornerShape(18.dp),
                        accentTint = IndigoAccent,
                        gradientColors = listOf(IndigoAccent, CyanAccent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                TintedIconBadge(
                                    icon = Icons.Default.AccountBalance,
                                    tint = IndigoAccent,
                                    contentDescription = "Add Bank Account",
                                    size = 42.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No bank accounts yet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Add Current, Savings, or Transferred accounts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onNavigateToAccounts,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            } else {
                item(key = "section_accounts_row") {
                    LazyRow(
                        modifier = Modifier.animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val typeColor = when {
                                acc.accountType.uppercase().contains("SAVING") -> SkyAccent
                                acc.accountType.uppercase().contains("CURRENT") -> AmberAccent
                                else -> VioletAccent
                            }
                            val typeGradient = when {
                                acc.accountType.uppercase().contains("SAVING") -> listOf(SkyAccent, CyanAccent)
                                acc.accountType.uppercase().contains("CURRENT") -> listOf(GoldAccent, OrangeAccent)
                                else -> listOf(VioletAccent, PrimaryIndigo)
                            }
                            val typeIcon = when {
                                acc.accountType.uppercase().contains("SAVING") -> Icons.Default.Savings
                                acc.accountType.uppercase().contains("CURRENT") -> Icons.Default.AccountBalance
                                else -> Icons.Default.Payments
                            }
                            GlassCard(
                                modifier = Modifier
                                    .width(224.dp)
                                    .animateItem()
                                    .testTag("account_card_${acc.id}"),
                                shape = RoundedCornerShape(18.dp),
                                accentTint = typeColor,
                                gradientColors = typeGradient
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TintedIconBadge(
                                                icon = typeIcon,
                                                tint = typeColor,
                                                contentDescription = acc.accountType,
                                                size = 32.dp,
                                                iconSize = 16.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = acc.accountName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { updatingAccountBalance = acc },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Update Balance",
                                                modifier = Modifier.size(18.dp),
                                                tint = typeColor
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${acc.bankName} • ${acc.accountType}${if (acc.lastFourDigits.isNotEmpty()) " • ${acc.lastFourDigits}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = formatCurrency(acc.currentBalance),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Updated ${formatShortDate(acc.updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
                    }

                    DashboardSection.UPCOMING_REMINDERS -> {
            if (reminders.isNotEmpty()) {
                item(key = "section_reminders_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Reminders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onNavigateToRecords) {
                            Text("View All")
                        }
                    }
                }

                item(key = "section_reminders_card") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        shape = RoundedCornerShape(20.dp),
                        accentTint = GoldAccent,
                        gradientColors = listOf(GoldAccent, RoseAccent)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            reminders.take(4).forEach { item ->
                                val isOverdue = item.status == "OVERDUE"
                                val statusFgColor = when {
                                    isOverdue -> Color(0xFFFDA4AF)
                                    item.daysRemaining <= 2 -> Color(0xFFFDE68A)
                                    else -> Color(0xFF93C5FD)
                                }
                                val typeIcon = when (item.type) {
                                    "CREDIT_CARD" -> Icons.Default.CreditCard
                                    "EMI" -> Icons.Default.ReceiptLong
                                    "INSURANCE" -> Icons.Default.Security
                                    "AUTOPAY" -> Icons.Default.Subscriptions
                                    "LOAN" -> Icons.Default.Handshake
                                    else -> Icons.Default.Payments
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TintedIconBadge(
                                            icon = typeIcon,
                                            tint = statusFgColor,
                                            contentDescription = item.type,
                                            size = 42.dp,
                                            iconSize = 20.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Due: ${formatShortDate(item.dueDate)} • ${if (isOverdue) "OVERDUE" else "in ${item.daysRemaining}d"}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = statusFgColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatCurrency(item.amount),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (isOverdue) Color(0xFFFDA4AF) else Color.White
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFCBD5E1),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    !ReminderNotificationManager.hasNotificationPermission(context)
                                                ) {
                                                    pendingReminder = item
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    dispatchReminder(item)
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = "Remind Me",
                                                tint = Color(0xFF93C5FD),
                                                modifier = Modifier.size(20.dp)
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

                    DashboardSection.RECENT_TRANSACTIONS -> {
            item(key = "section_recent_txns_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("View All")
                    }
                }
            }

            if (recentTxns.isEmpty()) {
                item(key = "section_recent_txns_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent transactions found. Tap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentTxns, key = { it.id }) { txn ->
                    TransactionItemRow(
                        txn = txn,
                        modifier = Modifier.animateItem()
                    )
                }
            }
                    }
                }
            }
        }

        // Floating Action Button to Add Transaction
        FloatingActionButton(
            onClick = { showAddTxnDialog = true },
            containerColor = ObsidianBlack,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_transaction_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }

    // Customize Dashboard Dialog
    if (showCustomizeDialog) {
        CustomizeDashboardDialog(
            config = dashboardLayout,
            onSwapMetricCard = { index, replacement -> viewModel.swapMetricCard(index, replacement) },
            onMoveMetricCard = { index, delta -> viewModel.moveMetricCard(index, delta) },
            onAddMetricCard = { card -> viewModel.addMetricCard(card) },
            onRemoveMetricCard = { card -> viewModel.removeMetricCard(card) },
            onMoveSection = { index, delta -> viewModel.moveSection(index, delta) },
            onToggleSection = { section -> viewModel.toggleSectionVisibility(section) },
            onResetToDefault = { viewModel.resetDashboardLayout() },
            onDismiss = { showCustomizeDialog = false }
        )
    }

    // Add Transaction Dialog
    if (showAddTxnDialog) {
        AddTransactionModal(
            viewModel = viewModel,
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddTxnDialog = false }
        )
    }

    // Update Balance Dialog
    if (updatingAccountBalance != null) {
        val acc = updatingAccountBalance!!
        UpdateBalanceModal(
            account = acc,
            onConfirm = { newBal, notes ->
                viewModel.updateAccountBalance(acc.id, newBal, notes)
                updatingAccountBalance = null
            },
            onDismiss = { updatingAccountBalance = null }
        )
    }
}

@Composable
private fun DashboardMetricCardTile(
    card: DashboardMetricCard,
    overview: FinancialOverview,
    upcomingDuesTotal: Double,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (card) {
        DashboardMetricCard.SAVINGS_BALANCE -> MetricStatCard(
            title = card.title,
            amount = overview.totalSavingsBalance,
            subtitle = card.subtitle,
            icon = Icons.Default.Savings,
            iconColor = SkyAccent,
            gradientColors = listOf(SkyAccent, CyanAccent),
            onClick = onNavigateToAccounts,
            modifier = modifier
        )
        DashboardMetricCard.CURRENT_BALANCE -> MetricStatCard(
            title = card.title,
            amount = overview.totalCurrentBalance,
            subtitle = card.subtitle,
            icon = Icons.Default.AccountBalance,
            iconColor = AmberAccent,
            gradientColors = listOf(GoldAccent, OrangeAccent),
            onClick = onNavigateToAccounts,
            modifier = modifier
        )
        DashboardMetricCard.TRANSFERRED_FUNDS -> MetricStatCard(
            title = card.title,
            amount = overview.totalTransferredBalance,
            subtitle = card.subtitle,
            icon = Icons.Default.Payments,
            iconColor = PrimaryIndigo,
            gradientColors = listOf(PrimaryIndigo, VioletAccent),
            onClick = onNavigateToAccounts,
            modifier = modifier
        )
        DashboardMetricCard.BALANCE_SPENDINGS -> MetricStatCard(
            title = card.title,
            amount = overview.calculatedSpendingFromBalanceUpdates,
            subtitle = card.subtitle,
            icon = Icons.Default.TrendingDown,
            iconColor = RoseAccent,
            gradientColors = listOf(RoseAccent, ExpenseRed),
            isNegative = true,
            onClick = onNavigateToAccounts,
            modifier = modifier
        )
        DashboardMetricCard.CREDIT_CARDS_DUE -> MetricStatCard(
            title = card.title,
            amount = overview.totalCreditCardOutstanding,
            subtitle = card.subtitle,
            icon = Icons.Default.CreditCard,
            iconColor = VioletAccent,
            gradientColors = listOf(VioletAccent, RoseAccent),
            isNegative = true,
            onClick = onNavigateToRecords,
            modifier = modifier
        )
        DashboardMetricCard.REMAINING_EMIS -> MetricStatCard(
            title = card.title,
            amount = overview.totalRemainingEmiLiabilities,
            subtitle = card.subtitle,
            icon = Icons.Default.ReceiptLong,
            iconColor = CyanAccent,
            gradientColors = listOf(CyanAccent, IndigoAccent),
            isNegative = true,
            onClick = onNavigateToRecords,
            modifier = modifier
        )
        DashboardMetricCard.MONTHLY_INCOME -> MetricStatCard(
            title = card.title,
            amount = overview.periodIncome,
            subtitle = card.subtitle,
            icon = Icons.Default.TrendingUp,
            iconColor = SkyAccent,
            gradientColors = listOf(SkyAccent, IndigoAccent),
            onClick = onNavigateToTransactions,
            modifier = modifier
        )
        DashboardMetricCard.MONTHLY_EXPENSE -> MetricStatCard(
            title = card.title,
            amount = overview.periodExpense,
            subtitle = card.subtitle,
            icon = Icons.Default.TrendingDown,
            iconColor = ExpenseRed,
            gradientColors = listOf(RoseAccent, OrangeAccent),
            isNegative = true,
            onClick = onNavigateToTransactions,
            modifier = modifier
        )
        DashboardMetricCard.MONEY_LENT -> MetricStatCard(
            title = card.title,
            amount = overview.totalMoneyLent,
            subtitle = card.subtitle,
            icon = Icons.Default.Handshake,
            iconColor = CyanAccent,
            gradientColors = listOf(CyanAccent, SkyAccent),
            onClick = onNavigateToRecords,
            modifier = modifier
        )
        DashboardMetricCard.MONEY_BORROWED -> MetricStatCard(
            title = card.title,
            amount = overview.totalMoneyBorrowed,
            subtitle = card.subtitle,
            icon = Icons.Default.Handshake,
            iconColor = WarningAmber,
            gradientColors = listOf(GoldAccent, RoseAccent),
            isNegative = true,
            onClick = onNavigateToRecords,
            modifier = modifier
        )
        DashboardMetricCard.UPCOMING_DUES -> MetricStatCard(
            title = card.title,
            amount = upcomingDuesTotal,
            subtitle = card.subtitle,
            icon = Icons.Default.NotificationsActive,
            iconColor = GoldAccent,
            gradientColors = listOf(GoldAccent, OrangeAccent),
            isNegative = upcomingDuesTotal > 0,
            onClick = onNavigateToRecords,
            modifier = modifier
        )
        DashboardMetricCard.AVAILABLE_FUNDS -> MetricStatCard(
            title = card.title,
            amount = overview.totalAvailableFunds,
            subtitle = card.subtitle,
            icon = Icons.Default.Payments,
            iconColor = PrimaryIndigo,
            gradientColors = listOf(PrimaryIndigo, CyanAccent),
            onClick = onNavigateToAccounts,
            modifier = modifier
        )
    }
}

@Composable
fun TransactionItemRow(
    txn: TransactionEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("txn_item_${txn.id}"),
        shape = RoundedCornerShape(16.dp),
        accentTint = getCategoryColor(txn.category),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                TintedIconBadge(
                    icon = getCategoryIcon(txn.category),
                    tint = getCategoryColor(txn.category),
                    contentDescription = txn.category,
                    size = 42.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = txn.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (txn.isAutomatic) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(IndigoAccent.copy(alpha = 0.28f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${txn.category} • ${txn.paymentMethod} • ${formatDate(txn.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isIncome = txn.transactionType == "INCOME"
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatCurrency(txn.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isIncome) Color(0xFF6EE7B7) else Color(0xFFFDA4AF)
                )
                if (txn.recipientOrSender.isNotEmpty()) {
                    Text(
                        text = txn.recipientOrSender,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    viewModel: FinanceViewModel,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedCategory by remember { mutableStateOf(if (categories.isNotEmpty()) categories.first().name else "Food") }
    var selectedPaymentMethod by remember { mutableStateOf("UPI") }
    var selectedAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var recipientText by remember { mutableStateOf("") }
    var upiRefText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val paymentMethods = listOf("UPI", "CASH", "DEBIT_CARD", "CREDIT_CARD", "NET_BANKING")
    val types = listOf("EXPENSE", "INCOME", "TRANSFER")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Type Chips (Expense, Income, Transfer)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { type ->
                            val label = type.lowercase().replaceFirstChar { it.uppercase() }
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (type) {
                                        "INCOME" -> SkyAccent
                                        "TRANSFER" -> VioletAccent
                                        else -> PrimaryIndigo
                                    },
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    CompactTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "Amount (₹)*",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transaction_amount")
                    )
                }

                item {
                    CompactTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = "Description*",
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transaction_desc")
                    )
                }

                item {
                    CompactTextField(
                        value = recipientText,
                        onValueChange = { recipientText = it },
                        label = "Recipient / Sender / Merchant",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category selection
                item {
                    Text("Category", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCategory = cat.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Account Selection
                if (accounts.isNotEmpty()) {
                    item {
                        Text("Account", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                        var expanded by remember { mutableStateOf(false) }
                        val currentAccName = accounts.firstOrNull { it.id == selectedAccountId }?.accountName ?: "Select Account"
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = currentAccName,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.accountName} (${formatCurrency(acc.currentBalance)})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Method
                item {
                    Text("Payment Method", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPaymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method, fontSize = 13.sp) },
                                    onClick = {
                                        selectedPaymentMethod = method
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    CompactTextField(
                        value = upiRefText,
                        onValueChange = { upiRefText = it },
                        label = "UPI Ref / UTR (Optional)",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    CompactTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = "Notes (Optional)",
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (amount > 0 && descriptionText.isNotBlank()) {
                                    viewModel.addTransaction(
                                        TransactionEntity(
                                            amount = amount,
                                            transactionType = selectedType,
                                            description = descriptionText.trim(),
                                            category = selectedCategory,
                                            paymentMethod = selectedPaymentMethod,
                                            sourceAccountId = selectedAccountId,
                                            recipientOrSender = recipientText.trim(),
                                            upiRefOrUtr = upiRefText.trim(),
                                            notes = notesText.trim()
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = Color.White),
                            modifier = Modifier.testTag("save_transaction_button")
                        ) {
                            Text("Save Transaction")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateBalanceModal(
    account: AccountEntity,
    onConfirm: (newBalance: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newBalanceText by remember { mutableStateOf(account.currentBalance.toString()) }
    var notesText by remember { mutableStateOf("") }

    val newBal = newBalanceText.toDoubleOrNull() ?: account.currentBalance
    val diff = newBal - account.currentBalance

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Update Account Balance",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "${account.accountName} (${account.bankName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Current Recorded Balance: ${formatCurrency(account.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                CompactTextField(
                    value = newBalanceText,
                    onValueChange = { newBalanceText = it },
                    label = "New Confirmed Balance (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_balance")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculated Difference:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${if (diff >= 0) "+" else ""}${formatCurrency(diff)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (diff >= 0) IncomeGreen else ExpenseRed
                    )
                }

                CompactTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Notes (e.g. Passbook sync / ATM withdrawal)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = newBalanceText.toDoubleOrNull()
                            if (parsed != null) {
                                onConfirm(parsed, notesText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = Color.White)
                    ) {
                        Text("Save Snapshot")
                    }
                }
            }
        }
    }
}
