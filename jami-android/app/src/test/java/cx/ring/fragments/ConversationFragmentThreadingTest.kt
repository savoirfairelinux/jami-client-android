package cx.ring.fragments

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import cx.ring.services.SharedPreferencesServiceImpl.Companion.PREF_THREADED_VIEW
import cx.ring.utils.ConversationPath
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.smartlist.ConversationItemViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ConversationFragmentThreadingTest {
    private val path = ConversationPath("account", Uri(Uri.SWARM_SCHEME, "conversation"))
    private lateinit var fragment: ConversationFragment
    private lateinit var accounts: AccountService
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        accounts = mock(AccountService::class.java)
        preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences("threading-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        fragment = ConversationFragment().apply {
            arguments = path.toBundle()
            accountService = accounts
        }
        setField("accountPreferences", preferences)
    }

    @Test
    fun feedViewModelEnablesThreadingWithoutAccountLookup() {
        val item = mock(ConversationItemViewModel::class.java)
        `when`(item.isFeed()).thenReturn(true)
        setField("mConversationItem", item)

        assertTrue(threadingEnabled())
        verifyNoInteractions(accounts)
    }

    @Test
    fun feedLookupEnablesThreadingBeforeTheViewModelArrives() {
        cachedConversation(Conversation.Mode.Feed, Conversation.Mode.Feed)

        assertTrue(threadingEnabled())
    }

    @Test
    fun transientFeedLookupUsesItsEffectiveMode() {
        for (mode in listOf(Conversation.Mode.Syncing, Conversation.Mode.Request)) {
            cachedConversation(mode, Conversation.Mode.Feed)

            assertTrue("$mode", threadingEnabled())
        }
    }

    @Test
    fun ordinaryChatsFollowThePreferenceWithAndWithoutAViewModel() {
        cachedConversation(Conversation.Mode.InvitesOnly, Conversation.Mode.InvitesOnly)
        val item = mock(ConversationItemViewModel::class.java)
        `when`(item.isFeed()).thenReturn(false)
        for (viewModel in listOf(null, item)) {
            setField("mConversationItem", viewModel)
            for (enabled in listOf(false, true)) {
                preferences.edit().putBoolean(PREF_THREADED_VIEW, enabled).commit()

                assertEquals(enabled, threadingEnabled())
            }
        }
    }

    @Test
    fun unavailableConversationStillFollowsThePreference() {
        assertFalse(threadingEnabled())
        preferences.edit().putBoolean(PREF_THREADED_VIEW, true).commit()

        assertTrue(threadingEnabled())
    }

    @Test
    fun missingPathDisablesThreadingWithoutAccountLookup() {
        fragment.arguments = null
        preferences.edit().putBoolean(PREF_THREADED_VIEW, true).commit()

        assertFalse(threadingEnabled())
        verifyNoInteractions(accounts)
    }

    private fun cachedConversation(mode: Conversation.Mode, effectiveMode: Conversation.Mode) {
        val conversation = mock(Conversation::class.java)
        `when`(conversation.mode).thenReturn(BehaviorSubject.createDefault(mode))
        `when`(conversation.effectiveMode).thenReturn(effectiveMode)
        val account = mock(Account::class.java)
        `when`(account.getByUri(path.conversationUri)).thenReturn(conversation)
        `when`(accounts.getAccount(path.accountId)).thenReturn(account)
    }

    private fun threadingEnabled(): Boolean =
        ConversationFragment::class.java.getDeclaredMethod("threadingEnabled")
            .apply { isAccessible = true }.invoke(fragment) as Boolean

    private fun setField(name: String, value: Any?) {
        ConversationFragment::class.java.getDeclaredField(name)
            .apply { isAccessible = true }.set(fragment, value)
    }
}
