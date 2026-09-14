/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.channels.Channel
import cx.ring.channels.ChannelRepository
import cx.ring.databinding.FragConversationBinding
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUri
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.Conversation
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Writing to a channel is writing to each of its members. The screen is the one used to write
 * to a single contact, so that what is written is written the same way; what it sends is one
 * message per member, each in its own conversation, indistinguishable from a message typed there.
 */
@AndroidEntryPoint
class BroadcastFragment : Fragment() {
    @Inject
    lateinit var conversationFacade: ConversationFacade
    @Inject
    lateinit var accountService: AccountService

    private val disposables = CompositeDisposable()
    private val broadcastDisposables = CompositeDisposable()
    private val broadcastQueue = PublishSubject.create<BroadcastJob>()
    private var binding: FragConversationBinding? = null
    private var pendingPhoto: File? = null
    private var channel: Channel? = null
    private var channelId: String? = null
    private lateinit var repository: ChannelRepository
    private lateinit var channelAccountId: String
    private var recipientsLoaded = false
    private var recipientCount = 0
    private var sendingText = false

    private data class BroadcastJob(
        val operation: () -> Completable,
        val onSuccess: () -> Unit,
        val onError: (Throwable) -> Unit,
    )

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        channelAccountId = arguments?.getString(KEY_ACCOUNT).orEmpty()
        channelId = state?.getString(KEY_CHANNEL_ID) ?: arguments?.getString(KEY_CHANNEL_ID)
        repository = ChannelRepository(requireContext(), accountService, channelAccountId)
        broadcastDisposables.add(
            broadcastQueue
                .concatMapCompletable { job ->
                    Completable.defer(job.operation)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(job.onSuccess)
                        .doOnError(job.onError)
                        .onErrorComplete()
                }
                .subscribe({}, { error -> Log.e(TAG, "Broadcast queue stopped", error) })
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        channelId?.let { outState.putString(KEY_CHANNEL_ID, it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?,
    ): View = FragConversationBinding.inflate(inflater, container, false).apply {
        binding = this
        toolbar.title = null
        toolbar.menu.clear()
        contactTitle.text = channel?.name ?: arguments?.getString(KEY_CHANNEL).orEmpty()
        contactSubtitle.setText(R.string.channels_loading)
        conversationAvatar.setImageResource(R.drawable.baseline_public_24)
        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener { requireActivity().finish() }

        // The screen is drawn edge to edge: keep the bar under the status bar and the input
        // above the navigation bar and the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            appbar.updatePadding(top = bars.top)
            mainContainer.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // A broadcast has no history of its own: what it sends lives in each conversation.
        histList.isVisible = false
        searchList.isVisible = false
        pbLoading.isVisible = false
        mapCard.isVisible = false
        fabLatest.isVisible = false
        unknownContactPrompt.isVisible = false
        trustRequestPrompt.isVisible = false
        ongoingCallPane.isVisible = false
        endedConversationPrompt.isVisible = false

        cvMessageInput.isVisible = true
        msgInputTxt.setHint(R.string.channels_message_hint)
        msgSend.isVisible = false
        emojiSend.isVisible = false
        btnAudioRecord.isVisible = false
        btnMenu.setOnClickListener { showAttachMenu(it) }
        btnTakePicture.setOnClickListener { takePicture() }
        msgInputTxt.addTextChangedListener { text ->
            val empty = text.isNullOrBlank()
            msgSend.isVisible = !empty
            btnTakePicture.isVisible = empty
        }
        msgSend.setOnClickListener { broadcast() }
        updateSendActions()
    }.root

    private fun showAttachMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, MENU_MEDIA, 0, R.string.select_media)
            menu.add(0, MENU_FILE, 1, R.string.send_file)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_MEDIA -> pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    MENU_FILE -> pickFile.launch("*/*")
                }
                true
            }
            show()
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestCamera.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val photo = AndroidFileUtils.createImageFile(requireContext())
            pendingPhoto = photo
            takePicture.launch(ContentUri.getUriForFile(requireContext(), photo))
        } catch (error: IOException) {
            Log.e(TAG, "Unable to prepare the camera file", error)
            binding?.let {
                Snackbar.make(it.root, R.string.taking_picture_error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePicture()
            else showCameraError(R.string.camera_permission_denied)
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            pendingPhoto?.let { photo ->
                pendingPhoto = null
                if (saved) broadcastFile(photo)
                else {
                    photo.delete()
                    showCameraError(R.string.taking_picture_error)
                }
            }
        }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
            uris.forEach { attach(it) }
        }

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris.forEach { attach(it) }
        }

    private fun attach(uri: android.net.Uri) {
        disposables.add(cacheAttachment(uri)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file -> broadcastFile(file) }, { error ->
                Log.e(TAG, "Unable to read the attachment", error)
                binding?.let {
                    Snackbar.make(it.root, R.string.invalid_file, Snackbar.LENGTH_LONG).show()
                }
            }))
    }

    private fun cacheAttachment(uri: android.net.Uri): Single<File> =
        Single.fromCallable {
            val resolver = requireContext().contentResolver
            val displayName = resolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "attachment"
            val safeName = displayName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .takeLast(80)
                .ifEmpty { "attachment" }
            val file = File.createTempFile("broadcast_", "_$safeName", requireContext().cacheDir)
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw FileNotFoundException(uri.toString())
            file
        }.subscribeOn(Schedulers.io())

    private fun showCameraError(message: Int) {
        binding?.let {
            Snackbar.make(it.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        recipientsLoaded = false
        binding?.contactSubtitle?.setText(R.string.channels_loading)
        updateSendActions()
        val account = loadedChannelAccount().toObservable()
        // Both the definition and the recipient list belong to the originating account.
        disposables.add(Observable.combineLatest(
            repository.observe(),
            conversationFacade.getConversationList(account),
            accountService.currentAccountSubject
        ) { channels, list, current -> Triple(channels, list, current) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (channels, list, current) ->
                // Only old saved arguments carry a name. Resolve it once, never as a fallback
                // for an ID that has since been deleted or renamed.
                if (channelId == null) {
                    val legacyName = arguments?.getString(KEY_CHANNEL)
                    channelId = channels.firstOrNull { it.name == legacyName }?.id.orEmpty()
                    arguments?.putString(KEY_CHANNEL_ID, channelId)
                }
                channel = channels.firstOrNull { it.id == channelId }
                recipientsLoaded = true
                recipientCount = list.conversations.count {
                    it.accountId == channelAccountId && channel?.contains(it) == true
                }
                binding?.apply {
                    channel?.let { contactTitle.text = it.name }
                    contactSubtitle.text = when {
                        current.accountId != channelAccountId ->
                            getString(R.string.channels_account_changed)
                        channel == null -> getString(R.string.channels_deleted)
                        recipientCount == 0 -> getString(R.string.channels_no_members)
                        else -> resources.getQuantityString(
                            R.plurals.channels_recipients, recipientCount, recipientCount)
                    }
                }
                updateSendActions()
            }, { error ->
                recipientsLoaded = false
                binding?.contactSubtitle?.setText(R.string.channels_load_error)
                updateSendActions()
                showBroadcastError(error)
            }))
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        broadcastDisposables.clear()
        super.onDestroy()
    }

    /**
     * Sending a file hands it over to the conversation that receives it, so a broadcast gives
     * each of them its own copy, and keeps none.
     */
    private fun broadcastFile(file: File) {
        broadcastQueue.onNext(
            BroadcastJob(
                operation = {
                    targets()
                        .flatMapCompletable { targets ->
                            if (targets.any { !it.isSwarm })
                                Completable.error(
                                    IllegalStateException(
                                        getString(R.string.channels_file_requires_swarm)
                                    )
                                )
                            else {
                                sendPaced(targets) { target ->
                                    Completable.fromAction {
                                        val copy = File.createTempFile(
                                            "broadcast",
                                            "_" + file.name,
                                            requireContext().cacheDir
                                        )
                                        file.copyTo(copy, overwrite = true)
                                        conversationFacade
                                            .sendFile(target, target.uri, copy)
                                            .blockingAwait()
                                    }
                                }
                            }
                        }
                        .doFinally { file.delete() }
                },
                onSuccess = { showBroadcastSuccess() },
                onError = { error -> showBroadcastError(error) }
            )
        )
    }

    private fun broadcast() {
        val text = binding?.msgInputTxt?.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding?.msgInputTxt?.setText("")
        sendingText = true
        updateSendActions()

        broadcastQueue.onNext(
            BroadcastJob(
                operation = {
                    targets().flatMapCompletable { targets ->
                        sendPaced(targets) { target ->
                            conversationFacade.sendTextMessage(target, target.uri, text)
                        }
                    }
                },
                onSuccess = {
                    sendingText = false
                    updateSendActions()
                    showBroadcastSuccess()
                },
                onError = { error ->
                    sendingText = false
                    updateSendActions()
                    binding?.msgInputTxt?.setText(text)
                    showBroadcastError(error)
                }
            )
        )
    }

    /** The conversations the channel holds: its contacts and its groups. */
    private fun targets(): Single<List<Conversation>> =
        Single.defer {
            checkAccount()
            val id = channelId ?: throw IllegalStateException(getString(R.string.channels_loading))
            val account = loadedChannelAccount().toObservable()
            repository.observe().firstOrError()
                .flatMap { snapshot ->
                    check(snapshot.any { it.id == id }) { getString(R.string.channels_deleted) }
                    conversationFacade.getConversationList(account).firstOrError()
                }
                .map { list ->
                    checkAccount()
                    val latest = repository.load().firstOrNull { it.id == id }
                        ?: throw IllegalStateException(getString(R.string.channels_deleted))
                    list.conversations.filter { it.accountId == channelAccountId && latest.contains(it) }
                        .ifEmpty { throw IllegalStateException(getString(R.string.channels_no_members)) }
                }
        }

    private fun loadedChannelAccount() = conversationFacade.currentAccountSubject
        .firstOrError()
        .map { account ->
            // Wait for history loading, then freeze this account instead of following switches.
            check(account.accountId == channelAccountId) { getString(R.string.channels_account_changed) }
            account
        }

    private fun checkAccount() {
        val account = accountService.currentAccount
        check(account?.accountId == channelAccountId) {
            getString(R.string.channels_account_changed)
        }
    }

    private fun updateSendActions() {
        val account = accountService.currentAccount
        val enabled = recipientsLoaded && channel != null && recipientCount > 0 &&
            account?.accountId == channelAccountId
        binding?.apply {
            msgSend.isEnabled = enabled && !sendingText
            btnMenu.isEnabled = enabled
            btnTakePicture.isEnabled = enabled
        }
    }

    private fun showBroadcastSuccess() {
        binding?.let {
            Snackbar.make(it.root, R.string.channels_message_sent, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showBroadcastError(error: Throwable) {
        Log.e(TAG, "Unable to broadcast to Channel $channelId", error)
        binding?.let {
            Snackbar.make(
                it.root,
                error.message ?: getString(R.string.channels_send_error),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Sending starts only after the previous delivery completes and waits a full interval after
     * it. This bounds the aggregate queue, including multiple selected attachments, at 15/sec.
     */
    private fun sendPaced(targets: List<Conversation>, send: (Conversation) -> Completable) =
        Observable.fromIterable(targets)
            .concatMapCompletable { target ->
                send(target).andThen(
                    Completable.timer(MIN_INTERVAL_MS, TimeUnit.MILLISECONDS)
                )
            }

    companion object {
        private val TAG = BroadcastFragment::class.simpleName!!
        const val KEY_CHANNEL = "channel"
        const val KEY_CHANNEL_ID = "channel_id"
        const val KEY_ACCOUNT = "account"
        private const val MENU_MEDIA = 1
        private const val MENU_FILE = 2
        private const val MAX_MESSAGES_PER_SECOND = 15L
        private const val MIN_INTERVAL_MS = 1000L / MAX_MESSAGES_PER_SECOND + 1L

        fun newInstance(channelId: String, accountId: String) = BroadcastFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_CHANNEL_ID, channelId)
                putString(KEY_ACCOUNT, accountId)
            }
        }
    }
}
