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