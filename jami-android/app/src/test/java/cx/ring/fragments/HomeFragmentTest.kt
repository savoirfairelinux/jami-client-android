package cx.ring.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.search.SearchView
import cx.ring.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.services.LogService
import net.jami.services.PreferencesService
import net.jami.smartlist.ConversationFilter
import net.jami.utils.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28, 34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeFragmentTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Inject lateinit var accounts: AccountService
    @Inject lateinit var facade: ConversationFacade
    @Inject lateinit var preferences: PreferencesService
    private lateinit var controller: ActivityController<HostActivity>
    private val currentAccount = BehaviorSubject.create<Account>()
    private lateinit var home: HomeFragment
    private var initialAppBarColor = 0
    private val activity get() = controller.get()
    private val filters get() = home.requireView().findViewById<ChipGroup>(R.id.conversation_filters)

    @Before
    fun setUp() {
        hilt.inject()
        Log.injectLogService(mock(LogService::class.java))
        `when`(accounts.currentAccountSubject).thenReturn(currentAccount)
        `when`(facade.currentAccountSubject).thenReturn(currentAccount)
        `when`(facade.getConversationList(currentAccount)).thenReturn(Observable.never())
        `when`(facade.getSearchResults(
            any<Observable<String>>() ?: Observable.never(), any<Observable<Account>>() ?: currentAccount
        )).thenReturn(Observable.never())
        `when`(accounts.getObservableAccountProfile("account")).thenReturn(Observable.never())
        `when`(preferences.donationSettings()).thenReturn(Observable.never())
        controller = Robolectric.buildActivity(HostActivity::class.java)
        activity.setTheme(R.style.AppTheme)
        controller.setup()
        home = HomeFragment()
        activity.supportFragmentManager.beginTransaction().replace(android.R.id.content, home, "home").commitNow()
        initialAppBarColor = backgroundColor(home.requireView().findViewById(R.id.app_bar))
        val account = Account("account",
            mapOf(ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
            emptyList(), mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to "REGISTERED"))
        val uri = Uri.fromId("a".repeat(40))
        account.addContact(uri.rawRingId, true)
        account.setContactLabels(uri, setOf("friends"))
        account.loadedSubject.onComplete()
        currentAccount.onNext(account)
        idle()
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) controller.pause().stop().destroy()
    }

    @Test
    fun backResetsEveryTypeAndLabelFilterThenStopsIntercepting() {
        for (filter in listOf(ConversationFilter.Contacts, ConversationFilter.Groups,
            ConversationFilter.Feeds, ConversationFilter.Label("friends"))) {
            chip(filter).performClick()
            assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(chip(ConversationFilter.All).isChecked)
            assertFalse(activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }

    @Test
    fun restoredFilterStillResetsOnBack() {
        chip(ConversationFilter.Label("friends")).performClick()
        controller.recreate()
        home = activity.supportFragmentManager.findFragmentByTag("home") as HomeFragment
        idle()
        assertTrue(chip(ConversationFilter.Label("friends")).isChecked)
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(chip(ConversationFilter.All).isChecked)
    }

    @Test
    fun backLeavesAnOpenConversationBeforeChangingTheHomeFilter() {
        chip(ConversationFilter.Groups).performClick()
        val conversation = Fragment()
        activity.supportFragmentManager.beginTransaction().add(conversation, ConversationFragment.TAG).commitNow()
        activity.conversationBack.isEnabled = true
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(1, activity.conversationBackPresses)
        assertTrue(chip(ConversationFilter.Groups).isChecked)
        activity.supportFragmentManager.beginTransaction().remove(conversation).commitNow()
        activity.conversationBack.isEnabled = false
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(chip(ConversationFilter.All).isChecked)
    }

    @Test
    fun backClosesSearchBeforeResettingTheFilter() {
        chip(ConversationFilter.Groups).performClick()
        home.requireView().findViewById<View>(R.id.new_swarm_fab).performClick()
        idle()
        val search = home.requireView().findViewById<SearchView>(R.id.search_view)
        assertTrue(search.isShowing)
        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertFalse(search.isShowing)
        assertTrue(chip(ConversationFilter.Groups).isChecked)
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(chip(ConversationFilter.All).isChecked)
    }

    @Test
    fun scrollingDoesNotChangeTheAppBarBackground() = assertStableAppBarBackground()

    @Test
    @Config(qualifiers = "night")
    fun scrollingDoesNotChangeTheAppBarBackgroundAtNight() = assertStableAppBarBackground()

    private fun assertStableAppBarBackground() {
        val root = home.requireView() as CoordinatorLayout
        val appBar = root.findViewById<AppBarLayout>(R.id.app_bar)
        val list = root.findViewById<View>(R.id.fragment_container)
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 640)
        root.dispatchDependentViewsChanged(appBar)
        val behavior = (appBar.layoutParams as CoordinatorLayout.LayoutParams).behavior as AppBarLayout.Behavior
        for (dy in listOf(40, -40, 80, -80)) {
            behavior.onNestedPreScroll(root, appBar, list, 0, dy, IntArray(2), ViewCompat.TYPE_TOUCH)
            appBar.isLifted = dy > 0
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertEquals(initialAppBarColor, backgroundColor(appBar))
            idle()
            assertEquals(initialAppBarColor, backgroundColor(appBar))
        }
    }

    private fun backgroundColor(view: View): Int {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        view.background.setBounds(0, 0, 8, 8)
        view.background.draw(Canvas(bitmap))
        return bitmap.getPixel(4, 4).also { bitmap.recycle() }
    }

    private fun chip(filter: ConversationFilter) =
        filters.children.filterIsInstance<Chip>().first { it.tag == filter }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

    @AndroidEntryPoint
    class HostActivity : AppCompatActivity() {
        var conversationBackPresses = 0
        val conversationBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                conversationBackPresses++
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.AppTheme)
            super.onCreate(savedInstanceState)
            onBackPressedDispatcher.addCallback(this, conversationBack)
        }
    }
}
