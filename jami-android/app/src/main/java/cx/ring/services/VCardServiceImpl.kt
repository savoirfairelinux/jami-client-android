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
package cx.ring.services

import android.content.Context
import net.jami.services.VCardService
import ezvcard.VCard
import net.jami.utils.VCardUtils
import android.graphics.Bitmap
import android.util.Base64
import cx.ring.utils.BitmapUtils
import ezvcard.parameter.ImageType
import ezvcard.property.Photo
import java.io.ByteArrayOutputStream
import ezvcard.property.RawProperty
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Profile
import java.io.File
import java.io.IOException

class VCardServiceImpl(private val mContext: Context) : VCardService() {
    override fun loadProfile(account: Account): Observable<Profile> = loadProfile(mContext, account)

    override fun loadSmallVCard(accountId: String, maxSize: Int): Maybe<VCard> =
        VCardUtils.loadLocalProfileFromDisk(mContext.filesDir, accountId)
            .filter { vcard: VCard -> !VCardUtils.isEmpty(vcard) }
            .map { vcard: VCard ->
                if (vcard.photos.isNotEmpty()) {
                    // Reduce photo to fit in maxSize, assuming JPEG compress with ratio of at least 8
                    val data = vcard.photos[0].data
                    val photo = BitmapUtils.bytesToBitmap(data, maxSize * 8)
                    val stream = ByteArrayOutputStream()
                    photo.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                    vcard.removeProperties(Photo::class.java)
                    vcard.addPhoto(Photo(stream.toByteArray(), ImageType.JPEG))
                }
                vcard.removeProperties(RawProperty::class.java)
                vcard
            }

    override fun saveVCardProfile(accountId: String, uri: String?, displayName: String?, picture: String?): Single<VCard> =
        Single.fromCallable { VCardUtils.writeData(uri, displayName, Base64.decode(picture, Base64.DEFAULT)) }
            .flatMap { vcard: VCard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, mContext.filesDir) }

    override fun loadVCardProfile(vcard: VCard): Single<Profile> = Companion.loadVCardProfile(vcard)

    override fun loadVCard(vcard: File): Single<Profile> =
        Single.fromCallable { readData(VCardUtils.loadFromDisk(vcard)) }
            .onErrorReturn { Profile.EMPTY_PROFILE }
            .subscribeOn(Schedulers.io())
            .cache()

    override fun loadConversationProfile(info: Map<String, String>): Single<Profile> =
        Single.fromCallable {
            val title = info["title"]
            Profile(if (title.isNullOrBlank()) null else title, BitmapUtils.base64ToBitmap(info["avatar"]), info["description"])
        }
            .subscribeOn(Schedulers.computation())
            .cache()

    override fun base64ToBitmap(base64: String?): Any? = BitmapUtils.base64ToBitmap(base64)

    companion object {
        fun loadProfile(context: Context, account: Account): Observable<Profile> {
            synchronized(account) {
                var ret = account.loadedProfile
                if (ret == null) {
                    ret = loadLocalProfileFromDiskWithDefault(context.filesDir, context.cacheDir, account.accountId)
                        .subscribeOn(Schedulers.io())
                        .cache()
                    account.loadedProfile = ret
                }
                return account.loadedProfileObservable
            }
        }

        fun loadVCardProfile(vcard: VCard): Single<Profile> = Single.fromCallable {
            readData(vcard)
        }

        fun readData(vcard: VCard?): Profile = readData(VCardUtils.readData(vcard))

        fun readData(profile: Pair<String?, ByteArray?>): Profile =
            Profile(profile.first, BitmapUtils.bytesToBitmap(profile.second))

        @Throws(IOException::class)
        fun loadPeerProfileFromDisk(filesDir: File, cacheDir: File, filename: String, accountId: String): Profile {
            val cacheFolder = VCardUtils.peerProfileCachePath(cacheDir, accountId)
            val cacheName = File(cacheFolder, "$filename.txt")
            val cachePicture = File(cacheFolder, filename)
            val profileFile = File(VCardUtils.peerProfilePath(filesDir, accountId), "$filename.vcf")
            return loadProfileWithCache(cacheName, cachePicture, profileFile)
        }

        @Throws(IOException::class)
        fun loadLocalProfileFromDisk(filesDir: File, cacheDir: File, accountId: String): Profile {
            val cacheFolder = VCardUtils.localProfileCachePath(cacheDir, accountId)
            val cacheName = File(cacheFolder, "${VCardUtils.ACCOUNT_PROFILE_NAME}.txt")
            val cachePicture = File(cacheFolder, "${VCardUtils.ACCOUNT_PROFILE_NAME}_pic")
            val profileFile = File(VCardUtils.localProfilePath(filesDir, accountId), VCardUtils.LOCAL_USER_VCARD_NAME)
            return loadProfileWithCache(cacheName, cachePicture, profileFile)
        }
        fun loadLocalProfileFromDiskWithDefault(filesDir: File, cacheDir: File, accountId: String): Single<Profile> =
            Single.fromCallable { loadLocalProfileFromDisk(filesDir, cacheDir, accountId) }
                .onErrorReturn { Profile.EMPTY_PROFILE }

        fun loadProfileWithCache(cacheName: File, cachePicture: File, profileFile: File): Profile {
            // Case 1: no profile for this peer
            if (!profileFile.exists()) {
                return Profile.EMPTY_PROFILE
            }

            // Case 2: read profile from cache
            if (cacheName.exists() && cacheName.lastModified() >= profileFile.lastModified()) {
                return Profile(
                    cacheName.readText(),
                    if (cachePicture.exists()) BitmapUtils.bytesToBitmap(cachePicture.readBytes()) else null
                )
            }

            // Case 3: read profile from disk and update cache
            val (name, picture) = VCardUtils.readData(VCardUtils.loadFromDisk(profileFile))
            cacheName.writeText(name ?: "")
            if (picture != null) {
                BitmapUtils.bytesToBitmap(picture)?.let { bitmap ->
                    BitmapUtils.createScaledBitmap(bitmap, 512).apply {
                        if (this === bitmap) {
                            // Case 3a: bitmap is already small enough, cache it as-is
                            Schedulers.io().createWorker().schedule {
                                cachePicture.outputStream().use {
                                    it.write(picture)
                                }
                            }
                            return Profile(name, bitmap)
                        }
                        bitmap.recycle()
                    }
                }?.let { scaledBitmap ->
                    // Case 3b: bitmap is too big, reduce it and write to cache
                    Schedulers.io().createWorker().schedule {
                        cachePicture.outputStream().use {
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
                        }
                    }
                    return Profile(name, scaledBitmap)
                }
            }
            return Profile(name, null)
        }
    }
}