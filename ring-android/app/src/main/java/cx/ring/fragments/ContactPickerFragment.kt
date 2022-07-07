package cx.ring.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import cx.ring.R
import cx.ring.adapters.ContactPickerAdapter
import cx.ring.client.HomeActivity
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
class ContactPickerFragment : BottomSheetDialogFragment() {
    private var binding: FragContactPickerBinding? = null
    private var adapter: ContactPickerAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private var mAccountId: String? = null
    private val mCurrentSelection: MutableSet<Contact> = HashSet()

    @Inject
    lateinit var mConversationFacade: ConversationFacade

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bdialog = super.onCreateDialog(savedInstanceState)
        if (bdialog is BottomSheetDialog) {
            val behavior = bdialog.behavior
            behavior.isFitToContents = false
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
        return bdialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDisposableBag.add(mConversationFacade.getConversationList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ conversations ->
                val contacts = conversations.filter { it.mode == Conversation.Mode.Legacy || it.mode == Conversation.Mode.OneToOne }
                adapter?.update(contacts)
            }){ e -> Log.e(TAG, "No contact to create a group!", e) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
                    if (v != null) binding!!.selectedContacts.removeView(v)
                }
                if (checked) {
                    if (mCurrentSelection.add(contact.contact)) {
                        val chip = Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Entry).apply {
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
            mDisposableBag.add(mConversationFacade.createConversation(mAccountId!!, mCurrentSelection)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { conversation: Conversation ->
                    (requireActivity() as HomeActivity).startConversation(conversation.accountId, conversation.uri)
                    val dialog = dialog
                    dialog?.cancel()
                })
        }
        binding!!.contactList.adapter = adapter
        return binding!!.root
    }

    override fun onDestroyView() {
        mDisposableBag.clear()
        binding = null
        adapter = null
        super.onDestroyView()
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    companion object {
        val TAG = ContactPickerFragment::class.java.simpleName
    }
}