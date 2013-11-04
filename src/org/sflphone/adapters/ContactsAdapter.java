package org.sflphone.adapters;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sflphone.R;
import org.sflphone.fragments.ContactListFragment;
import org.sflphone.model.CallContact;

import android.content.Context;
import android.util.SparseArray;
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

public class ContactsAdapter extends BaseAdapter implements SectionIndexer {

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    Context mContext;

    HashMap<String, Integer> alphaIndexer;
    String[] sections;
    WeakReference<ContactListFragment> parent;

    // private static final String TAG = ContactsAdapter.class.getSimpleName();

    public ContactsAdapter(ContactListFragment contactListFragment) {
        super();
        mContext = contactListFragment.getActivity();
        alphaIndexer = new HashMap<String, Integer>();
        headers = new HeadersHolder(new ArrayList<CallContact>());
        parent = new WeakReference<ContactListFragment>(contactListFragment);
    }

    HeadersHolder headers;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TRACK = 1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        // Log.i(TAG, "positon" + position + " type " + type);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        switch (type) {
        case TYPE_TRACK:
            return getViewContact(position, inflater, convertView);
        case TYPE_HEADER:
            return getViewHeader(position, inflater, convertView);
        }
        return null;
    }

    private View getViewHeader(int position, LayoutInflater inflater, View convertView) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.header, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.header_letter);
        name.setText(headers.getSection(position));
        return convertView;
    }

    private View getViewContact(int position, LayoutInflater inflater, View convertView) {
        ContactView entryView;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_contact, null);

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
        

        final CallContact item = headers.getCallContact(position);

        entryView.display_name.setText(item.getmDisplayName());

        if(item.hasPhoto()){
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
        return headers.size() + headers.getSections().size();
    }

    @Override
    public int getItemViewType(int pos) {
        return (headers.contains(pos) ? TYPE_HEADER : TYPE_TRACK);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void removeAll() {
        headers.clear();
        notifyDataSetChanged();
    }

    public void add(CallContact tr) {
        headers.add(tr);
        headers.buildIndex();
    }

    public void addAll(ArrayList<CallContact> tr) {
        headers = new HeadersHolder(tr);
        notifyDataSetChanged();
    }

    @Override
    public int getPositionForSection(int section) {
        return headers.getPositionFor(section);
    }

    @Override
    public int getSectionForPosition(int position) {
        return headers.getSectionFor(position);
    }

    @Override
    public Object[] getSections() {
        return headers.getSectionsArray();
    }

    public int getRealPosition(int pos) {
        return headers.getTrackPosition(pos);
    }

    @Override
    public CallContact getItem(int index) {
        return headers.getCallContact(index);
    }

    public class HeadersHolder {

        public static final String TAG = "HeadersHolder";
        HashMap<String, Integer> alphaIndexer;
        ArrayList<CallContact> contacts;

        String[] sectionsArray;

        public String[] getSectionsArray() {
            return sectionsArray;
        }

        int headersCount;
        private SparseArray<Item> items = new SparseArray<Item>();

        public SparseArray<Item> getItems() {
            return items;
        }

        public void setItems(SparseArray<Item> items) {
            this.items = items;
        }

        public SparseArray<Section> getSections() {
            return sections;
        }

        public void setSections(SparseArray<Section> sections) {
            this.sections = sections;
        }

        private SparseArray<Section> sections = new SparseArray<Section>();

        public HeadersHolder(ArrayList<CallContact> a) {
            alphaIndexer = new HashMap<String, Integer>();
            contacts = a;
            headersCount = 0;
            buildIndex();
        }

        // public int getHeadersCount() {
        // return headersCount;
        // }

        private class Item {
            public Item(int rpos, int headersCount2, CallContact track) {
                // Log.i(TAG, "Creating Item");

                sectionNumber = headersCount2;
                realPos = rpos;
                tr = track;

            }

            public int realPos;
            public int sectionNumber;
            public CallContact tr;
        }

        private class Section {
            // public int startPosition;
            public int number;
            public String header;

            public Section(int i, int headersCount, String str) {
                // Log.i(TAG, "Creating section");

                // startPosition = i + headersCount;
                number = headersCount;
                header = str;

            }

            @Override
            public String toString() {
                return header;
            }
        }

        public void buildIndex() {

            for (int x = 0; x < contacts.size(); x++) {
                String s = contacts.get(x).getmDisplayName();
                String ch = s.substring(0, 1);
                ch = ch.toUpperCase(Locale.CANADA);
                if (!alphaIndexer.containsKey(ch)) {
                    sections.put(x + headersCount, new Section(x, headersCount, ch));
                    headersCount++;
                }
                Integer result = alphaIndexer.put(ch, x + headersCount);
                items.put(x + headersCount, new Item(x, headersCount, contacts.get(x)));
                if (result == null) {

                }
            }

            Set<String> sect = alphaIndexer.keySet();

            // create a list from the set to sort
            ArrayList<String> sectionList = new ArrayList<String>(sect);
            Collections.sort(sectionList);
            sectionsArray = new String[sectionList.size()];
            sectionList.toArray(sectionsArray);

        }

        public HashMap<String, Integer> getAlphaIndexer() {
            return alphaIndexer;
        }

        public int getPositionFor(int section) {
            if (section == sectionsArray.length)
                return sectionsArray.length;
            return alphaIndexer.get(sectionsArray[section]);
        }

        public int getSectionFor(int position) {
            return (null != items.get(position)) ? items.get(position).sectionNumber : sections.get(position).number;
        }

        public boolean contains(int pos) {
            if (sections.get(pos) != null) {
                return true;
            }
            return false;
        }

        public CallContact getCallContact(int position) {

            if (items.get(position) == null)
                return null;

            return items.get(position).tr;

        }

        public int size() {
            return contacts.size();
        }

        public void clear() {
            contacts.clear();

        }

        public void add(CallContact tr) {
            contacts.add(tr);

        }

        public void addAll(ArrayList<CallContact> tr) {
            contacts.clear();
            contacts.addAll(tr);

        }

        public ArrayList<CallContact> getTracks() {
            return contacts;
        }

        public int getTrackPosition(int pos) {
            if (sections.get(pos) != null) {
                return items.get(pos + 1).realPos;
            }
            return items.get(pos).realPos;
        }

        public CharSequence getSection(int position) {
            return sections.get(position).header;
        }

    }

}
