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
import android.content.SharedPreferences
import cx.ring.R
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Circle
import net.jami.model.CircleMetadata
import net.jami.services.AccountService
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CircleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountService: AccountService
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val changeTrigger = BehaviorSubject.createDefault(Unit)

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        changeTrigger.onNext(Unit)
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private fun metadataKey(accountId: String) = "sip_metadata_$accountId"
    private fun migratedKey(accountId: String) = "sync_migrated_$accountId"
    private fun legacyAccountKey(accountId: String) = "channels_$accountId"

    fun load(accountId: String? = accountService.currentAccount?.accountId): List<Circle> {
        if (accountId.isNullOrBlank()) return CircleMetadata.defaults
        val metadata = readMetadata(accountId)
        val legacy = if (preferences.getBoolean(migratedKey(accountId), false)) null
        else preferences.getString(legacyAccountKey(accountId), null)
            ?: preferences.getString(LEGACY_UNSCOPED_SQUADS, null)
        val merged = if (legacy == null) metadata else {
            metadata + CircleMetadata.migrate(CircleMetadata.legacyCircles(legacy), metadata)
        }
        return CircleMetadata.circles(merged)
    }

    fun observe(accountId: String): Observable<List<Circle>> {
        if (accountId.isBlank()) return Observable.just(CircleMetadata.defaults)
        return changeTrigger.map { load(accountId) }
    }

    fun observeForCurrentAccount(): Observable<List<Circle>> {
        return accountService.currentAccountSubject.switchMap { account ->
            observe(account.accountId)
        }
    }

    /** Name to display: built-in Circles keeping their canonical name are localized. */
    fun displayName(circle: Circle): String {
        if (!CircleMetadata.hasDefaultName(circle)) return circle.name
        return context.getString(when (circle.id) {
            CircleMetadata.ALL_ID -> R.string.circle_default_all
            CircleMetadata.FRIENDS_ID -> R.string.circle_default_friends
            CircleMetadata.REAL_FRIENDS_ID -> R.string.circle_default_real_friends
            CircleMetadata.FAMILY_ID -> R.string.circle_default_family
            CircleMetadata.COLLEAGUE_ID -> R.string.circle_default_colleagues
            else -> return circle.name
        })
    }

    fun create(name: String, accountId: String? = accountService.currentAccount?.accountId): Completable =
        mutate(accountId) { CircleMetadata.create(it, name) }

    fun rename(
        circleId: String,
        name: String,
        accountId: String? = accountService.currentAccount?.accountId
    ): Completable = mutate(accountId) { CircleMetadata.rename(it, circleId, name) }

    fun delete(
        circleId: String,
        accountId: String? = accountService.currentAccount?.accountId
    ): Completable = mutate(accountId) { CircleMetadata.delete(it, circleId) }

    fun setMemberships(
        uri: String,
        group: Boolean,
        changes: Map<String, Boolean>,
        accountId: String? = accountService.currentAccount?.accountId
    ): Completable = mutate(accountId) { CircleMetadata.setMemberships(it, uri, group, changes) }

    fun setCircleMembers(
        circleId: String,
        memberUris: Set<String>,
        groupUris: Set<String>,
        accountId: String? = accountService.currentAccount?.accountId
    ): Completable = mutate(accountId) {
        CircleMetadata.setCircleMembers(it, circleId, memberUris, groupUris)
    }

    private fun readMetadata(accountId: String): Map<String, String> {
        val stored = preferences.getString(metadataKey(accountId), null) ?: return emptyMap()
        return try {
            val json = JSONObject(stored)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun mutate(
        accountId: String?,
        updates: (Map<String, String>) -> Map<String, String>
    ): Completable = Completable.fromAction {
        val id = requireNotNull(accountId?.takeIf { it.isNotBlank() }) {
            context.getString(R.string.circle_error_no_account)
        }
        synchronized(preferences) {
            val current = readMetadata(id)
            val legacy = if (preferences.getBoolean(migratedKey(id), false)) emptyMap()
            else {
                val raw = preferences.getString(legacyAccountKey(id), null)
                    ?: preferences.getString(LEGACY_UNSCOPED_SQUADS, null)
                if (raw == null) emptyMap()
                else CircleMetadata.migrate(CircleMetadata.legacyCircles(raw), current)
            }
            val base = current + legacy
            val changed = try {
                updates(base)
            } catch (e: CircleMetadata.CircleException) {
                throw IllegalArgumentException(context.getString(errorMessage(e.reason)), e)
            }
            val next = base + changed
            preferences.edit()
                .putString(metadataKey(id), JSONObject(next).toString())
                .putBoolean(migratedKey(id), true)
                .apply()
            changeTrigger.onNext(Unit)
        }
    }.subscribeOn(Schedulers.io())

    companion object {
        private fun errorMessage(reason: CircleMetadata.Reason): Int = when (reason) {
            CircleMetadata.Reason.INVALID_ID -> R.string.circle_error_invalid_id
            CircleMetadata.Reason.ALREADY_EXISTS -> R.string.circle_error_exists
            CircleMetadata.Reason.MISSING_MEMBER -> R.string.circle_error_missing_member
            CircleMetadata.Reason.NOT_EDITABLE -> R.string.circle_error_not_editable
            CircleMetadata.Reason.NOT_FOUND -> R.string.circle_error_not_found
            CircleMetadata.Reason.NAME_REQUIRED -> R.string.circle_error_name_required
            CircleMetadata.Reason.NAME_TAKEN -> R.string.circle_error_name_taken
        }

        private const val PREFERENCES = "jami_channels"
        private const val LEGACY_UNSCOPED_SQUADS = "channels"
    }
}
