/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
import cx.ring.databinding.FragConversationBinding
import cx.ring.repositories.CircleRepository
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
import net.jami.model.Circle
import net.jami.model.Conversation
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Writing to a Circle sends the message or file individually to each of its members
 * (contacts and groups).
 */
@AndroidEntryPoint
class BroadcastFragment : Fragment() {
    @Inject
    lateinit var conversationFacade: ConversationFacade

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var circleRepository: CircleRepository

    private val disposables = CompositeDisposable()
    private val broadcastDisposables = CompositeDisposable()
    private val broadcastQueue = PublishSubject.create<BroadcastJob>()
    private var binding: FragConversationBinding? = null
    private var pendingPhoto: File? = null
    private var circle: Circle? = null
    private var circleId: String? = null
    private lateinit var circleAccountId: String
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
        circleAccountId = arguments?.getString(KEY_ACCOUNT).orEmpty()
        circleId = state?.getString(KEY_CIRCLE_ID) ?: arguments?.getString(KEY_CIRCLE_ID)
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
        circleId?.let { outState.putString(KEY_CIRCLE_ID, it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View = FragConversationBinding.inflate(inflater, container, false).apply {
        binding = this
        toolbar.title = null
        toolbar.menu.clear()
        contactTitle.text = circle?.let { circleRepository.displayName(it) } ?: getString(R.string.function_circles)
        contactSubtitle.text = getString(R.string.broadcast_loading_circle)
        conversationAvatar.setImageResource(R.drawable.ic_circles_24)
        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener { requireActivity().finish() }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            appbar.updatePadding(top = bars.top)
            mainContainer.updatePadding(bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

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
        msgInputTxt.hint = getString(R.string.broadcast_input_hint)
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
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                    MENU_FILE -> pickFile.launch("*/*")
                }
                true
            }
            show()
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCamera.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val photo = AndroidFileUtils.createImageFile(requireContext())
            pendingPhoto = photo
            takePicture.launch(ContentUri.getUriForFile(requireContext(), photo))
        } catch (error: IOException) {
            Log.e(TAG, "Unable to prepare camera file", error)
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
        disposables.add(
            cacheAttachment(uri)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ file -> broadcastFile(file) }, { error ->
                    Log.e(TAG, "Unable to read attachment", error)
                    binding?.let {
                        Snackbar.make(it.root, R.string.invalid_file, Snackbar.LENGTH_LONG).show()
                    }
                })
        )
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
        binding?.contactSubtitle?.text = getString(R.string.broadcast_loading_circle)
        updateSendActions()
        val account = loadedCircleAccount().toObservable()
        disposables.add(
            Observable.combineLatest(
                circleRepository.observe(circleAccountId),
                conversationFacade.getConversationList(account),
                accountService.currentAccountSubject
            ) { circles, list, current -> Triple(circles, list, current) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (circles, list, current) ->
                    circle = circles.firstOrNull { it.id == circleId }
                    recipientsLoaded = true
                    recipientCount = list.conversations.count {
                        it.accountId == circleAccountId && circle?.contains(it) == true
                    }
                    binding?.apply {
                        circle?.let { contactTitle.text = getString(R.string.broadcast_title, circleRepository.displayName(it)) }
                        contactSubtitle.text = when {
                            current.accountId != circleAccountId -> getString(R.string.broadcast_account_changed)
                            circle == null -> getString(R.string.broadcast_circle_deleted)
                            recipientCount == 0 -> getString(R.string.broadcast_circle_empty)
                            else -> resources.getQuantityString(
                                R.plurals.broadcast_recipients, recipientCount, recipientCount
                            )
                        }
                    }
                    updateSendActions()
                }, { error ->
                    recipientsLoaded = false
                    binding?.contactSubtitle?.text = getString(R.string.broadcast_load_error)
                    updateSendActions()
                    showBroadcastError(error)
                })
        )
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

    private fun broadcastFile(file: File) {
        broadcastQueue.onNext(
            BroadcastJob(
                operation = {
                    targets()
                        .flatMapCompletable { targets ->
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

    private fun targets(): Single<List<Conversation>> =
        Single.defer {
            checkAccount()
            val id = circleId ?: throw BroadcastError(R.string.broadcast_loading_circle)
            val account = loadedCircleAccount().toObservable()
            circleRepository.observe(circleAccountId).firstOrError()
                .flatMap { snapshot ->
                    if (snapshot.none { it.id == id }) throw BroadcastError(R.string.broadcast_circle_deleted)
                    conversationFacade.getConversationList(account).firstOrError()
                }
                .map { list ->
                    checkAccount()
                    val latest = circleRepository.load(circleAccountId).firstOrNull { it.id == id }
                        ?: throw BroadcastError(R.string.broadcast_circle_deleted)
                    list.conversations.filter { it.accountId == circleAccountId && latest.contains(it) }
                        .ifEmpty { throw BroadcastError(R.string.broadcast_circle_empty) }
                }
        }

    private fun loadedCircleAccount() = conversationFacade.currentAccountSubject
        .firstOrError()
        .map { account ->
            if (account.accountId != circleAccountId) throw BroadcastError(R.string.broadcast_account_changed)
            account
        }

    private fun checkAccount() {
        val current = accountService.currentAccount
        if (current == null || current.accountId != circleAccountId)
            throw BroadcastError(R.string.broadcast_account_changed)
    }

    private fun sendPaced(
        targets: List<Conversation>,
        send: (Conversation) -> Completable,
    ): Completable =
        Observable.fromIterable(targets)
            .concatMapCompletable { target ->
                send(target).andThen(Completable.timer(PACE_MS, TimeUnit.MILLISECONDS))
            }

    private fun updateSendActions() {
        val canSend = recipientsLoaded && circle != null && recipientCount > 0 &&
            accountService.currentAccount?.accountId == circleAccountId
        binding?.apply {
            msgInputTxt.isEnabled = canSend && !sendingText
            msgSend.isEnabled = canSend && !sendingText
            btnMenu.isEnabled = canSend
            btnTakePicture.isEnabled = canSend
        }
    }

    private fun showBroadcastSuccess() {
        binding?.let {
            Snackbar.make(
                it.root,
                it.root.resources.getQuantityString(
                    R.plurals.broadcast_sent, recipientCount, recipientCount
                ),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showBroadcastError(error: Throwable) {
        Log.e(TAG, "Unable to broadcast to Circle", error)
        binding?.let {
            Snackbar.make(
                it.root,
                (error as? BroadcastError)?.let { e -> it.root.context.getString(e.messageRes) }
                    ?: error.message
                    ?: it.root.context.getString(R.string.broadcast_error),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /** Broadcast failure whose user-visible message is a string resource. */
    private class BroadcastError(val messageRes: Int) : IllegalStateException()

    companion object {
        const val KEY_CIRCLE_ID = "circle_id"
        const val KEY_ACCOUNT = "circle_account"
        private const val TAG = "BroadcastFragment"
        private const val MENU_MEDIA = 1
        private const val MENU_FILE = 2
        private const val PACE_MS = 67L

        fun newInstance(circleId: String, accountId: String) = BroadcastFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_CIRCLE_ID, circleId)
                putString(KEY_ACCOUNT, accountId)
            }
        }
    }
}
