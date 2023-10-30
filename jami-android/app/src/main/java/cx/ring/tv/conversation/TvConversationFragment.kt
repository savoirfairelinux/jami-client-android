/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.tv.conversation

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.CallActivity
import cx.ring.client.ContactDetailsActivity
import cx.ring.client.MediaViewerActivity
import cx.ring.databinding.FragConversationTvBinding
import cx.ring.fragments.CallFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.service.DRingService
import cx.ring.tv.call.TVCallActivity
import cx.ring.tv.camera.CustomCameraActivity
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.conversation.ConversationPresenter
import net.jami.conversation.ConversationView
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import java.io.File
import java.io.IOException
import java.util.*

@AndroidEntryPoint
class TvConversationFragment : BaseSupportFragment<ConversationPresenter, ConversationView>(),
    ConversationView {
    private var mConversationPath: ConversationPath? = null
    private var mSelectedPosition = 0
    private var fileName: File? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var mStartRecording = true
    var mStartPlaying = true
    private var mAdapter: TvConversationAdapter? = null
    private var mConversationAvatar: AvatarDrawable? = null
    private val mParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private val mCompositeDisposable = CompositeDisposable()
    private var binding: FragConversationTvBinding? = null
    private var mCurrentFileAbsolutePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mConversationPath = ConversationPath.fromBundle(arguments)
        }
        savedInstanceState?.getString(KEY_AUDIOFILE)?.let { audioFile -> fileName = File(audioFile) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fileName?.let { file -> outState.putString(KEY_AUDIOFILE, file.absolutePath) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationTvBinding.inflate(inflater, container, false).apply {
            buttonText.setOnClickListener { displaySpeechRecognizer() }
            buttonVideo.setOnClickListener {
                if (checkAudioPermission(REQUEST_AUDIO_PERMISSION_FOR_VIDEO))
                    openVideoRecorder()
            }
            buttonAudio.setOnClickListener {
                onRecord(mStartRecording)
                mStartRecording = !mStartRecording
            }
            buttonText.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus: Boolean ->
                    TransitionManager.beginDelayedTransition(textContainer)
                    textText.visibility = if (hasFocus) View.VISIBLE else View.GONE
                }
            buttonAudio.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus: Boolean ->
                    TransitionManager.beginDelayedTransition(audioContainer)
                    textAudio.visibility = if (hasFocus) View.VISIBLE else View.GONE
                }
            buttonVideo.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus: Boolean ->
                    TransitionManager.beginDelayedTransition(videoContainer)
                    textVideo.visibility = if (hasFocus) View.VISIBLE else View.GONE
                }
            recyclerView.layoutManager = LinearLayoutManager(root.context).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        mCompositeDisposable.dispose()
    }

    // Create an intent that can start the Speech Recognizer activity
    private fun displaySpeechRecognizer() {
        if (!checkAudioPermission(REQUEST_RECORD_AUDIO_PERMISSION)) return
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getText(R.string.conversation_input_speech_hint)), REQUEST_SPEECH_CODE)
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Can't get voice input", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun displayErrorToast(error: Error) {
        val errorString: String = when (error) {
            Error.NO_INPUT -> getString(R.string.call_error_no_camera_no_microphone)
            Error.INVALID_FILE -> getString(R.string.invalid_file)
            Error.NOT_ABLE_TO_WRITE_FILE -> getString(R.string.not_able_to_write_file)
            Error.NO_SPACE_LEFT -> getString(R.string.no_space_left_on_device)
            else -> getString(R.string.generic_error)
        }
        Toast.makeText(requireContext(), errorString, Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = TvConversationAdapter(this, presenter)
        presenter.init(mConversationPath!!.conversationUri, mConversationPath!!.accountId)
        binding!!.recyclerView.adapter = mAdapter
    }

    private fun checkAudioPermission(code: Int): Boolean {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, code)
            return false
        }
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return if (mAdapter!!.onContextItemSelected(item)) true
        else super.onContextItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_PHOTO -> if (resultCode == Activity.RESULT_OK && data != null) {
                val media = data.extras!![MediaStore.EXTRA_OUTPUT] as Uri?
                val type = data.type
                createMediaDialog(media, type)
            }
            REQUEST_SPEECH_CODE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val results: List<String>? =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results!![0]
                createTextDialog(spokenText)
            }
            REQUEST_CODE_SAVE_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { writeToFile(it) }
                }
                super.onActivityResult(requestCode, resultCode, data)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun writeToFile(data: Uri) {
        val path = mCurrentFileAbsolutePath ?: return
        val cr = context?.contentResolver ?: return
        val input = File(path)
        mCompositeDisposable.add(AndroidFileUtils.copyFileToUri(cr, input, data)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ Toast.makeText(context, R.string.file_saved_successfully, Toast.LENGTH_SHORT).show() })
            { Toast.makeText(context, R.string.generic_error, Toast.LENGTH_SHORT).show() })
    }

    private fun createTextDialog(spokenText: String) {
        if (spokenText.isEmpty()) {
            return
        }
        val alertDialog =
            MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
                .setTitle(spokenText)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send) { dialog: DialogInterface?, whichButton: Int ->
                    presenter.sendTextMessage(spokenText)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        alertDialog.window!!.setLayout(DIALOG_WIDTH, DIALOG_HEIGHT)
        alertDialog.setOwnerActivity(requireActivity())
        alertDialog.setOnShowListener { dialog: DialogInterface? ->
            val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isFocusable = true
            positive.isFocusableInTouchMode = true
            positive.requestFocus()
        }
        alertDialog.show()
    }

    private fun createMediaDialog(media: Uri?, type: String?) {
        if (media == null) {
            return
        }
        val activity = activity ?: return
        val file = AndroidFileUtils.getCacheFile(activity, media)
        val alertDialog =
            MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
                .setTitle(if (type == CustomCameraActivity.TYPE_IMAGE) R.string.tv_send_image_dialog_message else R.string.tv_send_video_dialog_message)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send) { dialog: DialogInterface?, whichButton: Int ->
                    startFileSend(file.flatMapCompletable { file -> sendFile(file) })
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.tv_media_preview, null)
                .create()
        alertDialog.window!!
            .setLayout(DIALOG_WIDTH, DIALOG_HEIGHT)
        alertDialog.setOwnerActivity(activity)
        alertDialog.setOnShowListener { dialog: DialogInterface? ->
            val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isFocusable = true
            positive.isFocusableInTouchMode = true
            positive.requestFocus()
            val button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {
                try {
                    if (type == CustomCameraActivity.TYPE_IMAGE) {
                        val i = Intent(context, MediaViewerActivity::class.java)
                        i.setAction(Intent.ACTION_VIEW).setDataAndType(media, "image/*").flags =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(i)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, media)
                        intent.setDataAndType(media, "video/*").flags =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting activity", e)
                }
            }
        }
        alertDialog.show()
    }

    private fun createAudioDialog() {
        val alertDialog =
            MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
                .setTitle(R.string.tv_send_audio_dialog_message)
                .setMessage("")
                .setPositiveButton(R.string.tv_dialog_send) { _, _ -> sendAudio() }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.tv_audio_play, null)
                .create()
        alertDialog.window!!
            .setLayout(DIALOG_WIDTH, DIALOG_HEIGHT)
        alertDialog.setOwnerActivity(requireActivity())
        alertDialog.setOnShowListener { dialog: DialogInterface? ->
            val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isFocusable = true
            positive.isFocusableInTouchMode = true
            positive.requestFocus()
            val button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {
                onPlay(mStartPlaying)
                if (mStartPlaying) {
                    button.setText(R.string.tv_audio_pause)
                    player?.let { player -> player.setOnCompletionListener {
                        button.setText(R.string.tv_audio_play)
                        mStartPlaying = true
                    } }
                } else {
                    button.setText(R.string.tv_audio_play)
                }
                mStartPlaying = !mStartPlaying
            }
        }
        alertDialog.show()
    }

    override fun addElement(element: Interaction) {
        mAdapter!!.add(element)
        scrollToTop()
    }

    override fun shareFile(path: File, displayName: String) {
        val c = context ?: return
        try {
            val fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path)
            val type = c.contentResolver.getType(fileUri)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                setDataAndType(fileUri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, fileUri)
            }
            startActivity(Intent.createChooser(sendIntent, null))
        } catch (e: Exception) {
            Snackbar.make(requireView(),
                "Error sharing file: ${e.localizedMessage}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun openFile(path: File, displayName: String) {
        val c = context ?: return
        try {
            val fileUri = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, path)
            val type = c.contentResolver.getType(fileUri)
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, fileUri)
            }
            startActivity(Intent.createChooser(openIntent, null))
        } catch (e: IllegalArgumentException) {
            Snackbar.make(
                requireView(),
                "Error opening file: " + e.localizedMessage,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Creates an intent using Android Storage Access Framework
     * This intent is then received by applications that can handle it like
     * Downloads or Google drive
     * @param currentFile DataTransfer of the file that is going to be stored
     * @param fileAbsolutePath absolute path of the file we want to save
     */
    override fun startSaveFile(file: DataTransfer, fileAbsolutePath: String) {
        mCurrentFileAbsolutePath = fileAbsolutePath
        try {
            // Use Android Storage File Access to download the file
            val downloadFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            downloadFileIntent.type = AndroidFileUtils.getMimeTypeFromExtension(file.extension)
            downloadFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
            downloadFileIntent.putExtra(Intent.EXTRA_TITLE, file.displayName)
            startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE)
        } catch (e: Exception) {
            Log.i(TAG, "No app detected for saving files.")
            val directory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            writeToFile(Uri.fromFile(File(directory, file.displayName)))
        }
    }

    override fun startReplyTo(interaction: Interaction) {
        TODO("Not yet implemented")
    }

    override fun refreshView(conversation: List<Interaction>) {
        mAdapter?.updateDataset(conversation)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onStop() {
        releaseRecorder()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION_FOR_VIDEO -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openVideoRecorder()
            }
            REQUEST_RECORD_AUDIO_PERMISSION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                mStartRecording = !mStartRecording
            }
        }
    }

    private fun onRecord(start: Boolean) {
        if (start) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun onPlay(start: Boolean) {
        if (start) {
            startPlaying()
        } else {
            stopPlaying()
        }
    }

    private fun startPlaying() {
        val fileName =  fileName ?: return
        try {
            player = MediaPlayer().apply {
                setDataSource(fileName.absolutePath)
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed")
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording() {
        if (!checkAudioPermission(REQUEST_RECORD_AUDIO_PERMISSION)) return
        if (recorder != null) {
            return
        }
        try {
            fileName = AndroidFileUtils.createAudioFile(requireContext())
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFile(fileName!!.absolutePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                } else {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error starting recording: " + e.localizedMessage, Toast.LENGTH_LONG).show()
            mStartRecording = !mStartRecording
            recorder?.let { rec ->
                rec.release()
                recorder = null
            }
            return
        }
        binding!!.buttonAudio.setImageResource(androidx.leanback.R.drawable.lb_ic_stop)
        binding!!.textAudio.setText(R.string.tv_audio_recording)
        val anim: Animation = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 500
            startOffset = 100
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding!!.textAudio.startAnimation(anim)
    }

    private fun releaseRecorder() {
        if (recorder != null) {
            try {
                recorder!!.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Exception stopping recorder")
            }
            recorder!!.release()
            recorder = null
        }
    }

    private fun stopRecording() {
        releaseRecorder()
        binding!!.buttonAudio.setImageResource(R.drawable.baseline_androidtv_message_audio)
        binding!!.textAudio.setText(R.string.tv_send_audio)
        binding!!.textAudio.clearAnimation()
        createAudioDialog()
    }

    private fun sendAudio() {
        val file = fileName
        if (file != null) {
            val singleFile = Single.just(file)
            fileName = null
            startFileSend(singleFile.flatMapCompletable { f -> sendFile(f) })
        }
    }

    private fun startFileSend(op: Completable) {
        op.observeOn(AndroidSchedulers.mainThread())
            .subscribe({}) { e: Throwable? ->
                Log.e(TAG, "startFileSend: not able to create cache file", e)
                displayErrorToast(Error.INVALID_FILE)
            }
    }

    private fun sendFile(file: File): Completable {
        return Completable.fromAction { presenter.sendFile(file) }
    }

    fun updatePosition(position: Int) {
        mSelectedPosition = position
    }

    fun updateAdapterItem() {
        if (mSelectedPosition != -1) {
            mAdapter!!.notifyItemChanged(mSelectedPosition)
            mSelectedPosition = -1
        }
    }

    private fun scrollToTop() {
        if (mAdapter!!.itemCount > 0) {
            binding!!.recyclerView.scrollToPosition(mAdapter!!.itemCount - 1)
        }
    }

    override fun displayContact(conversation: ConversationItemViewModel) {
        binding?.let { binding ->
            binding.title.text = conversation.title
            if (conversation.title.isEmpty() || conversation.title != conversation.uriTitle)
                binding.subtitle.text = conversation.uriTitle
            else
                binding.subtitle.visibility = View.GONE
        }
    }

    override fun updateElement(element: Interaction) {
        mAdapter?.update(element)
    }

    override fun removeElement(element: Interaction) {
        mAdapter?.remove(element)
    }

    fun getConversationAvatar(uri: String): AvatarDrawable? {
        return mParticipantAvatars[uri]
    }

    override fun scrollToEnd() {}

    override fun scrollToMessage(messageId: String, flash: Boolean) {}

    override fun updateContact(contact: ContactViewModel) {
        mCompositeDisposable.add(AvatarFactory.getAvatar(requireContext(), contact, true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { avatar: Drawable ->
                mParticipantAvatars[contact.contact.primaryNumber] = avatar as AvatarDrawable
                mAdapter!!.setPhoto()
            })
    }

    override fun setComposingStatus(composingStatus: ComposingStatus) {}
    override fun setConversationColor(integer: Int) {}
    override fun setConversationSymbol(symbol: CharSequence) {}
    override fun startShareLocation(accountId: String, conversationId: String) {}
    override fun showMap(accountId: String, contactId: String, open: Boolean) {}
    override fun hideMap() {}
    override fun showPluginListHandlers(accountId: String, contactId: String) {}
    override fun hideErrorPanel() {}
    override fun displayNetworkErrorPanel() {}
    override fun displayAccountOfflineErrorPanel() {}
    override fun setSettings(linkPreviews: Boolean) {}
    override fun addSearchResults(results: List<Interaction>) {}
    override fun goToSearchMessage(messageId: String) {}
    override fun shareText(body: String) {
        TODO("Not yet implemented")
    }

    override fun displayOnGoingCallPane(display: Boolean) {}
    override fun displayNumberSpinner(conversation: Conversation, number: net.jami.model.Uri) {}
    override fun hideNumberSpinner() {}
    override fun clearMsgEdit() {}
    override fun goToHome() {}
    override fun goToAddContact(contact: Contact) {}
    override fun goToCallActivity(conferenceId: String, hasVideo: Boolean) {}
    override fun goToCallActivityWithResult(
        accountId: String,
        conversationUri: net.jami.model.Uri,
        contactUri: net.jami.model.Uri,
        audioOnly: Boolean
    ) {
    }

    override fun goToContactActivity(accountId: String, uri: net.jami.model.Uri) {}
    override fun switchToUnknownView(name: String) {
        binding?.apply {
            conversationActionGroup.isVisible = false
            conversationActionMessage.text = getString(R.string.message_contact_not_trusted, name)
            conversationActionMessage.isVisible = true
        }
    }

    override fun switchToIncomingTrustRequestView(name: String) {
        binding?.apply {
            conversationActionGroup.isVisible = false
            conversationActionMessage.text = name
            conversationActionMessage.isVisible = true
        }
    }

    override fun switchToConversationView() {
        binding?.apply {
            conversationActionGroup.isVisible = true
            conversationActionMessage.isVisible = false
        }
    }

    override fun switchToBannedView() {
        binding?.apply {
            conversationActionGroup.isVisible = false
            conversationActionMessage.text = getString(R.string.conversation_contact_banned, "")
            conversationActionMessage.isVisible = true
        }
    }

    override fun switchToSyncingView() {
        binding?.apply {
            conversationActionGroup.isVisible = false
            conversationActionMessage.text = getString(R.string.conversation_contact_banned, "")
            conversationActionMessage.isVisible = true
        }
    }

    override fun switchToEndedView() {
        binding?.apply {
            conversationActionGroup.isVisible = false
            conversationActionMessage.text = getText(R.string.conversation_ended)
            conversationActionMessage.isVisible = true
        }
    }

    override fun openFilePicker() {}
    override fun acceptFile(accountId: String, conversationUri: net.jami.model.Uri, transfer: DataTransfer) {
        val cacheDir = requireContext().cacheDir
        val spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString())
        if (spaceLeft == -1L || transfer.totalSize > spaceLeft) {
            presenter.noSpaceLeft()
            return
        }
        requireActivity().startService(Intent(DRingService.ACTION_FILE_ACCEPT)
            .setClass(requireContext(), DRingService::class.java)
            .setData(ConversationPath.toUri(accountId, conversationUri))
            .putExtra(DRingService.KEY_MESSAGE_ID, transfer.messageId)
            .putExtra(DRingService.KEY_TRANSFER_ID, transfer.fileId))
    }

    override fun goToGroupCall(
        conversation: Conversation,
        contactUri: net.jami.model.Uri,
        hasVideo: Boolean
    ) {
        // Try to find an existing call
        val conf = conversation.currentCall

        // If there is an existing call, go to it
        if (conf != null
            && conf.participants.isNotEmpty()
            && conf.participants[0].callStatus != Call.CallStatus.INACTIVE
            && conf.participants[0].callStatus != Call.CallStatus.FAILURE
        ) {
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setClass(requireContext(), TVCallActivity::class.java)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.id)
            )
        } else { // Otherwise, start a new call
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), TVCallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, ContactDetailsActivity.REQUEST_CODE_CALL)
        }
    }

    override fun refuseFile(accountId: String, conversationUri: net.jami.model.Uri, transfer: DataTransfer) {
        requireActivity().startService(Intent(DRingService.ACTION_FILE_CANCEL)
            .setClass(requireContext(), DRingService::class.java)
            .setData(ConversationPath.toUri(accountId, conversationUri))
            .putExtra(DRingService.KEY_MESSAGE_ID, transfer.messageId)
            .putExtra(DRingService.KEY_TRANSFER_ID, transfer.fileId))
    }

    private fun openVideoRecorder() {
        val intent = Intent(activity, CustomCameraActivity::class.java)
            .setAction(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, REQUEST_CODE_PHOTO)
    }

    companion object {
        private val TAG = TvConversationFragment::class.java.simpleName
        private const val ARG_MODEL = "model"
        private const val KEY_AUDIOFILE = "audiofile"
        private const val REQUEST_CODE_PHOTO = 101
        private const val REQUEST_SPEECH_CODE = 102
        private const val REQUEST_CODE_SAVE_FILE = 103
        private const val DIALOG_WIDTH = 900
        private const val DIALOG_HEIGHT = 400
        private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val REQUEST_AUDIO_PERMISSION_FOR_VIDEO = 201

        fun newInstance(args: Bundle?): TvConversationFragment {
            return TvConversationFragment().apply {
                arguments = args
            }
        }
    }
}