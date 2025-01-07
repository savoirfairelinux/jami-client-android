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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.adapters.RingtoneAdapter
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.model.Account
import net.jami.model.ConfigKey
import net.jami.model.Ringtone
import net.jami.services.AccountService
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class RingtoneActivity : AppCompatActivity() {
    private lateinit var adapter: RingtoneAdapter
    private lateinit var mAccount: Account
    private lateinit var customRingtone: TextView
    private lateinit var customPlaying: ImageView
    private lateinit var customSelected: ImageView
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var disposable: Disposable? = null

    @Inject
    @Singleton
    lateinit var mAccountService: AccountService

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_ringtone)
        super.onCreate(savedInstanceState)
        val account = mAccountService.getAccount(intent.extras!!.getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!)
        if (account == null) {
            finish()
            return
        }
        mAccount = account
        customRingtone = findViewById(R.id.customRingtoneName)
        customPlaying = findViewById(R.id.custom_ringtone_playing)
        customSelected = findViewById(R.id.custom_ringtone_selected)
        adapter = RingtoneAdapter(prepareRingtones())
        val upcomingLayoutManager: LayoutManager = LinearLayoutManager(this)

        val recycler = findViewById<RecyclerView>(R.id.ringToneRecycler)
        recycler.layoutManager = upcomingLayoutManager
        recycler.itemAnimator = DefaultItemAnimator()
        recycler.adapter = adapter

        // loads the user's settings
        setPreference()

        val customRingtoneLayout = findViewById<ConstraintLayout>(R.id.customRingtoneLayout)
        customRingtoneLayout.setOnClickListener { displayFileSearchDialog() }
        customRingtoneLayout.setOnLongClickListener {
            displayRemoveDialog()
            true
        }
        disposable = adapter.getRingtone().subscribe({ ringtone: Ringtone ->
            setJamiRingtone(ringtone)
            removeCustomRingtone()
        }) { Log.e(TAG, "Error updating ringtone status") }
    }

    public override fun onDestroy() {
        super.onDestroy()
        disposable!!.dispose()
    }

    override fun onStop() {
        super.onStop()
        stopCustomPreview()
    }

    override fun finish() {
        super.finish()
        adapter.releaseMediaPlayer()
        mediaPlayer.release()
    }

    private fun prepareRingtones(): List<Ringtone> {
        val ringtoneList: MutableList<Ringtone> = ArrayList()
        val ringtoneFolder = File(filesDir, "ringtones")
        val ringtones = ringtoneFolder.listFiles() ?: return emptyList()
        val ringtoneIcon = getDrawable(R.drawable.baseline_notifications_active_24)!!
        Arrays.sort(ringtones) { a: File, b: File -> a.name.compareTo(b.name) }
        ringtoneList.add(Ringtone("Silent", null, getDrawable(R.drawable.baseline_notifications_off_24)!!))
        for (file in ringtones) {
            val name = stripFileNameExtension(file.name)
            ringtoneList.add(Ringtone(name, file.absolutePath, ringtoneIcon))
        }
        return ringtoneList
    }

    /**
     * Sets the selected ringtone (Jami or custom) on activity startup
     */
    private fun setPreference() {
        val path = File(mAccount.config[ConfigKey.RINGTONE_PATH])
        val customEnabled = mAccount.config.getBool(ConfigKey.RINGTONE_CUSTOM)
        if (customEnabled && path.exists()) {
            customRingtone.text = path.name
            customSelected.visibility = View.VISIBLE
        } else if (path.exists()) {
            adapter.selectDefaultItem(path.absolutePath, mAccount.config.getBool(ConfigKey.RINGTONE_ENABLED))
        } else {
            setDefaultRingtone()
        }
    }

    private fun setDefaultRingtone() {
        val ringtonesDir = File(filesDir, "ringtones")
        val ringtonePath = File(ringtonesDir, getString(R.string.ringtone_default_name)).absolutePath
        adapter.selectDefaultItem(ringtonePath, mAccount.config.getBool(ConfigKey.RINGTONE_ENABLED))
        mAccount.setDetail(ConfigKey.RINGTONE_PATH, ringtonePath)
        mAccount.setDetail(ConfigKey.RINGTONE_CUSTOM, false)
        updateAccount()
    }

    /**
     * Sets a Jami ringtone as the default
     *
     * @param ringtone the ringtone object
     */
    private fun setJamiRingtone(ringtone: Ringtone) {
        val path = ringtone.ringtonePath
        if (path == null) {
            mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, false)
            mAccount.setDetail(ConfigKey.RINGTONE_PATH, "")
        } else {
            mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, true)
            mAccount.setDetail(ConfigKey.RINGTONE_PATH, ringtone.ringtonePath)
            mAccount.setDetail(ConfigKey.RINGTONE_CUSTOM, false)
        }
        updateAccount()
    }

    /**
     * Sets a custom ringtone selected by the user
     *
     * @param path the ringtoen path
     * @see .onFileFound
     * @see .displayFileSearchDialog
     */
    private fun setCustomRingtone(path: String) {
        mAccount.setDetail(ConfigKey.RINGTONE_ENABLED, true)
        mAccount.setDetail(ConfigKey.RINGTONE_PATH, path)
        mAccount.setDetail(ConfigKey.RINGTONE_CUSTOM, true)
        updateAccount()
    }

    /**
     * Updates an account with new details
     */
    private fun updateAccount() {
        mAccountService.setCredentials(mAccount.accountId, mAccount.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount.accountId, mAccount.details)
    }

    /**
     * Previews a custom ringtone
     *
     * @param ringtone the ringtone file
     */
    private fun previewRingtone(ringtone: File) {
        try {
            mediaPlayer.setDataSource(ringtone.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException) {
            stopCustomPreview()
            Log.e(TAG, "Error previewing ringtone", e)
        } catch (e: NullPointerException) {
            stopCustomPreview()
            Log.e(TAG, "Error previewing ringtone", e)
        }
        mediaPlayer.setOnCompletionListener { stopCustomPreview() }
    }

    /**
     * Removes a custom ringtone and updates the view
     */
    private fun removeCustomRingtone() {
        customSelected.visibility = View.INVISIBLE
        customPlaying.visibility = View.INVISIBLE
        customRingtone.setText(R.string.ringtone_custom_prompt)
        stopCustomPreview()
    }

    /**
     * Stops audio previews from all possible sources
     */
    private fun stopCustomPreview() {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.reset()
        } catch (e: Exception) {
        }
    }

    /**
     * Handles playing and setting a custom ringtone or displaying an error if it is too large
     *
     * @param ringtone the ringtone path
     */
    private fun onFileFound(ringtone: File) {
        if (ringtone.length() / 1024 > MAX_SIZE_RINGTONE) {
            displayFileTooBigDialog()
        } else {
            // resetState will stop the preview
            adapter.resetState()
            customRingtone.text = ringtone.name
            customSelected.visibility = View.VISIBLE
            customPlaying.visibility = View.VISIBLE
            Glide.with(this)
                .load(R.raw.baseline_graphic_eq_black_24dp)
                .placeholder(R.drawable.baseline_graphic_eq_24)
                .into(DrawableImageViewTarget(customPlaying))
            previewRingtone(ringtone)
            setCustomRingtone(ringtone.absolutePath)
        }
    }

    /**
     * Displays the native file browser to select a ringtone
     */
    private fun displayFileSearchDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "audio/*"
        startActivityForResult(intent, SELECT_RINGTONE_PATH)
    }

    /**
     * Displays a dialog if the selected ringtone is too large
     */
    private fun displayFileTooBigDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.ringtone_error_title)
            .setMessage(getString(R.string.ringtone_error_size_too_big, MAX_SIZE_RINGTONE))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Displays a dialog that prompts the user to remove a custom ringtone
     */
    private fun displayRemoveDialog() {
        if (!mAccount.config.getBool(ConfigKey.RINGTONE_CUSTOM)) return
        val item = arrayOf("Remove")
        // subject callback from adapter will update the view
        AlertDialog.Builder(this)
            .setItems(item) { _: DialogInterface?, _: Int -> setDefaultRingtone() }.show()
    }

    @SuppressLint("WrongConstant")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        val uri = data.data
        if (resultCode == RESULT_CANCELED || uri == null) {
            return
        }
        val cr = contentResolver
        if (requestCode == SELECT_RINGTONE_PATH) {
            try {
                val path = AndroidFileUtils.getRealPathFromURI(this, uri) ?: throw IllegalArgumentException()
                onFileFound(File(path))
            } catch (e: Exception) {
                val takeFlags = (data.flags
                        and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                cr.takePersistableUriPermission(uri, takeFlags)
                AndroidFileUtils.getCacheFile(this, uri)
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ ringtone: File -> onFileFound(ringtone) }) {
                        Toast.makeText(
                            this,
                            getString(R.string.load_ringtone_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    companion object {
        const val MAX_SIZE_RINGTONE = 64 * 1024
        private const val SELECT_RINGTONE_PATH = 40

        /**
         * Gets the name of a file without its extension
         *
         * @param fileName the name of the file
         * @return the base name
         */
        fun stripFileNameExtension(fileName: String): String {
            val index = fileName.lastIndexOf('.')
            return if (index == -1)
                fileName
            else
                fileName.substring(0, index)
        }

        private val TAG = RingtoneActivity::class.simpleName!!
    }
}