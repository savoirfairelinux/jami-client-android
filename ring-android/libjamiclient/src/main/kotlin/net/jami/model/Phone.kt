/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import net.jami.model.Uri.Companion.fromString
import kotlin.jvm.JvmOverloads

class Phone {
    val numbertype: NumberType
    val number: Uri
    // Home, work, custom etc.
    val category : Int
    val label: String?

    constructor(number: Uri, category: Int) {
        numbertype = NumberType.UNKNOWN
        this.number = number
        label = null
        this.category = category
    }

    @JvmOverloads
    constructor(number: String?, category: Int, label: String? = null) {
        numbertype = NumberType.UNKNOWN
        this.category = category
        this.number = fromString(number!!)
        this.label = label
    }

    constructor(number: Uri, category: Int, label: String?) {
        numbertype = NumberType.UNKNOWN
        this.category = category
        this.number = number
        this.label = label
    }

    constructor(number: String?, category: Int, label: String?, numberType: NumberType) {
        numbertype = numberType
        this.number = fromString(number!!)
        this.label = label
        this.category = category
    }

    constructor(number: Uri, category: Int, label: String?, numberType: NumberType) {
        numbertype = numberType
        this.number = number
        this.label = label
        this.category = category
    }

    val type: NumberType
        get() = numbertype

    enum class NumberType(private val type: Int) {
        UNKNOWN(0), TEL(1), SIP(2), IP(2), RING(3);

        companion object {
            private val VALS = values()
            fun fromInteger(id: Int): NumberType {
                for (type in VALS) {
                    if (type.type == id) {
                        return type
                    }
                }
                return UNKNOWN
            }
        }
    }
}