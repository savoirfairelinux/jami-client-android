/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.search;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingTVApplication;
import cx.ring.call.CallActivity;
import cx.ring.client.CardPresenter;
import cx.ring.client.Contact;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class SearchFragment extends android.support.v17.leanback.app.SearchFragment implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider, Observer<ServiceEvent> {
    private static final String TAG = SearchFragment.class.getSimpleName();

    private static final int REQUEST_SPEECH = 0x00000010;
    private ArrayObjectAdapter mRowsAdapter;

    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLastNameLookupInput = null;
    private Contact contact = new Contact();

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        setSearchResultProvider(this);

        // dependency injection
        ((RingTVApplication) getActivity().getApplication()).getAndroidTVInjectionComponent().inject(this);

        if (mDeviceRuntimeService.hasAudioPermission()) {
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Cannot find activity for speech recognizer", e);
                    }
                }
            });
        }
        mAccountService.addObserver(this);
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        Log.d(TAG, mRowsAdapter.toString());
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        Log.i(TAG, String.format("Search Query Text Change %s", newQuery));
        Uri uri = new Uri(newQuery);
        if (uri.isRingId()) {
            Log.d(TAG, newQuery + " is a ring id !");
        } else {
            Log.d(TAG, "Nothin found for " + newQuery);
        }

        // Ring search
        if (mNameLookupInputHandler == null) {
            mNameLookupInputHandler = new NameLookupInputHandler(new WeakReference<>(mAccountService));
        }

        mNameLookupInputHandler.enqueueNextLookup(newQuery);
        mLastNameLookupInput = newQuery;

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search Query Text Submit %s", query));
        return true;
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        Log.d(TAG, "update");
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case REGISTERED_NAME_FOUND:
                String name = event.getEventInput(ServiceEvent.EventInput.NAME, String.class);
                if (mLastNameLookupInput != null
                        && (mLastNameLookupInput.equals("") || !mLastNameLookupInput.equals(name))) {
                    return;
                }
                String address = event.getEventInput(ServiceEvent.EventInput.ADDRESS, String.class);
                int state = event.getEventInput(ServiceEvent.EventInput.STATE, Integer.class);
                Log.d(TAG, "Name : " + name + ", address : " + address + ", state : " + state);
                if (!address.equals("") && address.length() > 2) {
                    loadRows(name, address);
                    setOnItemViewClickedListener(new ItemViewClickedListener());
                }
                break;
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = new Intent(getActivity(), CallActivity.class);
            intent.putExtra("account", mAccountService.getCurrentAccount().getAccountID());
            intent.putExtra("ringId", contact.getAddress());
            startActivity(intent);
        }
    }

    private void loadRows(final String name, final String address) {
        // offload processing from the UI thread
        new AsyncTask<String, Void, ListRow>() {

            @Override
            protected void onPreExecute() {
                mRowsAdapter.clear();
            }

            @Override
            protected ListRow doInBackground(String... params) {
                final List<Contact> result = new ArrayList<>();
                contact.setName(name);
                contact.setAddress(address);
                result.add(contact);

                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
                listRowAdapter.addAll(0, result);
                HeaderItem header = new HeaderItem(getActivity().getResources().getString(R.string.search_results));
                return new ListRow(header, listRowAdapter);
            }

            @Override
            protected void onPostExecute(ListRow listRow) {
                mRowsAdapter.add(listRow);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
