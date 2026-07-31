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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.CollabDocuments
import cx.ring.client.CollabEditorActivity
import cx.ring.databinding.FragCollabDocumentsBinding
import cx.ring.databinding.ItemCollabDocumentBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.CollaborativeDocument
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CollaborationService
import net.jami.utils.Log
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

/** The documents written together in a conversation. */
@AndroidEntryPoint
class CollabDocumentsFragment : Fragment() {

    @Inject
    lateinit var collaborationService: CollaborationService

    @Inject
    lateinit var accountService: AccountService

    private val disposable = CompositeDisposable()
    private var binding: FragCollabDocumentsBinding? = null
    private lateinit var path: ConversationPath
    private val adapter = DocumentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ConversationPath.fromBundle(arguments)!!
        // Both removals change the list: one takes a document out of it, the
        // other only marks it as no longer held here. Subscribed once for the
        // fragment's life, not on every return to the screen.
        disposable.add(collaborationService.documentsRemoved
            .filter {
                it.accountId == path.accountId &&
                    it.conversationId == path.conversationUri.rawRingId
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ refresh() }, { e -> Log.e(TAG, "removals", e) }))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragCollabDocumentsBinding.inflate(inflater, container, false).apply {
        documentList.adapter = adapter
        newDocument.setOnClickListener { promptNewDocument() }
        binding = this
    }.root

    override fun onResume() {
        super.onResume()
        // A document created on another device shows up as a commit in the
        // conversation, so the list is read again rather than kept.
        refresh()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun refresh() {
        disposable.add(collaborationService
            .documents(path.accountId, path.conversationUri)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ documents ->
                adapter.replace(documents)
                binding?.placeholder?.isVisible = documents.isEmpty()
            }, { e -> Log.e(TAG, "documents", e) }))
    }

    private fun promptNewDocument() {
        CollabDocuments.promptNew(context ?: return, path, collaborationService, disposable)
    }

    private fun open(documentId: String, name: String?) {
        val context = context ?: return
        startActivity(CollabEditorActivity.intent(context, path, documentId, name))
    }

    private inner class DocumentAdapter : RecyclerView.Adapter<DocumentHolder>() {
        private val documents = ArrayList<CollaborativeDocument>()

        fun replace(items: List<CollaborativeDocument>) {
            documents.clear()
            documents.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DocumentHolder(
            ItemCollabDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: DocumentHolder, position: Int) {
            val document = documents[position]
            holder.binding.documentName.text =
                document.name.ifEmpty { getString(R.string.collab_untitled) }
            holder.binding.documentSubtitle.text = subtitleOf(document)
            holder.binding.root.setOnClickListener {
                open(document.id, document.name)
            }
            holder.binding.removeLocally.isVisible = document.storedLocally
            holder.binding.removeLocally.setOnClickListener { confirmRemoval(document, everywhere = false) }
            holder.binding.removeEverywhere.isVisible = canRemoveEverywhere(document)
            holder.binding.removeEverywhere.setOnClickListener { confirmRemoval(document, everywhere = true) }
        }

        override fun getItemCount() = documents.size
    }

    private class DocumentHolder(val binding: ItemCollabDocumentBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Who wrote it and when, and whether this device still holds it.
     *
     * An entry that is no longer held stays open-able: opening it is what brings
     * it back, so it is told apart rather than dimmed.
     */
    private fun subtitleOf(document: CollaborativeDocument): String {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(document.timestamp * 1000))
        val author = document.author
        val line = if (author == null) date
        else getString(R.string.collab_created_by, shortId(author)) + " · " + date
        if (document.storedLocally) return line
        return line + " · " + getString(R.string.collab_not_on_this_device)
    }

    /**
     * Whether this device may retire a document for every member.
     *
     * Only its author may, and the daemon refuses anyone else: offering it to
     * the others would promise what cannot happen.
     */
    private fun canRemoveEverywhere(document: CollaborativeDocument): Boolean {
        val author = document.author ?: return false
        val self = accountService.getAccount(path.accountId)?.uri ?: return false
        return self.isNotEmpty() && author == self
    }

    /**
     * The two are asked apart because they are not the same question: one takes
     * a document away from everybody for good, the other only reclaims what this
     * device chose to keep.
     */
    private fun confirmRemoval(document: CollaborativeDocument, everywhere: Boolean) {
        val context = context ?: return
        val named = document.name.ifEmpty { getString(R.string.collab_untitled) }
        MaterialAlertDialogBuilder(context)
            .setTitle(if (everywhere) R.string.collab_remove_title else R.string.collab_remove_locally_title)
            .setMessage(getString(
                if (everywhere) R.string.collab_remove_message else R.string.collab_remove_locally_message,
                named))
            .setPositiveButton(R.string.collab_remove) { _, _ -> remove(document, everywhere) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Nothing is dropped from the list here.
     *
     * The daemon reports every removal through `documentsRemoved`, this device's
     * own included, and that one signal is what the list is rebuilt from: what
     * the author sees is what the peers see.
     */
    private fun remove(document: CollaborativeDocument, everywhere: Boolean) {
        val call = if (everywhere)
            collaborationService.removeDocument(path.accountId, path.conversationUri, document.id)
        else
            collaborationService.removeDocumentLocally(path.accountId, path.conversationUri, document.id)
        val failure = if (everywhere) R.string.collab_remove_error
        else R.string.collab_remove_locally_error
        disposable.add(call
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ removed -> if (!removed) toast(failure) },
                { e ->
                    Log.e(TAG, "remove", e)
                    toast(failure)
                }))
    }

    private fun toast(message: Int) {
        Toast.makeText(context ?: return, message, Toast.LENGTH_LONG).show()
    }

    private fun shortId(peerId: String) =
        if (peerId.length > 8) peerId.substring(0, 8) else peerId

    companion object {
        private val TAG = CollabDocumentsFragment::class.simpleName!!

        fun newInstance(accountId: String, conversationId: Uri) = CollabDocumentsFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
        }
    }
}
