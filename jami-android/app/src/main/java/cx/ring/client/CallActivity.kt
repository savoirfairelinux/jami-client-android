/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.fragments.CallFragment
import cx.ring.service.DRingService
import cx.ring.utils.ConversationPath.Companion.fromIntent
import cx.ring.utils.KeyboardVisibilityManager
import cx.ring.utils.MediaButtonsHelper
import dagger.hilt.android.AndroidEntryPoint
import net.jami.call.CallPresenter
import net.jami.services.NotificationService

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    private var mMainView: View? = null
    private var handler: Handler? = null
    private var currentOrientation = 1
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(1) { onBackPressed() }
        JamiApplication.instance?.startDaemon(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContentView(R.layout.activity_call_layout)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        handler = Handler(Looper.getMainLooper())
        mMainView = findViewById<View>(R.id.main_call_layout)?.apply {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            setOnClickListener {
                if (!isFullscreen)
                    hideSystemUI()
                else
                    showSystemUI()
            }
        }
        intent?.let { handleNewIntent(it) }
    }

    override fun onBackPressed() {
        val presenter = callFragment?.presenter
        if (presenter?.mOnGoingCall == true && presenter.isVideoActive()) {
            presenter.requestPipMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
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
        val acceptOption = intent.getStringExtra(CallPresenter.KEY_ACCEPT_OPTION)
        when (action) {
            Intent.ACTION_CALL -> {
                val contactId = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                val callFragment = CallFragment.newInstance(action, fromIntent(intent), contactId, wantVideo, acceptOption)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
            }
            Intent.ACTION_VIEW,
            DRingService.ACTION_CALL_ACCEPT -> {
                val currentId = callFragment?.arguments?.getString(NotificationService.KEY_CALL_ID)
                if (currentId != confId) {
                    val callFragment = CallFragment.newInstance(action, confId, wantVideo, acceptOption)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
                } else if (action != Intent.ACTION_VIEW) {
                    callFragment?.handleAcceptIntent(acceptOption, confId, wantVideo)
                }
            }
        }
    }

    private val onNoInteraction = Runnable {
        if (!isFullscreen) {
            hideSystemUI()
        }
    }

    private fun restartNoInteractionTimer() {
        handler?.let { handler ->
            handler.removeCallbacks(onNoInteraction)
            handler.postDelayed(onNoInteraction, 5L * 1000L)
        }
    }

    override fun onUserInteraction() {
        restartNoInteractionTimer()
    }

    public override fun onUserLeaveHint() {
        callFragment?.onUserLeave()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (currentOrientation != newConfig.orientation) {
            currentOrientation = newConfig.orientation
            if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) && !isFullscreen) {
                mMainView?.let {
                    when (currentOrientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> WindowInsetsControllerCompat(window, it).hide(
                            WindowInsetsCompat.Type.systemBars()
                        )
                        Configuration.ORIENTATION_PORTRAIT -> WindowInsetsControllerCompat(window, it).show(
                            WindowInsetsCompat.Type.systemBars()
                        )
                    }
                }
            }
        }
    }

    fun hideSystemUI() {
        val callFragment = callFragment ?: return
        KeyboardVisibilityManager.hideKeyboard(this)
        callFragment.resetBottomSheetState()
        callFragment.moveBottomSheet(CallFragment.BottomSheetAnimation.DOWN)
        isFullscreen = true
        if (!callFragment.isChoosePluginMode) {
            //callFragment.toggleVideoPluginsCarousel(false)
        }
        handler?.removeCallbacks(onNoInteraction)
    }

    fun showSystemUI() {
        val callFragment = callFragment ?: return
        mMainView?.apply {
            if (currentOrientation != 1) {
                WindowInsetsControllerCompat(window, this).show(WindowInsetsCompat.Type.statusBars())
                WindowInsetsControllerCompat(window, this).hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                window.navigationBarColor = resources.getColor(R.color.color_bottom_sheet_background)
                WindowInsetsControllerCompat(window, this).show(WindowInsetsCompat.Type.systemBars())
            }
            callFragment.moveBottomSheet(CallFragment.BottomSheetAnimation.UP)
            //callFragment.toggleVideoPluginsCarousel(true)
            restartNoInteractionTimer()
        }
        isFullscreen = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = callFragment
        if (fragment != null && MediaButtonsHelper.handleMediaKeyCode(keyCode, fragment))
            return true
        return super.onKeyDown(keyCode, event)
    }

    // Get the call Fragment
    private val callFragment: CallFragment?
        get() = supportFragmentManager.findFragmentByTag(CALL_FRAGMENT_TAG) as CallFragment?

    companion object {
        private val TAG = CallActivity::class.simpleName!!
        private const val CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG"

        /* result code sent in case of call failure */
        var RESULT_FAILURE = -10
    }
}
