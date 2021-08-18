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
import androidx.percentlayout.widget.PercentFrameLayout
import com.rodolfonavalon.shaperipplelibrary.model.Circle
import cx.ring.R
import cx.ring.adapters.ConfParticipantAdapter
import cx.ring.adapters.ConfParticipantAdapter.ConfParticipantSelected
import cx.ring.client.CallActivity
import cx.ring.client.ContactDetailsActivity
import cx.ring.client.ConversationSelectionActivity
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
import net.jami.model.Uri
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService.AudioState
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
            else if (action == CallFragment.ACTION_GET_CALL || action == CallActivity.ACTION_CALL_ACCEPT)
                presenter.initIncomingCall(args.getString(CallFragment.KEY_CONF_ID)!!, action == CallFragment.ACTION_GET_CALL)
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
        binding!!.shapeRipple.rippleShape = Circle()
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
        val r = runnable
        if (r != null) {
            view?.handler?.removeCallbacks(r)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
        if (!isInPictureInPictureMode) {
            mBackstackLost = true
        }
        presenter.pipModeChanged(isInPictureInPictureMode)
    }

    override fun displayContactBubble(display: Boolean) {
        binding!!.contactBubbleLayout.visibility = if (display) View.VISIBLE else View.GONE
    }

    override fun displayVideoSurface(
        displayVideoSurface: Boolean,
        displayPreviewContainer: Boolean
    ) {
        binding!!.videoSurface.visibility =
            if (displayVideoSurface) View.VISIBLE else View.GONE
        binding!!.previewContainer.visibility =
            if (displayPreviewContainer) View.VISIBLE else View.GONE
    }

    override fun displayPreviewSurface(display: Boolean) {
        if (display) {
            binding!!.videoSurface.setZOrderOnTop(false)
            //mVideoPreview.setZOrderMediaOverlay(true);
            binding!!.videoSurface.setZOrderMediaOverlay(false)
        } else {
            binding!!.videoSurface.setZOrderMediaOverlay(true)
            binding!!.videoSurface.setZOrderOnTop(true)
        }
    }

    override fun displayHangupButton(display: Boolean) {
        binding?.apply {
            confControlGroup!!.visibility = if (mConferenceMode && display) View.VISIBLE else View.GONE
            if (display) {
                callHangupBtn.visibility = View.VISIBLE
                callAddBtn.visibility = View.VISIBLE
            } else {
                callHangupBtn.startAnimation(fadeOutAnimation)
                callAddBtn.startAnimation(fadeOutAnimation)
                callHangupBtn.visibility = View.GONE
                callAddBtn.visibility = View.GONE
            }
            if (mConferenceMode && display) {
                confControlGroup.visibility = View.VISIBLE
            } else {
                confControlGroup.startAnimation(fadeOutAnimation)
            }
        }
    }

    override fun displayDialPadKeyboard() {}
    override fun switchCameraIcon(isFront: Boolean) {}
    override fun updateAudioState(state: AudioState) {}
    override fun updateMenu() {}
    override fun updateTime(duration: Long) {
        binding?.callStatusTxt?.text = String.format(
            Locale.getDefault(),
            "%d:%02d:%02d",
            duration / 3600,
            duration % 3600 / 60,
            duration % 60
        )
    }

    override fun updateContactBubble(calls: List<Call>) {
        mConferenceMode = calls.size > 1
        val contact = calls[0].contact!!
        val username = if (mConferenceMode) "Conference with " + calls.size + " people" else contact.ringUsername
        val displayName = if (mConferenceMode) null else contact.displayName
        Log.d(TAG, "updateContactBubble: username=" + username + ", uri=" + contact.uri + " photo:" + contact.photo)
        mSession?.setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayName)
                .build())
        val hasProfileName = displayName != null && !displayName.contentEquals(username)
        if (hasProfileName) {
            binding!!.contactBubbleNumTxt.visibility = View.VISIBLE
            binding!!.contactBubbleTxt.text = displayName
            binding!!.contactBubbleNumTxt.text = username
        } else {
            binding!!.contactBubbleNumTxt.visibility = View.GONE
            binding!!.contactBubbleTxt.text = username
        }
        binding!!.contactBubble.setImageDrawable(
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .build(requireActivity())
        )

        /*if (!mConferenceMode) {
            binding.confControlGroup.setVisibility(View.GONE);
        } else {
            binding.confControlGroup.setVisibility(View.VISIBLE);
            if (confAdapter  == null) {
                confAdapter = new ConfParticipantAdapter((view, call) -> {
                    Context context = requireContext();
                    PopupMenu popup = new PopupMenu(context, view);
                    popup.inflate(R.menu.conference_participant_actions);
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.conv_contact_details) {
                            presenter.openParticipantContact(call);
                        } else if (itemId == R.id.conv_contact_hangup) {
                            presenter.hangupParticipant(call);
                        } else {
                            return false;
                        }
                        return true;
                    });
                    MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popup.getMenu(), view);
                    menuHelper.setForceShowIcon(true);
                    menuHelper.show();
                });
            }
            confAdapter.updateFromCalls(calls);
            if (binding.confControlGroup.getAdapter() == null)
                binding.confControlGroup.setAdapter(confAdapter);
        }*/
    }

    override fun updateCallStatus(callStatus: CallStatus) {
        when (callStatus) {
            CallStatus.NONE -> binding!!.callStatusTxt.text = ""
            else -> binding!!.callStatusTxt.setText(CallFragment.callStateToHumanState(callStatus))
        }
    }

    override fun initMenu(
        isSpeakerOn: Boolean, displayFlip: Boolean, canDial: Boolean,
        showPluginBtn: Boolean, onGoingCall: Boolean
    ) {
    }

    override fun initNormalStateDisplay(audioOnly: Boolean, muted: Boolean) {
        mSession!!.isActive = true
        binding?.apply {
            shapeRipple.stopRipple()
            callAcceptBtn.visibility = View.GONE
            callRefuseBtn.visibility = View.GONE
            callHangupBtn.visibility = View.VISIBLE
            contactBubbleLayout.visibility = if (audioOnly) View.VISIBLE else View.INVISIBLE
        }
        requireActivity().invalidateOptionsMenu()
        handleVisibilityTimer()
    }

    override fun initIncomingCallDisplay() {
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

    override fun resetPluginPreviewVideoSize(previewWidth: Int, previewHeight: Int, rot: Int) {}

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
        val audioOnly: Boolean
        val permissionType: Int
        if (isIncoming) {
            audioOnly = presenter.isAudioOnly
            permissionType = REQUEST_PERMISSION_INCOMING
        } else {
            audioOnly = requireArguments().getBoolean(CallFragment.KEY_AUDIO_ONLY)
            permissionType = REQUEST_PERMISSION_OUTGOING
        }
        if (!audioOnly) {
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
        Log.w(TAG, "initializeCall $isIncoming")
        if (isIncoming) {
            presenter.acceptCall()
        } else {
            arguments?.let { args ->
                Log.w(TAG, "initializeCall presenter.initOutGoing")
                val conversation = ConversationPath.fromBundle(args)!!
                presenter.initOutGoing(
                    conversation.accountId,
                    conversation.conversationUri,
                    args.getString(Intent.EXTRA_PHONE_NUMBER),
                    args.getBoolean(CallFragment.KEY_AUDIO_ONLY)
                )
            }
        }
    }

    override fun goToContact(accountId: String, contact: Contact) {
        startActivity(Intent(Intent.ACTION_VIEW,
            android.net.Uri.withAppendedPath(
                android.net.Uri.withAppendedPath(
                    ContentUriHandler.CONTACT_CONTENT_URI,
                    accountId
                ), contact.primaryNumber))
            .setClass(requireContext(), ContactDetailsActivity::class.java)
        )
    }

    override fun displayPluginsButton(): Boolean {
        return false
    }

    override fun updateConfInfo(info: List<ParticipantInfo>) {
        val binding = binding!!
        binding.participantLabelContainer.removeAllViews()
        if (info.isNotEmpty()) {
            val inflater = LayoutInflater.from(binding.participantLabelContainer.context)
            for (i in info) {
                val displayName = i.contact.displayName
                if (!TextUtils.isEmpty(displayName)) {
                    val label = ItemParticipantLabelBinding.inflate(inflater)
                    val params = PercentFrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    params.percentLayoutInfo.leftMarginPercent = i.x / mVideoWidth.toFloat()
                    params.percentLayoutInfo.topMarginPercent = i.y / mVideoHeight.toFloat()
                    label.participantName.text = displayName
                    binding.participantLabelContainer.addView(label.root, params)
                }
            }
        }
        binding.participantLabelContainer.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE
        if (!mConferenceMode) {
            binding.confControlGroup!!.visibility = View.GONE
        } else {
            binding.confControlGroup!!.visibility = View.VISIBLE
            if (confAdapter == null) {
                confAdapter = ConfParticipantAdapter(object : ConfParticipantSelected {
                    override fun onParticipantSelected(view: View, contact: ParticipantInfo) {
                        val context = requireContext()
                        val popup = PopupMenu(context, view)
                        popup.inflate(R.menu.conference_participant_actions)
                        popup.setOnMenuItemClickListener { item: MenuItem ->
                            when (item.itemId) {
                                R.id.conv_contact_details -> presenter.openParticipantContact(contact)
                                R.id.conv_contact_hangup -> presenter.hangupParticipant(contact)
                                else -> return@setOnMenuItemClickListener false
                            }
                            true
                        }
                        val menuHelper = MenuPopupHelper(context, (popup.menu as MenuBuilder), view)
                        menuHelper.setForceShowIcon(true)
                        menuHelper.show()
                    }
                })
            }
            confAdapter!!.updateFromCalls(info)
            if (binding.confControlGroup.adapter == null)
                binding.confControlGroup.adapter = confAdapter
        }
    }

    override fun updateParticipantRecording(contacts: Set<Contact>) {
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
                .putExtra(CallFragment.KEY_CONF_ID, conferenceId),
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

        fun newInstance(action: String, accountId: String, conversationId: String, contactUri: String, audioOnly: Boolean): TVCallFragment {
            return TVCallFragment().apply { arguments = Bundle().apply {
                putString(CallFragment.KEY_ACTION, action)
                putAll(ConversationPath.toBundle(accountId, conversationId))
                putString(Intent.EXTRA_PHONE_NUMBER, contactUri)
                putBoolean(CallFragment.KEY_AUDIO_ONLY, audioOnly)
            }}
        }

        fun newInstance(action: String, confId: String?): TVCallFragment {
            return TVCallFragment().apply { arguments = Bundle().apply {
                putString(CallFragment.KEY_ACTION, action)
                putString(CallFragment.KEY_CONF_ID, confId)
            }}
        }
    }
}