/*
 * Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package cx.ring.channels

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.ChannelMetadata
import net.jami.services.AccountService
import net.jami.utils.Log
import org.json.JSONObject

typealias Channel = net.jami.model.Channel

class ChannelRepository(
    context: Context,
    private val accountService: AccountService,
    private val accountId: String?
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val activeKey = "active_id_$accountId"
    private val migratedKey = "sync_migrated_$accountId"
    private val localMetadataKey = "sip_metadata_$accountId"

    fun load(): List<Channel> {
        if (accountService.getAccount(accountId) == null) return ChannelMetadata.defaults
        val metadata = readMetadata()
        val legacy = if (preferences.getBoolean(migratedKey, false)) null
            else preferences.getString("channels_$accountId", null)
        return ChannelMetadata.channels(if (legacy == null) metadata else
            metadata + ChannelMetadata.migrate(ChannelMetadata.legacyChannels(legacy), metadata))
    }

    fun observe(): Observable<List<Channel>> {
        val id = accountId ?: return Observable.just(ChannelMetadata.defaults)
        val localChanges = Observable.create<Unit> { emitter ->
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == activeKey || key == localMetadataKey) emitter.onNext(Unit)
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitter.setCancellable { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return accountService.getAccountSingle(id).flatMapObservable { account ->
            account.loaded.andThen(migrate()).andThen(
                if (account.isJami)
                    Observable.merge(accountService.observeAccountMetadata(id).map { Unit }, localChanges)
                else localChanges.startWithItem(Unit)
            )
        }.map { load() }
    }

    var activeChannelId: String
        get() {
            val channels = load()
            val stored = preferences.getString(activeKey, null)
            val legacyName = ChannelMetadata.migrateName(preferences.getString("active_$accountId", null))
            return channels.firstOrNull { it.id == stored }?.id
                ?: if (stored == null) channels.firstOrNull { it.name == legacyName }?.id ?: ChannelMetadata.ALL_ID
                else ChannelMetadata.ALL_ID
        }
        set(value) {
            requireNotNull(accountService.getAccount(accountId)) { "Channel account is unavailable" }
            require(load().any { it.id == value }) { "Channel no longer exists" }
            preferences.edit().putString(activeKey, value).apply()
        }

    val activeChannelName: String get() = activeChannel().name

    fun activeChannel(): Channel {
        val channels = load()
        return channels.firstOrNull { it.id == activeChannelId } ?: ChannelMetadata.defaults.first()
    }

    fun create(name: String): Completable = mutate { ChannelMetadata.create(it, name) }

    fun rename(channelId: String, name: String): Completable =
        mutate { ChannelMetadata.rename(it, channelId, name) }

    fun delete(channelId: String): Completable =
        mutate { ChannelMetadata.delete(it, channelId) }

    fun setMemberships(uri: String, group: Boolean, changes: Map<String, Boolean>): Completable =
        mutate { ChannelMetadata.setMemberships(it, uri, group, changes) }

    fun addGroup(channelId: String, conversationUri: String): Completable =
        setMemberships(conversationUri, true, mapOf(channelId to true))

    private fun mutate(updates: (Map<String, String>) -> Map<String, String>): Completable =
        migrate().andThen(update(updates))
            .doOnError { error -> Log.e("ChannelRepository", "Unable to update Channels", error) }
            .cache()

    private fun readMetadata(): Map<String, String> {
        val account = requireNotNull(accountService.getAccount(accountId)) { "Channel account is unavailable" }
        if (account.isJami) return accountService.getAccountMetadata(account.accountId)
        val stored = preferences.getString(localMetadataKey, null) ?: return emptyMap()
        return JSONObject(stored).let { json ->
            json.keys().asSequence().associateWith { json.getString(it) }
        }
    }

    private fun update(
        updates: (Map<String, String>) -> Map<String, String>,
        onlyIfAbsent: Boolean = false
    ): Completable =
        Completable.defer {
            val account = requireNotNull(accountService.getAccount(accountId)) { "Channel account is unavailable" }
            if (account.isJami) accountService.updateAccountMetadata(account.accountId, updates, onlyIfAbsent)
            else Completable.fromAction {
                // SIP accounts have no Jami linked devices; preserve their existing local Channels.
                synchronized(preferences) {
                    val current = readMetadata()
                    val changed = updates(current).filterKeys { !onlyIfAbsent || it !in current }
                    if (changed.isNotEmpty()) check(preferences.edit()
                        .putString(localMetadataKey, JSONObject(current + changed).toString()).commit()) {
                        "Unable to persist Channels"
                    }
                }
            }.subscribeOn(Schedulers.io())
        }.cache()

    private fun migrate(): Completable = Completable.defer {
        require(!accountId.isNullOrBlank()) { "Channel account is unavailable" }
        if (preferences.getBoolean(migratedKey, false)) return@defer Completable.complete()
        update({ current ->
            val legacy = synchronized(preferences) {
                preferences.getString("channels_$accountId", null) ?: run {
                    val stored = preferences.getString("channels", null)
                    val owner = preferences.getString(LEGACY_OWNER, null)
                    if (stored == null || (owner != null && owner != accountId)) null
                    else {
                        // Claim old unscoped data durably for one real account, even if import fails.
                        check(preferences.edit().putString(LEGACY_OWNER, accountId).commit()) {
                            "Unable to claim legacy Channels"
                        }
                        stored
                    }
                }
            }
            if (legacy == null) emptyMap()
            else ChannelMetadata.migrate(ChannelMetadata.legacyChannels(legacy), current)
        }, onlyIfAbsent = true).andThen(Completable.fromAction {
            synchronized(preferences) {
                val active = preferences.getString("active_$accountId", null)
                    ?: if (preferences.getString(LEGACY_OWNER, null) == accountId)
                        preferences.getString("active", null) else null
                val migratedActive = load().firstOrNull { it.name == ChannelMetadata.migrateName(active) }?.id
                    ?: ChannelMetadata.ALL_ID
                val editor = preferences.edit().putBoolean(migratedKey, true).remove("channels_$accountId")
                if (!preferences.contains(activeKey)) editor.putString(activeKey, migratedActive)
                if (preferences.getString(LEGACY_OWNER, null) == accountId) {
                    editor.remove("channels").remove("active")
                }
                check(editor.commit()) { "Unable to complete Channel migration" }
            }
        })
    }.cache()

    companion object {
        private const val PREFERENCES = "jami_channels"
        private const val LEGACY_OWNER = "legacy_owner_account"
        const val ALL_CHANNEL = ChannelMetadata.ALL_NAME
        val DEFAULT_CHANNELS = ChannelMetadata.defaults.map { it.name }
    }
}
