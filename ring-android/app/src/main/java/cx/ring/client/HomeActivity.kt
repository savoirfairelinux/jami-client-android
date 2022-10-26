/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.client

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.about.AboutFragment
import cx.ring.account.AccountEditionFragment
import cx.ring.account.AccountWizardActivity
import cx.ring.application.JamiApplication
import cx.ring.contactrequests.ContactRequestsFragment
import cx.ring.databinding.ActivityHomeBinding
import cx.ring.fragments.ConversationFragment
import cx.ring.fragments.SmartListFragment
import cx.ring.interfaces.Colorable
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.settings.VideoSettingsFragment
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.settings.pluginssettings.PluginPathPreferenceFragment
import cx.ring.settings.pluginssettings.PluginSettingsFragment
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.takeFirstWhile
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener, Colorable {
    private var pagerContent: Fragment? = null
    private var frameContent: Fragment? = null
    private var fConversation: ConversationFragment? = null
    private var mPagerAdapter: ScreenSlidePagerAdapter? = null
//    private var mOutlineProvider: ViewOutlineProvider? = null
    private var mOrientation = 0
    private var mHasConversationBadge = false
    private var mHasPendingBadge = false

    @Inject
    lateinit
    var mContactService: ContactService

    @Inject
    lateinit
    var mAccountService: AccountService

    @Inject
    lateinit
    var mConversationFacade: ConversationFacade

    @Inject
    lateinit
    var mNotificationService: NotificationService

    private var mBinding: ActivityHomeBinding? = null
    private var mMigrationDialog: AlertDialog? = null
    private val mDisposable = CompositeDisposable()

    /* called before activity is killed, e.g. rotation */
    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt("orientation", mOrientation)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mOrientation = savedInstanceState.getInt("orientation")
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon()

        // Switch to TV if appropriate (could happen with buggy launcher)
        if (DeviceUtils.isTv(this)) {
            Log.d(TAG, "Switch to TV")
            val intent = intent
            intent.setClass(this, cx.ring.tv.main.HomeActivity::class.java)
            finish()
            startActivity(intent)
            return
        }

        if (!DeviceUtils.isTablet(this)) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attr = window.attributes
            attr.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        mPagerAdapter = ScreenSlidePagerAdapter(this)

        mBinding = ActivityHomeBinding.inflate(layoutInflater).also { binding ->
            setContentView(binding.root)
            supportActionBar?.title = ""
            binding.tabLayout.addOnTabSelectedListener(this)
//            mOutlineProvider = binding.appBar.outlineProvider
            binding.pager.adapter = mPagerAdapter
            binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.tabLayout.getTabAt(position)!!.select()
                    pagerContent = mPagerAdapter!!.fragments[position] as Fragment
                }
            })
            ViewCompat.setOnApplyWindowInsetsListener(binding.pager) { v, insets ->
                v.updatePadding(0,0,0,insets.systemWindowInsetBottom)
                insets
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.frame) { v, insets ->
                v.updatePadding(0,0,0,insets.systemWindowInsetBottom)
                insets
            }
        }
        handleIntent(intent)

        net.jami.utils.Log.d("panel", "${mBinding!!.panel.isSlideable}")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val panel = mBinding!!.panel
                
                if (panel.isSlideable && panel.isOpen) {
                    panel.closePane()
                    return
                }

                val pager = mBinding?.pager
                if (pager?.visibility == View.VISIBLE && pager.currentItem == TAB_INVITATIONS) {
                    pager.currentItem = TAB_CONVERSATIONS
                    return
                }
                
                if (mBinding!!.frame.isVisible){
                    popFragmentImmediate()
                    mBinding!!.mainToolbar.isVisible = false
                    mBinding!!.tabLayout.isVisible = mHasPendingBadge
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        setSupportActionBar(mBinding!!.mainToolbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (!DeviceUtils.isTablet(this)) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMigrationDialog?.apply {
            if (isShowing) dismiss()
            mMigrationDialog = null
        }
        pagerContent = null
        frameContent = null
        mDisposable.dispose()
        mBinding = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent: $intent")
        val extra = intent.extras
        val action = intent.action
        when (action) {
            ACTION_PRESENT_TRUST_REQUEST_FRAGMENT ->
                presentTrustRequestFragment(extra?.getString(AccountEditionFragment.ACCOUNT_ID_KEY) ?: return)
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE -> {
                val path = ConversationPath.fromBundle(extra)
                if (path != null) {
                    startConversation(path)
                } else {
                    intent.setClass(applicationContext, ShareActivity::class.java)
                    startActivity(intent)
                }
            }
            Intent.ACTION_VIEW,
            DRingService.ACTION_CONV_ACCEPT -> {
                val path = ConversationPath.fromIntent(intent)
                if (path != null)
                    startConversation(path)
            }
        }
        val fragment = mPagerAdapter!!.fragments[TAB_CONVERSATIONS]
        if (Intent.ACTION_SEARCH == action) {
                (fragment as SmartListFragment).handleIntent(intent)
        }
    }

    private fun showMigrationDialog() {
        if (mMigrationDialog != null) {
            return
        }
        mMigrationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.account_migration_title_dialog)
            .setMessage(R.string.account_migration_message_dialog)
            .setIcon(R.drawable.baseline_warning_24)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                goToAccountSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        mDisposable.add(
            mAccountService.observableAccountList
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { accounts: List<Account> ->
                    if (accounts.isEmpty()) {
                        startActivity(Intent(this, AccountWizardActivity::class.java))
                    }
                    for (account in accounts) {
                        if (account.needsMigration()) {
                            showMigrationDialog()
                            break
                        }
                    }
                })
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadPending }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { count -> setBadge(TAB_INVITATIONS, count) })
        mDisposable.add(mAccountService
            .currentAccountSubject
            .switchMap { obj -> obj.unreadConversations }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                    count -> setBadge(TAB_CONVERSATIONS, count)
            })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val targetSize = (AvatarFactory.SIZE_NOTIF * resources.displayMetrics.density).toInt()
            val maxShortcuts = getMaxShareShortcuts()
            mDisposable.add(mAccountService
                .currentAccountSubject
                .switchMap { obj -> obj.getConversationsSubject() }
                .debounce(10, TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .map { conversations -> conversations.takeFirstWhile(maxShortcuts)
                    { it.mode.blockingFirst() != Conversation.Mode.Syncing }
                }
                .switchMapSingle { conversations ->
                    if (conversations.isEmpty())
                        Single.just(emptyArray<Any>())
                    else
                        Single.zip(conversations.mapTo(ArrayList(conversations.size))
                        { c -> mContactService.getLoadedConversation(c)
                            .observeOn(Schedulers.computation())
                            .map { vm -> Pair(vm, BitmapUtils.drawableToBitmap(AvatarDrawable.Builder()
                                .withViewModel(vm)
                                .withCircleCrop(true)
                                .build(this), targetSize))
                            }
                        }) { obs -> obs }
                }
                .subscribe(this::setShareShortcuts)
                { e -> Log.e(TAG, "Error generating conversation shortcuts", e) })
        }
        if (fConversation == null)
            fConversation =
                supportFragmentManager.findFragmentByTag(ConversationFragment::class.java.simpleName) as ConversationFragment?
        val newOrientation = resources.configuration.orientation
        if (mOrientation != newOrientation) {
            mOrientation = newOrientation
            if (DeviceUtils.isTablet(this)) {
                goToHome()
            } else {
                // Remove ConversationFragment that might have been restored after an orientation change
                if (fConversation != null) {
                    supportFragmentManager
                        .beginTransaction()
                        .remove(fConversation!!)
                        .commitNow()
                    fConversation = null
                }
            }
        }

        // Select first conversation in tablet mode
        if (DeviceUtils.isTablet(this)) {
            val intent = intent
            val uri = intent?.data
            if ((intent == null || uri == null) && fConversation == null) {
                var smartlist: Observable<List<Observable<ConversationItemViewModel>>>? = null
                smartlist = if (mBinding?.pager!!.currentItem == TAB_CONVERSATIONS)
                    mConversationFacade.getSmartList(false)
                else mConversationFacade.pendingList
                mDisposable.add(smartlist
                    .filter { list -> list.isNotEmpty() }
                    .map { list -> list[0].firstOrError() }
                    .firstElement()
                    .flatMapSingle { e -> e }
					.observeOn(AndroidSchedulers.mainThread())
                    .subscribe { element ->
                        startConversation(element.accountId, element.uri)
                    })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mDisposable.clear()
    }

    fun toggleConversationVisibility(show: Boolean) {
        mBinding!!.conversation.isVisible = show
    }

    fun startConversation(conversationId: String) {
        mDisposable.add(mAccountService.currentAccountSubject
            .firstElement()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { account ->
                startConversation(account.accountId, Uri.fromString(conversationId))
            })
    }

    fun startConversation(accountId: String, conversationId: Uri) {
        startConversation(ConversationPath(accountId, conversationId))
    }

    private fun startConversation(path: ConversationPath) {
        fConversation = ConversationFragment()
        fConversation!!.arguments = path.toBundle()
        mBinding!!.panel.openPane()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.conversation,
                fConversation!!,
                ConversationFragment::class.java.simpleName
            )
            .commit()
    }

    private fun presentTrustRequestFragment(accountId: String) {
        mNotificationService.cancelTrustRequestNotification(accountId)
        val fragment = mPagerAdapter!!.fragments[TAB_INVITATIONS]
        (fragment as ContactRequestsFragment).presentForAccount(accountId)
        mBinding!!.pager.currentItem = TAB_INVITATIONS
    }

    fun goToHome() {
        mBinding!!.tabLayout.getTabAt(TAB_CONVERSATIONS)!!.select()
        pagerContent = SmartListFragment()
    }

    fun goToAdvancedSettings() {
        if (frameContent is SettingsFragment) {
            return
        }
        val content = SettingsFragment()
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, SETTINGS_TAG)
            .addToBackStack(SETTINGS_TAG).commit()
        if (!DeviceUtils.isTablet(this)) {
            mBinding!!.tabLayout.isVisible = false
        }
        mBinding!!.frame.isVisible = true
        mBinding!!.tabLayout.isVisible = false
    }

    fun goToAbout() {
        if (frameContent is AboutFragment) {
            return
        }
        val content = AboutFragment()
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, ABOUT_TAG)
            .addToBackStack(ABOUT_TAG).commit()
        if (!DeviceUtils.isTablet(this)) {
            mBinding!!.tabLayout.isVisible = false
        }
        mBinding!!.frame.isVisible = true
        mBinding!!.tabLayout.isVisible = false
    }

    fun goToVideoSettings() {
        if (frameContent is VideoSettingsFragment) {
            return
        }
        val content = VideoSettingsFragment()
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, VIDEO_SETTINGS_TAG)
            .addToBackStack(VIDEO_SETTINGS_TAG).commit()
        if (!DeviceUtils.isTablet(this)) {
            mBinding!!.tabLayout.isVisible = false
        }
        mBinding!!.frame.isVisible = true
        mBinding!!.tabLayout.isVisible = false
    }

    fun goToAccountSettings() {
        val account = mAccountService.currentAccount
        val bundle = Bundle()
        if (account!!.needsMigration()) {
            Log.d(TAG, "launchAccountMigrationActivity: Launch account migration activity")
            val intent = Intent()
                .setClass(this, AccountWizardActivity::class.java)
                .setData(
                    android.net.Uri.withAppendedPath(
                        ContentUriHandler.ACCOUNTS_CONTENT_URI,
                        account.accountId
                    )
                )
            startActivityForResult(intent, 1)
        } else {
            Log.d(TAG, "launchAccountEditFragment: Launch account edit fragment")
            bundle.putString(AccountEditionFragment.ACCOUNT_ID_KEY, account.accountId)
            if (frameContent !is AccountEditionFragment) {
                val content = AccountEditionFragment()
                content.arguments = bundle
                frameContent = content
                supportFragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(fragmentContainerId, content, ACCOUNTS_TAG)
                    .addToBackStack(ACCOUNTS_TAG)
                    .commit()
                if (!DeviceUtils.isTablet(this)) {
                    mBinding!!.tabLayout.isVisible = false
                }
                mBinding!!.frame.isVisible = true
                mBinding!!.tabLayout.isVisible = false
            }
        }
    }

    private fun setBadge(menuId: Int, number: Int) {
        val tab = mBinding!!.tabLayout.getTabAt(menuId)
        if (number == 0) {
            tab!!.removeBadge()
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = false else mHasPendingBadge = false
            if (pagerContent is ContactRequestsFragment) goToHome()
        } else {
            tab!!.orCreateBadge.number = number
            mBinding!!.tabLayout.isVisible = true
            if (menuId == TAB_CONVERSATIONS) mHasConversationBadge = true else mHasPendingBadge = true
        }
        mBinding!!.tabLayout.isVisible = mHasPendingBadge
        mBinding!!.pager.isUserInputEnabled = mHasConversationBadge || mHasPendingBadge
    }

    fun setToolbarTitle(@StringRes titleRes: Int) {
        mBinding?.mainToolbar?.let { toolbar ->
            toolbar.isVisible = true
            toolbar.title = getString(titleRes)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true);
        }
    }

    private val fragmentContainerId: Int
        get() = R.id.frame

    /**
     * Changes the current main fragment to a plugins list settings fragment
     */
    fun goToPluginsListSettings(accountId: String? = "") {
        if (frameContent is PluginsListSettingsFragment) {
            return
        }
        val content = PluginsListSettingsFragment.newInstance(accountId)
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGINS_LIST_SETTINGS_TAG)
            .addToBackStack(PLUGINS_LIST_SETTINGS_TAG).commit()
        mBinding!!.tabLayout.isVisible = false
    }

    /**
     * Changes the current main fragment to a plugin settings fragment
     * @param pluginDetails
     */
    fun gotToPluginSettings(pluginDetails: PluginDetails) {
        if (frameContent is PluginSettingsFragment) {
            return
        }
        val content = PluginSettingsFragment.newInstance(pluginDetails)
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGIN_SETTINGS_TAG)
            .addToBackStack(PLUGIN_SETTINGS_TAG).commit()
    }

    /**
     * Changes the current main fragment to a plugin PATH preference fragment
     */
    fun gotToPluginPathPreference(pluginDetails: PluginDetails, preferenceKey: String) {
        if (frameContent is PluginPathPreferenceFragment) {
            return
        }
        val content = PluginPathPreferenceFragment.newInstance(pluginDetails, preferenceKey)
        frameContent = content
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(fragmentContainerId, content, PLUGIN_PATH_PREFERENCE_TAG)
            .addToBackStack(PLUGIN_PATH_PREFERENCE_TAG).commit()
    }

    override fun setColor(color: Int) {
        //mToolbar.setBackground(new ColorDrawable(color));
    }

    fun setToolbarElevation(enable: Boolean) {
//        if (mBinding != null) mBinding!!.appBar.elevation = if (enable) resources.getDimension(R.dimen.toolbar_elevation) else 0f
    }

    fun setToolbarOutlineState(enabled: Boolean) {
        if (mBinding != null) {
//            if (!enabled) {
//                mBinding!!.appBar.outlineProvider = null
//            } else {
//                mBinding!!.appBar.outlineProvider = mOutlineProvider
//            }
        }
    }

    fun popFragmentImmediate() {
        val fm = supportFragmentManager
        fm.popBackStackImmediate()
        frameContent = fm.fragments.lastOrNull()
        mBinding!!.frame.isVisible = false
    }

    private fun enableAccount(newValue: Boolean) {
        val account = mAccountService.currentAccount
        if (account == null) {
            Log.w(TAG, "account not found!")
            return
        }
        account.isEnabled = newValue
        mAccountService.setAccountEnabled(account.accountId, newValue)
    }

    private fun getMaxShareShortcuts() =
        ShortcutManagerCompat.getMaxShortcutCountPerActivity(this).takeIf { it > 0 } ?: 4

    private fun setShareShortcuts(conversations: Array<Any>) {
        val shortcutInfoList = conversations.map { c ->
            val conversation = c as Pair<ConversationItemViewModel, Bitmap>
            var icon: IconCompat? = null
            try {
                icon = IconCompat.createWithBitmap(conversation.second)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load icon", e)
            }
            val title = conversation.first.title
            val path = ConversationPath(conversation.first.accountId, conversation.first.uri)
            val key = path.toKey()
            ShortcutInfoCompat.Builder(this, key)
                .setShortLabel(title)
                .setPerson(Person.Builder()
                    .setName(title)
                    .setKey(key)
                    .build())
                .setLongLived(true)
                .setIcon(icon)
                .setCategories(setOf(CONVERSATIONS_CATEGORY))
                .setIntent(Intent(Intent.ACTION_SEND, android.net.Uri.EMPTY, this, HomeActivity::class.java)
                        .putExtras(path.toBundle()))
                .build()
        }
        try {
            Log.d(TAG, "Adding shortcuts: " + shortcutInfoList.size)
            ShortcutManagerCompat.removeAllDynamicShortcuts(this)
            ShortcutManagerCompat.addDynamicShortcuts(this, shortcutInfoList)
        } catch (e: Exception) {
            Log.w(TAG, "Error adding shortcuts", e)
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        mBinding?.pager?.setCurrentItem(tab!!.position, true)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        val fragments = listOf(SmartListFragment(), ContactRequestsFragment())

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position] as Fragment
        }
    }

    companion object {
        val TAG: String = HomeActivity::class.simpleName!!
        const val REQUEST_CODE_CALL = 3
        const val REQUEST_CODE_CONVERSATION = 4
        const val REQUEST_CODE_PHOTO = 5
        const val REQUEST_CODE_GALLERY = 6
        const val REQUEST_CODE_QR_CONVERSATION = 7
        const val REQUEST_PERMISSION_CAMERA = 113
        const val REQUEST_PERMISSION_READ_STORAGE = 114
        const val ACCOUNTS_TAG = "Accounts"
        const val ABOUT_TAG = "About"
        const val SETTINGS_TAG = "Prefs"
        const val VIDEO_SETTINGS_TAG = "VideoPrefs"
        const val ACTION_PRESENT_TRUST_REQUEST_FRAGMENT =
            BuildConfig.APPLICATION_ID + "presentTrustRequestFragment"
        const val PLUGINS_LIST_SETTINGS_TAG = "PluginsListSettings"
        const val PLUGIN_SETTINGS_TAG = "PluginSettings"
        const val PLUGIN_PATH_PREFERENCE_TAG = "PluginPathPreference"
        private const val CONVERSATIONS_CATEGORY = "conversations"
        private const val TAB_CONVERSATIONS = 0
        private const val TAB_INVITATIONS = 1
    }

}
