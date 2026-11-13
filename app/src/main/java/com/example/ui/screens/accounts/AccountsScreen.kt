package com.example.ui.screens.accounts

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.entity.BalanceSnapshotEntity
import com.example.ui.components.CompactTextField
import com.example.ui.components.GlassCard
import com.example.ui.components.TintedIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.screens.dashboard.UpdateBalanceModal
import com.example.ui.theme.AccountsIndigoGradient
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.repository.recentSnapshots.collectAsStateWithLifecycle(emptyList())
    val overview by viewModel.financialOverview.collectAsStateWithLifecycle()

    var showAddAccountModal by remember { mutableStateOf(false) }
    var showTransferModal by remember { mutableStateOf(false) }
    var accountToUpdate by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var showHistoryModal by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bank Accounts",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Current, Savings & Transferred balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        if (accounts.size >= 2) {
                            IconButton(onClick = { showTransferModal = true }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Funds", tint = IndigoAccent)
                            }
                        }
                        IconButton(onClick = { showHistoryModal = true }) {
                            Icon(Icons.Default.History, contentDescription = "Balance History", tint = AmberAccent)
                        }
                    }
                }
            }

            // Total & Breakdown across Current, Savings, Transferred + Calculated Spendings
            item {
                val totalBalance = accounts.sumOf { it.currentBalance }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.3f), IndigoAccent.copy(alpha = 0.35f))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(AccountsIndigoGradient))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TOTAL AVAILABLE BALANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalBalance),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                            if (accounts.size >= 2) {
                                Button(
                                    onClick = { showTransferModal = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = ObsidianBlack
                                    )
                                ) {
                                    Text("Transfer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                                Text(formatCurrency(overview.totalCurrentBalance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFDE68A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Savings", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                                Text(formatCurrency(overview.totalSavingsBalance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF7DD3FC), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Transferred", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                                Text(formatCurrency(overview.totalTransferredBalance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC4B5FD), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Calculated Spendings (from balance updates)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatCurrency(overview.calculatedSpendingFromBalanceUpdates),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFFFDA4AF)
                                )
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    GlassCard(
                        shape = RoundedCornerShape(18.dp),
                        accentTint = IndigoAccent,
                        gradientColors = listOf(IndigoAccent, CyanAccent),
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
                                icon = Icons.Default.AccountBalance,
                                tint = IndigoAccent,
                                contentDescription = "No accounts",
                                size = 56.dp,
                                iconSize = 28.dp
                            )
                            Text(
                                text = "No Bank Accounts Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Add your Current, Savings, or Transferred accounts and update balances anytime to automatically calculate spendings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Button(
                                onClick = { showAddAccountModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Account")
                            }
                        }
                    }
                }
            } else {
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
                    val latestSnap = snapshots.firstOrNull { it.accountId == acc.id }

                    GlassCard(
                        shape = RoundedCornerShape(18.dp),
                        accentTint = typeColor,
                        gradientColors = typeGradient,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .testTag("account_full_card_${acc.id}")
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
                                        icon = typeIcon,
                                        tint = typeColor,
                                        contentDescription = acc.accountType,
                                        size = 44.dp,
                                        iconSize = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = acc.accountName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${acc.bankName} • ${acc.accountType}${if (acc.lastFourDigits.isNotEmpty()) " • ${acc.lastFourDigits}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(onClick = { accountToDelete = acc }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Account", tint = Color(0xFFFDA4AF))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Current Balance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = formatCurrency(acc.currentBalance),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                    if (latestSnap != null) {
                                        val isSpent = latestSnap.difference < 0
                                        Text(
                                            text = if (isSpent)
                                                "Last update: -${formatCurrency(kotlin.math.abs(latestSnap.difference))} spent"
                                            else
                                                "Last update: +${formatCurrency(latestSnap.difference)} added",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSpent) Color(0xFFFDA4AF) else Color(0xFF6EE7B7)
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { accountToUpdate = acc },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("update_balance_btn_${acc.id}")
                                ) {
                                    Text("Update Amount")
                                }
                            }

                            if (acc.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: ${acc.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to Add Account
        FloatingActionButton(
            onClick = { showAddAccountModal = true },
            containerColor = ObsidianBlack,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_account_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Account")
        }
    }

    // Add Account Dialog
    if (showAddAccountModal) {
        AddAccountDialog(
            onConfirm = { account ->
                viewModel.addAccount(account)
                showAddAccountModal = false
            },
            onDismiss = { showAddAccountModal = false }
        )
    }

    // Update Balance Modal
    if (accountToUpdate != null) {
        val acc = accountToUpdate!!
        UpdateBalanceModal(
            account = acc,
            onConfirm = { newBal, notes ->
                viewModel.updateAccountBalance(acc.id, newBal, notes)
                accountToUpdate = null
            },
            onDismiss = { accountToUpdate = null }
        )
    }

    // Delete Account Confirmation
    if (accountToDelete != null) {
        val acc = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Remove ${acc.accountName} (${formatCurrency(acc.currentBalance)}) from your accounts?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(acc.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Transfer Modal
    if (showTransferModal && accounts.size >= 2) {
        TransferFundsDialog(
            accounts = accounts,
            onConfirm = { fromId, toId, amt, notes ->
                viewModel.transferBetweenAccounts(fromId, toId, amt, notes)
                showTransferModal = false
            },
            onDismiss = { showTransferModal = false }
        )
    }

    // Balance Snapshot History Modal
    if (showHistoryModal) {
        BalanceHistoryDialog(
            snapshots = snapshots,
            onDismiss = { showHistoryModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onConfirm: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("CURRENT") }
    var lastFourDigits by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val types = listOf("CURRENT", "SAVINGS", "TRANSFERRED")

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add New Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                CompactTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = "Account Nickname* (e.g. HDFC Salary)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = "Bank / Entity Name* (e.g. HDFC Bank, Cash)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = accountType,
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
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, fontSize = 13.sp) },
                                onClick = {
                                    accountType = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                CompactTextField(
                    value = lastFourDigits,
                    onValueChange = { if (it.length <= 4) lastFourDigits = it },
                    label = "Last 4 Digits (Optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it },
                    label = "Current Amount (₹)*",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val openBal = openingBalanceText.toDoubleOrNull() ?: 0.0
                            if (accountName.isNotBlank() && bankName.isNotBlank()) {
                                onConfirm(
                                    AccountEntity(
                                        accountName = accountName.trim(),
                                        bankName = bankName.trim(),
                                        accountType = accountType,
                                        lastFourDigits = lastFourDigits.trim(),
                                        openingBalance = openBal,
                                        currentBalance = openBal,
                                        notes = notesText.trim()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = Color.White)
                    ) {
                        Text("Add Account")
                    }
                }
            }
        }
    }
}

@Composable
fun TransferFundsDialog(
    accounts: List<AccountEntity>,
    onConfirm: (fromId: Long, toId: Long, amount: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fromAccountId by remember { mutableStateOf(accounts.first().id) }
    var toAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.first().id) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Transfer Funds",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text("From Account:", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                accounts.forEach { acc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { fromAccountId = acc.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (fromAccountId == acc.id) IndigoAccent else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${acc.accountName} (${formatCurrency(acc.currentBalance)})",
                                fontSize = 12.5.sp,
                                color = if (fromAccountId == acc.id) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text("To Account:", fontSize = 11.5.sp, style = MaterialTheme.typography.labelSmall)
                accounts.filter { it.id != fromAccountId }.forEach { acc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { toAccountId = acc.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (toAccountId == acc.id) VioletAccent else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${acc.accountName} (${formatCurrency(acc.currentBalance)})",
                                fontSize = 12.5.sp,
                                color = if (toAccountId == acc.id) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                CompactTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount to Transfer (₹)*",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CompactTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Transfer Reason (Optional)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccountId != toAccountId) {
                                onConfirm(fromAccountId, toAccountId, amt, notesText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = Color.White)
                    ) {
                        Text("Confirm Transfer")
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceHistoryDialog(
    snapshots: List<BalanceSnapshotEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Balance Snapshot History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (snapshots.isEmpty()) {
                    Text(
                        text = "No balance update snapshots recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapshots) { snap ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatCurrency(snap.newBalance),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${if (snap.difference >= 0) "+" else ""}${formatCurrency(snap.difference)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (snap.difference >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }
                                    Text(
                                        text = "${formatDate(snap.timestamp)}${if (snap.notes.isNotEmpty()) " • ${snap.notes}" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
