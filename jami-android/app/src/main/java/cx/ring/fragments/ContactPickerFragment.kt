/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.window.layout.WindowMetricsCalculator
import cx.ring.R
import cx.ring.adapters.ContactPickerAdapter
import cx.ring.databinding.FragContactPickerBinding
import cx.ring.databinding.ItemSelectedContactBinding
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import java.text.Collator
import javax.inject.Inject

@AndroidEntryPoint
class ContactPickerFragment : AppCompatDialogFragment() {
    private var binding: FragContactPickerBinding? = null
    private var adapter: ContactPickerAdapter? = null
    private val disposables = CompositeDisposable()
    private var availableContacts: List<ConversationItemViewModel> = emptyList()
    private val selectedContactUris = linkedSetOf<String>()
    private var accountId: String? = null
    private var searchQuery = ""
    private lateinit var dataPasser: OnContactedPicked

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_ContactPicker)
        savedInstanceState?.let {
            selectedContactUris.addAll(it.getStringArrayList(STATE_SELECTION).orEmpty())
            accountId = it.getString(STATE_ACCOUNT)
            searchQuery = it.getString(STATE_QUERY).orEmpty()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )
            if (!resources.getBoolean(R.bool.contact_picker_floating)) {
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragContactPickerBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = checkNotNull(binding)
        val addingMembers = arguments?.getBoolean(ARG_ADD_MEMBERS) == true
        binding.toolbar.setTitle(if (addingMembers) R.string.picker_add_members else R.string.new_group)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.createGroupBtn.setText(if (addingMembers) R.string.picker_add_members else R.string.picker_create_group)
        adapter = ContactPickerAdapter(::onContactClicked)
        binding.contactList.adapter = adapter
        binding.contactList.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
        }
        binding.searchContacts.setText(searchQuery)
        binding.searchContacts.doAfterTextChanged {
            searchQuery = it?.toString().orEmpty()
            renderContacts()
        }
        binding.createGroupBtn.setOnClickListener {
            val selection = resolveContactPickerSelection(accountId, selectedContactUris, availableContacts)
            if (selection == null) {
                Log.w(TAG, "Cannot confirm an empty, stale or account-mismatched contact selection")
                renderSelection()
                return@setOnClickListener
            }
            dataPasser.onContactPicked(selection.accountId, selection.contacts)
            dismiss()
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val floating = resources.getBoolean(R.bool.contact_picker_floating)
            root.updatePadding(
                left = if (floating) 0 else systemBars.left,
                top = if (floating) 0 else systemBars.top,
                right = if (floating) 0 else systemBars.right,
                bottom = maxOf(if (floating) 0 else systemBars.bottom, imeBottom)
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
        renderSelection()
        renderContacts()
        observeContacts()
    }

    private fun onContactClicked(item: ConversationItemViewModel) {
        val binding = checkNotNull(binding)
        if (item.accountId != accountId) {
            Log.w(TAG, "Ignoring a contact click from a different picker account")
            return
        }
        val uri = item.getContact()!!.contact.uri.rawUriString
        if (!selectedContactUris.remove(uri)) selectedContactUris.add(uri)
        if (searchQuery.isNotEmpty()) {
            binding.searchContacts.setText("")
            binding.searchContacts.clearFocus()
            dialog?.window?.let {
                WindowCompat.getInsetsController(it, binding.root).hide(WindowInsetsCompat.Type.ime())
            }
        }
        renderSelection()
    }

    private fun observeContacts() {
        val excluded = arguments?.getStringArrayList(ARG_EXCLUDED_CONTACTS).orEmpty().toSet()
        val collator = Collator.getInstance()
        disposables.add(mConversationFacade.getConversationViewModelList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ conversations ->
                val newAccountId = conversations.firstOrNull()?.accountId
                if (accountId != null && newAccountId != null && accountId != newAccountId) {
                    selectedContactUris.clear()
                }
                accountId = newAccountId
                availableContacts = conversations
                    .filter {
                        (it.mode == Conversation.Mode.Legacy || it.mode == Conversation.Mode.OneToOne) &&
                            it.getContact()?.contact?.uri?.rawUriString?.let { uri -> uri !in excluded } == true
                    }
                    .distinctBy { it.accountId to it.getContact()!!.contact.uri }
                    .sortedWith { first, second -> collator.compare(first.title, second.title) }
                selectedContactUris.retainAll(availableContacts.map { it.getContact()!!.contact.uri.rawUriString }.toSet())
                renderSelection()
                renderContacts()
            }, { error -> Log.e(TAG, "Unable to load contacts for the contact picker", error) }))
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        if (resources.getBoolean(R.bool.contact_picker_floating)) {
            val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity()).bounds
            val margin = resources.getDimensionPixelSize(R.dimen.contact_picker_dialog_margin)
            window.setLayout(
                minOf(resources.getDimensionPixelSize(R.dimen.contact_picker_dialog_width), bounds.width() - 2 * margin),
                minOf(resources.getDimensionPixelSize(R.dimen.contact_picker_dialog_height), bounds.height() - 2 * margin)
            )
        } else {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    private fun renderContacts() {
        val binding = binding ?: return
        val query = searchQuery.trim()
        val contacts = availableContacts.filter {
            query.isEmpty() || it.title.contains(query, ignoreCase = true) || it.uriTitle.contains(query, ignoreCase = true)
        }
        adapter?.submitList(contacts)
        binding.emptyState.isVisible = contacts.isEmpty()
        binding.contactList.isVisible = contacts.isNotEmpty()
        binding.noContactTitle.setText(
            if (availableContacts.isEmpty()) R.string.no_contact_to_add else R.string.picker_no_matches
        )
    }

    private fun renderSelection() {
        val binding = binding ?: return
        val selectionChanged = binding.selectedContacts.childCount != selectedContactUris.size ||
            binding.selectedContacts.children.any { it.tag !in selectedContactUris }
        if (selectionChanged && binding.root.isLaidOut) {
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition().apply {
                excludeChildren(binding.contactList, true)
            })
        }
        adapter?.setSelection(selectedContactUris)
        binding.selectionCount.text = resources.getQuantityString(
            R.plurals.picker_selection_count, selectedContactUris.size, selectedContactUris.size
        )
        binding.createGroupBtn.isEnabled =
            resolveContactPickerSelection(accountId, selectedContactUris, availableContacts) != null
        binding.selectedContacts.children.toList().filter { it.tag !in selectedContactUris }
            .forEach { binding.selectedContacts.removeView(it) }
        for (uri in selectedContactUris) {
            val item = availableContacts.firstOrNull { it.getContact()!!.contact.uri.rawUriString == uri } ?: continue
            val selectedContact = binding.selectedContacts.findViewWithTag<View>(uri)
                ?.let { ItemSelectedContactBinding.bind(it) }
                ?: ItemSelectedContactBinding.inflate(layoutInflater, binding.selectedContacts, false).apply {
                    root.tag = uri
                    removeContact.setOnClickListener {
                        selectedContactUris.remove(uri)
                        renderSelection()
                    }
                    binding.selectedContacts.addView(root)
                }
            selectedContact.displayName.text = item.title
            selectedContact.removeContact.contentDescription = getString(R.string.picker_remove_contact, item.title)
            selectedContact.photo.setAvatar(
                AvatarDrawable.Builder()
                    .withContact(item.getContact())
                    .withCircleCrop(true)
                    .withCheck(false)
                    .withPresence(false)
                    .build(selectedContact.photo.context)
            )
        }
        binding.selectedContactsScroll.isVisible = selectedContactUris.isNotEmpty()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(STATE_SELECTION, ArrayList(selectedContactUris))
        outState.putString(STATE_ACCOUNT, accountId)
        outState.putString(STATE_QUERY, searchQuery)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        disposables.clear()
        binding?.contactList?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataPasser = context as OnContactedPicked
    }

    companion object {
        val TAG: String = ContactPickerFragment::class.java.simpleName
        private const val ARG_EXCLUDED_CONTACTS = "excludedContacts"
        private const val ARG_ADD_MEMBERS = "addMembers"
        private const val STATE_SELECTION = "selectedContacts"
        private const val STATE_ACCOUNT = "accountId"
        private const val STATE_QUERY = "searchQuery"

        fun newInstance(contacts: List<Contact>) = ContactPickerFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList(ARG_EXCLUDED_CONTACTS, ArrayList(contacts.map { it.uri.rawUriString }))
                putBoolean(ARG_ADD_MEMBERS, true)
            }
        }
    }

    interface OnContactedPicked {
        fun onContactPicked(accountId: String, contacts: Set<Contact>)
    }
}

internal data class ContactPickerSelection(val accountId: String, val contacts: Set<Contact>)

internal fun resolveContactPickerSelection(
    accountId: String?,
    selectedContactUris: Set<String>,
    availableContacts: List<ConversationItemViewModel>
): ContactPickerSelection? {
    if (accountId.isNullOrBlank() || selectedContactUris.isEmpty()) return null

    val selected = availableContacts.filter {
        it.getContact()?.contact?.uri?.rawUriString in selectedContactUris
    }
    if (selected.any { it.accountId != accountId }) return null

    val contacts = selected.mapNotNull { it.getContact()?.contact }.toSet()
    if (contacts.isEmpty() || contacts.map { it.uri.rawUriString }.toSet() != selectedContactUris) return null

    return ContactPickerSelection(accountId, contacts)
}
