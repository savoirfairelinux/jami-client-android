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
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.CallContact;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.search.SearchActivity;

public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {
    private static final String TAG = MainFragment.class.getSimpleName();

    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;

    // MISC sections card ids
    public static final long ID_ABOUT = 0;
    public static final long ID_LICENCE = 1;
    public static final long ID_CONTRIBUTORS = 2;


    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;
    SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter cardRowAdapter;
    private ArrayObjectAdapter mOtherAdapter;

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
    }

    private void setupUIElements() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_ring_logo_white));
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.color_primary_dark));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.color_primary_light));

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        /* CardPresenter */
        HeaderItem cardPresenterHeader = new HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header));
        cardRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        mRowsAdapter.add(new ListRow(cardPresenterHeader, cardRowAdapter));

        mOtherAdapter = new ArrayObjectAdapter(new CardPresenter());
        final HeaderItem miscHeader = new HeaderItem(HEADER_MISC, getString(R.string.menu_item_about));
        mOtherAdapter.add(new CardPresenter.SimpleCard(ID_ABOUT, getString(R.string.version_section), getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME, R.drawable.ic_ring_logo_white));
        mOtherAdapter.add(new CardPresenter.SimpleCard(ID_LICENCE, getString(R.string.section_license), R.drawable.ic_ring_logo_white));
        mOtherAdapter.add(new CardPresenter.SimpleCard(ID_CONTRIBUTORS, getString(R.string.credits), R.drawable.ic_group_black));

        mRowsAdapter.add(new ListRow(miscHeader, mOtherAdapter));


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
                cardRowAdapter.addAll(0, contacts);
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

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof CallContact) {
                presenter.contactClicked((CallContact) item);
            } else if (item instanceof CardPresenter.SimpleCard) {
                long id = ((CardPresenter.SimpleCard) item).getId();
                if (id == ID_LICENCE) {
//                    startActivityForResult(new Intent(this, org.videolan.vlc.gui.tv.preferences.PreferencesActivity.class), ACTIVITY_RESULT_PREFERENCES);
                } else if (id == ID_ABOUT) {
//                    startActivity(new Intent(this, org.videolan.vlc.gui.tv.AboutActivity.class));
                } else if (id == ID_CONTRIBUTORS) {
//                    startActivity(new Intent(this, org.videolan.vlc.gui.tv.AboutActivity.class));
                }
            }
        }
    }

}
