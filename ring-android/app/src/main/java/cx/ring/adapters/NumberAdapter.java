/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.model.Phone;

public class NumberAdapter extends BaseAdapter {
    final private Context mContext;
    final private ArrayList<Phone> mNumbers;
    private boolean mUseFullCellForGetView = false;

    public NumberAdapter(Context context, CallContact c, boolean useFullCellForGetView) {
        mContext = context;
        mNumbers = (c != null && c.getPhones() != null) ?
                c.getPhones() : new ArrayList<>();
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

        Phone number = mNumbers.get(position);
        ImageView numberIcon = convertView.findViewById(R.id.number_icon);
        numberIcon.setImageResource(number.getNumber().isRingId() ?
                R.drawable.ic_logo_24 : R.drawable.baseline_dialer_sip_24);

        if (longView) {
            TextView numberTxt = convertView.findViewById(R.id.number_txt);
            TextView numberLabelTxt = convertView.findViewById(R.id.number_label_txt);

            numberTxt.setText(number.getNumber().getRawUriString());
            numberLabelTxt.setText(ContactsContract.CommonDataKinds.Phone.getTypeLabel(mContext.getResources(), number.getCategory(), number.getLabel()));
        }

        return convertView;
    }
}
