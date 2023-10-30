/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import cx.ring.databinding.ItemSettingBinding

class SettingsAdapter(context: Context, resource: Int, objects: List<SettingItem>) :
    ArrayAdapter<SettingItem>(context, resource, objects) {
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var view = view
        val binding: ItemSettingBinding
        if (view == null) {
            binding = ItemSettingBinding.inflate((context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater))
            view = binding.root
            view.setTag(binding)
        } else
            binding = view.tag as ItemSettingBinding
        val item = getItem(position)
        binding.title.text = context.getString(item!!.titleRes)
        binding.icon.setImageResource(item.imageId)
        return view
    }
}
