package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.services.SharedPreferencesService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Inject
    SharedPreferencesService mSharedPreferencesService;

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            ((RingApplication) context.getApplicationContext()).getRingInjectionComponent().inject(this);
            boolean isAllowRingOnStartup = mSharedPreferencesService.loadSettings().isAllowRingOnStartup();

            if (isAllowRingOnStartup) {
                Log.w(TAG, "Starting Ring on boot");
                Intent serviceIntent = new Intent(context, LocalService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
