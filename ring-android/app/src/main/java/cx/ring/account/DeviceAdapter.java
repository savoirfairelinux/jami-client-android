/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import cx.ring.R;

public class DeviceAdapter extends BaseAdapter {
    private final Context mContext;
    private final ArrayList<Map.Entry<String, String>> mDevices = new ArrayList<>();

    public DeviceAdapter(Context c, Map<String, String> devices) {
        mContext = c;
        setData(devices);
    }

    public void setData(Map<String, String> devices) {
        mDevices.clear();
        if (devices != null && !devices.isEmpty()) {
            mDevices.ensureCapacity(devices.size());
            for (Map.Entry<String, String> e : devices.entrySet()) {
                mDevices.add(e);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false);
        }

        TextView devId = (TextView) view.findViewById(R.id.txt_device_id);
        devId.setText(mDevices.get(i).getKey());

        TextView devName = (TextView) view.findViewById(R.id.txt_device_label);
        devName.setText(mDevices.get(i).getValue());

        return view;
    }
}
