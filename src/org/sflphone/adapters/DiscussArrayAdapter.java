/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.adapters;

import java.util.ArrayList;
import java.util.List;

import org.sflphone.R;
import org.sflphone.model.SipMessage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DiscussArrayAdapter extends BaseAdapter {

    private TextView countryName;
    private List<SipMessage> messages = new ArrayList<SipMessage>();
    private LinearLayout wrapper;
    private Context mContext;

    public DiscussArrayAdapter(Context context, Bundle args) {
        mContext = context;
        
        if(args == null)
            messages = new ArrayList<SipMessage>();
        else
            messages = args.getParcelableArrayList("messages");
        
    }

    public void add(SipMessage object) {
        messages.add(object);
        notifyDataSetChanged();
    }

    public int getCount() {
        return this.messages.size();
    }

    public SipMessage getItem(int index) {
        return this.messages.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            row = inflater.inflate(R.layout.item_message, parent, false);
        }

        wrapper = (LinearLayout) row.findViewById(R.id.wrapper);

        SipMessage coment = getItem(position);

        countryName = (TextView) row.findViewById(R.id.comment);

        countryName.setText(coment.comment);

        countryName.setBackgroundResource(coment.left ? R.drawable.bubble_left_selector : R.drawable.bubble_right_selector);
        wrapper.setGravity(coment.left ? Gravity.LEFT : Gravity.RIGHT);

        return row;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

}