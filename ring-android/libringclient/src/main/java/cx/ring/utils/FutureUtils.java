/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FutureUtils {

    private final static String TAG = FutureUtils.class.getName();

    public static <T> T executeDaemonThreadCallable (ExecutorService executor,
                                                     long daemonThreadId,
                                                     boolean isSynchronous,
                                                     Callable<T> callable
                                                     ) {
        long currentThreadId = Thread.currentThread().getId();
        if (currentThreadId == daemonThreadId) {
            // we already are in the daemon thread
            try {
                return callable.call();
            } catch (Exception e) {
                return null;
            }
        }

        // we are not in the Daemon Thread
        // the dedicated daemon executor is required
        Future<T> result = executor.submit(callable);

        if (isSynchronous) {
            return getFutureResult(result);
        }

        return null;
    }

    public static <T> T getFutureResult(Future<T> future) {

        if (future == null) {
            return null;
        }

        try {
            return future.get();
        } catch (Exception e) {
            Log.e(TAG, "Error while unwrapping future", e);
            return null;
        }
    }

}
