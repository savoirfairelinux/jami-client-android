package net.jami.smartlist

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SmartListPresenterTest {
    @Test
    fun removalDialogKeepsFeedAndOrdinaryConversationIdentity() {
        val facade = mock(ConversationFacade::class.java)
        val accounts = Observable.never<Account>()
        `when`(facade.currentAccountSubject).thenReturn(accounts)
        `when`(facade.getConversationList(accounts)).thenReturn(Observable.never())
        val presenter = SmartListPresenter(facade, mock(AccountService::class.java), Schedulers.trampoline())
        val view = mock(SmartListView::class.java)
        presenter.bindView(view)

        for (mode in listOf(Conversation.Mode.Feed, Conversation.Mode.InvitesOnly, Conversation.Mode.OneToOne)) {
            val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, mode.name), mode)
            presenter.removeConversation(conversation)
            verify(view).displayDeleteDialog("account", conversation.uri, mode)
        }

        val syncing = Conversation("account", Uri(Uri.SWARM_SCHEME, "syncing"), Conversation.Mode.Syncing)
        syncing.requestMode = Conversation.Mode.Feed
        presenter.removeConversation(syncing)
        verify(view).displayDeleteDialog("account", syncing.uri, Conversation.Mode.Feed)
        presenter.unbindView()
    }
}
