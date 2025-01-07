/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.databinding.ActivityPushNotificationLogsBinding
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUri
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.services.HardwareService
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class PushNotificationLogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPushNotificationLogsBinding
    private val compositeDisposable = CompositeDisposable()
    private var disposable: Disposable? = null
    private lateinit var logAdapter: LogAdapter
    private lateinit var fileSaver: ActivityResultLauncher<String>
    private lateinit var logFile: File

    @Inject
    @Singleton
    lateinit var mHardwareService: HardwareService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPushNotificationLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        logAdapter = LogAdapter()
        binding.logRecyclerView.adapter = logAdapter
        binding.logRecyclerView.layoutManager = LinearLayoutManager(this)
        val pushSummaryTextView = findViewById<TextView>(R.id.pushSummaryTextView)
        pushSummaryTextView.text = "Push notifications received since " +
                "${mHardwareService.startTime}\n" +
                "high priority - ${mHardwareService.highPriorityPushCount}\n" +
                "normal priority - ${mHardwareService.normalPriorityPushCount}\n" +
                "unknown priority - ${mHardwareService.unknownPriorityPushCount}"

        logFile = mHardwareService.pushLogFile

        binding.startLoggingButton.setOnClickListener {
            if (disposable == null) startLogging() else stopLogging()
        }

        fileSaver = registerForActivityResult(ActivityResultContracts
            .CreateDocument("text/plain")) { result: Uri? ->
            if (result != null) {
                copyFileToUri(logFile, result)
            }
        }

        if (mHardwareService.loggingStatus) startLogging()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.push_notification_logs_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_share -> {
                shareLogs(logUri)
                return true
            }
            R.id.menu_save -> {
                saveFile()
                return true
            }
            R.id.menu_clear -> {
                clearLogs()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun startLogging() {
        logAdapter.clearLogs()
        compositeDisposable.add(mHardwareService.startPushLogs()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message: String ->
                val adapter = binding.logRecyclerView.adapter as LogAdapter
                adapter.addLogs(listOf(LogMessage(message)))
                binding.logRecyclerView.smoothScrollToPosition(adapter.itemCount - 1)
            })
            { e -> Log.w(TAG, "Error in logger", e) }
            .apply { disposable = this })
        setButtonState(true)

    }

    private fun stopLogging() {
        mHardwareService.stopPushLogs()
        disposable?.let {
            it.dispose()
            disposable = null
        }
        setButtonState(false)
    }

    private fun copyFileToUri(sourceFile: File, targetUri: Uri) {
        try {
            contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Snackbar.make(binding.root, R.string.file_saved_successfully, Snackbar
                .LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file", e)
            Snackbar.make(binding.root, R.string.generic_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun clearLogs() {
        logFile.writeText("")
        logAdapter.clearLogs()
    }

    private fun saveFile() {
        if (logFile.exists()) {
            fileSaver.launch(logFile.name)
        } else {
            Snackbar.make(binding.root, "Log file does not exist.", Snackbar
                .LENGTH_SHORT).show()
        }
    }

    private fun shareLogs(uriMaybe: Maybe<Uri>) {
        compositeDisposable.add(uriMaybe
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ uri: Uri ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(uri, contentResolver.getType(uri))
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                startActivity(Intent.createChooser(sendIntent, null))
            }) { e: Throwable ->
                Log.e(TAG, "Error sharing logs", e)
                Snackbar.make(binding.root, getString(R.string.sharing_log_error), Snackbar
                    .LENGTH_SHORT).show()
            })
    }

    private val log: Maybe<String>
        get() = logAdapter.getLogs().let {
            if (it.isEmpty()) Maybe.empty() else Maybe.just(it)
        }

    private val tempFile: Maybe<File>
        get() = log
            .observeOn(Schedulers.io())
            .map { log: String ->
                val file = AndroidFileUtils.createLogFile(this)
                FileOutputStream(file).use { os -> os.write(log.toByteArray()) }
                file
            }
    private val logUri: Maybe<Uri>
        get() = tempFile.map { file: File ->
            ContentUri.getUriForFile(this, file)
        }

    private fun setButtonState(logging: Boolean) {
        binding.startLoggingButton.setText(if (logging) R.string.pref_logs_stop else R
            .string.pref_logs_start)
        binding.startLoggingButton.setBackgroundColor(ContextCompat
            .getColor(this, if (logging) R.color.red_400 else R.color.colorSecondary))
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    companion object {
        private val TAG = PushNotificationLogsActivity::class.simpleName!!
    }

}