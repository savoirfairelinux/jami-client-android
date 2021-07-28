/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.main;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.tv.contact.TVContactActivity;
import cx.ring.tv.contact.TVContactFragment;
import cx.ring.views.AvatarFactory;

import net.jami.model.Account;
import net.jami.navigation.HomeNavigationViewModel;
import cx.ring.services.VCardServiceImpl;
import cx.ring.tv.account.TVAccountExport;
import cx.ring.tv.account.TVProfileEditingFragment;
import cx.ring.tv.settings.TVSettingsActivity;
import cx.ring.tv.account.TVShareActivity;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.cards.Card;
import cx.ring.tv.cards.CardListRow;
import cx.ring.tv.cards.CardPresenterSelector;
import cx.ring.tv.cards.CardRow;
import cx.ring.tv.cards.ShadowRowPresenterSelector;
import cx.ring.tv.cards.contacts.ContactCard;
import cx.ring.tv.cards.CardView;
import cx.ring.tv.cards.iconcards.IconCard;
import cx.ring.tv.cards.iconcards.IconCardHelper;
import cx.ring.tv.search.SearchActivity;
import cx.ring.tv.views.CustomTitleView;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;

import net.jami.smartlist.SmartListViewModel;
import net.jami.utils.QRCodeUtils;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class MainFragment extends BaseBrowseFragment<MainPresenter> implements MainView {

    private static final String TAG = MainFragment.class.getSimpleName();
    // Sections headers ids
    private static final long HEADER_CONTACTS = 0;
    private static final long HEADER_MISC = 1;
    private static final int TRUST_REQUEST_ROW_POSITION = 1;
    private static final int QR_ITEM_POSITION = 2;

    private static final String PREFERENCES_CHANNELS = "channels";
    private static final String KEY_CHANNEL_CONVERSATIONS = "conversations";

    private static final Uri HOME_URI = new Uri.Builder()
            .scheme(ContentUriHandler.SCHEME_TV)
            .authority(ContentUriHandler.AUTHORITY)
            .appendPath(ContentUriHandler.PATH_TV_HOME)
            .build();

    //private TVContactFragment mContactFragment;
    private SpinnerFragment mSpinnerFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter cardRowAdapter;
    private ArrayObjectAdapter contactRequestRowAdapter;
    private CustomTitleView mTitleView;
    private CardListRow requestsRow;
    private CardPresenterSelector selector;
    private IconCard qrCard = null;
    private ListRow AccountSettingsRow;
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final CompositeDisposable mHomeChannelDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHeadersState(HEADERS_DISABLED);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mTitleView = view.findViewById(R.id.browse_title_group);
        super.onViewCreated(view, savedInstanceState);
        setupUIElements(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposable.clear();
    }

    private void setupUIElements(@NonNull Activity activity) {
        selector = new CardPresenterSelector(activity);

        mRowsAdapter = new ArrayObjectAdapter(new ShadowRowPresenterSelector());

        /* Contact Presenter */
        CardRow contactRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                false,
                getString(R.string.tv_contact_row_header),
                new ArrayList<>());
        HeaderItem cardPresenterHeader = new HeaderItem(HEADER_CONTACTS, getString(R.string.tv_contact_row_header));
        cardRowAdapter = new ArrayObjectAdapter(selector);

        CardListRow contactListRow = new CardListRow(cardPresenterHeader, cardRowAdapter, contactRow);

        /* CardPresenter */
        mRowsAdapter.add(contactListRow);
        AccountSettingsRow = createAccountSettingsRow(activity);
        mRowsAdapter.add(AccountSettingsRow);
        setAdapter(mRowsAdapter);

        // listeners
        setOnSearchClickedListener(view -> startActivity(new Intent(getActivity(), SearchActivity.class)));
        setOnItemViewClickedListener(new ItemViewClickedListener());
        mTitleView.getSettingsButton().setOnClickListener(view -> presenter.onSettingsClicked());
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

    private ListRow createAccountSettingsRow(@NonNull Context context) {
        qrCard = IconCardHelper.getAccountShareCard(context, null);
        List<Card> cards = new ArrayList<>(3);
        cards.add(IconCardHelper.getAccountManagementCard(context));
        cards.add(IconCardHelper.getAccountAddDeviceCard(context));
        cards.add(qrCard);
        return createRow(getString(R.string.account_tv_settings_header), cards, false);
    }

    private CardListRow createContactRequestRow() {
        CardRow contactRequestRow = new CardRow(
                CardRow.TYPE_DEFAULT,
                false,
                getString(R.string.menu_item_contact_request),
                new ArrayList<ContactCard>());

        contactRequestRowAdapter = new ArrayObjectAdapter(selector);

        return new CardListRow(new HeaderItem(HEADER_MISC, getString(R.string.menu_item_contact_request)),
                contactRequestRowAdapter,
                contactRequestRow);
    }

    @Override
    public void showLoading(final boolean show) {
        if (show) {
            mSpinnerFragment = new SpinnerFragment();
            getParentFragmentManager().beginTransaction().replace(R.id.main_browse_fragment, mSpinnerFragment).commitAllowingStateLoss();
        } else {
            getParentFragmentManager().beginTransaction().remove(mSpinnerFragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void refreshContact(final int index, final SmartListViewModel contact) {
        ContactCard contactCard = (ContactCard) cardRowAdapter.get(index);
        contactCard.setModel(contact);
        cardRowAdapter.replace(index, contactCard);
    }

    @Override
    public void showContacts(final List<SmartListViewModel> contacts) {
        List<Card> cards = new ArrayList<>(contacts.size() + 1);
        cards.add(IconCardHelper.getAddContactCard(requireContext()));
        for (SmartListViewModel contact : contacts)
            cards.add(new ContactCard(contact));
        cardRowAdapter.setItems(cards, null);
        buildHomeChannel(requireContext().getApplicationContext(), contacts);
    }

    private static long createHomeChannel(Context context)  {
        Channel channel = new Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(context.getString(R.string.navigation_item_conversation))
                .setAppLinkIntentUri(HOME_URI)
                .build();
        ContentResolver cr = context.getContentResolver();
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES_CHANNELS, Context.MODE_PRIVATE);
        long channelId = sharedPref.getLong(KEY_CHANNEL_CONVERSATIONS, -1);
        if (channelId == -1) {
            Uri channelUri = cr.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues());
            channelId = ContentUris.parseId(channelUri);
            sharedPref.edit().putLong(KEY_CHANNEL_CONVERSATIONS, channelId).apply();
            int targetSize = (int) (AvatarFactory.SIZE_NOTIF * context.getResources().getDisplayMetrics().density);
            int targetPaddingSize = (int) (AvatarFactory.SIZE_PADDING * context.getResources().getDisplayMetrics().density);
            ChannelLogoUtils.storeChannelLogo(context, channelId, BitmapUtils.drawableToBitmap(context.getDrawable(R.drawable.ic_jami_48), targetSize, targetPaddingSize));
            TvContractCompat.requestChannelBrowsable(context, channelId);
        } else {
            cr.update(TvContractCompat.buildChannelUri(channelId), channel.toContentValues(), null, null);
        }
        return channelId;
    }

    private static Single<PreviewProgram> buildProgram(Context context, SmartListViewModel vm, String launcherName, long channelId) {
        return new AvatarDrawable.Builder()
                .withViewModel(vm)
                .withPresence(false)
                .buildAsync(context)
                .map(avatar -> {
                    File file = AndroidFileUtils.createImageFile(context);
                    Bitmap bitmapAvatar = BitmapUtils.drawableToBitmap(avatar, 256);
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                        bitmapAvatar.compress(Bitmap.CompressFormat.PNG, 100, os);
                    }
                    bitmapAvatar.recycle();
                    Uri uri = FileProvider.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, file);

                    // Grant permission to launcher
                    if (launcherName != null)
                        context.grantUriPermission(launcherName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                    PreviewProgram.Builder contactBuilder = new PreviewProgram.Builder()
                            .setChannelId(channelId)
                            .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                            .setTitle(vm.getContactName())
                            .setAuthor(vm.getContacts().get(0).getRingUsername())
                            .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1)
                            .setPosterArtUri(uri)
                            .setIntentUri(new Uri.Builder()
                                    .scheme(ContentUriHandler.SCHEME_TV)
                                    .authority(ContentUriHandler.AUTHORITY)
                                    .appendPath(ContentUriHandler.PATH_TV_CONVERSATION)
                                    .appendPath(vm.getAccountId())
                                    .appendPath(vm.getUri().getUri())
                                    .build())
                            .setInternalProviderId(vm.getUuid());
                    return contactBuilder.build();
                });
    }

    private void buildHomeChannel(Context context, List<SmartListViewModel> contacts) {
        if (contacts.isEmpty())
            return;

        // Get launcher package name
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        String launcherName = resolveInfo == null ? null : resolveInfo.activityInfo.packageName;

        ContentResolver cr = context.getContentResolver();

        mHomeChannelDisposable.clear();
        mHomeChannelDisposable.add(Single.fromCallable(() -> createHomeChannel(context))
                .doOnEvent((channelId, error) -> {
                    if (error != null)  {
                        Log.w(TAG, "Error creating home channel", error);
                    } else {
                        cr.delete(TvContractCompat.buildPreviewProgramsUriForChannel(channelId), null, null);
                    }
                })
                .flatMapObservable(channelId -> Observable.fromIterable(contacts)
                        .concatMapEager(contact -> buildProgram(context, contact, launcherName, channelId)
                                .toObservable()
                                .subscribeOn(Schedulers.io()), 8, 1))
                .subscribeOn(Schedulers.io())
                .subscribe(program -> cr.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, program.toContentValues()),
                        e -> Log.w(TAG, "Error updating home channel", e)));
    }

    @Override
    public void showContactRequests(final List<SmartListViewModel> contacts) {
        CardListRow row = (CardListRow) mRowsAdapter.get(TRUST_REQUEST_ROW_POSITION);
        boolean isRowDisplayed = row != null && row == requestsRow;

        List<ContactCard> cards = new ArrayList<>(contacts.size());
        for (SmartListViewModel contact : contacts)
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
        Intent intent = new Intent(Intent.ACTION_CALL, ConversationPath.toUri(accountID, number), getActivity(), TVCallActivity.class);
        startActivity(intent, null);
    }

    static private BitmapDrawable prepareAccountQr(Context context, String accountId) {
        Log.w(TAG, "prepareAccountQr " + accountId);
        if (TextUtils.isEmpty(accountId))
            return null;
        int pad = 16;
        QRCodeUtils.QRCodeData qrCodeData = QRCodeUtils.encodeStringAsQRCodeData(accountId, 0X00000000, 0xFFFFFFFF);
        Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth() + 2 * pad, qrCodeData.getHeight() + 2 * pad, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), pad, pad, qrCodeData.getWidth(), qrCodeData.getHeight());
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
        mDisposable.add(VCardServiceImpl.Companion
                .loadProfile(context, account)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(profile -> {
                    if (profile.first != null && !profile.first.isEmpty()) {
                        mTitleView.setAlias(profile.first);
                        if (address != null) {
                            setTitle(address);
                        } else {
                            setTitle("");
                        }
                    } else {
                        mTitleView.setAlias(address);
                    }
                })
                .flatMap(p -> AvatarDrawable.load(context, account))
                .subscribe(a -> {
                    mTitleView.getSettingsButton().setVisibility(View.VISIBLE);
                    mTitleView.getLogoView().setVisibility(View.VISIBLE);
                    mTitleView.getLogoView().setImageDrawable(a);
                }));
        qrCard.setDrawable(prepareAccountQr(context, account.getUri()));
        AccountSettingsRow.getAdapter().notifyItemRangeChanged(QR_ITEM_POSITION, 1);
    }

    @Override
    public void showExportDialog(String pAccountID, boolean hasPassword) {
        GuidedStepSupportFragment wizard = TVAccountExport.createInstance(pAccountID, hasPassword);
        GuidedStepSupportFragment.add(getParentFragmentManager(), wizard, R.id.main_browse_fragment);
    }

    @Override
    public void showProfileEditing() {
        GuidedStepSupportFragment.add(getParentFragmentManager(), new TVProfileEditingFragment(), R.id.main_browse_fragment);
    }

    @Override
    public void showAccountShare() {
        Intent intent = new Intent(getActivity(), TVShareActivity.class);
        startActivity(intent);
    }

    @Override
    public void showSettings() {
        startActivity(new Intent(getActivity(), TVSettingsActivity.class));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ContactCard) {
                SmartListViewModel model = ((ContactCard) item).getModel();
                Bundle bundle = ConversationPath.toBundle(model.getAccountId(), model.getUri());

                if (row == requestsRow) {
                    bundle.putString("type", TVContactActivity.TYPE_CONTACT_REQUEST_INCOMING);
                }

                TVContactFragment mContactFragment = new TVContactFragment();
                mContactFragment.setArguments(bundle);
                getParentFragmentManager().beginTransaction()
                        .hide(MainFragment.this)
                        .add(R.id.fragment_container, mContactFragment, TVContactFragment.TAG)
                        .addToBackStack(TVContactFragment.TAG)
                        .commit();
            } else if (item instanceof IconCard) {
                IconCard card = (IconCard) item;
                switch (card.getType()) {
                    case ACCOUNT_ADD_DEVICE:
                        presenter.onExportClicked();
                        break;
                    case ACCOUNT_EDIT_PROFILE:
                        presenter.onEditProfileClicked();
                        break;
                    case ACCOUNT_SHARE_ACCOUNT:
                        ImageView view = ((CardView) itemViewHolder.view).getMainImageView();
                        Intent intent = new Intent(getActivity(), TVShareActivity.class);
                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, TVShareActivity.SHARED_ELEMENT_NAME).toBundle();
                        requireActivity().startActivity(intent, bundle);
                        break;
                    case ADD_CONTACT:
                        startActivity(new Intent(getActivity(), SearchActivity.class));
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
