/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import cx.ring.R;

public class DeviceAdapter extends BaseAdapter {
    private final Context mContext;
    private final ArrayList<Map.Entry<String, String>> mDevices = new ArrayList<>();

    private String mCurrentDeviceId;
    private DeviceRevocationListener mListener;

    public DeviceAdapter(Context c, Map<String, String> devices, String currentDeviceId,
                         DeviceRevocationListener listener) {
        mContext = c;
        setData(devices, currentDeviceId);
        mListener = listener;
    }

    public void setData(Map<String, String> devices, String currentDeviceId) {
        mDevices.clear();
        mCurrentDeviceId = currentDeviceId;
        if (devices != null && !devices.isEmpty()) {
            mDevices.ensureCapacity(devices.size());
            mDevices.addAll(devices.entrySet());
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
    public View getView(final int i, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false);
        }
        boolean isCurrentDevice = mDevices.get(i).getKey().contentEquals(mCurrentDeviceId);

        TextView devId = view.findViewById(R.id.txt_device_id);
        devId.setText(mDevices.get(i).getKey());

        TextView devName = view.findViewById(R.id.txt_device_label);
        devName.setText(mDevices.get(i).getValue());
        ImageButton revokeButton = view.findViewById(R.id.revoke_button);
        ImageButton editButton = view.findViewById(R.id.rename_button);
        revokeButton.setVisibility(isCurrentDevice ? View.GONE : View.VISIBLE);
        editButton.setVisibility(isCurrentDevice ? View.VISIBLE : View.GONE);
        TextView thisDeviceText = view.findViewById(R.id.txt_device_thisflag);
        thisDeviceText.setVisibility(isCurrentDevice ? View.VISIBLE : View.GONE);
        if (isCurrentDevice) {
            editButton.setOnClickListener(view1 -> {
                if (mListener != null) {
                    mListener.onDeviceRename();
                }
            });
        }

        revokeButton.setOnClickListener(view12 -> {
            if (mListener != null) {
                mListener.onDeviceRevocationAsked(mDevices.get(i).getKey());
            }
        });

        return view;
    }

    public interface DeviceRevocationListener {
        void onDeviceRevocationAsked(String deviceId);

        void onDeviceRename();
    }
}
