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
package net.jami.linkdevice.presenter

interface AuthStateListener {
    fun onInitSignal() { // Should not be received since there is nothing to do.
        throw UnsupportedOperationException()
    }

    fun onTokenAvailableSignal(details: Map<String, String>)
    fun onConnectingSignal()
    fun onAuthenticatingSignal(details: Map<String, String>)
    fun onInProgressSignal()
    fun onDoneSignal(details: Map<String, String>)
}

enum class AuthError {
    NETWORK,
    AUTHENTICATION,
    UNKNOWN;

    companion object {
        fun fromString(value: String) = when (value) {
            "network" -> NETWORK
            "authentication" -> AUTHENTICATION
            else -> UNKNOWN
        }
    }
}

enum class AuthState(val value: Int) {
    INIT(0),
    TOKEN_AVAILABLE(1),
    CONNECTING(2),
    AUTHENTICATING(3),
    IN_PROGRESS(4),
    DONE(5);

    companion object {
        fun fromInt(value: Int) = AuthState.entries[value]
    }
}

data class AuthResult(
    val accountId: String,
    val state: AuthState,
    val details: Map<String, String> = emptyMap(),
    val operationId: Long? = null
)