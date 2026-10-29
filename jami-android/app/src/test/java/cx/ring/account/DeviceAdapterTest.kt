package cx.ring.account

import android.app.Application
import android.graphics.Typeface
import android.provider.Settings
import android.text.format.DateFormat
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import cx.ring.R
import net.jami.model.AccountDevice
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "en-rUS")
class DeviceAdapterTest {
    @Test
    fun deviceRowPreservesDetailsAndRevocationWhenRecycled() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        val device = AccountDevice("0123456789abcdef".repeat(4), "Laptop", 1700000000L)
        val otherDevice = AccountDevice("abcdef0123456789".repeat(4), "Tablet")
        val revoked = mutableListOf<String>()
        val adapter = DeviceAdapter(
            context, mapOf(device.id to device), "current",
            object : DeviceAdapter.DeviceRevocationListener {
                override fun onDeviceRevocationAsked(deviceId: String) {
                    revoked.add(deviceId)
                }
                override fun onDeviceRename() {
                    throw AssertionError("A remote device must not trigger rename")
                }
            }
        )
        val parent = FrameLayout(context)
        val row = adapter.getView(0, null, parent)
        assertEquals(device.name, row.findViewById<TextView>(R.id.txt_device_name).text.toString())
        val deviceId = row.findViewById<TextView>(R.id.txt_device_id)
        assertEquals(device.id, deviceId.text.toString())
        assertEquals(Typeface.MONOSPACE, deviceId.typeface)
        assertEquals(View.GONE, row.findViewById<View>(R.id.txt_device_thisflag).visibility)

        val previousTimeZone = TimeZone.getDefault()
        val previousHourFormat = Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
            adapter.getView(0, row, parent)
            assertEquals(
                context.getString(R.string.account_device_last_sync, "Nov 14, 22:13"),
                row.findViewById<TextView>(R.id.txt_device_last_sync).text.toString()
            )
        } finally {
            TimeZone.setDefault(previousTimeZone)
            Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, previousHourFormat)
        }

        val revokeButton = row.findViewById<ImageButton>(R.id.btn_device_action)
        assertEquals(context.getString(R.string.account_device_revoke), revokeButton.contentDescription)
        revokeButton.performClick()
        adapter.setData(mapOf(otherDevice.id to otherDevice), "current")
        assertSame(row, adapter.getView(0, row, parent))
        assertEquals(otherDevice.name, row.findViewById<TextView>(R.id.txt_device_name).text.toString())
        assertEquals(otherDevice.id, deviceId.text.toString())
        assertEquals(
            context.getString(R.string.account_device_last_sync, context.getString(R.string.account_device_never_synced)),
            row.findViewById<TextView>(R.id.txt_device_last_sync).text.toString()
        )
        revokeButton.performClick()
        assertEquals(listOf(device.id, otherDevice.id), revoked)
    }

    @Test
    fun syncDatesUseTodayAndAbbreviatedMonthsInEnglish() {
        assertReadableSyncDates("Feb 24", "Feb 25")
    }

    @Test
    @Config(qualifiers = "fr-rFR")
    fun syncDatesUseTodayAndAbbreviatedMonthsInFrench() {
        assertReadableSyncDates("24 f\u00e9vr.", "25 f\u00e9vr.")
    }

    private fun assertReadableSyncDates(firstDate: String, secondDate: String) {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        val adapter = DeviceAdapter(
            context, null, "current",
            object : DeviceAdapter.DeviceRevocationListener {
                override fun onDeviceRevocationAsked(deviceId: String) {}
                override fun onDeviceRename() {}
            }
        )
        val parent = FrameLayout(context)
        val previousTimeZone = TimeZone.getDefault()
        val previousHourFormat = Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            val today = System.currentTimeMillis() / 1000L
            val february24 = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.FEBRUARY, 24, 23, 30)
            }.timeInMillis / 1000L
            val february25 = february24 + 3600L
            var row: View? = null
            for (hourFormat in listOf("12", "24")) {
                Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, hourFormat)
                for ((timestamp, expected) in listOf(
                    today to context.getString(R.string.today),
                    february24 to firstDate,
                    february25 to secondDate,
                    today to context.getString(R.string.today)
                )) {
                    adapter.setData(mapOf("device" to AccountDevice("device", "Laptop", timestamp)), "current")
                    val boundRow = adapter.getView(0, row, parent)
                    row = boundRow
                    val time = DateFormat.getTimeFormat(context).format(Date(timestamp * 1000L))
                        .replace('\u202f', ' ')
                    assertEquals(
                        context.getString(R.string.account_device_last_sync, "$expected, $time"),
                        boundRow.findViewById<TextView>(R.id.txt_device_last_sync).text.toString()
                            .replace('\u202f', ' ')
                    )
                }
            }
        } finally {
            TimeZone.setDefault(previousTimeZone)
            Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, previousHourFormat)
        }
    }

    @Test
    fun lastSyncDatesHandleSecondsAndUnknownValues() {
        Log.injectLogService(object : LogService {
            override fun e(tag: String, message: String) {}
            override fun d(tag: String, message: String) {}
            override fun w(tag: String, message: String) {}
            override fun i(tag: String, message: String) {}
            override fun e(tag: String, message: String, e: Throwable) {}
            override fun d(tag: String, message: String, e: Throwable) {}
            override fun w(tag: String, message: String, e: Throwable) {}
            override fun i(tag: String, message: String, e: Throwable) {}
        })
        assertEquals(1700000000000L, DeviceAdapter.lastSyncDate(1700000000L)!!.time)
        val largestSeconds = Long.MAX_VALUE / 1000L
        assertEquals(largestSeconds * 1000L, DeviceAdapter.lastSyncDate(largestSeconds)!!.time)
        for (timestamp in listOf(0L, -1L, Long.MIN_VALUE, largestSeconds + 1L, Long.MAX_VALUE)) {
            assertNull(DeviceAdapter.lastSyncDate(timestamp))
        }
    }
}
