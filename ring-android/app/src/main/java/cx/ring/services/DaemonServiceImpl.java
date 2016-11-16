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
package cx.ring.services;

import android.content.Context;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.utils.Log;

public class DaemonServiceImpl extends DaemonService {

    private static final String TAG = DaemonServiceImpl.class.getName();

    @Inject
    Context mContext;

    @Override
    public void loadNativeLibrary() {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    System.loadLibrary("ring");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Could not load Ring library", e);
                    return false;
                }
            }
        });

        try {
            result.get();
            Log.i(TAG, "Ring library has been successfully loaded");
        } catch (Exception e) {
            Log.e(TAG, "Could not load Ring library", e);
        }
    }

    @Override
    public File provideFilesDir() {
        return mContext.getFilesDir();
    }

    @Override
    public String provideDefaultVCardName() {
        return mContext.getString(R.string.unknown);
    }
}
