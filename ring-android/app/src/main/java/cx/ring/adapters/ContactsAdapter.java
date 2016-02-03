/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.adapters;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import cx.ring.R;
import cx.ring.fragments.ContactListFragment;
import cx.ring.model.CallContact;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class ContactsAdapter extends BaseAdapter implements StickyListHeadersAdapter, SectionIndexer {
    private final ExecutorService infos_fetcher;
    private final Context mContext;
    private ArrayList<CallContact> mContacts;
    private int[] mSectionIndices;
    private Character[] mSectionLetters;
    WeakReference<ContactListFragment.Callbacks> parent;
    private LayoutInflater mInflater;

    final private LruCache<Long, Bitmap> mMemoryCache;

    private static final String TAG = ContactsAdapter.class.getSimpleName();

    public ContactsAdapter(Context c, ContactListFragment.Callbacks cb, LruCache<Long, Bitmap> cache, ExecutorService pool) {
        super();
        mContext = c;
        mInflater = LayoutInflater.from(mContext);
        parent = new WeakReference<>(cb);
        mContacts = new ArrayList<>();
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
        mMemoryCache = cache;
        infos_fetcher = pool;
    }

    private int[] getSectionIndices() {
        ArrayList<Integer> sectionIndices = new ArrayList<>(32);
        if (mContacts.isEmpty())
            return new int[0];
        char lastFirstChar = Character.toUpperCase(mContacts.get(0).getDisplayName().charAt(0));
        sectionIndices.add(0);
        for (int i = 1; i < mContacts.size(); i++) {
            char c = Character.toUpperCase(mContacts.get(i).getDisplayName().charAt(0));
            if (c != lastFirstChar) {
                lastFirstChar = c;
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
        for (int i = 0; i < mSectionIndices.length; i++)
            letters[i] = Character.toUpperCase(mContacts.get(mSectionIndices[i]).getDisplayName().charAt(0));
        return letters;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup root) {
        ContactView entryView;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_contact, root, false);

            entryView = new ContactView();
            /*entryView.quick_starred = (ImageButton) convertView.findViewById(R.id.quick_starred);
            entryView.quick_edit = (ImageButton) convertView.findViewById(R.id.quick_edit);
            entryView.quick_discard = (ImageButton) convertView.findViewById(R.id.quick_discard);
            entryView.quick_msg = (ImageButton) convertView.findViewById(R.id.quick_message);*/
            entryView.photo = (ImageView) convertView.findViewById(R.id.photo);
            entryView.display_name = (TextView) convertView.findViewById(R.id.display_name);
            entryView.quick_call = (ImageButton) convertView.findViewById(R.id.quick_call);
            entryView.position = -1;
            convertView.setTag(entryView);
        } else {
            entryView = (ContactView) convertView.getTag();
        }

        final CallContact item = mContacts.get(position);

        if (entryView.contact != null && entryView.contact.get() != null && item.getId() == entryView.contact.get().getId())
            return convertView;

        entryView.display_name.setText(item.getDisplayName());
        entryView.contact = new WeakReference<>(item);
        entryView.position = position;
        entryView.quick_call.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.get().onCallContact(item);
            }
        });
        final Long cid = item.getId();
        final Long pid = item.getPhotoId();
        Bitmap bmp = item.getPhoto();
        if (bmp == null) {
            bmp = mMemoryCache.get(pid);
            if (bmp != null) item.setPhoto(bmp);
        }

        if (bmp != null) {
            entryView.photo.setImageBitmap(bmp);
        } else {
            entryView.photo.setImageBitmap(null);
            final WeakReference<ContactView> wh = new WeakReference<>(entryView);
            infos_fetcher.execute(new ContactPictureTask(mContext, entryView.photo, item, new ContactPictureTask.PictureLoadedCallback() {
                @Override
                public void onPictureLoaded(final Bitmap bmp) {
                    mMemoryCache.put(pid, bmp);
                    final ContactView fh = wh.get();
                    if (fh == null || fh.photo.getParent() == null)
                        return;
                    if (fh.position == position)
                        fh.photo.post(new Runnable() {
                            @Override
                            public void run() {
                                final CallContact c = fh.contact.get();
                                if (c.getId() == cid) {
                                    c.setPhoto(bmp);
                                    fh.photo.setImageBitmap(bmp);
                                    fh.photo.startAnimation(AnimationUtils.loadAnimation(fh.photo.getContext(), R.anim.contact_fadein));
                                }
                            }
                        });
                }
            }));
        }

        return convertView;
    }

    /*********************
     * ViewHolder Pattern
     *********************/
    public class ContactView {
        ImageButton /*quick_starred, quick_edit, quick_discard, */quick_call, quick_msg;
        ImageView photo;
        TextView display_name;
        WeakReference<CallContact> contact = new WeakReference<>(null);
        int position;
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
        holder.text.setText(String.valueOf(Character.toUpperCase(mContacts.get(position).getDisplayName().charAt(0))));

        return convertView;
    }

    class HeaderViewHolder {
        TextView text;
    }

    @Override
    public long getHeaderId(int position) {
        // return the first character of the name as ID because this is what
        // headers are based upon
        return Character.toUpperCase(mContacts.get(position).getDisplayName().charAt(0));
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
        //notifyDataSetChanged();
    }

    public void setData(ArrayList<CallContact> contacts) {
        mContacts = contacts;
        mSectionIndices = getSectionIndices();
        mSectionLetters = getSectionLetters();
        notifyDataSetChanged();
    }

}
