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
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.model.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.RegisteredName
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class JamiAccountCreationPresenter @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
): RootPresenter<JamiAccountCreationView>() {
    private val contactQuery = PublishSubject.create<String>()

    private var accountCreationModel: AccountCreationModel? = null
    private var isUsernameCorrect = false
    private var isPasswordCorrect = true
    private var isConfirmCorrect = true
    private var showLoadingAnimation = true
    private var passwordConfirm: CharSequence = ""

    override fun bindView(view: JamiAccountCreationView) {
        super.bindView(view)
        mCompositeDisposable.add(contactQuery
            .debounce(TYPING_DELAY, TimeUnit.MILLISECONDS)
            .switchMapSingle { q: String -> accountService.findRegistrationByName("", "", q) }
            .observeOn(uiScheduler)
            .subscribe { q: RegisteredName -> onLookupResult(q.name, q.address, q.state) })
    }

    fun init(model: AccountCreationModel?) {
        if (model == null) {
            view?.cancel()
            return
        }
        accountCreationModel = model
    }

    /**
     * Called everytime the provided username for the new account changes
     * Sends the new value of the username to the ContactQuery subjet and shows the loading
     * animation if it has not been started before
     */
    fun userNameChanged(userName: String) {
        accountCreationModel?.username = userName
        isUsernameCorrect = false
        if (showLoadingAnimation) {
            view?.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.LOADING)
            showLoadingAnimation = false
        }
        contactQuery.onNext(userName)
    }

    fun registerUsernameChanged(isChecked: Boolean) {
        accountCreationModel?.let  { model ->
            if (!isChecked) {
                model.username = ""
            }
            checkForms()
        }
    }

    fun passwordUnset() {
        accountCreationModel?.password = ""
        isPasswordCorrect = true
        isConfirmCorrect = true
        view?.showInvalidPasswordError(false)
        view?.enableNextButton(true)
    }

    fun passwordChanged(password: String, repeat: CharSequence) {
        passwordConfirm = repeat
        passwordChanged(password)
    }

    fun passwordChanged(password: String) {
        accountCreationModel?.password = password
        if (password.isNotEmpty() && password.length < PASSWORD_MIN_LENGTH) {
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
        view?.enableNextButton(isPasswordCorrect && isConfirmCorrect)
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
        view?.enableNextButton(isPasswordCorrect && isConfirmCorrect)
    }

    fun createAccount() {
        if (isInputValid) {
            view?.goToAccountCreation()
        }
    }

    private val isInputValid: Boolean
        get() {
            val passwordOk = isPasswordCorrect && isConfirmCorrect
            val usernameOk = accountCreationModel?.username != null || isUsernameCorrect
            return passwordOk && usernameOk
        }

    private fun checkForms() {
        val valid = isInputValid
        if (valid && isUsernameCorrect)
            view?.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.AVAILABLE)
    }

    private fun onLookupResult(name: String, address: String?, state: Int) {
        val view = view
        //Once we get the result, we can show the loading animation again when the user types
        showLoadingAnimation = true
        if (view == null) {
            return
        }
        if (name.isEmpty()) {
            view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.RESET)
            isUsernameCorrect = false
        } else {
            when (state) {
                0 -> {
                    // on found
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN)
                    isUsernameCorrect = false
                }
                1 -> {
                    // invalid name
                    view.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.ERROR_USERNAME_INVALID)
                    isUsernameCorrect = false
                }
                2 -> {
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
        private const val PASSWORD_MIN_LENGTH = 6
        const val TYPING_DELAY = 350L
    }
}