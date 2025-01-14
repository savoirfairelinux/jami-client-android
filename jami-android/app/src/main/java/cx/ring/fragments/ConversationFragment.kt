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
package cx.ring.fragments

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.ConversationAdapter
import cx.ring.client.CallActivity
import cx.ring.client.ConversationDetailsActivity
import cx.ring.client.ConversationActivity
import cx.ring.client.ConversationDetailsActivity.Companion.EXIT_REASON
import cx.ring.client.ConversationDetailsActivity.Companion.ExitReason
import cx.ring.databinding.FragConversationBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.service.DRingService
import cx.ring.service.LocationSharingService
import cx.ring.services.NotificationServiceImpl
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationPreferences
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.utils.*
import cx.ring.utils.ContentUri.getShareItems
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.conversation.ConversationPresenter
import net.jami.conversation.ConversationView
import net.jami.daemon.JamiService
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.interaction.Call
import net.jami.model.interaction.DataTransfer
import net.jami.model.interaction.Interaction
import net.jami.model.interaction.TextMessage
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import java.io.File
import java.util.*

@AndroidEntryPoint
class ConversationFragment : BaseSupportFragment<ConversationPresenter, ConversationView>(),
    ConversationView, SearchView.OnQueryTextListener {
    private var locationServiceConnection: ServiceConnection? = null
    private var binding: FragConversationBinding? = null
    private var currentBottomView: View? = null
    private var mAdapter: ConversationAdapter? = null
    private var mSearchAdapter: ConversationAdapter? = null
    private val animation = ValueAnimator()
    private var mPreferences: SharedPreferences? = null
    private var mCurrentPhoto: File? = null
    private var mCurrentFileAbsolutePath: String? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var replyingTo: Interaction? = null
    private var mIsBubble = false
    private val mParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private val mSmallParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private var mapWidth = 0
    private var mapHeight = 0
    private var marginPxTotal = 0
    private var loading = true
    private var animating = 0
    private var lastInsets: WindowInsetsCompat? = null

    private fun updatePaddings(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        binding?.apply {
            if (errorMsgPane.isVisible) {
                errorMsgPane.updatePadding(top = insets.top)
                appbar.updatePadding(top = 0)
            } else {
                errorMsgPane.updatePadding(top = 0)
                appbar.updatePadding(top = insets.top)
            }

            mainContainer.updatePadding(bottom = insets.bottom)
        }
    }

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
            for (uri in uris) {
                startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                    .observeOn(DeviceUtils.uiScheduler)
                    .flatMapCompletable { file: File -> sendFile(file) })
            }
        }

    fun getConversationAvatar(uri: String): AvatarDrawable? = mParticipantAvatars[uri]

    override fun refreshView(conversation: List<Interaction>) {
        if (binding != null) binding!!.pbLoading.visibility = View.GONE
        mAdapter?.let { adapter ->
            adapter.updateDataset(conversation.toMutableList())
            loading = false
        }
    }

    override fun scrollToEnd() {
        mAdapter?.let { adapter ->
            if (adapter.itemCount > 0)
                binding?.histList?.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun scrollToMessage(messageId: String, highlight: Boolean) {
        val histList = binding?.histList ?: return
        mAdapter?.let { adapter ->
            val position = adapter.getMessagePosition(messageId)
            if (position == -1)
                return

            histList.scrollToPosition(position)

            if (highlight) {
                histList.doOnNextLayout {
                    histList.layoutManager?.findViewByPosition(position)
                        ?.setBackgroundColor(requireContext().getColor(R.color.surface))
                }
            }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val res = resources
        mapWidth = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_width)
        mapHeight = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_height)
        marginPxTotal = res.getDimensionPixelSize(R.dimen.conversation_message_input_margin)

        return FragConversationBinding.inflate(inflater, container, false).apply {
            animation.duration = 150
            animation.addUpdateListener { valueAnimator: ValueAnimator ->
                histList.updatePadding(bottom = valueAnimator.animatedValue as Int)
            }

            ViewCompat.setWindowInsetsAnimationCallback(
                mainContainer,
                object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                        animating++
                    }

                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: List<WindowInsetsAnimationCompat>
                    ): WindowInsetsCompat {
                        val windowInsets = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                        )
                        mainContainer.updatePadding(bottom = windowInsets.bottom)
                        return insets
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        animating--
                    }
                })

            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                lastInsets = windowInsets
                if (animating == 0) {
                    updatePaddings(windowInsets)
                }
                WindowInsetsCompat.CONSUMED
            }

            // Content may be both text and non-text (HTML, images, videos, audio files, etc).
            ViewCompat.setOnReceiveContentListener(msgInputTxt, SUPPORTED_MIME_TYPES) { _, payload ->
                // Split the incoming content into two groups: content URIs and everything else.
                // This way we can implement custom handling for URIs and delegate the rest.
                val split = payload.partition { item -> item.uri != null }
                val uriContent = split.first
                val remaining = split.second

                // Handles content URIs.
                if (uriContent != null) {
                    val clip = uriContent.clip
                    for (i in 0 until clip.itemCount) {
                        val uri = clip.getItemAt(i).uri
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .flatMapCompletable { sendFile(it) })
                    }
                }
                // Delegates the processing for text and everything else to the platform.
                remaining
            }

            msgInputTxt.setOnEditorActionListener { _, actionId: Int, _ -> actionSendMsgText(actionId) }
            msgInputTxt.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus: Boolean ->
                if (hasFocus) {
                    (childFragmentManager.findFragmentById(R.id.mapLayout) as? LocationSharingFragment)?.hideControls()
                }
            }

            msgInputTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val message = s.toString()
                    val hasMessage = !TextUtils.isEmpty(message)
                    presenter.onComposingChanged(hasMessage)
                    if (hasMessage) {
                        msgSend.visibility = View.VISIBLE
                        emojiSend.visibility = View.GONE
                    } else {
                        msgSend.visibility = View.GONE
                        emojiSend.visibility = View.VISIBLE
                    }
                    mPreferences?.let { preferences ->
                        if (hasMessage)
                            preferences.edit().putString(KEY_PREFERENCE_PENDING_MESSAGE, message).apply()
                        else preferences.edit().remove(KEY_PREFERENCE_PENDING_MESSAGE).apply()
                    }
                }
            })

            msgInputTxt.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (oldBottom == 0 && oldTop == 0) {
                    // set initial list padding
                    histList.updatePadding(bottom = (currentBottomView?.height ?: 0) + marginPxTotal)
                } else {
                    if (animation.isStarted) animation.cancel()
                    animation.setIntValues(
                        histList.paddingBottom,
                        (currentBottomView?.height ?: 0) + marginPxTotal
                    )
                    animation.start()
                }
            }

            replyCloseBtn.setOnClickListener { clearReply() }
            fabLatest.setOnClickListener { scrollToEnd() }

            toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            toolbar.setOnClickListener { presenter.openContact() }
            toolbar.addMenuProvider(menuProvider)

            ongoingCallPane.setOnClickListener { presenter.clickOnGoingPane() }
            msgSend.setOnClickListener { sendMessageText() }
            emojiSend.setOnClickListener { sendEmoji() }
            btnMenu.setOnClickListener { expandMenu(it) }
            btnTakePicture.setOnClickListener { takePicture() }
            unknownContactButton.setOnClickListener { presenter.onAddContact() }
            btnBlock.setOnClickListener { presenter.onBlockIncomingContactRequest() }
            btnRefuse.setOnClickListener { presenter.onRefuseIncomingContactRequest() }
            btnAccept.setOnClickListener { presenter.onAcceptIncomingContactRequest() }

            binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            mPreferences?.let { preferences ->
                val pendingMessage = preferences.getString(KEY_PREFERENCE_PENDING_MESSAGE, null)
                if (!pendingMessage.isNullOrEmpty()) {
                    msgInputTxt.setText(pendingMessage)
                    msgSend.visibility = View.VISIBLE
                    emojiSend.visibility = View.GONE
                }
            }

            histList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                // The minimum amount of items to have below current scroll position before loading more.
                val visibleLoadThreshold = 3
                // The amount of items to have below the current scroll position to display
                // the scroll to latest button.
                val visibleLatestThreshold = 8
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    if (!loading && histList.adapter != mSearchAdapter
                        && layoutManager.findFirstVisibleItemPosition() < visibleLoadThreshold
                    ) {
                        loading = true
                        presenter.loadMore()
                    }

                    // Recyclerview is composed of items which are sometimes invisible (to preserve
                    // the model and interaction relationship).
                    // Because of bug #1251, we use findLastCompletelyVisibleItemPosition because
                    // findLastVisibleItemPosition ignores invisible items (don't understand why).
                    val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    if (layoutManager.itemCount - lastVisibleItemPosition > visibleLatestThreshold)
                        fabLatest.show()
                    else fabLatest.hide()
                }
            })

            (histList.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
            histList.adapter = mAdapter
            searchList.adapter = mSearchAdapter
        }
    }

    override fun setConversationColor(@ColorInt color: Int) {
        mAdapter?.convColor = getConversationColor(requireContext(), color)
        mSearchAdapter?.convColor = getConversationColor(requireContext(), color)
    }

    override fun setConversationSymbol(symbol: CharSequence) {
        binding?.emojiSend?.text = getConversationSymbol(requireContext(), symbol)
    }

    override fun onDestroyView() {
        animation.removeAllUpdateListeners()
        binding?.histList?.adapter = null
        mCompositeDisposable.clear()
        locationServiceConnection?.let {
            try {
                requireContext().unbindService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding service: " + e.message)
            }
        }
        mAdapter = null
        super.onDestroyView()
        binding = null
    }

    private fun clearReply() {
        if (replyingTo != null) {
            replyingTo = null
            binding?.apply {
                replyGroup.isVisible = false
            }
        }
    }

    fun sendMessageText() {
        val message = binding!!.msgInputTxt.text.toString()
        presenter.sendTextMessage(message, replyingTo)
        clearMsgEdit()
    }

    fun sendEmoji() {
        presenter.sendTextMessage(binding!!.emojiSend.text.toString(), replyingTo)
        clearReply()
    }

    private fun expandMenu(v: View) {
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.conversation_share_actions)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.conv_send_audio -> sendAudioMessage()
                R.id.conv_send_video -> sendVideoMessage()
                R.id.conv_send_file -> openFilePicker()
                R.id.conv_select_media -> openGallery()
                R.id.conv_share_location -> shareLocation()
                R.id.chat_extensions -> presenter.showExtensionListHandlers()
            }
            false
        }
        popup.menu.findItem(R.id.chat_extensions).isVisible = JamiService.getPluginsEnabled() && !JamiService.getChatHandlers().isEmpty()
        popup.setForceShowIcon(true)
        popup.show()
    }

    override fun showExtensionListHandlers(accountId: String, contactId: String) {
        Log.w(TAG, "show Extension Chat Handlers List")
        val fragment = ExtensionHandlersListFragment.newInstance(accountId, contactId)
        childFragmentManager.beginTransaction()
            .add(R.id.extensionListHandlers, fragment, ExtensionHandlersListFragment.TAG)
            .commit()
        binding?.let { binding ->
            val params = binding.mapCard.layoutParams as RelativeLayout.LayoutParams
            if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                binding.mapCard.layoutParams = params
            }
            binding.mapCard.visibility = View.VISIBLE
        }
    }

    fun hideExtensionListHandlers() {
        if (binding!!.mapCard.visibility != View.GONE) {
            binding!!.mapCard.visibility = View.GONE
            val fragmentManager = childFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.extensionListHandlers)
            if (fragment != null) {
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
            }
        }
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        if (params.width != mapWidth) {
            params.width = mapWidth
            params.height = mapHeight
            binding!!.mapCard.layoutParams = params
        }
    }

    private fun shareLocation() {
        presenter.shareLocation()
    }

    fun closeLocationSharing(isSharing: Boolean) {
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        if (params.width != mapWidth) {
            params.width = mapWidth
            params.height = mapHeight
            params.updateMargins(top = resources.getDimensionPixelSize(R.dimen.location_sharing_minmap_margin))
            binding!!.mapCard.layoutParams = params
        }
        if (!isSharing) hideMap()
    }

    fun expandMapView() {
        // The binding.root view must have android:animateLayoutChanges="true"
        binding!!.root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        val margin = resources.getDimensionPixelSize(R.dimen.location_sharing_minmap_margin)
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(0, 0, 0, binding!!.cvMessageInput.height + margin)
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding!!.mapCard.layoutParams = params
    }

    override fun startShareLocation(accountId: String, conversationId: String) {
        showMap(accountId, conversationId, true)
    }

    override fun showMap(accountId: String, contactId: String, open: Boolean) {
        if (binding!!.mapCard.visibility == View.GONE) {
            expandMapView()

            val fragment = LocationSharingFragment.newInstance(accountId, contactId, open)
            childFragmentManager.beginTransaction()
                .add(R.id.mapLayout, fragment, "map")
                .commit()
            binding!!.mapCard.visibility = View.VISIBLE
        }
        if (open) {
            val fragment = childFragmentManager.findFragmentById(R.id.mapLayout)
            (fragment as? LocationSharingFragment)?.showControls()
        }
    }

    override fun hideMap() {
        if (binding!!.mapCard.visibility != View.GONE) {
            binding!!.mapCard.visibility = View.GONE
            val fragmentManager = childFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.mapLayout)
            if (fragment != null) {
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
            }
        }
    }

    private fun sendAudioMessage() {
        if (!presenter.deviceRuntimeService.hasAudioPermission()) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_CAPTURE_AUDIO)
        } else {
            try {
                val ctx = requireContext()
                val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                mCurrentPhoto = AndroidFileUtils.createAudioFile(ctx)
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_AUDIO)
            } catch (ex: Exception) {
                Log.e(TAG, "sendAudioMessage: error", ex)
                Toast.makeText(activity, getString(R.string.audio_recorder_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVideoMessage() {
        if (!presenter.deviceRuntimeService.hasVideoPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAPTURE_VIDEO)
        } else {
            try {
                val context = requireContext()
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                    putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                    putExtra(MediaStore.EXTRA_OUTPUT, ContentUri.getUriForFile(context, AndroidFileUtils.createVideoFile(context).apply {
                        mCurrentPhoto = this
                    }))
                }
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_VIDEO)
            } catch (ex: Exception) {
                Log.e(TAG, "sendVideoMessage: error", ex)
                Toast.makeText(activity, getString(R.string.video_recorder_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePicture() {
        if (!presenter.deviceRuntimeService.hasVideoPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_TAKE_PICTURE)
            return
        }
        val c = context ?: return
        try {
            val photoFile = AndroidFileUtils.createImageFile(c)
            Log.i(TAG, "takePicture: attempting to save to $photoFile")
            val photoURI = ContentUri.getUriForFile(c, photoFile)
            val takePictureIntent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            mCurrentPhoto = photoFile
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE)
        } catch (e: Exception) {
            Toast.makeText(c, getString(R.string.taking_picture_error), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER)
    }

    private fun openGallery() {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    private fun sendFile(file: File): Completable = Completable.fromAction {
        presenter.sendFile(file)
    }

    private fun startFileSend(op: Completable): Disposable {
        setLoading(true)
        return op.observeOn(DeviceUtils.uiScheduler)
            .doFinally { setLoading(false) }
            .subscribe({}) { e ->
                Log.e(TAG, "startFileSend: unable to create cache file", e)
                displayErrorToast(Error.INVALID_FILE)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        Log.w(TAG, "onActivityResult: $requestCode $resultCode $resultData")
        if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                val clipData = resultData.clipData
                if (clipData != null) { // checking multiple selection or not
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(DeviceUtils.uiScheduler)
                            .flatMapCompletable { file: File -> sendFile(file) })
                    }
                } else {
                    resultData.data?.let { uri ->
                        startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(DeviceUtils.uiScheduler)
                            .flatMapCompletable { file: File -> sendFile(file) })
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE || requestCode == REQUEST_CODE_CAPTURE_AUDIO || requestCode == REQUEST_CODE_CAPTURE_VIDEO) {
            if (resultCode != Activity.RESULT_OK) {
                mCurrentPhoto = null
                return
            }
            val currentPhoto = mCurrentPhoto
            var file: Single<File>? = null
            if (currentPhoto == null || !currentPhoto.exists() || currentPhoto.length() == 0L) {
                resultData?.data?.let { uri ->
                    file = AndroidFileUtils.getCacheFile(requireContext(), uri)
                }
            } else {
                file = Single.just(currentPhoto)
            }
            mCurrentPhoto = null
            val sendingFile = file
            if (sendingFile != null)
                startFileSend(sendingFile.flatMapCompletable { f -> sendFile(f) })
            else
                Toast.makeText(activity, getString(R.string.find_picture_error), Toast.LENGTH_SHORT).show()
        } else if (requestCode == REQUEST_CODE_SAVE_FILE) {
            val uri = resultData?.data
            if (resultCode == Activity.RESULT_OK && uri != null) {
                writeToFile(uri)
            }
        } else if (requestCode == REQUEST_CODE_EDIT_MESSAGE) {
            val uri = resultData?.data ?: return
            if (resultCode == Activity.RESULT_OK) {
                val path = InteractionPath.fromUri(uri) ?: return
                val message = resultData.getStringExtra(Intent.EXTRA_TEXT) ?: return
                presenter.editMessage(path.conversation.accountId, path.conversation.conversationUri, path.messageId, message)
            }
        }
    }

    private fun writeToFile(data: Uri) {
        val path = mCurrentFileAbsolutePath ?: return
        val cr = context?.contentResolver ?: return
        mCompositeDisposable.add(AndroidFileUtils.copyFileToUri(cr, File(path), data)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ Toast.makeText(context, R.string.file_saved_successfully, Toast.LENGTH_SHORT).show() })
            { Toast.makeText(context, R.string.generic_error, Toast.LENGTH_SHORT).show() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (i in permissions.indices) {
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    presenter.cameraPermissionChanged(granted)
                    if (granted) {
                        if (requestCode == REQUEST_CODE_CAPTURE_VIDEO)
                            sendVideoMessage()
                        else if (requestCode == REQUEST_CODE_TAKE_PICTURE)
                            takePicture()
                    }
                    return
                }
                Manifest.permission.RECORD_AUDIO -> {
                    if (granted && requestCode == REQUEST_CODE_CAPTURE_AUDIO)
                        sendAudioMessage()
                    return
                }
                else -> {}
            }
        }
    }

    override fun addElement(element: Interaction) {
        if (mAdapter!!.add(element) && element.type != Interaction.InteractionType.INVALID)
            scrollToEnd()
        loading = false
    }

    override fun updateElement(element: Interaction) {
        mAdapter?.update(element)
    }

    override fun removeElement(element: Interaction) {
        mAdapter?.remove(element)
    }

    override fun setComposingStatus(composingStatus: ComposingStatus) {
        mAdapter?.setComposingStatus(composingStatus)
        if (composingStatus == ComposingStatus.Active) scrollToEnd()
    }

    override fun acceptFile(accountId: String, conversationUri: net.jami.model.Uri, transfer: DataTransfer) {
        if (transfer.messageId == null && transfer.fileId == null)
            return
        val cacheDir = requireContext().cacheDir
        val spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString())
        if (spaceLeft == -1L || transfer.totalSize > spaceLeft) {
            presenter.noSpaceLeft()
            return
        }
        requireActivity().startService(Intent(DRingService.ACTION_FILE_ACCEPT, ConversationPath.toUri(accountId, conversationUri),
            requireContext(), DRingService::class.java)
            .putExtra(DRingService.KEY_MESSAGE_ID, transfer.messageId)
            .putExtra(DRingService.KEY_TRANSFER_ID, transfer.fileId)
        )
    }

    override fun shareFile(path: File, displayName: String) {
        val c = context ?: return
        AndroidFileUtils.shareFile(c, path, displayName)
    }

    override fun openFile(path: File, displayName: String) {
        val c = context ?: return
        AndroidFileUtils.openFile(c, path, displayName)
    }

    private fun actionSendMsgText(actionId: Int): Boolean = when (actionId) {
        EditorInfo.IME_ACTION_SEND -> {
            sendMessageText()
            true
        }
        else -> false
    }

    override fun onStart() {
        super.onStart()
        presenter.resume(mIsBubble)
    }

    override fun onStop() {
        super.onStop()
        presenter.pause()
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
            val searchMenuItem = menu.findItem(R.id.conv_search)
            searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    presenter.stopSearch()
                    binding?.searchList?.isVisible = false
                    currentBottomView?.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in))
                    return true
                }

                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    presenter.startSearch()
                    binding?.searchList?.isVisible = true
                    return true
                }
            })

            (searchMenuItem.actionView as? SearchView)?.let {
                it.setOnQueryTextListener(this@ConversationFragment)
                it.queryHint = getString(R.string.conversation_search_hint)
            }
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.conv_action_audiocall -> presenter.goToCall(false)
                R.id.conv_action_videocall -> presenter.goToCall(true)
                R.id.conv_contact_details -> presenter.openContact()
                else -> return false
            }
            return true
        }
    }

    // ==================== OnQueryTextListener methods =======================
    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String): Boolean {
        if (query.isNotBlank())
            presenter.setSearchQuery(query.trim())
        mSearchAdapter?.clearSearchResults()
        return true
    }
    // ================== OnQueryTextListener methods end =======================

    override fun addSearchResults(results: List<Interaction>) {
        mSearchAdapter?.addSearchResults(results)
    }

    override fun shareText(body: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, body)
            type = "text/plain"
        }, null))
    }

    override fun initPresenter(presenter: ConversationPresenter) {
        val path = ConversationPath.fromBundle(arguments)
        mIsBubble = requireArguments().getBoolean(NotificationServiceImpl.EXTRA_BUBBLE)
        Log.w(TAG, "initPresenter $path")
        if (path == null)
            return

        mAdapter = ConversationAdapter(this, presenter)
        mSearchAdapter = ConversationAdapter(this, presenter, isSearch = true)
        presenter.init(path.conversationUri, path.accountId)

        // Load shared preferences. Usually useful for non-swarm conversations.
        try {
            mPreferences = getConversationPreferences(
                    requireContext(), path.accountId, path.conversationUri
            ).also { sharedPreferences ->
                sharedPreferences.edit().remove(KEY_PREFERENCE_CONVERSATION_LAST_READ).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to load conversation preferences")
        }

        var connection = locationServiceConnection
        if (connection == null) {
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    Log.w(TAG, "onServiceConnected")
                    val binder = service as LocationSharingService.LocalBinder
                    val locationService = binder.service
                    if (locationService.isSharing(path)) {
                        showMap(path.accountId, path.conversationUri.uri, false)
                    }
                    /*try {
                        requireContext().unbindService(locationServiceConnection!!)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unbinding service", e)
                    }*/
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    Log.w(TAG, "onServiceDisconnected")
                    locationServiceConnection = null
                }
            }
            locationServiceConnection = connection
            Log.w(TAG, "bindService")
            requireContext().bindService(Intent(requireContext(), LocationSharingService::class.java), connection, 0)
        }
    }

    override fun updateContact(contact: ContactViewModel) {
        val contactKey = contact.contact.primaryNumber
        val a = mSmallParticipantAvatars[contactKey]
        if (a != null) {
            a.update(contact)
            mParticipantAvatars[contactKey]!!.update(contact)
            mAdapter?.setPhoto()
        } else {
            val builder = AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
            mParticipantAvatars[contactKey] = builder
                .withPresence(true)
                .build(requireContext())
            mSmallParticipantAvatars[contactKey] = builder
                .withPresence(false)
                .build(requireContext())
        }
    }

    override fun displayContact(conversation: ConversationItemViewModel) {
        val avatar = AvatarFactory.getAvatar(requireContext(), conversation).blockingGet()
        mParticipantAvatars[conversation.uri.rawRingId] = AvatarDrawable(avatar)
        setupActionbar(conversation, avatar)
    }

    override fun displayOngoingCallPane(display: Boolean, hasVideo: Boolean) {
        val binding = binding ?: return
        if (display) {
            (binding.returnActiveCallIcon.background as? AnimatedVectorDrawable)?.start()
            val icon = if (hasVideo) R.drawable.outline_videocam_24 else R.drawable.outline_call_24
            binding.returnActiveCallIcon.setImageResource(icon)
            binding.toolbar.menu.findItem(R.id.conv_action_videocall).setEnabled(false)
            binding.toolbar.menu.findItem(R.id.conv_action_audiocall).setEnabled(false)
            binding.ongoingCallPane.visibility = View.VISIBLE
        } else {
            (binding.returnActiveCallIcon.background as? AnimatedVectorDrawable)?.stop()
            binding.toolbar.menu.findItem(R.id.conv_action_videocall).setEnabled(true)
            binding.toolbar.menu.findItem(R.id.conv_action_audiocall).setEnabled(true)
            binding.ongoingCallPane.visibility = View.GONE
        }
    }

    override fun displayNumberSpinner(conversation: Conversation, number: net.jami.model.Uri) {
        binding!!.numberSelector.visibility = View.VISIBLE
        binding!!.numberSelector.setSelection(getIndex(binding!!.numberSelector, number))
    }

    override fun hideNumberSpinner() {
        binding!!.numberSelector.visibility = View.GONE
    }

    override fun clearMsgEdit() {
        clearReply()
        binding!!.msgInputTxt.setText("")
    }

    override fun goToHome() {
        val hostActivity = activity
        if (hostActivity is ConversationActivity) {
            hostActivity.finish()
        } else {
            // Post because we might be currently executing a fragment transaction
            view?.post { hostActivity?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    private val conversationDetailsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.extras?.getString(EXIT_REASON)?.let {
                    when (it) {
                        // Todo: Go to conversation for CONTACT_UNBLOCKED and INVITATION_ACCEPTED
                        ExitReason.CONTACT_ADDED.toString(),
                        ExitReason.CONTACT_DELETED.toString(),
                        ExitReason.CONTACT_BLOCKED.toString(),
                        ExitReason.CONTACT_UNBLOCKED.toString(),
                        ExitReason.CONVERSATION_LEFT.toString(),
                        ExitReason.INVITATION_ACCEPTED.toString() -> goToHome()
                        else -> {}
                    }
                }
            }
        }

    override fun goToDetailsActivity(accountId: String, uri: net.jami.model.Uri) {
        val logo = binding!!.conversationAvatar
        val options = ActivityOptionsCompat
            .makeSceneTransitionAnimation(requireActivity(), logo, "conversationIcon")
        val intent = Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, uri))
            .setClass(requireContext().applicationContext, ConversationDetailsActivity::class.java)
        conversationDetailsActivityLauncher.launch(intent, options)
    }

    override fun goToCallActivity(conferenceId: String, withCamera: Boolean) {
        startActivity(Intent(Intent.ACTION_VIEW)
            .setClass(requireContext(), CallActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(NotificationService.KEY_CALL_ID, conferenceId))
    }

    override fun goToCallActivityWithResult(accountId: String, conversationUri: net.jami.model.Uri, contactUri: net.jami.model.Uri, withCamera: Boolean) {
        startActivity(Intent(Intent.ACTION_CALL)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setClass(requireContext(), CallActivity::class.java)
            .putExtras(ConversationPath.toBundle(accountId, conversationUri))
            .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
            .putExtra(CallFragment.KEY_HAS_VIDEO, withCamera))
    }

    /**
     * Go to the group call activity
     */
    override fun goToGroupCall(
        conversation: Conversation, contactUri: net.jami.model.Uri, hasVideo: Boolean
    ) {
        // Attempt to find an existing call
        val conf = conversation.currentCall

        // If there is an existing call, go to it
        if (conf != null
            && conf.participants.isNotEmpty()
            && conf.participants[0].callStatus != Call.CallStatus.INACTIVE
            && conf.participants[0].callStatus != Call.CallStatus.FAILURE
        ) {
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setClass(requireContext(), CallActivity::class.java)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.id)
            )
        } else { // Otherwise, start a new call
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), CallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, ConversationDetailsActivity.REQUEST_CODE_CALL)
        }
    }

    private fun setupActionbar(conversation: ConversationItemViewModel, img: AvatarDrawable) {
        binding?.apply {
            conversationAvatar.setImageDrawable(img)
            contactTitle.text = conversation.title
            if (conversation.uriTitle != conversation.title) {
                contactSubtitle.text = conversation.uriTitle
                contactSubtitle.visibility = View.VISIBLE
            } else {
                contactSubtitle.text = ""
                contactSubtitle.visibility = View.GONE
            }
        }
    }

