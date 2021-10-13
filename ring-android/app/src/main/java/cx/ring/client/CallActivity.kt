/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.fragments.CallFragment
import cx.ring.utils.ConversationPath.Companion.fromIntent
import cx.ring.utils.KeyboardVisibilityManager
import cx.ring.utils.MediaButtonsHelper
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.NotificationService
import net.jami.utils.Log

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    private var mMainView: View? = null
    private var handler: Handler? = null
    private var currentOrientation = 1
    private var isFullscreen = false
    private var navBarBottomInset = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        JamiApplication.instance?.startDaemon()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        setContentView(R.layout.activity_call_layout)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        handler = Handler(Looper.getMainLooper())
        mMainView = findViewById<View>(R.id.main_call_layout)?.apply {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            setOnClickListener {
                isFullscreen = !isFullscreen
                if (isFullscreen) {
                    hideSystemUI()
                } else {
                    showSystemUI()
                }
            }
        }
        intent?.let { handleNewIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        restartNoInteractionTimer()
        currentOrientation = resources.configuration.orientation
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStop() {
        super.onStop()
        handler?.removeCallbacks(onNoInteraction)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNewIntent(intent)
    }

    private fun handleNewIntent(intent: Intent) {
        val action = intent.action
        val wantVideo = intent.getBooleanExtra(CallFragment.KEY_HAS_VIDEO, false)
        val confId = intent.getStringExtra(NotificationService.KEY_CALL_ID)
        when(action){
            Intent.ACTION_CALL -> {
                val contactId = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                val callFragment = CallFragment.newInstance(action, fromIntent(intent), contactId, wantVideo)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
            }
            Intent.ACTION_VIEW, ACTION_CALL_ACCEPT -> {
                val currentId = callFragment?.arguments?.getString(NotificationService.KEY_CALL_ID)
                if (currentId != confId) {
                            val callFragment = CallFragment.newInstance(action, confId, wantVideo)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
                }
            }
        }
    }

    private val onNoInteraction = Runnable {
        if (!isFullscreen) {
            isFullscreen = true
            hideSystemUI()
        }
    }

    private fun restartNoInteractionTimer() {
        handler?.let { handler ->
            handler.removeCallbacks(onNoInteraction)
            handler.postDelayed(onNoInteraction, (4 * 1000).toLong())
        }
    }

    public override fun onUserLeaveHint() {
        callFragment?.onUserLeave()
    }

    override fun onUserInteraction() {
        restartNoInteractionTimer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.w(CallFragment.TAG, "DEBUG onConfigurationChanged ------------ currentOrientation: $currentOrientation ?? newConfig.orientation: ${newConfig.orientation}; fullscreen: $isFullscreen")
        if (currentOrientation != newConfig.orientation) {
            currentOrientation = newConfig.orientation
            if (isFullscreen) hideSystemUI() else showSystemUI()
        } else {
            restartNoInteractionTimer()
        }
    }

    private fun hideSystemUI() {
        val callFragment = callFragment ?: return
        KeyboardVisibilityManager.hideKeyboard(this)
        callFragment.resetBottomSheetState()
        moveBottomSheet(BottomSheetAnimation.DOWN)
        if (!callFragment.isChoosePluginMode) {
            callFragment.toggleVideoPluginsCarousel(false)
        }
        handler?.removeCallbacks(onNoInteraction)
    }

    fun showSystemUI() {
        val callFragment = callFragment ?: return
        mMainView?.let { mainView ->
            if (currentOrientation != 1) {
                WindowInsetsControllerCompat(window, mainView).show(WindowInsetsCompat.Type.statusBars())
                WindowInsetsControllerCompat(window, mainView).hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                WindowInsetsControllerCompat(window, mainView).show(WindowInsetsCompat.Type.systemBars())
            }
            moveBottomSheet(BottomSheetAnimation.UP)
            callFragment.toggleVideoPluginsCarousel(true)
            restartNoInteractionTimer()
        }
    }

    enum class BottomSheetAnimation {
        UP, DOWN
    }

    private fun moveBottomSheet(movement: BottomSheetAnimation) {
        val dm = resources.displayMetrics

        mMainView?.let { mainView ->
            val topInset = ViewCompat.getRootWindowInsets(mainView)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())?.top!!
            val bottomInset = ViewCompat.getRootWindowInsets(mainView)
                ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())?.bottom ?: return

            when (movement) {
                BottomSheetAnimation.UP -> {
                    mainView.findViewById<View>(R.id.call_coordinator_option_container).let {
                        it.updatePadding(bottom = if (bottomInset == 0 || currentOrientation != 1 ) 0 else bottomInset)
                        mainView.findViewById<View>(R.id.call_options_bottom_sheet)
                            .updatePadding(bottom = if(bottomInset == 0 || currentOrientation != 1) (topInset*dm.density).toInt() else (bottomInset * dm.density).toInt() )
                        it.animate()
                            .translationY(0f)
                            .alpha(1.0f)
                            .setListener(null)
                    }
                    callFragment?.setBottomSheet(true)
                }
                BottomSheetAnimation.DOWN -> {
                    mainView.findViewById<View>(R.id.call_coordinator_option_container).let {
                        WindowInsetsControllerCompat(window, mainView).let { controller ->
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                        it.animate()
                            .translationY(250f)
                            .alpha(0.0f)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    super.onAnimationEnd(animation)
                                    it.updatePadding(bottom = 0)
                                    mainView.findViewById<View>(R.id.call_options_bottom_sheet)
                                        .updatePadding(bottom = 0)
                                    callFragment?.setBottomSheet(false)
                                }
                            })
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = callFragment
        if (fragment != null && MediaButtonsHelper.handleMediaKeyCode(keyCode, fragment))
            return true
        return super.onKeyDown(keyCode, event)
    }

    // Get the call Fragment
    private val callFragment: CallFragment?
        get() {
            return supportFragmentManager.findFragmentByTag(CALL_FRAGMENT_TAG) as CallFragment?
        }

    companion object {
        private val TAG = CallActivity::class.simpleName!!
        const val ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT"
        private const val CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG"

        /* result code sent in case of call failure */
        var RESULT_FAILURE = -10
    }
}