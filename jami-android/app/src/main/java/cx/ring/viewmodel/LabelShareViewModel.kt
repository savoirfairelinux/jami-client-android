/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

import android.content.Context
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUri.ShareItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationFilter
import net.jami.utils.Log
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LabelShareViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountService: AccountService,
    private val conversationFacade: ConversationFacade,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : ViewModel() {
    sealed interface State {
        data object Idle : State
        data object Sending : State
        data class Finished(val sent: Int, val failed: Int) : State
        data object Failed : State
    }

    private sealed interface Item {
        data class Text(val text: String) : Item
        data class Attachment(val file: File) : Item
    }

    private enum class Delivery { SENT, FAILED }

    private val shareState = MutableStateFlow<State>(State.Idle)
    val state = shareState.asStateFlow()
    private val disposables = CompositeDisposable()

    fun share(accountId: String, label: String, confirmedRecipients: Set<String>, items: List<ShareItem>) {
        if (shareState.value != State.Idle) {
            Log.w(TAG, "Ignoring duplicate label share")
            return
        }
        shareState.value = State.Sending
        disposables.add(Single.defer {
            val account = checkNotNull(accountService.getAccount(accountId)) { "Account is unavailable" }
            require(account.isJami && items.isNotEmpty()) { "Nothing to share or unsupported account" }
            val recipients = account.getContactsWithLabel(label)
            check(recipients.isNotEmpty() && recipients.map { it.uri.uri }.toSet() == confirmedRecipients) {
                "Label membership changed; confirm the recipients again"
            }
            Single.using(
                { Files.createTempDirectory(context.cacheDir.toPath(), "label-share-").toFile() },
                { directory ->
                    prepare(items, directory).flatMap { prepared ->
                        Observable.fromIterable(recipients.withIndex())
                            .concatMapSingle { (index, contact) ->
                                send(accountId, label, contact, prepared, directory, index)
                                    .toSingleDefault(Delivery.SENT)
                                    .onErrorReturn { error ->
                                        Log.e(TAG, "Cannot share with label recipient", error)
                                        Delivery.FAILED
                                    }
                            }.toList()
                    }
                },
                { directory ->
                    if (!directory.deleteRecursively())
                        Log.w(TAG, "Cannot clean up label share cache")
                }
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(uiScheduler)
            .subscribe({ results ->
                shareState.value = State.Finished(
                    results.count { it == Delivery.SENT }, results.count { it == Delivery.FAILED }
                )
            }, { error ->
                Log.e(TAG, "Cannot share to contact label", error)
                shareState.value = State.Failed
            }))
    }

    private fun prepare(items: List<ShareItem>, directory: File): Single<List<Item>> =
        Observable.fromIterable(items.withIndex()).concatMapSingle { (index, item) ->
            when {
                item.type.startsWith("text/") && item.text != null ->
                    Single.just<Item>(Item.Text(item.text.toString()))
                item.data != null -> {
                    val target = Files.createDirectory(directory.toPath().resolve("source-$index")).toFile()
                    AndroidFileUtils.getCacheFile(context, item.data, target).map<Item> { Item.Attachment(it) }
                }
                else -> Single.error(IllegalArgumentException("Unsupported shared item"))
            }
        }.toList()

    private fun send(
        accountId: String, label: String, contact: Contact, items: List<Item>, directory: File, recipient: Int
    ): Completable = Completable.defer {
        val account = checkNotNull(accountService.getAccount(accountId)) { "Account is unavailable" }
        check(account.getContactsWithLabel(label).any { it.uri.uri == contact.uri.uri }) {
            "Contact is no longer a recipient"
        }
        contact.conversationUri.firstOrError()
            .flatMap { conversationFacade.startConversation(accountId, it) }
            .flatMapCompletable { conversation ->
                check(conversation.accountId == accountId && ConversationFilter.Contacts.matches(conversation)
                    && conversation.contact?.uri?.uri == contact.uri.uri && conversation.request == null && !conversation.isEnded) {
                    "Recipient conversation is unavailable"
                }
                if (items.any { it is Item.Attachment }) {
                    check(conversation.isSwarm) { "Recipient cannot receive files" }
                    conversation.requirePermission(Conversation.Permission.File)
                }
                if (items.any { it is Item.Text }) conversation.requirePermission(Conversation.Permission.Text)
                Completable.concat(items.mapIndexed { index, item ->
                    when (item) {
                        is Item.Text -> conversationFacade.sendTextMessage(conversation, contact.uri, item.text)
                        is Item.Attachment -> Completable.defer {
                            // sendFile moves its input, so each recipient needs a separate copy.
                            val target = Files.createDirectory(directory.toPath().resolve("send-$recipient-$index")).toFile()
                            val copy = item.file.copyTo(File(target, item.file.name))
                            conversationFacade.sendFile(conversation, contact.uri, copy)
                        }
                    }
                })
            }
    }

    fun clearResult() {
        if (shareState.value != State.Sending) shareState.value = State.Idle
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }

    companion object {
        private const val TAG = "LabelShare"
    }
}
