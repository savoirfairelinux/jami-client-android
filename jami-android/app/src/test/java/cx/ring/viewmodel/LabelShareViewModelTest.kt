package cx.ring.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri as AndroidUri
import cx.ring.utils.ContentUri.ShareItem
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.CompletableSubject
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class LabelShareViewModelTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var account: Account
    private lateinit var accounts: AccountService
    private lateinit var facade: ConversationFacade
    private lateinit var model: LabelShareViewModel
    private val conversations = mutableMapOf<Contact, Conversation>()
    private val text = listOf(ShareItem("text/plain", text = "hello"))
    private var sendFile: ((Conversation, Uri, File) -> Completable)? = null

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        context = RuntimeEnvironment.getApplication()
        account = Account("account", mapOf(
            ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI,
            ConfigKey.ACCOUNT_USERNAME.key to "f".repeat(40)
        ), emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        accounts = mock(AccountService::class.java)
        `when`(accounts.getAccount("account")).thenReturn(account)
        facade = mock(ConversationFacade::class.java) { invocation ->
            if (invocation.method.name == "sendFile") {
                checkNotNull(sendFile).invoke(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        model = LabelShareViewModel(context, accounts, facade, Schedulers.trampoline())
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        assertTrue(context.cacheDir.listFiles().orEmpty().none { it.name.startsWith("label-share-") })
    }

    @Test
    fun sendsIndividuallyOnlyToCurrentUnblockedLabelMembers() {
        val first = contact('a')
        val second = contact('b')
        val blocked = contact('c')
        val unlabelled = contact('d')
        contact('f')
        account.removeContact(blocked.primaryNumber, true)
        account.setContactLabels(unlabelled.uri, emptySet())

        model.share("account", "friends", confirmed(), text)

        assertEquals(LabelShareViewModel.State.Finished(2, 0), model.state.value)
        verify(facade).sendTextMessage(conversations.getValue(first), first.uri, "hello")
        verify(facade).sendTextMessage(conversations.getValue(second), second.uri, "hello")
        verify(facade, never()).startConversation("account", conversations.getValue(blocked).uri)
        verify(facade, never()).startConversation("account", conversations.getValue(unlabelled).uri)
    }

    @Test
    fun everyRecipientGetsIndependentCopiesOfEveryAttachmentWithTheOriginalNames() {
        val first = contact('a')
        val second = contact('b')
        val sources = listOf("first", "second").map { contents ->
            File(temporary.newFolder(contents), "document.txt").apply { writeText(contents) }
        }
        val received = mutableMapOf<Contact, MutableList<String>>()
        val outgoingPaths = mutableSetOf<String>()
        var index = 0
        sendFile = { conversation, uri, file ->
            val contact = listOf(first, second).first { it.uri == uri }
            assertEquals(conversations.getValue(contact), conversation)
            Completable.fromAction {
                assertEquals("document.txt", file.name)
                received.getOrPut(contact) { mutableListOf() }.add(file.readText())
                outgoingPaths.add(file.absolutePath)
                Files.move(file.toPath(), File(temporary.root, "sent-${index++}").toPath())
            }
        }
        model.share("account", "friends", confirmed(),
            sources.map { ShareItem("application/octet-stream", AndroidUri.fromFile(it)) })

        assertEquals(LabelShareViewModel.State.Finished(2, 0), model.state.value)
        assertEquals(listOf("first", "second"), received[first])
        assertEquals(listOf("first", "second"), received[second])
        assertEquals(4, outgoingPaths.size)
        assertEquals(listOf("first", "second"), sources.map { it.readText() })
    }

    @Test
    fun aRecipientFailureIsReportedWithoutSkippingOthersOrAutomaticallyRetrying() {
        val first = contact('a')
        val failed = contact('b')
        val last = contact('c')
        `when`(facade.sendTextMessage(conversations.getValue(failed), failed.uri, "hello"))
            .thenReturn(Completable.error(IOException("send failed")))
        val confirmed = confirmed()
        model.share("account", "friends", confirmed, text)
        assertEquals(LabelShareViewModel.State.Finished(2, 1), model.state.value)
        model.share("account", "friends", confirmed, text)
        for (contact in listOf(first, failed, last))
            verify(facade).sendTextMessage(conversations.getValue(contact), contact.uri, "hello")
    }

    @Test
    fun changedMembershipRequiresAnotherConfirmationBeforeAnythingIsSent() {
        contact('a')
        val changed = contact('b')
        val confirmed = confirmed()
        account.removeContact(changed.primaryNumber, true)
        model.share("account", "friends", confirmed, text)
        assertEquals(LabelShareViewModel.State.Failed, model.state.value)
        verifyNoInteractions(facade)
    }

    @Test
    fun unreadableFilesAreRejectedBeforeAnyRecipientGetsEvenTheText() {
        contact('a')
        val missing = File(temporary.root, "missing.txt")
        model.share("account", "friends", confirmed(),
            text + ShareItem("application/octet-stream", AndroidUri.fromFile(missing)))
        assertEquals(LabelShareViewModel.State.Failed, model.state.value)
        verifyNoInteractions(facade)
    }

    @Test
    fun filePermissionsAreCheckedBeforeSendingAnyPartOfThePayload() {
        val contact = contact('a')
        conversations.getValue(contact).setPermissions(setOf("text"))
        val file = temporary.newFile("attachment.txt").apply { writeText("contents") }
        model.share("account", "friends", confirmed(),
            text + ShareItem("application/octet-stream", AndroidUri.fromFile(file)))
        assertEquals(LabelShareViewModel.State.Finished(0, 1), model.state.value)
        verify(facade, never()).sendTextMessage(conversations.getValue(contact), contact.uri, "hello")
    }

    @Test
    fun duplicateRequestsDoNotStartAnotherSendWhileAnOperationIsRunning() {
        val contact = contact('a')
        val pending = CompletableSubject.create()
        `when`(facade.sendTextMessage(conversations.getValue(contact), contact.uri, "hello")).thenReturn(pending)
        val recipients = confirmed()
        model.share("account", "friends", recipients, text)
        assertEquals(LabelShareViewModel.State.Sending, model.state.value)
        model.share("account", "friends", recipients, text)
        verify(facade).sendTextMessage(conversations.getValue(contact), contact.uri, "hello")
        pending.onComplete()
        assertEquals(LabelShareViewModel.State.Finished(1, 0), model.state.value)
    }

    private fun confirmed() = account.getContactsWithLabel("friends").map { it.uri.uri }.toSet()

    private fun contact(id: Char): Contact {
        val uri = Uri.fromId(id.toString().repeat(40))
        account.addContact(uri.rawRingId, true)
        account.setContactLabels(uri, setOf("friends"))
        val contact = account.getContactFromCache(uri)
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation-$id"), Conversation.Mode.OneToOne)
        conversation.addContact(contact, MemberRole.MEMBER)
        conversation.setPermissions(setOf("text", "file"))
        contact.setConversationUri(conversation.uri)
        conversations[contact] = conversation
        `when`(facade.startConversation("account", conversation.uri)).thenReturn(Single.just(conversation))
        `when`(facade.sendTextMessage(conversation, contact.uri, "hello")).thenReturn(Completable.complete())
        return contact
    }
}
