/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.utils

class Tuple<X, Y>(@JvmField val first: X, @JvmField val second: Y) {

    override fun toString(): String {
        return "($first,$second)"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Tuple<*, *>) {
            return false
        }
        val other_ = other as Tuple<X, Y>

        // this may cause NPE if nulls are valid values for first or second.
        // The logic may be improved to handle nulls properly, if needed.
        return other_.first == first && other_.second == second
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (first?.hashCode() ?: 0)
        result = prime * result + (second?.hashCode() ?: 0)
        return result
    }

}