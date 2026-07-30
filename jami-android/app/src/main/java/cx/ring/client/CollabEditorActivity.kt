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
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityCollabEditorBinding
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.CollaborativeVersion
import net.jami.services.AccountService
import net.jami.services.CollaborationService
import net.jami.utils.Log
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * A document of a conversation, opened for editing.
 *
 * The text itself lives in a WebView. A shared document is a CRDT that the
 * daemon moves around as opaque updates, and the replica that turns those
 * updates into text has to agree, character for character and attribute for
 * attribute, with the one the desktop client runs. Running the same library
 * the other clients run is what makes that agreement a fact rather than an
 * intention; reimplementing it in Kotlin would make it a hope.
 *
 * So this class carries updates and does not read them: bytes from the daemon
 * go to the page, bytes from the page go to the daemon, and everything about
 * what the document *says* stays on one side of that line.
 */
@AndroidEntryPoint
class CollabEditorActivity : AppCompatActivity() {

    @Inject
    lateinit var collaborationService: CollaborationService

    @Inject
    lateinit var accountService: AccountService

    private lateinit var binding: ActivityCollabEditorBinding
    private lateinit var path: ConversationPath
    private lateinit var documentId: String

    private val disposable = CompositeDisposable()
    private var documentName: String = ""
    private var editorReady = false
    private var opened = false

    /** Updates produced by the page before it was allowed to talk to the daemon. */
    private val pendingLocalUpdates = ArrayList<String>()

    /** Awareness states, throttled: a caret moves far more often than it needs to be sent. */
    private val awarenessOut = PublishSubject.create<String>()

    private val peers = HashMap<String, Peer>()

    /** The peer windows currently in the document, so that leaving is visible. */
    private val present = HashSet<String>()

