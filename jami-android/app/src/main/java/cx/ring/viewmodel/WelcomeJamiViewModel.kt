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
import cx.ring.utils.DeviceUtils
import cx.ring.utils.UiCustomization
import cx.ring.utils.getUiCustomizationFromConfigJson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import org.json.JSONObject
import javax.inject.Inject


data class WelcomeJamiUiState(
    val isJamiAccount: Boolean = false, // if the account is a Jami account or not (SIP account).
    val uiCustomization: UiCustomization? = null,
)

@HiltViewModel
class WelcomeJamiViewModel @Inject constructor(
    val accountService: AccountService,
) : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(WelcomeJamiUiState())
    val uiState: StateFlow<WelcomeJamiUiState> = _uiState.asStateFlow()

    private val disposable: CompositeDisposable = CompositeDisposable()

    /**
     * Initialize the JamiIdViewModel.
     */
    init {
        disposable.add(
            accountService.currentAccountSubject
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { account ->
                    // Can be null if the account doesn't have a config
                    val uiCustomization = try {
                        getUiCustomizationFromConfigJson(
                            configurationJson = JSONObject(account.config[ConfigKey.UI_CUSTOMIZATION]),
                            managerUri = account.config[ConfigKey.MANAGER_URI],
                        )
                    } catch (e: org.json.JSONException) {
                        null // If the JSON is invalid, we don't display the customization
                    }
                    _uiState.update { currentState ->
                        currentState.copy(
                            isJamiAccount = account.isJami,
                            uiCustomization = uiCustomization,
                        )
                    }
                }
        )
    }

    companion object {
        @Suppress("unused")
        private val TAG = WelcomeJamiViewModel::class.simpleName!!
    }
}