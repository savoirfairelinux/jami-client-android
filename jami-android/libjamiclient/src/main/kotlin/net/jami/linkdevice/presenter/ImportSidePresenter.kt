package net.jami.linkdevice.presenter

import io.reactivex.rxjava3.core.Scheduler
import net.jami.linkdevice.view.ImportSideResult
import net.jami.linkdevice.view.ImportSideView
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import javax.inject.Inject
import javax.inject.Named


class ImportSidePresenter @Inject constructor(
    private val mAccountService: AccountService,
    @Suppress("unused") @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : JamiDaemonListener, RootPresenter<ImportSideView>() {
    private var _currentState = LinkDeviceState.NONE
    val currentState: LinkDeviceState
        get() = _currentState

    init {
        mAccountService.deviceAuthStateObservable
            .filter { it.accountId == mAccountService.currentAccount?.accountId }
            .subscribe { it: AccountService.DeviceAuthResult ->
                updateDeviceAuthState(it)
            }.apply { mCompositeDisposable.add(this) }
    }

    fun onAuthentication(password: String) {
        // Todo: Send the password to Jami-daemon.
    }

    fun onCancel() {
        // Todo: Cancel the operation.
    }

    override fun onNoneSignal() {
        _currentState = LinkDeviceState.NONE
        view?.showAuthenticationUri(null)
    }

    override fun onTokenAvailableSignal(token: String) {
        _currentState = LinkDeviceState.TOKEN_AVAIL
        view?.showAuthenticationUri(token)
    }

    override fun onConnectingSignal(ip: String) {
        _currentState = LinkDeviceState.CONNECTING
        view?.showActionRequired()
    }

    override fun onAuthenticatingSignal(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?
    ) {
        _currentState = LinkDeviceState.AUTHENTICATION
        view?.showAuthentication(needPassword, jamiId, registeredName)
    }

    override fun onImportingSignal() {
        _currentState = LinkDeviceState.IMPORTING
        view?.showResult(result = ImportSideResult.IN_PROGRESS)
    }

    override fun onDoneSignal() {
        _currentState = LinkDeviceState.DONE
        view?.showResult(result = ImportSideResult.SUCCESS)
    }

    override fun onErrorSignal() {
        throw UnsupportedOperationException()
    }

    private fun updateDeviceAuthState(result: AccountService.DeviceAuthResult) {
        when (result.state) {
            AccountService.DeviceAuthState.NONE -> onNoneSignal()
            AccountService.DeviceAuthState.TOKEN_AVAILABLE -> onTokenAvailableSignal("")
            AccountService.DeviceAuthState.CONNECTING -> onConnectingSignal("" /* Todo: ip */)
            AccountService.DeviceAuthState.AUTHENTICATING -> onAuthenticatingSignal(
                needPassword = false /* Todo: needPassword */,
                jamiId = "" /* Todo: jamiId */,
                registeredName = null /* Todo: registeredName */
            )
            // Todo: AccountService.DeviceAuthState.IMPORTING -> onImportingSignal()
            AccountService.DeviceAuthState.DONE -> onDoneSignal()
            AccountService.DeviceAuthState.ERROR -> throw UnsupportedOperationException()
        }
    }

}