/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.loaders;

import java.util.Map;

import cx.ring.service.IDRingService;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class AccountsStateLoader extends AsyncTaskLoader<Map<String, String>> {

    private static final String TAG = AccountsStateLoader.class.getSimpleName();
    public static final String ACCOUNTS = "accounts";
    public static final String ACCOUNT_IP2IP = "IP2IP";
    final IDRingService service;
    final String accountId;

    public AccountsStateLoader(Context context, IDRingService ref, String accId) {
        super(context);
        service = ref;
        accountId = accId;
        Log.i(TAG, "new AccountsStateLoader " + accId);
    }

    /****************************************************/
    /** (1) A task that performs the asynchronous load **/
    /****************************************************/

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> loadInBackground() {
        Log.i(TAG, "AccountsStateLoader loadInBackground " + accountId);
        Map<String, String> result = null;
        try {
            result = service.getVolatileAccountDetails(accountId);
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, e.toString());
        }
        for (Map.Entry<String, String> entry : result.entrySet()) {
            Log.i(TAG, "AccountsStateLoader loadInBackground " + entry.getKey() + " -> " + entry.getValue());
        }
        return result;
    }

    /********************************************************/
    /** (2) Deliver the results to the registered listener **/
    /********************************************************/
/*
    @Override
    public void deliverResult(Bundle data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        Bundle oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }
*/
    /*********************************************************/
    /** (3) Implement the Loader’s state-dependent behavior **/
    /*********************************************************/
/*
    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(Bundle data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(Bundle data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }*/
}
