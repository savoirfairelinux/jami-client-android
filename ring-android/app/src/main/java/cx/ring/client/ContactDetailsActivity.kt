/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityContactDetailsBinding
import cx.ring.databinding.ItemContactActionBinding
import cx.ring.databinding.ItemContactHorizontalBinding
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationPreferences
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.*
import net.jami.services.ConversationFacade
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.NotificationService
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity() {
    @Inject
    @Singleton lateinit
    var mConversationFacade: ConversationFacade

    @Inject lateinit
    var mContactService: ContactService

    @Inject
    @Singleton lateinit
    var mAccountService: AccountService

    private var binding: ActivityContactDetailsBinding? = null

    internal class ContactAction {
        @DrawableRes
        val icon: Int
        val drawable: Single<Drawable>?
        val title: CharSequence
        val callback: () -> Unit
        var iconTint: Int
        var iconSymbol: CharSequence? = null

        constructor(@DrawableRes i: Int, tint: Int, t: CharSequence, cb: () -> Unit) {
            icon = i
            iconTint = tint
            title = t
            callback = cb
            drawable = null
        }

        constructor(@DrawableRes i: Int, t: CharSequence, cb: () -> Unit) {
            icon = i
            iconTint = Color.BLACK
            title = t
            callback = cb
            drawable = null
        }

        constructor(d: Single<Drawable>?, t: CharSequence, cb: () -> Unit) {
            drawable = d
            icon = 0
            iconTint = Color.BLACK
            title = t
            callback = cb
        }

        fun setSymbol(t: CharSequence?) {
            iconSymbol = t
        }
    }

    internal class ContactActionView(
        val binding: ItemContactActionBinding,
        parentDisposable: CompositeDisposable
    ) : RecyclerView.ViewHolder(binding.root) {
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

    private class ContactActionAdapter(private val disposable: CompositeDisposable) :
        RecyclerView.Adapter<ContactActionView>() {
        val actions = ArrayList<ContactAction>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactActionView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemContactActionBinding.inflate(layoutInflater, parent, false)
            return ContactActionView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactActionView, position: Int) {
            val action = actions[position]
            holder.disposable.clear()
            if (action.drawable != null) {
                holder.disposable.add(action.drawable.subscribe { background: Drawable ->
                    holder.binding.actionIcon.background = background
                })
            } else {
                holder.binding.actionIcon.setBackgroundResource(action.icon)
                holder.binding.actionIcon.text = action.iconSymbol
                if (action.iconTint != Color.BLACK) ViewCompat.setBackgroundTintList(
                    holder.binding.actionIcon,
                    ColorStateList.valueOf(action.iconTint)
                )
            }
            holder.binding.actionTitle.text = action.title
            holder.callback = action.callback
        }

        override fun onViewRecycled(holder: ContactActionView) {
            holder.disposable.clear()
            holder.binding.actionIcon.background = null
        }

        override fun getItemCount(): Int {
            return actions.size
        }
    }

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

    private class ContactViewAdapter(
        private val disposable: CompositeDisposable,
        private val contacts: List<ContactViewModel>,
        private val callback: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactView>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactView {
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemContactHorizontalBinding.inflate(layoutInflater, parent, false)
            return ContactView(itemBinding, disposable)
        }

        override fun onBindViewHolder(holder: ContactView, position: Int) {
            val contact = contacts[position]
            holder.disposable.clear()
            holder.disposable.add(AvatarFactory.getAvatar(holder.itemView.context, contact, false)
                .subscribe { drawable: Drawable ->
                    holder.binding.photo.setImageDrawable(drawable)
                })
            holder.binding.displayName.text =
                if (contact.contact.isUser) holder.itemView.context.getText(R.string.conversation_info_contact_you) else contact.displayName
            holder.itemView.setOnClickListener { callback.invoke(contact.contact) }
        }

        override fun onViewRecycled(holder: ContactView) {
            holder.disposable.clear()
            holder.binding.photo.setImageDrawable(null)
        }

        override fun getItemCount(): Int {
            return contacts.size
        }
    }

    private val mDisposableBag = CompositeDisposable()
    private val adapter = ContactActionAdapter(mDisposableBag)
    private var colorAction: ContactAction? = null
    private var symbolAction: ContactAction? = null
    private var colorActionPosition = 0
    private var symbolActionPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = ConversationPath.fromIntent(intent)
        if (path == null) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon()
        val conversation = try {
            mConversationFacade
                .startConversation(path.accountId, path.conversationUri)
                .blockingGet()
        } catch (e: Throwable) {
            finish()
            return
        }

        val binding = ActivityContactDetailsBinding.inflate(layoutInflater).also { this.binding = it }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        val preferences = getConversationPreferences(this, conversation.accountId, conversation.uri)
        colorActionPosition = 0
        symbolActionPosition = 1

        mDisposableBag.add(mConversationFacade.observeConversation(conversation)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { vm ->
                binding.contactImage.setImageDrawable(AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(this))
                supportActionBar?.title = vm.title
                binding.contactListLayout.visibility =
                    if (conversation.isSwarm) View.VISIBLE else View.GONE
                if (conversation.isSwarm) {
                    binding.conversationId.text = vm.uri.toString()
                    binding.contactList.adapter = ContactViewAdapter(mDisposableBag, vm.contacts)
                    { contact -> copyAndShow(contact.uri.rawUriString) }
                } else {
                    binding.conversationId.text = vm.uriTitle
                }
            })

        /*Map<String, String> details = Ringservice.getCertificateDetails(conversation.getContact().getUri().getRawRingId());
        for (Map.Entry<String, String> e : details.entrySet()) {
            Log.w(TAG, e.getKey() + " -> " + e.getValue());
        }*/
        @StringRes val infoString = if (conversation.isSwarm)
            if (conversation.mode.blockingFirst() == Conversation.Mode.OneToOne)
                R.string.conversation_type_private
            else
                R.string.conversation_type_group
        else R.string.conversation_type_contact
        /*@DrawableRes int infoIcon = conversation.isSwarm()
                ? (conversation.getMode() == Conversation.Mode.OneToOne
                ? R.drawable.baseline_person_24
                : R.drawable.baseline_group_24)
                : R.drawable.baseline_person_24;*/
        //adapter.actions.add(new ContactAction(R.drawable.baseline_info_24, getText(infoString), () -> {}));
        binding.conversationType.setText(infoString)
        //binding.conversationType.setCompoundDrawables(getDrawable(infoIcon), null, null, null);
        colorAction = ContactAction(R.drawable.item_color_background, 0, getText(R.string.conversation_preference_color)) {
            ColorChooserBottomSheet { color ->
                colorAction!!.iconTint = color
                adapter.notifyItemChanged(colorActionPosition)
                preferences.edit()
                    .putInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, color)
                    .apply()
            }.show(supportFragmentManager, "colorChooser")
        }
        val color = preferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, resources.getColor(R.color.color_primary_light))
        colorAction!!.iconTint = color
        adapter.actions.add(colorAction!!)
        symbolAction = ContactAction(0, getText(R.string.conversation_preference_emoji)) {
            EmojiChooserBottomSheet{ emoji ->
                symbolAction?.setSymbol(emoji)
                adapter.notifyItemChanged(symbolActionPosition)
                preferences.edit()
                    .putString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, emoji)
                    .apply()
            }.show(supportFragmentManager, "colorChooser")
        }.apply {
            setSymbol(preferences.getString(ConversationFragment.KEY_PREFERENCE_CONVERSATION_SYMBOL, resources.getString(R.string.conversation_default_emoji)))
            adapter.actions.add(this)
        }
        val conversationUri = conversation.uri.toString()//if (conversation.isSwarm) conversation.uri.toString() else conversation.uriTitle
        if (conversation.contacts.size <= 2 && conversation.contacts.isNotEmpty()) {
            val contact = conversation.contact!!
            adapter.actions.add(ContactAction(R.drawable.baseline_call_24, getText(R.string.ab_action_audio_call)) {
                goToCallActivity(conversation, contact.uri, true)
            })
            adapter.actions.add(ContactAction(R.drawable.baseline_videocam_24, getText(R.string.ab_action_video_call)) {
                goToCallActivity(conversation, contact.uri, false)
            })
            if (!conversation.isSwarm) {
                adapter.actions.add(ContactAction(R.drawable.baseline_clear_all_24, getText(R.string.conversation_action_history_clear)) {
                    MaterialAlertDialogBuilder(this@ContactDetailsActivity)
                        .setTitle(R.string.clear_history_dialog_title)
                        .setMessage(R.string.clear_history_dialog_message)
                        .setPositiveButton(R.string.conversation_action_history_clear) { _: DialogInterface?, _: Int ->
                            mConversationFacade.clearHistory(conversation.accountId, contact.uri).subscribe()
                            Snackbar.make(binding.root, R.string.clear_history_completed, Snackbar.LENGTH_LONG).show()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show()
                })
            }
            adapter.actions.add(ContactAction(R.drawable.baseline_block_24, getText(R.string.conversation_action_block_this)) {
                MaterialAlertDialogBuilder(this@ContactDetailsActivity)
                    .setTitle(getString(R.string.block_contact_dialog_title, conversationUri))
                    .setMessage(getString(R.string.block_contact_dialog_message, conversationUri))
                    .setPositiveButton(R.string.conversation_action_block_this) { _: DialogInterface?, _: Int ->
                        mAccountService.removeContact(conversation.accountId, contact.uri.rawRingId,true)
                        Toast.makeText(applicationContext, getString(R.string.block_contact_completed, conversationUri), Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show()
            })
        }
        binding.conversationId.text = conversationUri
        binding.infoCard.setOnClickListener { copyAndShow(path.conversationId) }
        binding.contactActionList.adapter = adapter
    }

    private fun copyAndShow(toCopy: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getText(R.string.clip_contact_uri), toCopy))
        Snackbar.make(binding!!.root, getString(R.string.conversation_action_copied_peer_number_clipboard, toCopy), Snackbar.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishAfterTransition()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        adapter.actions.clear()
        mDisposableBag.dispose()
        super.onDestroy()
        colorAction = null
        binding = null
    }

    private fun goToCallActivity(conversation: Conversation, contactUri: Uri, hasVideo: Boolean) {
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

    companion object {
        private val TAG = ContactDetailsActivity::class.simpleName!!
    }
}
