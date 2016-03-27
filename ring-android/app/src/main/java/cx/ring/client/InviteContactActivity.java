package cx.ring.client;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import cx.ring.R;

/**
 * Activity to handle the invite-intent. In a real app, this would look up the user on the network
 * and either connect ("add as friend", "follow") or invite them to the network
 */
public class InviteContactActivity extends Activity {
    private static final String TAG = "InviteContactActivity";
    private TextView mUriTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_contact);
        mUriTextView = (TextView) findViewById(R.id.invite_contact_uri);
        mUriTextView.setText(getIntent().getDataString());
    }
}
