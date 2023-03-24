/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model

import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.*

class Contact constructor(val uri: Uri, val isUser: Boolean = false) {
    enum class Status {
        BANNED, REQUEST_SENT, CONFIRMED, NO_REQUEST
    }

    var username: Single<String>? = null
    var presenceUpdates: Observable<Boolean>? = null
    private var mContactPresenceEmitter: Emitter<Boolean>? = null
    private val profileSubject: Subject<Single<Profile>> = BehaviorSubject.create()
    val profile: Observable<Profile> = profileSubject.switchMapSingle { single -> single }
    var loadedProfile: Single<Profile>? = null
        set(profile) {
            field = profile
            if  (profile != null)
                profileSubject.onNext(profile)
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

    fun setPresenceEmitter(emitter: Emitter<Boolean>?) {
        mContactPresenceEmitter?.let { e ->
            if (e != emitter)
                e.onComplete()
        }
        mContactPresenceEmitter = emitter
    }

    fun setPresence(present: Boolean) {
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
        if (username == null && displayName.contains(PREFIX_RING)) {
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

    val isBanned: Boolean
        get() = status == Status.BANNED

    fun setProfile(profile: Single<Profile>) {
        loadedProfile = profile
    }
    fun setProfile(profile: Profile?) {
        if (profile != null)
            loadedProfile = Single.just(profile)
    }

    companion object {
        private val TAG = Contact::class.simpleName!!
        const val UNKNOWN_ID = -1L
        const val DEFAULT_ID = 0L
        const val PREFIX_RING = Uri.RING_URI_SCHEME

        fun buildSIP(to: Uri): Contact = Contact(to).apply { username = Single.just("") }
        fun build(uri: String, isUser: Boolean = false) = Contact(Uri.fromString(uri), isUser)
    }
}