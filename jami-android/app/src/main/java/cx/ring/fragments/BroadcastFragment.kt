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
import net.jami.model.Conversation
import net.jami.services.ConversationFacade
import java.io.File
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

    private val disposables = CompositeDisposable()
    private var binding: FragConversationBinding? = null
    private var pendingPhoto: File? = null
    private lateinit var channel: Channel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?,
    ): View = FragConversationBinding.inflate(inflater, container, false).apply {
        binding = this
        val name = arguments?.getString(KEY_CHANNEL).orEmpty()
        channel = ChannelRepository(requireContext()).load().firstOrNull { it.name == name }
            ?: ChannelRepository(requireContext()).activeChannel()

        toolbar.title = null
        toolbar.menu.clear()
        contactTitle.text = channel.name
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
        val photo = AndroidFileUtils.createImageFile(requireContext())
        pendingPhoto = photo
        takePicture.launch(ContentUri.getUriForFile(requireContext(), photo))
    }

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePicture()
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            pendingPhoto?.let { photo ->
                pendingPhoto = null
                if (saved) broadcastFile(photo)
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
        disposables.add(AndroidFileUtils.getCacheFile(requireContext(), uri)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file -> broadcastFile(file) }, { error ->
                Log.e(TAG, "Unable to read the attachment", error)
                binding?.let {
                    Snackbar.make(it.root, R.string.invalid_file, Snackbar.LENGTH_LONG).show()
                }
            }))
    }

    override fun onStart() {
        super.onStart()
        // The subtitle says who will receive it, which is what the channel holds right now.
        disposables.add(conversationFacade.getConversationList(conversationFacade.currentAccountSubject)
            .map { list -> list.conversations.count { channel.contains(it) } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ count ->
                binding?.contactSubtitle?.text =
                    resources.getQuantityString(R.plurals.channels_recipients, count, count)
            }, { error -> Log.e(TAG, "Unable to count channel members", error) }))
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

    /**
     * Sending a file hands it over to the conversation that receives it, so a broadcast gives
     * each of them its own copy, and keeps none.
     */
    private fun broadcastFile(file: File) {
        val binding = binding ?: return
        disposables.add(targets()
            .flatMapCompletable { targets ->
                sendPaced(targets) { target ->
                    Completable.fromAction {
                        val copy = File.createTempFile("broadcast", "_" + file.name, requireContext().cacheDir)
                        file.copyTo(copy, overwrite = true)
                        conversationFacade.sendFile(target, target.uri, copy).blockingAwait()
                    }.subscribeOn(Schedulers.io())
                }
            }
            .doFinally { file.delete() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Snackbar.make(binding.root, R.string.channels_message_sent, Snackbar.LENGTH_SHORT).show()
            }, { error ->
                Log.e(TAG, "Unable to broadcast a file to ${channel.name}", error)
                Snackbar.make(binding.root, error.message ?: "Unable to send file", Snackbar.LENGTH_LONG).show()
            }))
    }

    private fun broadcast() {
        val binding = binding ?: return
        val text = binding.msgInputTxt.text.toString().trim()
        if (text.isEmpty()) return
        binding.msgInputTxt.setText("")
        binding.msgSend.isEnabled = false

        disposables.add(targets()
            .flatMapCompletable { targets ->
                sendPaced(targets) { target ->
                    conversationFacade.sendTextMessage(target, target.uri, text)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                binding.msgSend.isEnabled = true
                Snackbar.make(binding.root, R.string.channels_message_sent, Snackbar.LENGTH_SHORT).show()
            }, { error ->
                Log.e(TAG, "Unable to broadcast to ${channel.name}", error)
                binding.msgSend.isEnabled = true
                binding.msgInputTxt.setText(text)
                Snackbar.make(binding.root, error.message ?: "Unable to send message", Snackbar.LENGTH_LONG).show()
            }))
    }

    /** The conversations the channel holds: its contacts and its groups. */
    private fun targets(): Single<List<Conversation>> =
        conversationFacade.getConversationList(conversationFacade.currentAccountSubject)
            .firstOrError()
            .map { list ->
                list.conversations.filter { channel.contains(it) }
                    .ifEmpty { throw IllegalStateException(getString(R.string.channels_no_members)) }
            }

    /**
     * A channel can hold more conversations than the daemon should be asked to serve at once:
     * whatever is broadcast leaves at a bounded rate, one every [MIN_INTERVAL_MS].
     */
    private fun sendPaced(targets: List<Conversation>, send: (Conversation) -> Completable) =
        Observable.fromIterable(targets)
            .zipWith(Observable.interval(0, MIN_INTERVAL_MS, TimeUnit.MILLISECONDS, Schedulers.computation())) { target, _ -> target }
            .concatMapCompletable(send)

    companion object {
        private val TAG = BroadcastFragment::class.simpleName!!
        const val KEY_CHANNEL = "channel"
        private const val MENU_MEDIA = 1
        private const val MENU_FILE = 2
        private const val MAX_MESSAGES_PER_SECOND = 15L
        private const val MIN_INTERVAL_MS = 1000L / MAX_MESSAGES_PER_SECOND

        fun newInstance(channelName: String) = BroadcastFragment().apply {
            arguments = Bundle().apply { putString(KEY_CHANNEL, channelName) }
        }
    }
}
