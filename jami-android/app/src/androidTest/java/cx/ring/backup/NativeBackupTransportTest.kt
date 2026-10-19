package cx.ring.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import cx.ring.application.JamiApplication
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Conversation
import net.jami.model.interaction.TextMessage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Run createSource, then a system transport backup / clear / restore, then verifyRestore.
 * Pull backup-oracle.json from no_backup before clearing data and pass it as the oracle argument.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class NativeBackupTransportTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.targetContext.applicationContext as JamiApplication
    private val oracleFile = File(application.noBackupFilesDir, "backup-oracle.json")
    private val sourceDatabases = mutableListOf<SQLiteDatabase>()

    @Before
    fun requireTransportHarness() {
        assumeTrue("Requires the external BackupManager round-trip harness",
            InstrumentationRegistry.getArguments().getString("backupTransport") == "true")
    }

    @Test
    fun coldStartup() {
        assertFalse(application.isRuntimeReady)
        assertFalse(application.daemon.isStarted)
    }

    @Test
    fun createSource() {
        startRuntime()
        assertTrue(application.mAccountService.observableAccountList
            .firstOrError().timeout(30, TimeUnit.SECONDS).blockingGet().isEmpty())
        val accounts = JSONArray()
        for (protected in listOf(false, true)) {
            val details = application.mAccountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
                .timeout(30, TimeUnit.SECONDS).blockingGet()
            details[ConfigKey.ACCOUNT_ALIAS.key] = if (protected) "Backup protected" else "Backup unprotected"
            if (protected) details[ConfigKey.ARCHIVE_PASSWORD.key] = PASSWORD
            val account = application.mAccountService.addAccount(details)
                .filter { it.registrationState != AccountConfig.RegistrationState.INITIALIZING }
                .firstOrError().timeout(120, TimeUnit.SECONDS).blockingGet()
            await("Account identity was not created") { account.deviceId.isNotEmpty() && !account.username.isNullOrEmpty() }
            val conversation = application.mAccountService.startConversation(account.accountId, emptyList())
                .timeout(30, TimeUnit.SECONDS).blockingGet()
            val text = "Native backup round trip: protected=$protected"
            application.mAccountService.sendConversationMessage(account.accountId, conversation.uri, text, null)
            await("Native message was not committed") {
                conversation.sortedHistory.blockingGet().filterIsInstance<TextMessage>().any { it.body == text }
            }
            val attachment = File(application.filesDir,
                "${account.accountId}/conversation_data/${conversation.uri.rawRingId}/transport-attachment.bin")
            check(attachment.parentFile!!.isDirectory || attachment.parentFile!!.mkdirs())
            attachment.writeBytes(ATTACHMENT)
            val database = SQLiteDatabase.openOrCreateDatabase(
                File(application.filesDir, "${account.accountId}/backup-history.db"), null)
            sourceDatabases.add(database)
            assertTrue(database.enableWriteAheadLogging())
            database.rawQuery("PRAGMA wal_autocheckpoint=0", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            database.execSQL("CREATE TABLE history (message TEXT NOT NULL)")
            database.execSQL("INSERT INTO history VALUES (?)", arrayOf(text))
            assertTrue(File(application.filesDir, "${account.accountId}/backup-history.db-wal").isFile)
            assertTrue(File(application.filesDir, "${account.accountId}/archive.gz").isFile)
            accounts.put(JSONObject()
                .put("account", account.accountId)
                .put("username", account.username)
                .put("device", account.deviceId)
                .put("conversation", conversation.uri.rawRingId)
                .put("message", text)
                .put("protected", protected))
        }
        assertTrue(application.getSharedPreferences("ring_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("persistent_typing", false)
            .putBoolean("push_notifs", false)
            .putString("backup_test_local_only", "must-not-transfer")
            .commit())
        assertTrue(application.getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
            .edit().putBoolean("darkMode", true).commit())
        File(application.filesDir, "backup-test-local-only.txt").writeText("must-not-transfer")
        oracleFile.writeText(accounts.toString())
        assertEquals(2, accounts.length())
    }

    @Test
    fun verifyRestore() {
        val accounts = JSONArray(requireNotNull(InstrumentationRegistry.getArguments().getString("oracle")))
        assertFalse("Backup process must not start the native daemon", application.daemon.isStarted)
        assertFalse(File(application.filesDir, "backup-test-local-only.txt").exists())
        // Verify transport staging before any runtime demand can replay it.
        val pending = File(application.noBackupFilesDir, "restore/accounts-pending")
        assertTrue("System restore did not publish account data", pending.isDirectory)
        for (index in 0 until accounts.length()) {
            val account = accounts.getJSONObject(index)
            val id = account.getString("account")
            assertFalse(File(pending, "files/$id/ring_device.key").exists())
            assertFalse(File(pending, "files/$id/ring_device.crt").exists())
            assertTrue(File(pending, "files/$id/archive.gz").isFile)
            assertArrayEquals(ATTACHMENT, File(pending,
                "files/$id/conversation_data/${account.getString("conversation")}/transport-attachment.bin").readBytes())
        }
        startRuntime()
        assertFalse("Restore replay must finish before startup", pending.exists())
        val preferences = application.getSharedPreferences("ring_settings", Context.MODE_PRIVATE)
        assertFalse(preferences.getBoolean("persistent_typing", true))
        assertFalse(preferences.contains("backup_test_local_only"))
        assertFalse("Device-local push setting must not transfer", preferences.contains("push_notifs"))
        assertTrue(application.getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
            .getBoolean("darkMode", false))

        for (index in 0 until accounts.length()) {
            val expected = accounts.getJSONObject(index)
            val id = expected.getString("account")
            await("Restored account $id was not loaded") { application.mAccountService.getAccount(id) != null }
            val account = application.mAccountService.getAccount(id)!!
            if (expected.getBoolean("protected")) {
                await("Protected archive must require its password") { account.needsMigration() }
                assertEquals("INVALID", application.mAccountService.migrateAccount(id, "wrong-password")
                    .timeout(120, TimeUnit.SECONDS).blockingGet())
                await("Wrong password must leave migration blocked") { account.needsMigration() }
                assertFalse(File(application.filesDir, "$id/ring_device.key").exists())
                assertEquals("SUCCESS", application.mAccountService.migrateAccount(id, PASSWORD)
                    .timeout(120, TimeUnit.SECONDS).blockingGet())
            }
            await("Restored device identity was not regenerated") {
                account.deviceId.isNotEmpty() && !account.needsMigration()
            }
            assertEquals(expected.getString("username"), account.username)
            assertNotEquals("Source device identity was reused", expected.getString("device"), account.deviceId)
            assertTrue(File(application.filesDir, "$id/ring_device.key").isFile)
            assertTrue(File(application.filesDir, "$id/ring_device.crt").isFile)
            val conversationId = expected.getString("conversation")
            await("Restored conversation was not loaded") { account.getSwarm(conversationId) != null }
            val conversation = account.getSwarm(conversationId)!!
            await("Restored conversation is still syncing") {
                conversation.mode.firstOrError().blockingGet() !in
                    setOf(Conversation.Mode.Syncing, Conversation.Mode.Request)
            }
            val messages = application.mAccountService.loadMore(conversation)
                .timeout(30, TimeUnit.SECONDS).blockingGet().sortedHistory.blockingGet()
            assertTrue("Native daemon could not read the restored message",
                messages.filterIsInstance<TextMessage>().any { it.body == expected.getString("message") })
            assertArrayEquals(ATTACHMENT, File(application.filesDir,
                "$id/conversation_data/$conversationId/transport-attachment.bin").readBytes())
            SQLiteDatabase.openDatabase(File(application.filesDir, "$id/backup-history.db").path,
                null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery("SELECT message FROM history", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(expected.getString("message"), cursor.getString(0))
                    assertFalse(cursor.moveToNext())
                }
            }
        }
    }

    private fun startRuntime() {
        instrumentation.runOnMainSync { application.bootstrapDaemon() }
        await("Native daemon did not start") { application.daemon.isStarted }
        application.mAccountService.observableAccountList.firstOrError()
            .timeout(30, TimeUnit.SECONDS).blockingGet()
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 120_000
        while (!condition()) {
            assertTrue(message, SystemClock.elapsedRealtime() < deadline)
            SystemClock.sleep(100)
        }
    }

    companion object {
        private const val PASSWORD = "backup-transport-test-password"
        private val ATTACHMENT = ByteArray(64 * 1024) { (it % 251).toByte() }
    }
}