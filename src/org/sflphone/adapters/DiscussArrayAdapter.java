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
            LayoutInflater inflater = (LayoutInflater) LayoutInflater.from(mContext);
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

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

}