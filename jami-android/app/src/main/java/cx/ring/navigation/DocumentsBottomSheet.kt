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
package cx.ring.navigation

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cx.ring.R
import cx.ring.client.CollabEditorActivity
import cx.ring.databinding.ItemDocumentEntryBinding
import cx.ring.databinding.SheetDocumentsBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.CollaborativeDocument
import net.jami.model.Conversation
import net.jami.services.AccountService
import net.jami.services.CollaborationService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.Log
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

/**
 * Every collaborative document of the account, whichever conversation it is
 * written in, most recent first. Each one tells where it comes from and who
 * created it, and opens in the editor.
 *
 * Starting a new one is left to the fragment that opened the sheet, through
 * [REQUEST_NEW_DOCUMENT]: the creation goes on after the sheet is dismissed.
 */
@AndroidEntryPoint
class DocumentsBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var conversationFacade: ConversationFacade

    @Inject
    lateinit var collaborationService: CollaborationService

    @Inject
    lateinit var contactService: ContactService

    private val disposables = CompositeDisposable()
    // Each value reads the documents again. A document created on another device
    // shows up as a commit in its conversation, so the list is read rather than kept.
    private val refreshes = BehaviorSubject.create<Unit>()
    private var binding: SheetDocumentsBinding? = null
    private val adapter = DocumentAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            // Shown whole, the sheet grows with the list once read, up to most of the screen
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.maxHeight = maxSheetHeight()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = SheetDocumentsBinding.inflate(inflater, container, false).apply {
        documentList.adapter = adapter
        newDocument.setOnClickListener {
            val fragmentManager = parentFragmentManager
            dismiss()
            fragmentManager.setFragmentResult(REQUEST_NEW_DOCUMENT, Bundle.EMPTY)
        }
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disposables.add(accountService.currentAccountSubject
            .switchMap { account ->
                Observable.merge(
                    refreshes,
                    // The daemon reports removals, whichever device made them
                    collaborationService.documentsRemoved
                        .filter { it.accountId == account.accountId }
                        .map { }
                )
                    .switchMapSingle { loadDocuments(account) }
                    .switchMap { found -> describe(account, found) }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ entries -> show(entries) }) { e ->
                Log.e(TAG, "Can't list the documents", e)
                show(emptyList())
            })
    }

    override fun onResume() {
        super.onResume()
        // Back from the editor, a document may have been renamed or created
        refresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The activity handles rotations itself: keep to the new screen height
        (dialog as? BottomSheetDialog)?.behavior?.maxHeight = maxSheetHeight()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        super.onDestroyView()
    }

    private fun maxSheetHeight() = (resources.displayMetrics.heightPixels * MAX_HEIGHT_RATIO).toInt()

    private fun refresh() {
        // A list read again stays in place: the progress only stands in for an empty one
        binding?.let { b ->
            if (adapter.itemCount == 0) {
                b.progress.isVisible = true
                b.emptyHint.isVisible = false
            }
        }
        refreshes.onNext(Unit)
    }

    private fun show(entries: List<DocumentEntry>) {
        val b = binding ?: return
        b.progress.isVisible = false
        b.emptyHint.isVisible = entries.isEmpty()
        adapter.submitList(entries)
    }

    /** The documents of every conversation that can hold some, most recent first. */
    private fun loadDocuments(account: Account): Single<List<Pair<Conversation, CollaborativeDocument>>> =
        account.getConversationsSubject()
            .firstOrError()
            .flatMap { conversations ->
                Observable.fromIterable(conversations.filter { holdsDocuments(it) })
                    // One conversation after the other: reading its documents walks its
                    // history on the daemon's thread, which the rest of the client shares
                    .concatMapSingle { conversation ->
                        collaborationService.documents(account.accountId, conversation.uri)
                            .doOnError { e -> Log.w(TAG, "Can't read the documents of ${conversation.uri}", e) }
                            .onErrorReturnItem(emptyList())
                            .map { documents -> documents.map { conversation to it } }
                    }
                    .toList()
                    .map { found -> found.flatten().sortedByDescending { it.second.timestamp } }
            }

    /** Where each document comes from and who created it, as the user knows them. */
    private fun describe(
        account: Account,
        found: List<Pair<Conversation, CollaborativeDocument>>
    ): Observable<List<DocumentEntry>> {
        if (found.isEmpty()) return Observable.just(emptyList())
        val conversations = found.map { it.first }.distinct()
        val origins = Observable.combineLatest(
            conversations.map { conversationFacade.observeConversation(account, it, false) }
        ) { viewModels -> conversations.zip(viewModels.map { it as ConversationItemViewModel }).toMap() }

        val self = account.uri
        val authors = found.mapNotNull { it.second.author }.filter { it != self }.distinct()
        val names: Observable<Map<String, String>> = if (authors.isEmpty()) Observable.just(emptyMap())
        else Observable.combineLatest(authors.map { authorName(account, it) }) { values ->
            authors.zip(values.map { it as String }).toMap()
        }

        return Observable.combineLatest(origins, names) { origin, name ->
            found.map { (conversation, document) ->
                DocumentEntry(
                    document = document,
                    conversation = conversation,
                    origin = origin.getValue(conversation),
                    isGroup = conversation.isSwarmGroup(),
                    byUser = document.author != null && document.author == self,
                    authorName = document.author?.let { name[it] },
                )
            }
        }
    }

    /**
     * The name the user knows an author by: the one in its profile, else its
     * registered name. The short form of its id stands in until either is known.
     */
    private fun authorName(account: Account, author: String): Observable<String> {
        val fallback = shortId(author)
        val contact = try {
            account.getContactFromCache(author)
        } catch (e: IllegalArgumentException) {
            return Observable.just(fallback)
        }
        return contactService.observeContact(account.accountId, contact, false)
            .map { vm ->
                vm.profile.displayName?.takeIf { it.isNotBlank() }
                    ?: vm.registeredName?.takeIf { it.isNotBlank() }
                    ?: fallback
            }
            .onErrorReturnItem(fallback)
            .startWithItem(fallback)
            .distinctUntilChanged()
    }

    private fun open(entry: DocumentEntry) {
        val context = context ?: return
        val conversation = entry.conversation
        startActivity(CollabEditorActivity.intent(
            context,
            ConversationPath(conversation.accountId, conversation.uri),
            entry.document.id,
            entry.document.name
        ))
    }

    /** Who created it and when, as a conversation's own list of documents says it. */
    private fun detailsOf(context: Context, entry: DocumentEntry): String {
        val document = entry.document
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(document.timestamp * 1000))
        val author = if (entry.byUser) context.getString(R.string.documents_author_you) else entry.authorName
        val line = if (author == null) date
        else context.getString(R.string.collab_created_by, author) + " · " + date
        // Still listed when this device dropped it: opening it fetches it back
        if (document.storedLocally) return line
        return line + " · " + context.getString(R.string.collab_not_on_this_device)
    }

    private data class DocumentEntry(
        val document: CollaborativeDocument,
        val conversation: Conversation,
        /** The contact or the group the document is written with. */
        val origin: ConversationItemViewModel,
        val isGroup: Boolean,
        /** The user created it, which is said rather than named. */
        val byUser: Boolean,
        /** Who else created it, as the user knows them; null when the daemon did not say. */
        val authorName: String?,
    )

    private inner class DocumentAdapter : ListAdapter<DocumentEntry, DocumentHolder>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DocumentHolder(
            ItemDocumentEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: DocumentHolder, position: Int) {
            val entry = getItem(position)
            val b = holder.binding
            val context = b.root.context
            b.documentName.text = entry.document.name.ifEmpty { context.getString(R.string.collab_untitled) }
            b.originAvatar.setImageDrawable(
                AvatarDrawable.Builder()
                    .withViewModel(entry.origin)
                    .withCircleCrop(true)
                    .build(context)
            )
            b.originType.setImageResource(
                if (entry.isGroup) R.drawable.baseline_group_24 else R.drawable.outline_person_24
            )
            b.originTitle.text = entry.origin.title.ifBlank { entry.origin.uriTitle }
            b.documentDetails.text = detailsOf(context, entry)
            b.root.setOnClickListener { open(entry) }
            b.editDocument.setOnClickListener { open(entry) }
        }
    }

    private class DocumentHolder(val binding: ItemDocumentEntryBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val TAG = "DocumentsBottomSheet"

        /** Asks the fragment that opened the sheet to start a new document. */
        const val REQUEST_NEW_DOCUMENT = "DocumentsBottomSheet.newDocument"

        private const val MAX_HEIGHT_RATIO = 0.85f

        private val DIFF = object : DiffUtil.ItemCallback<DocumentEntry>() {
            override fun areItemsTheSame(oldItem: DocumentEntry, newItem: DocumentEntry) =
                oldItem.conversation === newItem.conversation && oldItem.document.id == newItem.document.id

            override fun areContentsTheSame(oldItem: DocumentEntry, newItem: DocumentEntry) =
                oldItem == newItem
        }

        /**
         * Documents are written in the swarm of a contact or of a group. A request
         * has no history here yet, nor has a conversation still being cloned, and
         * the Boards are left to their own function.
         */
        private fun holdsDocuments(conversation: Conversation): Boolean {
            if (!conversation.isSwarm || conversation.isFeed()) return false
            val mode = conversation.mode.blockingFirst()
            return mode != Conversation.Mode.Request && mode != Conversation.Mode.Syncing
        }

        private fun shortId(peerId: String) = peerId.take(8)

        fun newInstance() = DocumentsBottomSheet()
    }
}
