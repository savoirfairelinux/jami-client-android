/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.application.RingApplication;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.ConfigKey;
import cx.ring.model.Phone;
import cx.ring.navigation.RingNavigationFragment;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.settings.SettingsFragment;
import cx.ring.share.ShareFragment;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.FileUtils;

public class HomeActivity extends AppCompatActivity implements LocalService.Callbacks,
        RingNavigationFragment.OnNavigationSectionSelected,
        ActivityCompat.OnRequestPermissionsResultCallback {

    static final String TAG = HomeActivity.class.getSimpleName();

    public static final int REQUEST_CODE_PREFERENCES = 1;
    public static final int REQUEST_CODE_CREATE_ACCOUNT = 7;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;

    public static final int REQUEST_CODE_PHOTO = 5;
    public static final int REQUEST_CODE_GALLERY = 6;
    public static final int REQUEST_PERMISSION_CAMERA = 113;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 114;

    private static final String HOME_TAG = "Home";
    private static final String ACCOUNTS_TAG = "Accounts";
    private static final String ABOUT_TAG = "About";
    private static final String SETTINGS_TAG = "Prefs";
    private static final String SHARE_TAG = "Share";


    private LocalService service;
    private boolean mBound = false;
    private boolean mNoAccountOpened = false;
    private boolean mIsMigrationDialogAlreadyShowed;

    private ActionBarDrawerToggle mDrawerToggle;

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

    private float mToolbarSize;
    protected android.app.Fragment fContent;
    protected RingNavigationFragment fNavigation;

    public interface Refreshable {
        void refresh();
    }

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        mToolbarSize = getResources().getDimension(R.dimen.abc_action_bar_default_height_material);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
                if (mNavigationView != null) {
                    fNavigation.updateUserView();
                }
            }
        };

        mNavigationDrawer.addDrawerListener(mDrawerToggle);

        // Bind to LocalService
        String[] toRequest = LocalService.checkRequiredPermissions(this);
        ArrayList<String> permissionsWeCanAsk = new ArrayList<>();

        for (String permission : toRequest) {
            if (((RingApplication) getApplication()).canAskForPermission(permission)) {
                permissionsWeCanAsk.add(permission);
            }
        }

        if (!permissionsWeCanAsk.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsWeCanAsk.toArray(new String[permissionsWeCanAsk.size()]), LocalService.PERMISSIONS_REQUEST);
        } else if (!mBound) {
            Log.d(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            startService(intent);
            bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction());
            switch (intent.getAction()) {
                case LocalService.ACTION_ACCOUNT_UPDATE:

                    for (Account account : service.getAccounts()) {
                        if (account.needsMigration()) {
                            showMigrationDialog();
                        }
                    }

                    if (!mNoAccountOpened && service.getAccounts().isEmpty()) {
                        mNoAccountOpened = true;
                        startActivityForResult(new Intent(HomeActivity.this, AccountWizard.class), AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
                    } else {
                        fNavigation.updateAccounts(service.getAccounts());
                    }
                    break;
            }
        }
    };

    private void showMigrationDialog() {

        if (mIsMigrationDialogAlreadyShowed) {
            return;
        }

        mIsMigrationDialogAlreadyShowed = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                .setTitle(R.string.account_migration_title_dialog)
                .setMessage(R.string.account_migration_message_dialog)
                .setIcon(R.drawable.ic_warning)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        fNavigation.selectSection(RingNavigationFragment.Section.MANAGE);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    dialog.dismiss();
                }
            });
        }
        builder.show();
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
        Log.d(TAG, "onStart");

        String path = FileUtils.ringtonesPath(this);
        if (!(new File(path + "/default.wav")).exists()) {
            Log.d(TAG, "default.wav doesn't exist. Copying ringtones.");
            FileUtils.copyAssetFolder(getAssets(), "ringtones", path);
        }

        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case LocalService.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                for (int i = 0, n = permissions.length; i < n; i++) {
                    String permission = permissions[i];
                    ((RingApplication) getApplication()).permissionHasBeenAsked(permission);
                    switch (permission) {
                        case Manifest.permission.RECORD_AUDIO:
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "Missing required permission RECORD_AUDIO");
                                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                        .setTitle(R.string.start_error_title)
                                        .setMessage(R.string.start_error_mic_required)
                                        .setIcon(R.drawable.ic_mic_black)
                                        .setCancelable(false)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                finish();
                                            }
                                        });
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            finish();
                                        }
                                    });
                                }
                                builder.show();
                                return;
                            }
                            break;
                        case Manifest.permission.READ_CONTACTS:
                            sharedPref.edit().putBoolean(getString(R.string.pref_systemContacts_key), grantResults[i] == PackageManager.PERMISSION_GRANTED).apply();
                            break;
                        case Manifest.permission.CAMERA:
                            sharedPref.edit().putBoolean(getString(R.string.pref_systemCamera_key), grantResults[i] == PackageManager.PERMISSION_GRANTED).apply();
                            break;
                    }
                }
                if (!mBound) {
                    Intent intent = new Intent(this, LocalService.class);
                    startService(intent);
                    bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
                }
                break;
            }
            case REQUEST_PERMISSION_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_GALLERY);
                } else {
                    return;
                }
                break;
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CODE_PHOTO);
                } else {
                    return;
                }
                break;
        }
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
        setVideoEnabledFromPermission();
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawer.isDrawerVisible(GravityCompat.START)) {
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

    /* activity finishes itself or is being killed by the system */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unregisterReceiver(receiver);
            unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            Log.d(TAG, "onServiceConnected " + className.getClassName());
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();

            setVideoEnabledFromPermission();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
            intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
            registerReceiver(receiver, intentFilter);
            mBound = true;

            if (fNavigation == null) {
                fNavigation = new RingNavigationFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.navigation_container, fNavigation, null)
                        .commit();
            }

            FragmentManager fragmentManager = getFragmentManager();
            fContent = fragmentManager.findFragmentById(R.id.main_frame);
            if (fContent == null) {
                fContent = new SmartListFragment();
                fragmentManager.beginTransaction().replace(R.id.main_frame, fContent, HOME_TAG).addToBackStack(HOME_TAG).commit();
            } else if (fContent instanceof Refreshable) {
                fragmentManager.beginTransaction().replace(R.id.main_frame, fContent).addToBackStack(HOME_TAG).commit();
                ((Refreshable) fContent).refresh();
            }
            service.reloadAccounts();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected " + className.getClassName());
            if (fNavigation != null) {
                fNavigation = null;
            }
            mBound = false;
        }
    };

    // TODO: Remove this when low level services are ready
    public void onNavigationViewReady() {
        if (fNavigation != null) {
            if (service != null) {
                fNavigation.updateAccounts(service.getAccounts());
            }
            fNavigation.setCallback(service);
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
            case REQUEST_CODE_CREATE_ACCOUNT:
                mNoAccountOpened = false;
            case REQUEST_CODE_PREFERENCES:
            case AccountsManagementFragment.ACCOUNT_EDIT_REQUEST:
                if (fNavigation != null) {
                    fNavigation.updateAccounts(service.getAccounts());
                }
                break;
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
    public IDRingService getRemoteService() {
        return service.getRemoteService();
    }

    @Override
    public LocalService getService() {
        return service;
    }

    @Override
    public void onNavigationSectionSelected(RingNavigationFragment.Section section) {
        mNavigationDrawer.closeDrawers();

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

    @Override
    public void onAccountSelected() {
        mNavigationDrawer.closeDrawers();
    }

    @Override
    public void onAddAccountSelected() {
        mNavigationDrawer.closeDrawers();
        startActivityForResult(new Intent(HomeActivity.this, AccountWizard.class), AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
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
        if (mNavigationDrawer != null) {
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

    public void onCallContact(final CallContact c) {
        Log.d(TAG, "onCallContact " + c.toString() + " " + c.getId() + " " + c.getKey());
        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (Phone p : c.getPhones()) {
                numbers[i++] = p.getNumber().getRawUriString();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_number);
            builder.setItems(numbers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(CallActivity.ACTION_CALL)
                            .setClass(HomeActivity.this, CallActivity.class)
                            .setData(Uri.parse(selected.toString()));
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(CallActivity.ACTION_CALL)
                    .setClass(this, CallActivity.class)
                    .setData(Uri.parse(c.getPhones().get(0).getNumber().getRawUriString()));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
        }
    }

    public void onTextContact(final CallContact c) {
        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (Phone p : c.getPhones()) {
                numbers[i++] = p.getNumber().getRawUriString();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_number);
            builder.setItems(numbers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(Intent.ACTION_VIEW)
                            .setClass(HomeActivity.this, ConversationActivity.class)
                            .setData(Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, c.getIds().get(0)))
                            .putExtra("number", selected);
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setClass(this, ConversationActivity.class)
                    .setData(Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, c.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    private void setVideoEnabledFromPermission() {
        //~ Setting correct VIDEO_ENABLED value based on the state of the
        //~ permission. It can handle the case where the user decides to remove a permission from
        //~ the Android general settings.
        if (!LocalService.checkPermission(this, Manifest.permission.CAMERA) && service != null) {
            List<Account> accounts = service.getAccounts();
            if (accounts != null) {
                for (Account account : accounts) {
                    account.setDetail(ConfigKey.VIDEO_ENABLED, false);
                    account.notifyObservers();
                }
            }
        }
    }
}
