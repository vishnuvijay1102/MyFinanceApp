package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseProvider
import com.example.data.entity.AccountEntity
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.AutoPayEntity
import com.example.data.entity.AutoPayHistoryEntity
import com.example.data.entity.BalanceSnapshotEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CreditCardBillEntity
import com.example.data.entity.CreditCardEntity
import com.example.data.entity.CreditCardPaymentEntity
import com.example.data.entity.DetectedNotificationEntity
import com.example.data.entity.EmiEntity
import com.example.data.entity.EmiInstallmentEntity
import com.example.data.entity.InsuranceEntity
import com.example.data.entity.InsurancePaymentEntity
import com.example.data.entity.LoanEntity
import com.example.data.entity.LoanRepaymentEntity
import com.example.data.entity.RechargeEntity
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinancialOverview(
    val totalAvailableFunds: Double = 0.0,
    val totalCurrentBalance: Double = 0.0,
    val totalSavingsBalance: Double = 0.0,
    val totalTransferredBalance: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val calculatedSpendingFromBalanceUpdates: Double = 0.0,
    val totalMoneyLent: Double = 0.0,
    val totalMoneyBorrowed: Double = 0.0,
    val totalCreditCardOutstanding: Double = 0.0,
    val totalRemainingEmiLiabilities: Double = 0.0
)

data class UpcomingReminderItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dueDate: Long,
    val type: String, // CREDIT_CARD, EMI, INSURANCE, RECHARGE, AUTOPAY, LOAN
    val status: String, // OVERDUE, DUE_TODAY, UPCOMING
    val daysRemaining: Int = 0
)

class FinanceRepository(private val database: AppDatabase, private val context: Context) {

    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val loanDao = database.loanDao()
    private val creditCardDao = database.creditCardDao()
    private val emiDao = database.emiDao()
    private val autoPayDao = database.autoPayDao()
    private val insuranceDao = database.insuranceDao()
    private val rechargeDao = database.rechargeDao()
    private val auditLogDao = database.auditLogDao()
    private val detectedNotificationDao = database.detectedNotificationDao()

