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
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.tv.about.AboutActivity;
import cx.ring.tv.account.TVAccountExport;
import cx.ring.tv.account.TVProfileEditingActivity;
import cx.ring.tv.account.TVSettingsActivity;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardListRow;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.CardRow;
import cx.ring.tv.cards.ShadowRowPresenterSelector;
import cx.ring.tv.cards.contactrequests.ContactRequestCard;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.tv.cards.iconcards.IconCard;
import cx.ring.tv.cards.iconcards.IconCardHelper;
import cx.ring.tv.contactrequest.TVContactRequestActivity;
import cx.ring.tv.model.TVContactRequestViewModel;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.tv.search.SearchActivity;
import cx.ring.tv.views.CustomTitleView;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;

public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {

    private static final String TAG = MainFragment.class.getSimpleName();
    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;
    private static final int TRUST_REQUEST_ROW_POSITION = 1;
    SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private DisplayMetrics mMetrics;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter cardRowAdapter;
    private ArrayObjectAdapter contactRequestRowAdapter;
    private CustomTitleView titleView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUIElements();
        titleView = view.findViewById(R.id.browse_title_group);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.reloadConversations();
        presenter.reloadAccountInfos();
        presenter.loadContactRequest();
        mBackgroundManager.setDrawable(getResources().getDrawable(R.drawable.tv_background));
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
        CardRow contactRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                true,
                getString(R.string.tv_contact_row_header),
                cards);
        HeaderItem cardPresenterHeader = new HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header));
        cardRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));

        CardListRow contactListRow = new CardListRow(cardPresenterHeader, cardRowAdapter, contactRow);

        /* CardPresenter */
        mRowsAdapter.add(contactListRow);
        mRowsAdapter.add(createMyAccountRow());
        mRowsAdapter.add(createAboutCardRow());

        setAdapter(mRowsAdapter);

        // listeners
        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private Row createRow(String titleSection, List<Card> cards, boolean shadow) {
        CardRow row = new CardRow(
                CardRow.TYPE_DEFAULT,
                shadow,
                titleSection,
                cards);

        PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        for (Card card : cards) {
            listRowAdapter.add(card);
        }

        return new CardListRow(new HeaderItem(HEADER_MISC, titleSection), listRowAdapter, row);
    }

    private Row createMyAccountRow() {
        List<Card> cards = new ArrayList<>();
        cards.add(IconCardHelper.getAccountAddDeviceCard(getActivity()));
        cards.add(IconCardHelper.getAccountManagementCard(getActivity()));
        cards.add(IconCardHelper.getAccountSettingsCard(getActivity()));

        return createRow(getString(R.string.ring_account), cards, false);
    }

    private Row createContactRequestRow() {
        List<Card> cards = new ArrayList<>();
        CardRow contactRequestRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                true,
                getString(R.string.menu_item_contact_request),
                cards);

        contactRequestRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));

        return new CardListRow(new HeaderItem(HEADER_MISC, getString(R.string.menu_item_contact_request)),
                contactRequestRowAdapter,
                contactRequestRow);
    }

    private Row createAboutCardRow() {
        List<Card> cards = new ArrayList<>();
        cards.add(IconCardHelper.getVersionCard(getActivity()));
        cards.add(IconCardHelper.getLicencesCard(getActivity()));
        cards.add(IconCardHelper.getContributorCard(getActivity()));

        return createRow(getString(R.string.menu_item_about), cards, false);
    }

    @Override
    public void showLoading(final boolean show) {
        getActivity().runOnUiThread(() -> {
            if (show) {
                mSpinnerFragment = new SpinnerFragment();
                getFragmentManager().beginTransaction().replace(R.id.main_browse_fragment, mSpinnerFragment).commitAllowingStateLoss();
            } else {
                getFragmentManager().beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
            }
        });
    }

    @Override
    public void refreshContact(final int index, final TVListViewModel contact) {
        getActivity().runOnUiThread(() -> {
            ContactCard contactCard = (ContactCard) cardRowAdapter.get(index);
            contactCard.setModel(contact);
            cardRowAdapter.notifyArrayItemRangeChanged(index, 1);
        });
    }

    @Override
    public void showContacts(final List<TVListViewModel> contacts) {
        getActivity().runOnUiThread(() -> {
            cardRowAdapter.clear();
            if (!contacts.isEmpty()) {
                for (TVListViewModel contact : contacts) {
                    cardRowAdapter.add(new ContactCard(contact));
                }
                cardRowAdapter.notifyArrayItemRangeChanged(0, contacts.size());
            }
        });
    }

    @Override
    public void showContactRequestsRow(boolean display) {
        CardListRow row = (CardListRow) mRowsAdapter.get(TRUST_REQUEST_ROW_POSITION);
        boolean isContactRequestRowDisplayed = row.getCardRow().getTitle().equals(getString(R.string.menu_item_contact_request));

        if (display && !isContactRequestRowDisplayed) {
            mRowsAdapter.add(TRUST_REQUEST_ROW_POSITION, createContactRequestRow());
        } else if (!display && isContactRequestRowDisplayed) {
            mRowsAdapter.removeItems(TRUST_REQUEST_ROW_POSITION, 1);
        }
    }

    @Override
    public void showContactRequests(final ArrayList<TVContactRequestViewModel> contactRequests) {
        contactRequestRowAdapter.clear();
        for (TVContactRequestViewModel contact : contactRequests) {
            contactRequestRowAdapter.add(new ContactRequestCard(contact));
        }
        contactRequestRowAdapter.notifyArrayItemRangeChanged(0, contactRequests.size());
        mRowsAdapter.notifyItemRangeChanged(2, 3);
    }

    @Override
    public void callContact(String accountID, String ringID) {
        Intent intent = new Intent(getActivity(), TVCallActivity.class);
        intent.putExtra("account", accountID);
        intent.putExtra("ringId", ringID);
        getActivity().startActivity(intent, null);
    }

    @Override
    public void displayAccountInfos(final String address, final RingNavigationViewModel viewModel) {
        getActivity().runOnUiThread(() -> {
            if (address != null) {
                setTitle(address);
            } else {
                setTitle("");
            }

            if (getActivity() == null) {
                Log.e(TAG, "displayAccountInfos: Not able to get activity");
                return;
            }

            VCard vcard = viewModel.getVcard(getActivity().getFilesDir());
            if (vcard == null) {
                Log.e(TAG, "displayAccountInfos: Not able to get vcard");
                return;
            }

            FormattedName formattedName = vcard.getFormattedName();
            if (formattedName != null) {
                titleView.setAlias(formattedName.getValue());
            }

            List<Photo> photos = vcard.getPhotos();
            if (!photos.isEmpty() && photos.get(0) != null) {
                titleView.setCurrentAccountPhoto(photos.get(0).getData());
            }
        });
    }

    @Override
    public void showExportDialog(String pAccountID) {
        GuidedStepFragment wizard = TVAccountExport.createInstance(pAccountID);
        GuidedStepFragment.add(getFragmentManager(), wizard, R.id.main_browse_fragment);
    }

    @Override
    public void showProfileEditing() {
        Intent intent = new Intent(getActivity(), TVProfileEditingActivity.class);
        startActivity(intent);
    }

    @Override
    public void showLicence(int aboutType) {
        Intent intent = new Intent(getActivity(), AboutActivity.class);
        intent.putExtra("abouttype", aboutType);
        startActivity(intent);
    }

    @Override
    public void showSettings() {
        Intent intent = new Intent(getActivity(), TVSettingsActivity.class);
        startActivity(intent);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ContactCard) {
                presenter.contactClicked(((ContactCard) item).getModel());
            } else if (item instanceof ContactRequestCard) {
                Intent intent = new Intent(getActivity(), TVContactRequestActivity.class);
                intent.putExtra(TVContactRequestActivity.CONTACT_REQUEST, ((ContactRequestCard) item).getModel());

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        TVContactRequestActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);

            } else if (item instanceof IconCard) {
                IconCard card = (IconCard) item;
                switch (card.getType()) {
                    case ABOUT_CONTRIBUTOR:
                    case ABOUT_LICENCES:
                        presenter.onLicenceClicked(card.getType().ordinal());
                        break;
                    case ACCOUNT_ADD_DEVICE:
                        presenter.onExportClicked();
                        break;
                    case ACCOUNT_EDIT_PROFILE:
                        presenter.onEditProfileClicked();
                        break;
                    case ACCOUNT_SETTINGS:
                        presenter.onSettingsClicked();
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
