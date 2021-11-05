/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import java.lang.Exception
import java.util.*

class Contact private constructor(
    val uri: Uri, // Profile
    var profileName: String?,
    val isUser: Boolean
) {
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
    private var mOnline = false
    var id: Long = 0
    private var mLookupKey: String? = null
    var detailsLoaded = false
    private val mConversationUri: BehaviorSubject<Uri> = BehaviorSubject.createDefault(uri)
    var photo: Any? = null
    private val mContactUpdates: Subject<Contact> = BehaviorSubject.create()
    var updates: Observable<Contact>? = null

    constructor(uri: Uri, user: Boolean = false) : this(uri, null, user) {
    }

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

    fun matches(query: String): Boolean {
        return (profileName != null && profileName!!.lowercase().contains(query)
                //|| username != null && username!!.contains(query)
                || primaryNumber.contains(query))
    }

    var isOnline: Boolean
        get() = mOnline
        set(present) {
            mOnline = present
            mContactPresenceEmitter?.onNext(present)
        }

    fun setSystemId(id: Long) {
        this.id = id
    }

    fun setSystemContactInfo(id: Long, k: String?, displayName: String, photo_id: Long) {
        this.id = id
        mLookupKey = k
        profileName = displayName
        photoId = photo_id
        if (username == null && displayName.contains(PREFIX_RING)) {
            username = Single.just(displayName)
        }
    }

    val ids: ArrayList<String>
        get() {
            val ret = ArrayList<String>(phones.size + if (id == UNKNOWN_ID) 0 else 1)
            if (id != UNKNOWN_ID) ret.add(
                "c:" + java.lang.Long.toHexString(id)
            )
            for (p in phones) ret.add(p.number.rawUriString)
            return ret
        }

    /*var displayName: String
        get() {
            val profileName = profileName
            return if (profileName != null && profileName.isNotEmpty()) profileName else ringUsername
        }
        set(displayName) {
            profileName = displayName
        }*/

    fun hasNumber(number: String): Boolean {
        return hasNumber(Uri.fromString(number))
    }

    fun hasNumber(number: Uri?): Boolean {
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
        if (!hasNumber(tel)) phones.add(Phone(tel, cat, label, type))
    }

    fun addNumber(tel: Uri, cat: Int, label: String?, type: Phone.NumberType) {
        if (!hasNumber(tel)) phones.add(Phone(tel, cat, label, type))
    }

    fun hasPhoto(): Boolean {
        return photo != null
    }

    val isBanned: Boolean
        get() = status == Status.BANNED

    /**
     * A contact is Unknown when his name == his phone number
     *
     * @return true when Name == Number
     */
    val isUnknown: Boolean
        get() = profileName == null || profileName.contentEquals(phones[0].number.rawUriString)

    fun setProfile(profile: Profile?) {
        if (profile == null) return
        setProfile(profile.displayName, profile.avatar)
    }

    fun setProfile(name: String?, photo: Any?) {
        if (name != null && name.isNotEmpty() && !name.startsWith(Uri.RING_URI_SCHEME)) {
            profileName = name
        }
        if (photo != null) {
            this.photo = photo
        }
        detailsLoaded = true
        mContactUpdates.onNext(this)
    }

    companion object {
        private val TAG = Contact::class.simpleName!!
        const val UNKNOWN_ID = -1L
        const val DEFAULT_ID = 0L
        const val PREFIX_RING = Uri.RING_URI_SCHEME

        fun buildSIP(to: Uri): Contact {
            val contact = Contact(to)
            contact.username = Single.just("")
            return contact
        }

        fun build(uri: String, isUser: Boolean = false): Contact {
            return Contact(Uri.fromString(uri), isUser)
        }

        fun canonicalNumber(number: String?): String? {
            return if (number == null || number.isEmpty()) null else Uri.fromString(number).rawUriString
        }

        fun contactIdFromId(id: String): Long {
            return if (!id.startsWith("c:")) UNKNOWN_ID.toLong() else try {
                id.substring(2).toLong(16)
            } catch (e: Exception) {
                UNKNOWN_ID.toLong()
            }
        }
    }
}