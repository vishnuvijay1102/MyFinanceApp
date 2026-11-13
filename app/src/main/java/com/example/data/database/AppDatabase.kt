package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.AccountDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.AutoPayDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CreditCardDao
import com.example.data.dao.DetectedNotificationDao
import com.example.data.dao.EmiDao
import com.example.data.dao.InsuranceDao
import com.example.data.dao.LoanDao
import com.example.data.dao.RechargeDao
import com.example.data.dao.TransactionDao
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

@Database(
    entities = [
        AccountEntity::class,
        BalanceSnapshotEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        CreditCardEntity::class,
        CreditCardBillEntity::class,
        CreditCardPaymentEntity::class,
        EmiEntity::class,
        EmiInstallmentEntity::class,
        AutoPayEntity::class,
        AutoPayHistoryEntity::class,
        InsuranceEntity::class,
        InsurancePaymentEntity::class,
        RechargeEntity::class,
        AuditLogEntity::class,
        DetectedNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun loanDao(): LoanDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun emiDao(): EmiDao
    abstract fun autoPayDao(): AutoPayDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun rechargeDao(): RechargeDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun detectedNotificationDao(): DetectedNotificationDao
}
