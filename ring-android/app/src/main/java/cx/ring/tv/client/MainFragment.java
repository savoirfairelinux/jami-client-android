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

package cx.ring.tv.client;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.search.SearchActivity;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class MainFragment extends BrowseFragment implements Observer<ServiceEvent> {
    private static final String TAG = "MainFragment";
    @Inject
    ConversationFacade mConversationFacade;
    @Inject
    ContactService mContactService;
    @Inject
    AccountService mAccountService;
    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;
    private ArrayList<Conversation> mConversations;
    private ArrayObjectAdapter cardRowAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();

        mConversationFacade.addObserver(this);
        mContactService.addObserver(this);

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

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_ring_logo_white));
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
            case CONVERSATIONS_CHANGED:
                new ShowSpinnerTask().execute();
                break;
        }

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
                cardRowAdapter.add(callContact);
            }
            mRowsAdapter.notifyArrayItemRangeChanged(0, mConversations.size());
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof CallContact) {
                CallContact contact = (CallContact) item;
                Log.d(TAG, "item: " + item.toString());

                Intent intent = new Intent(getActivity(), TVCallActivity.class);
                intent.putExtra("account", mAccountService.getCurrentAccount().getAccountID());
                intent.putExtra("ringId", contact.getPhones().get(0).getNumber().toString());
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        TVCallActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
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
            getActivity().runOnUiThread(new Runnable() {
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
