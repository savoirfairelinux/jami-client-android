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
package cx.ring.client

import android.content.Context
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.services.CollaborationService
import net.jami.utils.Log

/**
 * Starting a document, from wherever the user thought of it: the conversation
 * itself or the list of documents it already holds.
 */
object CollabDocuments {
    private val TAG = CollabDocuments::class.simpleName!!

    fun promptNew(
        context: Context,
        path: ConversationPath,
        service: CollaborationService,
        disposable: CompositeDisposable,
    ) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.collab_document_name_hint)
            setSingleLine()
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.collab_new_document)
            .setView(FrameLayout(context).apply {
                val margin = resources.getDimensionPixelSize(R.dimen.padding_large)
                setPadding(margin, margin / 2, margin, 0)
                addView(input)
            })
            .setPositiveButton(R.string.collab_create) { _, _ ->
                val name = input.text.toString().trim()
                    .ifEmpty { context.getString(R.string.collab_untitled) }
                create(context, path, service, disposable, name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun create(
        context: Context,
        path: ConversationPath,
        service: CollaborationService,
        disposable: CompositeDisposable,
        name: String,
    ) {
        disposable.add(service.createDocument(path.accountId, path.conversationUri, name)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ documentId ->
                // An empty id is how the daemon refuses; opening an editor on it
                // would show a document that cannot exist.
                if (documentId.isEmpty()) {
                    Log.e(TAG, "create: the daemon returned no document id")
                    Toast.makeText(context, R.string.collab_create_error, Toast.LENGTH_SHORT).show()
                    return@subscribe
                }
                context.startActivity(CollabEditorActivity.intent(context, path, documentId, name))
            }, { e ->
                Log.e(TAG, "create", e)
                Toast.makeText(context, R.string.collab_create_error, Toast.LENGTH_SHORT).show()
            }))
    }
}
