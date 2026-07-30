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
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.client.CollabEditorActivity
import cx.ring.databinding.FragCollabDocumentsBinding
import cx.ring.databinding.ItemCollabDocumentBinding
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.CollaborativeDocument
import net.jami.model.Uri
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

    private val disposable = CompositeDisposable()
    private var binding: FragCollabDocumentsBinding? = null
    private lateinit var path: ConversationPath
    private val adapter = DocumentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ConversationPath.fromBundle(arguments)!!
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
            .documents(path.accountId, path.conversationId)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ documents ->
                adapter.replace(documents)
                binding?.placeholder?.isVisible = documents.isEmpty()
            }, { e -> Log.e(TAG, "documents", e) }))
    }

    private fun promptNewDocument() {
        val context = context ?: return
        val input = EditText(context).apply {
            hint = getString(R.string.collab_document_name_hint)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.collab_new_document)
            .setView(android.widget.FrameLayout(context).apply {
                val margin = resources.getDimensionPixelSize(R.dimen.padding_large)
                setPadding(margin, margin / 2, margin, 0)
                addView(input)
            })
            .setPositiveButton(R.string.collab_create) { _, _ ->
                create(input.text.toString().trim().ifEmpty { getString(R.string.collab_untitled) })
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun create(name: String) {
        disposable.add(collaborationService
            .createDocument(path.accountId, path.conversationId, name)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ documentId -> open(documentId, name) },
                { e -> Log.e(TAG, "create", e) }))
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
        }

        override fun getItemCount() = documents.size
    }

    private class DocumentHolder(val binding: ItemCollabDocumentBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun subtitleOf(document: CollaborativeDocument): String {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(document.timestamp * 1000))
        val author = document.author ?: return date
        return getString(R.string.collab_created_by, shortId(author)) + " · " + date
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
