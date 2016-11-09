package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import cx.ring.services.SettingsServiceImpl;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences appPrefs = c.getSharedPreferences(SettingsServiceImpl.RING_SETTINGS, Context.MODE_PRIVATE);
            boolean startOnBoot = appPrefs.getBoolean(SettingsServiceImpl.RING_ON_STARTUP, true);
            if (startOnBoot) {
                Log.w(TAG, "Starting Ring on boot");
                Intent serviceIntent = new Intent(c, LocalService.class);
                c.startService(serviceIntent);
            }
        }
    }
}
