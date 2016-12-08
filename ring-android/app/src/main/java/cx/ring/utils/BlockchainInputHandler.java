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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;

import cx.ring.services.AccountService;

public class BlockchainInputHandler extends Thread {

    private static final String TAG = BlockchainInputHandler.class.getName();

    private static final String RINGID_PATTERN = "^[a-f0-9]{40}$";
    private static final int WAIT_DELAY= 2000;
    private static final int KILL_DELAY = 6000;

    private WeakReference<AccountService> mAccountService;
    private String mTextToLookup;

    private boolean mIsWaitingForInputs = false;
    private long mLastEnqueuedInputTimeStamp = -1;

    public BlockchainInputHandler(@NonNull WeakReference<AccountService> accountService) {
        mAccountService = accountService;
    }

    public void enqueueNextLookup(String text) {

        if (mAccountService.get() == null) {
            return;
        }

        mLastEnqueuedInputTimeStamp = System.currentTimeMillis();
        mTextToLookup = text;

        if (!mIsWaitingForInputs) {
            mIsWaitingForInputs = true;
            start();
        }
    }

    private boolean isRingId(String text) {
        return text.matches(RINGID_PATTERN);
    }

    @Override
    public void run() {
        while (mIsWaitingForInputs) {
            try {
                Thread.sleep(100);

                long timeFromLastEnqueuedInput = System.currentTimeMillis() - mLastEnqueuedInputTimeStamp;
                if (timeFromLastEnqueuedInput >= KILL_DELAY) {
                    // we've been waiting for a long time, stop the wait
                    // the next user input will trigger the next wait
                    mIsWaitingForInputs = false;
                } else if (timeFromLastEnqueuedInput >= WAIT_DELAY) {
                    // trigger the blockchain lookup
                    final AccountService accountService = mAccountService.get();
                    if (isRingId(mTextToLookup)) {
                        accountService.lookupAddress("", "", mTextToLookup);
                    } else {
                        accountService.lookupName("", "", mTextToLookup);
                    }
                    // stop the wait
                    mIsWaitingForInputs = false;
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Error while waiting for next Blockchain lookup", e);
            }
        }

    }
}