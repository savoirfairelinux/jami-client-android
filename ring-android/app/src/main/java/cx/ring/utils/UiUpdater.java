package cx.ring.utils;

import android.os.Handler;
import android.os.Looper;

import net.jami.utils.Log;

public class UiUpdater {
        private static final String TAG = UiUpdater.class.getSimpleName();

        // Create a Handler that uses the Main Looper to run in
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private final Runnable mStatusChecker;
        private final long UPDATE_INTERVAL;

        public UiUpdater(final Runnable uiUpdater, long interval) {
            UPDATE_INTERVAL = interval;
            mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    // Run the passed runnable
                    try {
                        uiUpdater.run();
                        // Re-run it after the update interval
                        mHandler.postDelayed(this, UPDATE_INTERVAL);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception running task");
                    }
                }
            };
        }

        public UiUpdater(Runnable uiUpdater){
            this(uiUpdater, 1000L);
        }

        /**
         * Starts the periodical update routine (mStatusChecker 
         * adds the callback to the handler).
         */
        public synchronized void start(){
            mStatusChecker.run();
        }

        /**
         * Stops the periodical update routine from running,
         * by removing the callback.
         */
        public synchronized void stop(){
            mHandler.removeCallbacks(mStatusChecker);
        }
}
