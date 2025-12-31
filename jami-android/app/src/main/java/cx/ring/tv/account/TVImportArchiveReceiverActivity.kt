package cx.ring.tv.account

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import cx.ring.utils.AndroidFileUtils.getCacheFile
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

@AndroidEntryPoint
class TVImportArchiveReceiverActivity : ComponentActivity() {

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isTvDevice()) {
            Log.w(TAG, "Rejected: Not a TV device")
            finish()
            return
        }

        val uri = extractUri(intent)

        if (uri == null) {
            finish()
            return
        }

        if (!isJac(uri)) {
            Log.w(TAG, "Rejected non-.jac uri=$uri")
            finish()
            return
        }

        getCacheFile(this, uri)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file: File ->
                Log.w(TAG, "Cached .jac to: $file")

                startActivity(Intent(this, cx.ring.tv.main.HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(EXTRA_AUTO_OPEN_BACKUP_FLOW, true)
                    putExtra(EXTRA_ARCHIVE_PATH, file.absolutePath)
                })
                finish()
            }, { e ->
                Log.e(TAG, "Import archive failed", e)
                finish()
            })
            .let(disposables::add)
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun extractUri(i: Intent?): Uri? {
        if (i == null) return null
        return when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.firstOrNull()
            }
            else -> i.data
        }
    }

    private fun isJac(uri: Uri): Boolean {
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) {
                        val name = c.getString(idx) ?: ""
                        return name.endsWith(".jac", ignoreCase = true)
                    }
                }
        }
        return (uri.lastPathSegment ?: "").endsWith(".jac", ignoreCase = true)
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    companion object {
        private const val TAG = "TVImportArchiveReceiver"
        const val EXTRA_AUTO_OPEN_BACKUP_FLOW = "EXTRA_AUTO_OPEN_BACKUP_FLOW"
        const val EXTRA_ARCHIVE_PATH = "extra_archive_path"
    }
}