package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.utils.Log


data class WelcomeJamiUiState(
    val isJamiAccount: Boolean = false,
    val jamiId: String = "",
    val jamiHash: String = "",
)

class WelcomeJamiViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(WelcomeJamiUiState())
    val uiState: StateFlow<WelcomeJamiUiState> = _uiState.asStateFlow()

    companion object {
        private val TAG = WelcomeJamiViewModel::class.simpleName!!
    }

    fun init(isJamiAccount: Boolean, jamiId: String = "", jamiHash: String = "") {
        _uiState.update { currentState ->
            currentState.copy(
                isJamiAccount = isJamiAccount,
                jamiId = jamiId,
                jamiHash = jamiHash
            )
        }
    }
}