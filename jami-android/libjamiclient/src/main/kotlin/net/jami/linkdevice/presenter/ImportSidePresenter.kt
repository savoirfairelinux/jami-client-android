package net.jami.linkdevice.presenter

import net.jami.linkdevice.view.ImportSideResult
import net.jami.linkdevice.view.ImportSideView


class ImportSidePresenter(private val view: ImportSideView) : JamiDaemonListener {
    private var _currentState = LinkDeviceState.NONE
    val currentState: LinkDeviceState
        get() = _currentState

    fun onAuthentication(password: String) {
        // Todo: Send the password to Jami-daemon.
    }

    fun onCancel() {
        // Todo: Cancel the operation.
    }

    override fun onNoneSignal() {
        _currentState = LinkDeviceState.NONE
        view.showAuthenticationUri(null)
    }

    override fun onTokenAvailableSignal(token: String) {
        _currentState = LinkDeviceState.TOKEN_AVAIL
        view.showAuthenticationUri(token)
    }

    override fun onConnectingSignal(ip: String) {
        _currentState = LinkDeviceState.CONNECTING
        view.showActionRequired()
    }

    override fun onAuthenticatingSignal(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?
    ) {
        _currentState = LinkDeviceState.AUTHENTICATION
        view.showAuthentication(needPassword, jamiId, registeredName)
    }

    override fun onImportingSignal() {
        _currentState = LinkDeviceState.IMPORTING
        view.showResult(result = ImportSideResult.IN_PROGRESS)
    }

    override fun onDoneSignal() {
        _currentState = LinkDeviceState.DONE
        view.showResult(result = ImportSideResult.SUCCESS)
    }

    override fun onErrorSignal() {
        throw UnsupportedOperationException()
    }

}