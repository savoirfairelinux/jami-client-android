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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import cx.ring.R

class MessageEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_edit)
        val editText = findViewById<EditText>(R.id.msg_input_txt)
        editText.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
        findViewById<View>(R.id.msg_send).setOnClickListener {
            it.isVisible = false
            setResult(RESULT_OK, Intent(Intent.ACTION_EDIT)
                .setData(intent.data)
                .putExtra(Intent.EXTRA_TEXT, editText.text.toString()))
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
