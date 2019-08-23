/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.adapters.RingtoneAdapter;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.ConfigKey;
import cx.ring.model.Ringtone;
import cx.ring.services.AccountService;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.Log;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class RingtoneActivity extends AppCompatActivity {

    private final String TAG = RingtoneActivity.class.getSimpleName();

    private RingtoneAdapter adapter;
    private Account mAccount;
    private TextView customRingtone;
    private ImageView customPlaying, customSelected;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Disposable disposable;

    @Inject
    @Singleton
    AccountService mAccountService;

    public static final int MAX_SIZE_RINGTONE = 64 * 1024;
    private static final int SELECT_RINGTONE_PATH = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        setContentView(R.layout.activity_ringtone);
        super.onCreate(savedInstanceState);
        mAccount = mAccountService.getAccount(getIntent().getExtras().getString(AccountEditionActivity.ACCOUNT_ID_KEY));

        Toolbar toolbar = findViewById(R.id.ringtoneToolbar);
        toolbar.setNavigationOnClickListener(view ->
                finish());

        RecyclerView recycler = findViewById(R.id.ringToneRecycler);
        ConstraintLayout customRingtoneLayout = findViewById(R.id.customRingtoneLayout);
        customRingtone = findViewById(R.id.customRingtoneName);
        customPlaying = findViewById(R.id.custom_ringtone_playing);
        customSelected = findViewById(R.id.custom_ringtone_selected);
        adapter = new RingtoneAdapter(this);

        RecyclerView.LayoutManager upcomingLayoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(upcomingLayoutManager);
        recycler.setItemAnimator(new DefaultItemAnimator());
        recycler.setAdapter(adapter);

        // loads the user's settings
        setPreference();

        customRingtoneLayout.setOnClickListener(v ->
                displayFileSearchDialog());

        customRingtoneLayout.setOnLongClickListener(view -> {
            displayRemoveDialog();
            return true;
        });

        disposable = adapter.getRingtoneSubject().subscribe(ringtone -> {
            setJamiRingtone(ringtone);
            removeCustomRingtone();
        }, e -> Log.e(TAG, "Error updating ringtone status"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public void finish() {
        super.finish();
        adapter.releaseMediaPlayer();
        mediaPlayer.release();
    }

    /**
     * Sets the selected ringtone (Jami or custom) on activity startup
     */
    private void setPreference() {
        File path = new File(mAccount.getConfig().get(ConfigKey.RINGTONE_PATH));
        boolean customEnabled = mAccount.getConfig().getBool(ConfigKey.RINGTONE_CUSTOM);
        if (customEnabled) {
            customRingtone.setText(path.getName());
            customSelected.setVisibility(View.VISIBLE);
        } else {
            adapter.selectDefaultItem(path.getAbsolutePath(), mAccount.getConfig().getBool(ConfigKey.RINGTONE_ENABLED));
        }
    }

    /**
     * Sets a Jami ringtone as the default
     *
     * @param ringtone the ringtone object
     */
    private void setJamiRingtone(Ringtone ringtone) {
        String path = ringtone.getRingtonePath();
        if (path == null) {
            mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, false);
            mAccount.setDetail(ConfigKey.RINGTONE_PATH, "");
        } else {
            mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, true);
            mAccount.setDetail(ConfigKey.RINGTONE_PATH, ringtone.getRingtonePath());
            mAccount.setDetail(ConfigKey.RINGTONE_CUSTOM, false);
        }
        updateAccount();
    }

    /**
     * Sets a custom ringtone selected by the user
     *
     * @param path the ringtoen path
     * @see #onFileFound(File) onFileFound
     * @see #displayFileSearchDialog() displayFileSearchDialog
     */
    private void setCustomRingtone(String path) {
        mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, true);
        mAccount.setDetail(ConfigKey.RINGTONE_PATH, path);
        mAccount.setDetail(ConfigKey.RINGTONE_CUSTOM, true);
        updateAccount();
    }

    /**
     * Updates an account with new details
     */
    private void updateAccount() {
        mAccountService.setCredentials(mAccount.getAccountID(), mAccount.getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccount.getAccountID(), mAccount.getDetails());
    }

    /**
     * Previews a custom ringtone
     *
     * @param ringtone the ringtone file
     */
    private void previewRingtone(File ringtone) {
        try {
            mediaPlayer.setDataSource(ringtone.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException | NullPointerException e) {
            stopCustomPreview();
            Log.e(TAG, "Error previewing ringtone", e);
        }
        mediaPlayer.setOnCompletionListener(mp ->
                stopCustomPreview());
    }

    /**
     * Removes a custom ringtone and updates the view
     */
    private void removeCustomRingtone() {
        customSelected.setVisibility(View.INVISIBLE);
        customPlaying.setVisibility(View.INVISIBLE);
        customRingtone.setText(R.string.ringtone_custom_prompt);
        stopCustomPreview();
    }

    /**
     * Stops audio previews from all possible sources
     */
    private void stopCustomPreview() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();
        mediaPlayer.reset();
    }

    /**
     * Handles playing and setting a custom ringtone or displaying an error if it is too large
     *
     * @param ringtone the ringtone path
     */
    private void onFileFound(File ringtone) {
        if (ringtone.length() / 1024 > MAX_SIZE_RINGTONE) {
            displayFileTooBigDialog();
        } else {
            // resetState will stop the preview
            adapter.resetState();
            customRingtone.setText(ringtone.getName());
            customSelected.setVisibility(View.VISIBLE);
            customPlaying.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(R.raw.baseline_graphic_eq_black_24dp)
                    .placeholder(R.drawable.ic_graphic_eq_black_24dp)
                    .into(new DrawableImageViewTarget(customPlaying));
            previewRingtone(ringtone);
            setCustomRingtone(ringtone.getAbsolutePath());
        }
    }

    /**
     * Displays the native file browser to select a ringtone
     */
    private void displayFileSearchDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, SELECT_RINGTONE_PATH);
    }

    /**
     * Displays a dialog if the selected ringtone is too large
     */
    private void displayFileTooBigDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ringtone_error_title)
                .setMessage(getString(R.string.ringtone_error_size_too_big, MAX_SIZE_RINGTONE))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Displays a dialog that prompts the user to remove a custom ringtone
     */
    private void displayRemoveDialog() {
        if (!mAccount.getConfig().getBool(ConfigKey.RINGTONE_CUSTOM))
            return;
        String[] item = {"Remove"};
        // subject callback from adapter will update the view
        new AlertDialog.Builder(this)
                .setItems(item, (dialog, which) ->
                        adapter.setDefault()).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;
        Uri uri = data.getData();
        if (resultCode == Activity.RESULT_CANCELED || uri == null) {
            return;
        }

        ContentResolver cr = getContentResolver();
        if (requestCode == SELECT_RINGTONE_PATH) {
            try {
                String path = AndroidFileUtils.getRealPathFromURI(this, uri);
                if (path == null)
                    throw new IllegalArgumentException();
                onFileFound(new File(path));
            } catch (Exception e) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cr.takePersistableUriPermission(uri, takeFlags);
                AndroidFileUtils.getCacheFile(this, uri)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> onFileFound(file),
                                err -> Toast.makeText(this, "Can't load ringtone !", Toast.LENGTH_SHORT).show());
            }
        }
    }

}
