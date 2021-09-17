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
import androidx.appcompat.app.AppCompatActivity
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.fragments.CallFragment
import cx.ring.utils.ConversationPath.Companion.fromIntent
import cx.ring.utils.KeyboardVisibilityManager
import cx.ring.utils.MediaButtonsHelper
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.NotificationService

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    private var mMainView: View? = null
    private var handler: Handler? = null
    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT
    private var dimmed = false
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
            setOnClickListener {
                dimmed = !dimmed
                if (dimmed)
                    hideSystemUI()
                else
                    showSystemUI()
            }
        }
        intent?.let { handleNewIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        restartNoInteractionTimer()
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
        if (Intent.ACTION_CALL == action || ACTION_CALL == action) {
            val audioOnly = intent.getBooleanExtra(CallFragment.KEY_AUDIO_ONLY, true)
            val contactId = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            val callFragment = CallFragment.newInstance(
                CallFragment.ACTION_PLACE_CALL,
                fromIntent(intent),
                contactId,
                audioOnly
            )
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
        } else if (Intent.ACTION_VIEW == action || ACTION_CALL_ACCEPT == action) {
            val confId = intent.getStringExtra(NotificationService.KEY_CALL_ID)
            val callFragment = CallFragment.newInstance(
                if (Intent.ACTION_VIEW == action) CallFragment.ACTION_GET_CALL else ACTION_CALL_ACCEPT,
                confId
            )
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
        }
    }

    private val onNoInteraction = Runnable {
        if (!dimmed) {
            dimmed = true
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
        val callFragment = callFragment
        callFragment?.onUserLeave()
    }

    override fun onUserInteraction() {
        restartNoInteractionTimer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (currentOrientation != newConfig.orientation) {
            currentOrientation = newConfig.orientation
            if (dimmed) hideSystemUI() else showSystemUI()
        } else {
            restartNoInteractionTimer()
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun hideSystemUI() {
        KeyboardVisibilityManager.hideKeyboard(this)
        mMainView?.let { mainView ->
            mainView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            val callFragment = callFragment
            if (callFragment != null && !callFragment.isChoosePluginMode) {
                callFragment.toggleVideoPluginsCarousel(false)
            }
            handler?.removeCallbacks(onNoInteraction)
        }
    }

    fun showSystemUI() {
        mMainView?.let { mainView ->
            mainView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            callFragment?.toggleVideoPluginsCarousel(true)
            restartNoInteractionTimer()
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
            var callFragment: CallFragment? = null
            // Get the call Fragment
            val fragment = supportFragmentManager.findFragmentByTag(CALL_FRAGMENT_TAG)
            if (fragment is CallFragment) {
                callFragment = fragment
            }
            return callFragment
        }

    companion object {
        const val ACTION_CALL = BuildConfig.APPLICATION_ID + ".action.call"
        const val ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT"
        private const val CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG"

        /* result code sent in case of call failure */
        var RESULT_FAILURE = -10
    }
}