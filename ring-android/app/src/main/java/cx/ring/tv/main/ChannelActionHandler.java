package cx.ring.tv.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ChannelActionHandler extends BroadcastReceiver {
    private static final String TAG = ChannelActionHandler.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "onReceive " + intent);
    }
}
