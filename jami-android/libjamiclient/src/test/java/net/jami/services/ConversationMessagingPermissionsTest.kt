package net.jami.services

import io.reactivex.rxjava3.core.Observable
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.Interaction.TransferStatus
import net.jami.utils.Log
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.concurrent.ScheduledExecutorService

class ConversationMessagingPermissionsTest {
    @get:Rule
    val files = TemporaryFolder()
    private lateinit var accountService: AccountService
    private val runtimeService = mock(DeviceRuntimeService::class.java)
    private lateinit var facade: ConversationFacade

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        accountService = spy(AccountService(
            mock(ScheduledExecutorService::class.java), mock(HistoryService::class.java),
            runtimeService, mock(VCardService::class.java)
        ))
        val callService = mock(CallService::class.java)
        `when`(callService.callsUpdates).thenReturn(Observable.never())
        `when`(callService.confsUpdates).thenReturn(Observable.never())
        facade = ConversationFacade(
            mock(HistoryService::class.java), callService, accountService,
            mock(ContactService::class.java), mock(NotificationService::class.java),
            mock(HardwareService::class.java), runtimeService, mock(PreferencesService::class.java)
        )
        clearInvocations(accountService)
    }

    @Test
    fun deniedFilePublicationDoesNotPrepareOrMoveTheFile() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        conversation.setPermissions(setOf("fileReply"))
        val file = files.newFile("attachment.txt")
        facade.sendFile(conversation, conversation.uri, file).test()
            .assertError(SecurityException::class.java).assertNotComplete()
        assertTrue(file.exists())
        verifyNoInteractions(runtimeService, accountService)
    }

    @Test
    fun deniedSharedFileDeletionDoesNotCancelOrDeleteDownloadedContent() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        conversation.setPermissions(setOf("delete"))
        val file = files.newFile("download.txt")
        val transfer = DataTransfer("file", "account", "a".repeat(40), file.name, true, 0L, 1L, 0L)
            .apply {
                setSwarmInfo("feed", "message", null)
                destination = file
                transferStatus = TransferStatus.TRANSFER_ONGOING
            }
        assertThrows(SecurityException::class.java) { facade.deleteConversationItem(conversation, transfer) }
        assertTrue(file.exists())
        verifyNoInteractions(accountService)
    }
}
