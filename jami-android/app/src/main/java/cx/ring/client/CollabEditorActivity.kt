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
package cx.ring.client

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.interaction.CollaborativeDocument
import net.jami.model.interaction.CollaborativeEvent
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time collaborative text editor. The CRDT document lives in the daemon; this screen mirrors
 * its content. Two modes share the same screen:
 *  - plain ("text"): edits are sent as (index, deleteLen, insert) and remote changes arrive via
 *    [CollaborativeEvent.TextChange]; the format bar is hidden.
 *  - rich ("rich"): edits and formatting are exchanged as Quill deltas via applyCollaborativeDelta /
 *    [CollaborativeEvent.Delta]; a B/I/U/S + H1-H3 format bar is shown.
 * Android char indices are UTF-16 code units, matching the daemon's Y_OFFSET_UTF16 offsets, so no
 * index conversion is needed.
 */
@AndroidEntryPoint
class CollabEditorActivity : AppCompatActivity() {
    @Inject
    @Singleton
    lateinit var accountService: AccountService

    private lateinit var accountId: String
    private lateinit var conversationId: String
    private lateinit var documentId: String
    private var rich = false

    private lateinit var editor: EditText
    private lateinit var toolbar: MaterialToolbar
    private val disposable = CompositeDisposable()

    /** Mirror of the daemon document plain text; the reference for diffing local edits. */
    private var shadow = ""
    /** True while applying a remote/own programmatic change, so the watcher does not echo it. */
    private var applyingRemote = false
    /** True once the initial content has loaded; gates local sends and remote applies. */
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID) ?: return finish()
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return finish()
        documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID) ?: return finish()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        rich = intent.getStringExtra(EXTRA_KIND) == CollaborativeDocument.KIND_RICH

        JamiApplication.instance?.startDaemon(this)
        setContentView(R.layout.activity_collab_editor)
        // Keep the screen awake while a document is open for editing.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = name.ifEmpty { getString(R.string.collab_editable_document) }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        editor = findViewById(R.id.collab_editor)
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (applyingRemote || !ready) return
                sendLocalTextChange()
            }
        })

        if (rich) setupFormatBar()

        // Subscribe before opening so live edits are not missed once the snapshot is applied.
        disposable.add(accountService.collaborativeEvents
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { event -> onCollaborativeEvent(event) })

        if (rich) openRich() else openPlain()
    }

    // --- Opening ------------------------------------------------------------------------------

    private fun openPlain() {
        disposable.add(accountService.openCollaborativeDocument(accountId, conversationId, documentId)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ text ->
                applyingRemote = true
                editor.setText(text)
                editor.setSelection(text.length)
                applyingRemote = false
                shadow = text
                ready = true
                editor.isEnabled = true
            }) { e -> Log.e(TAG, "Error opening collaborative document", e) })
    }

    private fun openRich() {
        // Ensure the daemon session exists, then read the current content as a Quill delta.
        disposable.add(accountService.openCollaborativeDocument(accountId, conversationId, documentId)
            .flatMap { accountService.collaborativeDocumentContentDelta(accountId, conversationId, documentId) }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ deltaJson ->
                android.util.Log.e("JAMICOLLAB", "openRich deltaJson(len=${deltaJson.length})=${deltaJson.take(300)}")
                applyingRemote = true
                editor.setText(CollabRichText.deltaToSpannable(deltaJson))
                editor.setSelection(editor.text.length)
                applyingRemote = false
                shadow = editor.text.toString()
                android.util.Log.e("JAMICOLLAB", "openRich after setText editorLen=${editor.text.length}")
                ready = true
                editor.isEnabled = true
            }) { e -> Log.e(TAG, "Error opening rich collaborative document", e) })
    }

    // --- Local edits --------------------------------------------------------------------------

    private fun sendLocalTextChange() {
        val current = editor.text.toString()
        if (current == shadow) return
        if (rich) {
            val delta = CollabRichText.localTextDiffDelta(shadow, current, editor.text)
            shadow = current
            if (delta != null)
                accountService.applyCollaborativeDelta(accountId, conversationId, documentId, delta)
        } else {
            val (index, deleteLen, insert) = diffPlain(shadow, current)
            shadow = current
            accountService.editCollaborativeDocument(accountId, conversationId, documentId, index, deleteLen, insert)
        }
    }

    /** Common prefix/suffix diff producing a single (index, deleteLen, insert) edit. */
    private fun diffPlain(old: String, now: String): Triple<Int, Int, String> {
        val oldLen = old.length
        val newLen = now.length
        val maxScan = minOf(oldLen, newLen)
        var prefix = 0
        while (prefix < maxScan && old[prefix] == now[prefix]) prefix++
        var suffix = 0
        while (suffix < maxScan - prefix && old[oldLen - 1 - suffix] == now[newLen - 1 - suffix]) suffix++
        val deleteLen = oldLen - prefix - suffix
        val insert = now.substring(prefix, newLen - suffix)
        return Triple(prefix, deleteLen, insert)
    }

    // --- Format bar (rich only) ---------------------------------------------------------------

    private fun setupFormatBar() {
        findViewById<View>(R.id.format_bar).visibility = View.VISIBLE
        findViewById<View>(R.id.fmt_bold).setOnClickListener { toggleInline("b") }
        findViewById<View>(R.id.fmt_italic).setOnClickListener { toggleInline("i") }
        findViewById<View>(R.id.fmt_underline).setOnClickListener { toggleInline("u") }
        findViewById<View>(R.id.fmt_strike).setOnClickListener { toggleInline("s") }
        findViewById<View>(R.id.fmt_h1).setOnClickListener { toggleHeading(1) }
        findViewById<View>(R.id.fmt_h2).setOnClickListener { toggleHeading(2) }
        findViewById<View>(R.id.fmt_h3).setOnClickListener { toggleHeading(3) }
    }

    private fun toggleInline(attr: String) {
        if (!ready) return
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start < 0 || end <= start) return // inline formatting needs a selection
        applyingRemote = true
        val delta = CollabRichText.toggleInline(editor.text, attr, start, end)
        applyingRemote = false
        if (delta != null)
            accountService.applyCollaborativeDelta(accountId, conversationId, documentId, delta)
    }

    private fun toggleHeading(level: Int) {
        if (!ready) return
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(start)
        // Toggle: if the paragraph is already this heading level, clear it.
        val currentLevel = CollabRichText.attrsAt(editor.text, start).header
        val target = if (currentLevel == level) 0 else level
        applyingRemote = true
        val delta = CollabRichText.setHeading(editor.text, target, start, end)
        applyingRemote = false
        if (delta != null)
            accountService.applyCollaborativeDelta(accountId, conversationId, documentId, delta)
    }

    // --- Remote events ------------------------------------------------------------------------

    private fun onCollaborativeEvent(event: CollaborativeEvent) {
        if (event.accountId != accountId || event.conversationId != conversationId ||
            event.documentId != documentId) return
        when (event) {
            is CollaborativeEvent.Renamed -> toolbar.title = event.name
            is CollaborativeEvent.Delta -> if (rich && ready) applyRemoteDelta(event.deltaJson)
            is CollaborativeEvent.TextChange -> if (!rich && ready)
                applyRemoteTextChange(event.index, event.deleteLen, event.insert)
            // Cursor / ParticipantLeft are not surfaced in this version.
            else -> {}
        }
    }

    private fun applyRemoteDelta(deltaJson: String) {
        val selStart = editor.selectionStart
        val selEnd = editor.selectionEnd
        applyingRemote = true
        CollabRichText.applyRemoteDelta(editor.text, deltaJson)
        applyingRemote = false
        shadow = editor.text.toString()
        val len = editor.text.length
        editor.setSelection(selStart.coerceIn(0, len), selEnd.coerceIn(0, len))
    }

    private fun applyRemoteTextChange(index: Int, deleteLen: Int, insert: String) {
        val editable = editor.text
        val len = editable.length
        val start = index.coerceIn(0, len)
        val end = (index + deleteLen).coerceIn(start, len)
        val selStart = editor.selectionStart
        val selEnd = editor.selectionEnd
        applyingRemote = true
        editable.replace(start, end, insert)
        applyingRemote = false
        shadow = editor.text.toString()
        val delta = insert.length - (end - start)
        val newLen = editor.text.length
        val newStart = adjustCaret(selStart, start, end, delta).coerceIn(0, newLen)
        val newEnd = adjustCaret(selEnd, start, end, delta).coerceIn(0, newLen)
        editor.setSelection(minOf(newStart, newEnd), maxOf(newStart, newEnd))
    }

    /** Shift a caret position to account for a replacement of [start, end) by [delta] chars. */
    private fun adjustCaret(caret: Int, start: Int, end: Int, delta: Int): Int = when {
        caret <= start -> caret
        caret >= end -> caret + delta
        else -> start
    }

    override fun onDestroy() {
        if (ready) accountService.closeCollaborativeDocument(accountId, conversationId, documentId)
        disposable.dispose()
        super.onDestroy()
    }

    companion object {
        private val TAG = CollabEditorActivity::class.simpleName!!
        const val EXTRA_ACCOUNT_ID = "account_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_DOCUMENT_ID = "document_id"
        const val EXTRA_NAME = "name"
        const val EXTRA_KIND = "kind"
    }
}
