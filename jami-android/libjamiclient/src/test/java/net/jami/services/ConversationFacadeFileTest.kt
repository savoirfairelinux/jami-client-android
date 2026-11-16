package net.jami.services

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Flowable
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.utils.Log
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.*
import org.mockito.Answers
import org.mockito.stubbing.Answer
import java.io.File

class ConversationFacadeFileTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun failedFileMoveIsReportedAndNeverQueuedToTheDaemon() {
        Log.injectLogService(mock(LogService::class.java))
        val noEvents = Answer<Any?> { invocation ->
            when (invocation.method.returnType) {
                Observable::class.java -> Observable.never<Any>()
                Flowable::class.java -> Flowable.never<Any>()
                else -> Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        val accounts = mock(AccountService::class.java, noEvents)
        val runtime = mock(DeviceRuntimeService::class.java)
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.OneToOne)
        conversation.setPermissions(setOf("file"))
        val input = temporary.newFile("source.txt").apply { writeText("contents") }
        `when`(runtime.getNewConversationPath("account", "conversation", input.name))
            .thenReturn(File(temporary.root, "missing/destination.txt"))
        val facade = ConversationFacade(
            mock(HistoryService::class.java), mock(CallService::class.java, noEvents), accounts,
            mock(ContactService::class.java), mock(NotificationService::class.java),
            mock(HardwareService::class.java), runtime, mock(PreferencesService::class.java)
        )
        clearInvocations(accounts)

        facade.sendFile(conversation, Uri.fromId("a".repeat(40)), input).test()
            .assertError(IllegalStateException::class.java)

        assertTrue(input.exists())
        verifyNoInteractions(accounts)
    }
}
