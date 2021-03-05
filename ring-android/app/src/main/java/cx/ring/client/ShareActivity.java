package cx.ring.client;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import cx.ring.R;
import cx.ring.utils.ConversationPath;

public class ShareActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle extra = intent.getExtras();
        if (ConversationPath.fromBundle(extra) != null) {
            intent.setClass(this, ConversationActivity.class);
            startActivity(intent);
            finishAndRemoveTask();
            return;
        }
        setContentView(R.layout.activity_share);
    }

}
