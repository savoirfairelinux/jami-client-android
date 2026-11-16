package cx.ring.client

import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cx.ring.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.CompletableSubject
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
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
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28, 34])
class LabelShareActivityTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Inject lateinit var accounts: AccountService
    @Inject lateinit var facade: ConversationFacade
    private lateinit var controller: ActivityController<ShareActivity>
    private lateinit var conversation: Conversation
    private val pending = CompletableSubject.create()
    private val uri = Uri.fromId("a".repeat(40))

    @Before
    fun setUp() {
        hilt.inject()
        Log.injectLogService(mock(LogService::class.java))
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        val account = Account("account",
            mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
            emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        account.addContact(uri.rawRingId, true)
        account.setContactLabels(uri, setOf("friends"))
        val contact = account.getContactFromCache(uri)
        conversation = account.newSwarm("direct", Conversation.Mode.OneToOne)
        conversation.addContact(contact, MemberRole.MEMBER)
        conversation.setPermissions(setOf("text"))
        contact.setConversationUri(conversation.uri)
        val currentAccount = Observable.just(account)
        `when`(accounts.getAccount("account")).thenReturn(account)
        `when`(facade.currentAccountSubject).thenReturn(currentAccount)
        `when`(facade.getFullConversationList(
            eq(currentAccount) ?: currentAccount,
            any<Observable<String>>() ?: Observable.never(), eq(false)
        )).thenReturn(Observable.just(ConversationFacade.ConversationList()))
        `when`(facade.startConversation("account", conversation.uri)).thenReturn(Single.just(conversation))
        `when`(facade.sendTextMessage(conversation, uri, "hello")).thenReturn(pending)
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "hello")
        controller = Robolectric.buildActivity(ShareActivity::class.java, intent)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        idle()
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) controller.pause().stop().destroy()
        RxJavaPlugins.reset()
    }

    @Test
    fun labelSharingRequiresConfirmationAndDoesNotResendAfterRotation() {
        val labels = controller.get().findViewById<ChipGroup>(R.id.share_labels)
        val chip = labels.children.filterIsInstance<Chip>().single()
        assertEquals("friends", chip.text.toString())
        chip.performClick()
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()
        verify(facade, never()).sendTextMessage(conversation, uri, "hello")

        chip.performClick()
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        verify(facade).sendTextMessage(conversation, uri, "hello")
        assertTrue(controller.get().findViewById<View>(R.id.label_share_progress).isVisible)

        controller.recreate()
        idle()
        assertTrue(controller.get().findViewById<View>(R.id.label_share_progress).isVisible)
        verify(facade).sendTextMessage(conversation, uri, "hello")
        pending.onComplete()
        idle()
        assertTrue(controller.get().isFinishing)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
}
