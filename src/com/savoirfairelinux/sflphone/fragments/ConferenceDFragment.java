package com.savoirfairelinux.sflphone.fragments;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.loaders.ContactsLoader;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;

public class ConferenceDFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Bundle> {


    SimpleCallListAdapter mAdapter;

    /**
     * Create a new instance of CallActionsDFragment
     */
    public static ConferenceDFragment newInstance() {
        ConferenceDFragment f = new ConferenceDFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pick a style based on the num.
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_conference, null);

        ArrayList<Conference> calls = getArguments().getParcelableArrayList("calls");
        final Conference call_selected = getArguments().getParcelable("call_selected");

        mAdapter = new SimpleCallListAdapter(getActivity(), calls);
        ListView list = (ListView) rootView.findViewById(R.id.concurrent_calls);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {

                Intent in = new Intent();
                
                in.putExtra("call1", call_selected);
                in.putExtra("call2", mAdapter.getItem(pos));
                getTargetFragment().onActivityResult(getTargetRequestCode(), 0, in);
                dismiss();
            }
        });
        list.setEmptyView(rootView.findViewById(R.id.empty_view));

        

        final AlertDialog a = new AlertDialog.Builder(getActivity()).setView(rootView).setTitle("Transfer " + call_selected.getParticipants().get(0).getContact())
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dismiss();
                    }
                }).create();

        return a;
    }

    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle args) {
        Uri baseUri;

        if (args != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(args.getString("filter")));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }
        ContactsLoader l = new ContactsLoader(getActivity(), baseUri);
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {

        ArrayList<CallContact> tmp = data.getParcelableArrayList("Contacts");

    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {
        // Thi is called when the last Cursor provided to onLoadFinished
        // mListAdapter.swapCursor(null);
    }

    

    private class SimpleCallListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        ArrayList<Conference> calls;

        public SimpleCallListAdapter(final Context context, ArrayList<Conference> calls2) {
            super();
            mInflater = LayoutInflater.from(context);
            calls = calls2;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final TextView tv;
            if (convertView != null) {
                tv = (TextView) convertView;
            } else {
                tv = (TextView) mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }

            if(calls.get(position).getParticipants().size() == 1){
                tv.setText(calls.get(position).getParticipants().get(0).getContact().getmDisplayName());
            } else {
                tv.setText("Conference with "+ calls.get(position).getParticipants().size() + " participants");
            }
            
            return tv;
        }

        @Override
        public int getCount() {
            return calls.size();
        }

        @Override
        public Conference getItem(int pos) {
            return calls.get(pos);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }

}
