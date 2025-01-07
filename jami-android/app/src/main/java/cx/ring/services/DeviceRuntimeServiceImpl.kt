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
package cx.ring.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.provider.ContactsContract
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import androidx.core.content.ContextCompat
import cx.ring.application.JamiApplication
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.NetworkUtils
import net.jami.daemon.IntVect
import net.jami.daemon.StringVect
import net.jami.services.DeviceRuntimeService
import net.jami.services.LogService
import net.jami.utils.FileUtils
import net.jami.utils.StringUtils
import java.io.File
import java.util.Locale
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess

class DeviceRuntimeServiceImpl(
    private val mContext: Context,
    private val mExecutor: ScheduledExecutorService,
    private val logService: LogService
) : DeviceRuntimeService() {
    private fun copyAssets() {
        val extensionsPath = File(mContext.filesDir, "extensions")
        Log.w(TAG, "Extensions: " + extensionsPath.absolutePath)
        // Overwrite existing extensions folder in order to use newer extensions
        AndroidFileUtils.copyAssetFolder(mContext.assets, "extensions", extensionsPath)
    }

    override fun loadNativeLibrary() {
        logService.w(TAG, "loadNativeLibrary")
        Os.setenv("JAMI_LANG", Locale.getDefault().toString(), true)
        mExecutor.execute {
            Log.w(TAG, "System.loadLibrary")
            try {
                System.loadLibrary("jami-core-jni")
            } catch (e: Exception) {
                Log.e(TAG, "Could not load Jami library", e)
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
        }
    }

    override fun provideFilesDir(): File = mContext.filesDir

    override fun getFilePath(filename: String) = AndroidFileUtils.getFilePath(mContext, filename)

    override fun getConversationPath(accountId: String, conversationId: String, name: String): File =
        AndroidFileUtils.getConversationPath(mContext, accountId, conversationId, name)

    override fun getTemporaryPath(conversationId: String, name: String): File =
        AndroidFileUtils.getTempPath(mContext, conversationId, name)

    override fun getConversationDir(conversationId: String): File =
        AndroidFileUtils.getConversationDir(mContext, conversationId)

    override val cacheDir: File
        get() = mContext.cacheDir

    override val pushToken: String
        get() = JamiApplication.instance?.pushToken ?: ""
    override val pushPlatform: String
        get() = JamiApplication.instance?.pushPlatform ?: ""

    private fun isNetworkConnectedForType(connectivityManagerType: Int): Boolean {
        val info = NetworkUtils.getNetworkInfo(mContext)
        return info != null && info.isConnected && info.type == connectivityManagerType
    }

    override val isConnectedWifi: Boolean
        get() = isNetworkConnectedForType(ConnectivityManager.TYPE_WIFI)

    override val isConnectedBluetooth: Boolean
        get() = isNetworkConnectedForType(ConnectivityManager.TYPE_BLUETOOTH)

    override val isConnectedMobile: Boolean
        get() = isNetworkConnectedForType(ConnectivityManager.TYPE_MOBILE)

    override val isConnectedEthernet: Boolean
        get() = isNetworkConnectedForType(ConnectivityManager.TYPE_ETHERNET)

    override fun hasVideoPermission() = checkPermission(Manifest.permission.CAMERA)
    override fun hasAudioPermission() = checkPermission(Manifest.permission.RECORD_AUDIO)
    override fun hasContactPermission() = checkPermission(Manifest.permission.READ_CONTACTS)
    override fun hasCallLogPermission() = checkPermission(Manifest.permission.WRITE_CALL_LOG)
    override fun hasGalleryPermission() = checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

    override val profileName: String?
        get() {
            mContext.contentResolver.query(ContactsContract.Profile.CONTENT_URI, PROFILE_PROJECTION, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Profile.DISPLAY_NAME_PRIMARY))
                }
            }
            return null
        }

    override fun hardLinkOrCopy(source: File, dest: File): Boolean =
        try {
            Os.link(source.absolutePath, dest.absolutePath)
            true
        } catch (e: ErrnoException) {
            Log.w(TAG, "Can't create hardlink, copying instead: " + e.message)
            FileUtils.copyFile(source, dest)
        }

    private fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED

    override fun getHardwareAudioFormat(ret: IntVect) {
        var sampleRate = 44100
        var bufferSize = 64
        try {
            val am = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()
            bufferSize = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        } catch (e: Exception) {
            Log.w(javaClass.name, "Failed to read native OpenSL config", e)
        }
        ret.add(sampleRate)
        ret.add(bufferSize)
        Log.d(TAG, "getHardwareAudioFormat: $sampleRate $bufferSize")
    }

    override fun getAppDataPath(name: String, ret: StringVect) {
        when (name) {
            "files" -> ret.add(mContext.filesDir.absolutePath)
            "cache" -> ret.add(mContext.cacheDir.absolutePath)
            else -> ret.add(mContext.getDir(name, Context.MODE_PRIVATE).absolutePath)
        }
    }

    override fun getDeviceName(ret: StringVect) {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer)) {
            ret.add(StringUtils.capitalize(model))
        } else {
            ret.add("${StringUtils.capitalize(manufacturer)} $model")
        }
    }

    companion object {
        private val TAG = DeviceRuntimeServiceImpl::class.simpleName!!
        private val PROFILE_PROJECTION = arrayOf(
            ContactsContract.Profile._ID,
            ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
            ContactsContract.Profile.PHOTO_ID
        )
    }
}