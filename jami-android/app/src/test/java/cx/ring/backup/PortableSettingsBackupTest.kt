package cx.ring.backup

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.Service
import android.app.backup.BackupAgent
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.os.Build
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.service.DRingService
import cx.ring.service.SyncService
import cx.ring.service.CallNotificationService
import cx.ring.service.PeerTunnelForegroundService
import cx.ring.service.LocationSharingService
import cx.ring.service.PushForegroundService
import cx.ring.services.DataTransferService
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.File
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PortableSettingsBackupTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        File(context.noBackupFilesDir, "restore").deleteRecursively()
        context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun snapshotExcludesDeviceStateAndSecrets() {
        context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("persistent_typing", false)
            .putBoolean("log_is_active", true)
            .putBoolean("on_startup", true)
            .putBoolean("push_notifs", true)
            .putString("password", "must-not-travel")
            .commit()
        val snapshot = PortableSettingsBackup.validate(PortableSettingsBackup.snapshot(context))
        val settings = snapshot.getJSONObject("settings")
        assertEquals(setOf("persistent_typing"), settings.keys().asSequence().toSet())
        assertFalse(settings.getBoolean("persistent_typing"))
    }

    @Test
    fun accountFilesKeepArchivesAndConversationsButExcludeDeviceCredentials() {
        val directory = File(context.cacheDir, "account-transfer").apply { mkdirs() }
        val paths = setOf(
            "0123456789abcdef/archive.gz", "0123456789abcdef/config.yml",
            "0123456789abcdef/ring_device.key", "0123456789abcdef/ring_device.crt",
            "0123456789abcdef/conversations/conversation/.git/objects/pack/history.pack",
            "0123456789abcdef/conversation_data/conversation/attachment",
            "0123456789abcdef/history.db", "0123456789abcdef/history.db-wal",
            "0123456789abcdef/profiles/contact.vcf"
        )
        for (relativePath in paths) {
            File(directory, relativePath).apply { parentFile!!.mkdirs(); writeText(relativePath) }
        }
        java.nio.file.Files.createSymbolicLink(File(directory, "identity-link").toPath(),
            File(directory, "0123456789abcdef/ring_device.key").toPath())
        assertEquals(paths.filterNot { AccountDataBackup.isDeviceCredential(it) }.toSet(),
            AccountDataBackup.files(directory).map { it.relativeTo(directory).invariantSeparatorsPath }.toSet())
    }

    @Test
    @Config(sdk = [28, 34])
    fun accountRestorePublishesOnlyCompleteDataWithoutDeviceIdentity() {
        val account = File(context.filesDir, "0123456789abcdef").apply { mkdirs() }
        File(account, "ring_device.key").writeText("old-device-key")
        File(account, "ring_device.crt").writeText("old-device-certificate")
        val archive = File(account, "archive.gz").apply { writeText("old-archive") }
        val history = File(account, "conversations/conversation/.git/objects/pack/history.pack")
        val config = File(context.getDir("config", Context.MODE_PRIVATE), "dring.yml")
        val agent = agent()
        agent.onCreate()
        try {
            restore(agent, archive, "password-protected-account-archive".toByteArray())
            restore(agent, history, "conversation-history".toByteArray())
            restore(agent, File(account, "conversation_data/conversation/attachment"), byteArrayOf(0, 1, 2))
            restore(agent, File(account, "history.db"), "database".toByteArray())
            restore(agent, File(account, "history.db-wal"), "write-ahead-log".toByteArray())
            restore(agent, config, "account-order".toByteArray())
            restore(agent, File(account, "conversations/conversation/.git/refs/heads"), byteArrayOf(), type = BackupAgent.TYPE_DIRECTORY)
            restore(agent, File(account, "ring_device.key"), "source-device-key".toByteArray())
            restore(agent, File(account, "ring_device.crt"), "source-device-certificate".toByteArray())
            restore(agent, File(context.filesDir, AccountDataBackup.MARKER_PATH), AccountDataBackup.marker(6))
            AccountDataBackup.applyPending(context)
            assertEquals("old-archive", archive.readText())
            assertFalse(history.exists())
            agent.onRestoreFinished()
            assertTrue(AccountDataBackup.hasPending(context))
            assertEquals("old-archive", archive.readText())
        } finally {
            agent.onDestroy()
        }
        AccountDataBackup.applyPending(context)
        assertFalse(AccountDataBackup.hasPending(context))
        assertEquals("password-protected-account-archive", archive.readText())
        assertEquals("conversation-history", history.readText())
        assertTrue(File(account, "conversations/conversation/.git/refs/heads").isDirectory)
        assertEquals("account-order", config.readText())
        assertEquals("write-ahead-log", File(account, "history.db-wal").readText())
        assertArrayEquals(byteArrayOf(0, 1, 2), File(account, "conversation_data/conversation/attachment").readBytes())
        assertFalse(File(account, "ring_device.key").exists())
        assertFalse(File(account, "ring_device.crt").exists())
        AccountDataBackup.applyPending(context)
        assertEquals("conversation-history", history.readText())
    }

    @Test
    fun incompleteAccountRestoreDoesNotReplaceLiveData() {
        val archive = File(context.filesDir, "a/archive.gz").apply { parentFile!!.mkdirs(); writeText("live") }
        val agent = agent()
        agent.onCreate()
        try {
            restore(agent, archive, "incomplete".toByteArray())
        } finally {
            agent.onDestroy()
        }
        AccountDataBackup.applyPending(context)
        assertEquals("live", archive.readText())
        assertFalse(AccountDataBackup.hasPending(context))
    }

    @Test
    fun legacyAccountRestoreWithoutMarkerIsDiscarded() {
        val archive = File(context.filesDir, "a/archive.gz")
        val agent = agent()
        agent.onCreate()
        try {
            restore(agent, archive, "legacy".toByteArray())
            agent.onRestoreFinished()
        } finally {
            agent.onDestroy()
        }
        AccountDataBackup.applyPending(context)
        assertFalse(archive.exists())
        assertFalse(AccountDataBackup.hasPending(context))
    }

    @Test
    fun missingAccountFilesFailCompletionWithoutPublishingSettings() {
        val agent = agent()
        agent.onCreate()
        try {
            restore(agent, File(context.filesDir, "a/archive.gz"), "archive".toByteArray())
            restore(agent, File(context.filesDir, PortableSettingsBackup.RELATIVE_PATH), payload())
            restore(agent, File(context.filesDir, AccountDataBackup.MARKER_PATH), AccountDataBackup.marker(2))
            assertThrows(IllegalArgumentException::class.java) { agent.onRestoreFinished() }
            assertFalse(AccountDataBackup.hasPending(context))
            assertFalse(PortableSettingsBackup.applyPending(context))
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    fun truncatedOrDuplicateAccountFilesInvalidateTheRestore() {
        for (duplicate in listOf(false, true)) {
            val agent = agent()
            agent.onCreate()
            try {
                val archive = File(context.filesDir, "a/archive.gz")
                restore(agent, archive, "archive".toByteArray(), if (duplicate) 7 else 8)
                if (duplicate) restore(agent, archive, "duplicate".toByteArray())
                restore(agent, File(context.filesDir, AccountDataBackup.MARKER_PATH), AccountDataBackup.marker(1))
                agent.onRestoreFinished()
                assertFalse(AccountDataBackup.hasPending(context))
                assertFalse(archive.exists())
            } finally {
                agent.onDestroy()
            }
        }
    }

    @Test
    fun accountRestoreRejectsTraversalAndSymbolicLinks() {
        val account = File(context.filesDir, "a").apply { mkdirs() }
        java.nio.file.Files.createSymbolicLink(File(account, "outside").toPath(), context.cacheDir.toPath())
        val rejected = listOf(
            File(account, "ring_device.key"), File(account, "ring_device.crt"),
            File(account, "outside/archive.gz"), File(account, "../archive.gz"),
            File(context.dataDir, "shared_prefs/biometric_a.xml"), File(context.filesDir, "firebaselog.txt")
        )
        for (destination in rejected) assertNull(AccountDataBackup.restorePath(context, destination))
        assertEquals("files/a/archive.gz", AccountDataBackup.restorePath(context, File(account, "archive.gz")))
        assertEquals("config/dring.yml", AccountDataBackup.restorePath(context,
            File(context.getDir("config", Context.MODE_PRIVATE), "dring.yml")))
    }

    @Test
    fun accountRestoreAcceptsCanonicalDestinationsWhenContextRootIsAnAlias() {
        val alias = File(context.cacheDir, "files-alias")
        java.nio.file.Files.createSymbolicLink(alias.toPath(), context.filesDir.toPath())
        val aliasedContext = object : ContextWrapper(context) {
            override fun getFilesDir(): File = alias
        }
        assertEquals("files/a/archive.gz", AccountDataBackup.restorePath(aliasedContext,
            File(context.filesDir.canonicalFile, "a/archive.gz")))
        assertEquals("files/a/archive.gz", AccountDataBackup.restorePath(aliasedContext,
            File(alias, "a/archive.gz")))
        assertNull(AccountDataBackup.restorePath(aliasedContext, File(alias, "a/../archive.gz")))
    }

    @Test
    fun accountTransferSizeIncludesConfigAndHistoryButNotDeviceKeysOrLogs() {
        val account = File(context.filesDir, "a").apply { mkdirs() }
        File(account, "archive.gz").writeText("archive")
        File(account, "ring_device.key").writeText("device-private-key")
        File(account, "ring_device.crt").writeText("device-certificate")
        File(account, "history.db").writeText("history")
        File(context.filesDir, "firebaselog.txt").writeText("push-token")
        File(context.getDir("config", Context.MODE_PRIVATE), "dring.yml").writeText("config")
        val agent = agent()
        agent.onCreate()
        try {
            assertEquals(3, AccountDataBackup.transferFiles(context).count())
            assertEquals(PortableSettingsBackup.snapshot(context).size.toLong() + 20 + AccountDataBackup.marker(3).size,
                agent.onEstimateFullBackupBytes(Long.MAX_VALUE, BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER))
            assertEquals(0L, agent.onEstimateFullBackupBytes(Long.MAX_VALUE, 0))
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    fun exportReplaysAnAlreadyCompletedRestoreBeforeMeasuringData() {
        val bytes = "restored-account-archive".toByteArray()
        AccountDataBackup.stage(context, "files/a/archive.gz", ByteArrayInputStream(bytes), bytes.size.toLong())
        AccountDataBackup.finishRestore(context, 1)
        val settings = payload()
        PortableSettingsBackup.stage(context, ByteArrayInputStream(settings), settings.size.toLong())
        PortableSettingsBackup.finishRestore(context)
        val agent = agent()
        agent.onCreate()
        try {
            val size = agent.onEstimateFullBackupBytes(Long.MAX_VALUE, BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER)
            assertFalse(AccountDataBackup.hasPending(context))
            assertEquals("restored-account-archive", File(context.filesDir, "a/archive.gz").readText())
            assertFalse(context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE)
                .getBoolean("persistent_typing", true))
            assertEquals(bytes.size.toLong() + PortableSettingsBackup.snapshot(context).size + AccountDataBackup.marker(1).size, size)
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    @Config(sdk = [28, 34], shadows = [BackupStream::class])
    fun agentStreamsOnlyAccountDataAndNeverExportsDeviceCredentialsOrCloudData() {
        val account = File(context.filesDir, "a").apply { mkdirs() }
        File(account, "archive.gz").writeText("account-archive")
        File(account, "ring_device.key").writeText("device-private-key")
        File(account, "ring_device.crt").writeText("device-certificate")
        val refs = File(account, "conversations/conversation/.git/refs/heads").apply { mkdirs() }
        File(context.filesDir, "firebaselog.txt").writeText("push-token")
        val config = File(context.getDir("config", Context.MODE_PRIVATE), "dring.yml").apply { writeText("config") }
        val agent = agent()
        agent.onCreate()
        try {
            for (flags in listOf(0, BackupAgent.FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED,
                BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER or BackupAgent.FLAG_CROSS_PLATFORM_DATA_TRANSFER_IOS)) {
                agent.onFullBackup(output(flags))
                assertTrue(BackupStream.entries.isEmpty())
            }
            agent.onFullBackup(output(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER))
            val exported = BackupStream.entries.map { File(it.path) }.toSet()
            assertTrue(File(account, "archive.gz").canonicalFile in exported)
            assertTrue(refs.canonicalFile in exported)
            assertTrue(config.canonicalFile in exported)
            assertFalse(exported.any { it.name in setOf("ring_device.key", "ring_device.crt", "firebaselog.txt") })
            val marker = BackupStream.entries.single { it.path.endsWith(AccountDataBackup.MARKER_PATH) }.bytes!!
            assertEquals(2, AccountDataBackup.readMarker(ByteArrayInputStream(marker), marker.size.toLong()))
            assertTrue(File(account, "ring_device.key").exists())
            assertTrue(File(account, "ring_device.crt").exists())
            assertFalse(File(context.filesDir, AccountDataBackup.MARKER_PATH).exists())
            assertFalse(File(context.filesDir, PortableSettingsBackup.RELATIVE_PATH).exists())
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    fun runningDaemonRejectsAccountBackupRestoreAndReplay() {
        val application = application().apply { isDaemonRunning = true }
        val agent = agent(application)
        agent.onCreate()
        try {
            assertThrows(IllegalStateException::class.java) {
                agent.onEstimateFullBackupBytes(Long.MAX_VALUE, BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER)
            }
            assertThrows(IllegalStateException::class.java) {
                agent.onFullBackup(output(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER))
            }
            assertThrows(IllegalStateException::class.java) {
                restore(agent, File(context.filesDir, "a/archive.gz"), "archive".toByteArray())
            }
            assertThrows(IllegalStateException::class.java) { agent.onRestoreFinished() }
        } finally {
            agent.onDestroy()
        }
        AccountDataBackup.stage(context, "files/a/archive.gz", ByteArrayInputStream(byteArrayOf(1)), 1)
        AccountDataBackup.finishRestore(context, 1)
        assertFalse(application.prepareRuntime())
        assertTrue(AccountDataBackup.hasPending(context))
        assertFalse(File(context.filesDir, "a/archive.gz").exists())
    }

    @Test
    fun invalidAccountMarkersAreRejected() {
        for (value in listOf(
            "{}", """{"version":2,"files":0}""", """{"version":1,"files":-1}""",
            """{"version":1,"files":"1"}""", """{"version":1,"files":0} trailing"""
        )) {
            assertThrows(Exception::class.java) {
                AccountDataBackup.readMarker(ByteArrayInputStream(value.toByteArray()), value.toByteArray().size.toLong())
            }
        }
    }

    @Test
    fun restoreOnlyAppliesAfterCompletionAndIsIdempotent() {
        val bytes = payload()
        PortableSettingsBackup.stage(context, ByteArrayInputStream(bytes), bytes.size.toLong())
        assertFalse(PortableSettingsBackup.applyPending(context))
        PortableSettingsBackup.finishRestore(context)
        assertTrue(PortableSettingsBackup.applyPending(context))
        assertFalse(PortableSettingsBackup.applyPending(context))
        assertFalse(context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE)
            .getBoolean("persistent_typing", true))
        assertTrue(context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            .getBoolean("darkMode", false))
    }

    @Test
    fun interruptedRestoreDoesNotChangePreferences() {
        val bytes = payload()
        PortableSettingsBackup.stage(context, ByteArrayInputStream(bytes), bytes.size.toLong())
        PortableSettingsBackup.discardStaged(context)
        PortableSettingsBackup.finishRestore(context)
        assertFalse(PortableSettingsBackup.applyPending(context))
    }

    @Test
    fun restorePreservesDeviceLocalSettings() {
        context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("push_notifs", true).commit()
        val bytes = payload()
        PortableSettingsBackup.stage(context, ByteArrayInputStream(bytes), bytes.size.toLong())
        PortableSettingsBackup.finishRestore(context)
        PortableSettingsBackup.applyPending(context)
        assertTrue(context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE)
            .getBoolean("push_notifs", false))
    }

    @Test
    fun rejectsUnknownVersionsKeysAndTypes() {
        val invalid = listOf(
            """{"version":2,"settings":{},"theme":{}}""",
            """{"version":1,"settings":{"password":"secret"},"theme":{}}""",
            """{"version":1,"settings":{"persistent_typing":"true"},"theme":{}}""",
            """{"version":1,"settings":{"persistent_notification":3},"theme":{}}""",
            """{"version":1,"settings":{},"theme":{"darkMode":1}}""",
            """{"version":1,"settings":{},"theme":{},"account":"secret"}""",
            """{"version":1,"settings":{},"theme":{}} trailing"""
        )
        for (json in invalid) {
            assertThrows(Exception::class.java) { PortableSettingsBackup.validate(json.toByteArray()) }
        }
    }

    @Test
    fun rejectsOversizedAndTruncatedPayloads() {
        assertThrows(IllegalArgumentException::class.java) {
            PortableSettingsBackup.stage(context, ByteArrayInputStream(byteArrayOf()), PortableSettingsBackup.MAX_BYTES + 1L)
        }
        assertThrows(EOFException::class.java) {
            PortableSettingsBackup.stage(context, ByteArrayInputStream(byteArrayOf()), 10)
        }
    }

    @Test
    fun applicationCreationDoesNotInitializeRuntime() {
        val application = application()
        assertFalse(application.isRuntimeReady)
    }

    @Test
    fun backupAgentBlocksRuntimeInitialization() {
        val agent = JamiBackupAgent()
        ReflectionHelpers.callInstanceMethod<Void>(agent, "attachBaseContext", ClassParameter.from(Context::class.java, context))
        agent.onCreate()
        try {
            val application = application()
            assertFalse(application.prepareRuntime())
            assertFalse(application.isRuntimeReady)
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    fun backupInvalidatesPreviouslyQueuedRuntimeStarts() {
        val generation = JamiBackupAgent.generation
        assertTrue(JamiBackupAgent.canStartRuntime(generation))
        val agent = JamiBackupAgent()
        ReflectionHelpers.callInstanceMethod<Void>(agent, "attachBaseContext", ClassParameter.from(Context::class.java, context))
        agent.onCreate()
        try {
            assertFalse(JamiBackupAgent.canStartRuntime(generation))
            assertFalse(JamiBackupAgent.canStartRuntime(JamiBackupAgent.generation))
        } finally {
            agent.onDestroy()
        }
        assertFalse(JamiBackupAgent.canStartRuntime(generation))
        assertTrue(JamiBackupAgent.canStartRuntime(JamiBackupAgent.generation))
    }

    @Test
    @Config(sdk = [28, 34])
    fun restrictedServicesSkipInjectionPromoteAndStopSafely() {
        val serviceClasses = listOf(
            DRingService::class.java, SyncService::class.java, CallNotificationService::class.java,
            PeerTunnelForegroundService::class.java, LocationSharingService::class.java,
            DataTransferService::class.java, PushForegroundService::class.java
        )
        for (serviceClass in serviceClasses) {
            val controller = Robolectric.buildService(serviceClass).create()
            val service = controller.get()
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(Intent(), 0, 1))
            assertNotEquals(0, shadowOf(service).lastForegroundNotificationId)
            assertNull(shadowOf(service).lastForegroundNotification)
            assertTrue(shadowOf(service).isStoppedBySelf)
            controller.destroy()
        }
    }

    @Test
    fun onlyDeviceTransferMayExportSettings() {
        assertFalse(JamiBackupAgent.canExport(0))
        assertFalse(JamiBackupAgent.canExport(BackupAgent.FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED))
        assertFalse(JamiBackupAgent.canExport(BackupAgent.FLAG_CROSS_PLATFORM_DATA_TRANSFER_IOS))
        assertFalse(JamiBackupAgent.canExport(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER or BackupAgent.FLAG_CROSS_PLATFORM_DATA_TRANSFER_IOS))
        assertTrue(JamiBackupAgent.canExport(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER))
    }

    @Test
    fun secretNotificationPreferenceRoundTrips() {
        context.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit()
            .putInt("persistent_notification", 2).commit()
        val snapshot = PortableSettingsBackup.validate(PortableSettingsBackup.snapshot(context))
        assertEquals(2, snapshot.getJSONObject("settings").getInt("persistent_notification"))
    }

    @Test
    fun agentRejectsLegacyPathsAndMalformedSnapshotsWithoutCrashing() {
        val agent = JamiBackupAgent()
        ReflectionHelpers.callInstanceMethod<Void>(agent, "attachBaseContext", ClassParameter.from(Context::class.java, context))
        agent.onCreate()
        try {
            val source = File(context.cacheDir, "restore-input").apply { writeBytes(payload()) }
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { input ->
                agent.onRestoreFile(input, source.length(), File(context.filesDir, "ring_device"), BackupAgent.TYPE_FILE, 0, 0)
            }
            agent.onRestoreFinished()
            assertFalse(PortableSettingsBackup.applyPending(context))
            source.writeText("invalid")
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { input ->
                agent.onRestoreFile(input, source.length(), File(context.filesDir, PortableSettingsBackup.RELATIVE_PATH), BackupAgent.TYPE_FILE, 0, 0)
            }
            agent.onRestoreFinished()
            assertFalse(PortableSettingsBackup.applyPending(context))
        } finally {
            agent.onDestroy()
        }
    }

    @Test
    fun agentStagesValidSettingsUntilRestoreFinishes() {
        val agent = JamiBackupAgent()
        ReflectionHelpers.callInstanceMethod<Void>(agent, "attachBaseContext", ClassParameter.from(Context::class.java, context))
        agent.onCreate()
        try {
            val source = File(context.cacheDir, "restore-input").apply { writeBytes(payload()) }
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { input ->
                agent.onRestoreFile(input, source.length(), File(context.filesDir, PortableSettingsBackup.RELATIVE_PATH), BackupAgent.TYPE_FILE, 0, 0)
            }
            assertFalse(PortableSettingsBackup.applyPending(context))
            agent.onRestoreFinished()
        } finally {
            agent.onDestroy()
        }
        assertTrue(PortableSettingsBackup.applyPending(context))
    }

    @Test
    @Config(sdk = [26, 28, 34])
    fun legacyResourcesOnlyAllowFlaggedDeviceTransfer() {
        val entries = rules(R.xml.backup_rules, "full-backup-content")
        if (Build.VERSION.SDK_INT < 28) {
            assertEquals(domains, entries.map { it.domain }.toSet())
            assertTrue(entries.all { it.action == "exclude" && it.path == "." })
            assertFalse(JamiBackupAgent.canExport(BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER))
        } else {
            assertEquals(listOf(
                Rule("include", "file", ".", "deviceToDeviceTransfer"),
                Rule("include", "root", "app_config", "deviceToDeviceTransfer")
            ), entries)
        }
    }

    @Test
    fun modernResourcesDenyCloudAndAllowDeviceData() {
        val cloud = rules(R.xml.data_extraction_rules, "cloud-backup")
        assertEquals(domains, cloud.map { it.domain }.toSet())
        assertTrue(cloud.all { it.action == "exclude" && it.path == "." })
        assertEquals(
            listOf(Rule("include", "file", ".", null), Rule("include", "root", "app_config", null)),
            rules(R.xml.data_extraction_rules, "device-transfer")
        )
    }

    private data class Rule(val action: String, val domain: String?, val path: String?, val flags: String?)

    private val domains = setOf(
        "root", "file", "database", "sharedpref", "external", "device_root", "device_file",
        "device_database", "device_sharedpref"
    )

    private fun rules(resourceId: Int, section: String): List<Rule> {
        val entries = mutableListOf<Rule>()
        context.resources.getXml(resourceId).use { parser ->
            var insideSection = false
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name == section) insideSection = true
                    else if (insideSection && parser.name in setOf("include", "exclude")) {
                        entries.add(Rule(
                            parser.name, parser.getAttributeValue(null, "domain"),
                            parser.getAttributeValue(null, "path"), parser.getAttributeValue(null, "requireFlags")
                        ))
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == section) {
                    insideSection = false
                }
            }
        }
        return entries
    }

    class TestApplication : JamiApplication() {
        override val pushToken: Pair<String, String>? = null
        override val pushPlatform = "test"
        override var isDaemonRunning = false
        override fun getApplicationContext(): Context = this
    }

    @Implements(BackupAgent::class)
    class BackupStream {
        data class Entry(val path: String, val bytes: ByteArray?)

        @Implementation
        @Suppress("UNUSED_PARAMETER")
        fun fullBackupFile(file: File, output: FullBackupDataOutput) {
            entries.add(Entry(file.canonicalPath, if (file.isFile) file.readBytes() else null))
        }

        companion object {
            val entries = mutableListOf<Entry>()

            @JvmStatic
            @Resetter
            fun reset() = entries.clear()
        }
    }

    private fun output(flags: Int): FullBackupDataOutput = ReflectionHelpers.callConstructor(
        FullBackupDataOutput::class.java,
        ClassParameter.from(Long::class.javaPrimitiveType!!, Long.MAX_VALUE),
        ClassParameter.from(Int::class.javaPrimitiveType!!, flags)
    )

    private fun agent(baseContext: Context = context) = JamiBackupAgent().also { agent ->
        ReflectionHelpers.callInstanceMethod<Void>(agent, "attachBaseContext", ClassParameter.from(Context::class.java, baseContext))
    }

    private fun restore(
        agent: JamiBackupAgent, destination: File, bytes: ByteArray,
        size: Long = bytes.size.toLong(), type: Int = BackupAgent.TYPE_FILE
    ) {
        val source = File(context.cacheDir, "restore-input").apply { writeBytes(bytes) }
        ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { input ->
            agent.onRestoreFile(input, size, destination, type, 0, 0)
        }
    }

    private fun application() = TestApplication().also { application ->
        ReflectionHelpers.callInstanceMethod<Void>(application, "attachBaseContext", ClassParameter.from(Context::class.java, context))
        application.onCreate()
    }

    private fun payload() = JSONObject().put("version", 1)
        .put("settings", JSONObject().put("persistent_typing", false))
        .put("theme", JSONObject().put("darkMode", true))
        .toString().toByteArray()
}