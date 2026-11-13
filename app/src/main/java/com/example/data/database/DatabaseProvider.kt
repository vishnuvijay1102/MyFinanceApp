package com.example.data.database

import android.content.Context
import androidx.room.Room
import com.example.data.entity.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "myfinance_database"
            )
                .fallbackToDestructiveMigration(true)
                .build()
            INSTANCE = instance

            CoroutineScope(Dispatchers.IO).launch {
                checkAndPopulateDefaultCategories(instance)
            }

            instance
        }
    }

    private suspend fun checkAndPopulateDefaultCategories(database: AppDatabase) {
        try {
            // Check if categories table has entries
            val categoryDao = database.categoryDao()
            val categories = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM categories")
            val count = categories.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (count == 0) {
                populateInitialCategories(database)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun populateInitialCategories(database: AppDatabase) {
        val categoryDao = database.categoryDao()
        val defaultCategories = listOf(
            CategoryEntity(name = "Food", iconName = "restaurant", colorHex = "#F59E0B", isExpense = true),
            CategoryEntity(name = "Groceries", iconName = "shopping_cart", colorHex = "#10B981", isExpense = true),
            CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#EC4899", isExpense = true),
            CategoryEntity(name = "Travel", iconName = "flight", colorHex = "#3B82F6", isExpense = true),
            CategoryEntity(name = "Fuel", iconName = "local_gas_station", colorHex = "#EF4444", isExpense = true),
            CategoryEntity(name = "Medical", iconName = "medical_services", colorHex = "#84CC16", isExpense = true),
            CategoryEntity(name = "Entertainment", iconName = "movie", colorHex = "#8B5CF6", isExpense = true),
            CategoryEntity(name = "Bills", iconName = "receipt_long", colorHex = "#6366F1", isExpense = true),
            CategoryEntity(name = "Rent", iconName = "home", colorHex = "#F97316", isExpense = true),
            CategoryEntity(name = "Insurance", iconName = "shield", colorHex = "#06B6D4", isExpense = true),
            CategoryEntity(name = "Recharge", iconName = "phone_android", colorHex = "#0284C7", isExpense = true),
            CategoryEntity(name = "EMI", iconName = "account_balance", colorHex = "#D97706", isExpense = true),
            CategoryEntity(name = "Others", iconName = "more_horiz", colorHex = "#64748B", isExpense = true),
            CategoryEntity(name = "Salary", iconName = "payments", colorHex = "#10B981", isExpense = false),
            CategoryEntity(name = "Investment", iconName = "trending_up", colorHex = "#059669", isExpense = false),
            CategoryEntity(name = "Business", iconName = "storefront", colorHex = "#2563EB", isExpense = false)
        )
        categoryDao.insertCategories(defaultCategories)
    }

    suspend fun clearAllData(database: AppDatabase) {
        database.clearAllTables()
        populateInitialCategories(database)
    }
}
