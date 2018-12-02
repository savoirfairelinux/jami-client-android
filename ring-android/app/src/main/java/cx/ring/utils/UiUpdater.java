package cx.ring.utils;

import android.os.Handler;
import android.os.Looper;

public class UiUpdater {
        // Create a Handler that uses the Main Looper to run in
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private final Runnable mStatusChecker;
        private final int UPDATE_INTERVAL;

        public UiUpdater(final Runnable uiUpdater, int interval) {
            UPDATE_INTERVAL = interval;
            mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    // Run the passed runnable
                    uiUpdater.run();
                    // Re-run it after the update interval
                    mHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            };
        }

        public UiUpdater(Runnable uiUpdater){
            this(uiUpdater, 1000);
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
