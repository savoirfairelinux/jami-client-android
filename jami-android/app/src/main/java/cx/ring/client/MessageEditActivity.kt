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
