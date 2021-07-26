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
package net.jami.utils;

import net.jami.services.LogService;

public class Log {
    private static LogService mLogService;

    public static void injectLogService(LogService service) {
        mLogService = service;
    }

    public static void d(String tag, String message) {
        mLogService.d(tag, message);
    }

    public static void e(String tag, String message) {
        mLogService.e(tag, message);
    }

    public static void i(String tag, String message) {
        mLogService.i(tag, message);
    }

    public static void w(String tag, String message) {
        mLogService.w(tag, message);
    }

    public static void d(String tag, String message, Throwable e) {
        mLogService.d(tag, message, e);
    }

    public static void e(String tag, String message, Throwable e) {
        mLogService.e(tag, message, e);
    }

    public static void i(String tag, String message, Throwable e) {
        mLogService.i(tag, message, e);
    }

    public static void w(String tag, String message, Throwable e) {
        mLogService.w(tag, message, e);
    }

}
