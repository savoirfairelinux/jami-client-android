/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.client;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.AccountWizardActivity;
import cx.ring.application.JamiApplication;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.contacts.AvatarFactory;
import cx.ring.fragments.ConversationFragment;
import cx.ring.fragments.SIPAccountCreationFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.interfaces.Colorable;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.service.DRingService;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.settings.SettingsFragment;
import cx.ring.settings.VideoSettingsFragment;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class HomeActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener,
        Spinner.OnItemSelectedListener, Colorable {
    static final String TAG = HomeActivity.class.getSimpleName();

    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;
    public static final int REQUEST_CODE_PHOTO = 5;
    public static final int REQUEST_CODE_GALLERY = 6;
    public static final int REQUEST_CODE_QR_CONVERSATION = 7;
    public static final int REQUEST_PERMISSION_CAMERA = 113;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 114;

    public static final String HOME_TAG = "Home";
    public static final String CONTACT_REQUESTS_TAG = "Trust request";
    public static final String ACCOUNTS_TAG = "Accounts";
    public static final String ABOUT_TAG = "About";
    public static final String SETTINGS_TAG = "Prefs";
    public static final String VIDEO_SETTINGS_TAG = "VideoPrefs";
    public static final String ACTION_PRESENT_TRUST_REQUEST_FRAGMENT = BuildConfig.APPLICATION_ID + "presentTrustRequestFragment";

    protected Fragment fContent;
    protected ConversationFragment fConversation;

    private ToolbarSpinnerAdapter mAccountAdapter;
    private BackHandlerInterface mBottomSheetBackHandlerInterface;
    private BackHandlerInterface mAccountFragmentBackHandlerInterface;

    @Inject
    AccountService mAccountService;
    @Inject
    NotificationService mNotificationService;

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    @BindView(R.id.spinner_toolbar)
    Spinner mToolbarSpinner;

    @BindView(R.id.navigation_view)
    BottomNavigationView mBottomNavigationView;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    private boolean mIsMigrationDialogAlreadyShowed;
    private String mAccountWithPendingrequests = null;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final CompositeDisposable mAccountCheckDisposable = new CompositeDisposable();

    private boolean conversationSelected = false;

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisposable.add(mAccountCheckDisposable);

        JamiApplication.getInstance().startDaemon();
        FragmentManager fragmentManager = getSupportFragmentManager();

        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        // dependency injection
        JamiApplication.getInstance().getRingInjectionComponent().inject(this);

        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("");
        }

        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
        mBottomNavigationView.getMenu().getItem(1).setChecked(true);

        mToolbarSpinner.setOnItemSelectedListener(this);

        // if app opened from notification display trust request fragment when mService will connected
        Intent intent = getIntent();
        Bundle extra = intent.getExtras();
        String action = intent.getAction();
        if (ACTION_PRESENT_TRUST_REQUEST_FRAGMENT.equals(action)) {
            if (extra == null || extra.getString(ContactRequestsFragment.ACCOUNT_ID) == null) {
                return;
            }
            mAccountWithPendingrequests = extra.getString(ContactRequestsFragment.ACCOUNT_ID);
        } else if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleShareIntent(intent);
        }
        fContent = fragmentManager.findFragmentById(R.id.main_frame);
        if (fContent == null || Intent.ACTION_SEARCH.equals(action)) {
            fContent = new SmartListFragment();
            fragmentManager.beginTransaction().replace(R.id.main_frame, fContent, HOME_TAG).addToBackStack(HOME_TAG).commitAllowingStateLoss();
        } else if (fContent instanceof Refreshable) {
            fragmentManager.beginTransaction().replace(R.id.main_frame, fContent).addToBackStack(HOME_TAG).commitAllowingStateLoss();
            ((Refreshable) fContent).refresh();
        }
        if (mAccountWithPendingrequests != null) {
            presentTrustRequestFragment(mAccountWithPendingrequests);
            mAccountWithPendingrequests = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fContent = null;
        mBottomSheetBackHandlerInterface = null;
    }

    private void handleShareIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Bundle extra = intent.getExtras();
            if (extra != null) {
                if (ConversationPath.fromBundle(extra) != null) {
                    intent.setClass(this, ConversationActivity.class);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent);
        String action = intent.getAction();
        if (ACTION_PRESENT_TRUST_REQUEST_FRAGMENT.equals(action)) {
            Bundle extra = intent.getExtras();
            if (extra == null || extra.getString(ContactRequestsFragment.ACCOUNT_ID) == null) {
                return;
            }
            presentTrustRequestFragment(extra.getString(ContactRequestsFragment.ACCOUNT_ID));
            return;
        } else if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleShareIntent(intent);
            return;
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            if (fContent instanceof SmartListFragment) {
                ((SmartListFragment)fContent).handleIntent(intent);
            }
        }

        if (!DeviceUtils.isTablet(this) || !DRingService.ACTION_CONV_ACCEPT.equals(action)) {
            return;
        }

        if (fContent instanceof SmartListFragment) {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, intent.getStringExtra(ConversationFragment.KEY_CONTACT_RING_ID));
            startConversationTablet(bundle);
        }
    }

    private void showMigrationDialog() {
        if (mIsMigrationDialogAlreadyShowed) {
            return;
        }
        mIsMigrationDialogAlreadyShowed = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_migration_title_dialog)
                .setMessage(R.string.account_migration_message_dialog)
                .setIcon(R.drawable.baseline_warning_24)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> mBottomNavigationView.setSelectedItemId(R.id.navigation_settings))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void setToolbarState(int titleRes) {
        setToolbarState(getString(titleRes) , null);
    }

    public void setToolbarState(String title, String subtitle) {
        mToolbar.setLogo(null);
        mToolbar.setTitle(title);

        if (subtitle != null) {
            mToolbar.setSubtitle(subtitle);
        } else {
            mToolbar.setSubtitle(null);
        }
    }

    private void showProfileInfo() {
        mToolbarSpinner.setVisibility(View.VISIBLE);
        mToolbar.setTitle(null);
        mToolbar.setSubtitle(null);

        int targetSize = (int) (AvatarFactory.SIZE_AB * getResources().getDisplayMetrics().density);

        mDisposable.add(mAccountService
                .getCurrentAccountSubject()
                .switchMapSingle(account -> AvatarFactory.getBitmapAvatar(HomeActivity.this, account, targetSize)
                        .map(avatar -> new Pair<>(account, avatar)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    mToolbar.setLogo(new BitmapDrawable(getResources(), d.second));
                }, e -> Log.e(TAG, "Error loading avatar", e)));
    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        super.onResume();
        mAccountCheckDisposable.clear();
        mAccountCheckDisposable.add(mAccountService.getObservableAccountList()
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accounts -> {
                    if (accounts.isEmpty()) {
                        startActivity(new Intent(this, AccountWizardActivity.class));
                    }
                    for (Account account : accounts) {
                        if (account.needsMigration()) {
                            showMigrationDialog();
                            break;
                        }
                    }
                }));

        mDisposable.add(mAccountService.getProfileAccountList()
                .observeOn(mUiScheduler)
                .subscribe(accounts -> {
                    mAccountAdapter = new ToolbarSpinnerAdapter(HomeActivity.this, R.layout.item_toolbar_spinner, accounts);
                    mToolbarSpinner.setAdapter(mAccountAdapter);
                    showProfileInfo();
                }, e ->  cx.ring.utils.Log.e(TAG, "Error loading account list !", e)));

        mDisposable.add((mAccountService
                .getCurrentAccountSubject()
                .switchMap(Account::getUnreadPending)
                .subscribe(count -> setBadge(R.id.navigation_requests, count))));

        mDisposable.add((mAccountService
                .getCurrentAccountSubject()
                .switchMap(Account::getUnreadConversations)
                .subscribe(count -> setBadge(R.id.navigation_home, count))));
    }

    public void startConversationTablet(Bundle bundle) {
        fConversation = new ConversationFragment();
        fConversation.setArguments(bundle);

        if (!(fContent instanceof ContactRequestsFragment)) {
            mBottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }

        showTabletToolbar();

        conversationSelected = true;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.conversation_container, fConversation, ConversationFragment.class.getName())
                .commit();
    }

    private void presentTrustRequestFragment(String accountID) {
        Bundle bundle = new Bundle();
        bundle.putString(ContactRequestsFragment.ACCOUNT_ID, accountID);
        mNotificationService.cancelTrustRequestNotification(accountID);
        if (fContent instanceof ContactRequestsFragment) {
            ((ContactRequestsFragment) fContent).presentForAccount(bundle);
            return;
        }
        fContent = new ContactRequestsFragment();
        fContent.setArguments(bundle);
        mBottomNavigationView.getMenu().getItem(0).setChecked(true);
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                .addToBackStack(CONTACT_REQUESTS_TAG).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDisposable.clear();
    }

    @Override
    public void onBackPressed() {
        if (mAccountFragmentBackHandlerInterface != null && mAccountFragmentBackHandlerInterface.onBackPressed()) {
            return;
        }
        if (mBottomSheetBackHandlerInterface != null && mBottomSheetBackHandlerInterface.onBackPressed()) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        int fCount = fragmentManager.getBackStackEntryCount();
        if (fCount > 1) {
            fContent = fragmentManager.findFragmentById(R.id.main_frame);
            if (fContent instanceof ContactRequestsFragment) {
                conversationSelected = false;
            } else {
                conversationSelected = true;
            }
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fCount - 2);
            fContent = fragmentManager.findFragmentById(entry.getId());
            fragmentManager.popBackStack();
            if (fCount == 2) {
                mBottomNavigationView.getMenu().getItem(1).setChecked(true);
                mBottomNavigationView.setVisibility(View.VISIBLE);
                showProfileInfo();
                showToolbarSpinner();
                if (!conversationSelected) {
                    hideTabletToolbar();
                }
            } else {
                conversationSelected = false;
                hideTabletToolbar();
            }
            return;
        }
        finish();
    }

    private void popCustomBackStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
        fContent = fragmentManager.findFragmentById(entry.getId());
        int entryCount = fragmentManager.getBackStackEntryCount() - 1;
        for (int i = 0; i < entryCount; ++i) {
            fragmentManager.popBackStack();
        }
        hideTabletToolbar();
    }

    public void goToSettings() {
        if (fContent instanceof SettingsFragment) {
            return;
        }
        popCustomBackStack();
        hideToolbarSpinner();
        fContent = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, SETTINGS_TAG)
                .addToBackStack(SETTINGS_TAG).commit();
    }

    public void goToAbout() {
        if (fContent instanceof AboutFragment) {
            return;
        }
        popCustomBackStack();
        hideToolbarSpinner();
        fContent = new AboutFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, ABOUT_TAG)
                .addToBackStack(ABOUT_TAG).commit();
    }

    public void goToVideoSettings() {
        if (fContent instanceof VideoSettingsFragment) {
            return;
        }
        fContent = new VideoSettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, VIDEO_SETTINGS_TAG)
                .addToBackStack(VIDEO_SETTINGS_TAG).commit();
    }

    @Override
    public void setColor(int color) {
//        mToolbar.setBackground(new ColorDrawable(color));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Bundle bundle = new Bundle();

        switch (item.getItemId()) {
            case R.id.navigation_requests:
                bundle.putString(ContactRequestsFragment.ACCOUNT_ID, mAccountService.getCurrentAccount().getAccountID());
                if (fContent instanceof ContactRequestsFragment) {
                    ((ContactRequestsFragment) fContent).presentForAccount(bundle);
                    break;
                }
                popCustomBackStack();
                fContent = new ContactRequestsFragment();
                fContent.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                        .addToBackStack(CONTACT_REQUESTS_TAG).commit();
                conversationSelected = false;
                break;
            case R.id.navigation_home:
                if (fContent instanceof SmartListFragment) {
                    break;
                }
                popCustomBackStack();
                fContent = new SmartListFragment();
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, HOME_TAG)
                        .addToBackStack(HOME_TAG).commit();
                conversationSelected = false;
                showProfileInfo();
                showToolbarSpinner();
                break;
            case R.id.navigation_settings:
                Account account = mAccountService.getCurrentAccount();

                if (account.needsMigration()) {
                    Log.d(TAG, "launchAccountMigrationActivity: Launch account migration activity");

                    Intent intent = new Intent()
                            .setClass(this, AccountWizardActivity.class)
                            .setData(Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, account.getAccountID()));
                    startActivityForResult(intent, 1);
                } else {
                    Log.d(TAG, "launchAccountEditFragment: Launch account edit fragment");
                    bundle.putString(AccountEditionFragment.ACCOUNT_ID, account.getAccountID());

                    if (fContent instanceof AccountEditionFragment) {
                        break;
                    }
                    popCustomBackStack();
                    fContent = new AccountEditionFragment();
                    fContent.setArguments(bundle);
                    getSupportFragmentManager().beginTransaction()
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .replace(getFragmentContainerId(), fContent, ACCOUNTS_TAG)
                            .addToBackStack(ACCOUNTS_TAG).commit();
                    conversationSelected = false;
                    break;
                }

                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mAccountAdapter.getItemViewType(position) == ToolbarSpinnerAdapter.TYPE_ACCOUNT){
            mAccountService.setCurrentAccount(mAccountAdapter.getItem(position));
        } else {
            Intent intent = new Intent(HomeActivity.this, AccountWizardActivity.class);

            if (mAccountAdapter.getItemViewType(position) == ToolbarSpinnerAdapter.TYPE_CREATE_SIP) {
                intent.setAction(AccountConfig.ACCOUNT_TYPE_SIP);
            }
            startActivity(intent);

            Account account = mAccountService.getCurrentAccount();
            if (account != null) {
                mToolbarSpinner.setSelection(mAccountService.getAccountList().indexOf(account.getAccountID()));
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public interface Refreshable {
        void refresh();
    }

    public void setBadge(int menuId, int number) {
        if (number == 0) {
            mBottomNavigationView.removeBadge(menuId);
            return;
        }

        mBottomNavigationView.getOrCreateBadge(menuId);
        BadgeDrawable badgeDrawable = mBottomNavigationView.getBadge(menuId);
        if (badgeDrawable != null) {
            badgeDrawable.setNumber(number);
        }
    }

    private void hideTabletToolbar() {
        if (DeviceUtils.isTablet(this)) {
            TextView title = mToolbar.findViewById(R.id.contact_title);
            TextView subtitle = mToolbar.findViewById(R.id.contact_subtitle);
            ImageView logo = mToolbar.findViewById(R.id.contact_image);

            title.setText("");
            subtitle.setText("");
            logo.setImageDrawable(null);
        }
    }

    private void showTabletToolbar() {
        if (DeviceUtils.isTablet(this)) {
            mToolbar.findViewById(R.id.tablet_toolbar).setVisibility(View.VISIBLE);
        }
    }

    private void showToolbarSpinner() {
        mToolbarSpinner.setVisibility(View.VISIBLE);
    }

    private void hideToolbarSpinner() {
        if (!DeviceUtils.isTablet(HomeActivity.this)) {
            mToolbarSpinner.setVisibility(View.GONE);
        }
    }

    public boolean isConversationSelected(){
        return conversationSelected;
    }

    private int getFragmentContainerId() {
        if (DeviceUtils.isTablet(HomeActivity.this)) {
            return R.id.conversation_container;
        }

        return R.id.main_frame;
    }

    public void setBottomSheetOnBackPressedListener(BackHandlerInterface backPressedListener) {
        mBottomSheetBackHandlerInterface = backPressedListener;
    }

    public void setAccountFragmentOnBackPressedListener(BackHandlerInterface backPressedListener) {
        mAccountFragmentBackHandlerInterface = backPressedListener;
    }

}
