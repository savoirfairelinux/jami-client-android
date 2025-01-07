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
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Icon
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Rational
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cx.ring.R
import cx.ring.adapters.ConfParticipantAdapter
import cx.ring.adapters.ConfParticipantAdapter.ConfParticipantSelected
import cx.ring.adapters.ExtensionsAdapter
import cx.ring.client.*
import cx.ring.databinding.FragCallBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.extensions.ExtensionUtils
import cx.ring.service.DRingService
import cx.ring.settings.extensionssettings.ExtensionDetails
import cx.ring.utils.ActionHelper
import cx.ring.utils.ContentUri
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils.isTablet
import cx.ring.utils.DeviceUtils.isTv
import cx.ring.utils.MediaButtonsHelper.MediaButtonsHelperCallback
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.call.CallPresenter
import net.jami.call.CallView
import net.jami.daemon.JamiService
import net.jami.model.Call.CallStatus
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Contact
import net.jami.model.ContactViewModel
import net.jami.model.Uri
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.HardwareService.AudioState
import net.jami.services.NotificationService
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class CallFragment : BaseSupportFragment<CallPresenter, CallView>(), CallView,
    MediaButtonsHelperCallback {
    private var binding: FragCallBinding? = null
    private var mOrientationListener: OrientationEventListener? = null
    private var mScreenWakeLock: PowerManager.WakeLock? = null
    private var mCurrentOrientation = 0
    private var mPreviewWidth = 720
    private var mPreviewHeight = 1280
    private var mPreviewSurfaceWidth = 0
    private var mPreviewSurfaceHeight = 0
    private var isInPIP = false
    private lateinit var mProjectionManager: MediaProjectionManager
    private var mBackstackLost = false
    private var confAdapter: ConfParticipantAdapter? = null
    private var mConferenceMode = false
    var isChooseExtensionMode = false
        private set
    private var callMediaHandlers: List<ExtensionDetails> ?= null
    private val animation = ValueAnimator().apply { duration = 150 }
    private var previewDrag: PointF? = null
    private val previewSnapAnimation = ValueAnimator().apply {
        duration = 250
        setFloatValues(0f, 1f)
        interpolator = DecelerateInterpolator()
        addUpdateListener { a -> configurePreview(mPreviewSurfaceWidth, a.animatedFraction) }
    }
    private var previewMargin: Float = 0f
    private val previewMargins = IntArray(4)
    private var previewHiddenState = 0f
    private enum class PreviewPosition { LEFT, RIGHT }
    private var previewPosition = PreviewPosition.LEFT
    @Inject
    lateinit var mDeviceRuntimeService: DeviceRuntimeService
    private val mCompositeDisposable = CompositeDisposable()
    private var bottomSheetParams: BottomSheetBehavior<View>? = null
    private var extensionsAdapter: ExtensionsAdapter? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            switchCamera()
        }
    }

    override fun initPresenter(presenter: CallPresenter) {
        val args = requireArguments()
        presenter.wantVideo = args.getBoolean(KEY_HAS_VIDEO, false)
        args.getString(KEY_ACTION)?.let { action ->
            if (action == Intent.ACTION_CALL) {
                prepareCall(false)
            } else if (action == Intent.ACTION_VIEW || action == DRingService.ACTION_CALL_ACCEPT) {
                val option = if (action == DRingService.ACTION_CALL_ACCEPT) args.getString(CallPresenter.KEY_ACCEPT_OPTION) else null
                presenter.handleOption(option)
                presenter.initIncomingCall(args.getString(NotificationService.KEY_CALL_ID)!!, action == Intent.ACTION_VIEW)
            }
        }
    }

    fun handleAcceptIntent(option: String?, callId: String?, wantVideo: Boolean) {
        presenter.handleOption(option)
        if (wantVideo)
            acceptClicked()
        else
            acceptAudioClicked()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        previewMargin = inflater.context.resources.getDimension(R.dimen.call_preview_margin)
        return FragCallBinding.inflate(inflater, container, false)
            .also { b ->
                binding = b
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    //b.participantOverlayContainer.list
                    b.participantOverlayContainer.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        Log.w(TAG, "OnLayoutChange ${v}, ${left}, ${top}, ${right}, $bottom")
                        updatePipParams()
                    }
                }
                b.callAcceptBtn.setOnClickListener { acceptClicked() }
                b.callAcceptAudioBtn.setOnClickListener { acceptAudioClicked() }
                b.callRefuseBtn.setOnClickListener { refuseClicked() }
                b.callHngUpBtn.setOnClickListener { hangupClicked() }
                b.callSpeakerBtn.setOnClickListener { speakerClicked() }
                b.callMicBtn.setOnClickListener { micClicked() }
                b.callVideocamBtn.setOnClickListener { switchCamera() }
                b.callSharescreenBtn.setOnClickListener { shareScreenClicked() }
                b.addParticipantBtn.setOnClickListener { addParticipantClicked() }
                b.callDialpadBtn.setOnClickListener { displayDialPadKeyboard() }
                b.callRaiseHandBtn.setOnClickListener { raiseHandClicked() }
                b.callExtensionsBtn.setOnClickListener { extensionsButtonClicked() }
                b.callCameraFlipBtn.setOnClickListener { cameraFlip() }
                bottomSheetParams = binding?.callOptionsBottomSheet?.let { BottomSheetBehavior.from(it) }
            }.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun updatePipParams() {
        binding?.let { b ->
            try {
                val bounds = b.participantOverlayContainer.getMainLocationInWindow()
                if (!bounds.isEmpty) {
                    activity?.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(true)
                            .setSeamlessResizeEnabled(true)
                            .setAspectRatio(Rational(bounds.width(), bounds.height()))
                            .setSourceRectHint(bounds)
                            .build())
                } else {
                    activity?.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(false)
                            .build())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Can't set PIP params", e)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "RtlHardcoded", "WakelockTimeout")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(false)
        super.onViewCreated(view, savedInstanceState)

        val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mCurrentOrientation = windowManager.defaultDisplay.rotation
        val dpRatio = requireActivity().resources.displayMetrics.density
        val previewContainerMargin = resources.getDimensionPixelSize(R.dimen.call_preview_margin)
        animation.addUpdateListener { valueAnimator ->
            binding?.let { binding ->
                val upBy = valueAnimator.animatedValue as Int
                val layoutParams = binding.previewContainer.layoutParams as RelativeLayout.LayoutParams
                layoutParams.setMargins(previewContainerMargin, previewContainerMargin, previewContainerMargin, (upBy * dpRatio).toInt())
                binding.previewContainer.layoutParams = layoutParams
            }
        }

        mProjectionManager =
            requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        mScreenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "ring:callLock"
        ).apply {
            setReferenceCounted(false)
            if (!isHeld)
                acquire()
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            setBottomSheet(insets)
            insets
        }

        binding?.let { binding ->
            binding.participantOverlayContainer.layoutTransition?.apply {
                disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
                disableTransitionType(LayoutTransition.CHANGE_APPEARING)
            }

            binding.extensionPreviewSurface.holder.setFormat(PixelFormat.RGBX_8888)
            binding.extensionPreviewSurface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    presenter.extensionSurfaceCreated(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    presenter.extensionSurfaceDestroyed()
                }
            })

            val insets = ViewCompat.getRootWindowInsets(view)
            insets?.apply {
                presenter.uiVisibilityChanged(this.isVisible(WindowInsetsCompat.Type.navigationBars()))
            }

            // todo: doublon with CallActivity.onConfigurationChanged ??
            mOrientationListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    val rot = windowManager.defaultDisplay.rotation
                    if (mCurrentOrientation != rot) {
                        mCurrentOrientation = rot
                        presenter.configurationChanged(rot)
                        if (rot == 0 || rot == 2) resetPreviewVideoSize( null, null, 90) else resetPreviewVideoSize( null, null, 180)
                    }
                }
            }.apply { if (canDetectOrientation()) enable() }

            binding.callSpeakerBtn.isChecked = presenter.isSpeakerphoneOn()
            binding.callMicBtn.isChecked = presenter.isMicrophoneMuted

            binding.extensionPreviewSurface.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight)
            }
            binding.extensionPreviewContainer.setOnTouchListener { v: View, event: MotionEvent ->
                val action = event.actionMasked
                val parent = v.parent as RelativeLayout
                val params = v.layoutParams as RelativeLayout.LayoutParams

                return@setOnTouchListener when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        previewSnapAnimation.cancel()
                        previewDrag = PointF(event.x, event.y)
                        v.elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation_dragged)
                        params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        params.setMargins(
                            v.x.toInt(), v.y.toInt(),
                            parent.width - (v.x.toInt() + v.width),
                            parent.height - (v.y.toInt() + v.height)
                        )
                        v.layoutParams = params
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (previewDrag != null) {
                            val currentXPosition = params.leftMargin + (event.x - previewDrag!!.x).toInt()
                            val currentYPosition = params.topMargin + (event.y - previewDrag!!.y).toInt()
                            params.setMargins(
                                currentXPosition, currentYPosition,
                                -(currentXPosition + v.width - event.x.toInt()),
                                -(currentYPosition + v.height - event.y.toInt())
                            )
                            v.layoutParams = params
                            val outPosition = binding.extensionPreviewContainer.width * 0.85f
                            var drapOut = 0f
                            if (currentXPosition < 0) {
                                drapOut = min(1f, -currentXPosition / outPosition)
                            } else if (currentXPosition + v.width > parent.width) {
                                drapOut = min(1f, (currentXPosition + v.width - parent.width) / outPosition)
                            }
                            setPreviewDragHiddenState(drapOut)
                            true
                        } else false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (previewDrag != null) {
                            val currentXPosition = params.leftMargin + (event.x - previewDrag!!.x).toInt()
                            previewSnapAnimation.cancel()
                            previewDrag = null
                            v.elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation)
                            var ml = 0;
                            var mr = 0;
                            var mt = 0;
                            var mb = 0
                            val hp = binding.extensionPreviewHandle.layoutParams as FrameLayout.LayoutParams
                            if (params.leftMargin + v.width / 2 > parent.width / 2) {
                                params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
                                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                                mr = (parent.width - v.width - v.x).toInt()
                                previewPosition = PreviewPosition.RIGHT
                                hp.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                            } else {
                                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                                ml = v.x.toInt()
                                previewPosition = PreviewPosition.LEFT
                                hp.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                            }
                            binding.extensionPreviewHandle.layoutParams = hp
                            if (params.topMargin + v.height / 2 > parent.height / 2) {
                                params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                                mb = (parent.height - v.height - v.y).toInt()
                            } else {
                                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                                mt = v.y.toInt()
                            }
                            previewMargins[0] = ml
                            previewMargins[1] = mt
                            previewMargins[2] = mr
                            previewMargins[3] = mb
                            params.setMargins(ml, mt, mr, mb)
                            v.layoutParams = params
                            val outPosition = binding.extensionPreviewContainer.width * 0.85f
                            previewHiddenState = when {
                                currentXPosition < 0 -> min(1f, -currentXPosition / outPosition)
                                currentXPosition + v.width > parent.width -> min(
                                    1f,
                                    (currentXPosition + v.width - parent.width) / outPosition
                                )
                                else -> 0f
                            }
                            setPreviewDragHiddenState(previewHiddenState)
                            previewSnapAnimation.start()
                            true
                        } else false
                    }
                    else -> false
                }
            }

            binding.previewSurface.surfaceTextureListener = listener
            binding.previewSurface.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight)
            }
            binding.previewContainer.setOnTouchListener(previewTouchListener)

            binding.dialpadEditText.addTextChangedListener(object : TextWatcher {
                  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                      if (before == 0)
                        presenter.sendDtmf(s.subSequence(start, start + count))
                  }
                  override fun afterTextChanged(s: Editable) {
                      if (s.isNotEmpty())
                        s.clear()
                  }
              })

        }
    }

    override fun onUserLeave() {
        presenter.requestPipMode()
    }

    override fun onStop() {
        super.onStop()
        previewSnapAnimation.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mOrientationListener?.disable()
        mOrientationListener = null
        mCompositeDisposable.clear()
        mScreenWakeLock?.let {
            if (it.isHeld)
                it.release()
            mScreenWakeLock = null
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
    }


    //todo: enable pip when only our video is displayed
    override fun enterPipMode(accountId: String, callId: String?) {
        val context = requireContext()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val binding = binding ?: return
            if (binding.participantOverlayContainer.visibility != View.VISIBLE)
                return
            try {
                requireActivity().enterPictureInPictureMode(PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(1, 1))
                    .setActions(listOf(RemoteAction(
                        Icon.createWithResource(context, R.drawable.baseline_call_end_24),
                        getString(R.string.action_call_hangup),
                        getString(R.string.action_call_hangup),
                        PendingIntent.getService(
                            context,
                            Random().nextInt(),
                            Intent(DRingService.ACTION_CALL_END)
                                .setClass(context, DRingService::class.java)
                                .putExtra(NotificationService.KEY_CALL_ID, callId)
                                .putExtra(ConversationPath.KEY_ACCOUNT_ID, accountId),
                            ContentUri.immutable(PendingIntent.FLAG_ONE_SHOT)
                        )
                    ))).build())
            } catch (e: Exception) {
                Log.w(TAG, "Can't enter  PIP mode", e)
            }
        } else if (isTv(context)) {
            requireActivity().enterPictureInPictureMode()
        }
    }

    /**
     * Called when the Picture-in-Picture (PIP) mode state is changed.
     *
     * @param isInPictureInPictureMode  Indicates whether the app is currently in PIP mode.
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPIP = isInPictureInPictureMode
        val binding = binding ?: return

        binding.participantOverlayContainer.togglePipMode(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            val callActivity = activity as CallActivity?
            callActivity?.hideSystemUI()
            mBackstackLost = true
            // Hide UI elements not related to PIP mode.
            binding.callRelativeLayoutSurfaces.isVisible = false
            binding.callRelativeLayoutButtons.isVisible = false
            binding.callCoordinatorOptionContainer.isVisible = false
        } else {
            mBackstackLost = true
            // Show normal UI elements.
            binding.callRelativeLayoutSurfaces.isVisible = true
            binding.callRelativeLayoutButtons.isVisible = true
            binding.callCoordinatorOptionContainer.isVisible = true
        }
    }

    private val listener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.w(TAG, " onSurfaceTextureAvailable -------->  width: $width, height: $height")
            mPreviewSurfaceWidth = width
            mPreviewSurfaceHeight = height
            presenter.previewVideoSurfaceCreated(binding!!.previewSurface)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Log.w(TAG, " onSurfaceTextureSizeChanged ------>  width: $width, height: $height")
            mPreviewSurfaceWidth = width
            mPreviewSurfaceHeight = height
            configurePreview(width, 1f)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            presenter.previewVideoSurfaceDestroyed()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    /**
     * @param hiddenState 0.f if fully shown, 1.f if fully hidden.
     */
    private fun setPreviewDragHiddenState(hiddenState: Float) {
        binding?.let { binding ->
            binding.previewSurface.alpha = 1f - 3 * hiddenState / 4
            binding.extensionPreviewSurface.alpha = 1f - 3 * hiddenState / 4
            binding.previewHandle.alpha = hiddenState
            binding.extensionPreviewHandle.alpha = hiddenState
        }
    }

    private val previewTouchListener = object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val action = event.actionMasked
            val parent = v.parent as RelativeLayout
            val params = v.layoutParams as RelativeLayout.LayoutParams
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    previewSnapAnimation.cancel()
                    previewDrag = PointF(event.x, event.y)
                    v.elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation_dragged)
                    params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    params.setMargins(
                        v.x.toInt(),
                        v.y.toInt(),
                        parent.width - (v.x.toInt() + v.width),
                        parent.height - (v.y.toInt() + v.height)
                    )
                    v.layoutParams = params
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (previewDrag != null) {
                        val currentXPosition = params.leftMargin + (event.x - previewDrag!!.x).toInt()
                        val currentYPosition = params.topMargin + (event.y - previewDrag!!.y).toInt()
                        params.setMargins(
                            currentXPosition,
                            currentYPosition,
                            -(currentXPosition + v.width - event.x.toInt()),
                            -(currentYPosition + v.height - event.y.toInt())
                        )
                        v.layoutParams = params
                        val outPosition = binding!!.previewContainer.width * 0.85f
                        var drapOut = 0f
                        if (currentXPosition < 0) {
                            drapOut = min(1f, -currentXPosition / outPosition)
                        } else if (currentXPosition + v.width > parent.width) {
                            drapOut = min(1f, (currentXPosition + v.width - parent.width) / outPosition)
                        }
                        setPreviewDragHiddenState(drapOut)
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    if (previewDrag != null) {
                        val currentXPosition = params.leftMargin + (event.x - previewDrag!!.x).toInt()
                        previewSnapAnimation.cancel()
                        previewDrag = null
                        v.elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation)
                        var ml = 0
                        var mr = 0
                        var mt = 0
                        var mb = 0
                        val hp = binding!!.previewHandle.layoutParams as FrameLayout.LayoutParams
                        if (params.leftMargin + v.width / 2 > parent.width / 2) {
                            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                            mr = (parent.width - v.width - v.x).toInt()
                            previewPosition = PreviewPosition.RIGHT
                            hp.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                        } else {
                            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                            ml = v.x.toInt()
                            previewPosition = PreviewPosition.LEFT
                            hp.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                        }
                        binding!!.previewHandle.layoutParams = hp
                        if (params.topMargin + v.height / 2 > parent.height / 2) {
                            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            mb = (parent.height - v.height - v.y).toInt()
                        } else {
                            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                            mt = v.y.toInt()
                        }
                        previewMargins[0] = ml
                        previewMargins[1] = mt
                        previewMargins[2] = mr
                        previewMargins[3] = mb
                        params.setMargins(ml, mt, mr, mb)
                        v.layoutParams = params
                        val outPosition = binding!!.previewContainer.width * 0.85f
                        previewHiddenState = when {
                            currentXPosition < 0 ->
                                min(1f, -currentXPosition / outPosition)
                            currentXPosition + v.width > parent.width ->
                                min(1f, (currentXPosition + v.width - parent.width) / outPosition)
                            else -> 0f
                        }
                        setPreviewDragHiddenState(previewHiddenState)
                        previewSnapAnimation.start()
                        return true
                    }
                    return false
                }
                else -> return false
            }
        }
    }

    private fun configurePreview(width: Int, animatedFraction: Float) {
        val binding = binding ?: return
        val params = binding.previewContainer.layoutParams as RelativeLayout.LayoutParams
        val r = 1f - animatedFraction
        var hideMargin = 0f
        var targetHiddenState = 0f
        if (previewHiddenState > 0f) {
            targetHiddenState = 1f
            val v = width * 0.85f * animatedFraction
            hideMargin = if (previewPosition == PreviewPosition.RIGHT) v else -v
        }
        setPreviewDragHiddenState(previewHiddenState * r + targetHiddenState * animatedFraction)
        val f = previewMargin * animatedFraction
        params.setMargins(
            (previewMargins[0] * r + f + hideMargin).toInt(),
            (previewMargins[1] * r + f).toInt(),
            (previewMargins[2] * r + f - hideMargin).toInt(),
            (previewMargins[3] * r + f).toInt()
        )
        binding.previewContainer.layoutParams = params
        binding.extensionPreviewContainer.layoutParams = params
    }

    /**
     * Releases current wakelock and acquires a new proximity wakelock if current call is audio only.
     *
     * @param isAudioOnly true if it is an audio call
     */
    @SuppressLint("WakelockTimeout")
    override fun handleCallWakelock(isAudioOnly: Boolean) {
        if (isAudioOnly) {
            mScreenWakeLock?.apply {
                if (isHeld) release()
            }
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            mScreenWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "jami:callLock"
            ).apply {
                setReferenceCounted(false)
                if (!isHeld)
                    acquire()
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSION_INCOMING && requestCode != REQUEST_PERMISSION_OUTGOING) return
        var i = 0
        val n = permissions.size
        val hasVideo = presenter.wantVideo

        while (i < n) {
            val audioGranted = mDeviceRuntimeService.hasAudioPermission()
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    presenter.cameraPermissionChanged(granted)
                    if (audioGranted) {
                        initializeCall(requestCode == REQUEST_PERMISSION_INCOMING, hasVideo)
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    presenter.audioPermissionChanged(granted)
                    initializeCall(requestCode == REQUEST_PERMISSION_INCOMING, hasVideo)
                }
            }
            i++
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.w(TAG, "[screenshare] onActivityResult ---> requestCode: $requestCode, resultCode: $resultCode")
        when (requestCode) {
            REQUEST_CODE_ADD_PARTICIPANT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val path = ConversationPath.fromUri(data.data)
                    if (path != null) {
                        presenter.addConferenceParticipant(path.accountId, path.conversationUri)
                    }
                }
            }
            REQUEST_CODE_SCREEN_SHARE -> {
                Log.w(TAG, "[screenshare] onActivityResult ---> requestCode: $requestCode, resultCode: $resultCode")
                if (resultCode == Activity.RESULT_OK && data != null) {
                    try {
                        startScreenShare(resultCode, data)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error starting screen sharing", e)
                    }
                } else {
                    binding!!.callSharescreenBtn.isChecked = false
                }
            }
        }
    }

    override fun displayLocalVideo(display: Boolean) {
        Log.w(TAG, "displayLocalVideo -> $display")
        binding?.apply {
            val extensionMode = isChooseExtensionMode
            previewContainer.isVisible = !extensionMode && display
            extensionPreviewContainer.isVisible = extensionMode && display
            extensionPreviewSurface.isVisible = extensionMode && display
            if (extensionMode) extensionPreviewSurface.setZOrderMediaOverlay(true)
        }
    }

    override fun displayHangupButton(display: Boolean) {
        Log.w(TAG, "displayHangupButton $display")
        /* binding?.apply { confControlGroup.visibility = when {
            !mConferenceMode -> View.GONE
            display && !isChooseExtensionMode -> View.VISIBLE
            else -> View.INVISIBLE
        }} */
    }

    override fun displayDialPadKeyboard() {
        val binding = binding ?: return
        binding.dialpadEditText.requestFocus()
        val imm = binding.dialpadEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.dialpadEditText, InputMethodManager.SHOW_FORCED)
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun switchCamera() {
        val videoGranted = mDeviceRuntimeService.hasVideoPermission()
        if (!videoGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            binding!!.callVideocamBtn.isChecked = true
            return
        }
        binding!!.callSpeakerBtn.isChecked = false
        presenter.switchOnOffCamera()
    }

    override fun updateAudioState(state: AudioState) {
        binding!!.callSpeakerBtn.isChecked = state.output.type == HardwareService.AudioOutputType.SPEAKERS
    }

    override fun updateTime(duration: Long) {
        binding?.let { binding ->
            binding.callStatusTxt.text = if (duration <= 0) null else String.format(
                "%d:%02d:%02d",
                duration / 3600,
                duration % 3600 / 60,
                duration % 60
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun updateConfInfo(participantInfo: List<ParticipantInfo>) {
        Log.w(TAG, "updateConfInfo -> $participantInfo")

        val binding = binding ?: return
        mConferenceMode = participantInfo.size > 1

        if (participantInfo.isNotEmpty()) {
            val username = if (participantInfo.size > 1)
                "Conference with ${participantInfo.size} people"
            else participantInfo[0].contact.displayName
            val displayName = if (participantInfo.size > 1) null else participantInfo[0].contact.displayName
            val hasProfileName = displayName != null && !displayName.contentEquals(username)
            val activity = activity
            if (activity != null) {
                val call = participantInfo[0].call
                if (call != null) {
                    val conversationUri = if (call.conversationId != null)
                        Uri(Uri.SWARM_SCHEME, call.conversationId!!)
                    else call.contact!!.conversationUri.blockingFirst()
                    activity.intent = Intent(
                        Intent.ACTION_VIEW,
                        ConversationPath.toUri(call.account!!, conversationUri), context, CallActivity::class.java
                    )
                        .apply { putExtra(NotificationService.KEY_CALL_ID, call.confId ?: call.daemonIdString) }

                    arguments = Bundle().apply {
                        putString(KEY_ACTION, Intent.ACTION_VIEW)
                        putString(NotificationService.KEY_CALL_ID, call.confId ?: call.daemonIdString)
                    }
                } else {
                    Log.w(TAG, "DEBUG null call")
                }
            } else {
                Log.w(TAG, "DEBUG null activity")
            }
            if (hasProfileName) {
                binding.contactBubbleNumTxt.visibility = View.VISIBLE
                binding.contactBubbleTxt.text = displayName
                binding.contactBubbleNumTxt.text = username
            } else {
                binding.contactBubbleNumTxt.visibility = View.GONE
                binding.contactBubbleTxt.text = username
            }
            binding.contactBubble.setImageDrawable(
                AvatarDrawable.Builder()
                    .withContact(participantInfo[0].contact)
                    .withCircleCrop(true)
                    .withPresence(false)
                    .build(requireActivity())
            )
            generateParticipantOverlay(participantInfo)
            presenter.prepareBottomSheetButtonsStatus()
        }

        binding.confControlGroup.visibility = View.VISIBLE
        confAdapter?.updateFromCalls(participantInfo)
        // Create new adapter
            ?: ConfParticipantAdapter(participantInfo, object : ConfParticipantSelected {
                override fun onParticipantSelected(
                    contact: ParticipantInfo,
                    action: ConfParticipantAdapter.ParticipantAction
                ) {
                    when (action) {
                        ConfParticipantAdapter.ParticipantAction.ShowDetails -> presenter.openParticipantContact(contact)
                        ConfParticipantAdapter.ParticipantAction.Hangup -> presenter.hangupParticipant(contact)
                        ConfParticipantAdapter.ParticipantAction.Mute -> presenter.muteParticipant(
                            contact,
                            !contact.audioModeratorMuted
                        )
                        ConfParticipantAdapter.ParticipantAction.Extend -> presenter.maximizeParticipant(contact)
                    }
                }
            }).apply {
                setHasStableIds(true)
                confAdapter = this
                binding.confControlGroup.adapter = this
            }
        binding.root.post { setBottomSheet() }

        if(callMediaHandlers == null)
            callMediaHandlers = ExtensionUtils.getInstalledExtensions(binding.extensionsListContainer.context)
                    .filter { !it.handlerId.isNullOrEmpty() }
        extensionsAdapter = ExtensionsAdapter(
            mList = callMediaHandlers!!,
            listener = object : ExtensionsAdapter.ExtensionListItemListener {
                override fun onExtensionItemClicked(extensionDetails: ExtensionDetails) {}

                override fun onExtensionEnabled(extensionDetails: ExtensionDetails) {
                    extensionDetails.isRunning = !extensionDetails.isRunning
                    if (!isChooseExtensionMode) {
                        presenter.startExtension(extensionDetails.handlerId!!)
                        isChooseExtensionMode = true
                    } else {
                        if (extensionDetails.isRunning) {
                            presenter.toggleCallMediaHandler(extensionDetails.handlerId!!, true)
                        } else {
                            presenter.toggleCallMediaHandler(extensionDetails.handlerId!!, false)
                            for (handler in callMediaHandlers!!)
                                if (handler.isRunning) break
                                else {
                                    presenter.stopExtension()
                                    isChooseExtensionMode = false
                                }
                        }
                    }
                }
            },
            accountId = if (participantInfo.isNotEmpty()) participantInfo[0].call?.account else null
        )
        binding.extensionsListContainer.adapter = extensionsAdapter
    }

    private fun generateParticipantOverlay(participantsInfo: List<ParticipantInfo>) {
        val overlayViewBinding = binding?.participantOverlayContainer ?: return
        overlayViewBinding.participants = if (participantsInfo.size == 1) participantsInfo else participantsInfo.filterNot {
            it.contact.contact.isUser && it.device == presenter.getDeviceId()
        }
        overlayViewBinding.initialize()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            overlayViewBinding.post { updatePipParams() }
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

    override fun updateCallStatus(callState: CallStatus) {
        binding!!.callStatusTxt.setText(callStateToHumanState(callState))
    }

    /** Receive data from the presenter in order to display valid button to the user */
    override fun updateBottomSheetButtonStatus(
        isConference: Boolean,
        isSpeakerOn: Boolean,
        isMicrophoneMuted: Boolean,
        hasMultipleCamera: Boolean,
        canDial: Boolean,
        showExtensionBtn: Boolean,
        onGoingCall: Boolean,
        hasActiveCameraVideo: Boolean,
        hasActiveScreenShare: Boolean
    ) {
        binding?.apply {
            extensionsBtnContainer.isVisible = showExtensionBtn
            raiseHandBtnContainer.isVisible = mConferenceMode
            callDialpadBtn.isClickable = canDial
            dialpadBtnContainer.isVisible = canDial

            callVideocamBtn.apply {
                isChecked = !hasActiveCameraVideo
                setImageResource(if (isChecked) R.drawable.baseline_videocam_off_24 else R.drawable.baseline_videocam_on_24)
            }
            callCameraFlipBtn.apply {
                isEnabled = !callVideocamBtn.isChecked
                setImageResource(if (hasMultipleCamera && hasActiveCameraVideo) R.drawable.baseline_flip_camera_24 else R.drawable.baseline_flip_camera_24_off)
            }
            callSharescreenBtn.isChecked = hasActiveScreenShare
            callMicBtn.isChecked = isMicrophoneMuted
        }
    }

    /**
     * Set bottom sheet, define height for each state (Expanded/Half-expanded/Collapsed) based on current Display metrics (density & size)
     */
    private fun setBottomSheet(newInset: WindowInsetsCompat? = null) {
        val binding = binding ?: return
        val bsView = binding.callOptionsBottomSheet
        val bsHeight = binding.constraintBsContainer.height
        if (isInPIP || !bsView.isVisible) return

        val dm = resources.displayMetrics
        val density = dm.density
        val screenHeight = binding.callCoordinatorOptionContainer.height
        val gridViewHeight = binding.callParametersGrid.height
        val land = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // define bottomsheet width based on screen orientation
        val bsViewParam = bsView.layoutParams
        bsViewParam.width = if (land) getBottomSheetMaxWidth(dm.widthPixels, density) else -1
        bsView.layoutParams = bsViewParam

        val inset = newInset ?: ViewCompat.getRootWindowInsets(requireView()) ?: return
        val bottomInsets = inset.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
        val topInsets = inset.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top

        val desiredPeekHeight = if (land) (10f * density) + (gridViewHeight / 2f) else (10f * density) + (gridViewHeight / 2f) + bottomInsets
        val halfRatio = ((10f * density) + gridViewHeight + bottomInsets) / screenHeight

        val fullyExpandedOffset = if (screenHeight <= bsHeight + bottomInsets)
                (50 * density).toInt()
        else
                (screenHeight - bsHeight - bottomInsets)

        binding.callCoordinatorOptionContainer.updatePadding(bottom = if (land) 0 else bottomInsets)
        binding.callOptionsBottomSheet.updatePadding(bottom = if (land) (topInsets - (5 * density)).toInt() else bottomInsets)

        bottomSheetParams?.apply {
            expandedOffset = fullyExpandedOffset
            halfExpandedRatio = if (halfRatio <= 0 || halfRatio >= 1) 0.4f else halfRatio
            peekHeight = desiredPeekHeight.toInt()
            saveFlags = BottomSheetBehavior.SAVE_PEEK_HEIGHT
        }
    }

    private fun displayBottomSheet(display: Boolean) {
        val binding = binding ?: return
        binding.callOptionsBottomSheet.isVisible = display && presenter.mOnGoingCall == true
    }

    override fun resetBottomSheetState() {
        bottomSheetParams?.let { bs ->
            bs.isHideable = false
            bs.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    enum class BottomSheetAnimation {
        UP, DOWN
    }

    fun moveBottomSheet(movement: BottomSheetAnimation) {
        val binding = binding ?: return
        when (movement) {
            BottomSheetAnimation.UP -> {
                binding.callCoordinatorOptionContainer.let {
                    if (it.isVisible) {
                        it.animate()
                            .translationY(0f)
                            .alpha(1.0f)
                            .setListener(null)
                    }
                }
                displayBottomSheet(true)
                setBottomSheet()
            }
            BottomSheetAnimation.DOWN -> {
                binding.callCoordinatorOptionContainer.apply {
                    this@apply.updatePadding(bottom = 0)
                    binding.callOptionsBottomSheet.updatePadding(bottom = 0)
                    animate()
                        .translationY(250f)
                        .alpha(0.0f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                displayBottomSheet(false)
                            }
                        })
                    WindowInsetsControllerCompat(requireActivity().window, binding.root).apply {
                        requireActivity().window.navigationBarColor = resources.getColor(R.color.transparent)
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }
    }

    /**
     * Init the Call view when the call is ongoing
     * */
    override fun initNormalStateDisplay() {
        Log.w(CallPresenter.TAG, "initNormalStateDisplay")
        binding?.apply {
            callBtnRow.isVisible = false
            callAcceptBtn.isVisible = false
            callAcceptBtnText.isVisible = false
            callAcceptAudioBtn.isVisible = false
            callAcceptAudioBtnText.isVisible = false
            callRefuseBtn.isVisible = false
            callRefuseBtnText.isVisible = false
            contactBubbleLayout.isVisible = false
            participantOverlayContainer.isVisible = true
        }

        val callActivity = activity as CallActivity?
        callActivity?.showSystemUI()
    }

    override fun initIncomingCallDisplay(hasVideo: Boolean) {
        Log.w(TAG, "initIncomingCallDisplay")
        binding?.apply {
            callBtnRow.isVisible = true
            callAcceptBtn.isVisible = hasVideo
            callAcceptBtnText.isVisible = hasVideo
            callAcceptAudioBtn.isVisible = true
            callAcceptAudioBtnText.isVisible = true
            callRefuseBtn.isVisible = true
            callRefuseBtnText.isVisible = true
            contactBubbleLayout.isVisible = true
            participantOverlayContainer.isVisible = false
        }
    }

    override fun initOutGoingCallDisplay() {
        Log.w(TAG, "initOutGoingCallDisplay")
        binding?.apply {
            callAcceptBtn.isVisible = false
            callAcceptBtnText.isVisible = false
            callAcceptAudioBtn.isVisible = false
            callAcceptAudioBtnText.isVisible = false
            callRefuseBtn.isVisible = true
            callRefuseBtnText.isVisible = true
            contactBubbleLayout.isVisible = true
        }
    }

    // change le ratio de la video mais ne change pas la taille du container
    override fun resetPreviewVideoSize(previewWidth: Int?, previewHeight: Int?, rot: Int) {
        if (previewWidth == -1 && previewHeight == -1) return
        if (previewWidth != null ) mPreviewWidth = previewWidth
        if (previewHeight != null ) mPreviewHeight = previewHeight
        val flip = rot % 180 != 0
        if (isChooseExtensionMode) {
            binding?.extensionPreviewSurface?.setAspectRatio(
                if (flip) mPreviewHeight else mPreviewWidth,
                if (flip) mPreviewWidth else mPreviewHeight
            )
        } else {
            binding?.previewSurface?.setAspectRatio(
                if (flip) mPreviewHeight else mPreviewWidth,
                if (flip) mPreviewWidth else mPreviewHeight
            )
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity ?: return
        val binding = binding ?: return
        val rotation = activity.windowManager.defaultDisplay.rotation
        val rot = Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        val bufferRect = RectF(0f, 0f, mPreviewHeight.toFloat(), mPreviewWidth.toFloat())
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
        val scale = max(viewHeight.toFloat() / mPreviewHeight, viewWidth.toFloat() / mPreviewWidth)
        matrix.postScale(scale, scale, centerX, centerY)
        if (rot) {
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        if (!isChooseExtensionMode) {
            binding.previewSurface.setTransform(matrix)
        }
    }

    override fun goToConversation(accountId: String, conversationId: Uri) {
        val context = requireContext()
        if (isTablet(context)) {
            startActivity(Intent(Intent.ACTION_VIEW,
                ConversationPath.toUri(accountId, conversationId),
                context,
                HomeActivity::class.java))
        } else {
            startActivityForResult(Intent(Intent.ACTION_VIEW,
                ConversationPath.toUri(accountId, conversationId),
                context,
                ConversationActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT),
                HomeActivity.REQUEST_CODE_CONVERSATION)
        }
    }

    override fun goToAddContact(contact: Contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact), ConversationFragment.REQ_ADD_CONTACT)
    }

    override fun goToContact(accountId: String, contact: Contact) {
        startActivity(Intent(Intent.ACTION_VIEW,
            ConversationPath.toUri(accountId, contact.uri),
            requireContext(),
            ConversationDetailsActivity::class.java))
    }

    /**
     * Checks if permissions are accepted for camera and microphone. Takes into account whether call is incoming and outgoing, and requests permissions if not available.
     * Initializes the call if permissions are accepted.
     *
     * @param acceptIncomingCall true if call is incoming, false for outgoing
     * @see .initializeCall
     */
    override fun prepareCall(acceptIncomingCall: Boolean) {
        val hasVideo = presenter.wantVideo

        val permissionType =
            if (acceptIncomingCall) REQUEST_PERMISSION_INCOMING else REQUEST_PERMISSION_OUTGOING
        val audioGranted = mDeviceRuntimeService.hasAudioPermission()
        val videoGranted = !hasVideo || mDeviceRuntimeService.hasVideoPermission()

        if (!audioGranted || !videoGranted) {
            val perms = ArrayList<String>()
            if (!videoGranted) perms.add(Manifest.permission.CAMERA)
            if (!audioGranted) perms.add(Manifest.permission.RECORD_AUDIO)
            requestPermissions(perms.toTypedArray(), permissionType)
        } else initializeCall(acceptIncomingCall, hasVideo)
    }

    /**
     * Starts a call. Takes into account whether call is incoming or outgoing.
     *
     * @param isIncoming true if call is incoming, false for outgoing
     * @param hasVideo true if we already know that conversation has video
     */
    private fun initializeCall(isIncoming: Boolean, hasVideo: Boolean) {
        if (isIncoming) presenter.acceptCall(hasVideo)
        else
            arguments?.let { args ->
                val conversation = ConversationPath.fromBundle(args)!!
                presenter.initOutGoing(
                    conversation.accountId,
                    conversation.conversationUri,
                    args.getString(Intent.EXTRA_PHONE_NUMBER),
                    hasVideo
                )
            }
    }

    override fun finish(hangupReason: CallPresenter.HangupReason) {
        // Display a toast if the call was not allowed
        if (hangupReason == CallPresenter.HangupReason.ERROR)
            Toast.makeText(context, R.string.call_error, Toast.LENGTH_SHORT).show()

        activity?.let { activity ->
            activity.finishAndRemoveTask()
            if (mBackstackLost) {
                startActivity(
                    Intent.makeMainActivity(ComponentName(activity, HomeActivity::class.java))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun addParticipantClicked() {
        presenter.startAddParticipant()
    }

    //todo if videomode, should mute/unmute audio output, if audio only, should switch between speaker options
    fun speakerClicked() {
        presenter.speakerClick(binding!!.callSpeakerBtn.isChecked)
    }

    private fun startScreenShare(resultCode: Int, data: Intent) {
        if (presenter.startScreenShare(resultCode, data)) {
            if (isChooseExtensionMode) {
                binding!!.extensionPreviewSurface.visibility = View.GONE
                displayLocalVideo(false)
            } else {
                binding!!.previewContainer.visibility = View.GONE
                displayLocalVideo(false)
            }
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.screen_sharing_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun startScreenCapture() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHARE)
    }

    fun shareScreenClicked() {
        presenter.switchOnOffScreenShare()
    }

    fun micClicked() {
        binding?.callMicBtn?.let { micButton ->
            presenter.isMicrophoneMuted = !presenter.isMicrophoneMuted
            presenter.muteMicrophoneToggled(micButton.isChecked)
            //micButton.setImageResource(if (micButton.isChecked) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_24)
        }
    }

    fun raiseHandClicked() {
        presenter.raiseHand(binding!!.callRaiseHandBtn.isChecked)
    }

    fun hangupClicked() {
        presenter.hangupCall()
    }

    fun refuseClicked() {
        presenter.refuseCall()
    }

    fun acceptAudioClicked() {
        presenter.wantVideo = false
        prepareCall(true)
    }

    fun acceptClicked() {
        presenter.wantVideo = true
        prepareCall(true)
    }

    fun cameraFlip() {
        presenter.switchVideoInputClick()
    }

    override fun startAddParticipant(conferenceId: String) {
        startActivityForResult(
            Intent(Intent.ACTION_PICK)
                .setClass(requireActivity(), ConversationSelectionActivity::class.java)
                .putExtra(NotificationService.KEY_CALL_ID, conferenceId),
            REQUEST_CODE_ADD_PARTICIPANT
        )
    }

    fun getCallMediaHandlerDetails(id: String): Map<String, String> {
        return JamiService.getCallMediaHandlerDetails(id).toNative()
    }

    override fun positiveMediaButtonClicked() {
        presenter.positiveButtonClicked()
    }

    override fun negativeMediaButtonClicked() {
        presenter.negativeButtonClicked()
    }

    override fun toggleMediaButtonClicked() {
        presenter.toggleButtonClicked()
    }

    override fun displayExtensionsButton(): Boolean {
        return JamiService.getPluginsEnabled() && JamiService.getCallMediaHandlers().size > 0
    }

    override fun getMediaProjection(resultCode: Int, data: Any): MediaProjection {
        val dataIntent = data as Intent
        return mProjectionManager.getMediaProjection(resultCode, dataIntent)
    }

    public fun extensionsButtonClicked() {
        val binding = binding ?: return
        if(binding.callExtensionsBtn.isChecked){
            binding.confControlGroup.adapter = extensionsAdapter
        } else {
            binding.confControlGroup.adapter = confAdapter
        }
    }

    private val blinkingAnimation: Animation
        get() = AlphaAnimation(1f, 0f).apply {
            duration = 400
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }

    companion object {
        val TAG = CallFragment::class.simpleName!!
        const val KEY_ACTION = "action"
        const val KEY_HAS_VIDEO = "HAS_VIDEO"
        private const val REQUEST_CODE_ADD_PARTICIPANT = 6
        private const val REQUEST_PERMISSION_INCOMING = 1003
        private const val REQUEST_PERMISSION_OUTGOING = 1004
        private const val REQUEST_CODE_SCREEN_SHARE = 7

        fun newInstance(action: String, path: ConversationPath?, contactId: String?, hasVideo: Boolean, option: String? = null): CallFragment =
            CallFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ACTION, action)
                    putString(CallPresenter.KEY_ACCEPT_OPTION, option)
                    path?.toBundle(this)
                    putString(Intent.EXTRA_PHONE_NUMBER, contactId)
                    putBoolean(KEY_HAS_VIDEO, hasVideo)
                }
            }

        fun newInstance(action: String, confId: String?, hasVideo: Boolean, option: String? = null): CallFragment =
            CallFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ACTION, action)
                    putString(CallPresenter.KEY_ACCEPT_OPTION, option)
                    putString(NotificationService.KEY_CALL_ID, confId)
                    putBoolean(KEY_HAS_VIDEO, hasVideo)
                }
            }

        fun callStateToHumanState(state: CallStatus): Int = when (state) {
            CallStatus.SEARCHING -> R.string.call_human_state_searching
            CallStatus.CONNECTING -> R.string.call_human_state_connecting
            CallStatus.RINGING -> R.string.call_human_state_ringing
            CallStatus.CURRENT -> R.string.call_human_state_current
            CallStatus.HUNGUP -> R.string.call_human_state_hungup
            CallStatus.BUSY -> R.string.call_human_state_busy
            CallStatus.FAILURE -> R.string.call_human_state_failure
            CallStatus.HOLD -> R.string.call_human_state_hold
            CallStatus.UNHOLD -> R.string.call_human_state_unhold
            CallStatus.OVER -> R.string.call_human_state_over
            CallStatus.NONE -> R.string.call_human_state_none
            else -> R.string.call_human_state_none
        }

        /**
         * getBottomSheetMaxWidth(): Int
         *
         * return the width value for the bottomSheet based on screen size, density and ratio
         * */
        private fun getBottomSheetMaxWidth(width: Int, density: Float): Int {
            val gridMinWidth = 350 * density //width size in dp
            val wRatio = gridMinWidth / width
            return when {
                wRatio < 0f -> -1
                wRatio < 0.5f -> (gridMinWidth * 1.25).toInt()
                wRatio < 0.6f -> (gridMinWidth * 1.20).toInt()
                wRatio < 0.7f -> (gridMinWidth * 1.15).toInt()
                wRatio < 0.8f -> (gridMinWidth * 1.10).toInt()
                wRatio >= 1f -> -1
                else -> -1
            }
        }
    }
}