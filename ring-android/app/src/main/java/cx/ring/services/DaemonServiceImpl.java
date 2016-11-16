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

import javax.inject.Inject;

import cx.ring.R;

public class DaemonServiceImpl extends DaemonService{

    @Inject
    Context mContext;

    @Override
    public void loadNativeLibrary() {
        System.loadLibrary("ring");
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
