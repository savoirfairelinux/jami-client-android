/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.account.AccountEditionFragment;
import cx.ring.account.AccountWizardActivity;
import cx.ring.application.JamiApplication;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.contacts.AvatarFactory;
import cx.ring.databinding.ActivityHomeBinding;
import cx.ring.fragments.ConversationFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.interfaces.BackHandlerInterface;
import cx.ring.interfaces.Colorable;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.service.DRingService;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.NotificationService;
import cx.ring.settings.SettingsFragment;
import cx.ring.settings.VideoSettingsFragment;
import cx.ring.settings.pluginssettings.PluginDetails;
import cx.ring.settings.pluginssettings.PluginPathPreferenceFragment;
import cx.ring.settings.pluginssettings.PluginSettingsFragment;
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.StringUtils;
import cx.ring.views.SwitchButton;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

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

    private static final int NAVIGATION_CONTACT_REQUESTS = 0;
    private static final int NAVIGATION_CONVERSATIONS = 1;
    private static final int NAVIGATION_ACCOUNT = 2;

    public static final String HOME_TAG = "Home";
    public static final String CONTACT_REQUESTS_TAG = "Trust request";
    public static final String ACCOUNTS_TAG = "Accounts";
    public static final String ABOUT_TAG = "About";
    public static final String SETTINGS_TAG = "Prefs";
    public static final String VIDEO_SETTINGS_TAG = "VideoPrefs";

    public static final String ACTION_PRESENT_TRUST_REQUEST_FRAGMENT = BuildConfig.APPLICATION_ID + "presentTrustRequestFragment";

    public static final String PLUGINS_LIST_SETTINGS_TAG = "PluginsListSettings";
    public static final String PLUGIN_SETTINGS_TAG = "PluginSettings";
    public static final String PLUGIN_PATH_PREFERENCE_TAG = "PluginPathPreference";

    private static final String CONVERSATIONS_CATEGORY = "conversations";

    protected Fragment fContent;
    protected ConversationFragment fConversation;

    private AccountSpinnerAdapter mAccountAdapter;
    private BackHandlerInterface mAccountFragmentBackHandlerInterface;

    private ViewOutlineProvider mOutlineProvider;

    private int mOrientation;

    @Inject
    AccountService mAccountService;
    @Inject
    NotificationService mNotificationService;
    @Inject
    ContactService mContactService;

    private ActivityHomeBinding mBinding;

    private AlertDialog mMigrationDialog;
    private String mAccountWithPendingrequests = null;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private boolean conversationSelected = false;

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("orientation", mOrientation);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOrientation = savedInstanceState.getInt("orientation");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();

        mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // dependency injection
        JamiApplication.getInstance().getInjectionComponent().inject(this);

        setSupportActionBar(mBinding.mainToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("");
        }

        mBinding.navigationView.setOnNavigationItemSelectedListener(this);
        mBinding.navigationView.getMenu().getItem(NAVIGATION_CONVERSATIONS).setChecked(true);

        mOutlineProvider = mBinding.appBar.getOutlineProvider();

        if (!DeviceUtils.isTablet(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.bottom_navigation));
        }

        mBinding.spinnerToolbar.setOnItemSelectedListener(this);
        mBinding.accountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> enableAccount(isChecked));

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
        FragmentManager fragmentManager = getSupportFragmentManager();
        fContent = fragmentManager.findFragmentById(R.id.main_frame);
        if (fContent == null || Intent.ACTION_SEARCH.equals(action)) {
            fContent = new SmartListFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.main_frame, fContent, HOME_TAG)
                    .commitNow();
        } else if (fContent instanceof Refreshable) {
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
        if (mMigrationDialog != null) {
            if (mMigrationDialog.isShowing())
                mMigrationDialog.dismiss();
            mMigrationDialog = null;
        }
        fContent = null;
        mDisposable.dispose();
        mBinding = null;
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
        } else if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleShareIntent(intent);
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            if (fContent instanceof SmartListFragment) {
                ((SmartListFragment)fContent).handleIntent(intent);
            }
        } else if (DRingService.ACTION_CONV_ACCEPT.equals(action))  {
            if (DeviceUtils.isTablet(this)) {
                startConversationTablet(ConversationPath.fromIntent(intent).toBundle());
            }
        }
    }

    private void showMigrationDialog() {
        if (mMigrationDialog != null) {
            return;
        }
        mMigrationDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_migration_title_dialog)
                .setMessage(R.string.account_migration_message_dialog)
                .setIcon(R.drawable.baseline_warning_24)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> selectNavigationItem(R.id.navigation_settings))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void setToolbarState(@StringRes int titleRes) {
        setToolbarState(getString(titleRes) , null);
    }

    public void setToolbarState(String title, String subtitle) {
        mBinding.mainToolbar.setLogo(null);
        mBinding.mainToolbar.setTitle(title);
        mBinding.mainToolbar.setSubtitle(subtitle);
    }

    private void showProfileInfo() {
        mBinding.spinnerToolbar.setVisibility(View.VISIBLE);
        mBinding.mainToolbar.setTitle(null);
        mBinding.mainToolbar.setSubtitle(null);

        int targetSize = (int) (AvatarFactory.SIZE_AB * getResources().getDisplayMetrics().density);
        mDisposable.add(mAccountService.getCurrentAccountSubject()
                .switchMapSingle(account -> AvatarFactory.getBitmapAvatar(HomeActivity.this, account, targetSize)
                        .map(avatar -> new Pair<>(account, avatar)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> mBinding.mainToolbar.setLogo(new BitmapDrawable(getResources(), d.second)),
                        e -> Log.e(TAG, "Error loading avatar", e)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDisposable.add(mAccountService.getObservableAccountList()
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accounts -> {
                    if (mAccountAdapter == null) {
                        mAccountAdapter = new AccountSpinnerAdapter(HomeActivity.this, accounts);
                        mAccountAdapter.setNotifyOnChange(false);
                        mBinding.spinnerToolbar.setAdapter(mAccountAdapter);
                    } else {
                        mAccountAdapter.clear();
                        mAccountAdapter.addAll(accounts);
                        mAccountAdapter.notifyDataSetChanged();
                        if (accounts.size() > 0) {
                            mBinding.spinnerToolbar.setSelection(0);
                        }
                    }
                    if (fContent instanceof SmartListFragment) {
                        showProfileInfo();
                    }
                }, e ->  Log.e(TAG, "Error loading account list !", e)));

        mDisposable.add((mAccountService
                .getCurrentAccountSubject()
                .switchMap(Account::getUnreadPending)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> setBadge(R.id.navigation_requests, count))));

        mDisposable.add((mAccountService
                .getCurrentAccountSubject()
                .switchMap(Account::getUnreadConversations)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> setBadge(R.id.navigation_home, count))));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDisposable.add((mAccountService
                    .getCurrentAccountSubject()
                    .observeOn(Schedulers.io())
                    .map(account -> {
                        Collection<Conversation> conversations = account.getConversations();
                        synchronized (conversations) {
                            return new ArrayList<>(conversations);
                        }
                    })
                    .subscribe(this::initShareShortcuts, e -> Log.e(TAG, "Error generating conversation shortcuts", e))));
        }

        int newOrientation = getResources().getConfiguration().orientation;
        if (mOrientation != newOrientation) {
            mOrientation = newOrientation;
            hideTabletToolbar();
            if (DeviceUtils.isTablet(this)) {
                selectNavigationItem(R.id.navigation_home);
                showTabletToolbar();
                conversationSelected = true;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.clear();
    }

    public void startConversation(String conversationId) {
        mDisposable.add(mAccountService.getCurrentAccountSubject()
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(account -> startConversation(account.getAccountID(), new cx.ring.model.Uri(conversationId))));
    }
    public void startConversation(String accountId, cx.ring.model.Uri conversationId) {
        if (!DeviceUtils.isTablet(this)) {
            startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationId.toString()), this, ConversationActivity.class));
        } else {
            startConversationTablet(ConversationPath.toBundle(accountId, conversationId.toString()));
        }
    }

    public void startConversationTablet(Bundle bundle) {
        fConversation = new ConversationFragment();
        fConversation.setArguments(bundle);

        if (!(fContent instanceof ContactRequestsFragment)) {
            selectNavigationItem(R.id.navigation_home);
        }

        showTabletToolbar();

        conversationSelected = true;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.conversation_container, fConversation, ConversationFragment.class.getSimpleName())
                .commit();
    }

    private void presentTrustRequestFragment(String accountID) {
        mNotificationService.cancelTrustRequestNotification(accountID);
        if (fContent instanceof ContactRequestsFragment) {
            ((ContactRequestsFragment) fContent).presentForAccount(accountID);
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(ContactRequestsFragment.ACCOUNT_ID, accountID);
        fContent = new ContactRequestsFragment();
        fContent.setArguments(bundle);
        mBinding.navigationView.getMenu().getItem(NAVIGATION_CONTACT_REQUESTS).setChecked(true);
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                .addToBackStack(CONTACT_REQUESTS_TAG).commit();
    }

    @Override
    public void onBackPressed() {
        if (mAccountFragmentBackHandlerInterface != null && mAccountFragmentBackHandlerInterface.onBackPressed()) {
            return;
        }
        super.onBackPressed();
        fContent = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        if (fContent instanceof SmartListFragment) {
            mBinding.navigationView.getMenu().getItem(NAVIGATION_CONVERSATIONS).setChecked(true);
            showProfileInfo();
            showToolbarSpinner();
            hideTabletToolbar();
        }
    }

    private void popCustomBackStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int entryCount = fragmentManager.getBackStackEntryCount();
        for (int i = 0; i < entryCount; ++i) {
            fragmentManager.popBackStack();
        }
        fContent = fragmentManager.findFragmentById(R.id.main_frame);
        hideTabletToolbar();
        setToolbarElevation(false);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Account account = mAccountService.getCurrentAccount();
        if (account == null)
            return false;

        Bundle bundle = new Bundle();
        switch (item.getItemId()) {
            case R.id.navigation_requests:
                if (fContent instanceof ContactRequestsFragment) {
                    ((ContactRequestsFragment) fContent).presentForAccount(account.getAccountID());
                    break;
                }
                popCustomBackStack();
                fContent = new ContactRequestsFragment();
                bundle.putString(ContactRequestsFragment.ACCOUNT_ID, account.getAccountID());
                fContent.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                        .setReorderingAllowed(true)
                        .addToBackStack(CONTACT_REQUESTS_TAG)
                        .commit();
                conversationSelected = false;
                showProfileInfo();
                showToolbarSpinner();
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
                        .setReorderingAllowed(true)
                        .commit();
                conversationSelected = false;
                showProfileInfo();
                showToolbarSpinner();
                break;
            case R.id.navigation_settings:

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
                            .addToBackStack(ACCOUNTS_TAG)
                            .commit();
                    conversationSelected = false;
                    showProfileInfo();
                    showToolbarSpinner();
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
        int type = mAccountAdapter.getItemViewType(position);
        if (type == AccountSpinnerAdapter.TYPE_ACCOUNT) {
            Account account = mAccountAdapter.getItem(position);
            mAccountService.setCurrentAccount(account);
            showAccountStatus(fContent instanceof AccountEditionFragment && !account.isSip());
        } else {
            Intent intent = new Intent(HomeActivity.this, AccountWizardActivity.class);
            if (type == AccountSpinnerAdapter.TYPE_CREATE_SIP) {
                intent.setAction(AccountConfig.ACCOUNT_TYPE_SIP);
            }
            startActivity(intent);
            mBinding.spinnerToolbar.setSelection(mAccountService.getCurrentAccountIndex());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public interface Refreshable {
        void refresh();
    }

    public void setBadge(int menuId, int number) {
        if (number == 0)
            mBinding.navigationView.removeBadge(menuId);
        else
            mBinding.navigationView.getOrCreateBadge(menuId).setNumber(number);
    }

    private void hideTabletToolbar() {
        if (mBinding != null) {
            mBinding.contactTitle.setText(null);
            mBinding.contactSubtitle.setText(null);
            mBinding.contactImage.setImageDrawable(null);
            mBinding.tabletToolbar.setVisibility(View.GONE);
        }
    }

    private void showTabletToolbar() {
        if (mBinding != null && DeviceUtils.isTablet(this)) {
            mBinding.tabletToolbar.setVisibility(View.VISIBLE);
        }
    }

    public void setTabletTitle(@StringRes int titleRes) {
        mBinding.tabletToolbar.setVisibility(View.VISIBLE);
        mBinding.contactTitle.setText(titleRes);
        mBinding.contactTitle.setTextSize(19);
        mBinding.contactTitle.setTypeface(null, Typeface.BOLD);
        mBinding.contactImage.setVisibility(View.GONE);
        /*RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.contactTitle.getLayoutParams();
        params.removeRule(RelativeLayout.ALIGN_TOP);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        binding.contactTitle.setLayoutParams(params);*/
    }

    public void setToolbarTitle(@StringRes int titleRes) {
        if (DeviceUtils.isTablet(this)) {
            setTabletTitle(titleRes);
        } else {
            setToolbarState(titleRes);
        }
    }

    public void showAccountStatus(boolean show){
        mBinding.accountSwitch.setVisibility(show? View.VISIBLE : View.GONE);
    }

    private void showToolbarSpinner() {
        mBinding.spinnerToolbar.setVisibility(View.VISIBLE);
    }

    private void hideToolbarSpinner() {
        if (mBinding != null && !DeviceUtils.isTablet(this)) {
            mBinding.spinnerToolbar.setVisibility(View.GONE);
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

    public void setAccountFragmentOnBackPressedListener(BackHandlerInterface backPressedListener) {
        mAccountFragmentBackHandlerInterface = backPressedListener;
    }

    /**
     * Changes the current main fragment to a plugins list settings fragment
     */
    public void goToPluginsListSettings() {
        if (fContent instanceof PluginsListSettingsFragment) {
            return;
        }

        fContent = new PluginsListSettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, PLUGINS_LIST_SETTINGS_TAG)
                .addToBackStack(PLUGINS_LIST_SETTINGS_TAG).commit();
    }

    /**
     * Changes the current main fragment to a plugin settings fragment
     * @param pluginDetails
     */
    public void gotToPluginSettings(PluginDetails pluginDetails){
        if (fContent instanceof PluginSettingsFragment) {
            return;
        }
        fContent = PluginSettingsFragment.newInstance(pluginDetails);
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, PLUGIN_SETTINGS_TAG)
                .addToBackStack(PLUGIN_SETTINGS_TAG).commit();
    }

    /**
     * Changes the current main fragment to a plugin PATH preference fragment
     */
    public void gotToPluginPathPreference(PluginDetails pluginDetails, String preferenceKey){
        if (fContent instanceof PluginPathPreferenceFragment) {
            return;
        }
        fContent = PluginPathPreferenceFragment.newInstance(pluginDetails, preferenceKey);
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(getFragmentContainerId(), fContent, PLUGIN_PATH_PREFERENCE_TAG)
                .addToBackStack(PLUGIN_PATH_PREFERENCE_TAG).commit();
    }

    @Override
    public void setColor(int color) {
        //mToolbar.setBackground(new ColorDrawable(color));
    }

    public void setToolbarElevation(boolean enable) {
        if (mBinding != null)
            mBinding.appBar.setElevation(enable ? getResources().getDimension(R.dimen.toolbar_elevation) : 0);
    }

    public void setToolbarOutlineState(boolean enabled) {
        if (mBinding != null) {
            if (!enabled) {
                mBinding.appBar.setOutlineProvider(null);
            } else {
                mBinding.appBar.setOutlineProvider(mOutlineProvider);
            }
        }
	}
	
    public void popFragmentImmediate() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate();
        FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1);
        fContent = fm.findFragmentById(entry.getId());
    }

    public void selectNavigationItem(int id) {
        if (mBinding != null)
            mBinding.navigationView.setSelectedItemId(id);
    }

    public void enableAccount(boolean newValue) {
        Account account = mAccountService.getCurrentAccount();
        if (account == null) {
            Log.w(TAG, "account not found!");
            return;
        }

        account.setEnabled(newValue);
        mAccountService.setAccountEnabled(account.getAccountID(), newValue);
    }

    public SwitchButton getSwitchButton() {
        return mBinding.accountSwitch;
    }

    private void initShareShortcuts(Collection<Conversation> conversations) {
        List<CallContact> contactList = new ArrayList<>(conversations.size());
        String accountId = conversations.iterator().next().getAccountId();

        for (Conversation conversation : conversations) {
            contactList.add(conversation.getContact());
        }

        mDisposable.add(mContactService.getLoadedContact(accountId, contactList)
                .observeOn(Schedulers.io())
                .subscribe(contacts -> setShortcuts(conversations, contacts), e -> cx.ring.utils.Log.e(TAG, "Can't get contact", e)));
    }

    private void setShortcuts(Collection<Conversation> conversations, List<CallContact> contacts) {
        int targetSize = (int) (AvatarFactory.SIZE_NOTIF * getResources().getDisplayMetrics().density);
        int i = 0;
        int maxCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(this);
        if (maxCount == 0)
            maxCount = 4;

        List<Future<Bitmap>> futureIcons = new ArrayList<>(Math.min(conversations.size(),maxCount));
        for (Conversation conversation : conversations) {
            CallContact contact = conversation.getContact();
            futureIcons.add(AvatarFactory.getBitmapAvatar(this, contact, targetSize)
                    .subscribeOn(Schedulers.computation())
                    .toFuture());
            if (++i == maxCount) {
                break;
            }
        }
        List<ShortcutInfoCompat> shortcutInfoList = new ArrayList<>(futureIcons.size());

        i = 0;
        for (Conversation conversation : conversations) {
            IconCompat icon = null;
            try {
                icon = IconCompat.createWithBitmap(futureIcons.get(i).get());
            } catch (Exception e) {
                Log.w(TAG, "Failed to load icon", e);
            }

            Bundle bundle = ConversationPath.toBundle(conversation.getAccountId(), contacts.get(i).getPrimaryNumber());
            String key = ConversationPath.toKey(conversation.getAccountId(), contacts.get(i).getPrimaryNumber());

            Person person = new Person.Builder()
                    .setName(contacts.get(i).getDisplayName())
                    .setKey(key)
                    .build();

            ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(this, key)
                    .setShortLabel(contacts.get(i).getDisplayName())
                    .setPerson(person)
                    .setLongLived(true)
                    .setIcon(icon)
                    .setCategories(Collections.singleton(CONVERSATIONS_CATEGORY))
                    .setIntent(new Intent(Intent.ACTION_SEND, Uri.EMPTY, this, ShareActivity.class).putExtras(bundle))
                    .build();

            shortcutInfoList.add(shortcutInfo);
            if (++i == maxCount)
                break;
        }

        try {
            Log.d(TAG, "Adding shortcuts: " + shortcutInfoList.size());
            ShortcutManagerCompat.removeAllDynamicShortcuts(this);
            ShortcutManagerCompat.addDynamicShortcuts(this, shortcutInfoList);
        } catch (Exception e) {
            Log.w(TAG, "Error adding shortcuts", e);
        }
    }

}
