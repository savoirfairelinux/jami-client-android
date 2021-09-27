/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import cx.ring.utils.BitmapUtils
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.smartlist.SmartListViewModel

object AvatarFactory {
    const val SIZE_AB = 36
    const val SIZE_NOTIF = 48
    const val SIZE_PADDING = 8
    fun loadGlideAvatar(view: ImageView, contact: Contact) {
        getGlideAvatar(view.context, contact).into(view)
    }

    fun getAvatar(context: Context, contact: Contact?, presence: Boolean): Single<Drawable> {
        return Single.fromCallable {
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .withPresence(presence)
                .build(context)
        }
    }

    fun getAvatar(context: Context, conversation: Conversation, presence: Boolean): Single<Drawable> {
        return Single.fromCallable {
            AvatarDrawable.Builder()
                .withConversation(conversation)
                .withCircleCrop(true)
                .withPresence(presence)
                .build(context)
        }
    }

    fun getAvatar(context: Context, vm: SmartListViewModel): Single<Drawable> {
        return Single.fromCallable {
            AvatarDrawable.Builder()
                .withViewModel(vm)
                .withCircleCrop(true)
                .build(context)
        }
    }

    fun getAvatar(context: Context, contact: Contact?): Single<Drawable> {
        return getAvatar(context, contact, true)
    }

    fun getBitmapAvatar(context: Context, conversation: Conversation, size: Int, presence: Boolean): Single<Bitmap> {
        return getAvatar(context, conversation, presence)
            .map { d -> BitmapUtils.drawableToBitmap(d, size) }
    }

    fun getBitmapAvatar(context: Context, contact: Contact?, size: Int, presence: Boolean): Single<Bitmap> {
        return getAvatar(context, contact, presence)
            .map { d -> BitmapUtils.drawableToBitmap(d, size) }
    }

    fun getBitmapAvatar(context: Context, contact: Contact?, size: Int): Single<Bitmap> {
        return getBitmapAvatar(context, contact, size, true)
    }

    fun getBitmapAvatar(context: Context, account: Account, size: Int): Single<Bitmap> {
        return AvatarDrawable.load(context, account)
            .firstOrError()
            .map { d -> BitmapUtils.drawableToBitmap(d, size) }
    }

    private fun getDrawable(context: Context, photo: Bitmap?, profileName: String?, username: String?, id: String): Drawable {
        return AvatarDrawable.Builder()
            .withPhoto(photo)
            .withName(if (TextUtils.isEmpty(profileName)) username else profileName)
            .withId(id)
            .withCircleCrop(true)
            .build(context)
    }

    fun clearCache() {}

    private fun <T> getGlideRequest(context: Context, request: RequestBuilder<T>, photo: Bitmap?, profileName: String?, username: String?, id: String): RequestBuilder<T> {
        return request.load(getDrawable(context, photo, profileName, username, id))
    }

    private fun getGlideAvatar(context: Context, manager: RequestManager, contact: Contact): RequestBuilder<Drawable> {
        return getGlideRequest(context, manager.asDrawable(), contact.photo as Bitmap?, contact.profileName, contact.username, contact.primaryNumber)
    }

    private fun getGlideAvatar(context: Context, contact: Contact): RequestBuilder<Drawable> {
        return getGlideAvatar(context, Glide.with(context), contact)
    }
}