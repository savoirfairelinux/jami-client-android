/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Amirhossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import cx.ring.R
import cx.ring.account.RenameSwarmDialog
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityContactDetailsBinding
import cx.ring.databinding.ItemContactHorizontalBinding
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ContactPickerFragment
import cx.ring.fragments.ConversationActionsFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.fragments.ConversationGalleryFragment
import cx.ring.fragments.ConversationMembersFragment
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationPreferences
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Call
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.NotificationService
import javax.inject.Inject
import javax.inject.Singleton


@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener,
    ContactPickerFragment.OnContactedPicked, RenameSwarmDialog.RenameSwarmListener {
    @Inject
    @Singleton lateinit
    var mConversationFacade: ConversationFacade

    @Inject lateinit
    var mContactService: ContactService

    @Inject
    @Singleton lateinit
    var mAccountService: AccountService

    private var binding: ActivityContactDetailsBinding? = null
    private var path: ConversationPath? = null

    internal class ContactView(val binding: ItemContactHorizontalBinding, parentDisposable: CompositeDisposable)
        : RecyclerView.ViewHolder(binding.root) {
        var callback: (() -> Unit)? = null
        val disposable = CompositeDisposable()

        init {
            parentDisposable.add(disposable)
            itemView.setOnClickListener {
                try {
                    callback?.invoke()
                } catch (e: Exception) {
                    Log.w(TAG, "Error performing action", e)
                }
            }
        }
    }

    private val mDisposableBag = CompositeDisposable()
    private var mPagerAdapter: ScreenSlidePagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ConversationPath.fromIntent(intent)
        if (path == null) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon()
        val conversation = try {
            mConversationFacade
                .startConversation(path!!.accountId, path!!.conversationUri)
                .blockingGet()
        } catch (e: Throwable) {
            finish()
            return
        }

        val binding = ActivityContactDetailsBinding.inflate(layoutInflater).also { this.binding = it }
        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        val preferences = getConversationPreferences(this, conversation.accountId, conversation.uri)

        mDisposableBag.add(mConversationFacade.observeConversation(conversation)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { finish() }
            .subscribe({ vm ->
                binding.contactImage.setImageDrawable(AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(this))
                binding.title.text = vm.title
                if (conversation.getDescription() != null) binding.description.text = conversation.getDescription()
            }) { e ->
                Log.e(TAG, "e", e)
                finish()
            })

        val color = preferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, resources.getColor(R.color.color_primary_light))
        updateColor(color)
        binding.tabLayout.addOnTabSelectedListener(this)
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (conversation.isGroup()) {
            binding.addMember.isVisible = true
            binding.description.isVisible = true
            binding.addMember.setOnClickListener { ContactPickerFragment().show(supportFragmentManager, ContactPickerFragment.TAG) }
            binding.title.setOnClickListener {
                val title = getString(R.string.dialogtitle_title)
                val hint = getString(R.string.dialog_hint_title)
                RenameSwarmDialog().apply {
                    arguments = Bundle().apply { putString(RenameSwarmDialog.KEY, RenameSwarmDialog.KEY_TITLE) }
                    setTitle(title)
                    setHint(hint)
                    setText(binding.title.text.toString())
                    setListener(this@ContactDetailsActivity)
                }.show(supportFragmentManager, TAG)
            }
            binding.description.setOnClickListener {
                val title = getString(R.string.dialogtitle_description)
                val hint = getString(R.string.dialog_hint_description)
                RenameSwarmDialog().apply {
                    arguments = Bundle().apply { putString(RenameSwarmDialog.KEY, RenameSwarmDialog.KEY_DESCRIPTION) }
                    setTitle(title)
                    setHint(hint)
                    setText(conversation.getDescription())
                    setListener(this@ContactDetailsActivity)
                }.show(supportFragmentManager, TAG)
            }
        }


        mPagerAdapter = ScreenSlidePagerAdapter(this, conversation.accountId, conversation.uri)
        binding.pager.adapter = mPagerAdapter
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.getTabAt(position)!!.select()
            }
        })
    }

    public fun updateColor(color: Int) {
        binding!!.appBar.backgroundTintList = ColorStateList.valueOf(color)
        binding!!.addMember.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishAfterTransition()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        mDisposableBag.dispose()
        super.onDestroy()
        binding = null
    }

    fun goToCallActivity(conversation: Conversation, contactUri: Uri, hasVideo: Boolean) {
        val conf = conversation.currentCall
        if (conf != null && conf.participants.isNotEmpty()
            && conf.participants[0].callStatus != Call.CallStatus.INACTIVE
            && conf.participants[0].callStatus != Call.CallStatus.FAILURE) {
            startActivity(Intent(Intent.ACTION_VIEW)
                .setClass(applicationContext, CallActivity::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, conf.id))
        } else {
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(applicationContext, CallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL)
        }
    }

    private fun goToConversationActivity(accountId: String, conversationUri: Uri) {
        startActivity(Intent(Intent.ACTION_VIEW,
            ConversationPath.toUri(accountId, conversationUri),
            applicationContext,
            ConversationActivity::class.java
        ))
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        binding?.pager?.setCurrentItem(tab!!.position, true)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}



    override fun onContactPicked(accountId: String, contacts: Set<Contact>) {
        mAccountService.addConversationMembers(accountId, path!!.conversationUri.host, contacts.map { contact-> contact.uri.toString() })
    }

    override fun onSwarmRename(key:String, newName: String) {
        val map: MutableMap<String, String> = HashMap()
        if (key == RenameSwarmDialog.KEY_TITLE) {
            map["title"] = newName
        } else if (key == RenameSwarmDialog.KEY_DESCRIPTION) {
            map["description"] = newName
        }
        mAccountService.updateConversationInfo(path!!.accountId, path!!.conversationUri.host, map)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, accountId: String, conversationId: Uri) : FragmentStateAdapter(fa) {
        val fragments: List<Fragment> = listOf(
            ConversationActionsFragment.newInstance(accountId, conversationId),
            ConversationMembersFragment.newInstance(accountId, conversationId),
            ConversationGalleryFragment.newInstance(accountId, conversationId))

            override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

    companion object {
        private val TAG = ContactDetailsActivity::class.simpleName!!
        const val TAB_ABOUT = 0
        const val TAB_MEMBER = 1
        const val TAB_DOCUMENT = 2
    }
}
