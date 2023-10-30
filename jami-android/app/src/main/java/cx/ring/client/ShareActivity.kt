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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import cx.ring.utils.ConversationPath
import cx.ring.R
import cx.ring.application.JamiApplication
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val extra = intent.extras
        if (ConversationPath.fromBundle(extra) != null) {
            intent.setClass(this, ConversationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        JamiApplication.instance?.startDaemon(this)
        setContentView(R.layout.activity_share)
    }
}