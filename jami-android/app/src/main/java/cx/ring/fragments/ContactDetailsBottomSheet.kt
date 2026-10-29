package cx.ring.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.databinding.FragContactDetailsBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.utils.TextUtils.copyAndShow
import cx.ring.views.AvatarDrawable
import cx.ring.views.setDate
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.utils.Log
import javax.inject.Inject

@AndroidEntryPoint
class ContactDetailsBottomSheet : BottomSheetDialogFragment() {
    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var contactService: ContactService

    private val disposables = CompositeDisposable()
    private var binding: FragContactDetailsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragContactDetailsBinding.inflate(inflater, container, false)
        .also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = requireNotNull(ConversationPath.fromBundle(arguments))
        val account = accountService.getAccount(path.accountId)
        if (account == null) {
            showError(IllegalStateException("Contact account is no longer available"))
            dismiss()
            return
        }
        val contact = account.getContactFromCache(path.conversationUri)
        val b = requireNotNull(binding)
        val identifier = contact.uri.uri
        b.contactName.text = identifier
        b.identifier.text = identifier
        b.qrCode.text = identifier
        b.contactAvatar.setImageDrawable(AvatarDrawable.Builder()
            .withUri(contact.uri).withCircleCrop(true).withPresence(false).build(requireContext()))
        b.copyIdentifier.setOnClickListener {
            copyAndShow(requireContext(), getString(R.string.clip_contact_uri), identifier)
        }
        b.shareButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_contact_intent_title))
                putExtra(Intent.EXTRA_TEXT, getString(
                    R.string.share_contact_intent_body, identifier, getString(R.string.app_website)))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
        }

        disposables.add(contact.updatesSubject.startWithItem(contact)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { updated ->
                b.contactAddedDate.setDate(R.string.contact_added_date, updated.addedDate?.time ?: 0L)
                b.contactRemovedDate.setDate(R.string.contact_removed_date, updated.removedDate?.time ?: 0L)
                b.contactDatesPanel.isVisible = b.contactAddedDate.root.isVisible || b.contactRemovedDate.root.isVisible
            })
        disposables.add(contactService.observeContact(path.accountId, contact, false)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ model ->
                b.contactName.text = model.displayName
                b.registeredName.text = model.registeredName
                b.registeredName.isVisible = !model.registeredName.isNullOrEmpty() &&
                        model.registeredName != model.displayName
                b.contactAvatar.setImageDrawable(AvatarDrawable.Builder()
                    .withContact(model).withCircleCrop(true).withPresence(false).build(requireContext()))
            }, ::showError))
    }

    private fun showError(error: Throwable) {
        Log.e(TAG, "Unable to load contact details", error)
        Toast.makeText(requireContext(), R.string.generic_error, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ContactDetailsBottomSheet"

        fun newInstance(accountId: String, contactUri: Uri) = ContactDetailsBottomSheet().apply {
            arguments = ConversationPath.toBundle(accountId, contactUri)
        }
    }
}
