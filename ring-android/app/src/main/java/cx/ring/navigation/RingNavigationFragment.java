/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.navigation;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.Account;
import cx.ring.model.DaemonEvent;
import cx.ring.services.AccountService;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class RingNavigationFragment extends Fragment implements NavigationAdapter.OnNavigationItemClicked,
        AccountAdapter.OnAccountActionClicked, Observer<DaemonEvent> {
    private static final String TAG = RingNavigationFragment.class.getSimpleName();

    private AccountAdapter mAccountAdapter;

    @Inject
    AccountService mAccountService;

    /***************
     * Header views
     ***************/

    @BindView(R.id.account_selection)
    RelativeLayout mSelectedAccountLayout;

    @BindView(R.id.addaccount_btn)
    Button mNewAccountBtn;

    @BindView(R.id.user_photo)
    ImageView mUserImage;
    private Bitmap mSourcePhoto;
    private ImageView mProfilePhoto;
    private VCard mVCardProfile;

    @BindView(R.id.account_alias)
    TextView mSelectedAccountAlias;

    @BindView(R.id.account_host)
    TextView mSelectedAccountHost;

    @BindView(R.id.account_selected_error_indicator)
    AppCompatImageView mSelectedAccountError;

    @BindView(R.id.account_selected_arrow)
    AppCompatImageView mSelectedAccountArrow;

    /**************
     * Menu views
     **************/

    @BindView(R.id.drawer_menu)
    RecyclerView mMenuView;

    @BindView(R.id.drawer_accounts)
    RecyclerView mAccountsView;

    private NavigationAdapter mMenuAdapter;
    private OnNavigationSectionSelected mSectionListener;

    @Override
    public void onAccountSelected(Account selectedAccount) {

        toggleAccountList();
        // modify the state of the app
        // State observers will be notified
        mAccountService.setCurrentAccount(selectedAccount);

        //TODO: remove this when low level services are ready
        List<String> orderedAccountIdList = new ArrayList<>();
        String selectedID = selectedAccount.getAccountID();
        orderedAccountIdList.add(selectedID);
        for (Account account : mAccountAdapter.getAccounts()) {
            if (account.getAccountID().contentEquals(selectedID)) {
                continue;
            }
            orderedAccountIdList.add(account.getAccountID());
        }

        mAccountService.setAccountOrder(orderedAccountIdList);

        if (mSectionListener != null) {
            mSectionListener.onAccountSelected();
        }
    }

    @Override
    public void onAddRINGAccountSelected() {
        toggleAccountList();
        if (mSectionListener != null) {
            mSectionListener.onAddRingAccountSelected();
        }
    }

    @Override
    public void onAddSIPAccountSelected() {
        toggleAccountList();
        if (mSectionListener != null) {
            mSectionListener.onAddSipAccountSelected();
        }
    }

    @Override
    public void update(Observable o, DaemonEvent arg) {

        if (o instanceof AccountService && arg != null && arg.getEventType() == DaemonEvent.EventType.ACCOUNTS_CHANGED) {

            RingApplication.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAccounts(mAccountService.getAccounts());
                    updateUserView();
                    updateSelectedAccountView();
                }
            });
        }
    }

    public interface OnNavigationSectionSelected {
        void onNavigationSectionSelected(Section position);

        void onAccountSelected();

        void onAddRingAccountSelected();

        void onAddSipAccountSelected();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
        updateUserView();
        updateSelectedAccountView();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    /**
     * Exposed enumeration listing app sections
     */
    public enum Section {
        HOME(0),
        MANAGE(1),
        SETTINGS(2),
        SHARE(3),
        ABOUT(4);

        int position;

        Section(int pos) {
            position = pos;
        }

        public static Section valueOf(int sectionInt) {
            for (Section section : Section.values()) {
                if (section.position == sectionInt) {
                    return section;
                }
            }
            return HOME;
        }
    }

    /**
     * Internal class describing navigation sections
     */
    class NavigationItem {
        int mResTitleId;
        int mResImageId;

        NavigationItem(int resTitle, int resId) {
            mResTitleId = resTitle;
            mResImageId = resId;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View inflatedView = inflater.inflate(R.layout.frag_navigation, container, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        updateUserView();

        setupNavigationMenu();
        setupAccountList();

        return inflatedView;
    }

    private void setupAccountList() {
        mAccountAdapter = new AccountAdapter(new ArrayList<Account>(), getActivity());
        mAccountsView.setHasFixedSize(true);
        mAccountAdapter.setOnAccountActionClickedListener(this);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mAccountsView.setLayoutManager(mLayoutManager);
        mAccountsView.setAdapter(mAccountAdapter);
        mAccountsView.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: Remove this when low level services are ready
        ((HomeActivity) getActivity()).onNavigationViewReady();
    }

    /**
     * Can be called to reset the UI to the initial state, displaying the navigation items
     */
    public void displayNavigation() {
        mMenuView.setVisibility(View.VISIBLE);
        mAccountsView.setVisibility(View.GONE);
        mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
    }

    public void selectSection(Section manage) {
        mMenuAdapter.setSelection(manage.position);
    }

    @OnClick(R.id.account_selection)
    public void toggleAccountList() {
        boolean navigationIsDisplaying = mMenuView.getVisibility() == View.VISIBLE;
        if (navigationIsDisplaying) {
            mMenuView.setVisibility(View.GONE);
            mAccountsView.setVisibility(View.VISIBLE);
            mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp);
        } else {
            mMenuView.setVisibility(View.VISIBLE);
            mAccountsView.setVisibility(View.GONE);
            mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
        }
    }

    @Override
    public void onNavigationItemClicked(int position) {
        if (mSectionListener != null) {
            mSectionListener.onNavigationSectionSelected(Section.valueOf(position));
        }
    }

    public void updateUserView() {
        Log.d(TAG, "updateUserView");
        String accountID = mAccountService.getCurrentAccount() != null ? mAccountService.getCurrentAccount().getAccountID() : null;
        mVCardProfile = VCardUtils.loadLocalProfileFromDisk(getActivity().getFilesDir(), accountID, getString(R.string.unknown));
        if (!mVCardProfile.getPhotos().isEmpty()) {
            Photo tmp = mVCardProfile.getPhotos().get(0);
            mUserImage.setImageBitmap(BitmapUtils.cropImageToCircle(tmp.getData()));
        } else {
            mUserImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null));
        }
        mSelectedAccountAlias.setText(mVCardProfile.getFormattedName().getValue());
        Log.d(TAG, "User did change, updating user view.");
    }

    public void updatePhoto(Uri uriImage) {
        Bitmap imageProfile = ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage);
        updatePhoto(imageProfile);
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        mProfilePhoto.setImageBitmap(BitmapUtils.cropImageToCircle(image));
    }

    @OnClick(R.id.addaccount_btn)
    public void addNewAccount(View sender) {
        getActivity().startActivity(new Intent(getActivity(), AccountWizard.class));
    }

    private void setupNavigationMenu() {
        mMenuView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(getActivity());
        mMenuView.setLayoutManager(mLayoutManager2);

        ArrayList<NavigationItem> menu = new ArrayList<>();
        menu.add(0, new NavigationItem(R.string.menu_item_home, R.drawable.ic_home_black));
        menu.add(1, new NavigationItem(R.string.menu_item_accounts, R.drawable.ic_group_black));
        menu.add(2, new NavigationItem(R.string.menu_item_settings, R.drawable.ic_settings_black));
        menu.add(3, new NavigationItem(R.string.menu_item_share, R.drawable.ic_share_black));
        menu.add(4, new NavigationItem(R.string.menu_item_about, R.drawable.ic_info_black));

        mMenuAdapter = new NavigationAdapter(menu);
        mMenuView.setAdapter(mMenuAdapter);
        mMenuAdapter.setOnNavigationItemClickedListener(this);
    }

    public void setNavigationSectionSelectedListener(OnNavigationSectionSelected listener) {
        mSectionListener = listener;
    }

    @OnClick(R.id.profile_container)
    public void profileContainerClicked() {
        Log.d(TAG, "Click on the edit profile");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = (EditText) view.findViewById(R.id.user_name);
        editText.setText(mSelectedAccountAlias.getText());
        mProfilePhoto = (ImageView) view.findViewById(R.id.profile_photo);
        mProfilePhoto.setImageDrawable(mUserImage.getDrawable());

        ImageButton cameraView = (ImageButton) view.findViewById(R.id.camera);
        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (hasPermission) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            HomeActivity.REQUEST_PERMISSION_CAMERA);
                }
            }
        });

        ImageButton gallery = (ImageButton) view.findViewById(R.id.gallery);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (hasPermission) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            HomeActivity.REQUEST_PERMISSION_READ_STORAGE);
                }
            }
        });

        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = editText.getText().toString().trim();
                if (username.isEmpty()) {
                    username = getActivity().getString(R.string.unknown);
                }
                mVCardProfile.setFormattedName(new FormattedName(username));

                if (mSourcePhoto != null && mProfilePhoto.getDrawable() != ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null)) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    mSourcePhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Photo photo = new Photo(stream.toByteArray(), ImageType.PNG);
                    mVCardProfile.removeProperties(Photo.class);
                    mVCardProfile.addPhoto(photo);
                }

                mVCardProfile.removeProperties(RawProperty.class);
                VCardUtils.saveLocalProfileToDisk(mVCardProfile, mAccountService.getCurrentAccount().getAccountID(), getActivity().getFilesDir());
                updateUserView();
                updateAccounts(mAccountService.getAccounts());
            }
        });

        builder.show();
    }

    public void updateAccounts(List<Account> accounts) {
        mAccountAdapter.replaceAll(accounts);
        if (accounts.isEmpty()) {
            mNewAccountBtn.setVisibility(View.VISIBLE);
            mSelectedAccountLayout.setVisibility(View.GONE);

            // modify the state of the app
            // State observers will be notified
            mAccountService.setCurrentAccount(null);
        } else {
            mNewAccountBtn.setVisibility(View.GONE);
            mSelectedAccountLayout.setVisibility(View.VISIBLE);
            Account selected = accounts.get(0);
            mAccountService.setCurrentAccount(selected);
        }
    }

    private void updateSelectedAccountView() {
        Account selectedAccount = mAccountService.getCurrentAccount();
        if (selectedAccount == null) {
            return;
        }
        mSelectedAccountAlias.setText(mVCardProfile.getFormattedName().getValue());
        if (selectedAccount.isRing()) {
            String username = selectedAccount.getRegisteredName();
            if (!selectedAccount.registeringUsername && !TextUtils.isEmpty(username)) {
                mSelectedAccountHost.setText(username);
            } else {
                mSelectedAccountHost.setText(selectedAccount.getUsername());
            }
        } else if (selectedAccount.isSip() && !selectedAccount.isIP2IP()) {
            mSelectedAccountHost.setText(selectedAccount.getUsername() + "@" + selectedAccount.getHost());
        } else {
            mSelectedAccountHost.setText(R.string.account_type_ip2ip);
        }
        mSelectedAccountError.setVisibility(selectedAccount.isRegistered() ? View.GONE : View.VISIBLE);
    }
}
