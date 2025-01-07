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
package cx.ring.viewmodel

import androidx.lifecycle.ViewModel
import cx.ring.utils.UiCustomization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class WelcomeJamiUiState(
    val isJamiAccount: Boolean = false,
    val uiCustomization: UiCustomization? = null,
)

class WelcomeJamiViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(WelcomeJamiUiState())
    val uiState: StateFlow<WelcomeJamiUiState> = _uiState.asStateFlow()

    /**
     * Initialize the JamiIdViewModel.
     * @param isJamiAccount Tells if the account is a Jami account or not (SIP account).
     */
    fun init(
        isJamiAccount: Boolean,
        uiCustomization: UiCustomization? = null,
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                isJamiAccount = isJamiAccount,
                uiCustomization = uiCustomization,
            )
        }
    }

    companion object {
        @Suppress("unused")
        private val TAG = WelcomeJamiViewModel::class.simpleName!!
    }
}