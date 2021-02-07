/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.account;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import cx.ring.databinding.ItemSettingBinding;

public class SettingsAdapter extends ArrayAdapter<SettingItem> {
    public SettingsAdapter(@NonNull Context context, int resource, @NonNull List<SettingItem> objects) {
        super(context, resource, objects);
    }

    public View getView(int position, @Nullable View view, ViewGroup parent) {
        ItemSettingBinding binding;
        if (view == null) {
            binding = ItemSettingBinding.inflate((LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE));
            view = binding.getRoot();
            view.setTag(binding);
        } else
            binding = (ItemSettingBinding) view.getTag();

        SettingItem item = getItem(position);
        binding.title.setText(getContext().getString(item.getTitleRes()));
        binding.icon.setImageResource(item.getImageId());
        return view;
    }
}
