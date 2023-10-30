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
package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.services.AccountService
import net.jami.utils.Log

/**
 * The JamiIdFragment is a fragment that displays a JamiId.
 * It can be used to display a JamiId and to edit it.
 * JamiIdStatus help to know the status of the JamiId (like a state machine).
 */
enum class JamiIdStatus {
    USERNAME_NOT_DEFINED, // The username can be defined
    EDITING_USERNAME_INITIAL, // Initial state when nothing is typed
    EDITING_USERNAME_LOADING, // Looking if the typed username is available
    EDITING_USERNAME_NOT_AVAILABLE, // The typed username is not available
    EDITING_USERNAME_AVAILABLE, // The typed username is available
    USERNAME_DEFINED, // The username is defined (and can't be edited anymore)
}

data class JamiIdUiState(
    val jamiIdStatus: JamiIdStatus? = null,
    val username: String = "",
    val editedUsername: String = "",
)

enum class RegisteredState(val state: Int) {
    ALREADY_REGISTERED(0),
    NOT_REGISTERED(2),
}

class JamiIdViewModel : ViewModel() {
    // Expose screen UI state
    private val _uiState = MutableStateFlow(JamiIdUiState())
    val uiState: StateFlow<JamiIdUiState> = _uiState.asStateFlow()

    lateinit var onCheckUsernameAvailability: (String) -> Unit
    lateinit var onRegisterName: (String) -> Unit

    companion object {
        private val TAG = JamiIdViewModel::class.simpleName!!
    }

    /**
     * Initialize the JamiIdViewModel.
     * @param username The username to display.
     * @param jamiIdStatus The status of the JamiId.
     * @param onCheckUsernameAvailability The function to call when the user is typing a new
     * username. Should be debounced. Asynchronous.
     * @param onRegisterName The function to call when the user click on the 'Validate' button.
     * Synchronous.
     */
    fun init(
        username: String,
        jamiIdStatus: JamiIdStatus,
        onCheckUsernameAvailability: (String) -> Unit,
        onRegisterName: (String) -> Unit,
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                jamiIdStatus = jamiIdStatus,
                username = username
            )
        }
        this.onRegisterName = onRegisterName
        this.onCheckUsernameAvailability = onCheckUsernameAvailability
    }

    fun onChooseUsernameClicked() {
        Log.d(TAG, "'Choose a username' button clicked.")
        if (uiState.value.editedUsername != "") {
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_INITIAL)
            textChanged(uiState.value.editedUsername)
        } else changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_INITIAL)
    }

    fun onValidateClicked() {
        Log.d(TAG, "'Validate' button clicked.")
        onRegisterName(uiState.value.editedUsername)
        _uiState.update { currentState ->
            currentState.copy(
                username = currentState.editedUsername,
                jamiIdStatus = JamiIdStatus.USERNAME_DEFINED
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
     * Asynchronous. Called when the result of the username availability is received.
     */
    fun checkIfUsernameIsAvailableResult(registeredName: AccountService.RegisteredName) {
        if (uiState.value.jamiIdStatus != JamiIdStatus.EDITING_USERNAME_LOADING)
            return

        if (registeredName.state == RegisteredState.NOT_REGISTERED.state) {
            Log.d(TAG, "Username '${registeredName.name}' is available.")
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_AVAILABLE)
        } else {
            Log.d(TAG, "Username '${registeredName.name}' is not available.")
            changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_NOT_AVAILABLE)
        }
    }

    /**
     * Check if the username is available.
     */
    private fun checkIfUsernameIsAvailable(username: String) {
        Log.d(TAG, "Checking if '$username' is available as new username.")
        onCheckUsernameAvailability(username)
    }
}