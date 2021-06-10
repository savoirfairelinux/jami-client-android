package cx.ring.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;

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

    private Maybe<Uri> getLogFile() {
        return mHardwareService.startLogs()
                .firstElement()
                .observeOn(Schedulers.io())
                .map(log -> {
                    File file = AndroidFileUtils.createLogFile(this);
                    OutputStream os = new FileOutputStream(file);
                    os.write(log.getBytes());
                    return ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file);
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_log_share) {
            compositeDisposable.add(getLogFile()
                .subscribe(uri -> {
                    Log.w(TAG, "saved logs to " + uri);
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String type = getContentResolver().getType(uri);
                    sendIntent.setDataAndType(uri, type);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(sendIntent, null));
                }, e -> Toast.makeText(this, "Error sharing logs: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show()));
            return true;
        } else if (id == R.id.menu_log_save) {
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
        binding.fab.setText(logging ? "Stop logging" : "Start logging");
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