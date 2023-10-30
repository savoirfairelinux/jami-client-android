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
package cx.ring.client

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityConversationBinding
import cx.ring.fragments.ConversationFragment
import cx.ring.services.NotificationServiceImpl
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {
    private var mConversationFragment: ConversationFragment? = null
    private lateinit var conversationPath: ConversationPath
    private var mPendingIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent?.action
        val path: ConversationPath? = when {
            intent != null -> ConversationPath.fromIntent(intent)
            savedInstanceState != null -> ConversationPath.fromBundle(savedInstanceState)
            else -> null
        }
        if (path == null) {
            finish()
            return
        }

        conversationPath = path
        val isBubble = getIntent().getBooleanExtra(NotificationServiceImpl.EXTRA_BUBBLE, false)
        JamiApplication.instance?.startDaemon(this)
        val binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (mConversationFragment == null) {
            mConversationFragment = ConversationFragment().apply {
                arguments = conversationPath.toBundle().apply {
                    putBoolean(NotificationServiceImpl.EXTRA_BUBBLE, isBubble)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, mConversationFragment!!, null)
                .commitNow()
        }
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action || Intent.ACTION_VIEW == action) {
            mPendingIntent = intent
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        if (!DeviceUtils.isTablet(this)) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onContextMenuClosed(menu: Menu) {
        mConversationFragment?.updateAdapterItem()
        super.onContextMenuClosed(menu)
    }

    override fun onStart() {
        super.onStart()
        mPendingIntent?.let { pendingIntent ->
            handleShareIntent(pendingIntent)
            mPendingIntent = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        mConversationFragment?.handleShareIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        conversationPath.toBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                mConversationFragment?.sendMessageText()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}