/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package com.savoirfairelinux.sflphone.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.*;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.util.List;
import java.util.ArrayList;

import com.savoirfairelinux.sflphone.R;

/**
 * Main list of Call Elements.
 * We don't manage contacts ourself so they are
 */
public class CallElementList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    final String TAG = "CallElementList";
    ContactManager mContactManager;
    ArrayAdapter mAdapter;
    String mCurFilter;

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID, Contacts.DISPLAY_NAME,
                                                                       Contacts.PHOTO_ID, Contacts.LOOKUP_KEY };
    static final String[] CONTACTS_PHONES_PROJECTION = new String[] { Phone.NUMBER, Phone.TYPE };
    static final String[] CONTACTS_SIP_PROJECTION = new String[] { SipAddress.SIP_ADDRESS, SipAddress.TYPE };

    /**
     * Runnable that fill information in a contact card asynchroniously.
     */
    public static class InfosLoader implements Runnable
    {
        private View view;
        private long cid;
        private ContentResolver cr;

        public InfosLoader(Context context, View element, long contact_id)
        {
            cid = contact_id;
            cr = context.getContentResolver();
            view = element;
        }

        public static Bitmap loadContactPhoto(ContentResolver cr, long  id) {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
            if (input == null) {
                return null;
            }
            return BitmapFactory.decodeStream(input);
        }
		
        @Override
        public void run()
        {
             final Bitmap photo_bmp = loadContactPhoto(cr, cid);

             Cursor phones = cr.query(CommonDataKinds.Phone.CONTENT_URI,
                                      CONTACTS_PHONES_PROJECTION,
                                      CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                      new String[] { Long.toString(cid) }, null);

            final List<String> numbers = new ArrayList<String>();
            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(CommonDataKinds.Phone.NUMBER));
                //int type = phones.getInt(phones.getColumnIndex(CommonDataKinds.Phone.TYPE));
                numbers.add(number);
            }
            phones.close();
            // TODO: same for SIP adresses.

            final Bitmap bmp = photo_bmp;
            view.post(new Runnable()
            {
                @Override
                public void run()
                {
                /*
                ImageView photo_view = (ImageView) view.findViewById(R.id.photo);
                TextView phones_txt = (TextView) view.findViewById(R.id.phones);

                if (photo_bmp != null) {
                    photo_view.setImageBitmap(bmp);
                    photo_view.setVisibility(View.VISIBLE);
                } else {
                    photo_view.setVisibility(View.GONE);
                }

                if (numbers.size() > 0) {
                    String phonestxt = numbers.get(0);
                    for (int i = 1, n = numbers.size(); i < n; i++)
                        phonestxt += "\n" + numbers.get(i);
                    phones_txt.setText(phonestxt);
                    phones_txt.setVisibility(View.VISIBLE);
                } else
                    phones_txt.setVisibility(View.GONE);
                */
                }
            });
        }
    }

    /**
     * A CursorAdapter that creates and update call elements using corresponding contact infos.
     * TODO: handle contact list separatly to allow showing synchronized contacts on Call cards with multiple contacts etc.
     */
    public static class CallElementAdapter extends ArrayAdapter
    {
        private ExecutorService infos_fetcher = Executors.newCachedThreadPool();
        private Context mContext;
        private final List mCallList;

        public CallElementAdapter(Context context, List callList)
        {
            super(context, R.layout.call_element, callList);
            mContext = context;
            mCallList = callList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView = convertView;
            CallElementView entryView = null;

            if(rowView == null)
            {
                // Get a new instance of the row layout view
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.call_element, null);

                // Hold the view objects in an object
                // so they don't need to be re-fetched
                entryView = new CallElementView();
                entryView.toggleButton = (ImageButton) rowView.findViewById(R.id.toggleButton1);
                entryView.button = (Button) rowView.findViewById(R.id.button2);
                entryView.photo = (ImageView) rowView.findViewById(R.id.photo);
                entryView.displayName = (TextView) rowView.findViewById(R.id.display_name);
                entryView.phones = (TextView) rowView.findViewById(R.id.phones);

                // Cache the view obects in the tag
                // so they can be re-accessed later
                rowView.setTag(entryView);
            } else {
                entryView = (CallElementView) rowView.getTag();
            }

            // Transfer the stock data from the data object
            // to the view objects
            SipCall call = (SipCall) mCallList.get(position);
            entryView.displayName.setText(call.mCallInfo.mDisplayName);
            entryView.phones.setText(call.mCallInfo.mPhone);

            return rowView;
        }
    };

    public static class CallElementView
    {
        protected ImageButton toggleButton;
        protected Button button;
        protected ImageView photo;
        protected TextView displayName;
        protected TextView phones; 
    }

    public void addCall(SipCall c)
    {
        Log.i(TAG, "Adding call " + c.mCallInfo.mDisplayName);
        mAdapter.add(c);
    }

    public void removeCall(SipCall c)
    {
        Log.i(TAG, "Removing call " + c.mCallInfo.mDisplayName);
        mAdapter.remove(c);
    }
        

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data.  In a real
        // application this would come from a resource.
        //setEmptyText("No phone numbers");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);


        // Create an empty adapter we will use to display the loaded data.
        List calls = new ArrayList();
        mAdapter = new CallElementAdapter(getActivity(), calls);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        //setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        // getLoaderManager().initLoader(0, null, this)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //LayoutInflater newInflater = inflater.cloneInContext(new ContextThemeWrapper(getActivity(), R.style.));
        View inflatedView = inflater.inflate(R.layout.call_element_list, container, false);
        return inflatedView;
    }
	
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        // Insert desired behavior here.
        Log.i("CallElementList", "Item clicked: " + id);
        SipCall call = (SipCall) mAdapter.getItem(position);
        call.hangup(); 
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{

		//return new CursorLoader(getActivity(), CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);

		// This is called when a new Loader needs to be created.  This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.
		Uri baseUri;

		if (mCurFilter != null) {
			baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
		} else {
			baseUri = Contacts.CONTENT_URI;
		}

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		String select = "((" + Contacts.DISPLAY_NAME
						+ " NOTNULL) AND ("
						+ Contacts.HAS_PHONE_NUMBER
						+ "=1) AND ("
						+ Contacts.DISPLAY_NAME
						+ " != '' ))";
		//String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND (" + Contacts.DISPLAY_NAME + " != '' ))";

		return new CursorLoader(getActivity(),
								baseUri,
								CONTACTS_SUMMARY_PROJECTION,
								select,
								null,
								Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data)
	{
		// Swap the new cursor in.  (The framework will take care of closing the
		// old cursor once we return.)
		// mAdapter.swapCursor(data);

		// The list should now be shown.
		/*
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}*/
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed.  We need to make sure we are no
		// longer using it.
		// mAdapter.swapCursor(null);
	}
}
