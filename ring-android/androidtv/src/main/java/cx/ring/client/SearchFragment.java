package cx.ring.client;

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
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.client.Contact;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.NameLookupInputHandler;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

/**
 * Created by mschmit on 11/05/17.
 */

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
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        if (mDeviceRuntimeService.hasAudioPermission()) {
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    Log.v(TAG, "recognizeSpeech");
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
        Log.d(TAG, "getResultsAdapter");
        Log.d(TAG, mRowsAdapter.toString());

//        ArrayList<> mItems = someResult;
/*        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.addAll(0, mItems);
        HeaderItem header = new HeaderItem("Search results");
        mRowsAdapter.add(new ListRow(header, listRowAdapter));*/
        Log.i(TAG, "Display result");

        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery){
        Log.i(TAG, String.format("Search Query Text Change %s", newQuery));
        Uri uri = new Uri(newQuery);
        if (uri.isRingId()) {
            Log.d(TAG, newQuery + " is a ring id !");
        }
        else {
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
//            private final String query = mQuery;

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
                HeaderItem header = new HeaderItem("Search Results");
                return new ListRow(header, listRowAdapter);
            }

            @Override
            protected void onPostExecute(ListRow listRow) {
                mRowsAdapter.add(listRow);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
