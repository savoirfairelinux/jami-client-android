/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;

public class NumberAdapter extends BaseAdapter {
    final private Context mContext;
    final private ArrayList<CallContact.Phone> mNumbers;
    private boolean mUseFullCellForGetView = false;

    public NumberAdapter(Context context, CallContact c, boolean useFullCellForGetView) {
        mContext = context;
        mNumbers = (c != null && c.getPhones() != null) ?
                c.getPhones() : new ArrayList<CallContact.Phone>();
        mUseFullCellForGetView = useFullCellForGetView;
    }

    @Override
    public int getCount() {
        return mNumbers.size();
    }

    @Override
    public Object getItem(int position) {
        return mNumbers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.getViewWithLongNumber(this.mUseFullCellForGetView, position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return this.getViewWithLongNumber(true, position, convertView, parent);
    }

    private View getViewWithLongNumber(boolean longView, int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            if (longView) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_number, parent,
                        false);
            } else {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_number_selected,
                        parent, false);
            }
        }

        CallContact.Phone number = mNumbers.get(position);
        ImageView numberIcon = (ImageView) convertView.findViewById(R.id.number_icon);
        numberIcon.setImageResource(number.getNumber().isRingId() ?
                R.drawable.ring_logo_24dp : R.drawable.ic_dialer_sip_black_24dp);

        if (longView) {
            TextView numberTxt = (TextView) convertView.findViewById(R.id.number_txt);
            TextView numberLabelTxt = (TextView) convertView.findViewById(R.id.number_label_txt);

            numberTxt.setText(number.getNumber().getRawUriString());
            numberLabelTxt.setText(number.getTypeString(mContext.getResources()));
        }

        return convertView;
    }
}
