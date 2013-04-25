package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.CallElementAdapter.CallElementView;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ServiceConstants;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryAdapter extends BaseAdapter{

    Context mContext;
    ArrayList<HashMap<String, String>> dataset;


    public HistoryAdapter(Activity activity, ArrayList<HashMap<String, String>> entries) {
        mContext = activity;
        dataset = entries;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup arg2) {
        View rowView = convertView;
        HistoryView entryView = null;

        if (rowView == null) {
            // Get a new instance of the row layout view
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_contact, null);

            // Hold the view objects in an object
            // so they don't need to be re-fetched
            entryView = new HistoryView();
            entryView.displayName = (TextView) rowView.findViewById(R.id.display_name);

            rowView.setTag(entryView);
        } else {
            entryView = (HistoryView) rowView.getTag();
        }

        // Transfer the stock data from the data object
        // to the view objects
        
//        SipCall call = (SipCall) mCallList.values().toArray()[position];
        entryView.displayName.setText(dataset.get(pos).get(ServiceConstants.HISTORY_ACCOUNT_ID_KEY));
//        entryView.phones.setText(call.getPhone());
//        entryView.state.setText(CURRENT_STATE_LABEL + call.getCallStateString());

        return rowView;

    }
    
    /*********************
     * ViewHolder Pattern
     *********************/
    public class HistoryView {
        protected ImageView photo;
        protected TextView displayName;
        protected TextView phones;
        public TextView state;
    }
    
    @Override
    public int getCount() {
       
        return dataset.size();
    }

    @Override
    public HashMap<String, String> getItem(int pos) {
        return dataset.get(pos);
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    public void clear() {
       dataset.clear();
        
    }

    public void addAll(ArrayList<HashMap<String, String>> history) {
        dataset.addAll(history);
        
    }

}
