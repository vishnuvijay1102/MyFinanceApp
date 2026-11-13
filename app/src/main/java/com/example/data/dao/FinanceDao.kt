package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY id ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalance = :newBalance, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, newBalance: Double, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalanceSnapshot(snapshot: BalanceSnapshotEntity): Long

    @Query("SELECT * FROM balance_snapshots WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getSnapshotsForAccount(accountId: Long): Flow<List<BalanceSnapshotEntity>>

    @Query("SELECT * FROM balance_snapshots ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSnapshots(): Flow<List<BalanceSnapshotEntity>>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 15): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'INCOME' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Double?>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY repaymentDate DESC")
    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_repayments ORDER BY repaymentDate DESC")
    fun getAllRepayments(): Flow<List<LoanRepaymentEntity>>
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity): Long

    @Update
    suspend fun updateCreditCard(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardById(id: Long)

    @Query("SELECT * FROM credit_card_bills WHERE cardId = :cardId ORDER BY dueDate DESC")
    fun getBillsForCard(cardId: Long): Flow<List<CreditCardBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: CreditCardBillEntity): Long

    @Update
    suspend fun updateBill(bill: CreditCardBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CreditCardPaymentEntity): Long

    @Query("SELECT * FROM credit_card_payments WHERE cardId = :cardId ORDER BY paymentDate DESC")
    fun getPaymentsForCard(cardId: Long): Flow<List<CreditCardPaymentEntity>>
}

@Dao
interface EmiDao {
    @Query("SELECT * FROM emis ORDER BY startDate DESC")
    fun getAllEmis(): Flow<List<EmiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmi(emi: EmiEntity): Long

    @Update
    suspend fun updateEmi(emi: EmiEntity)

    @Query("DELETE FROM emis WHERE id = :id")
    suspend fun deleteEmiById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<EmiInstallmentEntity>)

    @Query("SELECT * FROM emi_installments WHERE emiId = :emiId ORDER BY installmentNumber ASC")
    fun getInstallmentsForEmi(emiId: Long): Flow<List<EmiInstallmentEntity>>

    @Update
    suspend fun updateInstallment(installment: EmiInstallmentEntity)
}

@Dao
interface AutoPayDao {
    @Query("SELECT * FROM autopays ORDER BY nextPaymentDate ASC")
    fun getAllAutoPays(): Flow<List<AutoPayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoPay(autoPay: AutoPayEntity): Long

    @Update
    suspend fun updateAutoPay(autoPay: AutoPayEntity)

    @Query("DELETE FROM autopays WHERE id = :id")
    suspend fun deleteAutoPayById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AutoPayHistoryEntity): Long

    @Query("SELECT * FROM autopay_history WHERE autoPayId = :autoPayId ORDER BY deductionDate DESC")
    fun getHistoryForAutoPay(autoPayId: Long): Flow<List<AutoPayHistoryEntity>>
}

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurances ORDER BY expiryDate ASC")
    fun getAllInsurances(): Flow<List<InsuranceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurance(insurance: InsuranceEntity): Long

    @Update
    suspend fun updateInsurance(insurance: InsuranceEntity)

    @Query("DELETE FROM insurances WHERE id = :id")
    suspend fun deleteInsuranceById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: InsurancePaymentEntity): Long

    @Query("SELECT * FROM insurance_payments WHERE insuranceId = :insuranceId ORDER BY paymentDate DESC")
    fun getPaymentsForInsurance(insuranceId: Long): Flow<List<InsurancePaymentEntity>>
}

@Dao
interface RechargeDao {
    @Query("SELECT * FROM recharges ORDER BY expiryDate ASC")
    fun getAllRecharges(): Flow<List<RechargeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecharge(recharge: RechargeEntity): Long

    @Update
    suspend fun updateRecharge(recharge: RechargeEntity)

    @Query("DELETE FROM recharges WHERE id = :id")
    suspend fun deleteRechargeById(id: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}

@Dao
interface DetectedNotificationDao {
    @Query("SELECT * FROM detected_notifications WHERE status = 'PENDING_CONFIRMATION' ORDER BY timestamp DESC")
    fun getPendingNotifications(): Flow<List<DetectedNotificationEntity>>

    @Query("SELECT * FROM detected_notifications ORDER BY timestamp DESC LIMIT 30")
    fun getAllNotifications(): Flow<List<DetectedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DetectedNotificationEntity): Long

    @Update
    suspend fun updateNotification(notification: DetectedNotificationEntity)

    @Query("UPDATE detected_notifications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT * FROM detected_notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): DetectedNotificationEntity?

    @Query("DELETE FROM detected_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
