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

class JamiBackupAgent : BackupAgent() {
    private var restoreFailed = false
    private var restoredFileCount: Int? = null

    override fun onCreate() {
        super.onCreate()
        synchronized(runtimeLock) {
            isActive = true
            generation++
        }
        PortableSettingsBackup.discardStaged(this)
        AccountDataBackup.discardStaged(this)
    }

    override fun onDestroy() {
        PortableSettingsBackup.discardStaged(this)
        AccountDataBackup.discardStaged(this)
        synchronized(runtimeLock) { isActive = false }
        super.onDestroy()
        (applicationContext as? JamiApplication)?.resumeAfterBackup()
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (!canExport(data.transportFlags)) return
        prepareExport()
        val snapshot = File(filesDir, PortableSettingsBackup.RELATIVE_PATH)
        val marker = File(filesDir, AccountDataBackup.MARKER_PATH)
        try {
            check(snapshot.parentFile!!.isDirectory || snapshot.parentFile!!.mkdirs())
            snapshot.writeBytes(PortableSettingsBackup.snapshot(this))
            fullBackupFile(snapshot, data)
            var fileCount = 0
            for (file in AccountDataBackup.transferEntries(this)) {
                fullBackupFile(file, data)
                if (file.isFile) fileCount++
            }
            marker.writeBytes(AccountDataBackup.marker(fileCount))
            fullBackupFile(marker, data)
        } finally {
            snapshot.delete()
            marker.delete()
        }
    }

    override fun onEstimateFullBackupBytes(quotaBytes: Long, transportFlags: Int): Long {
        if (!canExport(transportFlags)) return 0L
        prepareExport()
        var size = PortableSettingsBackup.snapshot(this).size.toLong()
        var fileCount = 0
        for (file in AccountDataBackup.transferFiles(this)) {
            size = Math.addExact(size, file.length())
            fileCount++
        }
        return Math.addExact(size, AccountDataBackup.marker(fileCount).size.toLong())
    }

    private fun prepareExport() {
        requireStoppedDaemon()
        AccountDataBackup.applyPending(this)
        PortableSettingsBackup.applyPending(this)
    }

    private fun requireStoppedDaemon() {
        check(isActive && (applicationContext as? JamiApplication)?.isDaemonRunning != true) {
            "Account transfer requires a stopped daemon"
        }
    }

    override fun onRestoreFile(
        data: ParcelFileDescriptor, size: Long, destination: File, type: Int, mode: Long, mtime: Long
    ) {
        requireStoppedDaemon()
        if (type !in setOf(TYPE_FILE, TYPE_DIRECTORY) || restoreFailed) return
        val expected = File(filesDir, PortableSettingsBackup.RELATIVE_PATH)
        val marker = File(filesDir, AccountDataBackup.MARKER_PATH)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(data.fileDescriptor)).use { input ->
                when (destination.canonicalFile) {
                    expected.canonicalFile -> {
                        require(type == TYPE_FILE)
                        PortableSettingsBackup.stage(this, input, size)
                    }
                    marker.canonicalFile -> {
                        require(type == TYPE_FILE)
                        require(restoredFileCount == null) { "Duplicate account transfer marker" }
                        restoredFileCount = AccountDataBackup.readMarker(input, size)
                    }
                    else -> {
                        val path = AccountDataBackup.restorePath(this, destination) ?: return
                        if (type == TYPE_DIRECTORY) {
                            require(size == 0L)
                            AccountDataBackup.stageDirectory(this, path)
                        } else {
                            AccountDataBackup.stage(this, path, input, size)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            restoreFailed = true
            PortableSettingsBackup.discardStaged(this)
            AccountDataBackup.discardStaged(this)
            Log.w(TAG, "Ignoring invalid device transfer (${exception.javaClass.simpleName})")
        }
    }

    override fun onRestoreFinished() {
        requireStoppedDaemon()
        if (!restoreFailed) {
            AccountDataBackup.finishRestore(this, restoredFileCount)
            PortableSettingsBackup.finishRestore(this)
        }
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput, newState: ParcelFileDescriptor) = Unit

    override fun onRestore(data: BackupDataInput, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    companion object {
        private const val TAG = "JamiBackupAgent"
        internal val runtimeLock = Any()
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