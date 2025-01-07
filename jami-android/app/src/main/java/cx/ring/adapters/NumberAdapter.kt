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
package cx.ring.adapters

import android.content.Context
import net.jami.model.Contact
import android.widget.BaseAdapter
import android.view.ViewGroup
import cx.ring.R
import android.widget.TextView
import android.provider.ContactsContract
import android.view.View
import android.widget.ImageView
import net.jami.model.Phone
import java.util.ArrayList

class NumberAdapter(private val mContext: Context, c: Contact?, fullCellForGetView: Boolean) :
    BaseAdapter() {
    private val mNumbers: ArrayList<Phone> = if (c?.phones != null) c.phones else ArrayList()
    private var mUseFullCellForGetView = fullCellForGetView

    override fun getCount(): Int = mNumbers.size

    override fun getItem(position: Int): Any = mNumbers[position]

    override fun getItemId(position: Int): Long = 0L

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View =
        getViewWithLongNumber(mUseFullCellForGetView, position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View =
        getViewWithLongNumber(true, position, convertView, parent)

    private fun getViewWithLongNumber(longView: Boolean, position: Int, convertView: View, parent: ViewGroup): View {
        val (number1, category, label) = mNumbers[position]
        val numberIcon = convertView.findViewById<ImageView>(R.id.number_icon)
        numberIcon.setImageResource(if (number1.isHexId) R.drawable.ic_jami_24 else R.drawable.baseline_dialer_sip_24)
        if (longView) {
            val numberTxt = convertView.findViewById<TextView>(R.id.number_txt)
            val numberLabelTxt = convertView.findViewById<TextView>(R.id.number_label_txt)
            numberTxt.text = number1.rawUriString
            numberLabelTxt.text = ContactsContract.CommonDataKinds.Phone.getTypeLabel(mContext.resources, category, label)
        }
        return convertView
    }
}