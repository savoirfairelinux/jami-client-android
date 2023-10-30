/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
import net.jami.model.ContactViewModel
import net.jami.model.Conversation
import net.jami.model.Profile
import net.jami.smartlist.ConversationItemViewModel

object AvatarFactory {
    const val SIZE_AB = 36
    const val SIZE_NOTIF = 48
    const val SIZE_PADDING = 8

    fun getAvatar(context: Context, contact: ContactViewModel, presence: Boolean = true): Single<Drawable> =
        Single.fromCallable {
            AvatarDrawable.Builder()
                .withContact(contact)
                .withCircleCrop(true)
                .withPresence(presence)
                .build(context)
        }

    fun getAvatar(context: Context, conversation: Conversation, profile: Profile, contacts: List<ContactViewModel>, presence: Boolean): Single<AvatarDrawable> =
        Single.fromCallable {
            AvatarDrawable.Builder()
                .withConversation(conversation, profile, contacts)
                .withCircleCrop(true)
                .withPresence(presence)
                .build(context)
        }

    fun getAvatar(context: Context, vm: ConversationItemViewModel): Single<AvatarDrawable> =
        Single.fromCallable {
            AvatarDrawable.Builder()
                .withViewModel(vm)
                .withCircleCrop(true)
                .build(context)
        }

    fun getBitmapAvatar(context: Context, conversation: Conversation, profile: Profile, contacts: List<ContactViewModel>, size: Int, presence: Boolean): Single<Bitmap> =
        getAvatar(context, conversation, profile, contacts, presence)
            .map { BitmapUtils.drawableToBitmap(it, size) }

    fun getBitmapAvatar(context: Context, contact: ContactViewModel, size: Int, presence: Boolean): Single<Bitmap> =
        getAvatar(context, contact, presence)
            .map { BitmapUtils.drawableToBitmap(it, size) }

    fun getBitmapAvatar(context: Context, contact: ContactViewModel, size: Int): Single<Bitmap> =
        getBitmapAvatar(context, contact, size, true)

    fun getBitmapAvatar(context: Context, account: Account, size: Int): Single<Bitmap> =
        AvatarDrawable.load(context, account)
            .firstOrError()
            .map { BitmapUtils.drawableToBitmap(it, size) }

    private fun getDrawable(context: Context, photo: Bitmap?, profileName: String?, username: String?, id: String): Drawable =
        AvatarDrawable.Builder()
            .withPhoto(photo)
            .withName(if (TextUtils.isEmpty(profileName)) username else profileName)
            .withId(id)
            .withCircleCrop(true)
            .build(context)

    fun clearCache() {}

    private fun <T> getGlideRequest(context: Context, request: RequestBuilder<T>, photo: Bitmap?, profileName: String?, username: String?, id: String): RequestBuilder<T> =
        request.load(getDrawable(context, photo, profileName, username, id))

    private fun getGlideAvatar(context: Context, manager: RequestManager, contact: ContactViewModel): RequestBuilder<Drawable> =
        getGlideRequest(context, manager.asDrawable(), contact.profile.avatar as Bitmap?, contact.profile.displayName, contact.registeredName, contact.contact.primaryNumber)

    private fun getGlideAvatar(context: Context, contact: ContactViewModel): RequestBuilder<Drawable> =
        getGlideAvatar(context, Glide.with(context), contact)

    fun loadGlideAvatar(view: ImageView, contact: ContactViewModel) = getGlideAvatar(view.context, contact).into(view)

}