    private data class Peer(val displayName: String, val color: Int)

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) attachImage(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationPath = ConversationPath.fromIntent(intent)
        val id = intent.getStringExtra(EXTRA_DOCUMENT_ID)
        if (conversationPath == null || id.isNullOrEmpty()) {
            finish()
            return
        }
        path = conversationPath
        documentId = id
        documentName = intent.getStringExtra(EXTRA_DOCUMENT_NAME).orEmpty()

        JamiApplication.instance?.startDaemon(this)

        binding = ActivityCollabEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        showTitle()

        bindFormatBar()
        binding.versionLeave.setOnClickListener { leaveVersion() }
        binding.versionRestore.setOnClickListener { restoreVersion() }
        setUpWebView()

        // A caret that has stopped moving still has to be reported, so this
        // keeps the last position of every window rather than the first.
        disposable.add(awarenessOut
            .throttleLatest(AWARENESS_INTERVAL_MS, TimeUnit.MILLISECONDS, true)
            .distinctUntilChanged()
            .flatMapCompletable { state ->
                collaborationService
                    .setAwareness(path.accountId, path.conversationUri, documentId, state)
                    .onErrorComplete()
            }
            .subscribe({}, { e -> Log.w(TAG, "awareness", e) }))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.collab_editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_collab_rename -> { promptRename(); true }
        R.id.menu_collab_history -> { showHistory(); true }
        R.id.menu_collab_export -> { exportToPdf(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        disposable.clear()
        if (!::binding.isInitialized) {
            super.onDestroy()
            return
        }
        if (opened) {
            // Fire and forget: the daemon has to know this replica is gone so the
            // others stop showing its caret, but there is nothing to wait for.
            collaborationService.closeDocument(path.accountId, path.conversationUri, documentId)
                .subscribe({}, { e -> Log.w(TAG, "close", e) })
        }
        binding.editor.destroy()
        super.onDestroy()
    }

    /* ------------------------------------------------------------- web view */

    private fun setUpWebView() {
        binding.editor.apply {
            settings.javaScriptEnabled = true
            // The page is bundled with the application and reaches nothing else.
            // What stops it going anywhere is shouldInterceptRequest below,
            // which answers every request itself; blockNetworkLoads is not used
            // for that, because it also refuses the requests this application
            // serves and would leave the editor unable to load itself.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.setSupportZoom(false)
            settings.textZoom = 100
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = true

            addJavascriptInterface(Bridge(), "JamiBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? {
                    val url = request.url
                    if (url.host != ASSET_DOMAIN) return DENIED
                    val path = url.path.orEmpty()
                    return when {
                        path.startsWith(ATTACHMENT_PATH) ->
                            serveAttachment(url.lastPathSegment) ?: DENIED
                        path.startsWith(ASSET_PATH) ->
                            serveAsset(path.removePrefix(ASSET_PATH)) ?: DENIED
                        else -> DENIED
                    }
                }

                override fun onReceivedError(
                    view: WebView, request: WebResourceRequest, error: WebResourceError
                ) {
                    Log.e(TAG, "editor ${request.url}: ${error.errorCode} ${error.description}")
                }

                // A document can hold a link to anywhere. Following one inside the
                // editor would leave the user typing into a web page; it belongs
                // to the browser, and only if it is a link and not a script.
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean {
                    val url = request.url
                    if (url.host == ASSET_DOMAIN) return false
                    if (request.isForMainFrame && url.scheme in OPENABLE_SCHEMES) {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url)) }
                    }
                    return true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d(TAG, "editor: ${message.message()} (${message.lineNumber()})")
                    return true
                }
            }

            loadUrl("https://$ASSET_DOMAIN$ASSET_PATH" + "editor.html")
        }
    }

    /**
     * Serves the editor itself.
     *
     * The three files it is made of are named explicitly rather than opened by
     * path: a document is written by someone else, and a request coming out of
     * one must not be able to name a file of this application that the editor
     * is not.
     */
    private fun serveAsset(name: String): WebResourceResponse? {
        val mimeType = EDITOR_FILES[name] ?: return null
        return try {
            WebResourceResponse(mimeType, "utf-8", assets.open(EDITOR_ASSET_DIR + name))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "asset $name", e)
            null
        }
    }

    /**
     * Serves an image of the document to the page.
     *
     * Called on a WebView thread, so the fetch is blocking on purpose: the
     * WebView is asking for the bytes of a resource it is about to lay out, and
     * has nowhere to put an answer that arrives later.
     */
    private fun serveAttachment(attachmentId: String?): WebResourceResponse? {
        if (attachmentId.isNullOrEmpty()) return null
        return try {
            val data = collaborationService
                .attachment(path.accountId, path.conversationUri, documentId, attachmentId)
                .blockingGet()
            if (data.isEmpty()) null
            else WebResourceResponse(
                guessMimeType(data), null, 200, "OK",
                mapOf("Cache-Control" to "max-age=31536000"),
                data.inputStream()
            )
        } catch (e: Exception) {
            Log.w(TAG, "attachment $attachmentId", e)
            null
        }
    }

    /* --------------------------------------------------------------- bridge */

    /**
     * What the page is allowed to ask of the application.
     *
     * Every method runs on a WebView thread, so anything touching a view is
     * posted back to the main one.
     */
    private inner class Bridge {

        @JavascriptInterface
        fun onReady() = runOnUiThread {
            editorReady = true
            openDocument()
        }

        @JavascriptInterface
        fun onUpdate(base64: String) {
            if (!opened) {
                // The page starts empty and immediately reports the state it is
                // in. Sending that before the document has been read would tell
                // the others this replica had emptied it.
                runOnUiThread { pendingLocalUpdates.add(base64) }
                return
            }
            sendUpdate(base64)
        }

        @JavascriptInterface
        fun onAwareness(state: String) {
            if (opened) awarenessOut.onNext(state)
        }

        @JavascriptInterface
        fun onSelection(json: String) = runOnUiThread { showFormats(json) }

        @JavascriptInterface
        fun onLog(message: String) {
            Log.w(TAG, message)
        }
    }

    private fun callEditor(function: String, vararg args: String) {
        val call = args.joinToString(",") { it }
        binding.editor.evaluateJavascript("window.JamiEditor.$function($call)", null)
    }

    /**
     * Call the editor, and wait for what it answers.
     *
     * The page can fail to do what it was asked, and telling the user it was
     * done when it was not leaves them believing in a document they do not
     * have. The answer is the value the function returned, as JSON.
     */
    private fun askEditor(function: String, vararg args: String, then: (String?) -> Unit) {
        val call = args.joinToString(",") { it }
        binding.editor.evaluateJavascript("window.JamiEditor.$function($call)") { then(it) }
    }

    private fun quote(value: String): String =
        org.json.JSONObject.quote(value)

    /* ------------------------------------------------------ document opening */

    private fun openDocument() {
        disposable.add(collaborationService
            .openDocument(path.accountId, path.conversationUri, documentId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ state ->
                // The daemon answers with the whole document as one update, and
                // an empty answer is how it says no: the document was never
                // announced in this conversation, or it refused to open it.
                // Showing an empty page instead would look like a document that
                // simply never syncs.
                if (state.isEmpty()) {
                    Log.e(TAG, "open $documentId: the daemon returned no state")
                    binding.loading.isVisible = false
                    binding.error.isVisible = true
                    binding.error.setText(R.string.collab_open_error)
                    return@subscribe
                }
                opened = true
                callEditor("applyUpdate", quote(encode(state)))
                binding.loading.isVisible = false
                binding.editor.isVisible = true
                // Whatever the page did while it waited now has a document to
                // apply to, and is worth sending.
                pendingLocalUpdates.forEach { sendUpdate(it) }
                pendingLocalUpdates.clear()
                listen()
                if (documentName.isEmpty()) refreshName()
            }, { e ->
                Log.e(TAG, "open $documentId", e)
                binding.loading.isVisible = false
                binding.error.isVisible = true
                binding.error.setText(R.string.collab_open_error)
            }))
    }

    private fun listen() {
        disposable.add(collaborationService
            .updatesFor(path.accountId, path.conversationUri, documentId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ update -> callEditor("applyUpdate", quote(encode(update))) },
                { e -> Log.e(TAG, "updates", e) }))

        disposable.add(collaborationService
            .awarenessFor(path.accountId, path.conversationUri, documentId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ a ->
                val peer = peerFor(a.peerId)
                present.add(a.peerId + "/" + a.clientId)
                callEditor("applyAwareness", quote(a.peerId), a.clientId.toString(),
                    quote(a.state), quote(peer.displayName), quote(colorOf(peer.color)))
                showParticipants()
            }, { e -> Log.e(TAG, "awareness", e) }))

        disposable.add(collaborationService
            .departuresFor(path.accountId, path.conversationUri, documentId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ left ->
                present.remove(left.peerId + "/" + left.clientId)
                callEditor("removeCursor", quote(left.peerId), left.clientId.toString())
                showParticipants()
            }, { e -> Log.e(TAG, "departures", e) }))

        disposable.add(collaborationService.documentsRenamed
            .filter { it.accountId == path.accountId
                    && it.conversationId == path.conversationUri.rawRingId
                    && it.documentId == documentId }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ renamed ->
                documentName = renamed.name
                showTitle()
            }, { e -> Log.e(TAG, "rename", e) }))
    }

    private fun sendUpdate(base64: String) {
        disposable.add(collaborationService
            .applyUpdate(path.accountId, path.conversationUri, documentId, decode(base64))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { e ->
                Log.e(TAG, "apply", e)
                showMessage(R.string.collab_send_error)
            }))
    }

    private fun refreshName() {
        disposable.add(collaborationService
            .name(path.accountId, path.conversationUri, documentId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ name -> documentName = name; showTitle() },
                { e -> Log.w(TAG, "name", e) }))
    }

    /* --------------------------------------------------------------- toolbar */

    private fun bindFormatBar() {
        fun on(button: ImageButton, action: () -> Unit) =
            button.setOnClickListener { action() }

        on(binding.formatBold) { callEditor("toggle", quote("bold")) }
        on(binding.formatItalic) { callEditor("toggle", quote("italic")) }
        on(binding.formatUnderline) { callEditor("toggle", quote("underline")) }
        on(binding.formatStrike) { callEditor("toggle", quote("strike")) }
        on(binding.formatH1) { callEditor("setHeader", "1") }
        on(binding.formatH2) { callEditor("setHeader", "2") }
        on(binding.formatH3) { callEditor("setHeader", "3") }
        on(binding.formatBullet) { callEditor("setList", quote("bullet")) }
        on(binding.formatOrdered) { callEditor("setList", quote("ordered")) }
        on(binding.formatAlignLeft) { callEditor("setAlign", quote("left")) }
        on(binding.formatAlignCenter) { callEditor("setAlign", quote("center")) }
        on(binding.formatAlignRight) { callEditor("setAlign", quote("right")) }
        on(binding.formatAlignJustify) { callEditor("setAlign", quote("justify")) }
        on(binding.formatLink) { promptLink() }
        on(binding.formatImage) {
            pickImage.launch(androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        on(binding.formatClear) { callEditor("clearFormat") }
        on(binding.formatUndo) { callEditor("undo") }
        on(binding.formatRedo) { callEditor("redo") }
    }

    /** Lights up the buttons that describe the text under the caret. */
    private fun showFormats(json: String) {
        val formats = runCatching {
            org.json.JSONObject(json).getJSONObject("formats")
        }.getOrNull() ?: return
        currentLink = formats.optString("link")

        fun mark(button: ImageButton, active: Boolean) {
            button.isSelected = active
            button.alpha = if (active) 1f else INACTIVE_ALPHA
        }

        mark(binding.formatBold, formats.optBoolean("bold"))
        mark(binding.formatItalic, formats.optBoolean("italic"))
        mark(binding.formatUnderline, formats.optBoolean("underline"))
        mark(binding.formatStrike, formats.optBoolean("strike"))
        val header = formats.optInt("header")
        mark(binding.formatH1, header == 1)
        mark(binding.formatH2, header == 2)
        mark(binding.formatH3, header == 3)
        val list = formats.optString("list")
        mark(binding.formatBullet, list == "bullet")
        mark(binding.formatOrdered, list == "ordered")
        val align = formats.optString("align")
        mark(binding.formatAlignLeft, align.isEmpty())
        mark(binding.formatAlignCenter, align == "center")
        mark(binding.formatAlignRight, align == "right")
        mark(binding.formatAlignJustify, align == "justify")
        mark(binding.formatLink, currentLink.isNotEmpty())
    }

    private var currentLink: String = ""

    private fun promptLink() {
        val input = EditText(this).apply {
            setText(currentLink)
            hint = getString(R.string.collab_link_hint)
            setSingleLine()
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.collab_link_title)
            .setView(wrapDialogView(input))
            .setPositiveButton(R.string.collab_link_add) { _, _ ->
                callEditor("setLink", quote(input.text.toString().trim()))
            }
            .setNegativeButton(android.R.string.cancel, null)
        if (currentLink.isNotEmpty()) {
            builder.setNeutralButton(R.string.collab_link_remove) { _, _ ->
                callEditor("setLink", quote(""))
            }
        }
        builder.show()
    }

    private fun promptRename() {
        val input = EditText(this).apply {
            setText(documentName)
            hint = getString(R.string.collab_document_name_hint)
            setSingleLine()
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.collab_rename)
            .setView(wrapDialogView(input))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                disposable.add(collaborationService
                    .setName(path.accountId, path.conversationUri, documentId, name)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ documentName = name; showTitle() },
                        { e -> Log.e(TAG, "rename", e) }))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun wrapDialogView(view: View): View =
        android.widget.FrameLayout(this).apply {
            val margin = resources.getDimensionPixelSize(R.dimen.padding_large)
            setPadding(margin, margin / 2, margin, 0)
            addView(view)
        }

    /* --------------------------------------------------------------- history */

    private fun showHistory() {
        disposable.add(collaborationService
            .history(path.accountId, path.conversationUri, documentId, HISTORY_LIMIT)
            .flatMap { versions -> namesFor(versions).map { names -> versions to names } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (versions, names) ->
                if (versions.isEmpty()) {
                    showMessage(R.string.collab_no_history)
                    return@subscribe
                }
                val labels = versions.map { label(it, names) }.toTypedArray()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.collab_history)
                    .setItems(labels) { _, which -> showVersion(versions[which]) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }, { e -> Log.e(TAG, "history", e) }))
    }

    /**
     * The name to show for every author in [versions].
     *
     * A checkpoint is written by whoever made the changes it holds, so the
     * history of a shared document is a history of several people. Without a
     * name against each entry it reads as one person's, which it is not.
     *
     * A profile that does not arrive must not hold the list back: the
     * identifier is a poor label but a timely one.
     */
    private fun namesFor(versions: List<CollaborativeVersion>): Single<Map<String, String>> {
        val account = accountService.getAccount(path.accountId)
        val authors = versions.map { it.author }.filter { it.isNotEmpty() }.distinct()
        if (account == null || authors.isEmpty()) return Single.just(emptyMap())
        val mine = account.uri
        val lookups = authors.map { author ->
            if (author == mine)
                Single.just(author to getString(R.string.conversation_info_contact_you))
            else account.getContactFromCache(author).profile
                .map { profile -> profile.displayName.orEmpty() }
                .timeout(PROFILE_WAIT, TimeUnit.SECONDS, Observable.just(""))
                .first("")
                .map { name -> author to name.ifEmpty { shortId(author) } }
        }
        return Single.zip(lookups) { pairs ->
            @Suppress("UNCHECKED_CAST")
            pairs.associate { it as Pair<String, String> }
        }
    }

    private fun label(version: CollaborativeVersion, names: Map<String, String>): String {
        val time = describe(version)
        val who = names[version.author] ?: return time
        return if (version.deltas > 0)
            getString(R.string.collab_version_deltas, who, time, version.deltas)
        else getString(R.string.collab_version_by, who, time)
    }

    private fun describe(version: CollaborativeVersion): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(version.timestamp * 1000))

    private fun showVersion(version: CollaborativeVersion) {
        disposable.add(collaborationService
            .stateAt(path.accountId, path.conversationUri, documentId, version.commitId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ state ->
                callEditor("showVersion", quote(encode(state)))
                binding.versionBar.isVisible = true
                binding.formatBar.isVisible = false
                binding.formatBarDivider.isVisible = false
                binding.versionLabel.text =
                    getString(R.string.collab_version_shown, describe(version))
            }, { e -> Log.e(TAG, "version", e) }))
    }

    private fun leaveVersion() {
        callEditor("leaveVersion")
        closeVersionBar()
    }

    /**
     * Put the document back to the version being read.
     *
     * The editor makes it an ordinary edit, so the others receive it the usual
     * way and can take it back by restoring a later version. Nothing here
     * rewinds anything: a document rewound on one device only is a document
     * two people no longer share.
     */
    private fun restoreVersion() {
        closeVersionBar()
        askEditor("restoreVersion") { restored ->
            when (restored) {
                "true" -> showMessage(R.string.collab_version_restored)
                "false" -> showMessage(R.string.collab_version_unchanged)
                else -> showMessage(R.string.collab_version_restore_error)
            }
        }
    }

    private fun closeVersionBar() {
        binding.versionBar.isVisible = false
        binding.formatBar.isVisible = true
        binding.formatBarDivider.isVisible = true
    }

    /* ----------------------------------------------------------------- images */

    private fun attachImage(uri: Uri) {
        disposable.add(Single
            .fromCallable { readImage(uri) }
            .subscribeOn(Schedulers.io())
            .flatMap { image ->
                collaborationService
                    .addAttachment(path.accountId, path.conversationUri, documentId, image.bytes)
                    .map { id -> id to image }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (id, image) ->
                callEditor("insertImage", quote(id), image.width.toString(), image.height.toString())
            }, { e ->
                Log.e(TAG, "attachment", e)
                showMessage(if (e is ImageTooLarge) R.string.collab_image_too_large
                            else R.string.collab_image_error)
            }))
    }

    private class ImageTooLarge : Exception()

    private class LoadedImage(val bytes: ByteArray, val width: Int, val height: Int)

    private fun readImage(uri: Uri): LoadedImage {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("unreadable image")
        if (bytes.size > MAX_ATTACHMENT_BYTES) throw ImageTooLarge()
        // Only the header is read: the size is needed to lay the image out, the
        // pixels are not needed at all.
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return LoadedImage(bytes, bounds.outWidth.coerceAtLeast(0), bounds.outHeight.coerceAtLeast(0))
    }

    /* ----------------------------------------------------------------- export */

    private fun exportToPdf() {
        val name = documentName.ifEmpty { getString(R.string.collab_untitled) }
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                name,
                binding.editor.createPrintDocumentAdapter(name),
                PrintAttributes.Builder().build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "export", e)
            showMessage(R.string.collab_export_error)
        }
    }

    /* ----------------------------------------------------------------- pieces */

    private fun showTitle() {
        binding.toolbar.title = documentName.ifEmpty { getString(R.string.collab_untitled) }
    }

    private fun showParticipants() {
        // One person editing from two devices is two carets but one person.
        val others = present.mapTo(HashSet()) { it.substringBefore('/') }.size
        binding.toolbar.subtitle = if (others == 0)
            getString(R.string.collab_editing_alone)
        else
            resources.getQuantityString(R.plurals.collab_editing_others, others, others)
    }

    /**
     * The name and colour to write on a peer's caret.
     *
     * A profile is loaded rather than read, so a caret starts out labelled with
     * a fragment of its owner's identifier and takes their name when it
     * arrives. Carets move constantly, so the next awareness event carries the
     * corrected label; nothing has to be redrawn on purpose.
     */
    private fun peerFor(peerId: String): Peer = peers.getOrPut(peerId) {
        val color = CURSOR_COLORS[peers.size % CURSOR_COLORS.size]
        accountService.getAccount(path.accountId)
            ?.getContactFromCache(peerId)
            ?.let { contact ->
                disposable.add(contact.profile
                    .firstElement()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ profile ->
                        val name = profile.displayName
                        if (!name.isNullOrEmpty()) peers[peerId] = Peer(name, color)
                    }, { e -> Log.w(TAG, "profile", e) }))
            }
        Peer(shortId(peerId), color)
    }

    private fun showMessage(resId: Int) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, resId, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .show()
    }

    private fun colorOf(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    private fun shortId(peerId: String) =
        if (peerId.length > 8) peerId.substring(0, 8) else peerId

    private fun guessMimeType(data: ByteArray): String = when {
        data.size > 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() -> "image/jpeg"
        data.size > 8 && data[0] == 0x89.toByte() && data[1] == 'P'.code.toByte() -> "image/png"
        data.size > 12 && data[0] == 'R'.code.toByte() && data[8] == 'W'.code.toByte() -> "image/webp"
        data.size > 6 && data[0] == 'G'.code.toByte() && data[1] == 'I'.code.toByte() -> "image/gif"
        else -> "application/octet-stream"
    }

    private fun encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    private fun decode(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)

    companion object {
        private val TAG = CollabEditorActivity::class.simpleName!!

        const val EXTRA_DOCUMENT_ID = "cx.ring.collab.DOCUMENT_ID"
        const val EXTRA_DOCUMENT_NAME = "cx.ring.collab.DOCUMENT_NAME"

        // The host WebViewAssetLoader reserves. It resolves to nothing, so a
        // request that escapes interception fails instead of leaving the device.
        /**
         * The answer to anything the page asks for that this application does
         * not serve. Returning null would let the WebView go and fetch it.
         */
        private val DENIED: WebResourceResponse
            get() = WebResourceResponse(
                "text/plain", "utf-8", 403, "Forbidden", emptyMap(), ByteArray(0).inputStream()
            )

        private const val ASSET_DOMAIN = "appassets.androidplatform.net"
        private const val ATTACHMENT_PATH = "/attachment/"
        private const val ASSET_PATH = "/assets/collab/"
        private const val EDITOR_ASSET_DIR = "collab/"

        private val EDITOR_FILES = mapOf(
            "editor.html" to "text/html",
            "editor.js" to "text/javascript",
            "editor.css" to "text/css",
        )

        private const val AWARENESS_INTERVAL_MS = 200L
        private const val HISTORY_LIMIT = 50
        /** How long a name is worth waiting for before showing an id. */
        private const val PROFILE_WAIT = 2L
        private const val INACTIVE_ALPHA = 0.55f

        // The daemon's own ceiling for one attachment.
        private const val MAX_ATTACHMENT_BYTES = 16 * 1024 * 1024

        private val OPENABLE_SCHEMES = setOf("http", "https", "mailto")

        private val CURSOR_COLORS = intArrayOf(
            0xE53935, 0x1E88E5, 0x43A047, 0xFB8C00, 0x8E24AA, 0x00ACC1, 0xF4511E
        )

        fun intent(
            context: Context, accountId: String, conversationUri: net.jami.model.Uri,
            documentId: String,
            name: String?
        ) = intent(context, ConversationPath(accountId, conversationUri), documentId, name)

        fun intent(context: Context, path: ConversationPath, documentId: String, name: String?) =
            Intent(Intent.ACTION_VIEW, path.toUri(), context, CollabEditorActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_ID, documentId)
                .putExtra(EXTRA_DOCUMENT_NAME, name)
    }
}
