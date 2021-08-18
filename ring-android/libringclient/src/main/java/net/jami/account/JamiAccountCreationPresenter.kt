/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.account

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import net.jami.account.JamiAccountCreationPresenter
import net.jami.mvp.AccountCreationModel
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.RegisteredName
import net.jami.utils.StringUtils.isEmpty
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class JamiAccountCreationPresenter @Inject constructor(
    var mAccountService: AccountService,
    @param:Named("UiScheduler") var mUiScheduler: Scheduler
): RootPresenter<JamiAccountCreationView>() {
    private val contactQuery = PublishSubject.create<String>()

    private var mAccountCreationModel: AccountCreationModel? = null
    private var isUsernameCorrect = false
    private var isPasswordCorrect = true
    private var isConfirmCorrect = true
    private var showLoadingAnimation = true
    private var mPasswordConfirm: CharSequence = ""

    override fun bindView(view: JamiAccountCreationView) {
        super.bindView(view)
        mCompositeDisposable.add(contactQuery
            .debounce(TYPING_DELAY, TimeUnit.MILLISECONDS)
            .switchMapSingle { q: String? -> mAccountService.findRegistrationByName("", "", q!!) }
            .observeOn(mUiScheduler)
            .subscribe { q: RegisteredName -> onLookupResult(q.name, q.address, q.state) })
    }

    fun init(accountCreationModel: AccountCreationModel?) {
        if (accountCreationModel == null) {
            view?.cancel()
        }
        mAccountCreationModel = accountCreationModel
    }

    /**
     * Called everytime the provided username for the new account changes
     * Sends the new value of the username to the ContactQuery subjet and shows the loading
     * animation if it has not been started before
     */
    fun userNameChanged(userName: String) {
        if (mAccountCreationModel != null) mAccountCreationModel!!.username = userName
        contactQuery.onNext(userName)
        isUsernameCorrect = false
        if (showLoadingAnimation) {
            val view = view
            view?.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.LOADING)
            showLoadingAnimation = false
        }
    }

    fun registerUsernameChanged(isChecked: Boolean) {
        if (mAccountCreationModel != null) {
            if (!isChecked) {
                mAccountCreationModel!!.username = ""
            }
            checkForms()
        }
    }

    fun passwordUnset() {
        if (mAccountCreationModel != null) mAccountCreationModel!!.password = null
        isPasswordCorrect = true
        isConfirmCorrect = true
        view!!.showInvalidPasswordError(false)
        view!!.enableNextButton(true)
    }

    fun passwordChanged(password: String, repeat: CharSequence) {
        mPasswordConfirm = repeat
        passwordChanged(password)
    }

    fun passwordChanged(password: String) {
        if (mAccountCreationModel != null) mAccountCreationModel!!.password = password
        if (!isEmpty(password) && password.length < PASSWORD_MIN_LENGTH) {
            view!!.showInvalidPasswordError(true)
            isPasswordCorrect = false
        } else {
            view!!.showInvalidPasswordError(false)
            isPasswordCorrect = password.length != 0
            isConfirmCorrect = if (!password.contentEquals(mPasswordConfirm)) {
                if (mPasswordConfirm.length > 0) view!!.showNonMatchingPasswordError(true)
                false
            } else {
                view!!.showNonMatchingPasswordError(false)
                true
            }
        }
        view!!.enableNextButton(isPasswordCorrect && isConfirmCorrect)
    }

    fun passwordConfirmChanged(passwordConfirm: String) {
        isConfirmCorrect = if (passwordConfirm != mAccountCreationModel!!.password) {
            view!!.showNonMatchingPasswordError(true)
            false
        } else {
            view!!.showNonMatchingPasswordError(false)
            true
        }
        mPasswordConfirm = passwordConfirm
        view!!.enableNextButton(isPasswordCorrect && isConfirmCorrect)
    }

    fun createAccount() {
        if (isInputValid) {
            val view = view
            view!!.goToAccountCreation(mAccountCreationModel)
        }
    }

    private val isInputValid: Boolean
        private get() {
            val passwordOk = isPasswordCorrect && isConfirmCorrect
            val usernameOk =
                mAccountCreationModel != null && mAccountCreationModel!!.username != null || isUsernameCorrect
            return passwordOk && usernameOk
        }

    private fun checkForms() {
        val valid = isInputValid
        if (valid && isUsernameCorrect) view!!.updateUsernameAvailability(JamiAccountCreationView.UsernameAvailabilityStatus.AVAILABLE)
    }

    private fun onLookupResult(name: String?, address: String?, state: Int) {
        val view = view
        //Once we get the result, we can show the loading animation again when the user types
        showLoadingAnimation = true
        if (view == null) {
            return
        }
        if (name == null || name.isEmpty()) {
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
                    mAccountCreationModel!!.username = name
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
        mAccountCreationModel!!.isPush = push
    }

    companion object {
        val TAG = JamiAccountCreationPresenter::class.java.simpleName
        private const val PASSWORD_MIN_LENGTH = 6
        private const val TYPING_DELAY = 350L
    }
}