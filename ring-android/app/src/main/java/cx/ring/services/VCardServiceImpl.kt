/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
import net.jami.utils.Tuple
import java.io.File

class VCardServiceImpl(private val mContext: Context) : VCardService() {
    override fun loadProfile(account: Account): Observable<Tuple<String?, Any?>> {
        return loadProfile(mContext, account)
    }

    override fun loadSmallVCard(accountId: String, maxSize: Int): Maybe<VCard> {
        return VCardUtils.loadLocalProfileFromDisk(mContext.filesDir, accountId)
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
    }

    override fun saveVCardProfile(accountId: String, uri: String, displayName: String, picture: String): Single<VCard> {
        return Single.fromCallable { VCardUtils.writeData(uri, displayName, Base64.decode(picture, Base64.DEFAULT)) }
            .flatMap { vcard: VCard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, mContext.filesDir) }
    }

    override fun loadVCardProfile(vcard: VCard): Single<Tuple<String?, Any?>> {
        return Single.fromCallable { readData(vcard) }
    }

    override fun peerProfileReceived(accountId: String, peerId: String, vcardFile: File): Single<Tuple<String?, Any?>> {
        return VCardUtils.peerProfileReceived(mContext.filesDir, accountId, peerId, vcardFile)
            .map { vcard -> readData(vcard) }
    }

    override fun base64ToBitmap(base64: String): Any? {
        return BitmapUtils.base64ToBitmap(base64)
    }

    companion object {
        fun loadProfile(context: Context, account: Account): Observable<Tuple<String?, Any?>> {
            synchronized(account) {
                var ret = account.loadedProfile
                if (ret == null) {
                    ret = VCardUtils.loadLocalProfileFromDiskWithDefault(context.filesDir, account.accountID)
                        .map { vcard: VCard -> readData(vcard) }
                        .subscribeOn(Schedulers.computation())
                        .cache()
                    account.loadedProfile = ret
                }
                return account.loadedProfileObservable
            }
        }

        fun readData(vcard: VCard?): Tuple<String?, Any?> {
            return readData(VCardUtils.readData(vcard))
        }

        fun readData(profile: Tuple<String?, ByteArray?>): Tuple<String?, Any?> {
            return Tuple(profile.first, BitmapUtils.bytesToBitmap(profile.second))
        }
    }
}