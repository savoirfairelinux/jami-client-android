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

import android.graphics.drawable.Drawable

abstract class Card(val type: Type, val title: String = "", val description: CharSequence = "") {
    var drawable: Drawable? = null

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
        CONTACT_WITH_USERNAME_ONLINE;

        fun hasPresence() = this == CONTACT_ONLINE || this == CONTACT_WITH_USERNAME_ONLINE
    }
}