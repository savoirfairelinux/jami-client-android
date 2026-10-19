package cx.ring.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import cx.ring.application.JamiApplication
import java.io.File
import java.io.FileInputStream

class JamiBackupAgent : BackupAgent() {
    private var restoreFailed = false

    override fun onCreate() {
        super.onCreate()
        isActive = true
        generation++
        PortableSettingsBackup.discardStaged(this)
    }

    override fun onDestroy() {
        PortableSettingsBackup.discardStaged(this)
        isActive = false
        super.onDestroy()
        (applicationContext as? JamiApplication)?.resumeAfterBackup()
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (!canExport(data.transportFlags)) return
        val snapshot = File(filesDir, PortableSettingsBackup.RELATIVE_PATH)
        try {
            check(snapshot.parentFile!!.isDirectory || snapshot.parentFile!!.mkdirs())
            snapshot.writeBytes(PortableSettingsBackup.snapshot(this))
            fullBackupFile(snapshot, data)
        } finally {
            snapshot.delete()
        }
    }

    override fun onEstimateFullBackupBytes(quotaBytes: Long, transportFlags: Int): Long =
        if (canExport(transportFlags)) PortableSettingsBackup.snapshot(this).size.toLong() else 0L

    override fun onRestoreFile(
        data: ParcelFileDescriptor, size: Long, destination: File, type: Int, mode: Long, mtime: Long
    ) {
        val expected = File(filesDir, PortableSettingsBackup.RELATIVE_PATH)
        if (type != TYPE_FILE || destination.canonicalFile != expected.canonicalFile) {
            Log.w(TAG, "Ignoring data outside the portable settings allowlist")
            return
        }
        if (restoreFailed) return
        try {
            PortableSettingsBackup.stage(this, FileInputStream(data.fileDescriptor), size)
        } catch (exception: Exception) {
            restoreFailed = true
            PortableSettingsBackup.discardStaged(this)
            Log.w(TAG, "Ignoring invalid portable settings restore (${exception.javaClass.simpleName})")
        }
    }

    override fun onRestoreFinished() {
        if (!restoreFailed) PortableSettingsBackup.finishRestore(this)
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput, newState: ParcelFileDescriptor) = Unit

    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    companion object {
        private const val TAG = "JamiBackupAgent"
        internal fun canExport(transportFlags: Int): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                transportFlags and FLAG_DEVICE_TO_DEVICE_TRANSFER != 0 &&
                transportFlags and FLAG_CROSS_PLATFORM_DATA_TRANSFER_IOS == 0

        @Volatile
        var isActive = false
            private set
        @Volatile
        internal var generation = 0L
            private set

        internal fun canStartRuntime(expectedGeneration: Long): Boolean =
            !isActive && generation == expectedGeneration
    }
}