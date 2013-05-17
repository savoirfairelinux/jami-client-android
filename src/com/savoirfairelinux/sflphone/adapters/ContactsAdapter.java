package com.savoirfairelinux.sflphone.adapters;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.CallContact;

public class ContactsAdapter extends BaseAdapter {

    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    private ArrayList<CallContact> dataset;
    Context mContext;

    private static final String TAG = ContactsAdapter.class.getSimpleName();

    public ContactsAdapter(Context context) {
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
            v = inflater.inflate(R.layout.item_contact, null);
        }

        CallContact item = dataset.get(pos);

        ((TextView) v.findViewById(R.id.display_name)).setText(item.getmDisplayName());
        ImageView photo_view = (ImageView) v.findViewById(R.id.photo);

        infos_fetcher.execute(new ContactPictureLoader(mContext, photo_view, item.getId()));

        return v;
    }

}
