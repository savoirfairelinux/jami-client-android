package net.jami.services

import io.reactivex.rxjava3.core.Completable
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.daemon.StringVect
import net.jami.daemon.VectMap
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.ConfigKey
import net.jami.model.MemberRole
import net.jami.model.Uri
import net.jami.utils.Log
import org.junit.Before
import org.junit.Test
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

class ConversationControlPermissionsTest {
    private lateinit var executor: ScheduledExecutorService
    private lateinit var accounts: AccountService
    private val member = Uri.fromId("a".repeat(40))

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        executor = mock(ScheduledExecutorService::class.java)
        doAnswer { it.getArgument<Runnable>(0).run(); null }.`when`(executor).execute(any(Runnable::class.java))
        doAnswer {
            FutureTask(it.getArgument<Callable<Any?>>(0)).apply { run() }
        }.`when`(executor).submit(any(Callable::class.java))
        accounts = spy(AccountService(
            executor, mock(HistoryService::class.java), mock(DeviceRuntimeService::class.java),
            mock(VCardService::class.java)
        ))
    }

    private fun grant(daemon: MockedStatic<JamiService>, key: String?) {
        val vector = mock(StringVect::class.java)
        `when`(vector.size).thenReturn(if (key == null) 0 else 1)
        if (key != null) `when`(vector[0]).thenReturn(key)
        daemon.`when`<StringVect> { JamiService.getConversationPermissions("account", "conversation") }
            .thenReturn(vector)
    }

    @Test
    fun profileInvitationsAndModerationRequireTheirRespectiveGrants() {
        mockStatic(JamiService::class.java).use { daemon ->
            mockConstruction(StringMap::class.java).use {
                val actions: List<Triple<String, () -> Completable, () -> Unit>> = listOf(
                    Triple("updateProfile",
                        { accounts.updateConversationInfo("account", "conversation", mapOf("title" to "Feed")) },
                        { daemon.verify { JamiService.updateConversationInfos(eq("account"), eq("conversation"), any(StringMap::class.java)) } }),
                    Triple("addMember",
                        { accounts.addConversationMembers("account", "conversation", listOf(member)) },
                        { daemon.verify { JamiService.addConversationMember("account", "conversation", member.rawRingId) } }),
                    Triple("banUnbanMember",
                        { accounts.removeConversationMember("account", "conversation", member) },
                        { daemon.verify { JamiService.removeConversationMember("account", "conversation", member.rawRingId) } }),
                    Triple("banUnbanMember",
                        { accounts.unbanConversationMember("account", "conversation", member) },
                        { daemon.verify { JamiService.addConversationMember("account", "conversation", member.rawRingId) } })
                )
                for ((permission, action, verifyAction) in actions) {
                    grant(daemon, null)
                    action().test().assertError(SecurityException::class.java).assertNotComplete()
                    daemon.verify { JamiService.getConversationPermissions("account", "conversation") }
                    daemon.verifyNoMoreInteractions()
                    daemon.clearInvocations()
                    grant(daemon, permission)
                    action().test().assertComplete().assertNoErrors()
                    verifyAction()
                    daemon.verify { JamiService.getConversationPermissions("account", "conversation") }
                    daemon.verifyNoMoreInteractions()
                    daemon.clearInvocations()
                }
            }
        }
    }

    @Test
    fun invitationsCannotUnbanMembersWithOnlyTheInvitationGrant() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val conversation = account.newSwarm("conversation", Conversation.Mode.InvitesOnly)
        conversation.roles[member.uri] = MemberRole.BLOCKED
        doReturn(account).`when`(accounts).getAccount("account")
        mockStatic(JamiService::class.java).use { daemon ->
            grant(daemon, "addMember")
            accounts.addConversationMembers("account", "conversation", listOf(Uri.fromId("b".repeat(40)), member))
                .test().assertError(SecurityException::class.java)
            daemon.verify({ JamiService.getConversationPermissions("account", "conversation") }, times(2))
            daemon.verifyNoMoreInteractions()
        }
    }

    @Test
    fun privateProfileOverridesDoNotRequireSharedProfilePermission() {
        val account = Account("account", emptyMap(), emptyList(),
            mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        account.newSwarm("conversation", Conversation.Mode.OneToOne)
        doReturn(account).`when`(accounts).getAccount("account")
        mockStatic(JamiService::class.java).use { daemon ->
            mockConstruction(StringMap::class.java).use {
                accounts.updateConversationInfo("account", "conversation", mapOf("title" to "Alias"))
                    .test().assertComplete().assertNoErrors()
                daemon.verify { JamiService.updateConversationInfos(eq("account"), eq("conversation"), any(StringMap::class.java)) }
                daemon.verifyNoMoreInteractions()
            }
        }
    }

    @Test
    fun documentCreationIsCheckedBeforeCallingNativeCode() {
        val documents = CollaborationService(executor)
        val uri = Uri(Uri.SWARM_SCHEME, "conversation")
        mockStatic(JamiService::class.java).use { daemon ->
            grant(daemon, null)
            documents.createDocument("account", uri, "Document").test().assertError(SecurityException::class.java)
            daemon.verify { JamiService.getConversationPermissions("account", "conversation") }
            daemon.verifyNoMoreInteractions()
            daemon.clearInvocations()
            grant(daemon, "createDocument")
            daemon.`when`<String> { JamiService.createCollaborativeDocument(eq("account"), eq("conversation"), eq("Document"), anyString()) }
                .thenReturn("document")
            documents.createDocument("account", uri, "Document").test().assertValue("document").assertComplete()
        }
    }

    @Test
    fun deniedSwarmAndRendezvousCallsDoNotRequestSystemCallSetup() {
        val calls = mock(CallService::class.java, withSettings()
            .useConstructor(executor, mock(ContactService::class.java), accounts, mock(DeviceRuntimeService::class.java))
            .defaultAnswer(CALLS_REAL_METHODS))
        mockStatic(JamiService::class.java).use { daemon ->
            grant(daemon, null)
            for (target in listOf(
                Uri(Uri.SWARM_SCHEME, "conversation"),
                Uri.fromString("rdv:conversation/host/device/conference")
            )) {
                calls.placeCallIfAllowed("account", Uri(Uri.SWARM_SCHEME, "other"), target, true)
                    .test().assertError(SecurityException::class.java)
                verify(calls, never()).requestPlaceCall("account", Uri(Uri.SWARM_SCHEME, "other"), target, true)
            }
            daemon.verify({ JamiService.getConversationPermissions("account", "conversation") }, times(2))
            daemon.verifyNoMoreInteractions()
        }
    }

    @Test
    fun conferenceHostingGrantAllowsSwarmCalls() {
        val uri = Uri(Uri.SWARM_SCHEME, "conversation")
        val calls = mock(CallService::class.java, withSettings()
            .useConstructor(executor, mock(ContactService::class.java), accounts, mock(DeviceRuntimeService::class.java))
            .defaultAnswer(CALLS_REAL_METHODS))
        mockStatic(JamiService::class.java).use { daemon ->
            mockConstruction(VectMap::class.java).use {
                mockConstruction(StringMap::class.java).use {
                    grant(daemon, "hostConference")
                    daemon.`when`<String> { JamiService.placeCallWithMedia(eq("account"), eq(uri.uri), any(VectMap::class.java)) }
                        .thenReturn("")
                    calls.placeCallIfAllowed("account", uri, uri, true).test().assertValueCount(1).assertComplete()
                    daemon.verify { JamiService.placeCallWithMedia(eq("account"), eq(uri.uri), any(VectMap::class.java)) }
                }
            }
        }
    }
}
