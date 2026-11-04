/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CreateFeedViewModel @Inject constructor(
    private val accountService: AccountService,
    private val savedState: SavedStateHandle,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler,
) : ViewModel() {
    sealed interface State {
        data object Idle : State
        data object Creating : State
        data class Created(val uri: Uri) : State
        data object Failed : State
    }

    val accountId: String = checkNotNull(savedState[ARG_ACCOUNT_ID])
    val name = savedState.getStateFlow(STATE_NAME, "")
    val description = savedState.getStateFlow(STATE_DESCRIPTION, "")
    private val creationState = MutableStateFlow<State>(State.Idle)
    val state = creationState.asStateFlow()
    private val disposables = CompositeDisposable()

    fun setName(value: String) {
        savedState[STATE_NAME] = value
    }

    fun setDescription(value: String) {
        savedState[STATE_DESCRIPTION] = value
    }

    fun create() {
        if (creationState.value == State.Creating || creationState.value is State.Created)
            return
        val title = name.value.trim()
        if (title.isEmpty()) {
            Log.w(TAG, "Cannot create a Feed without a name")
            creationState.value = State.Failed
            return
        }
        val profile = feedCreationProfile(title, description.value)
        creationState.value = State.Creating
        // Retry profile failures on the already-created Feed, not on a new one.
        val existingId = savedState.get<String>(STATE_CONVERSATION_ID)
        val conversation = if (existingId == null)
            accountService.startFeedConversation(accountId).map { it.uri }
        else Single.just(Uri(Uri.SWARM_SCHEME, existingId))
        disposables.add(conversation.observeOn(uiScheduler)
            .flatMap { uri ->
                savedState[STATE_CONVERSATION_ID] = uri.rawRingId
                accountService.updateConversationInfo(accountId, uri.rawRingId, profile)
                    .andThen(Single.just(uri))
            }
            .observeOn(uiScheduler)
            .subscribe({ uri -> creationState.value = State.Created(uri) }, { error ->
                Log.e(TAG, "Cannot create Feed", error)
                creationState.value = State.Failed
            }))
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }

    companion object {
        const val ARG_ACCOUNT_ID = "accountId"
        private const val STATE_NAME = "feedName"
        private const val STATE_DESCRIPTION = "feedDescription"
        private const val STATE_CONVERSATION_ID = "feedConversationId"
        private val TAG = CreateFeedViewModel::class.java.simpleName
    }
}

internal fun feedCreationProfile(name: String, description: String): Map<String, String> =
    mapOf("title" to name.trim(), "description" to description.trim())
