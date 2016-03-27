package cx.ring.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Service to handle view notifications. This allows the sample sync adapter to update the
 * information when the contact is being looked at
 */
public class NotifierService extends IntentService {
    private static final String TAG = "NotifierService";
    public NotifierService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // In reality, we would write some data (e.g. a high-res picture) to the contact.
        // for this demo, we just write a line to the log
        Log.i(TAG, "Contact opened: " + intent.getData());
    }
}