//    fun blockContactRequest() {
//        presenter.onBlockIncomingContactRequest()
//    }
//
//    fun refuseContactRequest() {
//        presenter.onRefuseIncomingContactRequest()
//    }
//
//    fun acceptContactRequest() {
//        presenter.onAcceptIncomingContactRequest()
//    }

    override fun switchToUnknownView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.VISIBLE
            trustRequestPrompt.visibility = View.GONE
            tvTrustRequestMessage.text = getString(R.string.outgoing_contact_invitation_message)
            trustRequestMessageLayout.visibility = View.VISIBLE
            currentBottomView = unknownContactPrompt
        }
    }

    override fun switchToIncomingTrustRequestView(name: String, requestMode: Conversation.Mode) {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.VISIBLE
            btnBlock.isVisible = requestMode == Conversation.Mode.OneToOne
            tvTrustRequestMessage.text = getString(R.string.invitation_received_message, name)
            trustRequestMessageLayout.visibility = View.VISIBLE
            currentBottomView = trustRequestPrompt
        }
    }

    override fun switchToConversationView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(true)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(true)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(true)
            cvMessageInput.visibility = View.VISIBLE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.GONE
            currentBottomView = cvMessageInput
            histList.updatePadding(bottom = cvMessageInput.height + marginPxTotal)
        }
    }

    override fun switchToSyncingView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.VISIBLE
            tvTrustRequestMessage.text = getText(R.string.conversation_syncing)
        }
        currentBottomView = null
    }

    override fun switchToBlockedView() {
        binding?.apply {
            toolbar.menu.findItem(R.id.conv_search).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_videocall).setVisible(false)
            toolbar.menu.findItem(R.id.conv_action_audiocall).setVisible(false)
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.VISIBLE
            tvTrustRequestMessage.text = getText(R.string.conversation_blocked)
        }
    }

    override fun switchToEndedView() {
        binding?.apply {
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.VISIBLE
            tvTrustRequestMessage.text = getText(R.string.conversation_ended)
        }
        currentBottomView = null
    }

    private fun setLoading(isLoading: Boolean) {
        val binding = binding ?: return
        if (isLoading) {
            binding.btnTakePicture.visibility = View.GONE
            binding.pbDataTransfer.visibility = View.VISIBLE
        } else {
            binding.btnTakePicture.visibility = View.VISIBLE
            binding.pbDataTransfer.visibility = View.GONE
        }
    }

    fun handleShareIntent(intent: Intent) {
        Log.w(TAG, "handleShareIntent $intent")
        val action = intent.action
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            intent.getShareItems(requireContext()).forEach { shareItem ->
                if (shareItem.type == "text/plain" && shareItem.text != null) {
                    binding!!.msgInputTxt.setText(shareItem.text)
                } else if (shareItem.data != null){
                    startFileSend(AndroidFileUtils.getCacheFile(requireContext(), shareItem.data).flatMapCompletable { file -> sendFile(file) })
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val path = ConversationPath.fromIntent(intent)
            if (path != null && intent.getBooleanExtra(EXTRA_SHOW_MAP, false)) {
                shareLocation()
            }
        }
    }

    /**
     * Creates an intent using Android Storage Access Framework
     * This intent is then received by applications that can handle it like
     * Downloads or Google drive
     * @param file DataTransfer of the file that is going to be stored
     * @param fileAbsolutePath absolute path of the file we want to save
     */
    override fun startSaveFile(file: DataTransfer, fileAbsolutePath: String) {
        //Get the current file absolute path and store it
        mCurrentFileAbsolutePath = fileAbsolutePath
        try {
            //Use Android Storage File Access to download the file
            val downloadFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = AndroidFileUtils.getMimeTypeFromExtension(file.extension)
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, file.displayName)
            }
            startActivityForResult(downloadFileIntent, REQUEST_CODE_SAVE_FILE)
        } catch (e: Exception) {
            Log.i(TAG, "No app detected for saving files.")
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            writeToFile(Uri.fromFile(File(directory, file.displayName)))
        }
    }

    override fun startReplyTo(interaction: Interaction) {
        replyingTo = interaction
        binding?.apply {
            if (interaction is TextMessage) {
                replyMessage.text = interaction.body
                replyMessage.isVisible = true
            }
            replyGroup.isVisible = true
        }
    }

    override fun displayNetworkErrorPanel() {
        binding?.apply {
            errorMsgPane.visibility = View.VISIBLE
            errorMsgPane.setOnClickListener(null)
            errorMsgPane.setText(R.string.error_no_network)
        }
        lastInsets?.let { updatePaddings(it) }
    }

    override fun displayAccountOfflineErrorPanel() {
        binding?.apply {
            errorMsgPane.visibility = View.VISIBLE
            errorMsgPane.setOnClickListener(null)
            errorMsgPane.setText(R.string.error_account_offline)
            for (idx in 0 until btnContainer.childCount) {
                btnContainer.getChildAt(idx).isEnabled = false
            }
        }
        lastInsets?.let { updatePaddings(it) }
    }

    override fun setSettings(linkPreviews: Boolean) {
        mAdapter?.showLinkPreviews = linkPreviews
        mSearchAdapter?.showLinkPreviews = linkPreviews
    }

    override fun hideErrorPanel() {
        binding?.errorMsgPane?.visibility = View.GONE
        lastInsets?.let { updatePaddings(it) }
    }

    override fun goToSearchMessage(messageId: String) {
        binding?.toolbar?.menu?.findItem(R.id.conv_search)?.collapseActionView()
        binding?.histList?.doOnNextLayout {
            presenter.scrollToMessage(messageId)
        }
    }

    companion object {
        val TAG = ConversationFragment::class.simpleName
        const val REQ_ADD_CONTACT = 42
        const val KEY_PREFERENCE_PENDING_MESSAGE = "pendingMessage"
        @Deprecated("Use daemon feature")
        const val KEY_PREFERENCE_CONVERSATION_LAST_READ = "lastRead"
        const val EXTRA_SHOW_MAP = "showMap"
        private const val REQUEST_CODE_FILE_PICKER = 1000
        private const val REQUEST_PERMISSION_CAMERA = 1001
        private const val REQUEST_CODE_TAKE_PICTURE = 1002
        private const val REQUEST_CODE_SAVE_FILE = 1003
        private const val REQUEST_CODE_CAPTURE_AUDIO = 1004
        private const val REQUEST_CODE_CAPTURE_VIDEO = 1005
        const val REQUEST_CODE_EDIT_MESSAGE = 1006
        private fun getIndex(spinner: Spinner, myString: net.jami.model.Uri): Int {
            var i = 0
            val n = spinner.count
            while (i < n) {
                if ((spinner.getItemAtPosition(i) as Phone).number == myString) {
                    return i
                }
                i++
            }
            return 0
        }

        private val SUPPORTED_MIME_TYPES = arrayOf("image/png", "image/jpg", "image/gif", "image/webp")
    }
}