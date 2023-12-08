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
package cx.ring.fragments

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.adapters.ConversationAdapter
import cx.ring.client.CallActivity
import cx.ring.client.ContactDetailsActivity
import cx.ring.client.ConversationActivity
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragConversationBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.service.DRingService
import cx.ring.service.LocationSharingService
import cx.ring.services.NotificationServiceImpl
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationColor
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationPreferences
import cx.ring.services.SharedPreferencesServiceImpl.Companion.getConversationSymbol
import cx.ring.utils.*
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import net.jami.call.CallPresenter
import net.jami.conversation.ConversationPresenter
import net.jami.conversation.ConversationPresenter.IncomingCallAction
import net.jami.conversation.ConversationView
import net.jami.daemon.JamiService
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.services.NotificationService
import net.jami.smartlist.ConversationItemViewModel
import java.io.File
import java.util.*

@AndroidEntryPoint
class ConversationFragment : BaseSupportFragment<ConversationPresenter, ConversationView>(),
    ConversationView, SearchView.OnQueryTextListener {
    private var locationServiceConnection: ServiceConnection? = null
    private var binding: FragConversationBinding? = null
    private var mAudioCallBtn: MenuItem? = null
    private var mVideoCallBtn: MenuItem? = null
    private var currentBottomView: View? = null
    private var mAdapter: ConversationAdapter? = null
    private var mSearchAdapter: ConversationAdapter? = null
    private var marginPx = 0
    private var marginPxTotal = 0
    private val animation = ValueAnimator()
    private var mPreferences: SharedPreferences? = null
    private var mCurrentPhoto: File? = null
    private var mCurrentFileAbsolutePath: String? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var mSelectedPosition = 0
    private var replyingTo: Interaction? = null
    private var mIsBubble = false
    private var mConversationAvatar: AvatarDrawable? = null
    private val mParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private val mSmallParticipantAvatars: MutableMap<String, AvatarDrawable> = HashMap()
    private var mapWidth = 0
    private var mapHeight = 0
    private var loading = true
    private var animating = 0

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
            for (uri in uris) {
                startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri)
                    .observeOn(DeviceUtils.uiScheduler)
                    .flatMapCompletable { file: File -> sendFile(file) })
            }
        }

    fun getConversationAvatar(uri: String): AvatarDrawable? = mParticipantAvatars[uri]

    fun getSmallConversationAvatar(uri: String): AvatarDrawable? {
        synchronized(mSmallParticipantAvatars) { return mSmallParticipantAvatars[uri] }
    }

    override fun refreshView(conversation: List<Interaction>) {
        if (binding != null) binding!!.pbLoading.visibility = View.GONE
        mAdapter?.let { adapter ->
            adapter.updateDataset(conversation)
            loading = false
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun scrollToEnd() {
        mAdapter?.let { adapter ->
            if (adapter.itemCount > 0)
                binding!!.histList.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun scrollToMessage(messageId: String, highlight: Boolean) {
        val histList = binding?.histList ?: return
        mAdapter?.let { adapter ->
            val position = adapter.getMessagePosition(messageId)
            if(position == -1)
                return
            binding!!.histList.scrollToPosition(position)

            if(highlight) {
                histList.doOnNextLayout {
                    histList.layoutManager?.findViewByPosition(position)
                        ?.setBackgroundColor(resources.getColor(R.color.surface))
                }
            }
        }
    }

    private fun updateListPadding() {
        /* val binding = binding ?: return
        val bottomView = currentBottomView ?: return
        val bottomViewHeight = bottomView.height
        if (bottomViewHeight != 0) {
            val padding = bottomViewHeight + marginPxTotal
            val params = binding.mapCard.layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = padding
            binding.mapCard.layoutParams = params
        } */
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

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                val count = childFragmentManager.backStackEntryCount
                if (count > 0) {
                    childFragmentManager.popBackStack()
                    if (count == 1)
                        isEnabled = false
                }
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.onBackPressedDispatcher?.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val res = resources
        marginPx = res.getDimensionPixelSize(R.dimen.conversation_message_input_margin)
        mapWidth = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_width)
        mapHeight = res.getDimensionPixelSize(R.dimen.location_sharing_minmap_height)
        marginPxTotal = marginPx
        return FragConversationBinding.inflate(inflater, container, false).let { binding ->
            this@ConversationFragment.binding = binding
            binding.presenter = this@ConversationFragment
            animation.duration = 150
            animation.addUpdateListener { valueAnimator: ValueAnimator -> binding.histList.updatePadding(bottom = valueAnimator.animatedValue as Int) }

            (activity as AppCompatActivity?)!!.setSupportActionBar(binding.toolbar)

            val layoutToAnimate = binding.relativeLayout
            if (Build.VERSION.SDK_INT >= 30) {
                ViewCompat.setWindowInsetsAnimationCallback(
                    layoutToAnimate,
                    object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                            animating++
                        }
                        override fun onProgress(insets: WindowInsetsCompat, runningAnimations: List<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                            layoutToAnimate.updatePadding(bottom = insets.systemWindowInsetBottom)
                            return insets
                        }

                        override fun onEnd(animation: WindowInsetsAnimationCompat) {
                            animating--
                        }
                    })
            }
            ViewCompat.setOnApplyWindowInsetsListener(layoutToAnimate) { _, insets: WindowInsetsCompat ->
                if (animating == 0) {
                    layoutToAnimate.updatePadding(
                        top = insets.systemWindowInsetTop,
                        bottom = insets.systemWindowInsetBottom
                    )
                }
                WindowInsetsCompat.CONSUMED
            }


            // Content may be both text and non-text (HTML, images, videos, audio files, etc).
            ViewCompat.setOnReceiveContentListener(
                binding.msgInputTxt,
                SUPPORTED_MIME_TYPES
            ) { _, payload ->
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

            binding.msgInputTxt.setOnEditorActionListener { _, actionId: Int, _ -> actionSendMsgText(actionId) }
            binding.msgInputTxt.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus: Boolean ->
                if (hasFocus) {
                    (childFragmentManager.findFragmentById(R.id.mapLayout) as LocationSharingFragment?)?.hideControls()
                }
            }
            binding.msgInputTxt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val message = s.toString()
                    val hasMessage = !TextUtils.isEmpty(message)
                    presenter.onComposingChanged(hasMessage)
                    if (hasMessage) {
                        binding.msgSend.visibility = View.VISIBLE
                        binding.emojiSend.visibility = View.GONE
                    } else {
                        binding.msgSend.visibility = View.GONE
                        binding.emojiSend.visibility = View.VISIBLE
                    }
                    mPreferences?.let { preferences ->
                        if (hasMessage)
                            preferences.edit().putString(KEY_PREFERENCE_PENDING_MESSAGE, message).apply()
                        else
                            preferences.edit().remove(KEY_PREFERENCE_PENDING_MESSAGE).apply()
                    }
                }
            })
            binding.replyCloseBtn.setOnClickListener {
                clearReply()
            }
            binding.fabLatest.setOnClickListener {
                scrollToEnd()
            }
            setHasOptionsMenu(true)
            binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            binding.toolbar.setNavigationOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            binding.ongoingCallPane.setOnClickListener { presenter.clickOnGoingPane() }
            binding.ringingCallPane.setOnClickListener {
                presenter.clickRingingPane(IncomingCallAction.VIEW_ONLY)
            }
            binding.acceptAudioCallButton.setOnClickListener {
                presenter.clickRingingPane(IncomingCallAction.ACCEPT_AUDIO)
            }
            binding.acceptVideoCallButton.setOnClickListener {
                presenter.clickRingingPane(IncomingCallAction.ACCEPT_VIDEO)
            }

            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let { binding ->
            mPreferences?.let { preferences ->
                val pendingMessage = preferences.getString(KEY_PREFERENCE_PENDING_MESSAGE, null)
                if (!pendingMessage.isNullOrEmpty()) {
                    binding.msgInputTxt.setText(pendingMessage)
                    binding.msgSend.visibility = View.VISIBLE
                    binding.emojiSend.visibility = View.GONE
                }
            }
            binding.msgInputTxt.addOnLayoutChangeListener { _, _, _, _, _, oldLeft, oldTop, oldRight, oldBottom ->
                if (oldBottom == 0 && oldTop == 0) {
                    updateListPadding()
                } else {
                    if (animation.isStarted) animation.cancel()
                    animation.setIntValues(
                        binding.histList.paddingBottom,
                        (currentBottomView?.height ?: 0) + marginPxTotal
                    )
                    animation.start()
                }
            }
            binding.histList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                // The minimum amount of items to have below current scroll position
                // before loading more.
                val visibleLoadThreshold = 3
                // The amount of items to have below the current scroll position to display
                // the scroll to latest button.
                val visibleLatestThreshold = 8
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager? ?: return
                    if (!loading
                        && layoutManager.findFirstVisibleItemPosition() < visibleLoadThreshold
                    ) {
                        loading = true
                        presenter.loadMore()
                    }

                    // Recyclerview is composed of items which are sometimes invisible (to preserve
                    // the model and interaction relationship).
                    // Because of bug #1251, we use findLastCompletelyVisibleItemPosition because
                    // findLastVisibleItemPosition ignores invisible items (don't understand why).
                    val lastVisibleItemPosition =
                        layoutManager.findLastCompletelyVisibleItemPosition()
                    if (layoutManager.itemCount - lastVisibleItemPosition > visibleLatestThreshold)
                        binding.fabLatest.show()
                    else binding.fabLatest.hide()
                }
            })

            val animator = binding.histList.itemAnimator as DefaultItemAnimator?
            animator?.supportsChangeAnimations = false
            binding.histList.adapter = mAdapter
        }
    }

    override fun setConversationColor(@ColorInt color: Int) {
        mAdapter?.setPrimaryColor(getConversationColor(requireContext(), color))
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

    override fun onContextItemSelected(item: MenuItem): Boolean =
        if (mAdapter!!.onContextItemSelected(item)) true
        else super.onContextItemSelected(item)

    fun updateAdapterItem() {
        if (mSelectedPosition != -1) {
            mAdapter?.notifyItemChanged(mSelectedPosition)
            mSelectedPosition = -1
        }
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

    @SuppressLint("RestrictedApi")
    fun expandMenu(v: View) {
        val context = requireContext()
        val popup = PopupMenu(context, v)
        popup.inflate(R.menu.conversation_share_actions)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.conv_send_audio -> sendAudioMessage()
                R.id.conv_send_video -> sendVideoMessage()
                R.id.conv_send_file -> openFilePicker()
                R.id.conv_select_media -> openGallery()
                R.id.conv_share_location -> shareLocation()
                R.id.chat_plugins -> presenter.showPluginListHandlers()
            }
            false
        }
        popup.menu.findItem(R.id.chat_plugins).isVisible = JamiService.getPluginsEnabled() && !JamiService.getChatHandlers().isEmpty()
        val menuHelper = MenuPopupHelper(context, (popup.menu as MenuBuilder), v)
        menuHelper.setForceShowIcon(true)
        menuHelper.show()
    }

    override fun showPluginListHandlers(accountId: String, contactId: String) {
        Log.w(TAG, "show Plugin Chat Handlers List")
        val fragment = PluginHandlersListFragment.newInstance(accountId, contactId)
        childFragmentManager.beginTransaction()
            .add(R.id.pluginListHandlers, fragment, PluginHandlersListFragment.TAG)
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

    fun hidePluginListHandlers() {
        if (binding!!.mapCard.visibility != View.GONE) {
            binding!!.mapCard.visibility = View.GONE
            val fragmentManager = childFragmentManager
            val fragment = fragmentManager.findFragmentById(R.id.pluginListHandlers)
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
            binding!!.mapCard.layoutParams = params
        }
        if (!isSharing) hideMap()
    }

    fun openLocationSharing() {
        binding!!.conversationLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        val params = binding!!.mapCard.layoutParams as RelativeLayout.LayoutParams
        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding!!.mapCard.layoutParams = params
        }
    }

    override fun startShareLocation(accountId: String, conversationId: String) {
        showMap(accountId, conversationId, true)
    }

    /**
     * Used to update with the past adapter position when a long click was registered
     */
    fun updatePosition(position: Int) {
        mSelectedPosition = position
    }

    override fun showMap(accountId: String, contactId: String, open: Boolean) {
        if (binding!!.mapCard.visibility == View.GONE) {
            Log.w(TAG, "showMap $accountId $contactId")
            val fragmentManager = childFragmentManager
            val fragment = LocationSharingFragment.newInstance(accountId, contactId, open)
            fragmentManager.beginTransaction()
                .add(R.id.mapLayout, fragment, "map")
                .commit()
            binding!!.mapCard.visibility = View.VISIBLE
        }
        if (open) {
            val fragment = childFragmentManager.findFragmentById(R.id.mapLayout)
            if (fragment != null) {
                (fragment as LocationSharingFragment).showControls()
            }
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
                Toast.makeText(activity, "Can't find audio recorder app", Toast.LENGTH_SHORT).show()
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
                    putExtra(MediaStore.EXTRA_OUTPUT, ContentUriHandler.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, AndroidFileUtils.createVideoFile(context).apply {
                        mCurrentPhoto = this
                    }))
                }
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_VIDEO)
            } catch (ex: Exception) {
                Log.e(TAG, "sendVideoMessage: error", ex)
                Toast.makeText(activity, "Can't find video recorder app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun takePicture() {
        if (!presenter.deviceRuntimeService.hasVideoPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_TAKE_PICTURE)
            return
        }
        val c = context ?: return
        try {
            val photoFile = AndroidFileUtils.createImageFile(c)
            Log.i(TAG, "takePicture: trying to save to $photoFile")
            val photoURI = ContentUriHandler.getUriForFile(c, ContentUriHandler.AUTHORITY_FILES, photoFile)
            val takePictureIntent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            mCurrentPhoto = photoFile
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE)
        } catch (e: Exception) {
            Toast.makeText(c, "Error taking picture: " + e.localizedMessage, Toast.LENGTH_SHORT)
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
                Log.e(TAG, "startFileSend: not able to create cache file", e)
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
                Toast.makeText(activity, "Can't find picture", Toast.LENGTH_SHORT).show()
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
                //TODO ici showTime ?
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

    override fun refuseFile(accountId: String, conversationUri: net.jami.model.Uri, transfer: DataTransfer) {
        if (transfer.messageId == null && transfer.fileId == null)
            return
        requireActivity().startService(Intent(DRingService.ACTION_FILE_CANCEL, ConversationPath.toUri(accountId, conversationUri),
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

    fun onClick() {
        presenter.clickOnGoingPane()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isVisible)
            return
        menu.clear()
        inflater.inflate(R.menu.conversation_actions, menu)
        mAudioCallBtn = menu.findItem(R.id.conv_action_audiocall)
        mVideoCallBtn = menu.findItem(R.id.conv_action_videocall)
        val searchMenuItem = menu.findItem(R.id.conv_search)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                val binding = binding ?: return false
                presenter.stopSearch()
                binding.histList.adapter = mAdapter
                updateListPadding()
                currentBottomView?.isVisible = true
                if (animation.isStarted) animation.cancel()
                animation.setIntValues(binding.histList.paddingBottom, (currentBottomView?.height ?: 0) + marginPxTotal)
                animation.start()
                return true
            }
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                val binding = binding ?: return false
                mSearchAdapter = ConversationAdapter(this@ConversationFragment, presenter, true)
                presenter.startSearch()
                currentBottomView?.isVisible = false
                binding.histList.adapter = mSearchAdapter
                if (animation.isStarted) animation.cancel()
                animation.setIntValues(binding.histList.paddingBottom, marginPxTotal)
                animation.start()
                return true
            }
        })
        (searchMenuItem.actionView as SearchView).let {
            it.setOnQueryTextListener(this)
            it.queryHint = getString(R.string.conversation_search_hint)
        }
    }

    fun openContact() {
        presenter.openContact()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startActivity(Intent(activity, HomeActivity::class.java))
            R.id.conv_action_audiocall -> presenter.goToCall(false)
            R.id.conv_action_videocall -> presenter.goToCall(true)
            R.id.conv_contact_details -> presenter.openContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String): Boolean {
        if (query.isNotBlank())
            presenter.setSearchQuery(query.trim())
        mSearchAdapter?.clearSearchResults()
        return true
    }

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
        if (path == null) return
        val uri = path.conversationUri
        mAdapter = ConversationAdapter(this, presenter)
        presenter.init(uri, path.accountId)

        // Load shared preferences. Usually useful for non-swarm conversations.
        try {
            mPreferences = getConversationPreferences(requireContext(), path.accountId, uri)
                .also { sharedPreferences ->
                    sharedPreferences.edit().remove(KEY_PREFERENCE_CONVERSATION_LAST_READ).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't load conversation preferences")
        }

        var connection = locationServiceConnection
        if (connection == null) {
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    Log.w(TAG, "onServiceConnected")
                    val binder = service as LocationSharingService.LocalBinder
                    val locationService = binder.service
                    //val path = ConversationPath(presenter.path)
                    if (locationService.isSharing(path)) {
                        showMap(path.accountId, uri.uri, false)
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
        mConversationAvatar = avatar
        mParticipantAvatars[conversation.uri.rawRingId] = AvatarDrawable(avatar)
        setupActionbar(conversation)
    }

    override fun displayOnGoingCallPane(display: Boolean) {
        binding!!.ongoingCallPane.visibility = if (display) View.VISIBLE else View.GONE
    }

    override fun displayRingingCallPane(display: Boolean, withCamera: Boolean) {
        binding!!.ringingCallPane.visibility =
            if (display) View.VISIBLE else View.GONE
        binding!!.acceptVideoCallButton.visibility =
            if (withCamera) View.VISIBLE else View.GONE
    }

    override fun displayNumberSpinner(conversation: Conversation, number: net.jami.model.Uri) {
        binding!!.numberSelector.visibility = View.VISIBLE
        //binding.numberSelector.setAdapter(new NumberAdapter(getActivity(), conversation.getContact(), false));
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
        if (activity is ConversationActivity) {
            requireActivity().finish()
        } else {
            // Post because we might be currently executing a fragment transaction
            view?.post { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
    }

    override fun goToAddContact(contact: Contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact), REQ_ADD_CONTACT)
    }


    override fun goToContactActivity(accountId: String, uri: net.jami.model.Uri) {
        val logo = binding!!.contactImage
        val intent = Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, uri))
            .setClass(requireContext().applicationContext, ContactDetailsActivity::class.java)
        startActivity(intent,
            ActivityOptions.makeSceneTransitionAnimation(activity, logo, "conversationIcon").toBundle())
    }

    override fun acceptAndGoToCallActivity(call: Call, withCamera: Boolean) {
        startActivity(
            Intent(DRingService.ACTION_CALL_ACCEPT)
                .setClass(requireContext().applicationContext, CallActivity::class.java)
                .putExtra(ConversationPath.KEY_ACCOUNT_ID, call.account)
                .putExtra(NotificationService.KEY_CALL_ID, call.daemonIdString)
                .putExtra(CallPresenter.KEY_ACCEPT_OPTION, CallPresenter.ACCEPT_HOLD)
                .putExtra(CallFragment.KEY_HAS_VIDEO, withCamera)
        )
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
                    .setClass(requireContext(), CallActivity::class.java)
                    .putExtra(NotificationService.KEY_CALL_ID, conf.id)
            )
        } else { // Otherwise, start a new call
            val intent = Intent(Intent.ACTION_CALL)
                .setClass(requireContext(), CallActivity::class.java)
                .putExtras(ConversationPath.toBundle(conversation))
                .putExtra(Intent.EXTRA_PHONE_NUMBER, contactUri.uri)
                .putExtra(CallFragment.KEY_HAS_VIDEO, hasVideo)
            startActivityForResult(intent, ContactDetailsActivity.REQUEST_CODE_CALL)
        }
    }

    private fun setupActionbar(conversation: ConversationItemViewModel) {
        val title = binding!!.contactTitle
        val subtitle = binding!!.contactSubtitle
        val logo = binding!!.contactImage
        logo.setImageDrawable(mConversationAvatar)
        logo.visibility = View.VISIBLE
        logo.setOnClickListener { openContact() }
        title.text = conversation.title
        title.textSize = 15f
       title.setTypeface(null, Typeface.NORMAL)
       if (conversation.uriTitle != conversation.title) {
            subtitle.text = conversation.uriTitle
            subtitle.visibility = View.VISIBLE
        } else {
            subtitle.text = ""
            subtitle.visibility = View.GONE
       }
    }

    fun blockContactRequest() {
        presenter.onBlockIncomingContactRequest()
    }

    fun refuseContactRequest() {
        presenter.onRefuseIncomingContactRequest()
    }

    fun acceptContactRequest() {
        presenter.onAcceptIncomingContactRequest()
    }

    fun addContact() {
        presenter.onAddContact()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val visible = binding!!.cvMessageInput.visibility == View.VISIBLE
        mAudioCallBtn?.isVisible = visible
        mVideoCallBtn?.isVisible = visible
    }

    override fun switchToUnknownView(name: String) {
        binding?.apply {
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.VISIBLE
            trustRequestPrompt.visibility = View.GONE
            tvTrustRequestMessage.text = getString(R.string.message_contact_not_trusted, name)
            trustRequestMessageLayout.visibility = View.VISIBLE
            currentBottomView = unknownContactPrompt
        }
        requireActivity().invalidateOptionsMenu()
        updateListPadding()
    }

    override fun switchToIncomingTrustRequestView(name: String) {
        binding?.apply {
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.VISIBLE
            tvTrustRequestMessage.text = getString(R.string.message_contact_not_trusted_yet, name)
            trustRequestMessageLayout.visibility = View.VISIBLE
            currentBottomView = trustRequestPrompt
        }
        requireActivity().invalidateOptionsMenu()
        updateListPadding()
    }

    override fun switchToConversationView() {
        binding?.apply {
            cvMessageInput.visibility = View.VISIBLE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.GONE
            currentBottomView = cvMessageInput
        }
        requireActivity().invalidateOptionsMenu()
        updateListPadding()
    }

    override fun switchToSyncingView() {
        binding?.apply {
            cvMessageInput.visibility = View.GONE
            unknownContactPrompt.visibility = View.GONE
            trustRequestPrompt.visibility = View.GONE
            trustRequestMessageLayout.visibility = View.VISIBLE
            tvTrustRequestMessage.text = getText(R.string.conversation_syncing)
        }
        currentBottomView = null
        requireActivity().invalidateOptionsMenu()
        updateListPadding()
    }

    override fun switchToBannedView() {
        binding?.apply {
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
        requireActivity().invalidateOptionsMenu()
        updateListPadding()
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
            val type = intent.type
            if (type == null) {
                Log.w(TAG, "Can't share with no type")
                return
            }
            if (type.startsWith("text/plain")) {
                binding!!.msgInputTxt.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            } else {
                val intentUri = intent.data
                if (intentUri != null)
                    startFileSend(AndroidFileUtils.getCacheFile(requireContext(), intentUri).flatMapCompletable { file -> sendFile(file) })
                intent.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (uri != intentUri)
                            startFileSend(AndroidFileUtils.getCacheFile(requireContext(), uri).flatMapCompletable { file -> sendFile(file) })
                    }
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
    }

    override fun setSettings(linkPreviews: Boolean) {
        mAdapter?.apply {
            showLinkPreviews = linkPreviews
        }
    }

    override fun hideErrorPanel() {
        binding?.errorMsgPane?.visibility = View.GONE
    }

    override fun goToSearchMessage(messageId: String) {
        binding?.toolbar?.menu?.findItem(R.id.conv_search)?.collapseActionView()
        binding?.histList?.doOnNextLayout {
            presenter.scrollToMessage(messageId)
        }
    }

    companion object {
        private val TAG = ConversationFragment::class.simpleName
        const val REQ_ADD_CONTACT = 42
        const val KEY_PREFERENCE_PENDING_MESSAGE = "pendingMessage"
        const val KEY_PREFERENCE_CONVERSATION_COLOR = "color"
        @Deprecated("Use daemon feature")
        const val KEY_PREFERENCE_CONVERSATION_LAST_READ = "lastRead"
        const val KEY_PREFERENCE_CONVERSATION_SYMBOL = "symbol"
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