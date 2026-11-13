package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["accountName"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountName: String,
    val bankName: String,
    val accountType: String, // BANK, CASH, SAVINGS, WALLET
    val lastFourDigits: String = "",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "balance_snapshots",
    indices = [Index(value = ["accountId"]), Index(value = ["timestamp"])]
)
data class BalanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val previousBalance: Double,
    val newBalance: Double,
    val difference: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["sourceAccountId"]),
        Index(value = ["transactionType"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val transactionType: String, // EXPENSE, INCOME, TRANSFER, LOAN_REPAYMENT, REFUND
    val description: String,
    val category: String,
    val paymentMethod: String, // UPI, CASH, DEBIT_CARD, CREDIT_CARD, NET_BANKING
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val recipientOrSender: String = "",
    val upiRefOrUtr: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // SUCCESS, PENDING, FAILED
    val notes: String = "",
    val isAutomatic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isExpense: Boolean = true,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "loans",
    indices = [Index(value = ["status"]), Index(value = ["startDate"])]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personOrOrg: String,
    val loanType: String, // LENT (Receivable), BORROWED (Payable)
    val originalAmount: Double,
    val interestRate: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val expectedRepaymentDate: Long = System.currentTimeMillis(),
    val description: String = "",
    val status: String = "ACTIVE", // ACTIVE, FULLY_REPAID, SETTLED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loan_repayments",
    indices = [Index(value = ["loanId"])]
)
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val repaymentDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMethod: String = "UPI"
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardNickname: String,
    val bankName: String,
    val creditLimit: Double,
    val currentOutstanding: Double = 0.0,
    val billingCycleDay: Int = 15,
    val paymentDueDay: Int = 5,
    val minimumDue: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "credit_card_bills",
    indices = [Index(value = ["cardId"]), Index(value = ["dueDate"])]
)
data class CreditCardBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val statementMonth: String,
    val statementDate: Long = System.currentTimeMillis(),
    val dueDate: Long,
    val totalDue: Double,
    val minimumDue: Double = 0.0,
    val status: String = "UNPAID", // UNPAID, PARTIALLY_PAID, PAID
    val paidAmount: Double = 0.0
)

@Entity(
    tableName = "credit_card_payments",
    indices = [Index(value = ["cardId"])]
)
data class CreditCardPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val billId: Long? = null,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val fromAccountId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "emis")
data class EmiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanName: String,
    val loanProvider: String,
    val originalLoanAmount: Double,
    val interestRate: Double = 0.0,
    val emiAmount: Double,
    val startDate: Long = System.currentTimeMillis(),
    val monthlyDueDay: Int = 5,
    val totalInstallments: Int,
    val remainingInstallments: Int,
    val nextPaymentDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, CLOSED
    val notes: String = ""
)

@Entity(
    tableName = "emi_installments",
    indices = [Index(value = ["emiId"])]
)
data class EmiInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emiId: Long,
    val installmentNumber: Int,
    val amount: Double,
    val dueDate: Long,
    val paidDate: Long? = null,
    val status: String = "UPCOMING", // UPCOMING, PAID, OVERDUE, SKIPPED, CANCELLED
    val notes: String = ""
)

@Entity(tableName = "autopays")
data class AutoPayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val category: String,
    val expectedAmount: Double,
    val frequency: String = "MONTHLY", // WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM
    val startDate: Long = System.currentTimeMillis(),
    val nextPaymentDate: Long = System.currentTimeMillis(),
    val sourceAccountId: Long? = null,
    val mandateStatus: String = "ACTIVE", // ACTIVE, PAUSED, CANCELLED, UNKNOWN
    val notes: String = ""
)

@Entity(
    tableName = "autopay_history",
    indices = [Index(value = ["autoPayId"])]
)
data class AutoPayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val autoPayId: Long,
    val deductionDate: Long = System.currentTimeMillis(),
    val amount: Double,
    val status: String = "CONFIRMED", // CONFIRMED, SKIPPED, FAILED
    val notes: String = ""
)

@Entity(
    tableName = "insurances",
    indices = [Index(value = ["expiryDate"])]
)
data class InsuranceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val policyName: String,
    val insuranceCompany: String,
    val policyNumberMasked: String = "",
    val policyType: String, // HEALTH, VEHICLE, LIFE, TERM, OTHER
    val premiumAmount: Double,
    val paymentFrequency: String = "YEARLY",
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis(),
    val renewalDate: Long = System.currentTimeMillis(),
    val paymentStatus: String = "ACTIVE", // ACTIVE, DUE_SOON, OVERDUE, EXPIRED
    val notes: String = ""
)

@Entity(
    tableName = "insurance_payments",
    indices = [Index(value = ["insuranceId"])]
)
data class InsurancePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val insuranceId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val coveragePeriodEnd: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "recharges",
    indices = [Index(value = ["expiryDate"])]
)
data class RechargeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceProvider: String,
    val serviceType: String, // MOBILE, BROADBAND, DTH, ELECTRICITY, WATER, GAS, OTHER
    val identifierMasked: String = "",
    val expectedAmount: Double,
    val validityDays: Int = 30,
    val expiryDate: Long = System.currentTimeMillis(),
    val nextRechargeDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "UPI",
    val notes: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val recordId: Long,
    val action: String, // INSERT, UPDATE, DELETE
    val oldValueJson: String? = null,
    val newValueJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

@Entity(tableName = "detected_notifications")
data class DetectedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String, // Google Pay, PhonePe, Paytm, BHIM, Bank SMS
    val packageName: String = "",
    val amount: Double,
    val direction: String, // DEBIT, CREDIT
    val recipientOrSender: String = "",
    val upiRef: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String = "",
    val status: String = "PENDING_CONFIRMATION" // PENDING_CONFIRMATION, CONFIRMED, IGNORED
)
