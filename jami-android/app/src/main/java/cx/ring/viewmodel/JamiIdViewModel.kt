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
package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.model.Account
import net.jami.services.AccountService
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * The JamiIdFragment is a fragment that displays a JamiId.
 * It can be used to display a JamiId and to edit it.
 * JamiIdStatus help to know the status of the JamiId (like a state machine).
 */
enum class JamiIdStatus {
    USERNAME_NOT_DEFINED, // The username can be defined
    EDITING_USERNAME_INITIAL, // Initial state when nothing is typed
    EDITING_USERNAME_LOADING, // Looking if the typed username is available / while registering.
    EDITING_USERNAME_NOT_AVAILABLE, // The typed username is not available
    EDITING_USERNAME_AVAILABLE, // The typed username is available
    USERNAME_DEFINED, // The username is defined (and can't be edited anymore)
}

data class JamiIdUiState(
    val jamiIdStatus: JamiIdStatus? = null,
    val username: String = "",
    val editedUsername: String = "",
)


@HiltViewModel
class JamiIdViewModel @Inject constructor(
    private val accountService: AccountService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : ViewModel() {

    val account: Account get() = accountService.currentAccount!!

    // Expose screen UI state
    private val _uiState = MutableStateFlow(JamiIdUiState())
    val uiState: StateFlow<JamiIdUiState> = _uiState.asStateFlow()

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val usernameAvailabilitySubject = PublishSubject.create<String>()

    init {
        Log.i(TAG, "JamiIdViewModel created.")
        // Subject to check if a username is available
        disposable.add(
            accountService.currentAccountSubject
                .switchMap { account -> usernameAvailabilitySubject.map { Pair(account, it) } }
                .debounce(500, TimeUnit.MILLISECONDS)
                .switchMapSingle { (account, username) ->
                    Log.i(TAG, "Checking if '$username' is available as new username...")
                    accountService.findRegistrationByName(
                        account.accountId, "", username
                    )
                }
                .observeOn(mUiScheduler)
                .subscribe { checkIfUsernameIsAvailableResult(it) }
        )

        disposable.add(
            accountService.currentAccountSubject
                .switchMap { account -> accountService.getObservableAccountProfile(account.accountId) }
                .observeOn(mUiScheduler)
                .subscribe { (account, _) ->
                    // Skip profile update if unchanged.
                    if (account.username == uiState.value.username) return@subscribe

                    _uiState.update { currentState ->
                        Log.i(TAG, "Updating username to '${account.displayUsername ?: ""}'.")
                        if (account.registeredName.isNotEmpty())
                            currentState.copy(
                                username = account.registeredName,
                                jamiIdStatus = JamiIdStatus.USERNAME_DEFINED
                            )
                        else
                            currentState.copy(
                                username = account.username ?: "",
                                jamiIdStatus = JamiIdStatus.USERNAME_NOT_DEFINED,
                                editedUsername = ""
                            )
                    }
                }
        )

    }

    fun onChooseUsernameClicked() {
        Log.i(TAG, "'Choose a username' button clicked.")
        if (uiState.value.editedUsername != "") {
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_INITIAL)
            textChanged(uiState.value.editedUsername)
        } else changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_INITIAL)
    }

    fun onValidateClicked(scheme: String = "", password: String = "") {
        Log.i(TAG, "'Validate' button clicked.")
        accountService.registerName(
            account,
            name = uiState.value.editedUsername,
            scheme,
            password
        )
        _uiState.update { currentState ->
            currentState.copy(
                jamiIdStatus = JamiIdStatus.EDITING_USERNAME_LOADING
            )
        }
    }

    fun onLooseFocus() {
        changeJamiIdStatus(JamiIdStatus.USERNAME_NOT_DEFINED)
    }

    /**
     * Called when the user is typing a new username.
     */
    fun textChanged(typingUsername: String) {
        if ( // No need to process the text if the username is already defined
            uiState.value.jamiIdStatus == JamiIdStatus.USERNAME_NOT_DEFINED ||
            uiState.value.jamiIdStatus == JamiIdStatus.USERNAME_DEFINED
        ) return

        if (typingUsername.length < 3) {
            _uiState.update { currentState ->
                currentState.copy(
                    editedUsername = typingUsername,
                    jamiIdStatus = JamiIdStatus.EDITING_USERNAME_INITIAL
                )
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    editedUsername = typingUsername,
                    jamiIdStatus = JamiIdStatus.EDITING_USERNAME_LOADING
                )
            }
            checkIfUsernameIsAvailable(typingUsername)
        }
    }

    /**
     * Asynchronous. Called when the result of the username availability is received.
     */
    private fun checkIfUsernameIsAvailableResult(registeredName: AccountService.RegisteredName) {
        if (uiState.value.jamiIdStatus != JamiIdStatus.EDITING_USERNAME_LOADING)
            return

        if (registeredName.state == AccountService.LookupState.NotFound) {
            Log.i(TAG, "Username '${registeredName.name}' is available.")
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_AVAILABLE)
        } else {
            Log.i(TAG, "Username '${registeredName.name}' is not available.")
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_NOT_AVAILABLE)
        }
    }

    /**
     * Change the status of the JamiId.
     */
    private fun changeJamiIdStatus(newStatus: JamiIdStatus) {
        _uiState.update { currentState ->
            currentState.copy(
                jamiIdStatus = newStatus
            )
        }
    }

    /**
     * Check if the username is available.
     */
    private fun checkIfUsernameIsAvailable(username: String) {
        usernameAvailabilitySubject.onNext(username)
    }

    companion object {
        private val TAG = JamiIdViewModel::class.simpleName!!
    }
}