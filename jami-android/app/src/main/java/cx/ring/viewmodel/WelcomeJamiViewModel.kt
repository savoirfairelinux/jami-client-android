package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import cx.ring.utils.UiCustomization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.services.AccountService
import net.jami.utils.Log


data class WelcomeJamiUiState(
    val isJamiAccount: Boolean = false,
    val jamiId: String = "",
    val jamiHash: String = "",
    val uiCustomization: UiCustomization? = null,
)

class WelcomeJamiViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(WelcomeJamiUiState())
    val uiState: StateFlow<WelcomeJamiUiState> = _uiState.asStateFlow()

    private lateinit var jamiIdViewModel: JamiIdViewModel
    private lateinit var onRegisterName: (String) -> Unit
    private lateinit var onCheckUsernameAvailability: (String) -> Unit

    companion object {
        private val TAG = WelcomeJamiViewModel::class.simpleName!!
    }

    /**
     * Initialize the JamiIdViewModel.
     * @param isJamiAccount Tells if the account is a Jami account or not (SIP account).
     * @param jamiId The JamiId to display. Empty if the account is not a Jami account.
     * @param jamiHash The JamiHash to display. Empty if the account is not a Jami account.
     * @param onRegisterName The function to call when the user click on the 'Validate' button.
     * Synchronous.
     * @param onCheckUsernameAvailability The function to call when the user is typing a new.
     * Asynchronous.
     */
    fun init(
        isJamiAccount: Boolean,
        jamiId: String = "",
        jamiHash: String = "",
        onRegisterName: (String) -> Unit,
        onCheckUsernameAvailability: (String) -> Unit,
        uiCustomization: UiCustomization? = null,
    ) {
        this.onRegisterName = onRegisterName
        this.onCheckUsernameAvailability = onCheckUsernameAvailability

        _uiState.update { currentState ->
            currentState.copy(
                isJamiAccount = isJamiAccount,
                jamiId = jamiId,
                jamiHash = jamiHash,
                uiCustomization = uiCustomization,
            )
        }
    }

    fun checkIfUsernameIsAvailableResult(result: AccountService.RegisteredName) =
        jamiIdViewModel.checkIfUsernameIsAvailableResult(registeredName = result)

    fun initJamiIdViewModel(jamiIdViewModel: JamiIdViewModel) {

        this.jamiIdViewModel = jamiIdViewModel
        val currentUiState = uiState.value

        if (currentUiState.jamiId != "") {
            Log.d(TAG, "Username is registered : ${currentUiState.jamiId}")
            jamiIdViewModel.init(
                username = currentUiState.jamiId,
                jamiIdStatus = JamiIdStatus.USERNAME_DEFINED,
                onCheckUsernameAvailability = {},
                onRegisterName = {}
            )
        } else {
            Log.d(TAG, "Username is not registered : ${currentUiState.jamiHash}")
            jamiIdViewModel.init(
                username = currentUiState.jamiHash,
                jamiIdStatus = JamiIdStatus.USERNAME_NOT_DEFINED,
                onCheckUsernameAvailability = { username: String ->
                    onCheckUsernameAvailability(username)
                },
                onRegisterName = { username: String ->
                    onRegisterName(username)
                }
            )
        }
    }
}