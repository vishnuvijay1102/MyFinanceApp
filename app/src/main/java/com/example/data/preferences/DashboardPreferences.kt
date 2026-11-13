package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dashboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "myfinance_dashboard_prefs"
)

enum class DashboardMetricCard(
    val title: String,
    val subtitle: String,
    val defaultOrder: Int
) {
    SAVINGS_BALANCE("Savings Balance", "Savings accounts", 0),
    CURRENT_BALANCE("Current Balance", "Current accounts", 1),
    TRANSFERRED_FUNDS("Transferred Funds", "Transferred / Cash", 2),
    BALANCE_SPENDINGS("Balance Spendings", "Calculated from updates", 3),
    CREDIT_CARDS_DUE("Credit Cards Due", "Outstanding bills", 4),
    REMAINING_EMIS("Remaining EMIs", "Loan obligations", 5),
    MONTHLY_INCOME("Monthly Income", "This month's credits", 6),
    MONTHLY_EXPENSE("Monthly Expenses", "This month's debits", 7),
    MONEY_LENT("Money Lent", "Receivable from others", 8),
    MONEY_BORROWED("Money Borrowed", "Payable to others", 9),
    UPCOMING_DUES("Upcoming Dues", "Active reminders total", 10),
    AVAILABLE_FUNDS("Available Funds", "Total liquid funds", 11);

    companion object {
        val defaultCards: List<DashboardMetricCard> = listOf(
            SAVINGS_BALANCE,
            CURRENT_BALANCE,
            TRANSFERRED_FUNDS,
            BALANCE_SPENDINGS,
            CREDIT_CARDS_DUE,
            REMAINING_EMIS
        )
    }
}

enum class DashboardSection(
    val title: String,
    val description: String
) {
    HERO_BALANCE("Total Available Balance", "Hero summary card at top"),
    METRIC_GRID("Summary Stat Cards", "Customizable 2-column metric cards"),
    EXPENSE_DONUT("Expense Breakdown", "Category spending donut chart"),
    INCOME_EXPENSE_BAR("Income vs Expense", "Monthly comparison bar chart"),
    BANK_ACCOUNTS("Bank Accounts", "Quick view & balance updates"),
    UPCOMING_REMINDERS("Upcoming Reminders", "EMIs, cards, AutoPay & bills"),
    RECENT_TRANSACTIONS("Recent Transactions", "Latest recorded entries");

    companion object {
        val defaultOrder: List<DashboardSection> = entries.toList()
    }
}

data class DashboardLayoutConfig(
    val metricCards: List<DashboardMetricCard> = DashboardMetricCard.defaultCards,
    val sectionOrder: List<DashboardSection> = DashboardSection.defaultOrder,
    val hiddenSections: Set<DashboardSection> = emptySet()
) {
    val visibleSections: List<DashboardSection>
        get() = sectionOrder.filter { it !in hiddenSections }

    val availableCardsToAdd: List<DashboardMetricCard>
        get() = DashboardMetricCard.entries.filter { it !in metricCards }
}

class DashboardPreferencesRepository(private val context: Context) {

    private object Keys {
        val METRIC_CARDS = stringPreferencesKey("dashboard_metric_cards")
        val SECTION_ORDER = stringPreferencesKey("dashboard_section_order")
        val HIDDEN_SECTIONS = stringPreferencesKey("dashboard_hidden_sections")
    }

    val layoutConfig: Flow<DashboardLayoutConfig> = context.dashboardDataStore.data.map { prefs ->
        val savedCardsRaw = prefs[Keys.METRIC_CARDS]
        val metricCards = if (savedCardsRaw == null) {
            DashboardMetricCard.defaultCards
        } else if (savedCardsRaw.isEmpty()) {
            emptyList()
        } else {
            savedCardsRaw.split(",")
                .mapNotNull { id -> runCatching { DashboardMetricCard.valueOf(id) }.getOrNull() }
                .distinct()
        }

        val savedOrderRaw = prefs[Keys.SECTION_ORDER]
        val parsedOrder = if (savedOrderRaw.isNullOrBlank()) {
            DashboardSection.defaultOrder
        } else {
            val existing = savedOrderRaw.split(",")
                .mapNotNull { id -> runCatching { DashboardSection.valueOf(id) }.getOrNull() }
                .distinct()
            val missing = DashboardSection.entries.filter { it !in existing }
            existing + missing
        }

        val hiddenRaw = prefs[Keys.HIDDEN_SECTIONS].orEmpty()
        val hiddenSet = if (hiddenRaw.isBlank()) {
            emptySet()
        } else {
            hiddenRaw.split(",")
                .mapNotNull { id -> runCatching { DashboardSection.valueOf(id) }.getOrNull() }
                .toSet()
        }

        DashboardLayoutConfig(
            metricCards = metricCards,
            sectionOrder = parsedOrder,
            hiddenSections = hiddenSet
        )
    }

    suspend fun saveMetricCards(cards: List<DashboardMetricCard>) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.METRIC_CARDS] = cards.distinct().joinToString(",") { it.name }
        }
    }

    suspend fun saveSectionLayout(order: List<DashboardSection>, hidden: Set<DashboardSection>) {
        context.dashboardDataStore.edit { prefs ->
            prefs[Keys.SECTION_ORDER] = order.distinct().joinToString(",") { it.name }
            prefs[Keys.HIDDEN_SECTIONS] = hidden.joinToString(",") { it.name }
        }
    }

    suspend fun resetToDefault() {
        context.dashboardDataStore.edit { prefs ->
            prefs.remove(Keys.METRIC_CARDS)
            prefs.remove(Keys.SECTION_ORDER)
            prefs.remove(Keys.HIDDEN_SECTIONS)
        }
    }
}
