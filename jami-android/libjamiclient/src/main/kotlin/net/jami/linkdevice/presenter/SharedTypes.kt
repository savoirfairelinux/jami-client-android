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