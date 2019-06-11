/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>s
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
package cx.ring.tv.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.navigation.RingNavigationViewModel;
import cx.ring.services.VCardServiceImpl;
import cx.ring.tv.about.AboutActivity;
import cx.ring.tv.account.TVAccountExport;
import cx.ring.tv.account.TVProfileEditingActivity;
import cx.ring.tv.account.TVSettingsActivity;
import cx.ring.tv.account.TVShareActivity;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardListRow;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.CardRow;
import cx.ring.tv.cards.ShadowRowPresenterSelector;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.tv.cards.iconcards.IconCard;
import cx.ring.tv.cards.iconcards.IconCardHelper;
import cx.ring.tv.contact.TVContactActivity;
import cx.ring.tv.contactrequest.TVContactRequestActivity;
import cx.ring.tv.model.TVListViewModel;
import cx.ring.tv.search.SearchActivity;
import cx.ring.tv.views.CustomTitleView;
import cx.ring.views.AvatarDrawable;

public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {

    private static final String TAG = MainFragment.class.getSimpleName();
    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;
    private static final int TRUST_REQUEST_ROW_POSITION = 1;
    SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter cardRowAdapter;
    private ArrayObjectAdapter contactRequestRowAdapter;
    private CustomTitleView titleView;
    private CardListRow requestsRow;
    private CardPresenterSelector selector;

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
        mBackgroundManager.setDrawable(getResources().getDrawable(R.drawable.tv_background));
        presenter.reloadAccountInfos();
    }

    private void setupUIElements() {
        selector = new CardPresenterSelector(getActivity());
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.color_primary_dark));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.color_primary_light));

        mRowsAdapter = new ArrayObjectAdapter(new ShadowRowPresenterSelector());

        /* Contact Presenter */
        CardRow contactRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                true,
                getString(R.string.tv_contact_row_header),
                new ArrayList<>());
        HeaderItem cardPresenterHeader = new HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header));
        cardRowAdapter = new ArrayObjectAdapter(selector);

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

        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(selector);
        for (Card card : cards) {
            listRowAdapter.add(card);
        }

        return new CardListRow(new HeaderItem(HEADER_MISC, titleSection), listRowAdapter, row);
    }

    private Row createMyAccountRow() {
        List<Card> cards = new ArrayList<>();
        cards.add(IconCardHelper.getAccountAddDeviceCard(getActivity()));
        cards.add(IconCardHelper.getAccountManagementCard(getActivity()));
        cards.add(IconCardHelper.getAccountShareCard(getActivity()));
        cards.add(IconCardHelper.getAccountSettingsCard(getActivity()));

        return createRow(getString(R.string.ring_account), cards, false);
    }

    private CardListRow createContactRequestRow() {
        CardRow contactRequestRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                true,
                getString(R.string.menu_item_contact_request),
                new ArrayList<ContactCard>());

        contactRequestRowAdapter = new ArrayObjectAdapter(selector);

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
        if (show) {
            mSpinnerFragment = new SpinnerFragment();
            getFragmentManager().beginTransaction().replace(R.id.main_browse_fragment, mSpinnerFragment).commitAllowingStateLoss();
        } else {
            getFragmentManager().beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void refreshContact(final int index, final TVListViewModel contact) {
        ContactCard contactCard = (ContactCard) cardRowAdapter.get(index);
        contactCard.setModel(contact);
        cardRowAdapter.replace(index, contactCard);
    }

    @Override
    public void showContacts(final List<TVListViewModel> contacts) {
        List<ContactCard> cards = new ArrayList<>(contacts.size());
        for (TVListViewModel contact : contacts)
            cards.add(new ContactCard(contact));
        cardRowAdapter.setItems(cards,null);
    }

    @Override
    public void showContactRequests(final List<TVListViewModel> contacts) {
        CardListRow row = (CardListRow) mRowsAdapter.get(TRUST_REQUEST_ROW_POSITION);
        boolean isRowDisplayed = row != null && row == requestsRow;

        List<ContactCard> cards = new ArrayList<>(contacts.size());
        for (TVListViewModel contact : contacts)
            cards.add(new ContactCard(contact));

        if (isRowDisplayed && contacts.isEmpty()) {
            mRowsAdapter.removeItems(TRUST_REQUEST_ROW_POSITION, 1);
        } else if (!contacts.isEmpty()) {
            if (requestsRow == null)
                requestsRow = createContactRequestRow();
            contactRequestRowAdapter.setItems(cards, null);
            if (!isRowDisplayed)
                mRowsAdapter.add(TRUST_REQUEST_ROW_POSITION, requestsRow);
        }
    }

    @Override
    public void callContact(String accountID, String number) {
        Intent intent = new Intent(getActivity(), TVCallActivity.class);
        intent.putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountID);
        intent.putExtra(ConversationFragment.KEY_CONTACT_RING_ID, number);
        getActivity().startActivity(intent, null);
    }

    @Override
    public void displayAccountInfos(final RingNavigationViewModel viewModel) {
        if (getActivity() == null) {
            Log.e(TAG, "displayAccountInfos: Not able to get activity");
            return;
        }

        Account account = viewModel.getAccount();
        String address = account.getDisplayUsername();
        VCardServiceImpl
                .loadProfile(account)
                .doOnSuccess(profile -> {
                    if (profile.first != null && !profile.first.isEmpty()) {
                        titleView.setAlias(profile.first);
                        if (address != null) {
                            setTitle(address);
                        } else {
                            setTitle("");
                        }
                    } else {
                        titleView.setAlias(address);
                    }
                })
                .flatMap(p -> AvatarDrawable.load(getActivity(), account))
                .subscribe(a -> {
                    titleView.getLogoView().setVisibility(View.VISIBLE);
                    titleView.getLogoView().setImageDrawable(a);
                });
    }

    @Override
    public void showExportDialog(String pAccountID) {
        GuidedStepSupportFragment wizard = TVAccountExport.createInstance(pAccountID);
        GuidedStepSupportFragment.add(getFragmentManager(), wizard, R.id.main_browse_fragment);
    }

    @Override
    public void showProfileEditing() {
        Intent intent = new Intent(getActivity(), TVProfileEditingActivity.class);
        startActivity(intent);
    }

    @Override
    public void showAccountShare() {
        Intent intent = new Intent(getActivity(), TVShareActivity.class);
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
                TVListViewModel model = ((ContactCard) item).getModel();
                if (row == requestsRow) {
                    Intent intent = new Intent(getActivity(), TVContactRequestActivity.class);
                    intent.putExtra(TVContactRequestActivity.CONTACT_REQUEST, model.getContact().getPrimaryUri());

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            TVContactRequestActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                } else {
                    Intent intent = new Intent(getActivity(), TVContactActivity.class);
                    intent.putExtra(TVContactActivity.CONTACT_REQUEST, model.getContact().getPrimaryUri());

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            TVContactRequestActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                }
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
                    case ACCOUNT_SHARE_ACCOUNT:
                        presenter.onShareAccountClicked();
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
