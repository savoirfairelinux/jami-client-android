package net.jami.services

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.StringVect
import net.jami.daemon.VectMap
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.model.TrustRequest
import net.jami.model.Uri
import net.jami.utils.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.ScheduledExecutorService

class ConversationAddedTest {
    private val callbacks = TestScheduler()
    private val account = Account(
        "account", mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI), emptyList(),
        mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED")
    )
    private val vcardService = mock(VCardService::class.java)
    private lateinit var service: AccountService

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        account.setHistoryLoaded()
        RxJavaPlugins.setComputationSchedulerHandler { callbacks }
        try {
            service = spy(AccountService(
                mock(ScheduledExecutorService::class.java),
                mock(HistoryService::class.java),
                mock(DeviceRuntimeService::class.java),
                vcardService
            ))
        } finally {
            RxJavaPlugins.setComputationSchedulerHandler(null)
        }
        doReturn(account).`when`(service).getAccount(account.accountId)
    }

    @Test
    fun addedPromotesInviteWithoutLoadingHistoryUntilReady() {
        val id = "feed"
        val uri = Uri(Uri.SWARM_SCHEME, id)
        val inviter = Uri.fromId("a".repeat(40))
        val profile = Profile("Offline feed", null)
        account.addContact(inviter.rawRingId, true)
        account.addRequest(TrustRequest(account.accountId, inviter, 1700000000000L, uri, Single.just(profile), Conversation.Mode.Feed))
        val conversation = account.getByUri(uri)!!

        val syncingInfo = hashMapOf("syncing" to "true", "mode" to "5", "createdMs" to "1700000000123", "title" to "Offline feed")
        val readyInfo = hashMapOf("mode" to "5", "title" to "Offline feed")
        val syncing = nativeInfo(syncingInfo)
        val ready = nativeInfo(readyInfo)
        doReturn(Single.just(profile)).`when`(vcardService).loadConversationProfile(syncingInfo)
        doReturn(Single.just(profile)).`when`(vcardService).loadConversationProfile(readyInfo)
        val members = mock(VectMap::class.java)
        doReturn(emptyList<StringMap>().iterator()).`when`(members).iterator()
        val permissions = mock(StringVect::class.java)
        for (pageSize in listOf(8, 2)) {
            doAnswer {
                assertEquals(Conversation.Mode.Feed, conversation.mode.blockingFirst())
                Single.just(conversation)
            }.`when`(service).loadMore(conversation, pageSize)
        }

        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<StringMap> { JamiService.conversationInfos(account.accountId, id) }
                .thenReturn(syncing, ready)
            daemon.`when`<VectMap> { JamiService.getConversationMembers(account.accountId, id) }.thenReturn(members)
            daemon.`when`<StringVect> { JamiService.getConversationPermissions(account.accountId, id) }.thenReturn(permissions)

            service.conversationAdded(account.accountId, id)
            callbacks.triggerActions()

            assertSame(conversation, account.getSwarm(id))
            assertEquals(listOf(conversation), account.getConversationsSubject().blockingFirst())
            assertTrue(account.getPending().isEmpty())
            assertNull(conversation.request)
            assertEquals(Conversation.Mode.Syncing, conversation.mode.blockingFirst())
            assertEquals(Conversation.Mode.Feed, conversation.effectiveMode)
            assertSame(profile, conversation.profile.blockingFirst())
            assertEquals(1700000000123L, conversation.lastEvent?.timestamp)
            assertFalse(conversation.lastElementLoadedSubject.hasValue())
            daemon.verify { JamiService.conversationInfos(account.accountId, id) }
            daemon.verifyNoMoreInteractions()

            service.conversationReady(account.accountId, id)
            callbacks.triggerActions()

            assertSame(conversation, account.getSwarm(id))
            assertEquals(Conversation.Mode.Feed, conversation.mode.blockingFirst())
            assertTrue(conversation.lastElementLoadedSubject.hasValue())
            verify(service).loadMore(conversation, 8)
        }
    }

    @Test
    fun addedWithoutInviteOrMetadataIsVisibleWhileSyncing() {
        val id = "linked"
        val info = nativeInfo(hashMapOf())
        mockStatic(JamiService::class.java).use { daemon ->
            daemon.`when`<StringMap> { JamiService.conversationInfos(account.accountId, id) }
                .thenReturn(info)

            service.conversationAdded(account.accountId, id)
            callbacks.triggerActions()

            val conversation = account.getConversationsSubject().blockingFirst().single()
            assertEquals(id, conversation.uri.rawRingId)
            assertEquals(Conversation.Mode.Syncing, conversation.mode.blockingFirst())
            conversation.contactUpdates.test().assertValue(emptyList())
        }
    }

    private fun nativeInfo(info: HashMap<String, String>): StringMap =
        mock(StringMap::class.java).also { doReturn(info).`when`(it).toNativeFromUtf8() }
}
