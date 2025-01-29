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
package net.jami.model

import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.*
import net.jami.utils.Log

class Contact constructor(val uri: Uri, val isUser: Boolean = false) {
    enum class Status {
        BLOCKED, REQUEST_SENT, CONFIRMED, NO_REQUEST
    }

    var username: Single<String>? = null
    var presenceUpdates: Observable<PresenceStatus>? = null
    private var mContactPresenceEmitter: Emitter<PresenceStatus>? = null

    private val loadedProfileSubject: BehaviorSubject<Profile> = BehaviorSubject.create()
    private val customProfileSubject: BehaviorSubject<Profile> = BehaviorSubject.create()
    private val profileSubject: BehaviorSubject<Profile> = BehaviorSubject.create()
    val profile: Observable<Profile> = profileSubject

    var loadedProfile: Single<Profile>?
        get() = loadedProfileSubject.value?.let {Single.just(it)}
        set(profile) {
            profile?.subscribe(
                {loadedProfileSubject.onNext(it)},
                {error -> Log.e(TAG,"Error setting loaded profile: ${error.message}")}
            )
        }

    var customProfile: Single<Profile>?
        get() = customProfileSubject.value?.let {Single.just(it)}
        set(profile) {
            profile?.subscribe(
                {customProfileSubject.onNext(it)},
                {error -> Log.e(TAG,"Error setting custom profile: ${error.message}")}
            )
        }

    init {
        Observable.combineLatest(
            loadedProfileSubject.distinctUntilChanged(),
            customProfileSubject.distinctUntilChanged(),
            ::mergeProfile
        ).subscribe(profileSubject)
    }

    var photoId: Long = 0
        private set
    val phones = ArrayList<Phone>()
    var isStared = false
        private set
    var isFromSystem = false
    var status = Status.NO_REQUEST
    var addedDate: Date? = null
    var id: Long = 0
    private var mLookupKey: String? = null
    private val mConversationUri: BehaviorSubject<Uri> = BehaviorSubject.createDefault(uri)
    private val mContactUpdates: Subject<Contact> = BehaviorSubject.create()
    var updates: Observable<Contact>? = null

    fun setConversationUri(conversationUri: Uri) {
        mConversationUri.onNext(conversationUri)
    }

    val conversationUri: Observable<Uri>
        get() = mConversationUri
    val updatesSubject: Observable<Contact>
        get() = mContactUpdates

    fun setPresenceEmitter(emitter: Emitter<PresenceStatus>?) {
        mContactPresenceEmitter?.let { e ->
            if (e != emitter)
                e.onComplete()
        }
        mContactPresenceEmitter = emitter
        if (emitter == null) {
            presenceUpdates = null
        }
    }

    enum class PresenceStatus {
        OFFLINE,
        AVAILABLE,
        CONNECTED
    }

    fun setPresence(present: PresenceStatus) {
        mContactPresenceEmitter?.onNext(present)
    }

    fun setSystemId(id: Long) {
        this.id = id
    }

    fun setSystemContactInfo(id: Long, k: String?, displayName: String, photo_id: Long) {
        this.id = id
        mLookupKey = k
        loadedProfile = Single.just(Profile(displayName, null))
        photoId = photo_id
        if (username == null && displayName.startsWith(Uri.RING_URI_SCHEME)
            || displayName.startsWith(Uri.JAMI_URI_SCHEME)) {
            username = Single.just(displayName)
        }
    }

    private fun hasNumber(number: Uri?): Boolean {
        if (number == null || number.isEmpty) return false
        for (p in phones) if (p.number.toString() == number.toString()) return true
        return false
    }

    override fun toString(): String {
        //username?.let { username -> if (username.isNotEmpty()) return@toString username }
        return uri.rawUriString
    }

    val primaryNumber: String
        get() = uri.rawRingId

    fun setStared() {
        isStared = true
    }

    fun addPhoneNumber(tel: Uri, cat: Int, label: String?) {
        if (!hasNumber(tel)) phones.add(Phone(tel, cat, label))
    }

    fun addNumber(tel: String, cat: Int, label: String?, type: Phone.NumberType) {
        val uri = Uri.fromString(tel)
        if (!hasNumber(uri)) phones.add(Phone(uri, cat, label, type))
    }

    fun addNumber(tel: Uri, cat: Int, label: String?, type: Phone.NumberType) {
        if (!hasNumber(tel)) phones.add(Phone(tel, cat, label, type))
    }

    val isBlocked: Boolean
        get() = status == Status.BLOCKED

    private fun mergeProfile(primary: Profile, custom: Profile): Profile {
        val mergedName = custom.displayName ?: primary.displayName
        val mergedPicture = custom.avatar ?: primary.avatar
        return Profile(mergedName, mergedPicture)
    }

    fun setProfile(profile: Single<Profile>) {
        profile.subscribe(
            { loadedProfileSubject.onNext(it) },
            { error -> Log.e(TAG,"Error setting loaded profile: ${error.message}") }
        )
    }

    fun setProfile(profile: Profile) {
        loadedProfileSubject.onNext(profile)
    }

    fun setCustomProfile(profile: Profile) {
        customProfileSubject.onNext(profile)
    }

    companion object {
        private val TAG = Contact::class.simpleName!!
        const val UNKNOWN_ID = -1L
        const val DEFAULT_ID = 0L

        fun buildSIP(to: Uri): Contact = Contact(to).apply { username = Single.just("") }
        fun build(uri: String, isUser: Boolean = false) = Contact(Uri.fromString(uri), isUser)
    }
}
