package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import cx.ring.R;
import cx.ring.fragments.SettingsFragment;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
            boolean startOnBoot = sharedPreferences.getBoolean(c.getString(R.string.pref_startOnBoot_key), true);
            if (startOnBoot) {
                Log.w(TAG, "Starting Ring on boot");
                Intent serviceIntent = new Intent(c, LocalService.class);
                c.startService(serviceIntent);
            }
        }
    }
}
