/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.RegisteredName
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class JamiAccountCreationPresenter @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
): RootPresenter<JamiAccountCreationView>() {
    private val contactQuery = PublishSubject.create<String>()

    private var accountCreationModel: AccountCreationModel? = null
    private var isUsernameCorrect = true
    private var isPasswordEnabled = false
    private var isPasswordCorrect = true
    private var isConfirmCorrect = true
    private var showLoadingAnimation = true
    private var passwordConfirm: CharSequence = ""

    override fun bindView(view: JamiAccountCreationView) {
        super.bindView(view)
        mCompositeDisposable.add(contactQuery
            .debounce(TYPING_DELAY, TimeUnit.MILLISECONDS)
            .switchMapSingle { query: String ->
                if (query.isEmpty())
                    Single.never()
                else
                    accountService.findRegistrationByName("", "", query)
            }
            .observeOn(uiScheduler)
            .subscribe { res: RegisteredName -> onLookupResult(res.query, res.address, res.state) })
    }

    fun init(model: AccountCreationModel?) {
        if (model == null) {
            view?.cancel()
            return
        }
        accountCreationModel = model
        isUsernameCorrect = model.username.isEmpty()
        checkForms()
    }

    fun fullNameUpdated(fullName: String) {
        accountCreationModel?.fullName = fullName.trim()
    }

    fun photoUpdated(bitmap: Single<Any>) {
        mCompositeDisposable.add(bitmap
            .observeOn(uiScheduler)
            .subscribe({ b: Any -> accountCreationModel?.photo = b })
            { e: Throwable -> Log.e(TAG, "Can't load image", e) })
    }

    fun photoRemoved() {
        accountCreationModel?.photo = null
    }

    /**
     * Called everytime the provided username for the new account changes
     * Sends the new value of the username to the ContactQuery subjet and shows the loading
     * animation if it has not been started before
     */
    fun userNameChanged(userName: String) {
        accountCreationModel?.username = userName
        if (userName.isEmpty()) {
            isUsernameCorrect = true
            showLoadingAnimation = true
            contactQuery.onNext("")
            view?.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.RESET)
            checkForms()
            return
        }
        isUsernameCorrect = false
        if (showLoadingAnimation) {
            view?.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.LOADING)
            showLoadingAnimation = false
        }
        checkForms()
        contactQuery.onNext(userName)
    }

    fun registerUsernameChanged(isChecked: Boolean) {
        accountCreationModel?.let  { model ->
            if (!isChecked) {
                model.username = ""
                isUsernameCorrect = true
            }
            checkForms()
        }
    }

    fun passwordUnset() {
        accountCreationModel?.password = ""
        passwordConfirm = ""
        isPasswordEnabled = false
        isPasswordCorrect = true
        isConfirmCorrect = true
        view?.showInvalidPasswordError(false)
        view?.showNonMatchingPasswordError(false)
        checkForms()
    }

    fun passwordChanged(password: String, repeat: CharSequence) {
        isPasswordEnabled = true
        passwordConfirm = repeat
        passwordChanged(password)
    }

    fun passwordChanged(password: String) {
        accountCreationModel?.password = password
        if (isPasswordEnabled && password.isEmpty()) {
            view?.showInvalidPasswordError(false)
            isPasswordCorrect = false
        } else if (password.isNotEmpty() && password.length < PASSWORD_MIN_LENGTH) {
            view?.showInvalidPasswordError(true)
            isPasswordCorrect = false
        } else {
            view?.showInvalidPasswordError(false)
            isPasswordCorrect = true
            isConfirmCorrect = if (!password.contentEquals(passwordConfirm)) {
                if (passwordConfirm.isNotEmpty())
                    view?.showNonMatchingPasswordError(true)
                false
            } else {
                view?.showNonMatchingPasswordError(false)
                true
            }
        }
        checkForms()
    }

    fun passwordConfirmChanged(passwordConfirm: String) {
        isConfirmCorrect = if (passwordConfirm != accountCreationModel?.password) {
            view?.showNonMatchingPasswordError(true)
            false
        } else {
            view?.showNonMatchingPasswordError(false)
            true
        }
        this.passwordConfirm = passwordConfirm
        checkForms()
    }

    fun createAccount() {
        if (isInputValid) {
            view?.goToAccountCreation()
        }
    }

    private val isInputValid: Boolean
        get() {
            val passwordOk = !isPasswordEnabled || (isPasswordCorrect && isConfirmCorrect)
            val username = accountCreationModel?.username ?: ""
            val usernameOk = username.isEmpty() || isUsernameCorrect
            return passwordOk && usernameOk
        }

    private fun checkForms() {
        view?.enableNextButton(isInputValid)
    }

    private fun onLookupResult(name: String, address: String?, state: AccountService.LookupState) {
        val view = view
        //Once we get the result, we can show the loading animation again when the user types
        showLoadingAnimation = true
        if (view == null) {
            return
        }
        val currentUsername = accountCreationModel?.username ?: ""
        if (name != currentUsername) {
            return
        }
        if (name.isEmpty()) {
            view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.RESET)
            isUsernameCorrect = true
        } else {
            when (state) {
                AccountService.LookupState.Success -> {
                    // on found
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN)
                    isUsernameCorrect = false
                }
                AccountService.LookupState.Invalid -> {
                    // invalid name
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_INVALID)
                    isUsernameCorrect = false
                }
                AccountService.LookupState.NotFound -> {
                    // available
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.AVAILABLE)
                    accountCreationModel?.username = name
                    isUsernameCorrect = true
                }
                else -> {
                    // on error
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR)
                    isUsernameCorrect = false
                }
            }
        }
        checkForms()
    }

    fun setPush(push: Boolean) {
        accountCreationModel?.isPush = push
    }

    companion object {
        val TAG = JamiAccountCreationPresenter::class.simpleName!!
        const val PASSWORD_MIN_LENGTH = 6
        const val TYPING_DELAY = 350L
    }
}