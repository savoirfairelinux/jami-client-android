/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package cx.ring.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.DialogFragment;
import cx.ring.R;
import cx.ring.loaders.ContactsLoader;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
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
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TransferDFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<ContactsLoader.Result> {
    public static final int RESULT_TRANSFER_CONF = Activity.RESULT_FIRST_USER + 1;
    public static final int RESULT_TRANSFER_NUMBER = Activity.RESULT_FIRST_USER + 2;

    private AutoCompleteTextView mEditText;
    private AutoCompleteAdapter autoCompleteAdapter;
    SimpleCallListAdapter mAdapter;

    /**
     * Create a new instance of CallActionsDFragment
     */
    static TransferDFragment newInstance() {
        TransferDFragment f = new TransferDFragment();
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
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_transfer, null);

        /*
        ArrayList<Conference> calls = getArguments().getParcelableArrayList("calls");
        final SipCall call_selected = getArguments().getParcelable("call_selected");

        mAdapter = new SimpleCallListAdapter(getActivity(), calls);
        ListView list = (ListView) rootView.findViewById(R.id.concurrent_calls);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {

                Intent in = new Intent();
                in.putExtra("target", mAdapter.getItem(pos));
                in.putExtra("transfer", call_selected);

                getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_TRANSFER_CONF, in);
                dismiss();
            }
        });
        list.setEmptyView(rootView.findViewById(R.id.empty_view));

        mEditText = (AutoCompleteTextView) rootView.findViewById(R.id.external_number);
        mEditText.setAdapter(autoCompleteAdapter);

        final AlertDialog a = new AlertDialog.Builder(getActivity()).setView(rootView)
                .setTitle("Transfer " + call_selected.getContact().getDisplayName())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, new Intent());
                        dismiss();
                    }
                }).create();

        a.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                Button b = a.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if(mEditText.getText().length() == 0){
                            Toast.makeText(getActivity(), "Enter a number to transfer this call", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent in = new Intent();
                            in.putExtra("to_number", mEditText.getText().toString());
                            in.putExtra("transfer", call_selected);
                            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_TRANSFER_NUMBER, in);
                            dismiss();
                        }
                    }
                });

            }
        });
        return a;*/
        return null;
    }

    @Override
    public Loader<ContactsLoader.Result> onCreateLoader(int id, Bundle args) {
        Uri baseUri;

        if (args != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(args.getString("filter")));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }
        ContactsLoader l = new ContactsLoader(getActivity());
        l.forceLoad();
        return l;
    }

    @Override
    public void onLoadFinished(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {

//        ArrayList<CallContact> tmp = data.getParcelableArrayList("Contacts");

    }

    @Override
    public void onLoaderReset(Loader<ContactsLoader.Result> loader) {
        // Thi is called when the last Cursor provided to onLoadFinished
        // mListAdapter.swapCursor(null);
    }

    private class AutoCompleteAdapter extends ArrayAdapter<Address> implements Filterable {

        private LayoutInflater mInflater;
        private Geocoder mGeocoder;
//        private StringBuilder mSb = new StringBuilder();

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

            tv.setText(calls.get(position).getParticipants().get(0).getContact().getDisplayName());
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
