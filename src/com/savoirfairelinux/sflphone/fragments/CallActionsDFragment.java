package com.savoirfairelinux.sflphone.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.loaders.ContactsLoader;
import com.savoirfairelinux.sflphone.model.CallContact;
import com.savoirfairelinux.sflphone.model.SipCall;

public class CallActionsDFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Bundle>{

    private AutoCompleteTextView mEditText;
    private AutoCompleteAdapter autoCompleteAdapter;
    SimpleCallListAdapter mAdapter;
    /**
     * Create a new instance of CallActionsDFragment
     */
    static CallActionsDFragment newInstance(int num) {
        CallActionsDFragment f = new CallActionsDFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pick a style based on the num.
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }
    
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {   
//        return new AlertDialog.Builder(getActivity())                
//                .setPositiveButton(android.R.string.ok,
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int whichButton) {                          
//
//                        }
//                    }
//                )
//                .setNegativeButton(android.R.string.no,
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int whichButton) {
//
//                        }
//                    }
//                )
//                .create();
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_action_calls, container);
        
        ArrayList<SipCall> calls = getArguments().getParcelableArrayList("calls");
        mAdapter = new SimpleCallListAdapter(getActivity(), calls);
        ListView list = (ListView) rootView.findViewById(R.id.concurrent_calls);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                
                Intent in = new Intent();
                in.putExtra("selected_call", mAdapter.getItem(pos));
                getTargetFragment().onActivityResult(getTargetRequestCode(), 0, in);
                dismiss();
            }
        });
        list.setEmptyView(rootView.findViewById(R.id.empty_view));
        
        mEditText = (AutoCompleteTextView) rootView.findViewById(R.id.external_number);
        mEditText.setAdapter(autoCompleteAdapter);
        getDialog().setTitle("Transfer");

        

        return rootView;
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
    

    private class AutoCompleteAdapter extends ArrayAdapter<Address> implements Filterable {
     
        private LayoutInflater mInflater;
        private Geocoder mGeocoder;
        private StringBuilder mSb = new StringBuilder();
     
        public AutoCompleteAdapter(final Context context) {
            super(context, -1);
            mInflater = LayoutInflater.from(context);
            mGeocoder = new Geocoder(context);
        }
     
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final TextView tv;
            if (convertView != null) {
                tv = (TextView) convertView;
            } else {
                tv = (TextView) mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
     
            return tv;
        }
     

     
        @Override
        public Filter getFilter() {
            Filter myFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    List<Address> addressList = null;
                    if (constraint != null) {
                        try {
                            addressList = mGeocoder.getFromLocationName((String) constraint, 5);
                        } catch (IOException e) {
                        }
                    }
                    if (addressList == null) {
                        addressList = new ArrayList<Address>();
                    }
     
                    final FilterResults filterResults = new FilterResults();
                    filterResults.values = addressList;
                    filterResults.count = addressList.size();
     
                    return filterResults;
                }
     
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(final CharSequence contraint, final FilterResults results) {
                    clear();
                    for (Address address : (List<Address>) results.values) {
                        add(address);
                    }
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
     
                @Override
                public CharSequence convertResultToString(final Object resultValue) {
                    return resultValue == null ? "" : ((Address) resultValue).getAddressLine(0);
                }
            };
            return myFilter;
        }
    }
    
    private class SimpleCallListAdapter extends BaseAdapter  {
        
        private LayoutInflater mInflater;
        ArrayList<SipCall> calls;
        
     
        public SimpleCallListAdapter(final Context context, ArrayList<SipCall> calls2) {
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
     
            tv.setText(calls.get(position).getContacts().get(0).getmDisplayName());
            return tv;
        }

        @Override
        public int getCount() {
            return calls.size();
        }

        @Override
        public SipCall getItem(int pos) {
            return calls.get(pos);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }
}
