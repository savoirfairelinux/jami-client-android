/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sflphone.R;
import org.sflphone.model.CallContact;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class StarredContactsAdapter extends BaseAdapter {

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    private ArrayList<CallContact> dataset;
    Context mContext;

//    private static final String TAG = ContactsAdapter.class.getSimpleName();

    public StarredContactsAdapter(Context context) {
        super();
        mContext = context;
        dataset = new ArrayList<CallContact>();
    }

    public void removeAll() {
        dataset.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<CallContact> arrayList) {
        dataset.addAll(arrayList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataset.size();
    }

    @Override
    public CallContact getItem(int index) {
        return dataset.get(index);
    }

    @Override
    public long getItemId(int index) {
        return dataset.get(index).getId();
    }

    @Override
    public View getView(int pos, View convView, ViewGroup parent) {

        View v = convView;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (v == null) {
            v = inflater.inflate(R.layout.item_contact_starred, null);
        }

        CallContact item = dataset.get(pos);

        ((TextView) v.findViewById(R.id.display_name)).setText(item.getmDisplayName());
        ImageView photo_view = (ImageView) v.findViewById(R.id.photo);

        infos_fetcher.execute(new ContactPictureTask(mContext, photo_view, item.getId()));

        return v;
    }
}