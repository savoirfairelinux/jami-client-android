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

package cx.ring.adapters;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cx.ring.R;
import cx.ring.fragments.ContactListFragment;
import cx.ring.model.CallContact;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

public class ContactsAdapter extends BaseAdapter implements StickyListHeadersAdapter, SectionIndexer {

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    Context mContext;

    private ArrayList<CallContact> mContacts;
    private int[] mSectionIndices;
    private Character[] mSectionLetters;
    WeakReference<ContactListFragment> parent;
    private LayoutInflater mInflater;

    // private static final String TAG = ContactsAdapter.class.getSimpleName();

    public ContactsAdapter(ContactListFragment contactListFragment) {
        super();
        mContext = contactListFragment.getActivity();
        mInflater = LayoutInflater.from(mContext);
        parent = new WeakReference<>(contactListFragment);
        mContacts = new ArrayList<>();
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
    }

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CONTACT = 1;

    private int[] getSectionIndices() {
        ArrayList<Integer> sectionIndices = new ArrayList<>();
        if (mContacts.isEmpty())
            return new int[0];
        char lastFirstChar = mContacts.get(0).getDisplayName().charAt(0);
        sectionIndices.add(0);
        for (int i = 1; i < mContacts.size(); i++) {
            if (mContacts.get(i).getDisplayName().charAt(0) != lastFirstChar) {
                lastFirstChar = mContacts.get(i).getDisplayName().charAt(0);
                sectionIndices.add(i);
            }
        }
        int[] sections = new int[sectionIndices.size()];
        for (int i = 0; i < sectionIndices.size(); i++) {
            sections[i] = sectionIndices.get(i);
        }
        return sections;
    }

    private Character[] getSectionLetters() {
        Character[] letters = new Character[mSectionIndices.length];
        for (int i = 0; i < mSectionIndices.length; i++) {
            letters[i] = mContacts.get(mSectionIndices[i]).getDisplayName().charAt(0);
        }
        return letters;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup root) {
        ContactView entryView;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_contact, null);

            entryView = new ContactView();
            entryView.quick_starred = (ImageButton) convertView.findViewById(R.id.quick_starred);
            entryView.quick_edit = (ImageButton) convertView.findViewById(R.id.quick_edit);
            entryView.quick_discard = (ImageButton) convertView.findViewById(R.id.quick_discard);
            entryView.quick_call = (ImageButton) convertView.findViewById(R.id.quick_call);
            entryView.quick_msg = (ImageButton) convertView.findViewById(R.id.quick_message);
            entryView.photo = (ImageView) convertView.findViewById(R.id.photo);
            entryView.display_name = (TextView) convertView.findViewById(R.id.display_name);
            convertView.setTag(entryView);
        } else {
            entryView = (ContactView) convertView.getTag();
        }

        final CallContact item = mContacts.get(position);

        entryView.display_name.setText(item.getDisplayName());

        if (item.hasPhoto()) {
            entryView.photo.setImageBitmap(item.getPhoto());
        } else {
            infos_fetcher.execute(new ContactPictureTask(mContext, entryView.photo, item));
        }

        entryView.quick_call.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                parent.get().mCallbacks.onCallContact(item);

            }
        });

        entryView.quick_msg.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                parent.get().mCallbacks.onTextContact(item);
            }
        });

        entryView.quick_starred.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        entryView.quick_edit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                parent.get().mCallbacks.onEditContact(item);

            }
        });

        entryView.quick_discard.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Coming soon", Toast.LENGTH_SHORT).show();

            }
        });

        entryView.quick_edit.setClickable(false);
        entryView.quick_discard.setClickable(false);
        entryView.quick_starred.setClickable(false);

        return convertView;
    }

    /*********************
     * ViewHolder Pattern
     *********************/
    public class ContactView {
        ImageButton quick_starred, quick_edit, quick_discard, quick_call, quick_msg;
        ImageView photo;
        TextView display_name;
    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;

        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.header_letter);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        // set header text as first char in name
        char headerChar = mContacts.get(position).getDisplayName().subSequence(0, 1).charAt(0);

        holder.text.setText("" + headerChar);

        return convertView;

    }

    class HeaderViewHolder {
        TextView text;
    }

    @Override
    public long getHeaderId(int position) {
        // return the first character of the name as ID because this is what
        // headers are based upon
        return mContacts.get(position).getDisplayName().subSequence(0, 1).charAt(0);
    }

    @Override
    public int getPositionForSection(int section) {
        if (section >= mSectionIndices.length) {
            section = mSectionIndices.length - 1;
        } else if (section < 0) {
            section = 0;
        }
        return mSectionIndices[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < mSectionIndices.length; i++) {
            if (position < mSectionIndices[i]) {
                return i - 1;
            }
        }
        return mSectionIndices.length - 1;
    }

    @Override
    public Object[] getSections() {
        return mSectionLetters;
    }

    @Override
    public CallContact getItem(int position) {
        return mContacts.get(position);
    }

    public void clear() {
        mContacts = new ArrayList<>();
        mSectionIndices = new int[0];
        mSectionLetters = new Character[0];
        notifyDataSetChanged();
    }

    public void restore() {
        mContacts = new ArrayList<>();
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<CallContact> tmp) {
        mContacts.addAll(tmp);
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
        notifyDataSetChanged();
    }

    public void setData(ArrayList<CallContact> contacts) {
        mContacts = contacts;
        notifyDataSetChanged();
    }

}
