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
package cx.ring.tests.services;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.services.DeviceRuntimeService;

public class DeviceRuntimeServiceImpl extends DeviceRuntimeService {

    private static final String TAG = DeviceRuntimeServiceImpl.class.getName();

    @Inject
    ExecutorService mExecutor;

    @Override
    public void loadNativeLibrary() {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                try {
                    System.loadLibrary("ring");
                } catch (Exception e1) {
                    System.err.println("Could not load Ring library");
                    return false;
                }
                return true;
            }
        });

        try {
            result.get();
            System.err.println("Ring library has been successfully loaded");
        } catch (Exception e) {
            System.err.println("Could not load Ring library");
        }
    }

    @Override
    public File provideFilesDir() {
        return new File("");
    }

    @Override
    public String provideDefaultVCardName() {
        return "Test";
    }
}
