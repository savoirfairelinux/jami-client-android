/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.utils

import net.jami.services.LogService

object Log {
    private lateinit var mLogService: LogService

    fun injectLogService(service: LogService) {
        mLogService = service
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        mLogService.d(tag, message)
    }

    fun e(tag: String, message: String) {
        mLogService.e(tag, message)
    }

    fun i(tag: String, message: String) {
        mLogService.i(tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        mLogService.w(tag, message)
    }

    fun d(tag: String, message: String, e: Throwable) {
        mLogService.d(tag, message, e)
    }

    @JvmStatic
    fun e(tag: String, message: String, e: Throwable) {
        mLogService.e(tag, message, e)
    }

    fun i(tag: String, message: String, e: Throwable) {
        mLogService.i(tag, message, e)
    }

    fun w(tag: String, message: String, e: Throwable) {
        mLogService.w(tag, message, e)
    }
}