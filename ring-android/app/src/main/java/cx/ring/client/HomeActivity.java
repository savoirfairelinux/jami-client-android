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
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.transition.Explode;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import cx.ring.R;
import cx.ring.fragments.AboutFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.ContactListFragment;
import cx.ring.fragments.SettingsFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.model.CallContact;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.views.MenuHeaderView;

public class HomeActivity extends AppCompatActivity implements LocalService.Callbacks,
        NavigationView.OnNavigationItemSelectedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        ContactListFragment.Callbacks {

    static final String TAG = HomeActivity.class.getSimpleName();

    public static final int REQUEST_CODE_PREFERENCES = 1;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;

    private LocalService service;
    private boolean mBound = false;
    private boolean mNoAccountOpened = false;

    private NavigationView fMenu;
    private MenuHeaderView fMenuHead = null;
    private DrawerLayout mNavigationDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private LinearLayout mToolbarSpacerView;
    private TextView mToolbarSpacerTitle;
    private float mToolbarSize;
    private FloatingActionButton actionButton;
    protected android.app.Fragment fContent;

    public interface Refreshable {
        void refresh();
    }

    private static void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, "Uncaught Exception detected in thread ", e);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Could not set the Default Uncaught Exception Handler");
        }
    }

    /* called before activity is killed, e.g. rotation */
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setDefaultUncaughtExceptionHandler();

        mToolbarSize = getResources().getDimension(R.dimen.abc_action_bar_default_height_material);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        actionButton = (FloatingActionButton) findViewById(R.id.action_button);

        mToolbarSpacerView = (LinearLayout)findViewById(R.id.toolbar_spacer);
        mToolbarSpacerTitle = (TextView)findViewById(R.id.toolbar_spacer_title);

        fMenu = (NavigationView) findViewById(R.id.left_drawer);
        fMenu.setNavigationItemSelectedListener(this);
        mNavigationDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

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
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
                if (null != fMenuHead) {
                    fMenuHead.updateUserView();
                }
            }
        };

        mNavigationDrawer.addDrawerListener(mDrawerToggle);

        // Bind to LocalService
        String[] toRequest = LocalService.checkRequiredPermissions(this);
        if (toRequest.length > 0) {
            ActivityCompat.requestPermissions(this, toRequest, LocalService.PERMISSIONS_REQUEST);
        } else if (!mBound) {
            Log.i(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            startService(intent);
            bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
        }
    }

    /**
     * Listener given to the MenuHeaderView to be notified when the QRCode is clicked
     */
    private View.OnClickListener mQRCodeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            QRCodeFragment zoom = QRCodeFragment.newInstance(fMenuHead.getSelectedAccount().getShareURI());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                zoom.setEnterTransition(new Explode());
                zoom.setReturnTransition(new Fade());
            }

            mNavigationDrawer.closeDrawers();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.drawer_layout, zoom)
                    .addToBackStack(null)
                    .commit();
        }
    };

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction());
            switch (intent.getAction()) {
                case LocalService.ACTION_ACCOUNT_UPDATE:
                    if (!mNoAccountOpened && service.getAccounts().isEmpty()) {
                        mNoAccountOpened = true;
                        startActivityForResult(new Intent().setClass(HomeActivity.this, AccountWizard.class), AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
                    } else {
                        fMenuHead.updateAccounts(service.getAccounts());
                    }
                    break;
            }
        }
    };

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
        Log.i(TAG, "onStart");
        if (!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("installed", false)) {
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("installed", true).commit();
            copyAssetFolder(getAssets(), "ringtones", getFilesDir().getAbsolutePath() + "/ringtones");
        }
        super.onStart();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.w(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case LocalService.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                for (int i = 0, n = permissions.length; i < n; i++) {
                    switch (permissions[i]) {
                        case Manifest.permission.RECORD_AUDIO:
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "Missing required permission RECORD_AUDIO");
                                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                        .setTitle(R.string.start_error_title)
                                        .setMessage(R.string.start_error_mic_required)
                                        .setIcon(R.drawable.ic_mic_black_48dp)
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
        }
    }

    public void setToolbarState(boolean double_h, int title_res) {

        mToolbar.setMinimumHeight((int) mToolbarSize);
        ViewGroup.LayoutParams toolbarSpacerViewParams = mToolbarSpacerView.getLayoutParams();

        if (double_h) {
            // setting the height of the toolbar spacer with the same height than the toolbar
            toolbarSpacerViewParams.height = (int)mToolbarSize;
            mToolbarSpacerView.setLayoutParams(toolbarSpacerViewParams);

            // setting the toolbar spacer title (hiding the real toolbar title)
            mToolbarSpacerTitle.setText(title_res);
            mToolbar.setTitle("");

            // the spacer and the action button become visible
            mToolbarSpacerView.setVisibility(View.VISIBLE);
            actionButton.setVisibility(View.VISIBLE);
        } else {
            // hide the toolbar spacer and the action button
            mToolbarSpacerView.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);
            mToolbar.setTitle(title_res);

        }
    }

    public FloatingActionButton getActionButton() {
        return actionButton;
    }

    private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            Log.i(TAG, "Creating :" + toPath);
            boolean res = true;
            for (String file : files)
                if (file.contains("")) {
                    Log.i(TAG, "Copying file :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAsset(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                } else {
                    Log.i(TAG, "Copying folder :" + fromAssetPath + "/" + file + " to " + toPath + "/" + file);
                    res &= copyAssetFolder(assetManager, fromAssetPath + "/" + file, toPath + "/" + file);
                }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /* user gets back to the activity, e.g. through task manager */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /* activity gets back to the foreground and user input */
    @Override
    protected void onResume() {
        super.onResume();
        this.setVideoEnabledFromPermission();
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawer.isDrawerVisible(Gravity.LEFT)) {
            mNavigationDrawer.closeDrawer(Gravity.LEFT);
            return;
        }

        if (getFragmentManager().getBackStackEntryCount() > 1) {
            popCustomBackStack();
            fMenu.getMenu().findItem(R.id.menuitem_home).setChecked(true);
            return;
        }

        super.onBackPressed();
    }

    private void popCustomBackStack() {
        FragmentManager fm = getFragmentManager();
        FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(0);
        fContent = fm.findFragmentByTag(entry.getName());
        for (int i = 0; i < fm.getBackStackEntryCount() - 1; ++i) {
            fm.popBackStack();
        }
    }

    /* activity no more in foreground */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
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
            Log.i(TAG, "onServiceConnected " + className.getClassName());
            LocalService.LocalBinder binder = (LocalService.LocalBinder) s;
            service = binder.getService();

            setVideoEnabledFromPermission();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalService.ACTION_CONF_UPDATE);
            intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
            registerReceiver(receiver, intentFilter);
            mBound = true;

            fMenuHead = (MenuHeaderView) fMenu.getHeaderView(0);
            if (fMenuHead == null) {
                fMenuHead = new MenuHeaderView(HomeActivity.this);
                fMenuHead.setCallbacks(service);
                fMenu.addHeaderView(fMenuHead);
                fMenuHead.setQRCodeListener(mQRCodeClickListener);
            }

            FragmentManager fm = getFragmentManager();
            fContent = fm.findFragmentById(R.id.main_frame);
            if (fContent == null) {
                fContent = new SmartListFragment();
                fm.beginTransaction().replace(R.id.main_frame, fContent, "Home").addToBackStack("Home").commit();
            } else if (fContent instanceof Refreshable) {
                fm.beginTransaction().replace(R.id.main_frame, fContent).addToBackStack("Home").commit();
                ((Refreshable) fContent).refresh();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "onServiceDisconnected " + className.getClassName());
            if (fMenuHead != null) {
                fMenuHead.setCallbacks(null);
                fMenuHead = null;
            }
            mBound = false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PREFERENCES:
            case AccountsManagementFragment.ACCOUNT_EDIT_REQUEST:
                if (fMenuHead != null)
                    fMenuHead.updateAccounts(service.getAccounts());
                break;
            case REQUEST_CODE_CALL:
                if (resultCode == CallActivity.RESULT_FAILURE) {
                    Log.w(TAG, "Call Failed");
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
    public boolean onNavigationItemSelected(MenuItem pos) {
        pos.setChecked(true);
        mNavigationDrawer.closeDrawers();

        switch (pos.getItemId()) {
            case R.id.menuitem_home:

                if (fContent instanceof SmartListFragment)
                    break;

                if (getFragmentManager().getBackStackEntryCount() == 1)
                    break;

                popCustomBackStack();

                break;
            case R.id.menuitem_accounts:
                if (fContent instanceof AccountsManagementFragment)
                    break;
                fContent = new AccountsManagementFragment();
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "Accounts").addToBackStack("Accounts").commit();
                break;
            case R.id.menuitem_about:
                if (fContent instanceof AboutFragment)
                    break;
                fContent = new AboutFragment();
                getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.main_frame, fContent, "About").addToBackStack("About").commit();
                break;
            case R.id.menuitem_prefs:
                this.goToSettings();
                break;
            default:
                return false;
        }

        return true;
    }

    public void goToSettings() {
        if (fMenu != null) {
            MenuItem settingsItem = fMenu.getMenu().findItem(R.id.menuitem_prefs);
            if (settingsItem != null) {
                settingsItem.setChecked(true);
            }
        }
        if (mNavigationDrawer != null) {
            mNavigationDrawer.closeDrawers();
        }
        if (fContent instanceof SettingsFragment)
            return;
        fContent = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.main_frame, fContent, "Prefs")
                .addToBackStack("Prefs")
                .commit();
    }

    @Override
    public void onCallContact(final CallContact c) {
        Log.w(TAG, "onCallContact " + c.toString() + " " + c.getId() + " " + c.getKey());
        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                numbers[i++] = p.getNumber().getRawUriString();

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

    @Override
    public void onTextContact(final CallContact c) {
        if (c.getPhones().size() > 1) {
            final CharSequence numbers[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                numbers[i++] = p.getNumber().getRawUriString();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.choose_number);
            builder.setItems(numbers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = numbers[which];
                    Intent intent = new Intent(Intent.ACTION_VIEW)
                            .setClass(HomeActivity.this, ConversationActivity.class)
                            .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)))
                            .putExtra("number", selected);
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setClass(this, ConversationActivity.class)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    private void setVideoEnabledFromPermission() {
        //~ Setting correct AccountDetailBasic.CONFIG_VIDEO_ENABLED value based on the state of the
        //~ permission. It can handle the case where the user decides to remove a permission from
        //~ the Android general settings.
        if (!LocalService.checkPermission(this, Manifest.permission.CAMERA)) {
            if (null != service) {
                List<Account> accounts = service.getAccounts();
                if (null != accounts) {
                    for (Account account : accounts) {
                        account.getBasicDetails()
                                .setDetailString(AccountDetailBasic.CONFIG_VIDEO_ENABLED,
                                        Boolean.toString(false));
                        account.notifyObservers();
                    }
                }
            }
        }
    }

    /**
     * Inner fragment used to show a fullscreen QRCode
     * Only used here, may need its own file if used somewhere else
     */
    public static class QRCodeFragment extends android.support.v4.app.Fragment {

        private static String ARG_URI = "QRCodeFragment.URI";
        private ImageView mQrImage;

        /**
         * Create a new QRCodeFragment
         *
         * @param uri the string representing the uri to be displayed
         * @return a new QRCodeFragment instance
         */
        public static QRCodeFragment newInstance(String uri) {
            Bundle args = new Bundle();
            args.putString(ARG_URI, uri);
            QRCodeFragment fragment = new QRCodeFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.frag_qrcode, container, false);

            final String uriToShow = getArguments().getString(ARG_URI);
            mQrImage = (ImageView) rootView.findViewById(R.id.qr_image);

            mQrImage.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (uriToShow != null) {
                        Bitmap qrBitmap = encodeStringAsQrBitmap(uriToShow, mQrImage.getMeasuredWidth());
                        mQrImage.setImageBitmap(qrBitmap);
                    }
                }
            });
            rootView.findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getFragmentManager().popBackStack();
                }
            });
            return rootView;
        }

        /**
         * @param input          uri to be displayed
         * @param qrWindowPixels the ImageView size that will contain the QRcode
         * @return the resulting image
         */
        public static Bitmap encodeStringAsQrBitmap(String input, int qrWindowPixels) {
            QRCodeWriter qr_writer = new QRCodeWriter();
            BitMatrix qr_image_matrix;
            try {
                qr_image_matrix = qr_writer.encode(input, BarcodeFormat.QR_CODE, qrWindowPixels, qrWindowPixels);
            } catch (WriterException e) {
                e.printStackTrace();
                return null;
            }

            int qrImageWidth = qr_image_matrix.getWidth();
            int qrImageHeight = qr_image_matrix.getHeight();
            int[] pixels = new int[qrImageWidth * qrImageHeight];

            final int BLACK = 0x00FFFFFF;
            final int WHITE = 0xFFFFFFFF;

            for (int row = 0; row < qrImageHeight; row++) {
                int offset = row * qrImageWidth;
                for (int column = 0; column < qrImageWidth; column++) {
                    pixels[offset + column] = qr_image_matrix.get(column, row) ? BLACK : WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(qrImageWidth, qrImageHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, qrImageWidth, 0, 0, qrImageWidth, qrImageHeight);
            return bitmap;
        }
    }
}
