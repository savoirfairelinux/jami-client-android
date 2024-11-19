package net.jami.linkdevice.view

enum class ImportSideResult {
    IN_PROGRESS,
    SUCCESS,
    FAILURE
}

interface ImportSideView {
    /**
     * Show the authentication token.
     * @param authenticationUri The authentication token. Null if not yet available.
     */
    fun showAuthenticationUri(authenticationUri: String?)

    /**
     * Show action required meaning the user needs to do something on the other side.
     */
    fun showActionRequired()

    /**
     * Show the importing account identity.
     * @param needPassword Whether the account needs a password.
     * @param jamiId The Jami ID of the imported account.
     * @param registeredName The registered name of the imported account (if any).
     */
    fun showAuthentication(needPassword: Boolean, jamiId: String, registeredName: String?) // Todo: add error

    /**
     * Show the result of the operation.
     * @param result The result of the operation.
     */
    fun showResult(result: ImportSideResult)
}