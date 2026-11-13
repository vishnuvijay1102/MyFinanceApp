package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.firstOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MyFinance", appName)
  }

  @Test
  fun `test UPI notification parser debit extraction`() {
    val sampleText = "Paid Rs. 250 to Olive BistroIndiranagar using UPI Ref 332145899 on Google Pay"
    val parsed = com.example.service.UpiNotificationListenerService.parseUpiText(
      appName = "Google Pay",
      packageName = "com.google.android.apps.nbu.paisa.user",
      text = sampleText
    )
    org.junit.Assert.assertNotNull(parsed)
    assertEquals(250.0, parsed!!.amount, 0.01)
    assertEquals("DEBIT", parsed.direction)
  }

  @Test
  fun `upi notification posts heads-up system popup with confirm and ignore actions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val entity = com.example.data.entity.DetectedNotificationEntity(
      id = 42L,
      packageName = "com.google.android.apps.nbu.paisa.user",
      appName = "Google Pay",
      rawText = "Paid Rs. 350 to Swiggy",
      amount = 350.0,
      direction = "DEBIT",
      recipientOrSender = "Swiggy",
      upiRef = "123456789"
    )
    com.example.service.UpiPopupNotificationManager.showUpiPopupNotification(context, entity)

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNm = org.robolectric.Shadows.shadowOf(nm)
    val posted = shadowNm.getNotification(10_042)
    org.junit.Assert.assertNotNull(posted)
    assertEquals(2, posted.actions.size)
    assertEquals("Confirm & Save", posted.actions[0].title.toString())
    assertEquals("Ignore", posted.actions[1].title.toString())
  }

  @Test
  fun `dashboard preferences persist swapped cards, hidden sections, and reset`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.preferences.DashboardPreferencesRepository(context)
    prefs.resetToDefault()

    val customCards = com.example.data.preferences.DashboardMetricCard.defaultCards.map {
      if (it == com.example.data.preferences.DashboardMetricCard.CURRENT_BALANCE) {
        com.example.data.preferences.DashboardMetricCard.MONTHLY_INCOME
      } else {
        it
      }
    }
    prefs.saveMetricCards(customCards)
    prefs.saveSectionLayout(
      order = com.example.data.preferences.DashboardSection.defaultOrder.reversed(),
      hidden = setOf(com.example.data.preferences.DashboardSection.EXPENSE_DONUT)
    )

    val loaded = prefs.layoutConfig.firstOrNull()
    org.junit.Assert.assertNotNull(loaded)
    assertEquals(customCards, loaded!!.metricCards)
    assertEquals(
      com.example.data.preferences.DashboardSection.defaultOrder.reversed(),
      loaded.sectionOrder
    )
    assertEquals(
      setOf(com.example.data.preferences.DashboardSection.EXPENSE_DONUT),
      loaded.hiddenSections
    )

    prefs.resetToDefault()
    val reset = prefs.layoutConfig.firstOrNull()!!
    assertEquals(com.example.data.preferences.DashboardMetricCard.defaultCards, reset.metricCards)
    assertEquals(emptySet<com.example.data.preferences.DashboardSection>(), reset.hiddenSections)
  }
}
