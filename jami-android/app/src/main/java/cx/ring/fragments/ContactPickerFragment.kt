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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import cx.ring.adapters.ContactPickerAdapter
import cx.ring.databinding.FragContactPickerBinding
import cx.ring.viewholders.ContactPickerViewHolder.ContactPickerListeners
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ContactPickerFragment(val contacts: List<Contact> = emptyList()) : BottomSheetDialogFragment() {
    private var binding: FragContactPickerBinding? = null
    private var adapter: ContactPickerAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private var mAccountId: String? = null
    private val mCurrentSelection: MutableSet<Contact> = HashSet()
    lateinit var dataPasser: OnContactedPicked

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDisposableBag.add(mConversationFacade.getConversationViewModelList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ conversations ->
                // Returns a list of ConversationItemViewModels that actually corresponds to
                // user contacts that are not in the group.
                val contactsNotInGroup = conversations.filter {
                    (it.mode == Conversation.Mode.Legacy
                            || it.mode == Conversation.Mode.OneToOne)
                            // Filter out contacts that are already in the group
                            && it.getContact()?.contact !in this.contacts
                }
                if(contactsNotInGroup.isEmpty()) {
                    binding?.noContactTitle?.visibility = View.VISIBLE
                    binding?.noContactLogo?.visibility = View.VISIBLE
                } else{
                    binding?.noContactTitle?.visibility = View.GONE
                    binding?.noContactLogo?.visibility = View.GONE
                }
                adapter?.update(contactsNotInGroup)
            }){ e -> Log.e(TAG, "No contact to create a group!", e) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragContactPickerBinding.inflate(layoutInflater, container, false)
        adapter = ContactPickerAdapter(null, object : ContactPickerListeners {
            override fun onItemClick(item: ConversationItemViewModel) {
                mAccountId = item.accountId
                val checked = !item.isChecked
                item.isChecked = checked
                adapter!!.update(item)
                val contact = item.getContact() ?: return
                val remover = Runnable {
                    mCurrentSelection.remove(contact.contact)
                    if (mCurrentSelection.isEmpty()) binding!!.createGroupBtn.isEnabled = false
                    item.isChecked = false
                    adapter!!.update(item)
                    val v = binding!!.selectedContacts.findViewWithTag<View>(item)
                    TransitionManager.beginDelayedTransition(binding!!.selectedContactsTooolbar, AutoTransition().setDuration(100))
                    if (v != null) binding!!.selectedContacts.removeView(v)
                }
                if (checked) {
                    if (mCurrentSelection.add(contact.contact)) {
                        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Input_Icon).apply {
                            text = item.title
                            chipIcon = AvatarDrawable.Builder()
                                .withContact(contact)
                                .withCircleCrop(true)
                                .withCheck(false)
                                .withPresence(false)
                                .build(binding!!.root.context)
                            isCloseIconVisible = true
                            tag = item
                            setOnCloseIconClickListener { remover.run() }
                        }
                        TransitionManager.beginDelayedTransition(binding!!.selectedContactsTooolbar, AutoTransition().setDuration(100))
                        binding!!.selectedContacts.addView(chip)
                    }
                    binding!!.createGroupBtn.isEnabled = true
                } else {
                    remover.run()
                }
            }

            override fun onItemLongClick(item: ConversationItemViewModel) {}
        })
        binding!!.createGroupBtn.setOnClickListener { v: View? ->
            passData(mAccountId!!, mCurrentSelection)
            val dialog = dialog
            dialog?.cancel()
        }
        binding!!.contactList.adapter = adapter
        return binding!!.root
    }

    private fun passData(accountId: String, contacts: MutableSet<Contact>){
        dataPasser.onContactPicked(accountId, contacts)
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
    }
}