package com.savoirfairelinux.sflphone.adapters;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.SipCall;

/**
 * A CursorAdapter that creates and update call elements using corresponding contact infos. TODO: handle contact list separatly to allow showing
 * synchronized contacts on Call cards with multiple contacts etc.
 */
public class CallElementAdapter extends ArrayAdapter {
    private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
    private Context mContext;
    private final List mCallList;
    private static final String CURRENT_STATE_LABEL = "    CURRENT STATE: ";

    public CallElementAdapter(Context context, List callList) {
        super(context, R.layout.item_contact, callList);
        mContext = context;
        mCallList = callList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        CallElementView entryView = null;

        if (rowView == null) {
            // Get a new instance of the row layout view
            LayoutInflater inflater = LayoutInflater.from(mContext);
            rowView = inflater.inflate(R.layout.item_contact, null);

            // Hold the view objects in an object
            // so they don't need to be re-fetched
            entryView = new CallElementView();
            entryView.photo = (ImageView) rowView.findViewById(R.id.photo);
            entryView.displayName = (TextView) rowView.findViewById(R.id.display_name);
            entryView.phones = (TextView) rowView.findViewById(R.id.phones);
            entryView.state = (TextView) rowView.findViewById(R.id.callstate);

            // Cache the view obects in the tag
            // so they can be re-accessed later
            rowView.setTag(entryView);
        } else {
            entryView = (CallElementView) rowView.getTag();
        }

        // Transfer the stock data from the data object
        // to the view objects
        SipCall call = (SipCall) mCallList.get(position);
        call.setAssociatedRowView(rowView);
        entryView.displayName.setText(call.getDisplayName());
        entryView.phones.setText(call.getPhone());
        entryView.state.setText(CURRENT_STATE_LABEL + call.getCallStateString());

        return rowView;
    }

    /*********************
     * ViewHolder Pattern
     *********************/
    public class CallElementView {
        protected ImageView photo;
        protected TextView displayName;
        protected TextView phones;
        public TextView state;
    }
    
    
}
