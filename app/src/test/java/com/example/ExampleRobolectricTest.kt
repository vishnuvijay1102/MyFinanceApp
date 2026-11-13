package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}
