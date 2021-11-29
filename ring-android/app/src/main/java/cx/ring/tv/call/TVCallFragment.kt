/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.util.Log
import android.util.Rational
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.percentlayout.widget.PercentFrameLayout
import cx.ring.R
import cx.ring.adapters.ConfParticipantAdapter
import cx.ring.adapters.ConfParticipantAdapter.ConfParticipantSelected
import cx.ring.client.CallActivity
import cx.ring.client.ContactDetailsActivity
import cx.ring.client.ConversationSelectionActivity
import cx.ring.databinding.ItemParticipantHandContainerBinding
import cx.ring.databinding.ItemParticipantLabelBinding
import cx.ring.databinding.TvFragCallBinding
import cx.ring.fragments.CallFragment
import cx.ring.fragments.ConversationFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.tv.main.HomeActivity
import cx.ring.utils.ActionHelper
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import net.jami.call.CallPresenter
import net.jami.call.CallView
import net.jami.daemon.JamiService
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Uri
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService.AudioState
import net.jami.services.NotificationService
import java.util.*
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class TVCallFragment : BaseSupportFragment<CallPresenter, CallView>(), CallView {
    private var binding: TvFragCallBinding? = null

    // Screen wake lock for incoming call
    private var runnable: Runnable? = null
    private var mPreviewWidth = 720
    private var mPreviewHeight = 1280
    private val mPreviewWidthRot = 720
    private val mPreviewHeightRot = 1280
    private var mScreenWakeLock: PowerManager.WakeLock? = null
    private var mBackstackLost = false
    private var mTextureAvailable = false
    private var confAdapter: ConfParticipantAdapter? = null
    private var mConferenceMode = false
    private var mVideoWidth = -1
    private var mVideoHeight = -1
    private val fadeOutAnimation: Animation by lazy { AlphaAnimation(1f, 0f).apply {
        interpolator = AccelerateInterpolator()
        startOffset = 1000
        duration = 1000
    }}
    private val blinkingAnimation: Animation by lazy { AlphaAnimation(1f, 0f).apply {
        duration = 400
        interpolator = LinearInterpolator()
        repeatCount = Animation.INFINITE
        repeatMode = Animation.REVERSE
    }}
    private var mSession: MediaSessionCompat? = null

    @Inject
    lateinit var mDeviceRuntimeService: DeviceRuntimeService

    override fun initPresenter(presenter: CallPresenter) {
        val args = requireArguments()
        args.getString(CallFragment.KEY_ACTION)?.let { action ->
            if (action == CallFragment.ACTION_PLACE_CALL || action == Intent.ACTION_CALL)
                prepareCall(false)
            else if (action == Intent.ACTION_VIEW || action == CallActivity.ACTION_CALL_ACCEPT)
                presenter.initIncomingCall(args.getString(NotificationService.KEY_CALL_ID)!!, action == Intent.ACTION_VIEW)
        }
    }

    override fun handleCallWakelock(isAudioOnly: Boolean) {}
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return TvFragCallBinding.inflate(inflater, container, false).also { b ->
            b.presenter = this
            binding = b
        }.root
    }

    private val listener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
            presenter.previewVideoSurfaceCreated(binding!!.previewSurface)
            mTextureAvailable = true
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            presenter.previewVideoSurfaceDestroyed()
            mTextureAvailable = false
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    override fun onStart() {
        super.onStart()
        mScreenWakeLock?.apply {
            if (!isHeld) acquire()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.w(TAG, "onViewCreated");
        mSession = MediaSessionCompat(requireContext(), TAG).apply {
            setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, getString(R.string.pip_title))
                .build())
        }
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        mScreenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "ring:callLock")
            .apply { setReferenceCounted(false) }
        binding!!.videoSurface.holder.setFormat(PixelFormat.RGBA_8888)
        binding!!.videoSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                presenter.videoSurfaceCreated(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                presenter.videoSurfaceDestroyed()
            }
        })
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> resetVideoSize(mVideoWidth, mVideoHeight) }
        binding!!.previewSurface.surfaceTextureListener = listener
        runnable = Runnable { presenter.uiVisibilityChanged(false) }
    }

    override fun onResume() {
        super.onResume()
        if (mTextureAvailable) presenter.previewVideoSurfaceCreated(binding!!.previewSurface)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mScreenWakeLock != null && mScreenWakeLock!!.isHeld) {
            mScreenWakeLock!!.release()
        }
        mScreenWakeLock = null
        if (mSession != null) {
            mSession!!.release()
            mSession = null
        }
        presenter.hangupCall()
        runnable = null
        binding = null
    }

    override fun onStop() {
        super.onStop()
        if (mScreenWakeLock != null && mScreenWakeLock!!.isHeld) {
            mScreenWakeLock!!.release()
        }
        runnable?.let { r ->
            view?.handler?.removeCallbacks(r)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSION_INCOMING && requestCode != REQUEST_PERMISSION_OUTGOING) return
        var i = 0
        val n = permissions.size
        while (i < n) {
            val audioGranted = mDeviceRuntimeService.hasAudioPermission()
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    presenter.cameraPermissionChanged(granted)
                    if (audioGranted) {
                        initializeCall(requestCode == REQUEST_PERMISSION_INCOMING)
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    presenter.audioPermissionChanged(granted)
                    initializeCall(requestCode == REQUEST_PERMISSION_INCOMING)
                }
            }
            i++
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val path = ConversationPath.fromUri(data.data)
                if (path != null) {
                    presenter.addConferenceParticipant(path.accountId, path.conversationUri)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        when(isInPictureInPictureMode){
            true -> {
                binding!!.previewContainer.visibility = View.GONE
                binding!!.videoSurface.setZOrderOnTop(false)
                binding!!.videoSurface.setZOrderMediaOverlay(false)
            }
            false -> {
                mBackstackLost = true
                binding!!.previewContainer.visibility = View.VISIBLE
                binding!!.videoSurface.setZOrderMediaOverlay(true)
                binding!!.videoSurface.setZOrderOnTop(true)
            }
        }
    }

    override fun displayContactBubble(display: Boolean) {
        binding!!.contactBubbleLayout.visibility = if (display) View.VISIBLE else View.GONE
    }
    override fun displayPeerVideo(display: Boolean) {
        binding!!.videoSurface.visibility = if (display) View.VISIBLE else View.GONE
        displayContactBubble(!display)
    }

    override fun displayLocalVideo(display: Boolean) {
        binding!!.previewContainer.visibility = if (display) View.VISIBLE else View.GONE
    }

    override fun displayHangupButton(display: Boolean) {
        binding?.apply {
            if (display) {
                callHangupBtn.visibility = View.VISIBLE
                callAddBtn.visibility = View.VISIBLE
            } else {
                callHangupBtn.startAnimation(fadeOutAnimation)
                callAddBtn.startAnimation(fadeOutAnimation)
                callHangupBtn.visibility = View.GONE
                callAddBtn.visibility = View.GONE
            }
        }
    }

    override fun displayDialPadKeyboard() {}
    override fun updateAudioState(state: AudioState) {}

    /*
        override fun updateMenu() {}
    */
    override fun updateTime(duration: Long) {
        binding?.callStatusTxt?.text = String.format(
            Locale.getDefault(),
            "%d:%02d:%02d",
            duration / 3600,
            duration % 3600 / 60,
            duration % 60
        )
    }

    override fun updateCallStatus(callStatus: CallStatus) {
        when (callStatus) {
            CallStatus.NONE -> binding!!.callStatusTxt.text = ""
            else -> binding!!.callStatusTxt.setText(CallFragment.callStateToHumanState(callStatus))
        }
    }

    override fun updateBottomSheetButtonStatus(
        isConference: Boolean,
        isSpeakerOn: Boolean,
        isMicrophoneMuted: Boolean,
        displayFlip: Boolean,
        canDial: Boolean,
        showPluginBtn: Boolean,
        onGoingCall: Boolean,
        hasActiveVideo: Boolean) {
    }

    override fun resetBottomSheetState() {}

    override fun initNormalStateDisplay() {
        mSession!!.isActive = true
        binding?.apply {
            callAcceptBtn.visibility = View.GONE
            callRefuseBtn.visibility = View.GONE
            callHangupBtn.visibility = View.VISIBLE
            contactBubbleLayout.visibility = View.VISIBLE
        }
        requireActivity().invalidateOptionsMenu()
        handleVisibilityTimer()
    }


    override fun initIncomingCallDisplay(hasVideo: Boolean) {
        mSession!!.isActive = true
        binding?.apply {
            callAcceptBtn.visibility = View.VISIBLE
            callAcceptBtn.requestFocus()
            callRefuseBtn.visibility = View.VISIBLE
            callHangupBtn.visibility = View.GONE
        }
    }

    override fun initOutGoingCallDisplay() {
        binding?.apply {
            callAcceptBtn.visibility = View.GONE
            callRefuseBtn.visibility = View.VISIBLE
            callHangupBtn.visibility = View.GONE
        }
    }

    override fun resetPreviewVideoSize(previewWidth: Int, previewHeight: Int, rot: Int) {
        if (previewWidth == -1 && previewHeight == -1) return
        mPreviewWidth = previewWidth
        mPreviewHeight = previewHeight
        val flip = rot % 180 != 0
        binding!!.previewSurface.setAspectRatio(
            if (flip) mPreviewHeight else mPreviewWidth,
            if (flip) mPreviewWidth else mPreviewHeight
        )
    }

    override fun resetVideoSize(videoWidth: Int, videoHeight: Int) {
        Log.w(TAG, "resetVideoSize " + videoWidth + "x" + videoHeight)
        val rootView = view as ViewGroup? ?: return
        val videoRatio = videoWidth / videoHeight.toDouble()
        val screenRatio = rootView.width / rootView.height.toDouble()
        val params = binding!!.videoSurface.layoutParams as RelativeLayout.LayoutParams
        val oldW = params.width
        val oldH = params.height
        if (videoRatio >= screenRatio) {
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT
            params.height = (videoHeight * rootView.width.toDouble() / videoWidth.toDouble()).toInt()
        } else {
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT
            params.width = (videoWidth * rootView.height.toDouble() / videoHeight.toDouble()).toInt()
        }
        if (oldW != params.width || oldH != params.height) {
            binding!!.videoSurface.layoutParams = params
        }
        mVideoWidth = videoWidth
        mVideoHeight = videoHeight
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity: Activity? = activity
        if (null == binding || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val rot = Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation
        Log.w(TAG, "configureTransform " + viewWidth + "x" + viewHeight + " rot=" + rot + " mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight)
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (rot) {
            val bufferRect = RectF(0f, 0f, mPreviewHeightRot.toFloat(), mPreviewWidthRot.toFloat())
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = max(viewHeight.toFloat() / mPreviewHeightRot, viewWidth.toFloat() / mPreviewWidthRot)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        binding!!.previewSurface.setTransform(matrix)
    }

    /**
     * Checks if permissions are accepted for camera and microphone. Takes into account whether call is incoming and outgoing, and requests permissions if not available.
     * Initializes the call if permissions are accepted.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     * @see .initializeCall
     */
    override fun prepareCall(isIncoming: Boolean) {
        val audioGranted = mDeviceRuntimeService.hasAudioPermission()
        val hasVideo: Boolean
        val permissionType: Int
        if (isIncoming) {
            hasVideo = presenter.wantVideo
            permissionType = REQUEST_PERMISSION_INCOMING
        } else {
            hasVideo = requireArguments().getBoolean(CallFragment.KEY_HAS_VIDEO)
            permissionType = REQUEST_PERMISSION_OUTGOING
        }
        if (!hasVideo) {
            val videoGranted = mDeviceRuntimeService.hasVideoPermission()
            if ((!audioGranted || !videoGranted) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val perms = ArrayList<String>()
                if (!videoGranted) {
                    perms.add(Manifest.permission.CAMERA)
                }
                if (!audioGranted) {
                    perms.add(Manifest.permission.RECORD_AUDIO)
                }
                requestPermissions(perms.toTypedArray(), permissionType)
            } else if (audioGranted && videoGranted) {
                initializeCall(isIncoming)
            }
        } else {
            if (!audioGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionType)
            } else if (audioGranted) {
                initializeCall(isIncoming)
            }
        }
    }

    /**
     * Starts a call. Takes into account whether call is incoming or outgoing.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     */
    private fun initializeCall(isIncoming: Boolean) {
        val hasVideo : Boolean = presenter.wantVideo
        if (isIncoming) {
            presenter.acceptCall(hasVideo)
        } else {
            arguments?.let { args ->
                val conversation = ConversationPath.fromBundle(args)!!
                presenter.initOutGoing(
                    conversation.accountId,
                    conversation.conversationUri,
                    args.getString(Intent.EXTRA_PHONE_NUMBER),
                    args.getBoolean(CallFragment.KEY_HAS_VIDEO)
                )
            }
        }
    }

    override fun goToContact(accountId: String, contact: Contact) {
        startActivity(Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contact.uri), requireContext(), ContactDetailsActivity::class.java))
    }

    override fun displayPluginsButton(): Boolean {
        return false
    }

    @SuppressLint("RestrictedApi")
    override fun updateConfInfo(participantInfo: List<ParticipantInfo>) {
        val binding = binding ?: return
        mConferenceMode = participantInfo.size > 1
        if (participantInfo.isNotEmpty()) {
            val username = if (participantInfo.size > 1)
                "Conference with ${participantInfo.size} people"
            else participantInfo[0].contact.displayName
            val displayName = if (participantInfo.size > 1) null else participantInfo[0].contact.displayName
            val hasProfileName = displayName != null && !displayName.contentEquals(username)
            val call = participantInfo[0].call
            if (call != null) {
                val conversationUri = if (call.conversationId != null)
                    Uri(Uri.SWARM_SCHEME, call.conversationId!!)
                else call.contact!!.conversationUri.blockingFirst()
                activity?.let { activity ->
                    activity.intent = Intent(Intent.ACTION_VIEW,
                        ConversationPath.toUri(call.account!!, conversationUri), context, TVCallActivity::class.java)
                        .apply { putExtra(NotificationService.KEY_CALL_ID, call.confId ?: call.daemonIdString) }
                }
                arguments = Bundle().apply {
                    putString(CallFragment.KEY_ACTION, Intent.ACTION_VIEW)
                    putString(NotificationService.KEY_CALL_ID, call.confId ?: call.daemonIdString)
                }
            }
            if (hasProfileName) {
                binding.contactBubbleNumTxt.visibility = View.VISIBLE
                binding.contactBubbleTxt.text = displayName
                binding.contactBubbleNumTxt.text = username
            } else {
                binding.contactBubbleNumTxt.visibility = View.GONE
                binding.contactBubbleTxt.text = username
            }
            binding.contactBubble.setImageDrawable(AvatarDrawable.Builder()
                .withContact(participantInfo[0].contact)
                .withCircleCrop(true)
                .withPresence(false)
                .build(requireActivity()))

            generateParticipantOverlay(participantInfo)
        }
    }

    private fun generateParticipantOverlay(participantsInfo: List<ParticipantInfo>){
        val overlayViewBinding =  binding?.participantOverlayContainer ?: return
        overlayViewBinding.removeAllViews()
        overlayViewBinding.visibility = if (participantsInfo.isEmpty()) View.GONE else View.VISIBLE
        val inflater = LayoutInflater.from(overlayViewBinding.context)
        for (i in participantsInfo) {

            // adding name, mic etc..
            val displayName = i.contact.displayName
            if (!TextUtils.isEmpty(displayName)) {
                val participantInfoOverlay = ItemParticipantLabelBinding.inflate(inflater)
                val infoOverlayLayoutParams = PercentFrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                infoOverlayLayoutParams.percentLayoutInfo.startMarginPercent = i.x / mVideoWidth.toFloat()
                infoOverlayLayoutParams.percentLayoutInfo.endMarginPercent = 1f - (i.x + i.w) / mVideoWidth.toFloat()
                infoOverlayLayoutParams.percentLayoutInfo.topMarginPercent = i.y / mVideoHeight.toFloat()
                infoOverlayLayoutParams.percentLayoutInfo.bottomMarginPercent =  1f - (i.y + i.h) / mVideoHeight.toFloat()

                participantInfoOverlay.participantName.text = displayName
                //label.moderator.isVisible = i.isModerator
                participantInfoOverlay.mute.isVisible = i.audioModeratorMuted || i.audioLocalMuted
                overlayViewBinding.addView(participantInfoOverlay.root, infoOverlayLayoutParams)
            }

            val raisedHandBadge = ItemParticipantHandContainerBinding.inflate(inflater)
            val raisedHandBadgeLayoutParam = PercentFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            raisedHandBadgeLayoutParam.percentLayoutInfo.startMarginPercent = i.x / mVideoWidth.toFloat()
            raisedHandBadgeLayoutParam.percentLayoutInfo.endMarginPercent = 1f - (i.x + i.w) / mVideoWidth.toFloat()
            raisedHandBadgeLayoutParam.percentLayoutInfo.topMarginPercent = i.y / mVideoHeight.toFloat()

            raisedHandBadge.raisedHand.isVisible = i.isHandRaised
            overlayViewBinding.addView(raisedHandBadge.root, raisedHandBadgeLayoutParam)

        }
    }

    override fun updateParticipantRecording(contacts: List<ContactViewModel>) {
        binding?.let { binding ->
            if (contacts.isEmpty()) {
                binding.recordLayout.visibility = View.INVISIBLE
                binding.recordIndicator.clearAnimation()
                return
            }
            val names = StringBuilder()
            val contact = contacts.iterator()
            for (i in contacts.indices) {
                names.append(" ").append(contact.next().displayName)
                if (i != contacts.size - 1) {
                    names.append(",")
                }
            }
            binding.recordLayout.visibility = View.VISIBLE
            binding.recordIndicator.animation = blinkingAnimation
            binding.recordName.text = getString(R.string.remote_recording, names)
        }
    }

    override fun toggleCallMediaHandler(id: String, callId: String, toggle: Boolean) {
        JamiService.toggleCallMediaHandler(id, callId, toggle)
    }

    override fun goToConversation(accountId: String, conversationId: Uri) {}
    override fun goToAddContact(contact: Contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact), ConversationFragment.REQ_ADD_CONTACT)
    }

    override fun startAddParticipant(conferenceId: String) {
        startActivityForResult(Intent(Intent.ACTION_PICK)
                .setClass(requireActivity(), ConversationSelectionActivity::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, conferenceId),
            REQUEST_CODE_ADD_PARTICIPANT)
    }

    fun addParticipant() {
        presenter.startAddParticipant()
    }

    fun hangUpClicked() {
        presenter.hangupCall()
    }

    fun refuseClicked() {
        presenter.refuseCall()
    }

    fun acceptClicked() {
        presenter.wantVideo = true
        prepareCall(true)
    }

    override fun finish() {
        mSession?.isActive = false
        activity?.let { activity ->
            if (mBackstackLost) {
                activity.finishAndRemoveTask()
                startActivity(Intent.makeMainActivity(ComponentName(activity, HomeActivity::class.java))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                activity.finish()
            }
        }
    }

    override fun onUserLeave() {
        presenter.requestPipMode()
    }

    override fun enterPipMode(callId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val paramBuilder = PictureInPictureParams.Builder()
            if (binding!!.videoSurface.visibility == View.VISIBLE) {
                val l = IntArray(2)
                binding!!.videoSurface.getLocationInWindow(l)
                val x = l[0]
                val y = l[1]
                val w = binding!!.videoSurface.width
                val h = binding!!.videoSurface.height
                val videoBounds = Rect(x, y, x + w, y + h)
                paramBuilder.setAspectRatio(Rational(w, h))
                paramBuilder.setSourceRectHint(videoBounds)
            }
            requireActivity().enterPictureInPictureMode(paramBuilder.build())
        } else {
            requireActivity().enterPictureInPictureMode()
        }
    }

    fun onKeyDown() {
        handleVisibilityTimer()
    }

    private fun handleVisibilityTimer() {
        presenter.uiVisibilityChanged(true)
        val view = view
        val r = runnable
        if (view != null && r != null) {
            val handler = view.handler
            if (handler != null) {
                handler.removeCallbacks(r)
                handler.postDelayed(r, 5000)
            }
        }
    }

    companion object {
        private val TAG = TVCallFragment::class.simpleName!!
        private const val REQUEST_CODE_ADD_PARTICIPANT = 6
        private const val REQUEST_PERMISSION_INCOMING = 1003
        private const val REQUEST_PERMISSION_OUTGOING = 1004

        fun newInstance(action: String, accountId: String, conversationId: String, contactUri: String, hasVideo: Boolean): TVCallFragment {
            return TVCallFragment().apply { arguments = Bundle().apply {
                putString(CallFragment.KEY_ACTION, action)
                putAll(ConversationPath.toBundle(accountId, conversationId))
                putString(Intent.EXTRA_PHONE_NUMBER, contactUri)
                putBoolean(CallFragment.KEY_HAS_VIDEO, hasVideo)
            }}
        }

        fun newInstance(action: String, confId: String?): TVCallFragment {
            return TVCallFragment().apply { arguments = Bundle().apply {
                putString(CallFragment.KEY_ACTION, action)
                putString(NotificationService.KEY_CALL_ID, confId)
            }}
        }
    }
}