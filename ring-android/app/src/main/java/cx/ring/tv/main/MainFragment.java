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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.navigation.HomeNavigationViewModel;
import cx.ring.services.VCardServiceImpl;
import cx.ring.tv.about.AboutActivity;
import cx.ring.tv.account.TVAccountExport;
import cx.ring.tv.account.TVProfileEditingFragment;
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
import cx.ring.tv.model.TVListViewModel;
import cx.ring.tv.search.SearchActivity;
import cx.ring.tv.views.CustomTitleView;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.QRCodeUtils;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {

    private static final String TAG = MainFragment.class.getSimpleName();
    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;
    private static final int TRUST_REQUEST_ROW_POSITION = 1;
    private static final int QR_ITEM_POSITION = 2;
    private SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter cardRowAdapter;
    private ArrayObjectAdapter contactRequestRowAdapter;
    private CustomTitleView titleView;
    private CardListRow requestsRow;
    private CardPresenterSelector selector;
    private IconCard qrCard = null;
    private ListRow myAccountRow;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUIElements(requireActivity());
        titleView = view.findViewById(R.id.browse_title_group);
        presenter.reloadAccountInfos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposable.clear();
    }

    private void setupUIElements(@NonNull Activity activity) {
        selector = new CardPresenterSelector(activity);
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
        myAccountRow = createMyAccountRow(activity);
        mRowsAdapter.add(myAccountRow);
        mRowsAdapter.add(createAboutCardRow(activity));
        setAdapter(mRowsAdapter);

        // listeners
        setOnSearchClickedListener(view -> {
            startActivity(new Intent(getActivity(), SearchActivity.class));
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private ListRow createRow(String titleSection, List<Card> cards, boolean shadow) {
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

    private ListRow createMyAccountRow(@NonNull Context context) {
        qrCard = IconCardHelper.getAccountShareCard(context, null);
        List<Card> cards = new ArrayList<>(4);
        cards.add(IconCardHelper.getAccountAddDeviceCard(context));
        cards.add(IconCardHelper.getAccountManagementCard(context));
        cards.add(qrCard);
        cards.add(IconCardHelper.getAccountSettingsCard(context));
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

    private Row createAboutCardRow(@NonNull Context context) {
        List<Card> cards = new ArrayList<>(3);
        cards.add(IconCardHelper.getVersionCard(context));
        cards.add(IconCardHelper.getLicencesCard(context));
        cards.add(IconCardHelper.getContributorCard(context));
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

    static private BitmapDrawable prepareAccountQr(Context context, String accountId) {
        Log.w(TAG, "prepareAccountQr " + accountId);
        if (TextUtils.isEmpty(accountId))
            return null;
        QRCodeUtils.QRCodeData qrCodeData = QRCodeUtils.encodeStringAsQRCodeData(accountId, 0X00000000, 0xFFFFFFFF);
        Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth(), qrCodeData.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), 0, 0, qrCodeData.getWidth(), qrCodeData.getHeight());
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @Override
    public void displayAccountInfos(final HomeNavigationViewModel viewModel) {
        Account account = viewModel.getAccount();
        if (account != null)
            updateModel(account);
    }

    @Override
    public void updateModel(Account account) {
        Context context = requireContext();
        String address = account.getDisplayUsername();
        mDisposable.clear();
        mDisposable.add(VCardServiceImpl
                .loadProfile(account)
                .observeOn(AndroidSchedulers.mainThread())
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
                .flatMap(p -> AvatarDrawable.load(context, account))
                .subscribe(a -> {
                    titleView.getLogoView().setVisibility(View.VISIBLE);
                    titleView.getLogoView().setImageDrawable(a);
                }));
        qrCard.setDrawable(prepareAccountQr(context, account.getUri()));
        myAccountRow.getAdapter().notifyItemRangeChanged(QR_ITEM_POSITION, 1);
    }

    @Override
    public void showExportDialog(String pAccountID) {
        GuidedStepSupportFragment wizard = TVAccountExport.createInstance(pAccountID);
        GuidedStepSupportFragment.add(getFragmentManager(), wizard, R.id.main_browse_fragment);
    }

    @Override
    public void showProfileEditing() {
        GuidedStepSupportFragment.add(getFragmentManager(), new TVProfileEditingFragment(), R.id.main_browse_fragment);
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
                    Intent intent = new Intent(getActivity(), TVContactActivity.class);
                    intent.putExtra(TVContactActivity.CONTACT_REQUEST_URI, model.getContact().getPrimaryUri());
                    intent.setDataAndType(ConversationPath.toUri(model.getAccountId(), model.getContact().getPrimaryUri()), TVContactActivity.TYPE_CONTACT_REQUEST_INCOMING);

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            TVContactActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(model.getAccountId(), model.getContact().getPrimaryUri()), getActivity(), TVContactActivity.class);

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            TVContactActivity.SHARED_ELEMENT_NAME).toBundle();
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
                        ImageView view = ((ImageCardView) itemViewHolder.view).getMainImageView();
                        Intent intent = new Intent(getActivity(), TVShareActivity.class);
                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, TVShareActivity.SHARED_ELEMENT_NAME).toBundle();
                        requireActivity().startActivity(intent, bundle);
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
