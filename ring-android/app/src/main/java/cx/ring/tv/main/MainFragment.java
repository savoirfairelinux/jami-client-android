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

package cx.ring.tv.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.CallContact;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardListRow;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.CardRow;
import cx.ring.tv.cards.ShadowRowPresenterSelector;
import cx.ring.tv.cards.about.AboutCard;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.tv.search.SearchActivity;

public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {

    private static final String TAG = MainFragment.class.getSimpleName();
    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;
    SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter cardRowAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUIElements();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.reloadConversations();
        presenter.reloadAccountInfos();
    }

    private void setupUIElements() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.color_primary_dark));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.color_primary_light));

        mRowsAdapter = new ArrayObjectAdapter(new ShadowRowPresenterSelector());



        /* Contact Presenter */

        List<Card> cards = new ArrayList<>();
        CardRow contacttRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                true,
                getString(R.string.tv_contact_row_header),
                cards);
        HeaderItem cardPresenterHeader = new HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header));
        cardRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));

        CardListRow contactListRow = new CardListRow(cardPresenterHeader, cardRowAdapter, contacttRow);

        /* CardPresenter */
        mRowsAdapter.add(contactListRow);
        mRowsAdapter.add(createAboutCardRow());

        setAdapter(mRowsAdapter);

        // listeners
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }


    private Row createAboutCardRow() {
        List<Card> cards = new ArrayList<>();
        cards.add(new AboutCard(Card.Type.VERSION, getString(R.string.version_section) + " " + BuildConfig.VERSION_NAME, "", "ic_ring_logo_white" ));
     
        CardRow aboutRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                false,
                getString(R.string.menu_item_about),
                cards);

        PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        for (Card card : cards) {
            listRowAdapter.add(card);
        }

        return new CardListRow(new HeaderItem(HEADER_MISC, getString(R.string.menu_item_about)), listRowAdapter, aboutRow);
    }


    @Override
    public void showLoading(final boolean show) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    mSpinnerFragment = new SpinnerFragment();
                    getFragmentManager().beginTransaction().replace(R.id.main_browse_fragment, mSpinnerFragment).commitAllowingStateLoss();
                } else {
                    getFragmentManager().beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
                }
            }
        });
    }

    @Override
    public void showContacts(final ArrayList<CallContact> contacts) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardRowAdapter.clear();
                for (CallContact contact : contacts) {
                    cardRowAdapter.add(new ContactCard(contact));
                }
                mRowsAdapter.notifyArrayItemRangeChanged(0, contacts.size());
            }
        });
    }

    @Override
    public void callContact(String accountID, String ringID) {
        Intent intent = new Intent(getActivity(), TVCallActivity.class);
        intent.putExtra("account", accountID);
        intent.putExtra("ringId", ringID);
        getActivity().startActivity(intent, null);
    }

    @Override
    public void displayAccountInfos(final String address) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (address != null) {
                    setTitle(address);
                } else {
                    setTitle("");
                }
            }
        });
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ContactCard) {
                presenter.contactClicked(((ContactCard) item).getCallContact());
            }
        }
    }

}
