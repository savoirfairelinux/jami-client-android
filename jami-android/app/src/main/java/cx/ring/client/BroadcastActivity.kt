/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.databinding.ActivityConversationBinding
import cx.ring.fragments.BroadcastFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the Circle broadcast screen (sends the message or media to every member of the Circle).
 */
@AndroidEntryPoint
class BroadcastActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val circleId = intent?.getStringExtra(BroadcastFragment.KEY_CIRCLE_ID)
        val accountId = intent?.getStringExtra(BroadcastFragment.KEY_ACCOUNT)
        if (circleId.isNullOrEmpty() || accountId.isNullOrEmpty()) {
            finish()
            return
        }
        JamiApplication.instance?.startDaemon(this)
        setContentView(ActivityConversationBinding.inflate(layoutInflater).root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, BroadcastFragment.newInstance(circleId, accountId), null)
                .commitNow()
        }
    }

    companion object {
        fun intent(context: Context, accountId: String, circleId: String): Intent =
            Intent(context, BroadcastActivity::class.java)
                .putExtra(BroadcastFragment.KEY_ACCOUNT, accountId)
                .putExtra(BroadcastFragment.KEY_CIRCLE_ID, circleId)
    }
}
