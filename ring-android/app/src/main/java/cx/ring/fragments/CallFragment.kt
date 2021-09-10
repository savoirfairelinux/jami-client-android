/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.fragments

import android.Manifest
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
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.TextUtils
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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.percentlayout.widget.PercentFrameLayout
import com.rodolfonavalon.shaperipplelibrary.model.Circle
import cx.ring.R
import cx.ring.adapters.ConfParticipantAdapter
import cx.ring.adapters.ConfParticipantAdapter.ConfParticipantSelected
import cx.ring.client.*
import cx.ring.databinding.FragCallBinding
import cx.ring.databinding.ItemParticipantLabelBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.plugins.RecyclerPicker.RecyclerPicker
import cx.ring.plugins.RecyclerPicker.RecyclerPickerLayoutManager.ItemSelectedListener
import cx.ring.service.DRingService
import cx.ring.utils.ActionHelper
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
import net.jami.model.Call
import net.jami.model.Call.CallStatus
import net.jami.model.Conference.ParticipantInfo
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import net.jami.services.HardwareService.AudioState
import net.jami.services.NotificationService
import java.util.*
import javax.inject.Inject
import kotlin.math.min

@AndroidEntryPoint
class CallFragment : BaseSupportFragment<CallPresenter, CallView>(), CallView,
    MediaButtonsHelperCallback, ItemSelectedListener {
    private var binding: FragCallBinding? = null
    private var mOrientationListener: OrientationEventListener? = null
    private var dialPadBtn: MenuItem? = null
    private var pluginsMenuBtn: MenuItem? = null
    private var restartVideo = false
    private var restartPreview = false
    private var mScreenWakeLock: PowerManager.WakeLock? = null
    private var mCurrentOrientation = 0
    private var mVideoWidth = -1
    private var mVideoHeight = -1
    private var mPreviewWidth = 720
    private var mPreviewHeight = 1280
    private var mPreviewSurfaceWidth = 0
    private var mPreviewSurfaceHeight = 0
    private lateinit var mProjectionManager: MediaProjectionManager
    private var mBackstackLost = false
    private var confAdapter: ConfParticipantAdapter? = null
    private var mConferenceMode = false
    var isChoosePluginMode = false
        private set
    private var pluginsModeFirst = true
    private var callMediaHandlers: List<String>? = null
    private var previousPluginPosition = -1
    private var rp: RecyclerPicker? = null
    private val animation = ValueAnimator().apply { duration = 150 }
    private var previewDrag: PointF? = null
    private val previewSnapAnimation = ValueAnimator().apply {
        duration = 250
        setFloatValues(0f, 1f)
        interpolator = DecelerateInterpolator()
        addUpdateListener { a -> configurePreview(mPreviewSurfaceWidth, a.animatedFraction) }
    }
    private val previewMargins = IntArray(4)
    private var previewHiddenState = 0f

    private enum class PreviewPosition { LEFT, RIGHT }
    private var previewPosition = PreviewPosition.RIGHT

    @Inject
    lateinit var mDeviceRuntimeService: DeviceRuntimeService

    private val mCompositeDisposable = CompositeDisposable()
    override fun initPresenter(presenter: CallPresenter) {
        val args = requireArguments()
        args.getString(KEY_ACTION)?.let { action ->
            if (action == ACTION_PLACE_CALL)
                prepareCall(false)
            else if (action == ACTION_GET_CALL || action == CallActivity.ACTION_CALL_ACCEPT)
                presenter.initIncomingCall(args.getString(KEY_CONF_ID)!!, action == ACTION_GET_CALL)
        }
    }

    override fun onUserLeave() {
        presenter.requestPipMode()
    }

    override fun enterPipMode(callId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        val context = requireContext()
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
            } else {
                return
            }
            val actions = ArrayList<RemoteAction>(1)
            actions.add(RemoteAction(Icon.createWithResource(context, R.drawable.baseline_call_end_24),
                    getString(R.string.action_call_hangup),
                    getString(R.string.action_call_hangup),
                    PendingIntent.getService(context, Random().nextInt(),
                        Intent(DRingService.ACTION_CALL_END)
                            .setClass(context, JamiService::class.java)
                            .putExtra(NotificationService.KEY_CALL_ID, callId), PendingIntent.FLAG_ONE_SHOT)))
            paramBuilder.setActions(actions)
            try {
                requireActivity().enterPictureInPictureMode(paramBuilder.build())
            } catch (e: Exception) {
                Log.w(TAG, "Can't enter  PIP mode", e)
            }
        } else if (isTv(context)) {
            requireActivity().enterPictureInPictureMode()
        }
    }

    override fun onStart() {
        super.onStart()
        if (restartVideo && restartPreview) {
            displayVideoSurface(true, !presenter.isPipMode)
            restartVideo = false
            restartPreview = false
        } else if (restartVideo) {
            displayVideoSurface(displayVideoSurface = true, displayPreviewContainer = false)
            restartVideo = false
        }
    }

    override fun onStop() {
        super.onStop()
        previewSnapAnimation.cancel()
        binding?.let { binding ->
            if (binding.videoSurface.visibility == View.VISIBLE) {
                restartVideo = true
            }
            if (!isChoosePluginMode) {
                if (binding.previewContainer.visibility == View.VISIBLE) {
                    restartPreview = true
                }
            } else {
                if (binding.pluginPreviewContainer.visibility == View.VISIBLE) {
                    restartPreview = true
                    presenter.stopPlugin()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return (DataBindingUtil.inflate(inflater, R.layout.frag_call, container, false) as FragCallBinding).also { b ->
            b.presenter = this
            binding = b
            rp = RecyclerPicker(b.recyclerPicker, R.layout.item_picker, LinearLayout.HORIZONTAL, this)
                .apply { setFirstLastElementsWidths(112, 112) }
        }.root
    }

    private val listener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            mPreviewSurfaceWidth = width
            mPreviewSurfaceHeight = height
            presenter.previewVideoSurfaceCreated(binding!!.previewSurface)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
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
            binding.pluginPreviewSurface.alpha = 1f - 3 * hiddenState / 4
            binding.previewHandle.alpha = hiddenState
            binding.pluginPreviewHandle.alpha = hiddenState
        }
    }

    private val previewTouchListener = object: View.OnTouchListener {
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

    @SuppressLint("ClickableViewAccessibility", "RtlHardcoded", "WakelockTimeout")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onViewCreated(view, savedInstanceState)
        mCurrentOrientation = resources.configuration.orientation
        val dpRatio = requireActivity().resources.displayMetrics.density
        animation.addUpdateListener { valueAnimator ->
            binding?.let { binding ->
                val upBy = valueAnimator.animatedValue as Int
                val layoutParams = binding.previewContainer.layoutParams as RelativeLayout.LayoutParams
                layoutParams.setMargins(0, 0, 0, (upBy * dpRatio).toInt())
                binding.previewContainer.layoutParams = layoutParams
            }
        }
        val activity = activity
        if (activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (activity is AppCompatActivity) {
                val ab = activity.supportActionBar
                if (ab != null) {
                    ab.setHomeAsUpIndicator(R.drawable.baseline_chat_24)
                    ab.setDisplayHomeAsUpEnabled(true)
                }
            }
        }
        mProjectionManager = requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        mScreenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "ring:callLock"
        ).apply {
            setReferenceCounted(false)
            if (!isHeld)
                acquire()
        }
        binding?.let { binding ->
            binding.videoSurface.holder.setFormat(PixelFormat.RGBA_8888)
            binding.videoSurface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    presenter.videoSurfaceCreated(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    presenter.videoSurfaceDestroyed()
                }
            })
            binding.pluginPreviewSurface.holder.setFormat(PixelFormat.RGBA_8888)
            binding.pluginPreviewSurface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    presenter.pluginSurfaceCreated(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    presenter.pluginSurfaceDestroyed()
                }
            })
            view.setOnSystemUiVisibilityChangeListener { visibility: Int ->
                val ui = visibility and (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
                presenter.uiVisibilityChanged(ui)
            }
            val ui = view.systemUiVisibility and (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
            presenter.uiVisibilityChanged(ui)
            view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> resetVideoSize(mVideoWidth, mVideoHeight) }
            val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mOrientationListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    val rot = windowManager.defaultDisplay.rotation
                    if (mCurrentOrientation != rot) {
                        mCurrentOrientation = rot
                        presenter.configurationChanged(rot)
                    }
                }
            }.apply { if (canDetectOrientation()) enable() }
            binding.shapeRipple.rippleShape = Circle()
            binding.callSpeakerBtn.isChecked = presenter.isSpeakerphoneOn
            binding.callMicBtn.isChecked = presenter.isMicrophoneMuted
            binding.pluginPreviewSurface.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight)
            }
            binding.previewSurface.surfaceTextureListener = listener
            binding.previewSurface.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                configureTransform(mPreviewSurfaceWidth, mPreviewSurfaceHeight)
            }
            binding.previewContainer.setOnTouchListener(previewTouchListener)
            binding.pluginPreviewContainer.setOnTouchListener { v: View, event: MotionEvent ->
                val action = event.actionMasked
                val parent = v.parent as RelativeLayout
                val params = v.layoutParams as RelativeLayout.LayoutParams
                if (action == MotionEvent.ACTION_DOWN) {
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
                        parent.height - (v.y
                            .toInt() + v.height)
                    )
                    v.layoutParams = params
                    return@setOnTouchListener true
                } else if (action == MotionEvent.ACTION_MOVE) {
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
                        val outPosition = binding.pluginPreviewContainer.width * 0.85f
                        var drapOut = 0f
                        if (currentXPosition < 0) {
                            drapOut = min(1f, -currentXPosition / outPosition)
                        } else if (currentXPosition + v.width > parent.width) {
                            drapOut = min(1f, (currentXPosition + v.width - parent.width) / outPosition)
                        }
                        setPreviewDragHiddenState(drapOut)
                        return@setOnTouchListener true
                    }
                    return@setOnTouchListener false
                } else if (action == MotionEvent.ACTION_UP) {
                    if (previewDrag != null) {
                        val currentXPosition = params.leftMargin + (event.x - previewDrag!!.x).toInt()
                        previewSnapAnimation.cancel()
                        previewDrag = null
                        v.elevation = v.context.resources.getDimension(R.dimen.call_preview_elevation)
                        var ml = 0; var mr = 0; var mt = 0; var mb = 0
                        val hp = binding.pluginPreviewHandle.layoutParams as FrameLayout.LayoutParams
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
                        binding.pluginPreviewHandle.layoutParams = hp
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
                        val outPosition = binding.pluginPreviewContainer.width * 0.85f
                        previewHiddenState = when {
                            currentXPosition < 0 -> min(1f, -currentXPosition / outPosition)
                            currentXPosition + v.width > parent.width -> min(1f, (currentXPosition + v.width - parent.width) / outPosition)
                            else -> 0f
                        }
                        setPreviewDragHiddenState(previewHiddenState)
                        previewSnapAnimation.start()
                        return@setOnTouchListener true
                    }
                    return@setOnTouchListener false
                } else {
                    return@setOnTouchListener false
                }
            }
            binding.dialpadEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    presenter.sendDtmf(s.subSequence(start, start + count))
                }

                override fun afterTextChanged(s: Editable) {}
            })
        }

    }

    private fun configurePreview(width: Int, animatedFraction: Float) {
        val context = context
        if (context == null || binding == null) return
        val margin = context.resources.getDimension(R.dimen.call_preview_margin)
        val params = binding!!.previewContainer.layoutParams as RelativeLayout.LayoutParams
        val r = 1f - animatedFraction
        var hideMargin = 0f
        var targetHiddenState = 0f
        if (previewHiddenState > 0f) {
            targetHiddenState = 1f
            val v = width * 0.85f * animatedFraction
            hideMargin = if (previewPosition == PreviewPosition.RIGHT) v else -v
        }
        setPreviewDragHiddenState(previewHiddenState * r + targetHiddenState * animatedFraction)
        val f = margin * animatedFraction
        params.setMargins(
            (previewMargins[0] * r + f + hideMargin).toInt(),
            (previewMargins[1] * r + f).toInt(),
            (previewMargins[2] * r + f - hideMargin).toInt(),
            (previewMargins[3] * r + f).toInt()
        )
        binding!!.previewContainer.layoutParams = params
        binding!!.pluginPreviewContainer.layoutParams = params
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
                "ring:callLock"
            ).apply {
                setReferenceCounted(false)
                if (!isHeld)
                    acquire()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mOrientationListener != null) {
            mOrientationListener!!.disable()
            mOrientationListener = null
        }
        mCompositeDisposable.clear()
        if (mScreenWakeLock != null && mScreenWakeLock!!.isHeld) {
            mScreenWakeLock!!.release()
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
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
        } else if (requestCode == REQUEST_CODE_SCREEN_SHARE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    startScreenShare(mProjectionManager.getMediaProjection(resultCode, data))
                } catch (e: Exception) {
                    Log.w(TAG, "Error starting screen sharing", e)
                }
            } else {
                binding!!.callScreenshareBtn.isChecked = false
            }
        }
    }

    override fun onCreateOptionsMenu(m: Menu, inf: MenuInflater) {
        super.onCreateOptionsMenu(m, inf)
        inf.inflate(R.menu.ac_call, m)
        dialPadBtn = m.findItem(R.id.menuitem_dialpad)
        pluginsMenuBtn = m.findItem(R.id.menuitem_video_plugins)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        presenter.prepareOptionMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            presenter.chatClick()
        } else if (itemId == R.id.menuitem_dialpad) {
            presenter.dialpadClick()
        } else if (itemId == R.id.menuitem_video_plugins) {
            displayVideoPluginsCarousel()
        }
        return true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        val activity = activity as AppCompatActivity?
        val actionBar = activity?.supportActionBar
        if (actionBar != null) {
            if (isInPictureInPictureMode) {
                actionBar.hide()
            } else {
                mBackstackLost = true
                actionBar.show()
            }
        }
        presenter.pipModeChanged(isInPictureInPictureMode)
    }

    override fun displayContactBubble(display: Boolean) {
        if (binding != null) binding!!.contactBubbleLayout.handler.post {
            if (binding != null) binding!!.contactBubbleLayout.visibility =
                if (display) View.VISIBLE else View.GONE
        }
    }

    override fun displayVideoSurface(
        displayVideoSurface: Boolean,
        displayPreviewContainer: Boolean
    ) {
        binding!!.videoSurface.visibility =
            if (displayVideoSurface) View.VISIBLE else View.GONE
        if (isChoosePluginMode) {
            binding!!.pluginPreviewSurface.visibility =
                if (displayPreviewContainer) View.VISIBLE else View.GONE
            binding!!.pluginPreviewContainer.visibility =
                if (displayPreviewContainer) View.VISIBLE else View.GONE
            binding!!.previewContainer.visibility = View.GONE
        } else {
            binding!!.pluginPreviewSurface.visibility = View.GONE
            binding!!.pluginPreviewContainer.visibility = View.GONE
            binding!!.previewContainer.visibility =
                if (displayPreviewContainer) View.VISIBLE else View.GONE
        }
        updateMenu()
    }

    override fun displayPreviewSurface(display: Boolean) {
        if (display) {
            binding!!.videoSurface.setZOrderOnTop(false)
            binding!!.videoSurface.setZOrderMediaOverlay(false)
        } else {
            binding!!.videoSurface.setZOrderMediaOverlay(true)
            binding!!.videoSurface.setZOrderOnTop(true)
        }
    }

    override fun displayHangupButton(display: Boolean) {
        var display = display
        Log.w(TAG, "displayHangupButton $display")
        display = display and !isChoosePluginMode
        binding?.apply {
            callControlGroup.visibility = if (display) View.VISIBLE else View.GONE
            callHangupBtn.visibility = if (display) View.VISIBLE else View.GONE
            confControlGroup.visibility = if (mConferenceMode && display) View.VISIBLE else View.GONE
        }
    }

    override fun displayDialPadKeyboard() {
        binding!!.dialpadEditText.requestFocus()
        val imm = binding!!.dialpadEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun switchCameraIcon(isFront: Boolean) {
        binding!!.callCameraFlipBtn.setImageResource(if (isFront) R.drawable.baseline_camera_front_24 else R.drawable.baseline_camera_rear_24)
    }

    override fun updateAudioState(state: AudioState) {
        binding!!.callSpeakerBtn.isChecked = state.outputType == HardwareService.AudioOutput.SPEAKERS
    }

    override fun updateMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun updateTime(duration: Long) {
        binding?.let { binding ->
            binding.callStatusTxt.text = if (duration <= 0) null else String.format(
                Locale.getDefault(),
                "%d:%02d:%02d",
                duration / 3600,
                duration % 3600 / 60,
                duration % 60
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun updateContactBubble(contacts: List<Call>) {
        Log.w(TAG, "updateContactBubble " + contacts.size)
        val username = if (contacts.size > 1)
            "Conference with " + contacts.size + " people"
        else contacts[0].contact!!.displayName
        val displayName = if (contacts.size > 1) null else contacts[0].contact!!.displayName
        val hasProfileName = displayName != null && !displayName.contentEquals(username)
        val activity = activity as AppCompatActivity?
        if (activity != null) {
            val ab = activity.supportActionBar
            if (ab != null) {
                if (hasProfileName) {
                    ab.title = displayName
                    ab.subtitle = username
                } else {
                    ab.title = username
                    ab.subtitle = null
                }
                ab.setDisplayShowTitleEnabled(true)
            }
        }
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
                .withContact(contacts[0].contact)
                .withCircleCrop(true)
                .withPresence(false)
                .build(requireActivity())
        )
    }

    @SuppressLint("RestrictedApi")
    override fun updateConfInfo(participantInfo: List<ParticipantInfo>) {
        Log.w(TAG, "updateConfInfo $participantInfo")
        mConferenceMode = participantInfo.size > 1
        binding!!.participantLabelContainer.removeAllViews()
        if (participantInfo.isNotEmpty()) {
            val inflater = LayoutInflater.from(binding!!.participantLabelContainer.context)
            for (i in participantInfo) {
                val displayName = i.contact.displayName
                if (!TextUtils.isEmpty(displayName)) {
                    val label = ItemParticipantLabelBinding.inflate(inflater)
                    val params = PercentFrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    params.percentLayoutInfo.leftMarginPercent = i.x / mVideoWidth.toFloat()
                    params.percentLayoutInfo.topMarginPercent = i.y / mVideoHeight.toFloat()
                    params.percentLayoutInfo.rightMarginPercent =
                        1f - (i.x + i.w) / mVideoWidth.toFloat()
                    //params.getPercentLayoutInfo().rightMarginPercent = (i.x + i.w) / (float) mVideoWidth;
                    label.participantName.text = displayName
                    label.moderator.visibility = if (i.isModerator) View.VISIBLE else View.GONE
                    label.mute.visibility = if (i.audioMuted) View.VISIBLE else View.GONE
                    binding!!.participantLabelContainer.addView(label.root, params)
                }
            }
        }
        binding!!.participantLabelContainer.visibility =
            if (participantInfo.isEmpty()) View.GONE else View.VISIBLE
        if (participantInfo.isEmpty() || participantInfo.size < 2) {
            binding!!.confControlGroup.visibility = View.GONE
        } else {
            binding!!.confControlGroup.visibility = View.VISIBLE
            if (confAdapter == null) {
                confAdapter =
                    ConfParticipantAdapter(object : ConfParticipantSelected {
                        override fun onParticipantSelected(view: View, contact: ParticipantInfo) {
                            val maximized = presenter.isMaximized(contact)
                            val popup = PopupMenu(view.context, view)
                            popup.inflate(R.menu.conference_participant_actions)
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.conv_contact_details -> presenter.openParticipantContact(contact)
                                    R.id.conv_contact_hangup -> presenter.hangupParticipant(contact)
                                    R.id.conv_mute -> presenter.muteParticipant(contact, !contact.audioMuted)
                                    R.id.conv_contact_maximize -> presenter.maximizeParticipant(contact)
                                    else -> return@setOnMenuItemClickListener false
                                }
                                true
                            }
                            val menu = popup.menu as MenuBuilder
                            val maxItem = menu.findItem(R.id.conv_contact_maximize)
                            val muteItem = menu.findItem(R.id.conv_mute)
                            if (maximized) {
                                maxItem.setTitle(R.string.action_call_minimize)
                                maxItem.setIcon(R.drawable.baseline_close_fullscreen_24)
                            } else {
                                maxItem.setTitle(R.string.action_call_maximize)
                                maxItem.setIcon(R.drawable.baseline_open_in_full_24)
                            }
                            if (!contact.audioMuted) {
                                muteItem.setTitle(R.string.action_call_mute)
                                muteItem.setIcon(R.drawable.baseline_mic_off_24)
                            } else {
                                muteItem.setTitle(R.string.action_call_unmute)
                                muteItem.setIcon(R.drawable.baseline_mic_24)
                            }
                            val menuHelper = MenuPopupHelper(view.context, menu, view)
                            menuHelper.gravity = Gravity.END
                            menuHelper.setForceShowIcon(true)
                            menuHelper.show()
                        }
                    })
            }
            confAdapter!!.updateFromCalls(participantInfo)
            if (binding!!.confControlGroup.adapter == null) binding!!.confControlGroup.adapter =
                confAdapter
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

    override fun updateCallStatus(callStatus: CallStatus) {
        binding!!.callStatusTxt.setText(callStateToHumanState(callStatus))
    }

    override fun initMenu(
        isSpeakerOn: Boolean, displayFlip: Boolean, canDial: Boolean,
        showPluginBtn: Boolean, onGoingCall: Boolean
    ) {
        if (binding != null) {
            binding!!.callCameraFlipBtn.visibility = if (displayFlip) View.VISIBLE else View.GONE
        }
        if (dialPadBtn != null) {
            dialPadBtn!!.isVisible = canDial
        }
        if (pluginsMenuBtn != null) {
            pluginsMenuBtn!!.isVisible = showPluginBtn
        }
        updateMenu()
    }

    override fun initNormalStateDisplay(audioOnly: Boolean, isMuted: Boolean) {
        Log.w(TAG, "initNormalStateDisplay")
        binding?.apply {
            shapeRipple.stopRipple()
            callAcceptBtn.visibility = View.GONE
            callRefuseBtn.visibility = View.GONE
            callControlGroup.visibility = View.VISIBLE
            callHangupBtn.visibility = View.VISIBLE
            contactBubbleLayout.visibility = if (audioOnly) View.VISIBLE else View.GONE
            callMicBtn.isChecked = isMuted
        }
        requireActivity().invalidateOptionsMenu()
        val callActivity = activity as CallActivity?
        callActivity?.showSystemUI()
    }

    override fun initIncomingCallDisplay() {
        Log.w(TAG, "initIncomingCallDisplay")
        binding?.apply {
            callAcceptBtn.visibility = View.VISIBLE
            callRefuseBtn.visibility = View.VISIBLE
            callControlGroup.visibility = View.GONE
            callHangupBtn.visibility = View.GONE
            contactBubbleLayout.visibility = View.VISIBLE
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun initOutGoingCallDisplay() {
        Log.w(TAG, "initOutGoingCallDisplay")
        binding?.apply {
            callAcceptBtn.visibility = View.GONE
            callRefuseBtn.visibility = View.VISIBLE
            callControlGroup.visibility = View.GONE
            callHangupBtn.visibility = View.GONE
            contactBubbleLayout.visibility = View.VISIBLE
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun resetPreviewVideoSize(previewWidth: Int, previewHeight: Int, rot: Int) {
        if (previewWidth == -1 && previewHeight == -1) return
        mPreviewWidth = previewWidth
        mPreviewHeight = previewHeight
        val flip = rot % 180 != 0
        binding?.previewSurface?.setAspectRatio(
            if (flip) mPreviewHeight else mPreviewWidth,
            if (flip) mPreviewWidth else mPreviewHeight
        )
    }

    override fun resetPluginPreviewVideoSize(previewWidth: Int, previewHeight: Int, rot: Int) {
        if (previewWidth == -1 && previewHeight == -1) return
        mPreviewWidth = previewWidth
        mPreviewHeight = previewHeight
        val flip = rot % 180 != 0
        binding?.pluginPreviewSurface?.setAspectRatio(
            if (flip) mPreviewHeight else mPreviewWidth,
            if (flip) mPreviewWidth else mPreviewHeight
        )
    }

    override fun resetVideoSize(videoWidth: Int, videoHeight: Int) {
        val rootView = view as ViewGroup? ?: return
        val videoRatio = videoWidth / videoHeight.toDouble()
        val screenRatio = rootView.width / rootView.height.toDouble()
        val params = binding!!.videoSurface.layoutParams as RelativeLayout.LayoutParams
        val oldW = params.width
        val oldH = params.height
        if (videoRatio >= screenRatio) {
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT
            params.height =
                (videoHeight * rootView.width.toDouble() / videoWidth.toDouble()).toInt()
        } else {
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT
            params.width =
                (videoWidth * rootView.height.toDouble() / videoHeight.toDouble()).toInt()
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
        // Log.w(TAG, "configureTransform " + viewWidth + "x" + viewHeight + " rot=" + rot + " mPreviewWidth=" + mPreviewWidth + " mPreviewHeight=" + mPreviewHeight);
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (rot) {
            val bufferRect = RectF(0f, 0f, mPreviewHeight.toFloat(), mPreviewWidth.toFloat())
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewHeight,
                viewWidth.toFloat() / mPreviewWidth
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        if (!isChoosePluginMode) {
//            binding.pluginPreviewSurface.setTransform(matrix);
//        }
//        else {
            binding!!.previewSurface.setTransform(matrix)
        }
    }

    override fun goToConversation(accountId: String, conversationId: Uri) {
        val context = requireContext()
        if (isTablet(context)) {
            startActivity(
                Intent(DRingService.ACTION_CONV_ACCEPT, ConversationPath.toUri(accountId, conversationId), context, HomeActivity::class.java)
            )
        } else {
            startActivityForResult(
                Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, conversationId), context, ConversationActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT),
                HomeActivity.REQUEST_CODE_CONVERSATION
            )
        }
    }

    override fun goToAddContact(contact: Contact) {
        startActivityForResult(ActionHelper.getAddNumberIntentForContact(contact), ConversationFragment.REQ_ADD_CONTACT)
    }

    override fun goToContact(accountId: String, contact: Contact) {
        startActivity(
            Intent(Intent.ACTION_VIEW, ConversationPath.toUri(accountId, contact.uri))
                .setClass(requireContext(), ContactDetailsActivity::class.java)
        )
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
            val args = arguments
            audioOnly = args != null && args.getBoolean(KEY_AUDIO_ONLY)
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
        if (isIncoming) {
            presenter.acceptCall()
        } else {
            arguments?.let { args ->
                val conversation = ConversationPath.fromBundle(args)!!
                presenter.initOutGoing(
                    conversation.accountId,
                    conversation.conversationUri,
                    args.getString(Intent.EXTRA_PHONE_NUMBER),
                    args.getBoolean(KEY_AUDIO_ONLY)
                )
            }
        }
    }

    override fun finish() {
        activity?.let { activity ->
            activity.finishAndRemoveTask()
            if (mBackstackLost) {
                startActivity(
                    Intent.makeMainActivity(ComponentName(activity, HomeActivity::class.java)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun speakerClicked() {
        presenter.speakerClick(binding!!.callSpeakerBtn.isChecked)
    }

    private fun startScreenShare(mediaProjection: MediaProjection?) {
        if (presenter.startScreenShare(mediaProjection)) {
            if (isChoosePluginMode) {
                binding!!.pluginPreviewSurface.visibility = View.GONE
            } else {
                binding!!.previewSurface.visibility = View.GONE
            }
        } else {
            Toast.makeText(requireContext(), "Can't start screen sharing", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun stopShareScreen() {
        binding?.previewSurface?.visibility = View.VISIBLE
        presenter.stopScreenShare()
    }

    fun shareScreenClicked(checked: Boolean) {
        if (!checked) {
            stopShareScreen()
        } else {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHARE)
        }
    }

    fun micClicked() {
        binding?.let { binding->
            presenter.muteMicrophoneToggled(binding.callMicBtn.isChecked)
            binding.callMicBtn.setImageResource(if (binding.callMicBtn.isChecked) R.drawable.baseline_mic_off_24 else R.drawable.baseline_mic_24)
        }
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

    fun cameraFlip() {
        presenter.switchVideoInputClick()
    }

    fun addParticipant() {
        presenter.startAddParticipant()
    }

    override fun startAddParticipant(conferenceId: String) {
        startActivityForResult(Intent(Intent.ACTION_PICK)
                .setClass(requireActivity(), ConversationSelectionActivity::class.java)
                .putExtra(KEY_CONF_ID, conferenceId),
            REQUEST_CODE_ADD_PARTICIPANT)
    }

    override fun toggleCallMediaHandler(id: String, callId: String, toggle: Boolean) {
        JamiService.toggleCallMediaHandler(id, callId, toggle)
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

    override fun displayPluginsButton(): Boolean {
        return JamiService.getPluginsEnabled() && JamiService.getCallMediaHandlers().size > 0
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reset the padding of the RecyclerPicker on each
        rp!!.setFirstLastElementsWidths(112, 112)
        binding!!.recyclerPicker.visibility = View.GONE
        if (isChoosePluginMode) {
            displayHangupButton(false)
            binding!!.recyclerPicker.visibility = View.VISIBLE
            movePreview(true)
            if (previousPluginPosition != -1) {
                rp!!.scrollToPosition(previousPluginPosition)
            }
        } else {
            movePreview(false)
        }
    }

    fun toggleVideoPluginsCarousel(toggle: Boolean) {
        if (isChoosePluginMode) {
            if (toggle) {
                binding!!.recyclerPicker.visibility = View.VISIBLE
                movePreview(true)
            } else {
                binding!!.recyclerPicker.visibility = View.INVISIBLE
                movePreview(false)
            }
        }
    }

    fun movePreview(up: Boolean) {
        // Move the preview container (cardview) by a certain margin
        if (up) {
            animation.setIntValues(12, 128)
        } else {
            animation.setIntValues(128, 12)
        }
        animation.start()
    }

    /**
     * Function that is called to show/hide the plugins recycler viewer and update UI
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    fun displayVideoPluginsCarousel() {
        isChoosePluginMode = !isChoosePluginMode
        val context: Context = requireActivity()

        // Create callMediaHandlers and videoPluginsItems in a lazy manner
        if (pluginsModeFirst) {
            // Init
            val callMediaHandlers = JamiService.getCallMediaHandlers()
            val videoPluginsItems: MutableList<Drawable> = ArrayList(callMediaHandlers.size + 1)
            videoPluginsItems.add(context.getDrawable(R.drawable.baseline_cancel_24)!!)
            // Search for plugin call media handlers icons
            // If a call media handler doesn't have an icon use a standard android icon
            for (callMediaHandler in callMediaHandlers) {
                val details = getCallMediaHandlerDetails(callMediaHandler)
                var drawablePath = details["iconPath"]
                if (drawablePath != null && drawablePath.endsWith("svg")) drawablePath =
                    drawablePath.replace(".svg", ".png")
                val handlerIcon = Drawable.createFromPath(drawablePath) ?: context.getDrawable(R.drawable.ic_jami)!!
                videoPluginsItems.add(handlerIcon)
            }
            rp!!.updateData(videoPluginsItems)
            pluginsModeFirst = false
        }
        if (isChoosePluginMode) {
            // hide hang up button and other call buttons
            displayHangupButton(false)
            // Display the plugins recyclerpicker
            binding!!.recyclerPicker.visibility = View.VISIBLE
            movePreview(true)

            // Start loading the first or previous plugin if one was active
            if (callMediaHandlers!!.isNotEmpty()) {
                // If no previous plugin was active, take the first, else previous
                val position: Int
                if (previousPluginPosition < 1) {
                    rp!!.scrollToPosition(1)
                    position = 1
                    previousPluginPosition = 1
                } else {
                    position = previousPluginPosition
                }
                val callMediaId = callMediaHandlers!![position - 1]
                presenter.startPlugin(callMediaId)
            }
        } else {
            if (previousPluginPosition > 0) {
                val callMediaId = callMediaHandlers!![previousPluginPosition - 1]
                presenter.toggleCallMediaHandler(callMediaId, false)
                rp!!.scrollToPosition(previousPluginPosition)
            }
            presenter.stopPlugin()
            binding!!.recyclerPicker.visibility = View.GONE
            movePreview(false)
            displayHangupButton(true)
        }

        //change preview image
        displayVideoSurface(true, true)
    }

    /**
     * Called whenever a plugin drawable in the recycler picker is clicked or scrolled to
     */
    override fun onItemSelected(position: Int) {
        Log.i(TAG, "selected position: $position")
        /* If there was a different plugin before, unload it
         * If previousPluginPosition = -1 or 0, there was no plugin
         */if (previousPluginPosition > 0) {
            val callMediaId = callMediaHandlers!![previousPluginPosition - 1]
            presenter.toggleCallMediaHandler(callMediaId, false)
        }
        if (position > 0) {
            previousPluginPosition = position
            val callMediaId = callMediaHandlers!![position - 1]
            presenter.toggleCallMediaHandler(callMediaId, true)
        }
    }

    /**
     * Called whenever a plugin drawable in the recycler picker is clicked
     */
    override fun onItemClicked(position: Int) {
        Log.i(TAG, "selected position: $position")
        if (position == 0) {
            /* If there was a different plugin before, unload it
             * If previousPluginPosition = -1 or 0, there was no plugin
             */
            if (previousPluginPosition > 0) {
                val callMediaId = callMediaHandlers!![previousPluginPosition - 1]
                presenter.toggleCallMediaHandler(callMediaId, false)
                rp!!.scrollToPosition(previousPluginPosition)
            }
            val callActivity = activity as CallActivity?
            callActivity?.showSystemUI()
            toggleVideoPluginsCarousel(false)
            displayVideoPluginsCarousel()
        }
    }

    private val blinkingAnimation: Animation
        get() {
            return AlphaAnimation(1f, 0f).apply {
                duration = 400
                interpolator = LinearInterpolator()
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
        }

    companion object {
        val TAG = CallFragment::class.simpleName!!
        const val ACTION_PLACE_CALL = "PLACE_CALL"
        const val ACTION_GET_CALL = "GET_CALL"
        const val KEY_ACTION = "action"
        const val KEY_CONF_ID = "confId"
        const val KEY_AUDIO_ONLY = "AUDIO_ONLY"
        private const val REQUEST_CODE_ADD_PARTICIPANT = 6
        private const val REQUEST_PERMISSION_INCOMING = 1003
        private const val REQUEST_PERMISSION_OUTGOING = 1004
        private const val REQUEST_CODE_SCREEN_SHARE = 7

        fun newInstance(action: String, path: ConversationPath?, contactId: String?, audioOnly: Boolean): CallFragment {
            val bundle = Bundle()
            bundle.putString(KEY_ACTION, action)
            path?.toBundle(bundle)
            bundle.putString(Intent.EXTRA_PHONE_NUMBER, contactId)
            bundle.putBoolean(KEY_AUDIO_ONLY, audioOnly)
            val countDownFragment = CallFragment()
            countDownFragment.arguments = bundle
            return countDownFragment
        }

        fun newInstance(action: String, confId: String?): CallFragment {
            val bundle = Bundle()
            bundle.putString(KEY_ACTION, action)
            bundle.putString(KEY_CONF_ID, confId)
            val countDownFragment = CallFragment()
            countDownFragment.arguments = bundle
            return countDownFragment
        }

        fun callStateToHumanState(state: CallStatus?): Int {
            return when (state) {
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
        }
    }
}