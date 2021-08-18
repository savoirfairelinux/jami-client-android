/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.util.Log;

import net.jami.services.LogService;

public class LogServiceImpl implements LogService {

    public void e(String tag, String message) {
        Log.e(tag, message);
    }

    public void d(String tag, String message) {
        Log.d(tag, message);
    }

    public void w(String tag, String message) {
        Log.w(tag, message);
    }

    public void i(String tag, String message) {
        Log.i(tag, message);
    }

    public void e(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
    }

    public void d(String tag, String message, Throwable e) {
        Log.d(tag, message, e);
    }

    public void w(String tag, String message, Throwable e) {
        Log.w(tag, message, e);
    }

    public void i(String tag, String message, Throwable e) {
        Log.i(tag, message, e);
    }
}
