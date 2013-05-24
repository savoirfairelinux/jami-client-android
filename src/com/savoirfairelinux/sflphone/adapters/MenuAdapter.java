package com.savoirfairelinux.sflphone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class MenuAdapter extends BaseAdapter{
    
    private static final String TAG = MenuAdapter.class.getSimpleName();
    private Context mContext;
    String categories[];

    public MenuAdapter(Context c, String[] cat) {
        mContext = c;
        categories = cat;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = convertView;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (v == null) {
            v = inflater.inflate(R.layout.item_menu, null);
        }

        ((TextView) v.findViewById(R.id.menu_title_categorie)).setText(categories[pos]);

        return v;
    }

    @Override
    public int getCount() {
        return categories.length;
    }

    @Override
    public String getItem(int pos) {
        return categories[pos];
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }
 

}
