/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package cx.ring.repositories

import android.content.Context
import cx.ring.R
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.Board
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boards are daemon Feeds, the same objects Jami Qt publishes and subscribes to:
 * the owner authorizes contacts, who find the Board in the owner's catalogue and
 * subscribe explicitly. Nothing is stored locally: the daemon is the single source of truth.
 */
@Singleton
class BoardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountService: AccountService,
    private val contactService: ContactService,
    private val conversationFacade: ConversationFacade,
    private val circleRepository: CircleRepository
) {
    private val boards: Observable<List<Board>> =
        accountService.currentAccountSubject
            .switchMap { account ->
                Observable.merge(
                    accountService.feedsChanged.filter { it == account.accountId }.map { },
                    account.getConversationsSubject().map { }
                )
                    .startWithItem(Unit)
                    .debounce(150, TimeUnit.MILLISECONDS)
                    .switchMapSingle { loadBoards(account) }
            }
            .replay(1)
            .refCount()

    /** Owned, subscribed and offered Boards of the current account. */
    fun observeBoards(): Observable<List<Board>> = boards

    /** Asks every contact for the Boards it offers to the current account. */
    fun refresh() {
        accountService.currentAccount?.let { accountService.refreshFeeds(it.accountId) }
    }

    fun isBoard(accountId: String, conversationId: String): Boolean =
        accountService.getAccount(accountId)?.getSwarm(conversationId)?.isFeed() == true

    private fun loadBoards(account: Account): Single<List<Board>> =
        accountService.getFeeds(account.accountId).flatMap { feeds ->
            val selfUri = selfId(account)
            val owners = feeds.mapNotNull { it["feedOwner"] }.filter { it.isNotBlank() && it != selfUri }.distinct()
            resolveNames(account, owners).map { names ->
                feeds.mapNotNull { info ->
                    val unread = info["id"]?.let { account.getSwarm(it) }?.unreadTextMessages?.size ?: 0
                    Board.fromDaemon(account.accountId, info, context.getString(R.string.board_default_title), names[info["feedOwner"]].orEmpty(), unread)
                }.sortedWith(
                    compareByDescending<Board> { it.owned }
                        .thenByDescending { it.subscribed }
                        .thenBy { it.title.lowercase() }
                )
            }
        }.onErrorReturn { e ->
            Log.w(TAG, "Unable to load Boards", e)
            emptyList()
        }

    /** One-line summary of the Board's state, e.g. "My board • Replies allowed • 2 authorized". */
    fun statusSummary(board: Board): String = buildList {
        add(context.getString(when {
            board.owned -> R.string.board_status_mine
            board.subscribed -> R.string.board_status_subscribed
            board.requested -> R.string.board_status_requested
            else -> R.string.board_status_offered
        }))
        if (!board.owned && board.ownerName.isNotBlank())
            add(context.getString(R.string.board_status_owner, board.ownerName))
        add(context.getString(if (board.allowReplies) R.string.board_status_replies else R.string.board_status_read_only))
        if (board.owned)
            add(context.resources.getQuantityString(
                R.plurals.board_status_authorized, board.authorizedUris.size, board.authorizedUris.size))
    }.joinToString(" • ")

    private fun resolveNames(account: Account, uris: List<String>): Single<Map<String, String>> {
        if (uris.isEmpty())
            return Single.just(emptyMap())
        return Single.zip(uris.map { uri ->
            contactService.getLoadedContact(account.accountId, uri)
                .map { uri to it.displayName }
                .timeout(2, TimeUnit.SECONDS)
                .onErrorReturnItem(uri to uri.take(8))
        }) { results ->
            results.associate {
                @Suppress("UNCHECKED_CAST")
                it as Pair<String, String>
            }
        }
    }

    /** Confirmed contacts selected directly or through the chosen Circles; the daemon only accepts these. */
    fun resolveAuthorized(contactUris: Set<String>, circleIds: Set<String>): Set<String> {
        val account = accountService.currentAccount ?: return emptySet()
        val selfUri = selfId(account)
        val circleMembers = circleRepository.load(account.accountId)
            .filter { it.id in circleIds }
            .flatMap { it.members }
        return (contactUris + circleMembers)
            .map { normalize(it) }
            .filter { it.length == 40 && it != selfUri }
            .filter { account.getContact(it)?.status == Contact.Status.CONFIRMED }
            .toSet()
    }

    /** Creates a Board and authorizes the selected contacts to subscribe to it. */
    fun createBoard(
        title: String,
        allowReplies: Boolean,
        authorizedContactUris: Set<String>,
        authorizedCircleIds: Set<String>
    ): Single<Board> {
        val account = accountService.currentAccount
            ?: return Single.error(IllegalStateException(context.getString(R.string.board_error_no_account)))
        val accountId = account.accountId
        val cleanTitle = title.trim()
        val authorized = resolveAuthorized(authorizedContactUris, authorizedCircleIds)
        return accountService.createFeed(accountId, cleanTitle, "", allowReplies)
            .flatMap { id ->
                setAccess(accountId, id, authorized, true).andThen(Single.just(
                    Board(
                        id = id,
                        accountId = accountId,
                        title = cleanTitle,
                        ownerUri = selfId(account),
                        owned = true,
                        allowReplies = allowReplies,
                        authorizedUris = authorized,
                        available = true
                    )
                ))
            }
    }

    /** Updates the settings of an owned Board and the contacts allowed to subscribe. */
    fun updateBoard(
        board: Board,
        title: String,
        allowReplies: Boolean,
        authorizedContactUris: Set<String>,
        authorizedCircleIds: Set<String>
    ): Completable {
        val authorized = resolveAuthorized(authorizedContactUris, authorizedCircleIds)
        val settings = HashMap<String, String>()
        val cleanTitle = title.trim()
        if (cleanTitle.isNotEmpty() && cleanTitle != board.title)
            settings["title"] = cleanTitle
        if (allowReplies != board.allowReplies)
            settings["feedReplies"] = if (allowReplies) "true" else "false"
        val update = if (settings.isEmpty()) Completable.complete()
            else accountService.updateFeed(board.accountId, board.id, settings).flatMapCompletable { ok ->
                if (ok) Completable.complete() else Completable.error(IllegalStateException(context.getString(R.string.board_error_update)))
            }
        return update
            .andThen(setAccess(board.accountId, board.id, authorized - board.authorizedUris, true))
            .andThen(setAccess(board.accountId, board.id, board.authorizedUris - authorized, false))
    }

    /** Permanently closes an owned Board: subscribers lose access to it. */
    fun closeBoard(board: Board): Completable =
        accountService.updateFeed(board.accountId, board.id, mapOf("feedClosed" to "true"))
            .flatMapCompletable { ok ->
                if (ok) Completable.complete() else Completable.error(IllegalStateException(context.getString(R.string.board_error_close)))
            }

    /** Subscribes to (or unsubscribes from) a Board offered by a contact. */
    fun subscribe(board: Board, subscribed: Boolean): Completable =
        accountService.subscribeFeed(board.accountId, board.id, subscribed)
            .flatMapCompletable { ok ->
                if (ok) Completable.complete()
                else Completable.error(IllegalStateException(
                    context.getString(if (subscribed) R.string.board_error_unavailable else R.string.board_error_unsubscribe)))
            }

    /** Publishes a text on an owned Board. */
    fun publishText(boardId: String, text: String): Completable {
        val account = accountService.currentAccount
            ?: return Completable.error(IllegalStateException(context.getString(R.string.board_error_no_account)))
        val conversation = account.getSwarm(boardId)
            ?: return Completable.error(IllegalStateException(context.getString(R.string.board_error_not_found)))
        return conversationFacade.sendTextMessage(conversation, conversation.uri, text.trim())
    }

    private fun setAccess(accountId: String, feedId: String, uris: Set<String>, allowed: Boolean): Completable =
        Observable.fromIterable(uris)
            .concatMapSingle { uri ->
                accountService.setFeedAccess(accountId, feedId, uri, allowed)
                    .doOnSuccess { ok -> if (!ok) Log.w(TAG, "Feed access ${if (allowed) "grant" else "revoke"} failed for $uri") }
            }
            .ignoreElements()

    private fun selfId(account: Account): String = normalize(account.uri.orEmpty())

    private fun normalize(uri: String): String = Uri.fromString(uri.trim()).rawRingId

    companion object {
        private val TAG = BoardRepository::class.simpleName!!
    }
}
