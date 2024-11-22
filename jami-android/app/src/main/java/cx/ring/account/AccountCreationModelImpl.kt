/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.account

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import net.jami.model.AccountCreationModel
import net.jami.utils.Log

class AccountCreationModelImpl : AccountCreationModel() {
    override fun convertProfileImageToBase64(photo: Any?): String? {
        val bitmap = photo as? Bitmap
        if (bitmap == null) return null
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Bitmap to Base64", e)
            null
        }
    }

    companion object {
        private val TAG = AccountCreationModelImpl::class.simpleName!!
    }
}