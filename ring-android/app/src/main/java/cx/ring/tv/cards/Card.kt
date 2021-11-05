/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified by: Loïc Siret <loic.siret@savoirfairelinux.com>
 *
 */
package cx.ring.tv.cards

import android.content.Context
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

open class Card {
    @DrawableRes
    var localImageResource = -1
    private var mDrawable: Drawable? = null
    var title = ""
    var description: CharSequence = ""
    var type: Type? = null
    //var id: Long = 0
    var width = 0
    var height = 0

    /*not used at the moment but will be use in futur*/
    private var mFooterColor: String? = null

    fun setDrawable(bitmapDrawable: Drawable?) {
        mDrawable = bitmapDrawable
    }

    fun getDrawable(context: Context): Drawable {
        return mDrawable ?: if (localImageResource != -1) ContextCompat.getDrawable(context, localImageResource)!! else ColorDrawable(0x00000000)
    }

    /*not used at the moment but will be use in futur*/
    val footerColor: Int
        get() = if (mFooterColor == null) -1 else Color.parseColor(mFooterColor)

    /*not used at the moment but will be use in futur*/
    fun setFooterColor(footerColor: String) {
        mFooterColor = footerColor
    }

    enum class Type {
        DEFAULT,
        SEARCH_RESULT,
        ACCOUNT_ADD_DEVICE,
        ACCOUNT_EDIT_PROFILE,
        ACCOUNT_SHARE_ACCOUNT,
        ADD_CONTACT,
        CONTACT,
        CONTACT_ONLINE,
        CONTACT_WITH_USERNAME,
        CONTACT_WITH_USERNAME_ONLINE,
        CONTACT_REQUEST,
        CONTACT_REQUEST_WITH_USERNAME
    }
}