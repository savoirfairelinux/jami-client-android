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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.account.AccountWizardActivity;
import cx.ring.application.JamiApplication;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.interfaces.Colorable;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.navigation.HomeNavigationFragment;
import cx.ring.service.DRingService;
import cx.ring.services.AccountService;
import cx.ring.services.NotificationService;
import cx.ring.settings.SettingsFragment;
import cx.ring.settings.VideoSettingsFragment;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class HomeActivity extends AppCompatActivity implements HomeNavigationFragment.OnNavigationSectionSelected, Colorable {
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
    private static final String NAVIGATION_TAG = "Navigation";

    protected Fragment fContent;
    protected HomeNavigationFragment fNavigation;
    protected ConversationFragment fConversation;

    @Inject
    AccountService mAccountService;
    @Inject
    NotificationService mNotificationService;
    @BindView(R.id.drawer_layout)
    DrawerLayout mNavigationDrawer;

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.app_bar)
    AppBarLayout mAppBar;
    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mToolbarLayout;

    @BindView(R.id.action_button)
    FloatingActionButton actionButton;

    @BindView(R.id.content_frame)
    ViewGroup mFrameLayout;

    private boolean mIsMigrationDialogAlreadyShowed;
    private ActionBarDrawerToggle mDrawerToggle;
    private Boolean isDrawerLocked = false;
    private String mAccountWithPendingrequests = null;
    private float mToolbarSize;
    private float mToolbarElevation;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();
        mToolbarSize = getResources().getDimension(R.dimen.abc_action_bar_default_height_material);
        mToolbarElevation = getResources().getDimension(R.dimen.toolbar_elevation);
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (savedInstanceState != null) {
            fNavigation = (HomeNavigationFragment) fragmentManager.findFragmentByTag(NAVIGATION_TAG);
        }
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        // dependency injection
        JamiApplication.getInstance().getRingInjectionComponent().inject(this);

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
            fNavigation = new HomeNavigationFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_container, fNavigation, NAVIGATION_TAG)
                    .commit();
        }

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
    protected void onDestroy() {
        super.onDestroy();
        fContent = null;
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
        }
        if (!DeviceUtils.isTablet(this) || !DRingService.ACTION_CONV_ACCEPT.equals(action)) {
            return;
        }

        if (!getSupportFragmentManager().findFragmentByTag(HOME_TAG).isVisible()) {
            fNavigation.selectSection(HomeNavigationFragment.Section.HOME);
            onNavigationSectionSelected(HomeNavigationFragment.Section.HOME);
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> fNavigation.selectSection(HomeNavigationFragment.Section.MANAGE))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void setToolbarTop(boolean top) {
        if (top) {
            mAppBar.setElevation(0);
        } else {
            mAppBar.setElevation(mToolbarElevation);
        }
    }

    public void setToolbarState(boolean doubleHeight, int titleRes) {
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setIcon(null);
        mToolbar.setLogo(null);
        if (doubleHeight) {
            mAppBar.setElevation(mToolbarElevation);
            mToolbarLayout.setTitleEnabled(true);
            mToolbarLayout.setTitle(getString(titleRes));
        } else {
            mAppBar.setElevation(0);
            mToolbarLayout.setTitleEnabled(false);
            mToolbar.setTitle(getString(titleRes));
        }
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mAppBar.getLayoutParams();
        params.height = doubleHeight ? (int) (2.01 * mToolbarSize) : CoordinatorLayout.LayoutParams.WRAP_CONTENT;
        mAppBar.setLayoutParams(params);
        mAppBar.setExpanded(doubleHeight);
    }

    public FloatingActionButton getActionButton() {
        return actionButton;
    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        super.onResume();
        mDisposable.clear();
        mDisposable.add(mAccountService.getObservableAccountList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accounts -> {
                    for (Account account : accounts) {
                        if (account.needsMigration()) {
                            showMigrationDialog();
                            break;
                        }
                    }
                }));
    }

    public void startConversationTablet(Bundle bundle) {
        fConversation = new ConversationFragment();
        fConversation.setArguments(bundle);

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
        fNavigation.selectSection(HomeNavigationFragment.Section.CONTACT_REQUESTS);
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
        if (mNavigationDrawer.isDrawerVisible(GravityCompat.START) && !isDrawerLocked) {
            mNavigationDrawer.closeDrawer(GravityCompat.START);
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        int fCount = fragmentManager.getBackStackEntryCount();
        if (fCount > 1) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fCount - 2);
            fContent = fragmentManager.findFragmentById(entry.getId());
            fragmentManager.popBackStack();
            if (fCount == 2)
                fNavigation.selectSection(HomeNavigationFragment.Section.HOME);
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
    }

    public void onNavigationViewReady() {
        if (fNavigation != null) {
            fNavigation.setNavigationSectionSelectedListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationSectionSelected(HomeNavigationFragment.Section section) {
        if (!isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }

        switch (section) {
            case HOME:
                if (fContent instanceof SmartListFragment) {
                    break;
                }
                FragmentManager manager = getSupportFragmentManager();
                if (manager.getBackStackEntryCount() == 1) {
                    break;
                }

                popCustomBackStack();
                fContent = manager.findFragmentByTag(HOME_TAG);
                break;
            case CONTACT_REQUESTS:
                Bundle bundle = new Bundle();
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
                break;
            case MANAGE:
                if (fContent instanceof AccountsManagementFragment) {
                    break;
                }
                popCustomBackStack();
                fContent = new AccountsManagementFragment();
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, ACCOUNTS_TAG)
                        .addToBackStack(ACCOUNTS_TAG).commit();
                break;
            case ABOUT:
                if (fContent instanceof AboutFragment) {
                    break;
                }
                popCustomBackStack();
                fContent = new AboutFragment();
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.main_frame, fContent, ABOUT_TAG)
                        .addToBackStack(ABOUT_TAG).commit();
                break;
            case SETTINGS:
                this.goToSettings();
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


    public void goToSettings() {
        if (mNavigationDrawer != null && !isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
        if (fContent instanceof SettingsFragment) {
            return;
        }
        popCustomBackStack();
        fContent = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, SETTINGS_TAG)
                .addToBackStack(SETTINGS_TAG).commit();
    }

    public void goToVideoSettings() {
        if (mNavigationDrawer != null && !isDrawerLocked) {
            mNavigationDrawer.closeDrawers();
        }
        if (fContent instanceof VideoSettingsFragment) {
            return;
        }
        fContent = new VideoSettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, VIDEO_SETTINGS_TAG)
                .addToBackStack(VIDEO_SETTINGS_TAG).commit();
    }

    @Override
    public void setColor(int color) {
        //mToolbar.setBackground(new ColorDrawable(color));
    }

    public interface Refreshable {
        void refresh();
    }
}
