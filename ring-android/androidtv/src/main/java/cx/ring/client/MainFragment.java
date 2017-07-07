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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.services.ContactService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class MainFragment extends BrowseFragment implements Observer<ServiceEvent> {
    private static final String TAG = "MainFragment";

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    private Drawable mDefaultBackground;
    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;

    private ArrayList<Conversation> mConversations;
    private ArrayObjectAdapter cardRowAdapter;

    @Inject
    ConversationFacade mConversationFacade;

    @Inject
    ContactService mContactService;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
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
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {

    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            //Do Nothing
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            //Do Nothing
        }
    }

}
