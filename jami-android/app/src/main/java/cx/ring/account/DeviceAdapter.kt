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
import cx.ring.views.TwoButtonEditText
import android.widget.TextView
import net.jami.model.AccountDevice
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date

class DeviceAdapter(
    private val mContext: Context, devices: Map<String, AccountDevice>?,
    currentDeviceId: String?,
    private val mListener: DeviceRevocationListener
) : BaseAdapter() {
    private val mDevices = ArrayList<AccountDevice>()
    private var mCurrentDeviceId: String? = null

    fun setData(devices: Map<String, AccountDevice>?, currentDeviceId: String?) {
        mDevices.clear()
        mCurrentDeviceId = currentDeviceId
        if (devices != null && devices.isNotEmpty()) {
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
        val devId: TwoButtonEditText = view.findViewById(R.id.txt_device_id)
        val thisDevice = view.findViewById<TextView>(R.id.txt_device_thisflag)
        devId.text = device.name
        var hint = device.id
        hint = hint.substring(0, (hint.length * 0.66).toInt())
        devId.setHint(hint)
        view.findViewById<TextView>(R.id.txt_device_last_sync).apply {
            val timestamp = if (device.lastSync > 0) {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(Math.multiplyExact(device.lastSync, 1000L)))
            } else {
                mContext.getString(R.string.account_device_never_synced)
            }
            text = mContext.getString(R.string.account_device_last_sync, timestamp)
            visibility = if (isCurrentDevice) View.GONE else View.VISIBLE
        }
        if (isCurrentDevice) {
            thisDevice.visibility = View.VISIBLE
            devId.setLeftDrawable(R.drawable.baseline_edit_rounded_24)
            devId.setLeftDrawableOnClickListener { mListener.onDeviceRename() }
        } else {
            thisDevice.visibility = View.GONE
            devId.setLeftDrawable(R.drawable.baseline_cancel_24)
            devId.setDrawableTint(R.color.icon_color)
            devId.setLeftDrawableOnClickListener { mListener.onDeviceRevocationAsked(device.id) }
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
}