/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.account.AccountWizardActivity;
import cx.ring.application.RingApplication;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ServiceEvent;
import cx.ring.navigation.RingNavigationFragment;
import cx.ring.service.DRingService;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.settings.SettingsFragment;
import cx.ring.share.ShareFragment;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class HomeActivity extends AppCompatActivity implements RingNavigationFragment.OnNavigationSectionSelected,
        Observer<ServiceEvent> {

    public static final int REQUEST_CODE_CREATE_ACCOUNT = 7;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;
    public static final int REQUEST_CODE_PHOTO = 5;
    public static final int REQUEST_CODE_GALLERY = 6;
    public static final int REQUEST_PERMISSION_CAMERA = 113;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 114;
    public static final String HOME_TAG = "Home";
    public static final String CONTACT_REQUESTS_TAG = "Trust request";
    public static final String ACCOUNTS_TAG = "Accounts";
    public static final String ABOUT_TAG = "About";
    public static final String SETTINGS_TAG = "Prefs";
    public static final String SHARE_TAG = "Share";
    static public final String ACTION_PRESENT_TRUST_REQUEST_FRAGMENT = BuildConfig.APPLICATION_ID + "presentTrustRequestFragment";
    static final String TAG = HomeActivity.class.getSimpleName();
    private static final String NAVIGATION_TAG = "Navigation";
    private final Handler mHandler = new Handler();
    protected android.app.Fragment fContent;
    protected RingNavigationFragment fNavigation;
    protected ConversationFragment fConversation;
    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    PreferencesService mPreferencesService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    AccountService mAccountService;
    private final Runnable mConnectivityChecker = new Runnable() {
        @Override
        public void run() {
            mAccountService.setAccountsActive(mPreferencesService.hasNetworkConnected());
        }
    };
    @Inject
    NotificationService mNotificationService;
    @BindView(R.id.left_drawer)
    NavigationView mNavigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout mNavigationDrawer;
    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.toolbar_spacer)
    LinearLayout mToolbarSpacerView;
    @BindView(R.id.toolbar_spacer_title)
    TextView mToolbarSpacerTitle;
    @BindView(R.id.action_button)
    FloatingActionButton actionButton;
    @BindView(R.id.content_frame)
    RelativeLayout mFrameLayout;
    private boolean mIsMigrationDialogAlreadyShowed;
    private ActionBarDrawerToggle mDrawerToggle;
    private Boolean isDrawerLocked = false;
    private String mAccountWithPendingrequests = null;
    private float mToolbarSize;

    @Override
    public void update(Observable o, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
                for (Account account : mAccountService.getAccounts()) {
                    if (account.needsMigration()) {
                        showMigrationDialog();
                    }
                }
                break;
            default:
                break;
        }

    }

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RingApplication.getInstance().startDaemon();

        mToolbarSize = getResources().getDimension(R.dimen.abc_action_bar_default_height_material);

        if (savedInstanceState != null) {
            fNavigation = (RingNavigationFragment) getFragmentManager().findFragmentByTag(NAVIGATION_TAG);
        }
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        // dependency injection
        RingApplication.getInstance().getRingInjectionComponent().inject(this);

        setSupportActionBar(mToolbar);

        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mNavigationDrawer, /* DrawerLayout object */
                //  R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
                if (fNavigation != null) {
                    fNavigation.displayNavigation();
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };

        if (mFrameLayout.getPaddingLeft() == (int) getResources().getDimension(R.dimen.drawer_size)) {
            mNavigationDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            mNavigationDrawer.setScrimColor(Color.TRANSPARENT);
            isDrawerLocked = true;
        }

        if (!isDrawerLocked) {
            mNavigationDrawer.addDrawerListener(mDrawerToggle);
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.setDisplayHomeAsUpEnabled(true);
                supportActionBar.setHomeButtonEnabled(true);
            }
        }

        if (fNavigation == null && savedInstanceState == null) {
            fNavigation = new RingNavigationFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, fNavigation, NAVIGATION_TAG)
                    .commit();
        }

        // if app opened from notification display trust request fragment when mService will connected
        Intent intent = getIntent();
        Bundle extra = intent.getExtras();
        if (ACTION_PRESENT_TRUST_REQUEST_FRAGMENT.equals(intent.getAction())) {
            if (extra == null || extra.getString(ContactRequestsFragment.ACCOUNT_ID) == null) {
                return;
            }
            mAccountWithPendingrequests = extra.getString(ContactRequestsFragment.ACCOUNT_ID);
        }


        setVideoEnabledFromPermission();

        FragmentManager fragmentManager = getFragmentManager();
        fContent = fragmentManager.findFragmentById(R.id.main_frame);
        if (fNavigation != null) {
            onNavigationViewReady();
        }
        if (fContent == null) {
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent);
        if (ACTION_PRESENT_TRUST_REQUEST_FRAGMENT.equals(intent.getAction())) {
            Bundle extra = intent.getExtras();
            if (extra == null || extra.getString(ContactRequestsFragment.ACCOUNT_ID) == null) {
                return;
            }
            presentTrustRequestFragment(extra.getString(ContactRequestsFragment.ACCOUNT_ID));
            return;
        }
        if (!ConversationFragment.isTabletMode(this) || !DRingService.ACTION_CONV_ACCEPT.equals(intent.getAction())) {
            return;
        }

        if (!getFragmentManager().findFragmentByTag(HOME_TAG).isVisible()) {
            fNavigation.selectSection(RingNavigationFragment.Section.HOME);
            onNavigationSectionSelected(RingNavigationFragment.Section.HOME);
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

        new AlertDialog.Builder(HomeActivity.this)
                .setTitle(R.string.account_migration_title_dialog)
                .setMessage(R.string.account_migration_message_dialog)
                .setIcon(R.drawable.ic_warning)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    fNavigation.selectSection(RingNavigationFragment.Section.MANAGE);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setOnCancelListener(DialogInterface::dismiss)
                .setOnDismissListener(DialogInterface::dismiss)
                .show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        String path = FileUtils.ringtonesPath(this);
        if (!(new File(path + "/default.wav")).exists()) {
            Log.d(TAG, "onStart: default.wav doesn't exist. Copying ringtones.");
            FileUtils.copyAssetFolder(getAssets(), "ringtones", path);
        }

        super.onStart();
    }

    public void setToolbarState(boolean doubleHeight, int titleRes) {

        mToolbar.setMinimumHeight((int) mToolbarSize);
        ViewGroup.LayoutParams toolbarSpacerViewParams = mToolbarSpacerView.getLayoutParams();

        if (doubleHeight) {
            // setting the height of the toolbar spacer with the same height than the toolbar
            toolbarSpacerViewParams.height = (int) mToolbarSize;
            mToolbarSpacerView.setLayoutParams(toolbarSpacerViewParams);

            // setting the toolbar spacer title (hiding the real toolbar title)
            mToolbarSpacerTitle.setText(titleRes);
            mToolbar.setTitle("");

            // the spacer and the action button become visible
            mToolbarSpacerView.setVisibility(View.VISIBLE);
            actionButton.setVisibility(View.VISIBLE);
        } else {
            // hide the toolbar spacer and the action button
            mToolbarSpacerView.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);
            mToolbar.setTitle(titleRes);
        }
    }

    public FloatingActionButton getActionButton() {
        return actionButton;
    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
        mConnectivityChecker.run();
        mHandler.postDelayed(mConnectivityChecker, 100);
        setVideoEnabledFromPermission();
    }

    public void startConversationTablet(Bundle bundle) {
        fConversation = new ConversationFragment();
        fConversation.setArguments(bundle);

        getFragmentManager().beginTransaction()
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
        fNavigation.selectSection(RingNavigationFragment.Section.CONTACT_REQUESTS);
        getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                .addToBackStack(CONTACT_REQUESTS_TAG).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawer.isDrawerVisible(GravityCompat.START) && !isDrawerLocked) {
            mNavigationDrawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            popCustomBackStack();
            fNavigation.selectSection(RingNavigationFragment.Section.HOME);
            return;
        }

        finish();
    }

    private void popCustomBackStack() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
        fContent = fragmentManager.findFragmentByTag(entry.getName());
        for (int i = 0; i < fragmentManager.getBackStackEntryCount() - 1; ++i) {
            fragmentManager.popBackStack();
        }
    }

    // TODO: Remove this when low level services are ready
    public void onNavigationViewReady() {
        if (fNavigation != null) {
            fNavigation.setNavigationSectionSelectedListener(HomeActivity.this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_CALL:
                if (resultCode == CallActivity.RESULT_FAILURE) {
                    Log.w(TAG, "Call Failed");
                }
                break;
            case REQUEST_CODE_PHOTO:
                if (resultCode == RESULT_OK && data != null) {
                    fNavigation.updatePhoto((Bitmap) data.getExtras().get("data"));
                }
                break;
            case REQUEST_CODE_GALLERY:
                if (resultCode == RESULT_OK && data != null) {
                    fNavigation.updatePhoto(data.getData());
                }
                break;
        }
    }

    @Override
    public void onNavigationSectionSelected(RingNavigationFragment.Section section) {
        if (!isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }

        switch (section) {
            case HOME:
                if (fContent instanceof SmartListFragment) {
                    break;
                }
                if (getFragmentManager().getBackStackEntryCount() == 1) {
                    break;
                }

                popCustomBackStack();
                fContent = getFragmentManager().findFragmentByTag(HOME_TAG);
                break;
            case CONTACT_REQUESTS:
                Bundle bundle = new Bundle();
                bundle.putString(ContactRequestsFragment.ACCOUNT_ID, null);
                if (fContent instanceof ContactRequestsFragment) {
                    ((ContactRequestsFragment) fContent).presentForAccount(bundle);
                    break;
                }
                fContent = new ContactRequestsFragment();
                fContent.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, CONTACT_REQUESTS_TAG)
                        .addToBackStack(CONTACT_REQUESTS_TAG).commit();
                break;
            case MANAGE:
                if (fContent instanceof AccountsManagementFragment) {
                    break;
                }
                fContent = new AccountsManagementFragment();
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, ACCOUNTS_TAG)
                        .addToBackStack(ACCOUNTS_TAG).commit();
                break;
            case ABOUT:
                if (fContent instanceof AboutFragment) {
                    break;
                }
                fContent = new AboutFragment();
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, ABOUT_TAG)
                        .addToBackStack(ABOUT_TAG).commit();
                break;
            case SETTINGS:
                this.goToSettings();
                break;
            case SHARE:
                goToShare();
                break;
            default:
                break;
        }
    }

    public void onAccountSelected() {
        if (!isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
    }

    @Override
    public void onAddSipAccountSelected() {
        if (!isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
        Intent intent = new Intent(HomeActivity.this, AccountWizardActivity.class);
        intent.setAction(AccountConfig.ACCOUNT_TYPE_SIP);
        startActivityForResult(intent, AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
    }

    @Override
    public void onAddRingAccountSelected() {
        if (!isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
        Intent intent = new Intent(HomeActivity.this, AccountWizardActivity.class);
        intent.setAction(AccountConfig.ACCOUNT_TYPE_RING);
        startActivityForResult(intent, AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
    }

    private void goToShare() {
        if (fContent instanceof ShareFragment) {
            return;
        }
        fContent = new ShareFragment();

        getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, SHARE_TAG)
                .addToBackStack(SHARE_TAG).commit();
    }

    public void goToSettings() {
        if (mNavigationDrawer != null && !isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
        if (fContent instanceof SettingsFragment) {
            return;
        }
        fContent = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, SETTINGS_TAG)
                .addToBackStack(SETTINGS_TAG).commit();
    }

    private void setVideoEnabledFromPermission() {
        //~ Setting correct VIDEO_ENABLED value based on the state of the
        //~ permission. It can handle the case where the user decides to remove a permission from
        //~ the Android general settings.
        if (!mDeviceRuntimeService.hasVideoPermission()) {
            mAccountService.setAccountsVideoEnabled(false);
        }
    }

    public interface Refreshable {
        void refresh();
    }
}
