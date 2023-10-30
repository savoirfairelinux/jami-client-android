/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityLogsBinding
import cx.ring.databinding.CrashReportBinding
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUriHandler
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.android.tombstone.TombstoneProtos.Tombstone
import net.jami.services.HardwareService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogsBinding
    private val compositeDisposable = CompositeDisposable()
    private var disposable: Disposable? = null
    private lateinit var fileSaver: ActivityResultLauncher<String>
    private var mCurrentFile: File? = null

    @Inject
    @Singleton
    lateinit var mHardwareService: HardwareService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon(this)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fileSaver = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { result: Uri? ->
            if (result != null)
                compositeDisposable.add(AndroidFileUtils.copyFileToUri(contentResolver, mCurrentFile, result)
                        .observeOn(AndroidSchedulers.mainThread()).subscribe({
                            if (!mCurrentFile!!.delete())
                                Log.w(TAG, "Can't delete temp file")
                            mCurrentFile = null
                            Snackbar.make(binding.root, R.string.file_saved_successfully, Snackbar.LENGTH_SHORT).show()
                        }) { Snackbar.make(binding.root, R.string.generic_error, Snackbar.LENGTH_SHORT).show()
                    })
        }
        binding.fab.setOnClickListener { if (disposable == null) startLogging() else stopLogging() }

        // Check for previous crash reasons, if any.
        if (savedInstanceState == null)
            showNativeCrashes()

        if (mHardwareService.isLogging) startLogging()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logs_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun shareLogs(uriMaybe: Maybe<Uri>) {
        compositeDisposable.add(uriMaybe
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ uri: Uri ->
                Log.w(TAG, "saved logs to $uri")
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(uri, contentResolver.getType(uri))
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                startActivity(Intent.createChooser(sendIntent, null))
            }) { e: Throwable ->
                Snackbar.make(binding.root, "Error sharing logs: " + e.localizedMessage, Snackbar.LENGTH_SHORT).show()
            })
    }
    fun saveFile(fileMaybe: Maybe<File>) {
        compositeDisposable.add(fileMaybe.subscribe { file: File ->
            mCurrentFile = file
            fileSaver.launch(file.name)
        })
    }

    private val log: Maybe<String>
        get() {
            if (mHardwareService.isLogging)
                return mHardwareService.startLogs().firstElement()
            val log = binding.logView.text
            return if (log.isNullOrEmpty()) Maybe.empty()
            else Maybe.just(log.toString())
        }
    private val logFile: Maybe<File>
        get() = log
            .observeOn(Schedulers.io())
            .map { log: String ->
                val file = AndroidFileUtils.createLogFile(this)
                FileOutputStream(file).use { os -> os.write(log.toByteArray()) }
                file
            }
    private val logUri: Maybe<Uri>
        get() = logFile.map { file: File ->
            ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file)
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_log_share -> {
                shareLogs(logUri)
                return true
            }
            R.id.menu_log_save -> {
                saveFile(logFile)
                return true
            }
            R.id.menu_log_crashes -> {
                showNativeCrashes(true)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    internal class CrashBottomSheet : BottomSheetDialogFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = CrashReportBinding.inflate(inflater, container, false).apply {
            crash.text = arguments?.getString("crash")
            toolbar.menu.findItem(R.id.menu_log_crashes)?.isVisible = false
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_log_share -> {
                        (activity as LogsActivity).shareLogs(crashUri)
                        true
                    }
                    R.id.menu_log_save -> {
                        (activity as LogsActivity).saveFile(crashFile)
                        true
                    }
                    else -> super.onOptionsItemSelected(it)
                }
            }
        }.root

        private val crashFile: Maybe<File> by lazy {
            val crashReport = arguments?.getString("crash")?.toByteArray() ?: return@lazy Maybe.empty()
            val crashFile = AndroidFileUtils.createLogFile(requireContext())
            FileOutputStream(crashFile).use { it.write(crashReport) }
            return@lazy Maybe.just(crashFile)
        }
        private val crashUri: Maybe<Uri> by lazy {
            crashFile.map { file: File ->
                ContentUriHandler.getUriForFile(requireContext(), ContentUriHandler.AUTHORITY_FILES, file)
            }
        }

        companion object {
            const val TAG = "CrashBottomSheet"
        }
    }

    private fun showNativeCrashes(userInitiated: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val activityManager = getSystemService<ActivityManager>() ?: return
        val exitReasons: MutableList<ApplicationExitInfo> =
            activityManager.getHistoricalProcessExitReasons(/* packageName = */ null, /* pid = */0, /* maxNum = */5)

        val stringStream = StringBuilder()
        exitReasons.forEachIndexed { index, aei ->
            if (aei.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                try {
                    val trace: InputStream = aei.traceInputStream ?: return@forEachIndexed
                    val time = Instant.ofEpochMilli(aei.timestamp)
                    stringStream.append("Previous native crash #$index at ${time}: ${aei.description} ${aei.reason} ${aei.pid} ${aei.processName}\n")
                    val tombstone: Tombstone = Tombstone.parseFrom(trace)
                    stringStream.append("Tombstone ${tombstone.tid} ${tombstone.abortMessage}\n")
                    tombstone.causesList.forEachIndexed { i, cause ->
                        stringStream.append("Cause $i: ${cause.humanReadable}\n")
                    }
                    tombstone.threadsMap[tombstone.tid]?.currentBacktraceList?.forEachIndexed { index, frame ->
                        stringStream.append("\t#$index ${frame.fileName} ${frame.functionName}+${frame.functionOffset}\n")
                    }
                    // Enable to print all threads backtrace
                    /*tombstone.threadsMap.values.forEach { thread ->
                        Log.w(TAG, "Backstack for thread ${thread.id} ${thread.name}:")
                        thread.currentBacktraceOrBuilderList.forEachIndexed { index, frame ->
                            Log.w(TAG, "#$index ${frame.fileName} ${frame.functionName}+${frame.functionOffset}")
                        }
                    }*/
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse tombstone", e)
                }
            }
        }
        val crashText = stringStream.toString()
        if (crashText.isNotEmpty()) {
            CrashBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("crash", stringStream.toString())
                }
            }.show(supportFragmentManager, CrashBottomSheet.TAG)
        } else if (userInitiated) {
            Snackbar.make(binding.root, "No recent native crash", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startLogging() {
        // Allows to start logging at application startup.
        mHardwareService.mPreferenceService.isLogActive = true

        binding.logView.text = ""
        compositeDisposable.add(mHardwareService.startLogs()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message: String ->
                binding.logView.text = message
                binding.scroll.post { binding.scroll.fullScroll(View.FOCUS_DOWN) }
            }) { e -> Log.w(TAG, "Error in logger", e) }
            .apply { disposable = this })
        setButtonState(true)
    }

    private fun stopLogging() {
        mHardwareService.mPreferenceService.isLogActive = false

        disposable?.let {
            it.dispose()
            disposable = null
        }
        mHardwareService.stopLogs()
        setButtonState(false)
    }

    private fun setButtonState(logging: Boolean) {
        binding.fab.setText(if (logging) R.string.pref_logs_stop else R.string.pref_logs_start)
        binding.fab.setBackgroundColor(ContextCompat.getColor(this, if (logging) R.color.red_400 else R.color.colorSecondary))
    }

    override fun onDestroy() {
        disposable?.let {
            it.dispose()
            disposable = null
        }
        compositeDisposable.clear()
        super.onDestroy()
    }

    companion object {
        private val TAG = LogsActivity::class.simpleName!!
    }
}