    // Accounts
    val allActiveAccounts: Flow<List<AccountEntity>> = accountDao.getAllActiveAccounts()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val recentSnapshots: Flow<List<BalanceSnapshotEntity>> = accountDao.getRecentSnapshots()

    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        val id = accountDao.insertAccount(account)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "accounts",
                recordId = id,
                action = "INSERT",
                details = "Created account ${account.accountName} with initial balance ${account.openingBalance}"
            )
        )
        id
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.updateAccount(account)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "accounts",
                recordId = account.id,
                action = "UPDATE",
                details = "Updated account ${account.accountName}"
            )
        )
    }

    suspend fun deleteAccount(accountId: Long) = withContext(Dispatchers.IO) {
        val existing = accountDao.getAccountById(accountId)
        accountDao.deleteAccountById(accountId)
        if (existing != null) {
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    tableName = "accounts",
                    recordId = accountId,
                    action = "DELETE",
                    details = "Deleted account ${existing.accountName} (${existing.accountType})"
                )
            )
        }
    }

    suspend fun updateAccountBalance(
        accountId: Long,
        newBalance: Double,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountById(accountId) ?: return@withContext
        val oldBalance = account.currentBalance
        val diff = newBalance - oldBalance
        accountDao.updateAccountBalance(accountId, newBalance)
        accountDao.insertBalanceSnapshot(
            BalanceSnapshotEntity(
                accountId = accountId,
                previousBalance = oldBalance,
                newBalance = newBalance,
                difference = diff,
                notes = notes
            )
        )
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "accounts",
                recordId = accountId,
                action = "UPDATE",
                details = "Balance changed from ₹$oldBalance to ₹$newBalance (diff: ₹$diff). Notes: $notes"
            )
        )
    }

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val from = accountDao.getAccountById(fromAccountId) ?: return@withContext
        val to = accountDao.getAccountById(toAccountId) ?: return@withContext

        updateAccountBalance(fromAccountId, from.currentBalance - amount, "Transfer to ${to.accountName}: $notes")
        updateAccountBalance(toAccountId, to.currentBalance + amount, "Transfer from ${from.accountName}: $notes")

        val txnId = transactionDao.insertTransaction(
            TransactionEntity(
                amount = amount,
                transactionType = "TRANSFER",
                description = "Transfer: ${from.accountName} -> ${to.accountName}",
                category = "Others",
                paymentMethod = "TRANSFER",
                sourceAccountId = fromAccountId,
                destinationAccountId = toAccountId,
                notes = notes
            )
        )
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "transactions",
                recordId = txnId,
                action = "INSERT",
                details = "Transferred ₹$amount from ${from.accountName} to ${to.accountName}"
            )
        )
    }

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(15)

    suspend fun addTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val id = transactionDao.insertTransaction(transaction)
        transaction.sourceAccountId?.let { accId ->
            val acc = accountDao.getAccountById(accId)
            if (acc != null) {
                val updatedBal = when (transaction.transactionType) {
                    "EXPENSE" -> acc.currentBalance - transaction.amount
                    "INCOME" -> acc.currentBalance + transaction.amount
                    else -> acc.currentBalance
                }
                accountDao.updateAccountBalance(accId, updatedBal)
            }
        }
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "transactions",
                recordId = id,
                action = "INSERT",
                details = "Added ${transaction.transactionType} of ₹${transaction.amount} for ${transaction.description}"
            )
        )
        id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "transactions",
                recordId = transaction.id,
                action = "UPDATE",
                details = "Updated transaction #${transaction.id}: ${transaction.description} (₹${transaction.amount})"
            )
        )
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "transactions",
                recordId = transaction.id,
                action = "DELETE",
                details = "Deleted transaction #${transaction.id}: ${transaction.description} (₹${transaction.amount})"
            )
        )
    }

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun addCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    // Loans (Borrowing & Lending)
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    val allRepayments: Flow<List<LoanRepaymentEntity>> = loanDao.getAllRepayments()

    suspend fun addLoan(loan: LoanEntity): Long = withContext(Dispatchers.IO) {
        loanDao.insertLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
        loanDao.deleteLoanById(loan.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "loans",
                recordId = loan.id,
                action = "DELETE",
                details = "Deleted ${loan.loanType} loan record for ${loan.personOrOrg} (₹${loan.originalAmount})"
            )
        )
    }

    suspend fun addLoanRepayment(repayment: LoanRepaymentEntity) = withContext(Dispatchers.IO) {
        loanDao.insertRepayment(repayment)
        val loan = loanDao.getLoanById(repayment.loanId)
        if (loan != null) {
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    tableName = "loan_repayments",
                    recordId = repayment.loanId,
                    action = "INSERT",
                    details = "Repayment of ₹${repayment.amount} recorded for ${loan.personOrOrg}"
                )
            )
        }
    }

    // Credit Cards
    val allCreditCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCreditCards()

    suspend fun addCreditCard(card: CreditCardEntity): Long = withContext(Dispatchers.IO) {
        creditCardDao.insertCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCardEntity) = withContext(Dispatchers.IO) {
        creditCardDao.deleteCreditCardById(card.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "credit_cards",
                recordId = card.id,
                action = "DELETE",
                details = "Deleted credit card ${card.cardNickname} (${card.bankName})"
            )
        )
    }

    suspend fun recordCreditCardPayment(payment: CreditCardPaymentEntity) = withContext(Dispatchers.IO) {
        creditCardDao.insertPayment(payment)
    }

    // EMIs
    val allEmis: Flow<List<EmiEntity>> = emiDao.getAllEmis()

    suspend fun addEmi(emi: EmiEntity): Long = withContext(Dispatchers.IO) {
        val emiId = emiDao.insertEmi(emi)
        val installments = mutableListOf<EmiInstallmentEntity>()
        val monthMs = 30L * 86400000L
        for (i in 1..emi.totalInstallments) {
            installments.add(
                EmiInstallmentEntity(
                    emiId = emiId,
                    installmentNumber = i,
                    amount = emi.emiAmount,
                    dueDate = emi.startDate + (i * monthMs),
                    status = if (i <= (emi.totalInstallments - emi.remainingInstallments)) "PAID" else "UPCOMING"
                )
            )
        }
        emiDao.insertInstallments(installments)
        emiId
    }

    suspend fun deleteEmi(emi: EmiEntity) = withContext(Dispatchers.IO) {
        emiDao.deleteEmiById(emi.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "emis",
                recordId = emi.id,
                action = "DELETE",
                details = "Deleted EMI ${emi.loanName} (${emi.loanProvider})"
            )
        )
    }

    suspend fun payEmiInstallment(emiId: Long, installment: EmiInstallmentEntity) = withContext(Dispatchers.IO) {
        emiDao.updateInstallment(installment.copy(status = "PAID", paidDate = System.currentTimeMillis()))
    }

    // AutoPay & Subscriptions
    val allAutoPays: Flow<List<AutoPayEntity>> = autoPayDao.getAllAutoPays()

    suspend fun addAutoPay(autoPay: AutoPayEntity): Long = withContext(Dispatchers.IO) {
        autoPayDao.insertAutoPay(autoPay)
    }

    suspend fun updateAutoPay(autoPay: AutoPayEntity) = withContext(Dispatchers.IO) {
        autoPayDao.updateAutoPay(autoPay)
    }

    suspend fun deleteAutoPay(autoPay: AutoPayEntity) = withContext(Dispatchers.IO) {
        autoPayDao.deleteAutoPayById(autoPay.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "autopays",
                recordId = autoPay.id,
                action = "DELETE",
                details = "Deleted AutoPay mandate ${autoPay.merchantName} (₹${autoPay.expectedAmount})"
            )
        )
    }

    // Insurance
    val allInsurances: Flow<List<InsuranceEntity>> = insuranceDao.getAllInsurances()

    suspend fun addInsurance(insurance: InsuranceEntity): Long = withContext(Dispatchers.IO) {
        insuranceDao.insertInsurance(insurance)
    }

    suspend fun deleteInsurance(insurance: InsuranceEntity) = withContext(Dispatchers.IO) {
        insuranceDao.deleteInsuranceById(insurance.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "insurances",
                recordId = insurance.id,
                action = "DELETE",
                details = "Deleted insurance policy ${insurance.policyName} (${insurance.insuranceCompany})"
            )
        )
    }

    // Recharges
    val allRecharges: Flow<List<RechargeEntity>> = rechargeDao.getAllRecharges()

    suspend fun addRecharge(recharge: RechargeEntity): Long = withContext(Dispatchers.IO) {
        rechargeDao.insertRecharge(recharge)
    }

    suspend fun deleteRecharge(recharge: RechargeEntity) = withContext(Dispatchers.IO) {
        rechargeDao.deleteRechargeById(recharge.id)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                tableName = "recharges",
                recordId = recharge.id,
                action = "DELETE",
                details = "Deleted utility/recharge ${recharge.serviceProvider} (${recharge.serviceType})"
            )
        )
    }

    // Audit Logs
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs(50)

    // Detected Notifications
    val pendingNotifications: Flow<List<DetectedNotificationEntity>> =
        detectedNotificationDao.getPendingNotifications()
    val allNotifications: Flow<List<DetectedNotificationEntity>> =
        detectedNotificationDao.getAllNotifications()

    suspend fun insertDetectedNotification(notification: DetectedNotificationEntity): Long =
        withContext(Dispatchers.IO) {
            detectedNotificationDao.insertNotification(notification)
        }

    suspend fun updateNotificationStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        detectedNotificationDao.updateStatus(id, status)
    }

    suspend fun confirmDetectedNotification(
        notification: DetectedNotificationEntity,
        category: String = "Others",
        sourceAccountId: Long? = null
    ) = withContext(Dispatchers.IO) {
        detectedNotificationDao.updateStatus(notification.id, "CONFIRMED")
        addTransaction(
            TransactionEntity(
                amount = notification.amount,
                transactionType = if (notification.direction == "CREDIT") "INCOME" else "EXPENSE",
                description = if (notification.recipientOrSender.isNotEmpty())
                    "${notification.appName}: ${notification.recipientOrSender}"
                else
                    "${notification.appName} Payment",
                category = category,
                paymentMethod = "UPI",
                sourceAccountId = sourceAccountId,
                recipientOrSender = notification.recipientOrSender,
                upiRefOrUtr = notification.upiRef,
                timestamp = notification.timestamp,
                status = "SUCCESS",
                notes = "Automatically detected via ${notification.appName}",
                isAutomatic = true
            )
        )
    }

    // Comprehensive Financial Overview Calculation (Focused on Current Balances & Spendings)
    fun getFinancialOverview(periodStartTime: Long, periodEndTime: Long): Flow<FinancialOverview> {
        val coreFlow = combine(allActiveAccounts, allTransactions, recentSnapshots) { accs, txns, snaps -> Triple(accs, txns, snaps) }
        val loansFlow = combine(allLoans, allRepayments) { loans, repayments -> Pair(loans, repayments) }
        val liabilitiesFlow = combine(allCreditCards, allEmis) { cards, emis -> Pair(cards, emis) }

        return combine(coreFlow, loansFlow, liabilitiesFlow) { (accounts, transactions, snapshots), (loans, repayments), (cards, emis) ->
            var currentBal = 0.0
            var savingsBal = 0.0
            var transferredBal = 0.0

            accounts.forEach { acc ->
                val type = acc.accountType.uppercase()
                when {
                    type.contains("CURRENT") -> currentBal += acc.currentBalance
                    type.contains("SAVING") -> savingsBal += acc.currentBalance
                    else -> transferredBal += acc.currentBalance
                }
            }

            val availableFunds = currentBal + savingsBal + transferredBal

            var inc = 0.0
            var exp = 0.0
            transactions.forEach { t ->
                if (t.timestamp in periodStartTime..periodEndTime) {
                    if (t.transactionType == "INCOME") inc += t.amount
                    if (t.transactionType == "EXPENSE") exp += t.amount
                }
            }

            // Calculate spendings based on manual balance updates (where new balance is lower than previous balance)
            var calculatedSpending = 0.0
            snapshots.forEach { snap ->
                if (snap.timestamp in periodStartTime..periodEndTime && snap.difference < 0) {
                    calculatedSpending += kotlin.math.abs(snap.difference)
                }
            }

            val repaymentByLoan = repayments.groupBy { it.loanId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            var moneyLent = 0.0
            var moneyBorrowed = 0.0

            loans.forEach { loan ->
                val repaid = repaymentByLoan[loan.id] ?: 0.0
                val remaining = maxOf(0.0, loan.originalAmount - repaid)
                if (loan.loanType.equals("LENT", ignoreCase = true)) {
                    moneyLent += remaining
                } else {
                    moneyBorrowed += remaining
                }
            }

            val ccOutstanding = cards.sumOf { it.currentOutstanding }
            val emiLiabilities = emis.filter { it.status == "ACTIVE" }
                .sumOf { it.remainingInstallments * it.emiAmount }

            FinancialOverview(
                totalAvailableFunds = availableFunds,
                totalCurrentBalance = currentBal,
                totalSavingsBalance = savingsBal,
                totalTransferredBalance = transferredBal,
                periodIncome = inc,
                periodExpense = exp,
                calculatedSpendingFromBalanceUpdates = calculatedSpending,
                totalMoneyLent = moneyLent,
                totalMoneyBorrowed = moneyBorrowed,
                totalCreditCardOutstanding = ccOutstanding,
                totalRemainingEmiLiabilities = emiLiabilities
            )
        }
    }

    // Unified Reminders Calculation with explicit days remaining
    fun getUnifiedReminders(): Flow<List<UpcomingReminderItem>> {
        val p1 = combine(allCreditCards, allEmis) { cards, emis -> Pair(cards, emis) }
        val p2 = combine(allInsurances, allRecharges) { ins, recs -> Pair(ins, recs) }
        val p3 = combine(allAutoPays, allLoans) { autopays, loans -> Pair(autopays, loans) }

        return combine(p1, p2, p3) { (cards, emis), (insurances, recharges), (autopays, loans) ->
            val list = mutableListOf<UpcomingReminderItem>()
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            cards.forEach { card ->
                if (card.currentOutstanding > 0) {
                    val cal = java.util.Calendar.getInstance()
                    val todayDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val targetDay = card.paymentDueDay.coerceIn(1, 31)
                    if (todayDay > targetDay) {
                        cal.add(java.util.Calendar.MONTH, 1)
                    }
                    val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    cal.set(java.util.Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val dueDate = cal.timeInMillis
                    val diffMs = dueDate - now
                    val daysRemaining = maxOf(0, kotlin.math.ceil(diffMs.toDouble() / dayMs).toInt())
                    val status = if (diffMs < 0) "OVERDUE" else if (daysRemaining == 0) "DUE_TODAY" else "UPCOMING"
                    list.add(
                        UpcomingReminderItem(
                            id = card.id,
                            title = "${card.cardNickname} Bill",
                            subtitle = if (daysRemaining == 0) "Due today" else "Due in $daysRemaining days",
                            amount = card.currentOutstanding,
                            dueDate = dueDate,
                            type = "CREDIT_CARD",
                            status = status,
                            daysRemaining = daysRemaining
                        )
                    )
                }
            }

            emis.filter { it.status == "ACTIVE" }.forEach { emi ->
                val status = if (emi.nextPaymentDate < now) "OVERDUE" else if (emi.nextPaymentDate - now < dayMs) "DUE_TODAY" else "UPCOMING"
                val daysRemaining = maxOf(0, ((emi.nextPaymentDate - now) / dayMs).toInt())
                list.add(
                    UpcomingReminderItem(
                        id = emi.id,
                        title = "${emi.loanName} EMI",
                        subtitle = "${emi.loanProvider} (${emi.remainingInstallments} installments left)",
                        amount = emi.emiAmount,
                        dueDate = emi.nextPaymentDate,
                        type = "EMI",
                        status = status,
                        daysRemaining = daysRemaining
                    )
                )
            }

            insurances.forEach { ins ->
                val status = if (ins.renewalDate < now) "OVERDUE" else if (ins.renewalDate - now < 30 * dayMs) "DUE_TODAY" else "UPCOMING"
                val daysRemaining = maxOf(0, ((ins.renewalDate - now) / dayMs).toInt())
                list.add(
                    UpcomingReminderItem(
                        id = ins.id,
                        title = ins.policyName,
                        subtitle = "${ins.insuranceCompany} renewal",
                        amount = ins.premiumAmount,
                        dueDate = ins.renewalDate,
                        type = "INSURANCE",
                        status = status,
                        daysRemaining = daysRemaining
                    )
                )
            }

            recharges.forEach { rec ->
                val status = if (rec.nextRechargeDate < now) "OVERDUE" else if (rec.nextRechargeDate - now < 3 * dayMs) "DUE_TODAY" else "UPCOMING"
                val daysRemaining = maxOf(0, ((rec.nextRechargeDate - now) / dayMs).toInt())
                list.add(
                    UpcomingReminderItem(
                        id = rec.id,
                        title = "${rec.serviceProvider} (${rec.serviceType})",
                        subtitle = "Validity expiring soon",
                        amount = rec.expectedAmount,
                        dueDate = rec.nextRechargeDate,
                        type = "RECHARGE",
                        status = status,
                        daysRemaining = daysRemaining
                    )
                )
            }

            autopays.filter { it.mandateStatus == "ACTIVE" }.forEach { auto ->
                val status = if (auto.nextPaymentDate < now) "OVERDUE" else "UPCOMING"
                val daysRemaining = maxOf(0, ((auto.nextPaymentDate - now) / dayMs).toInt())
                list.add(
                    UpcomingReminderItem(
                        id = auto.id,
                        title = "${auto.merchantName} AutoPay",
                        subtitle = "${auto.frequency} mandate",
                        amount = auto.expectedAmount,
                        dueDate = auto.nextPaymentDate,
                        type = "AUTOPAY",
                        status = status,
                        daysRemaining = daysRemaining
                    )
                )
            }

            loans.filter { it.status == "ACTIVE" }.forEach { loan ->
                val status = if (loan.expectedRepaymentDate < now) "OVERDUE" else "UPCOMING"
                val daysRemaining = maxOf(0, ((loan.expectedRepaymentDate - now) / dayMs).toInt())
                list.add(
                    UpcomingReminderItem(
                        id = loan.id,
                        title = "${loan.loanType}: ${loan.personOrOrg}",
                        subtitle = "Expected repayment",
                        amount = loan.originalAmount,
                        dueDate = loan.expectedRepaymentDate,
                        type = "LOAN",
                        status = status,
                        daysRemaining = daysRemaining
                    )
                )
            }

            list.sortBy { it.dueDate }
            list
        }
    }

    // CSV Export of transactions
    suspend fun exportTransactionsToCsv(): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "MyFinance_Transactions_${System.currentTimeMillis()}.csv")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val transactions = database.openHelper.readableDatabase.query(
            "SELECT id, amount, transactionType, description, category, paymentMethod, recipientOrSender, upiRefOrUtr, timestamp, status, notes FROM transactions ORDER BY timestamp DESC"
        )

        file.printWriter().use { out ->
            out.println("ID,Amount,Type,Description,Category,PaymentMethod,Party,UPI_Ref,DateTime,Status,Notes")
            while (transactions.moveToNext()) {
                val id = transactions.getLong(0)
                val amount = transactions.getDouble(1)
                val type = transactions.getString(2)
                val desc = transactions.getString(3).replace(",", ";")
                val cat = transactions.getString(4)
                val payMethod = transactions.getString(5)
                val party = transactions.getString(6).replace(",", ";")
                val ref = transactions.getString(7)
                val time = sdf.format(Date(transactions.getLong(8)))
                val status = transactions.getString(9)
                val notes = (transactions.getString(10) ?: "").replace(",", ";")
                out.println("$id,$amount,$type,\"$desc\",$cat,$payMethod,\"$party\",$ref,\"$time\",$status,\"$notes\"")
            }
        }
        transactions.close()
        file
    }

    // Clear All User Data & Re-seed only basic categories
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        DatabaseProvider.clearAllData(database)
    }
}
