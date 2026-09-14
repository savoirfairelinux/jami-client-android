/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityConversationBinding
import cx.ring.fragments.BroadcastFragment
import dagger.hilt.android.AndroidEntryPoint

/** Hosts the screen that writes one message to every member of a channel. */
@AndroidEntryPoint
class BroadcastActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelId = intent?.getStringExtra(BroadcastFragment.KEY_CHANNEL_ID)
        val legacyName = intent?.getStringExtra(BroadcastFragment.KEY_CHANNEL)
        val accountId = intent?.getStringExtra(BroadcastFragment.KEY_ACCOUNT)
        if ((channelId.isNullOrEmpty() && legacyName.isNullOrEmpty()) || accountId.isNullOrEmpty()) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon(this)
        setContentView(ActivityConversationBinding.inflate(layoutInflater).root)
        if (savedInstanceState == null) {
            val fragment = if (channelId != null) BroadcastFragment.newInstance(channelId, accountId)
                else BroadcastFragment().apply {
                    arguments = Bundle().apply {
                        putString(BroadcastFragment.KEY_CHANNEL, legacyName)
                        putString(BroadcastFragment.KEY_ACCOUNT, accountId)
                    }
                }
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, fragment, null)
                .commitNow()
        }
    }
}
