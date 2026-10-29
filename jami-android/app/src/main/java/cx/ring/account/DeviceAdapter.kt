/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.account

import android.content.Context
import android.widget.BaseAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import cx.ring.R
import android.widget.ImageButton
import android.widget.TextView
import net.jami.model.AccountDevice
import net.jami.utils.Log
import android.text.format.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.util.ArrayList
import java.util.Date
import java.util.Formatter

class DeviceAdapter(
    private val mContext: Context, devices: Map<String, AccountDevice>?,
    currentDeviceId: String?,
    private val mListener: DeviceRevocationListener
) : BaseAdapter() {
    private val mDevices = ArrayList<AccountDevice>()
    private var mCurrentDeviceId: String? = null
    private val dateFormatter by lazy {
        Formatter(StringBuilder(48), mContext.resources.configuration.locales[0])
    }

    fun setData(devices: Map<String, AccountDevice>?, currentDeviceId: String?) {
        mDevices.clear()
        mCurrentDeviceId = currentDeviceId
        if (!devices.isNullOrEmpty()) {
            mDevices.ensureCapacity(devices.size)
            mDevices.addAll(devices.values)
        }
        mDevices.removeAll { d -> d.id == mCurrentDeviceId }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = mDevices.size

    override fun getItem(i: Int): Any = mDevices[i]

    override fun getItemId(i: Int): Long = 0

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false)
        val device = mDevices[i]
        val isCurrentDevice = device.id.contentEquals(mCurrentDeviceId)
        val action = view.findViewById<ImageButton>(R.id.btn_device_action)
        val thisDevice = view.findViewById<TextView>(R.id.txt_device_thisflag)
        view.findViewById<TextView>(R.id.txt_device_name).text = device.name
        view.findViewById<TextView>(R.id.txt_device_id).text = device.id
        view.findViewById<TextView>(R.id.txt_device_last_sync).apply {
            val timestamp = lastSyncDate(device.lastSync)?.let {
                val zone = ZoneId.systemDefault()
                val date = if (it.toInstant().atZone(zone).toLocalDate() == LocalDate.now(zone)) {
                    mContext.getString(R.string.today)
                } else {
                    (dateFormatter.out() as StringBuilder).setLength(0)
                    DateUtils.formatDateRange(
                        mContext, dateFormatter, it.time, it.time,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
                        zone.id
                    ).toString()
                }
                (dateFormatter.out() as StringBuilder).setLength(0)
                val time = DateUtils.formatDateRange(
                    mContext, dateFormatter, it.time, it.time, DateUtils.FORMAT_SHOW_TIME, zone.id
                ).toString()
                "$date, $time"
            } ?: mContext.getString(R.string.account_device_never_synced)
            text = mContext.getString(R.string.account_device_last_sync, timestamp)
            visibility = if (isCurrentDevice) View.GONE else View.VISIBLE
        }
        if (isCurrentDevice) {
            thisDevice.visibility = View.VISIBLE
            action.setImageResource(R.drawable.baseline_edit_rounded_24)
            action.contentDescription = mContext.getString(R.string.rename_device_button)
            action.setOnClickListener { mListener.onDeviceRename() }
        } else {
            thisDevice.visibility = View.GONE
            action.setImageResource(R.drawable.baseline_cancel_24)
            action.contentDescription = mContext.getString(R.string.account_device_revoke)
            action.setOnClickListener { mListener.onDeviceRevocationAsked(device.id) }
        }
        return view
    }

    interface DeviceRevocationListener {
        fun onDeviceRevocationAsked(deviceId: String)
        fun onDeviceRename()
    }

    init {
        setData(devices, currentDeviceId)
    }

    companion object {
        internal fun lastSyncDate(lastSync: Long): Date? {
            if (lastSync <= 0L)
                return null
            if (lastSync > Long.MAX_VALUE / 1000L) {
                Log.w("DeviceAdapter", "Invalid device lastSync timestamp")
                return null
            }
            return Date(lastSync * 1000L)
        }
    }
}