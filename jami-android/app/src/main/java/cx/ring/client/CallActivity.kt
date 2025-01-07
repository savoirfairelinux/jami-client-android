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
package cx.ring.client

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityCallLayoutBinding
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
    private var binding: ActivityCallLayoutBinding? = null
    private var handler: Handler? = null
    private var areControlsVisible = false

    // Get the callFragment.
    private val callFragment: CallFragment?
        get() = supportFragmentManager.findFragmentByTag(CALL_FRAGMENT_TAG) as CallFragment?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val presenter = callFragment?.presenter
                if (presenter?.mOnGoingCall == true && presenter.isVideoActive()) {
                    presenter.requestPipMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        JamiApplication.instance?.startDaemon(this)

        // Setup the activity to be shown on locked screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION") // Deprecated in API 27.
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        handler = Handler(Looper.getMainLooper())

        // Init the layout.
        binding = ActivityCallLayoutBinding.inflate(layoutInflater).apply {
            setContentView(root)

            WindowInsetsControllerCompat(window, root).hide(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, false)

            root.setOnClickListener { if (!areControlsVisible) hideSystemUI() else showSystemUI() }
        }

        intent?.let { handleNewIntent(it) }
    }

    override fun onResume() {
        super.onResume()
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
                val callFragment = CallFragment
                    .newInstance(action, fromIntent(intent), contactId, wantVideo, acceptOption)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
            }
            Intent.ACTION_VIEW,
            DRingService.ACTION_CALL_ACCEPT -> {
                val currentId = callFragment?.arguments?.getString(NotificationService.KEY_CALL_ID)
                if (currentId != confId) {
                    val callFragment = CallFragment
                        .newInstance(action, confId, wantVideo, acceptOption)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_call_layout, callFragment, CALL_FRAGMENT_TAG).commit()
                } else if (action != Intent.ACTION_VIEW) {
                    callFragment?.handleAcceptIntent(acceptOption, confId, wantVideo)
                }
            }
        }
    }

    private val onNoInteraction = Runnable { if (!areControlsVisible) hideSystemUI() }

    // Restart the timer for hiding the system UI.
    private fun restartNoInteractionTimer() {
        handler?.let { handler ->
            handler.removeCallbacks(onNoInteraction)
            handler.postDelayed(onNoInteraction, 5L * 1000L)
        }
    }

    override fun onUserInteraction() {
        restartNoInteractionTimer()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val fragment = callFragment
        if (fragment != null && MediaButtonsHelper.handleMediaKeyCode(keyCode, fragment))
            return true
        return super.onKeyDown(keyCode, event)
    }

    public override fun onUserLeaveHint() {
        callFragment?.onUserLeave()
    }

    fun hideSystemUI() {
        val callFragment = callFragment ?: return
        KeyboardVisibilityManager.hideKeyboard(this)
        callFragment.moveBottomSheet(CallFragment.BottomSheetAnimation.DOWN)
        areControlsVisible = true
        binding?.root?.apply {
            WindowInsetsControllerCompat(window, this).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
        handler?.removeCallbacks(onNoInteraction)
    }

    fun showSystemUI() {
        val callFragment = callFragment ?: return
        areControlsVisible = false
        binding?.root?.apply {
            WindowInsetsControllerCompat(window, this)
                .show(WindowInsetsCompat.Type.statusBars())
            callFragment.moveBottomSheet(CallFragment.BottomSheetAnimation.UP)
            restartNoInteractionTimer()
        }
    }

    companion object {
        private val TAG = CallActivity::class.simpleName!!
        private const val CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG"
    }
}
