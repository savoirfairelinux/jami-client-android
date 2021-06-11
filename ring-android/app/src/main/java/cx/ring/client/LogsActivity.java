/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.jami.services.HardwareService;
import net.jami.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.databinding.ActivityLogsBinding;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LogsActivity extends AppCompatActivity {
    private static final String TAG = LogsActivity.class.getSimpleName();

    private ActivityLogsBinding binding;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable disposable;

    private ActivityResultLauncher<String> fileSaver;
    private File mCurrentFile = null;

    @Inject
    @Singleton
    HardwareService mHardwareService;

    public LogsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();
        JamiApplication.getInstance().getInjectionComponent().inject(this);
        binding = ActivityLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);

        fileSaver = registerForActivityResult(new ActivityResultContracts.CreateDocument(), result -> compositeDisposable.add(
                AndroidFileUtils.copyFileToUri(getContentResolver(), mCurrentFile, result).
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribe(() -> {
                            if (!mCurrentFile.delete())
                                Log.w(TAG, "Can't delete temp file");
                            mCurrentFile = null;
                            Snackbar.make(binding.getRoot(), R.string.file_saved_successfully, Snackbar.LENGTH_SHORT).show();
                        }, error -> Snackbar.make(binding.getRoot(), R.string.generic_error, Snackbar.LENGTH_SHORT).show())));

        binding.fab.setOnClickListener(view -> {
            if (disposable == null)
                startLogging();
            else
                stopLogging();
        });
        if (mHardwareService.isLogging())
            startLogging();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logs_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private Maybe<String> getLog() {
        if (mHardwareService.isLogging())
            return mHardwareService.startLogs()
                .firstElement();
        CharSequence log = binding.logView.getText();
        if (StringUtils.isEmpty(log))
            return Maybe.empty();
        return Maybe.just(log.toString());
    }

    private Maybe<File> getLogFile() {
        return getLog()
                .observeOn(Schedulers.io())
                .map(log -> {
                    File file = AndroidFileUtils.createLogFile(this);
                    OutputStream os = new FileOutputStream(file);
                    os.write(log.getBytes());
                    return file;
                });
    }

    private Maybe<Uri> getLogUri() {
        return getLogFile().map(file -> ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.menu_log_share) {
            compositeDisposable.add(getLogUri()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(uri -> {
                        Log.w(TAG, "saved logs to " + uri);
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        String type = getContentResolver().getType(uri);
                        sendIntent.setDataAndType(uri, type);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(sendIntent, null));
                    }, e -> Snackbar.make(binding.getRoot(), "Error sharing logs: " + e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show()));
            return true;
        } else if (id == R.id.menu_log_save) {
            compositeDisposable.add(getLogFile()
                    .subscribe(file -> {
                        mCurrentFile = file;
                        fileSaver.launch(file.getName());
                    }));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void startLogging() {
        binding.logView.setText("");
        disposable = mHardwareService.startLogs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message -> {
                    binding.logView.setText(message);
                    binding.scroll.post(() -> binding.scroll.fullScroll(View.FOCUS_DOWN));
                }, e -> Log.w(TAG, "Error in logger", e));
        compositeDisposable.add(disposable);
        setButtonState(true);
    }

    void stopLogging() {
        disposable.dispose();
        disposable = null;
        mHardwareService.stopLogs();
        setButtonState(false);
    }

    void setButtonState(boolean logging) {
        binding.fab.setText(logging ? R.string.pref_logs_stop : R.string.pref_logs_start);
        binding.fab.setBackgroundColor(ContextCompat.getColor(this, logging ? R.color.red_400 : R.color.colorSecondary));
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        compositeDisposable.clear();
        super.onDestroy();
    }
}