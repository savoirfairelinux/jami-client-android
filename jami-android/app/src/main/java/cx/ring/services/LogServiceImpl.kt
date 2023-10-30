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
package cx.ring.services

import android.util.Log
import net.jami.services.LogService

class LogServiceImpl : LogService {
    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun e(tag: String, message: String, e: Throwable) {
        Log.e(tag, message, e)
    }

    override fun d(tag: String, message: String, e: Throwable) {
        Log.d(tag, message, e)
    }

    override fun w(tag: String, message: String, e: Throwable) {
        Log.w(tag, message, e)
    }

    override fun i(tag: String, message: String, e: Throwable) {
        Log.i(tag, message, e)
    }
}