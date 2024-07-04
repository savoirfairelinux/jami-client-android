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
package net.jami.utils

import net.jami.daemon.*
import java.util.ArrayList

object SwigNativeConverter {
    fun toSwig(creds: List<Map<String, String>>): VectMap =
        creds.mapTo(VectMap().apply { reserve(creds.size) }) {
            StringMap.toSwig(it)
        }

    fun toJava(vector: StringVect): ArrayList<String> = ArrayList(vector)

    fun toJava(vector: MessageVect): ArrayList<Message> =
        vector.mapTo(ArrayList<Message>(vector.size)) { it }
}