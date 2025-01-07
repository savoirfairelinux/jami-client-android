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
                    ret = VCardUtils.loadLocalProfileFromDiskWithDefault(context.filesDir, account.accountId)
                        .map { vcard: VCard -> readData(vcard) }
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
    }
}