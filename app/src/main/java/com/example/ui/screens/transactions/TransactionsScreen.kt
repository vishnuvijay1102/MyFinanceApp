package com.example.ui.screens.transactions

import android.content.Intent
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.entity.TransactionEntity
import com.example.ui.components.CompactTextField
import com.example.ui.components.GlassCard
import com.example.ui.components.TintedIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.screens.dashboard.AddTransactionModal
import com.example.ui.screens.dashboard.TransactionItemRow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TealAccent
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    var showAddModal by remember { mutableStateOf(false) }
    var selectedTxnForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var txnToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val selectedPeriodFilter by viewModel.transactionPeriod.collectAsStateWithLifecycle()

    // Filtering logic
    val filteredList = allTransactions.filter { txn ->
        val matchesPeriod = if (selectedPeriodFilter == null) {
            true
        } else {
            val (start, end) = viewModel.getPeriodRange(selectedPeriodFilter!!)
            txn.timestamp in start..end
        }
        val matchesType = when (selectedTypeFilter) {
            "ALL" -> true
            else -> txn.transactionType.equals(selectedTypeFilter, ignoreCase = true)
        }
        val matchesCategory = when (selectedCategoryFilter) {
            "ALL" -> true
            else -> txn.category.equals(selectedCategoryFilter, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                txn.description.contains(searchQuery, ignoreCase = true) ||
                txn.recipientOrSender.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true) ||
                txn.upiRefOrUtr.contains(searchQuery, ignoreCase = true)

        matchesPeriod && matchesType && matchesCategory && matchesSearch
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen Header with Export CSV action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredList.size} records found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.exportCsv { file ->
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Transactions CSV"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = IndigoAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            CompactTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search transactions",
                placeholder = "Description, party, UTR...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_transactions_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Period Filter Chips (Moved from Dashboard to Transactions)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedPeriodFilter == null,
                        onClick = { viewModel.setTransactionPeriod(null) },
                        label = { Text("All Time") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(com.example.ui.viewmodel.TimePeriod.values()) { p ->
                    FilterChip(
                        selected = selectedPeriodFilter == p,
                        onClick = { viewModel.setTransactionPeriod(p) },
                        label = { Text(p.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf("ALL", "EXPENSE", "INCOME", "TRANSFER")
                items(types) { t ->
                    FilterChip(
                        selected = selectedTypeFilter == t,
                        onClick = { selectedTypeFilter = t },
                        label = { Text(if (t == "ALL") "All Types" else t) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "ALL",
                        onClick = { selectedCategoryFilter = "ALL" },
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat.name,
                        onClick = { selectedCategoryFilter = cat.name },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transactions List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        accentTint = IndigoAccent,
                        gradientColors = listOf(IndigoAccent, CyanAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            TintedIconBadge(
                                icon = Icons.Default.ReceiptLong,
                                tint = IndigoAccent,
                                contentDescription = "No transactions",
                                size = 56.dp,
                                iconSize = 28.dp
                            )
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Tap + to log a transaction or adjust your period filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 108.dp)
                ) {
                    items(filteredList, key = { it.id }) { txn ->
                        TransactionItemRow(
                            txn = txn,
                            modifier = Modifier.animateItem(),
                            onClick = { selectedTxnForDetail = txn }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Transaction
        FloatingActionButton(
            onClick = { showAddModal = true },
            containerColor = ObsidianBlack,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_txn_screen_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }

    // Add Modal
    if (showAddModal) {
        AddTransactionModal(
            viewModel = viewModel,
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddModal = false }
        )
    }

    // Detail & Edit / Delete Dialog
    if (selectedTxnForDetail != null) {
        val txn = selectedTxnForDetail!!
        TransactionDetailDialog(
            txn = txn,
            onEdit = { updatedTxn ->
                viewModel.updateTransaction(updatedTxn)
                selectedTxnForDetail = null
            },
            onDelete = {
                txnToDelete = txn
                selectedTxnForDetail = null
            },
            onDismiss = { selectedTxnForDetail = null }
        )
    }

    // Delete Confirmation
    if (txnToDelete != null) {
        val txn = txnToDelete!!
        AlertDialog(
            onDismissRequest = { txnToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete '${txn.description}' (${formatCurrency(txn.amount)})? This will be recorded in audit history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txn)
                        txnToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txnToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionDetailDialog(
    txn: TransactionEntity,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editDesc by remember { mutableStateOf(txn.description) }
    var editAmount by remember { mutableStateOf(txn.amount.toString()) }
    var editNotes by remember { mutableStateOf(txn.notes) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Transaction" else "Transaction Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                    }
                }

                if (isEditing) {
                    CompactTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = "Amount (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompactTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = "Description",
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompactTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = "Notes",
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val amt = editAmount.toDoubleOrNull() ?: txn.amount
                                onEdit(
                                    txn.copy(
                                        amount = amt,
                                        description = editDesc.trim(),
                                        notes = editNotes.trim(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                        ) {
                            Text("Save Changes")
                        }
                    }
                } else {
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        accentTint = IndigoAccent,
                        gradientColors = listOf(IndigoAccent, VioletAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = formatCurrency(txn.amount),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(text = "Type: ${txn.transactionType} • ${txn.status}", color = Color(0xFFE2E8F0))
                            Text(text = "Category: ${txn.category}", color = Color(0xFFE2E8F0))
                            Text(text = "Method: ${txn.paymentMethod}", color = Color(0xFFE2E8F0))
                            if (txn.recipientOrSender.isNotEmpty()) {
                                Text(text = "Party: ${txn.recipientOrSender}", color = Color(0xFFE2E8F0))
                            }
                            if (txn.upiRefOrUtr.isNotEmpty()) {
                                Text(text = "Ref / UTR: ${txn.upiRefOrUtr}", color = Color(0xFFE2E8F0))
                            }
                            Text(text = "Date: ${formatDate(txn.timestamp)}", color = Color(0xFFCBD5E1))
                            if (txn.notes.isNotEmpty()) {
                                Text(text = "Notes: ${txn.notes}", color = Color(0xFFCBD5E1))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
