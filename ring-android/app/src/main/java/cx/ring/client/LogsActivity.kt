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
package cx.ring.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.application.JamiApplication.Companion.instance
import cx.ring.databinding.ActivityLogsBinding
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUriHandler
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.services.HardwareService
import net.jami.utils.StringUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private var binding: ActivityLogsBinding? = null
    private val compositeDisposable = CompositeDisposable()
    private var disposable: Disposable? = null
    private var fileSaver: ActivityResultLauncher<String>? = null
    private var mCurrentFile: File? = null

    @JvmField
    @Inject
    @Singleton
    var mHardwareService: HardwareService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance!!.startDaemon()
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbar)
        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        fileSaver = registerForActivityResult(CreateDocument()) { result: Uri? ->
            compositeDisposable.add(
                AndroidFileUtils.copyFileToUri(contentResolver, mCurrentFile, result)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                    {
                        if (!mCurrentFile!!.delete()) Log.w(TAG, "Can't delete temp file")
                        mCurrentFile = null
                        Snackbar.make(
                            binding!!.root,
                            R.string.file_saved_successfully,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }) {
                        Snackbar.make(
                        binding!!.root, R.string.generic_error, Snackbar.LENGTH_SHORT
                    ).show()
                })
        }
        binding!!.fab.setOnClickListener { if (disposable == null) startLogging() else stopLogging() }
        if (mHardwareService!!.isLogging) startLogging()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logs_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private val log: Maybe<String>
        get() {
            if (mHardwareService!!.isLogging) return mHardwareService!!.startLogs()
                .firstElement()
            val log = binding!!.logView.text
            return if (StringUtils.isEmpty(log)) Maybe.empty() else Maybe.just(
                log.toString()
            )
        }
    private val logFile: Maybe<File>
        get() = log
            .observeOn(Schedulers.io())
            .map { log: String ->
                val file = AndroidFileUtils.createLogFile(this)
                val os: OutputStream = FileOutputStream(file)
                os.write(log.toByteArray())
                file
            }
    private val logUri: Maybe<Uri>
        get() = logFile.map { file: File? ->
            ContentUriHandler.getUriForFile(
                this,
                ContentUriHandler.AUTHORITY_FILES,
                file!!
            )
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_log_share -> {
                compositeDisposable.add(
                    logUri
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ uri: Uri ->
                            Log.w(TAG, "saved logs to $uri")
                            val sendIntent = Intent(Intent.ACTION_SEND)
                            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            val type = contentResolver.getType(uri)
                            sendIntent.setDataAndType(uri, type)
                            sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
                            startActivity(Intent.createChooser(sendIntent, null))
                        }) { e: Throwable ->
                            Snackbar.make(
                                binding!!.root,
                                "Error sharing logs: " + e.localizedMessage,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        })
                return true
            }
            R.id.menu_log_save -> {
                compositeDisposable.add(
                    logFile
                        .subscribe { file: File ->
                            mCurrentFile = file
                            fileSaver!!.launch(file.name)
                        })
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun startLogging() {
        binding!!.logView.text = ""
        disposable = mHardwareService!!.startLogs()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message: String? ->
                binding!!.logView.text = message
                binding!!.scroll.post { binding!!.scroll.fullScroll(View.FOCUS_DOWN) }
            }) { e: Throwable? -> Log.w(TAG, "Error in logger", e) }
        compositeDisposable.add(disposable)
        setButtonState(true)
    }

    private fun stopLogging() {
        disposable!!.dispose()
        disposable = null
        mHardwareService!!.stopLogs()
        setButtonState(false)
    }

    private fun setButtonState(logging: Boolean) {
        binding!!.fab.setText(if (logging) R.string.pref_logs_stop else R.string.pref_logs_start)
        binding!!.fab.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (logging) R.color.red_400 else R.color.colorSecondary
            )
        )
    }

    override fun onDestroy() {
        if (disposable != null) {
            disposable!!.dispose()
            disposable = null
        }
        compositeDisposable.clear()
        super.onDestroy()
    }

    companion object {
        private val TAG = LogsActivity::class.java.simpleName
    }
}