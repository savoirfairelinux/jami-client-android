package cx.ring.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import android.app.Service
import android.app.backup.BackupAgent
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
            assertEquals(listOf(Rule("include", "file", PortableSettingsBackup.RELATIVE_PATH, "deviceToDeviceTransfer")), entries)
        }
    }

    @Test
    fun modernResourcesDenyCloudAndAllowOnlyPortableSettings() {
        val cloud = rules(R.xml.data_extraction_rules, "cloud-backup")
        assertEquals(domains, cloud.map { it.domain }.toSet())
        assertTrue(cloud.all { it.action == "exclude" && it.path == "." })
        assertEquals(
            listOf(Rule("include", "file", PortableSettingsBackup.RELATIVE_PATH, null)),
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