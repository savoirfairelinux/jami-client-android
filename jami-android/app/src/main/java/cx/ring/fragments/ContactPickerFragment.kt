/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.adapters.ContactPickerAdapter
import cx.ring.databinding.DialogSwarmTitleBinding
import cx.ring.databinding.FragContactPickerBinding
import cx.ring.repositories.CircleRepository
import cx.ring.viewholders.ContactPickerViewHolder.ContactPickerListeners
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Circle
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ContactPickerFragment(val contacts: List<Contact> = emptyList()) : BottomSheetDialogFragment() {
    private var binding: FragContactPickerBinding? = null
    private var adapter: ContactPickerAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private var mAccountId: String? = null
    private val selectedById: LinkedHashMap<String, Contact> = LinkedHashMap()
    private val contactVmById: LinkedHashMap<String, ConversationItemViewModel> = LinkedHashMap()
    private val selectedCircleIds: MutableSet<String> = LinkedHashSet()
    private var currentCircles: List<Circle> = emptyList()
    private var renderedChipsKey: Pair<List<Circle>, Set<String>>? = null
    lateinit var dataPasser: OnContactedPicked

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var circleRepository: CircleRepository

    private val existingContactIds: Set<String> by lazy {
        contacts.map { normalizeId(it.primaryNumber) }.filter { it.isNotEmpty() }.toSet()
    }

    private fun normalizeId(raw: String): String =
        Uri.fromString(raw.trim()).rawRingId
            .removePrefix("ring:")
            .removePrefix("jami:")
            .trim()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDisposableBag.add(
            Observable.combineLatest(
                observeContactsWithPresence(),
                circleRepository.observeForCurrentAccount()
            ) { conversations, circles -> conversations to circles }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (conversations, circles) ->
                    val uniqueContacts = LinkedHashMap<String, ConversationItemViewModel>()
                    for (vm in conversations) {
                        if (vm.mode != Conversation.Mode.Legacy && vm.mode != Conversation.Mode.OneToOne) continue
                        val contact = vm.getContact()?.contact ?: continue
                        if (contact.isUser) continue
                        val cid = normalizeId(contact.primaryNumber)
                        if (cid.isEmpty() || cid in existingContactIds) continue
                        if (cid !in uniqueContacts) {
                            vm.isChecked = cid in selectedById
                            uniqueContacts[cid] = vm
                        }
                    }

                    contactVmById.clear()
                    contactVmById.putAll(uniqueContacts)
                    if (mAccountId == null) {
                        mAccountId = uniqueContacts.values.firstOrNull()?.accountId
                            ?: accountService.currentAccount?.accountId
                    }

                    val contactsNotInGroup = uniqueContacts.values.toList()
                    if (contactsNotInGroup.isEmpty()) {
                        binding?.noContactTitle?.visibility = View.VISIBLE
                        binding?.noContactLogo?.visibility = View.VISIBLE
                    } else {
                        binding?.noContactTitle?.visibility = View.GONE
                        binding?.noContactLogo?.visibility = View.GONE
                    }
                    adapter?.update(contactsNotInGroup)

                    currentCircles = circles
                    // Presence changes must not rebuild the chips
                    val chipsKey = circles to uniqueContacts.keys.toSet()
                    if (chipsKey != renderedChipsKey) {
                        renderedChipsKey = chipsKey
                        renderCircleChips(circles)
                    }
                }) { e -> Log.e(TAG, "No contact to create a group!", e) }
        )
    }

    /** 1:1 conversations observed with presence, so each avatar shows its presence badge. */
    private fun observeContactsWithPresence(): Observable<List<ConversationItemViewModel>> =
        accountService.currentAccountSubject.switchMap { account ->
            account.getConversationsSubject().switchMap { conversations ->
                val contacts = conversations.filter { !it.isSwarmGroup() }
                if (contacts.isEmpty()) Observable.just(emptyList())
                else mConversationFacade.observeConversations(account, contacts, true)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragContactPickerBinding.inflate(layoutInflater, container, false)
        adapter = ContactPickerAdapter(null, object : ContactPickerListeners {
            override fun onItemClick(item: ConversationItemViewModel) {
                mAccountId = item.accountId
                val contactVm = item.getContact() ?: return
                val cid = normalizeId(contactVm.contact.primaryNumber)
                if (cid.isEmpty()) return

                if (cid in selectedById) {
                    deselectContactById(cid)
                } else {
                    selectContactById(cid, contactVm.contact)
                }
                syncCircleChipStates()
            }

            override fun onItemLongClick(item: ConversationItemViewModel) {}
        })
        val isCreatingNewGroup = contacts.isEmpty()
        binding!!.groupNameBox.visibility = if (isCreatingNewGroup) View.VISIBLE else View.GONE

        binding!!.createGroupBtn.setOnClickListener {
            val accountId = mAccountId ?: accountService.currentAccount?.accountId ?: return@setOnClickListener
            val uniqueContacts = LinkedHashMap<String, Contact>()
            for ((cid, contact) in selectedById) {
                if (cid.isNotEmpty() && !contact.isUser && cid !in existingContactIds) {
                    uniqueContacts[cid] = contact
                }
            }
            if (uniqueContacts.isEmpty()) return@setOnClickListener

            val enteredName = binding?.groupNameTxt?.text?.toString()?.trim().orEmpty()
            if (isCreatingNewGroup && enteredName.isEmpty()) {
                val ctx = requireContext()
                val dialogBinding = DialogSwarmTitleBinding.inflate(LayoutInflater.from(ctx)).apply {
                    titleTxtBox.hint = getString(R.string.group_name_hint)
                }
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.group_name_dialog_title)
                    .setView(dialogBinding.root)
                    .setPositiveButton(R.string.picker_btn) { d, _ ->
                        val dialogName = dialogBinding.titleTxt.text?.toString()?.trim().orEmpty()
                        passData(accountId, uniqueContacts.values.toMutableSet(), dialogName)
                        d.dismiss()
                        dialog?.cancel()
                    }
                    .setNeutralButton(R.string.group_no_name) { d, _ ->
                        passData(accountId, uniqueContacts.values.toMutableSet(), null)
                        d.dismiss()
                        dialog?.cancel()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                passData(accountId, uniqueContacts.values.toMutableSet(), enteredName.ifEmpty { null })
                dialog?.cancel()
            }
        }
        binding!!.contactList.adapter = adapter
        return binding!!.root
    }

    private fun resolveCircleContacts(circle: Circle): Map<String, Contact> {
        val result = LinkedHashMap<String, Contact>()
        val account = accountService.currentAccount
        val selfId = account?.uri?.let { normalizeId(it) }.orEmpty()

        if (circle.isAllContacts) {
            for ((cid, vm) in contactVmById) {
                val c = vm.getContact()?.contact ?: continue
                if (!c.isUser && cid != selfId && cid !in existingContactIds) {
                    result[cid] = c
                }
            }
            return result
        }

        // Direct members of the circle
        for (rawMember in circle.members) {
            val cid = normalizeId(rawMember)
            if (cid.isEmpty() || cid == selfId || cid in existingContactIds) continue
            val c = contactVmById[cid]?.getContact()?.contact
                ?: account?.getContactFromCache(Uri.fromId(cid))
            if (c != null && !c.isUser) {
                result[cid] = c
            }
        }

        // If the circle also references groups, include their members without duplicates
        if (circle.groups.isNotEmpty() && account != null) {
            val normalizedGroupIds = circle.groups.map { normalizeId(it) }.toSet()
            for (conv in account.getConversations()) {
                if (normalizeId(conv.uri.rawRingId) in normalizedGroupIds) {
                    for (c in conv.contacts) {
                        val cid = normalizeId(c.primaryNumber)
                        if (!c.isUser && cid.isNotEmpty() && cid != selfId && cid !in existingContactIds) {
                            result.putIfAbsent(cid, c)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun renderCircleChips(circles: List<Circle>) {
        val b = binding ?: return
        val ctx = context ?: return
        val chipGroup = b.circlesChipGroup
        chipGroup.removeAllViews()

        val selectableCircles = circles.filter { circle ->
            !circle.isAllContacts || contactVmById.isNotEmpty()
        }
        if (selectableCircles.isEmpty()) {
            b.circlesContainer.visibility = View.GONE
            return
        }
        b.circlesContainer.visibility = View.VISIBLE

        for (circle in selectableCircles) {
            val membersMap = resolveCircleContacts(circle)
            val count = membersMap.size
            val chip = Chip(ctx, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                val circleName = circleRepository.displayName(circle)
                text = if (count > 0) "$circleName ($count)" else circleName
                isCheckable = true
                isChecked = circle.id in selectedCircleIds
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#6366F1"))
                tag = circle.id
                setOnClickListener {
                    if (isChecked) {
                        selectedCircleIds.add(circle.id)
                        val resolved = resolveCircleContacts(circle)
                        for ((cid, contact) in resolved) {
                            selectContactById(cid, contact)
                        }
                        if (binding?.groupNameTxt?.text.isNullOrBlank() && !circle.isAllContacts) {
                            binding?.groupNameTxt?.setText(circleRepository.displayName(circle))
                        }
                    } else {
                        selectedCircleIds.remove(circle.id)
                        val stillCoveredIds = selectedCircleIds
                            .mapNotNull { id -> currentCircles.firstOrNull { it.id == id } }
                            .flatMap { resolveCircleContacts(it).keys }
                            .toSet()
                        val resolved = resolveCircleContacts(circle)
                        for (cid in resolved.keys) {
                            if (cid !in stillCoveredIds) {
                                deselectContactById(cid)
                            }
                        }
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun syncCircleChipStates() {
        val chipGroup = binding?.circlesChipGroup ?: return
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            val circleId = chip.tag as? String ?: continue
            val circle = currentCircles.firstOrNull { it.id == circleId } ?: continue
            val members = resolveCircleContacts(circle).keys
            val allSelected = members.isNotEmpty() && members.all { it in selectedById }
            chip.isChecked = allSelected
            if (allSelected) selectedCircleIds.add(circleId) else selectedCircleIds.remove(circleId)
        }
    }

    private fun selectContactById(cid: String, contact: Contact) {
        val b = binding ?: return
        if (cid.isEmpty() || contact.isUser || cid in existingContactIds) return
        val vm = contactVmById[cid]
        if (vm != null) {
            mAccountId = vm.accountId
            if (!vm.isChecked) {
                vm.isChecked = true
                adapter?.update(vm)
            }
        } else if (mAccountId == null) {
            mAccountId = accountService.currentAccount?.accountId
        }

        // Prevent duplicate selection or duplicate chip
        if (cid in selectedById) return
        selectedById[cid] = contact

        val chipTag = "contact_$cid"
        if (b.selectedContacts.findViewWithTag<View>(chipTag) == null) {
            val contactVm = vm?.getContact()
            val chipTitle = vm?.title?.ifBlank { contact.primaryNumber.take(10) }
                ?: contact.primaryNumber.take(10)
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Input_Icon).apply {
                text = chipTitle
                if (contactVm != null) {
                    chipIcon = AvatarDrawable.Builder()
                        .withContact(contactVm)
                        .withCircleCrop(true)
                        .withCheck(false)
                        .withPresence(false)
                        .build(b.root.context)
                }
                isCloseIconVisible = true
                tag = chipTag
                setOnCloseIconClickListener {
                    deselectContactById(cid)
                    syncCircleChipStates()
                }
            }
            TransitionManager.beginDelayedTransition(b.selectedContactsTooolbar, AutoTransition().setDuration(100))
            b.selectedContacts.addView(chip)
        }
        b.createGroupBtn.isEnabled = selectedById.isNotEmpty()
    }

    private fun deselectContactById(cid: String) {
        val b = binding ?: return
        selectedById.remove(cid)
        val vm = contactVmById[cid]
        if (vm != null && vm.isChecked) {
            vm.isChecked = false
            adapter?.update(vm)
        }
        val chipView = b.selectedContacts.findViewWithTag<View>("contact_$cid")
        if (chipView != null) {
            TransitionManager.beginDelayedTransition(b.selectedContactsTooolbar, AutoTransition().setDuration(100))
            b.selectedContacts.removeView(chipView)
        }
        b.createGroupBtn.isEnabled = selectedById.isNotEmpty()
    }

    private fun passData(accountId: String, contacts: MutableSet<Contact>, groupName: String? = null) {
        dataPasser.onContactPicked(accountId, contacts, groupName)
    }

    override fun onDestroyView() {
        mDisposableBag.clear()
        binding = null
        adapter = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataPasser = context as OnContactedPicked
    }

    companion object {
        val TAG: String = ContactPickerFragment::class.java.simpleName
    }

    interface OnContactedPicked {
        fun onContactPicked(accountId: String, contacts: Set<Contact>)
        fun onContactPicked(accountId: String, contacts: Set<Contact>, groupName: String?) {
            onContactPicked(accountId, contacts)
        }
    }
}