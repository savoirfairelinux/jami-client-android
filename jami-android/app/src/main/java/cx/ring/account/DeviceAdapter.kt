/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import java.util.ArrayList

class DeviceAdapter(
    private val mContext: Context, devices: Map<String, String>?,
    currentDeviceId: String?,
    private val mListener: DeviceRevocationListener
) : BaseAdapter() {
    private val mDevices = ArrayList<Map.Entry<String, String>>()
    private var mCurrentDeviceId: String? = null

    fun setData(devices: Map<String, String>?, currentDeviceId: String?) {
        mDevices.clear()
        mCurrentDeviceId = currentDeviceId
        if (devices != null && devices.isNotEmpty()) {
            mDevices.ensureCapacity(devices.size)
            mDevices.addAll(devices.entries)
        }
        mDevices.removeAll { d ->d.key == mCurrentDeviceId }
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mDevices.size
    }

    override fun getItem(i: Int): Any {
        return mDevices[i]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false)
        val isCurrentDevice = mDevices[i].key.contentEquals(mCurrentDeviceId)
        val devId: TwoButtonEditText = view.findViewById(R.id.txt_device_id)
        val thisDevice = view.findViewById<TextView>(R.id.txt_device_thisflag)
        devId.text = mDevices[i].value
        var hint = mDevices[i].key
        hint = hint.substring(0, (hint.length * 0.66).toInt())
        devId.setHint(hint)
        if (isCurrentDevice) {
            thisDevice.visibility = View.VISIBLE
            devId.setLeftDrawable(R.drawable.baseline_edit_twoton_24dp)
            devId.setLeftDrawableOnClickListener { mListener.onDeviceRename() }
        } else {
            thisDevice.visibility = View.GONE
            devId.setLeftDrawable(R.drawable.baseline_cancel_24)
            devId.setLeftDrawableOnClickListener { mListener.onDeviceRevocationAsked(mDevices[i].key) }
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