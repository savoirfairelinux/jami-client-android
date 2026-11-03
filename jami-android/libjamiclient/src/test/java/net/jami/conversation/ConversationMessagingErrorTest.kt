package net.jami.conversation

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Conversation
import net.jami.model.Error
import net.jami.model.Uri
import net.jami.services.*
import net.jami.utils.Log
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ConversationMessagingErrorTest {
    @Test
    fun sendsReportPermissionDenialsAndOtherFailuresToTheView() {
        Log.injectLogService(mock(LogService::class.java))
        val facade = mock(ConversationFacade::class.java)
        val presenter = ConversationPresenter(
            mock(ContactService::class.java), mock(AccountService::class.java),
            mock(HardwareService::class.java), facade, mock(VCardService::class.java),
            mock(DeviceRuntimeService::class.java), mock(CollaborationService::class.java),
            mock(PreferencesService::class.java), Schedulers.trampoline()
        )
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        ConversationPresenter::class.java.getDeclaredField("mConversation").apply {
            isAccessible = true
            set(presenter, conversation)
        }
        val view = mock(ConversationView::class.java)
        presenter.bindView(view)
        for ((failure, expected) in listOf(
            SecurityException("denied") to Error.PERMISSION_DENIED,
            IllegalStateException("unavailable") to Error.GENERIC_ERROR
        )) {
            `when`(facade.sendTextMessage(conversation, conversation.uri, "message", null))
                .thenReturn(Completable.error(failure))
            presenter.sendTextMessage("message")
            verify(view).displayErrorToast(expected)
        }
        presenter.onDestroy()
    }
}
