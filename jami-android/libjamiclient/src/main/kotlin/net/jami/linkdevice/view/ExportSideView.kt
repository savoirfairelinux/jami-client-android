/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package net.jami.linkdevice.view

import net.jami.linkdevice.presenter.AuthError

enum class ExportSideInputError {
    INVALID_INPUT,
    NOT_FOUND_ON_NETWORK
}

enum class ExportSideResult {
    IN_PROGRESS,
    SUCCESS,
    FAILURE
}

interface ExportSideView {
    /**
     * Show the input screen.
     * @param error The error to show. Null if no error.
     */
    fun showInput(error: ExportSideInputError? = null)

    /**
     * Show the IP address of the device.
     * @param ip The IP address of the device.
     */
    fun showIP(ip: String)

    /**
     * Show the password protection screen.
     * Requires the user to enter a password on the other side.
     */
    fun showPasswordProtection()

    /**
     * Show the result of the operation.
     * @param result The result of the operation.
     * @param error The error that occurred. Null if no error.
     */
    fun showResult(result: ExportSideResult, error: AuthError? = null)
}