package net.jami.services

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.StringVect
import net.jami.daemon.VectMap
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.AccountDevice
import net.jami.model.ConfigKey
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.model.Profile
import net.jami.model.TrustRequest
import net.jami.model.Uri
import net.jami.smartlist.ConversationFilter
import net.jami.model.interaction.ContactEvent
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.Interaction
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.io.File

class AccountServiceTest {
    private lateinit var service: AccountService
    private lateinit var executor: ScheduledExecutorService

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
        executor = mock(ScheduledExecutorService::class.java)
        doAnswer {
            it.getArgument<Runnable>(0).run()
            null
        }.`when`(executor).execute(any(Runnable::class.java))
        doAnswer {
            FutureTask(it.getArgument<Callable<Any?>>(0)).apply { run() }
        }.`when`(executor).submit(any(Callable::class.java))
        service = createService()
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
    }

    private fun createService(): AccountService {
        val profiles = mock(VCardService::class.java)
        doReturn(Profile.EMPTY_PROFILE_SINGLE).`when`(profiles).loadConversationProfile(anyMap())
        return spy(AccountService(
            executor,
            mock(HistoryService::class.java),
            mock(DeviceRuntimeService::class.java),
            profiles
        ))
    }

    private fun queueConversationCallbacks(): TestScheduler {
        val scheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { scheduler }
        service = createService()
        return scheduler
    }

    @Test
    fun feedInitialCommitIsVisibleAndSortsAnEmptyFeedFirst() {
        repeat(2) {
            val account = Account("account", emptyMap(), emptyList(),
                mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
            val group = account.newSwarm("group", Conversation.Mode.InvitesOnly)
            account.conversationStarted(group)
            group.lastEvent = Interaction("account").apply {
                type = Interaction.InteractionType.TEXT
                timestamp = 1000
            }
            val feed = account.newSwarm("feed", Conversation.Mode.Feed)
            account.conversationStarted(feed)
            val updates = account.getConversationsSubject(true).test()
            val creation = decodeInitialCommit(account, feed)
            feed.addSwarmElement(creation, false)

            assertEquals(Interaction.InteractionType.CONTACT, creation.type)
            assertEquals(ContactEvent.Event.FEED_CREATED, (creation as ContactEvent).event)
            assertEquals(2000L, creation.timestamp)
            assertEquals("initial", creation.messageId)
            assertEquals(listOf(feed, group), updates.values().last())
            assertEquals(0, account.unreadConversations.blockingFirst())
            group.lastEvent = Interaction("account").apply {
                type = Interaction.InteractionType.TEXT
                timestamp = 3000
            }
            assertEquals(listOf(group, feed), updates.values().last())
            updates.dispose()
        }
    }

    @Test
    fun feedInitialCommitIsVisibleInTransientModes() {
        for (mode in listOf(Conversation.Mode.Syncing, Conversation.Mode.Request)) {
            val account = Account("account", emptyMap(), emptyList(),
                mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
            val feed = account.newSwarm("feed", mode)
            if (mode == Conversation.Mode.Syncing) {
                feed.requestMode = Conversation.Mode.Feed
            } else {
                feed.request = TrustRequest("account", Uri.fromId("a".repeat(40)), 0,
                    feed.uri, mode = Conversation.Mode.Feed)
            }

            val creation = decodeInitialCommit(account, feed)

            assertEquals("$mode", Interaction.InteractionType.CONTACT, creation.type)
            assertEquals(ContactEvent.Event.FEED_CREATED, (creation as ContactEvent).event)
            assertEquals(2000L, creation.timestamp)
            assertEquals("initial", creation.messageId)
        }
    }

    @Test
    fun ordinaryGroupInitialCommitsKeepTheirExistingHiddenBehavior() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val group = account.newSwarm("group", Conversation.Mode.InvitesOnly)
        assertEquals(Interaction.InteractionType.INVALID, decodeInitialCommit(account, group).type)
    }

    private fun decodeInitialCommit(account: Account, conversation: Conversation): Interaction =
        AccountService::class.java.getDeclaredMethod(
            "getInteraction", Account::class.java, Conversation::class.java, Map::class.java
        ).apply { isAccessible = true }.invoke(service, account, conversation, mapOf(
            "id" to "initial", "type" to "initial", "author" to "a".repeat(40),
            "timestamp" to "2", "mode" to if (conversation.effectiveMode == Conversation.Mode.Feed) "5" else "2"
        )) as Interaction

    @Test
    fun feedCreationReturnsTheRegisteredFeedWithoutCreatingAGroup() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("feed", Conversation.Mode.Feed)
        doReturn(Single.just(account)).`when`(service).getAccountSingle("account")
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<String> { JamiService.startFeedConversation("account") }.thenReturn("feed")

            service.startFeedConversation("account").test()
                .awaitDone(3, TimeUnit.SECONDS).assertValue(conversation).assertComplete().assertNoErrors()

            daemon.verify { JamiService.startFeedConversation("account") }
            daemon.verifyNoMoreInteractions()
        }
    }

    @Test
    fun rejectedFeedCreationCompletesWithFailure() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        doReturn(Single.just(account)).`when`(service).getAccountSingle("account")
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<String> { JamiService.startFeedConversation("account") }.thenReturn("")

            service.startFeedConversation("account").test()
                .assertError(IllegalStateException::class.java).assertNoValues()

            daemon.verify { JamiService.startFeedConversation("account") }
            daemon.verifyNoMoreInteractions()
        }
    }

    @Test
    fun sipAccountsCannotCreateFeeds() {
        val account = Account("account", mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_SIP),
            emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        doReturn(Single.just(account)).`when`(service).getAccountSingle("account")
        mockStatic(JamiService::class.java).use { daemon ->
            service.startFeedConversation("account").test().assertError(IllegalArgumentException::class.java)
            daemon.verifyNoInteractions()
        }
    }

    @Test
    fun rejectedNameLookupCompletesWithFailure() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenReturn(false)

            service.findRegistrationByName("account", "", "alice").test()
                .assertValue { it.query == "alice" && it.state == AccountService.LookupState.NetworkError }
                .assertComplete()
        }
    }

    @Test
    fun rejectedAddressLookupCompletesWithFailure() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupAddress("account", "", "address") }.thenReturn(false)

            service.findRegistrationByAddress("account", "", "address").test()
                .assertValue { it.query == "address" && it.state == AccountService.LookupState.NetworkError }
                .assertComplete()
        }
    }

    @Test
    fun acceptedLookupWaitsForMatchingCallback() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenReturn(true)
            val result = service.findRegistrationByName("account", "", "alice").test()
            result.assertNoValues().assertNotComplete()

            service.registeredNameFound("other-account", "alice", 0, "other-address", "alice")
            service.registeredNameFound("account", "other-name", 0, "other-address", "other-name")
            result.assertNoValues()

            service.registeredNameFound("account", "alice", 0, "address", "alice")
            result.assertValue { it.address == "address" && it.state == AccountService.LookupState.Success }
                .assertComplete()
        }
    }

    @Test
    fun synchronousCallbackIsNotLost() {
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<Boolean> { JamiService.lookupName("account", "", "alice") }.thenAnswer {
                service.registeredNameFound("account", "alice", 0, "address", "alice")
                true
            }

            service.findRegistrationByName("account", "", "alice").test()
                .assertValue { it.address == "address" && it.state == AccountService.LookupState.Success }
                .assertComplete()
        }
    }

    @Test
    fun deviceNotificationReloadsTimestampsAndRemovals() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val previous = AccountDevice("device", "Phone", 10)
        val updated = previous.copy(lastSync = 20)
        account.devices = mapOf("device" to previous, "revoked" to AccountDevice("revoked", "Old phone"))
        doReturn(account).`when`(service).getAccount("account")
        doReturn(mapOf("device" to updated)).`when`(service).loadAccountDevices("account")

        service.knownDevicesChanged("account")

        verify(service).loadAccountDevices("account")
        assertEquals(mapOf("device" to updated), account.devices)
    }

    @Test
    fun bootstrapActivationCannotFallBehindAlreadyQueuedPushWork() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        AccountService::class.java.getDeclaredField("mAccountList").apply {
            isAccessible = true
            set(service, listOf(account))
        }
        val queue = ArrayDeque<Runnable>()
        doAnswer { queue.addLast(it.getArgument(0)); null }
            .`when`(executor).execute(any(Runnable::class.java))
        val ids = mock(StringVect::class.java)
        doReturn(mutableListOf(account.accountId).iterator()).`when`(ids).iterator()
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<StringVect> { JamiService.getAccountList() }.thenReturn(ids)
            executor.execute { service.loadAccountsFromDaemon(true) }
            executor.execute {
                // This task represents a push already queued while cold bootstrap runs.
                daemon.verify { JamiService.setAccountActive(account.accountId, true) }
            }
            while (queue.isNotEmpty()) queue.removeFirst().run()
        }
    }

    @Test
    fun emptyHistoryPagesStillUpdateConversationPermissions() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("conversation", Conversation.Mode.InvitesOnly)
        doReturn(account).`when`(service).getAccount("account")
        val updates = conversation.permissions.test()

        service.swarmLoaded(1, "account", "conversation", emptyList(), setOf("textReply", "reaction"))
        updates.awaitCount(1).assertValue(setOf("textReply", "reaction"))
        service.swarmLoaded(2, "account", "conversation", emptyList(), emptySet())
        updates.awaitCount(2).assertValues(setOf("textReply", "reaction"), emptySet())
    }

    @Test
    fun membershipChangesRefreshDaemonPermissions() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("conversation", Conversation.Mode.InvitesOnly)
        doReturn(account).`when`(service).getAccount("account")
        val permissions = mock(StringVect::class.java)
        doReturn(1).`when`(permissions).size
        doReturn("textReply").`when`(permissions).get(0)
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<StringVect> { JamiService.getConversationPermissions("account", "conversation") }
                .thenReturn(permissions)
            AccountService::class.java.getDeclaredMethod(
                "conversationMemberEventNow", String::class.java, String::class.java, String::class.java,
                Int::class.javaPrimitiveType
            ).apply { isAccessible = true }.invoke(service, "account", "conversation", "a".repeat(40), 0)

            conversation.permissions.test().assertValue(setOf("textReply"))
            daemon.verify { JamiService.getConversationPermissions("account", "conversation") }
        }
    }

    @Test
    fun messagingOperationsRequireTheirOwnPermissionBeforeCallingNativeCode() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("conversation", Conversation.Mode.Feed)
        val transfer = DataTransfer("file", "account", "a".repeat(40), "file.txt", true, 0L, 1L, 0L)
            .apply { setSwarmInfo("conversation", "file-message", null) }
        conversation.addSwarmElement(transfer, false)
        doReturn(account).`when`(service).getAccount("account")
        mockStatic(JamiService::class.java).use { daemon ->
            val operations = listOf(
                Triple("text", null, 0),
                Triple("textReply", "post", 0),
                Triple("reaction", "post", 2),
                Triple("edit", "post", 1),
                Triple("delete", "post", 1),
                Triple("deleteFile", "file-message", 1)
            )
            for ((permission, target, flag) in operations) {
                val text = if (permission.startsWith("delete")) "" else "message"
                conversation.setPermissions(emptySet())
                assertThrows(SecurityException::class.java) {
                    service.sendConversationMessage("account", conversation.uri, text, target, flag)
                }
                daemon.verifyNoInteractions()
                conversation.setPermissions(setOf(permission))
                service.sendConversationMessage("account", conversation.uri, text, target, flag)
                daemon.verify { JamiService.sendMessage("account", "conversation", text, target.orEmpty(), flag) }
                daemon.clearInvocations()
            }
        }
    }

    @Test
    fun filePublishingAndRepliesAreCheckedBeforeCallingNativeCode() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("conversation", Conversation.Mode.Feed)
        val file = File("file.txt")
        conversation.setPermissions(setOf("fileReply"))
        mockStatic(JamiService::class.java).use { daemon ->
            assertThrows(SecurityException::class.java) { service.sendFile(conversation, file) }
            daemon.verifyNoInteractions()
            service.sendFile(conversation, file, "post")
            daemon.verify { JamiService.sendFile("account", "conversation", file.absolutePath, file.name, "post") }
        }
    }

    @Test
    fun historyEventsPreserveContactAddedDate() {
        val parser = AccountService::class.java.getDeclaredMethod(
            "getInteraction", Account::class.java, Conversation::class.java, Map::class.java
        ).apply { isAccessible = true }
        val peer = "a".repeat(40)
        for (added in listOf(null, 1700000000123L)) {
            val account = Account("account",
                mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
                emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
            val contact = account.getContactFromCache(Uri.fromId(peer))
            added?.let { contact.updateDates(mapOf("addedMs" to it.toString())) }
            val conversation = account.newSwarm("conversation", Conversation.Mode.OneToOne)
            conversation.addContact(contact, MemberRole.MEMBER)
            for (type in listOf("initial", "member")) {
                val message = mapOf(
                    "id" to "message", "type" to type, "author" to peer,
                    "timestamp" to "1700000100", "invited" to peer, "uri" to peer, "action" to "join"
                )
                val event = parser.invoke(service, account, conversation, message) as ContactEvent

                assertEquals(1700000100000L, event.timestamp)
                assertSame(contact, event.contact)
                assertEquals(added, contact.addedDate?.time)
                assertNull(contact.removedDate)
                assertEquals(
                    if (type == "initial") ContactEvent.Event.INVITED else ContactEvent.Event.ADDED,
                    event.event
                )
            }
        }
    }

    @Test
    fun invitationsDecodeFeedAndPreserveExistingDefaults() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        doReturn(account).`when`(service).getAccount("account")
        for ((mode, expected) in listOf(
            "5" to Conversation.Mode.Feed,
            "2" to Conversation.Mode.InvitesOnly,
            null to Conversation.Mode.OneToOne
        )) {
            val conversationId = "conversation-$mode"
            val metadata = mutableMapOf("from" to "a".repeat(40), "received" to "1700000000")
            mode?.let { metadata["mode"] = it }

            service.conversationRequestReceived("account", conversationId, metadata)

            val request = account.getByUri(Uri(Uri.SWARM_SCHEME, conversationId))!!
            assertEquals(Conversation.Mode.Request, request.mode.blockingFirst())
            assertEquals(expected, request.requestMode)
            assertEquals(expected, request.request!!.mode)
        }
    }

    @Test
    fun leftFeedBecomesJoinableOnlyAfterItsSubscribedRowIsRemoved() {
        val callbacks = queueConversationCallbacks()
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
        val feed = account.newSwarm("feed", Conversation.Mode.Feed)
        account.conversationStarted(feed)
        doReturn(account).`when`(service).getAccount("account")
        val pending = account.getPendingSubject().map { it to account.getSwarm("feed") }.test()

        service.conversationRemoved("account", "feed")
        service.conversationRequestReceived("account", "feed", feedInvitation)

        pending.assertValueCount(1)
        callbacks.triggerActions()

        val (requests, subscribed) = pending.values().last()
        assertNull(subscribed)
        val request = requests.single()
        assertSame(request, account.getByUri(feed.uri))
        assertEquals(Conversation.Mode.Request, request.mode.blockingFirst())
        assertEquals(listOf(request), requests.filter(ConversationFilter.Feeds::matches))
        assertEquals(emptyList<Conversation>(), requests.filter(ConversationFilter.Groups::matches))
        assertEquals(1700000000000L, request.request!!.timestamp)
        account.getConversationsSubject(true).test().assertValue(emptyList())
        pending.dispose()
    }

    @Test
    fun queuedReadinessCannotConsumeTheRetainedInvitationAndTheFeedCanRejoin() {
        val callbacks = queueConversationCallbacks()
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
        val feed = account.newSwarm("feed", Conversation.Mode.Feed)
        account.conversationStarted(feed)
        doReturn(account).`when`(service).getAccount("account")
        mockStatic(JamiService::class.java).use { daemon ->
            stubReadyFeed(daemon, feed)

            service.conversationReady("account", "feed")
            service.conversationRemoved("account", "feed")
            service.conversationRequestReceived("account", "feed", feedInvitation)
            callbacks.triggerActions()

            val invitation = account.getPending().single()
            assertEquals(Conversation.Mode.Feed, invitation.effectiveMode)
            service.acceptTrustRequest("account", invitation.uri)
            daemon.verify { JamiService.acceptConversationRequest("account", "feed") }
            stubReadyFeed(daemon, invitation)
            service.conversationReady("account", "feed")
            callbacks.triggerActions()

            assertSame(invitation, account.getSwarm("feed"))
            assertEquals(Conversation.Mode.Feed, invitation.mode.blockingFirst())
            assertNull(invitation.request)
            assertEquals(emptyList<Conversation>(), account.getPending().toList())
            account.getConversationsSubject(true).test().assertValue(listOf(invitation))
        }
    }

    @Test
    fun dismissingAQueuedFeedInvitationDoesNotLeaveItJoinable() {
        val callbacks = queueConversationCallbacks()
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")).apply { setHistoryLoaded() }
        doReturn(account).`when`(service).getAccount("account")

        service.conversationRequestReceived("account", "feed", feedInvitation)
        service.conversationRequestDeclined("account", "feed")
        callbacks.triggerActions()

        assertNull(account.getByUri(Uri(Uri.SWARM_SCHEME, "feed")))
        account.getPendingSubject().test().assertValue(emptyList())
    }

    private val feedInvitation = mapOf(
        "from" to "a".repeat(40), "received" to "1700000000", "mode" to "5"
    )

    private fun stubReadyFeed(daemon: MockedStatic<JamiService>, conversation: Conversation) {
        val info = mock(StringMap::class.java)
        doReturn(hashMapOf("mode" to "5")).`when`(info).toNativeFromUtf8()
        val members = mock(VectMap::class.java)
        doReturn(emptyList<StringMap>().iterator()).`when`(members).iterator()
        daemon.`when`<StringMap> { JamiService.conversationInfos("account", "feed") }.thenReturn(info)
        daemon.`when`<VectMap> { JamiService.getConversationMembers("account", "feed") }.thenReturn(members)
        daemon.`when`<StringVect> { JamiService.getConversationPermissions("account", "feed") }
            .thenReturn(mock(StringVect::class.java))
        doReturn(Single.just(conversation)).`when`(service).loadMore(conversation, 8)
        doReturn(Single.just(conversation)).`when`(service).loadMore(conversation, 2)
    }
}
