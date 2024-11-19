package net.jami.linkdevice.presenter

import io.reactivex.rxjava3.core.Scheduler
import net.jami.linkdevice.view.ExportSideInputError
import net.jami.linkdevice.view.ExportSideResult
import net.jami.linkdevice.view.ExportSideView
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

enum class LinkDeviceState {
    NONE,
    TOKEN_AVAIL,
    CONNECTING,
    AUTHENTICATION,
    IMPORTING,
    DONE,

    @Suppress("unused")
    ERROR
}

class ExportSidePresenter @Inject constructor(
    private val mAccountService: AccountService,
    @Suppress("unused") @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : JamiDaemonListener, RootPresenter<ExportSideView>() {
    private var _currentState = LinkDeviceState.NONE
    val currentState: LinkDeviceState
        get() = _currentState

    fun onAuthenticationUri(jamiAuthentication: String) {
        // Verify the input.
        if (jamiAuthentication.isEmpty()
            || !jamiAuthentication.startsWith("jami-auth://")
            || (jamiAuthentication.length != 59)
        ) {
            view?.showInput(ExportSideInputError.INVALID_INPUT)
            return
        }

        // Todo: Send the URI to Jami-daemon.
    }

    fun onIdentityConfirmation() {
        // Todo: Send the confirmation to Jami-daemon.
    }

    fun onCancel() {
        // Todo: Cancel the operation.
    }

    override fun onNoneSignal() {
        _currentState = LinkDeviceState.NONE
        view?.showInput()
    }

    override fun onTokenAvailableSignal(token: String) {
        throw UnsupportedOperationException()
    }

    override fun onConnectingSignal(ip: String) {
        _currentState = LinkDeviceState.CONNECTING
        view?.showIP(ip = ip)
    }

    override fun onAuthenticatingSignal(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?
    ) {
        _currentState = LinkDeviceState.AUTHENTICATION
        view?.showPasswordProtection()
    }

    override fun onImportingSignal() {
        _currentState = LinkDeviceState.IMPORTING
        view?.showResult(result = ExportSideResult.IN_PROGRESS)
    }

    override fun onDoneSignal() {
        _currentState = LinkDeviceState.DONE
        view?.showResult(result = ExportSideResult.SUCCESS)
    }

    override fun onErrorSignal() {
        throw UnsupportedOperationException()
    }

    companion object {
        private val TAG = ExportSidePresenter::class.simpleName!!
    }
}


interface JamiDaemonListener {
    fun onNoneSignal()
    fun onTokenAvailableSignal(token: String) {
        throw UnsupportedOperationException()
    }

    fun onConnectingSignal(ip: String)
    fun onAuthenticatingSignal(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String? = null
    )

    fun onImportingSignal()
    fun onDoneSignal()
    fun onErrorSignal() {
        throw UnsupportedOperationException()
    }
}