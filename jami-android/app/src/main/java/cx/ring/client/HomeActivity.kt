/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.client

import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.about.AboutFragment
import cx.ring.account.AccountEditionFragment
import cx.ring.account.AccountWizardActivity
import cx.ring.account.JamiAccountSummaryFragment
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityHomeBinding
import cx.ring.fragments.ContactPickerFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.fragments.HomeFragment
import cx.ring.fragments.WelcomeJamiFragment
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.utils.ContentUri
import cx.ring.utils.ContentUri.isJamiLink
import cx.ring.utils.ContentUri.toJamiLink
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.utils.getUiCustomizationFromConfigJson
import cx.ring.viewmodel.WelcomeJamiViewModel
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory.toAdaptiveIcon
import cx.ring.views.twopane.TwoPaneLayout
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.ConfigKey
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.takeFirstWhile
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), ContactPickerFragment.OnContactedPicked {
    private val welcomeJamiViewModel by lazy { ViewModelProvider(this)[WelcomeJamiViewModel::class.java] }
    private var frameContent: Fragment? = null
    private var fConversation: ConversationFragment? = null
    private var fWelcomeJami: WelcomeJamiFragment? = null
    private var mHomeFragment: HomeFragment? = null

    @Inject lateinit
    var mContactService: ContactService

    @Inject lateinit
    var mAccountService: AccountService

    @Inject lateinit
    var mConversationFacade: ConversationFacade

    @Inject lateinit
    var mNotificationService: NotificationService

    private var mBinding: ActivityHomeBinding? = null
    private var mMigrationDialog: AlertDialog? = null
    private val mDisposable = CompositeDisposable()

    private val conversationBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    removeFragment(fConversation)
                    fConversation = null

                    // Hiding the conversation
                    if (mBinding?.panel?.isSlideable == true) { // No space to keep the pane open
                        mBinding?.panel?.closePane()
                    } else showWelcomeFragment()

                    // Next back press doesn't have to be handled by this callback.
                    isEnabled = false
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        JamiApplication.instance?.startDaemon(this)

        mBinding = ActivityHomeBinding.inflate(layoutInflater).also { binding ->
            setContentView(binding.root)
            //supportActionBar?.title = ""
            binding.panel.addPanelListener(object : TwoPaneLayout.PanelListener {
                override fun onPanelOpened(panel: View) {
                    conversationBackPressedCallback.isEnabled = true
                }

                override fun onPanelClosed(panel: View) {
                    conversationBackPressedCallback.isEnabled = false
                    removeFragment(fConversation)
                    removeFragment(fWelcomeJami)
                    fConversation = null
                    fWelcomeJami = null
                }
            })
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mHomeFragment = supportFragmentManager.findFragmentById(R.id.home_fragment) as HomeFragment
        frameContent = supportFragmentManager.findFragmentById(R.id.frame)
        supportFragmentManager.addOnBackStackChangedListener {
            frameContent = supportFragmentManager.findFragmentById(R.id.frame)
        }
        if (frameContent != null) {
            mBinding!!.frame.isVisible = true
        }

        fConversation = supportFragmentManager
            .findFragmentByTag(ConversationFragment::class.java.simpleName)
                as? ConversationFragment?

        if (fConversation != null) {
            Log.w(TAG, "Restore conversation fragment $fConversation")
            conversationBackPressedCallback.isEnabled = true
            mBinding!!.panel.openPane()
        } else {
            Log.w(TAG, "No conversation Restored")
        }
        onBackPressedDispatcher.addCallback(this, conversationBackPressedCallback)
        handleIntent(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        mMigrationDialog?.apply {
            if (isShowing) dismiss()
            mMigrationDialog = null
        }
        frameContent = null
        mHomeFragment = null
        mDisposable.dispose()
        mBinding = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mBinding!!.panel.doOnNextLayout {
            it as TwoPaneLayout

            if (it.isSlideable) {
                if (fConversation == null) {
                    it.closePane()
                    return@doOnNextLayout
                }
                it.openPane() // Force the pane to be open to show the conversation
            } else {
                if (fConversation == null) {
                    showWelcomeFragment()
                    it.openPane()
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent: $intent")
        when (intent.action) {

            NotificationService.NOTIF_TRUST_REQUEST_MULTIPLE -> {
                val accountId = intent.getStringExtra(NotificationService.NOTIF_TRUST_REQUEST_ACCOUNT_ID)

                // Select the current account if it's not active
                if (mAccountService.currentAccount?.accountId != accountId)
                    mAccountService.currentAccount = mAccountService.getAccount(accountId)

                mHomeFragment!!.handleIntent(intent)
            }

            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                val path = ConversationPath.fromBundle(intent.extras)
                if (path != null) {
                    startConversation(path, intent)
                } else {
                    intent.setClass(applicationContext, ShareActivity::class.java)
                    startActivity(intent)
                }
            }

            Intent.ACTION_VIEW, DRingService.ACTION_CONV_ACCEPT -> {
                val intentUri = intent.data
                if (intentUri.isJamiLink()) {
                    mHomeFragment!!.handleIntent(Intent(Intent.ACTION_SEARCH).apply {
                        putExtra(SearchManager.QUERY, intentUri.toJamiLink())
                    })
                } else {
                    val path = ConversationPath.fromUri(intent.data)
                    if (path != null) {
                        // Select the current account if it's not active
                        if (mAccountService.currentAccount?.accountId != path.accountId)
                            mAccountService.currentAccount = mAccountService.getAccount(path.accountId)
                        startConversation(path, intent)
                    }
                }
            }

            Intent.ACTION_SEARCH -> {
                mHomeFragment!!.handleIntent(intent)
            }
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

    private val iconSize by lazy { max(ShortcutManagerCompat.getIconMaxHeight(this), ShortcutManagerCompat.getIconMaxWidth(this)) }
    private val maxShortcuts by lazy { getMaxShareShortcuts() }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()

        mDisposable.add(
            mAccountService.observableAccountList
                .observeOn(DeviceUtils.uiScheduler)
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
            .switchMap { obj -> obj.getConversationsSubject() }
            .debounce(10, TimeUnit.SECONDS, Schedulers.computation())
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
                        .map { vm ->
                            Pair(vm, AvatarDrawable.Builder()
                                .withViewModel(vm)
                                .withCircleCrop(false)
                                .build(this)
                                .toAdaptiveIcon(iconSize))
                        }
                    }) { obs -> obs }
            }
            .subscribe(this::setShareShortcuts)
            { e -> Log.e(TAG, "Error generating conversation shortcuts", e) })

        // Subscribe on account to display correct welcome fragment
        mDisposable.add(
            mAccountService.currentAccountSubject
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { account ->
                    // Can be null if the account doesn't have a config
                    val uiCustomization = try {
                        getUiCustomizationFromConfigJson(
                            configurationJson = JSONObject(account.config[ConfigKey.UI_CUSTOMIZATION]),
                            managerUri = account.config[ConfigKey.MANAGER_URI],
                        )
                    } catch (e: org.json.JSONException) {
                        null // If the JSON is invalid, we don't display the customization
                    }

                    welcomeJamiViewModel.init(
                        isJamiAccount = account.isJami,
                        uiCustomization = uiCustomization,
                    )

                    val currentConversationAccountId =
                        ConversationPath.fromBundle(fConversation?.arguments)?.accountId
                    if (account.accountId != currentConversationAccountId) {
                        mBinding!!.panel.doOnNextLayout {
                            it as TwoPaneLayout
                            if (!it.isSlideable) showWelcomeFragment()
                        }
                    }
                }
        )
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
        mDisposable.clear()
    }

    /**
     * Remove the fragment from the activity.
     * @param fragment the fragment to remove, can be null
     */
    private fun removeFragment(fragment: Fragment?) {
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
    }

    fun showWelcomeFragment() {
        val welcomeJamiFragment = WelcomeJamiFragment()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.conversation,
                welcomeJamiFragment,
                welcomeJamiFragment::class.java.simpleName
            )
            .commit()
        fWelcomeJami = welcomeJamiFragment
        fConversation = null
    }

    fun startConversation(conversationId: String) {
        mDisposable.add(mAccountService.currentAccountSubject
            .firstElement()
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { account ->
                startConversation(account.accountId, Uri.fromString(conversationId))
            })
    }

    fun startConversation(accountId: String, conversationId: Uri) {
        startConversation(ConversationPath(accountId, conversationId))
    }

    private fun startConversation(path: ConversationPath, intent: Intent? = null) {
        val conversation = ConversationFragment().apply {
            arguments = path.toBundle()
        }
        Log.w(TAG, "startConversation $path old:$fConversation ${supportFragmentManager.backStackEntryCount}")

        // If a conversation is already displayed, we replace it,
        // else we add it
        conversationBackPressedCallback.isEnabled = true
        supportFragmentManager.beginTransaction()
            .replace(R.id.conversation, conversation, ConversationFragment.TAG)
            .runOnCommit {
                intent?.let { conversation.handleShareIntent(it) }
            }.commit()
        fConversation = conversation
        mBinding!!.panel.openPane()
    }

    fun goToAdvancedSettings() {
        if (frameContent is SettingsFragment) {
            return
        }
        val fragment = SettingsFragment()
        frameContent = fragment
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.frame, fragment, SettingsFragment.TAG)
            .addToBackStack(SettingsFragment.TAG)
            .commit()
        mBinding!!.frame.isVisible = true
    }

    fun goToAbout() {
        if (frameContent is AboutFragment) {
            return
        }
        val fragment = AboutFragment()
        frameContent = fragment
        mBinding!!.frame.isVisible = true
        supportFragmentManager
            .beginTransaction()
            //.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.frame, fragment, AboutFragment.TAG)
            .addToBackStack(AboutFragment.TAG)
            .commit()
    }

    /** Go to "account settings" parameters. Should be called only if an account is loaded. */
    fun goToAccountSettings() {
        val account = mAccountService.currentAccount
        if (account == null) {
            Log.e(TAG, "No account loaded, cannot open \"Account settings\"")
            return
        }

        if (account.needsMigration()) {
            Log.d(TAG, "launchAccountMigrationActivity: Launch account migration activity")
            val intent = Intent(this, AccountWizardActivity::class.java)
                .setData(
                    android.net.Uri.withAppendedPath(
                        ContentUri.ACCOUNTS_CONTENT_URI, account.accountId
                    )
                )
            startActivityForResult(intent, 1)
        } else {

            if (account.isJami) {   // display JamiAccountSummary

                if (frameContent is JamiAccountSummaryFragment) return

                val fragment = JamiAccountSummaryFragment().apply {
                    arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, account.accountId) }
                }

                // Place it into the frame
                frameContent = fragment
                supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.frame, fragment, JamiAccountSummaryFragment.TAG)
                        .addToBackStack(JamiAccountSummaryFragment.TAG)
                        .commit()

            } else {    //is SIP account --> display SIPView

                // If already on account settings, do nothing
                if (frameContent is AccountEditionFragment) return

                // Create the fragment
                val fragment = AccountEditionFragment()
                fragment.arguments = Bundle().apply {
                    putString(AccountEditionFragment.ACCOUNT_ID_KEY, account.accountId)
                }

                // Place it into the frame
                frameContent = fragment
                supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.frame, fragment, AccountEditionFragment.TAG)
                        .addToBackStack(AccountEditionFragment.TAG)
                        .commit()

            }

            mBinding!!.frame.isVisible = true
        }
    }

    private fun getMaxShareShortcuts() =
        ShortcutManagerCompat.getMaxShortcutCountPerActivity(this).takeIf { it > 0 } ?: 4

    private fun setShareShortcuts(conversations: Array<Any>) {
        val shortcutInfoList = conversations.map { c ->
            val conversation = c as Pair<ConversationItemViewModel, IconCompat>
            val title = conversation.first.title
            val path = ConversationPath(conversation.first.accountId, conversation.first.uri)
            val key = path.toKey()
            ShortcutInfoCompat.Builder(this, key)
                .setIsConversation()
                .setShortLabel(title)
                .setPerson(Person.Builder()
                    .setName(title)
                    .setKey(key)
                    .build())
                .setLongLived(true)
                .setIcon(conversation.second)
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

    override fun onContactPicked(accountId: String, contacts: Set<Contact>) {
        mDisposable.add(mConversationFacade.createConversation(accountId, contacts)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { conversation: Conversation ->
                startConversation(conversation.accountId, conversation.uri)
            })
    }

    companion object {
        val TAG: String = HomeActivity::class.simpleName!!
        const val REQUEST_CODE_CALL = 3
        const val REQUEST_CODE_CONVERSATION = 4
        const val REQUEST_PERMISSION_CAMERA = 113
        const val REQUEST_PERMISSION_READ_STORAGE = 114
        private const val CONVERSATIONS_CATEGORY = "conversations"
    }

}