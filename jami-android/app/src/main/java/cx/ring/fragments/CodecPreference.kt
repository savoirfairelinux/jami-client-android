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
package cx.ring.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import net.jami.model.Codec

internal class CodecPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
    Preference(context, attrs, defStyleAttr) {
    private val listAdapter: CodecAdapter = CodecAdapter(context)

    private fun setListViewHeight(listView: ListView, llMain: LinearLayout) {
        val listAdapter = listView.adapter ?: return
        var totalHeight = 0
        var firstHeight: Int
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST)
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            firstHeight = listItem.measuredHeight
            totalHeight += firstHeight
        }
        val params = llMain.layoutParams as RecyclerView.LayoutParams
        params.height = totalHeight + listView.dividerHeight * listAdapter.count
        llMain.layoutParams = params
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val mCodecList = holder.findViewById(R.id.dndlistview) as ListView
        mCodecList.isFocusable = false
        if (mCodecList.adapter !== listAdapter)
            mCodecList.adapter = listAdapter
        mCodecList.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos: Int, _ ->
            listAdapter.getItem(pos).toggleState()
            listAdapter.notifyDataSetChanged()
            callChangeListener(activeCodecList)
        }
        setListViewHeight(mCodecList, mCodecList.parent as LinearLayout)
    }

    val activeCodecList: ArrayList<Long>
        get() {
            val results = ArrayList<Long>()
            for (i in 0 until listAdapter.count) {
                if (listAdapter.getItem(i).isEnabled) {
                    results.add(listAdapter.getItem(i).payload)
                }
            }
            return results
        }

    fun setCodecs(codecs: ArrayList<Codec>) {
        listAdapter.setDataset(codecs)
        notifyChanged()
        notifyHierarchyChanged()
    }

    fun refresh() {
        listAdapter.notifyDataSetChanged()
    }

    private class CodecAdapter constructor(private val mContext: Context) : BaseAdapter() {
        private val items = ArrayList<Codec>()
        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Codec = items[position]

        override fun getItemId(position: Int): Long = getItem(position).payload

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val entryView: CodecView
            var rowView = convertView
            if (rowView == null) {
                rowView = LayoutInflater.from(mContext).inflate(R.layout.item_codec, parent, false)
                entryView = CodecView(
                    rowView.findViewById(R.id.codec_name),
                    rowView.findViewById(R.id.codec_samplerate),
                    rowView.findViewById(R.id.codec_checked)
                )
                rowView.tag = entryView
            } else {
                entryView = rowView.tag as CodecView
            }
            val codec = items[pos]
            entryView.name.text = codec.name
            entryView.samplerate.visibility = if (codec.isSpeex) View.VISIBLE else View.GONE
            entryView.samplerate.text = codec.sampleRate
            entryView.enabled.isChecked = codec.isEnabled
            return rowView!!
        }

        fun setDataset(codecs: ArrayList<Codec>) {
            items.clear()
            items.addAll(codecs)
            notifyDataSetChanged()
        }

        class CodecView (
            val name: TextView,
            val samplerate: TextView,
            val enabled: CheckBox
        )
    }

    init {
        widgetLayoutResource = R.layout.frag_audio_mgmt
    }
}