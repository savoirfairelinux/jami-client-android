/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package cx.ring.client;

import java.util.ArrayList;
import java.util.TimerTask;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ContactService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class MainFragment extends BrowseFragment implements Observer<ServiceEvent> {
    private static final String TAG = "MainFragment";

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;

    private ArrayList<Conversation> mConversations;
    private ArrayObjectAdapter cardRowAdapter;
    private Drawable mDefaultBackground;

    @Inject
    ConversationFacade mConversationFacade;

    @Inject
    ContactService mContactService;

    @Inject
    CallService mCallService;

    @Inject
    AccountService mAccountService;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
        mCallService.addObserver(this);
        mAccountService.addObserver(this);
        mConversationFacade.addObserver(this);
    }

    private void loadRows() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        /* CardPresenter */
        HeaderItem cardPresenterHeader = new HeaderItem(1, "Contacts");
        CardPresenter cardPresenter = new CardPresenter();
        cardRowAdapter = new ArrayObjectAdapter(cardPresenter);
        mRowsAdapter.add(new ListRow(cardPresenterHeader, cardRowAdapter));

        /* set */
        setAdapter(mRowsAdapter);
    }

    private synchronized void getConversations() {
        if (mConversations == null) {
            mConversations = new ArrayList<>();
        }

        mConversations.clear();
        mConversations.addAll(mConversationFacade.getConversationsList());

        if (mConversations != null && mConversations.size() > 0) {
            cardRowAdapter.clear();
            for (int i = 0; i < mConversations.size(); i++) {
                Conversation conversation = mConversations.get(i);
                CallContact callContact = conversation.getContact();
                mContactService.loadContactData(callContact);
                Log.d(TAG, "contact >> " + callContact.getDisplayName() + " " + callContact.getPhoto());

                Contact contact = new Contact();
                contact.setId(i);
                contact.setName(callContact.getDisplayName());
                contact.setAddress(conversation.getUuid());
                contact.setPhoto(callContact.getPhoto());

                cardRowAdapter.add(contact);
                Log.d(TAG, "current contact : " + contact.toString());
            }
            mRowsAdapter.notifyArrayItemRangeChanged(0, mConversations.size());
        }

    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_logo_ring_beta2_blanc));
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        Log.d(TAG, "TV EVENT : " + event.getEventType());
        switch (event.getEventType()) {
            case HISTORY_LOADED:
            case CONVERSATIONS_CHANGED:
                new ShowSpinnerTask().execute();
                break;
            case INCOMING_CALL:
                Log.d(TAG, "TV: Someone is calling?");
                String callId = event.getEventInput(ServiceEvent.EventInput.CALL_ID, String.class);
                if (callId != null) {
                    Log.d(TAG, "call id : " + callId);
                    Intent intent = new Intent(getActivity(), CallActivity.class);
                    intent.putExtra("account", mAccountService.getCurrentAccount().getAccountID());
                    intent.putExtra("ringId", "");
                    intent.putExtra("callId", callId);
                    startActivity(intent);
                }

                break;
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Contact) {
                Contact contact = (Contact) item;
                Log.d(TAG, "item: " + item.toString());

                Intent intent = new Intent(getActivity(), CallActivity.class);
                intent.putExtra("account", mAccountService.getCurrentAccount().getAccountID());
                intent.putExtra("ringId", contact.getAddress());
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }


    private class ShowSpinnerTask extends AsyncTask<Void, Void, Void> {
        SpinnerFragment mSpinnerFragment;

        @Override
        protected void onPreExecute() {
            mSpinnerFragment = new SpinnerFragment();
            getFragmentManager().beginTransaction().add(R.id.main_browse_fragment, mSpinnerFragment).commit();
        }

        @Override
        protected Void doInBackground(Void... params) {
            getActivity().runOnUiThread(new Runnable(){
                public void run() {
                    getConversations();
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            getFragmentManager().beginTransaction().remove(mSpinnerFragment).commit();
        }
    }
}
