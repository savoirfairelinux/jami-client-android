/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
package cx.ring.client

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityConversationBinding
import cx.ring.fragments.ConversationFragment
import cx.ring.interfaces.Colorable
import cx.ring.services.NotificationServiceImpl
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity(), Colorable {
    private var mConversationFragment: ConversationFragment? = null
    private lateinit var conversationPath: ConversationPath
    private var mPendingIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent?.action
        var path: ConversationPath? = null
        if (intent != null) {
            path = ConversationPath.fromIntent(intent)
        } else if (savedInstanceState != null) {
            path = ConversationPath.fromBundle(savedInstanceState)
        }
        if (path == null) {
            finish()
            return
        }
        conversationPath = path;
        val isBubble = getIntent().getBooleanExtra(NotificationServiceImpl.EXTRA_BUBBLE, false)
        JamiApplication.instance!!.startDaemon()
        val binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)
        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
        binding.contactImage.setOnClickListener { v: View? -> if (mConversationFragment != null) mConversationFragment!!.openContact() }
        if (mConversationFragment == null) {
            val bundle = conversationPath.toBundle()
            bundle.putBoolean(NotificationServiceImpl.EXTRA_BUBBLE, isBubble)
            mConversationFragment = ConversationFragment()
            mConversationFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, mConversationFragment!!, null)
                .commitNow()
        }
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action || Intent.ACTION_VIEW == action) {
            mPendingIntent = intent
        }
    }

    override fun onContextMenuClosed(menu: Menu) {
        mConversationFragment!!.updateAdapterItem()
        super.onContextMenuClosed(menu)
    }

    override fun onStart() {
        super.onStart()
        if (mPendingIntent != null) {
            handleShareIntent(mPendingIntent!!)
            mPendingIntent = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        if (mConversationFragment != null) mConversationFragment!!.handleShareIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        conversationPath.toBundle(outState)
        super.onSaveInstanceState(outState)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (mConversationFragment != null) mConversationFragment!!.sendMessageText()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun setColor(@ColorInt color: Int) {
        //colouriseToolbar(binding.mainToolbar, color);
        //mToolbar.setBackground(new ColorDrawable(color));
        //getWindow().setStatusBarColor(color);
    }
}