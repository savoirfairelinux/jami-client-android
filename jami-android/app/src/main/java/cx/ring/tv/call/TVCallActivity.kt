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
package cx.ring.tv.call

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import cx.ring.R
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import net.jami.services.NotificationService
import net.jami.call.CallView
import android.view.MotionEvent
import cx.ring.application.JamiApplication
import cx.ring.utils.ConversationPath

@AndroidEntryPoint
class TVCallActivity : FragmentActivity() {
    private var callFragment: TVCallFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        setContentView(R.layout.tv_activity_call)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        JamiApplication.instance?.startDaemon(this)
        val path = ConversationPath.fromIntent(intent)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        if (path != null) {
            Log.d(TAG, "onCreate: outgoing call $path ${intent.action}")
            callFragment = TVCallFragment.newInstance(intent.action!!, path.accountId, path.conversationId,
                intent.extras!!.getString(Intent.EXTRA_PHONE_NUMBER, path.conversationId),  true)
            fragmentTransaction.replace(R.id.main_call_layout, callFragment!!, CALL_FRAGMENT_TAG)
                .commit()
        } else {
            Log.d(TAG, "onCreate: incoming call")
            val confId = getIntent().getStringExtra(NotificationService.KEY_CALL_ID)
            Log.d(TAG, "onCreate: conf $confId")
            callFragment = TVCallFragment.newInstance(Intent.ACTION_VIEW, confId)
            fragmentTransaction.replace(R.id.main_call_layout, callFragment!!, CALL_FRAGMENT_TAG)
                .commit()
        }
    }

    public override fun onUserLeaveHint() {
        val fragment = supportFragmentManager.findFragmentByTag(CALL_FRAGMENT_TAG)
        if (fragment is CallView) {
            val callFragment = fragment as CallView
            callFragment.onUserLeave()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        callFragment?.onKeyDown()
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        callFragment?.onKeyDown()
        return super.onTouchEvent(event)
    }

    companion object {
        val TAG = TVCallActivity::class.simpleName!!
        private const val CALL_FRAGMENT_TAG = "CALL_FRAGMENT_TAG"
    }
}