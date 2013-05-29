package com.savoirfairelinux.sflphone.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.SipCall;

public class CallListAdapter extends BaseAdapter {

    private ArrayList<SipCall> calls;
    private Context mContext;
    


    public CallListAdapter(Context c, ArrayList<SipCall> array) {
        mContext = c;
        calls = array;
    }



    @Override
    public SipCall getItem(int position) {
        return calls.get(position);
    }



    @Override
    public int getCount() {
        return calls.size();
    }



    @Override
    public long getItemId(int position) {
        return Long.parseLong(calls.get(position).getCallId());
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_calllist, null);
        } 
        
        
        SipCall tmp = calls.get(position);
        ((TextView)rowView.findViewById(R.id.call_title)).setText(tmp.getContacts().get(0).getmDisplayName());
        ((TextView)rowView.findViewById(R.id.call_status)).setText(""+tmp.getCallStateString());
        
        



        return rowView;
    }



    public void update(HashMap<String, SipCall> list) {
        calls.clear();
        calls.addAll(list.values());
        notifyDataSetChanged();
        
    }

    

}
