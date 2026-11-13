package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseProvider
import com.example.data.entity.AccountEntity
import com.example.data.entity.AutoPayEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CreditCardEntity
import com.example.data.entity.CreditCardPaymentEntity
import com.example.data.entity.DetectedNotificationEntity
import com.example.data.entity.EmiEntity
import com.example.data.entity.EmiInstallmentEntity
import com.example.data.entity.InsuranceEntity
import com.example.data.entity.LoanEntity
import com.example.data.entity.LoanRepaymentEntity
import com.example.data.entity.RechargeEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.FinanceRepository
import com.example.data.repository.FinancialOverview
import com.example.data.repository.UpcomingReminderItem
import com.example.service.UpiNotificationListenerService
import com.example.service.UpiPopupNotificationManager
import com.example.ui.components.PieSlice
import com.example.ui.components.getCategoryColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class TimePeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    val repository = FinanceRepository(database, application)

    // Transaction reporting period filter
    private val _transactionPeriod = MutableStateFlow<TimePeriod?>(null)
    val transactionPeriod: StateFlow<TimePeriod?> = _transactionPeriod.asStateFlow()

    fun setTransactionPeriod(period: TimePeriod?) {
        _transactionPeriod.value = period
    }

    // Compute period start and end timestamps
    fun getPeriodRange(period: TimePeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val calStart = Calendar.getInstance()
        when (period) {
            TimePeriod.DAILY -> {
                calStart.set(Calendar.HOUR_OF_DAY, 0)
                calStart.set(Calendar.MINUTE, 0)
                calStart.set(Calendar.SECOND, 0)
                calStart.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.WEEKLY -> {
                calStart.add(Calendar.DAY_OF_YEAR, -7)
            }
            TimePeriod.MONTHLY -> {
                calStart.set(Calendar.DAY_OF_MONTH, 1)
                calStart.set(Calendar.HOUR_OF_DAY, 0)
                calStart.set(Calendar.MINUTE, 0)
                calStart.set(Calendar.SECOND, 0)
                calStart.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.YEARLY -> {
                calStart.set(Calendar.DAY_OF_YEAR, 1)
                calStart.set(Calendar.HOUR_OF_DAY, 0)
                calStart.set(Calendar.MINUTE, 0)
                calStart.set(Calendar.SECOND, 0)
                calStart.set(Calendar.MILLISECOND, 0)
            }
        }
        return Pair(calStart.timeInMillis, now)
    }

    // Dashboard Financial Overview permanently focused on current month
    val financialOverview: StateFlow<FinancialOverview> = run {
        val (start, end) = getPeriodRange(TimePeriod.MONTHLY)
        repository.getFinancialOverview(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialOverview()
    )

    // Accounts
    val activeAccounts: StateFlow<List<AccountEntity>> = repository.allActiveAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Categories
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Pie chart slices for current monthly expenses
    val expenseCategorySlices: StateFlow<List<PieSlice>> = run {
        val (start, end) = getPeriodRange(TimePeriod.MONTHLY)
        repository.allTransactions.map { txns ->
            val expenseTxns = txns.filter { it.transactionType == "EXPENSE" && it.timestamp in start..end }
            val grouped = expenseTxns.groupBy { it.category }
            grouped.map { (cat, list) ->
                PieSlice(
                    label = cat,
                    value = list.sumOf { it.amount },
                    color = getCategoryColor(cat)
                )
            }.sortedByDescending { it.value }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Loans
    val allLoans: StateFlow<List<LoanEntity>> = repository.allLoans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRepayments: StateFlow<List<LoanRepaymentEntity>> = repository.allRepayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Credit Cards
    val allCreditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // EMIs
    val allEmis: StateFlow<List<EmiEntity>> = repository.allEmis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // AutoPays
    val allAutoPays: StateFlow<List<AutoPayEntity>> = repository.allAutoPays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Insurances
    val allInsurances: StateFlow<List<InsuranceEntity>> = repository.allInsurances.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Recharges
    val allRecharges: StateFlow<List<RechargeEntity>> = repository.allRecharges.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Unified Reminders
    val upcomingReminders: StateFlow<List<UpcomingReminderItem>> = repository.getUnifiedReminders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Detected Notifications (Section 5)
    val pendingNotifications: StateFlow<List<DetectedNotificationEntity>> = repository.pendingNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Audit logs
    val recentAuditLogs = repository.recentAuditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // CRUD Methods
    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.addAccount(account)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun updateAccountBalance(accountId: Long, newBalance: Double, notes: String) {
        viewModelScope.launch {
            repository.updateAccountBalance(accountId, newBalance, notes)
        }
    }

    fun transferBetweenAccounts(fromId: Long, toId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.transferBetweenAccounts(fromId, toId, amount, notes)
        }
    }

    fun addLoan(loan: LoanEntity) {
        viewModelScope.launch {
            val id = repository.addLoan(loan)
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 6000,
                title = "${loan.loanType}: ${loan.personOrOrg}",
                amount = loan.originalAmount,
                dueDateMillis = loan.expectedRepaymentDate,
                dueDateFormatted = com.example.ui.components.formatShortDate(loan.expectedRepaymentDate),
                type = "LOAN"
            )
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun addLoanRepayment(loanId: Long, amount: Double, notes: String, paymentMethod: String) {
        viewModelScope.launch {
            repository.addLoanRepayment(
                LoanRepaymentEntity(
                    loanId = loanId,
                    amount = amount,
                    notes = notes,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun addCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            val id = repository.addCreditCard(card)
            val cal = Calendar.getInstance()
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)
            val targetDay = card.paymentDueDay.coerceIn(1, 31)
            if (todayDay > targetDay) {
                cal.add(Calendar.MONTH, 1)
            }
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
            val dueMillis = cal.timeInMillis
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 1000,
                title = card.cardNickname,
                amount = card.currentOutstanding,
                dueDateMillis = dueMillis,
                dueDateFormatted = com.example.ui.components.formatShortDate(dueMillis),
                type = "CREDIT_CARD"
            )
        }
    }

    fun deleteCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
        }
    }

    fun recordCreditCardPayment(cardId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.recordCreditCardPayment(
                CreditCardPaymentEntity(
                    cardId = cardId,
                    amount = amount,
                    notes = notes
                )
            )
        }
    }

    fun addEmi(emi: EmiEntity) {
        viewModelScope.launch {
            val id = repository.addEmi(emi)
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 2000,
                title = emi.loanName,
                amount = emi.emiAmount,
                dueDateMillis = emi.nextPaymentDate,
                dueDateFormatted = com.example.ui.components.formatShortDate(emi.nextPaymentDate),
                type = "EMI"
            )
        }
    }

    fun deleteEmi(emi: EmiEntity) {
        viewModelScope.launch {
            repository.deleteEmi(emi)
        }
    }

    fun addAutoPay(autoPay: AutoPayEntity) {
        viewModelScope.launch {
            val id = repository.addAutoPay(autoPay)
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 3000,
                title = autoPay.merchantName,
                amount = autoPay.expectedAmount,
                dueDateMillis = autoPay.nextPaymentDate,
                dueDateFormatted = com.example.ui.components.formatShortDate(autoPay.nextPaymentDate),
                type = "AUTOPAY"
            )
        }
    }

    fun deleteAutoPay(autoPay: AutoPayEntity) {
        viewModelScope.launch {
            repository.deleteAutoPay(autoPay)
        }
    }

    fun addInsurance(insurance: InsuranceEntity) {
        viewModelScope.launch {
            val id = repository.addInsurance(insurance)
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 4000,
                title = insurance.policyName,
                amount = insurance.premiumAmount,
                dueDateMillis = insurance.renewalDate,
                dueDateFormatted = com.example.ui.components.formatShortDate(insurance.renewalDate),
                type = "INSURANCE"
            )
        }
    }

    fun deleteInsurance(insurance: InsuranceEntity) {
        viewModelScope.launch {
            repository.deleteInsurance(insurance)
        }
    }

    fun addRecharge(recharge: RechargeEntity) {
        viewModelScope.launch {
            val id = repository.addRecharge(recharge)
            com.example.service.ReminderNotificationManager.scheduleReminderNotification(
                context = getApplication(),
                id = id.toInt() + 5000,
                title = recharge.serviceProvider,
                amount = recharge.expectedAmount,
                dueDateMillis = recharge.nextRechargeDate,
                dueDateFormatted = com.example.ui.components.formatShortDate(recharge.nextRechargeDate),
                type = "UTILITY"
            )
        }
    }

    fun deleteRecharge(recharge: RechargeEntity) {
        viewModelScope.launch {
            repository.deleteRecharge(recharge)
        }
    }

    // Detected Notification confirmation actions (Section 5)
    fun confirmDetectedNotification(
        notification: DetectedNotificationEntity,
        category: String,
        sourceAccountId: Long?
    ) {
        viewModelScope.launch {
            repository.confirmDetectedNotification(notification, category, sourceAccountId)
        }
    }

    fun ignoreDetectedNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.updateNotificationStatus(notificationId, "IGNORED")
        }
    }

    // Simulation tool for testing Section 5 home-screen popup detection
    fun simulateDetectedNotification(
        appName: String,
        amount: Double,
        direction: String,
        party: String
    ) {
        viewModelScope.launch {
            val text = when (direction) {
                "DEBIT" -> "Paid ₹$amount to $party using UPI Ref 49201948271 on $appName"
                else -> "Received ₹$amount from $party via UPI Ref 99482019472 on $appName"
            }
            val parsed = UpiNotificationListenerService.parseUpiText(
                appName = appName,
                packageName = "com.upi.simulator",
                text = text
            ) ?: DetectedNotificationEntity(
                appName = appName,
                amount = amount,
                direction = direction,
                recipientOrSender = party,
                upiRef = "UPI/${(100000000..999999999).random()}",
                rawText = text
            )
            val id = repository.insertDetectedNotification(parsed)
            UpiPopupNotificationManager.showUpiPopupNotification(
                context = getApplication(),
                notification = parsed.copy(id = id)
            )
        }
    }

    // CSV Export
    fun exportCsv(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val file = repository.exportTransactionsToCsv()
            onComplete(file)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
