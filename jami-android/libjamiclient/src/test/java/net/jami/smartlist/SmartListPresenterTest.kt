package net.jami.smartlist

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.TrustRequest
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.TimeUnit

class SmartListPresenterTest {
    @Test
    fun filtersSeparateContactsNormalGroupsAndFeeds() {
        val modes = listOf(
            Conversation.Mode.Legacy, Conversation.Mode.OneToOne,
            Conversation.Mode.AdminInvitesOnly, Conversation.Mode.InvitesOnly,
            Conversation.Mode.Public, Conversation.Mode.Feed
        )
        val conversations = modes.map { mode ->
            Conversation("account", Uri(Uri.SWARM_SCHEME, mode.name), mode)
        }
        assertEquals(conversations, conversations.filter(ConversationFilter.All::matches))
        assertEquals(conversations.take(2), conversations.filter(ConversationFilter.Contacts::matches))
        assertEquals(conversations.subList(2, 5), conversations.filter(ConversationFilter.Groups::matches))
        assertEquals(conversations.takeLast(1), conversations.filter(ConversationFilter.Feeds::matches))
    }

    @Test
    fun invitationsAndSyncingConversationsUseTheirUnderlyingType() {
        for ((mode, expected) in listOf(
            Conversation.Mode.OneToOne to ConversationFilter.Contacts,
            Conversation.Mode.InvitesOnly to ConversationFilter.Groups,
            Conversation.Mode.Feed to ConversationFilter.Feeds
        )) {
            val uri = Uri(Uri.SWARM_SCHEME, mode.name)
            val request = Conversation("account", uri, Conversation.Mode.Request).apply {
                this.request = TrustRequest("account", Uri.fromId("sender"), 1L, uri, mode = mode)
            }
            val syncing = Conversation("account", uri, Conversation.Mode.Syncing).apply {
                requestMode = mode
            }
            for (conversation in listOf(request, syncing)) {
                assertTrue(ConversationFilter.All.matches(conversation))
                assertEquals(
                    listOf(expected),
                    ConversationFilter.entries.filter { it != ConversationFilter.All && it.matches(conversation) }
                )
            }
        }
    }

    @Test
    fun filterChangesRefreshTheExistingListAndEmptyState() {
        val facade = mock(ConversationFacade::class.java)
        val accounts = Observable.never<Account>()
        val lists = PublishSubject.create<ConversationFacade.ConversationList>()
        `when`(facade.currentAccountSubject).thenReturn(accounts)
        `when`(facade.getConversationList(accounts)).thenReturn(lists)
        val scheduler = TestScheduler()
        val presenter = SmartListPresenter(facade, mock(AccountService::class.java), scheduler)
        val displayed = mutableListOf<List<Conversation>>()
        var emptyStates = 0
        val view = mock(SmartListView::class.java) { invocation ->
            when (invocation.method.name) {
                "updateList" -> displayed.add(invocation.getArgument<ConversationFacade.ConversationList>(0).conversations)
                "displayNoConversationMessage" -> emptyStates++
            }
            null
        }
        presenter.bindView(view)
        val contact = Conversation("account", Uri(Uri.SWARM_SCHEME, "contact"), Conversation.Mode.OneToOne)
        val feed = Conversation("account", Uri(Uri.SWARM_SCHEME, "feed"), Conversation.Mode.Feed)
        lists.onNext(ConversationFacade.ConversationList(listOf(contact, feed)))
        scheduler.triggerActions()
        presenter.setConversationFilter(ConversationFilter.Feeds)
        scheduler.triggerActions()
        presenter.setConversationFilter(ConversationFilter.Groups)
        scheduler.triggerActions()
        presenter.setConversationFilter(ConversationFilter.Contacts)
        scheduler.triggerActions()
        val group = Conversation("account", Uri(Uri.SWARM_SCHEME, "group"), Conversation.Mode.InvitesOnly)
        lists.onNext(ConversationFacade.ConversationList(listOf(group)))
        scheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS)
        presenter.setConversationFilter(ConversationFilter.Groups)
        scheduler.triggerActions()

        assertEquals(listOf(listOf(contact, feed), listOf(feed), listOf(contact), listOf(group)), displayed)
        assertEquals(2, emptyStates)
        presenter.unbindView()
    }

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
