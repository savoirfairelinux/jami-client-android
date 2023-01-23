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
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.slidingpanelayout.widget.SlidingPaneLayout.PanelSlideListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.about.AboutFragment
import cx.ring.account.AccountEditionFragment
import cx.ring.account.AccountWizardActivity
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityHomeBinding
import cx.ring.fragments.ContactPickerFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.fragments.HomeFragment
import cx.ring.interfaces.Colorable
import cx.ring.service.DRingService
import cx.ring.settings.SettingsFragment
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Contact
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
class HomeActivity : AppCompatActivity(), Colorable, ContactPickerFragment.OnContactedPicked {
    private var frameContent: Fragment? = null
    private var fConversation: ConversationFragment? = null
    private var mHomeFragment: HomeFragment? = null
    private var mOrientation = 0

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
    private val conversationBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                mBinding?.panel?.closePane()
            }
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

        mBinding = ActivityHomeBinding.inflate(layoutInflater).also { binding ->
            setContentView(binding.root)
            //supportActionBar?.title = ""
            binding.panel.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
            binding.panel.addPanelSlideListener(object : PanelSlideListener {
                override fun onPanelSlide(panel: View, slideOffset: Float) {
                }

                override fun onPanelOpened(panel: View) {
                    conversationBackPressedCallback.isEnabled = true
                }

                override fun onPanelClosed(panel: View) {
                    conversationBackPressedCallback.isEnabled = false
                    fConversation?.let { c ->
                        supportFragmentManager.beginTransaction()
                            .remove(c)
                            .commit()
                        fConversation = null
                    }
                }
            })
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mHomeFragment = supportFragmentManager.findFragmentById(R.id.home_fragment) as? HomeFragment?
        frameContent = supportFragmentManager.findFragmentById(fragmentContainerId)
        supportFragmentManager.addOnBackStackChangedListener {
            frameContent = supportFragmentManager.findFragmentById(fragmentContainerId)
        }
        if (frameContent != null) {
            mBinding!!.frame.isVisible = true
        }

        fConversation = supportFragmentManager.findFragmentByTag(ConversationFragment::class.java.simpleName) as? ConversationFragment?
        if (fConversation != null) {
            Log.w(TAG, "Restore conversation fragment $fConversation")
            conversationBackPressedCallback.isEnabled = true
            mBinding!!.conversation.isVisible = true
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
        mDisposable.dispose()
        mBinding = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fConversation?.let { c ->
            mBinding!!.panel.doOnNextLayout { it as SlidingPaneLayout
                if (it.isSlideable && !it.isOpen) {
                    supportFragmentManager.beginTransaction()
                        .remove(c)
                        .commit()
                    fConversation = null
                }
            }
        }
        conversationBackPressedCallback.isEnabled = fConversation != null
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
        if (Intent.ACTION_SEARCH == action) {
            mHomeFragment!!.handleIntent(intent)
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
        val targetSize = (AvatarFactory.SIZE_NOTIF * resources.displayMetrics.density).toInt()
        val maxShortcuts = getMaxShareShortcuts()
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

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
        mDisposable.clear()
    }

    fun toggleConversationVisibility(show: Boolean) {
        //mBinding!!.conversation.isVisible = show
        if (fConversation == null)
            return
        val binding = mBinding ?: return
        if (show)
            binding.panel.openPane()
        else
            binding.panel.closePane()
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

    private fun startConversation(path: ConversationPath) {
        val conversation = ConversationFragment().apply {
            arguments = path.toBundle()
        }
        Log.w(TAG, "startConversation $path old:$fConversation ${supportFragmentManager.backStackEntryCount}")
        //mBinding!!.conversation.getFragment<>()
        if (fConversation == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.conversation, conversation, ConversationFragment::class.java.simpleName)
                .commit()
            mBinding!!.conversation.isVisible = true
        } else
            supportFragmentManager.beginTransaction()
                .replace(R.id.conversation, conversation, ConversationFragment::class.java.simpleName)
                .commit()
        fConversation = conversation
        mBinding!!.panel.openPane()
    }

    private fun presentTrustRequestFragment(accountId: String) {
        mNotificationService.cancelTrustRequestNotification(accountId)
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
            .addToBackStack(SETTINGS_TAG)
            .commit()
        mBinding!!.frame.isVisible = true
    }

    fun goToAbout() {
        if (frameContent is AboutFragment) {
            return
        }
        val content = AboutFragment()
        frameContent = content
        mBinding!!.frame.isVisible = true
        supportFragmentManager
            .beginTransaction()
            //.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(fragmentContainerId, content, ABOUT_TAG)
            .addToBackStack(ABOUT_TAG)
            .commit()
    }

    fun goToAccountSettings() {
        val account = mAccountService.currentAccount
        val bundle = Bundle()
        if (account!!.needsMigration()) {
            Log.d(TAG, "launchAccountMigrationActivity: Launch account migration activity")
            val intent = Intent()
                .setClass(this, AccountWizardActivity::class.java)
                .setData(
                    android.net.Uri.withAppendedPath(ContentUriHandler.ACCOUNTS_CONTENT_URI, account.accountId)
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
                mBinding!!.frame.isVisible = true
            }
        }
    }

    override fun setColor(color: Int) {
        //mToolbar.setBackground(new ColorDrawable(color));
    }

    fun setToolbarElevation(enable: Boolean) {
//        if (mBinding != null) mBinding!!.appBar.elevation = if (enable) resources.getDimension(R.dimen.toolbar_elevation) else 0f
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
                .setIsConversation()
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
        const val REQUEST_CODE_PHOTO = 5
        const val REQUEST_CODE_GALLERY = 6
        const val REQUEST_CODE_QR_CONVERSATION = 7
        const val REQUEST_PERMISSION_CAMERA = 113
        const val REQUEST_PERMISSION_READ_STORAGE = 114
        const val ACCOUNTS_TAG = "Accounts"
        const val ABOUT_TAG = "About"
        const val SETTINGS_TAG = "Prefs"
        const val ACTION_PRESENT_TRUST_REQUEST_FRAGMENT =
            BuildConfig.APPLICATION_ID + "presentTrustRequestFragment"
        private const val CONVERSATIONS_CATEGORY = "conversations"
        private const val fragmentContainerId: Int = R.id.frame
    }

}
