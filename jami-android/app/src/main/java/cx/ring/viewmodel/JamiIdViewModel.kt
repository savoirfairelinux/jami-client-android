package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.services.AccountService
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
    val JamiIdStatus: JamiIdStatus? = null,
    val username: String = "",
    val editedUsername: String = "",
)

enum class RegisteredState(val state: Int) {
    ALREADY_REGISTERED(0),
    NOT_REGISTERED(2),
}

@HiltViewModel
class JamiIdViewModel @Inject constructor(
    private val accountService: AccountService,
) : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(JamiIdUiState())
    private val usernameIsAvailableDisposable = CompositeDisposable()
    val uiState: StateFlow<JamiIdUiState> = _uiState.asStateFlow()

    companion object {
        private val TAG = JamiIdViewModel::class.simpleName!!
    }

    override fun onCleared() {
        super.onCleared()
        usernameIsAvailableDisposable.dispose()
    }

    fun init(username: String, jamiIdStatus: JamiIdStatus) {
        _uiState.update { currentState ->
            currentState.copy(
                JamiIdStatus = jamiIdStatus,
                username = username
            )
        }
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
    }

    fun onLooseFocus() {
        changeJamiIdStatus(JamiIdStatus.USERNAME_NOT_DEFINED)
    }

    /**
     * Called when the user is typing a new username.
     */
    fun textChanged(typingUsername: String) {
        // Only check if the username is at least 3 characters long
        if (typingUsername.length < 3) {
            _uiState.update { currentState ->
                currentState.copy(
                    editedUsername = typingUsername,
                    JamiIdStatus = JamiIdStatus.EDITING_USERNAME_INITIAL
                )
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    editedUsername = typingUsername,
                    JamiIdStatus = JamiIdStatus.EDITING_USERNAME_LOADING
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
                JamiIdStatus = newStatus
            )
        }
    }

    /**
     * Check if the username is available.
     */
    private fun checkIfUsernameIsAvailable(username: String) {
        Log.d(TAG, "Checking if '$username' is available as new username.")

        usernameIsAvailableDisposable.clear() // Clear to avoid multiple requests
        usernameIsAvailableDisposable.add(
            accountService.findRegistrationByName("", "", username)
                .observeOn(DeviceUtils.uiScheduler)
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribe { it: AccountService.RegisteredName ->
                    if (uiState.value.JamiIdStatus != JamiIdStatus.EDITING_USERNAME_LOADING)
                        return@subscribe

                    if (it.state == RegisteredState.NOT_REGISTERED.state) {
                        Log.d(TAG, "Username '$username' is available.")
                        changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_AVAILABLE)
                    } else {
                        Log.d(TAG, "Username '$username' is not available.")
                        changeJamiIdStatus(JamiIdStatus.EDITING_USERNAME_NOT_AVAILABLE)
                    }
                }
        )
    }
}