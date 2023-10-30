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
package net.jami.utils

import java.lang.StringBuilder

class ProfileChunk(private val numberOfParts: Int) {
    private var mTotalSize = 0
    private var mInsertedParts: Int = 0
    private val mParts: Array<String> = Array(numberOfParts) { "" }

    /**
     * Inserts a profile part in the data structure, at a given position
     *
     * @param part  the part to insert
     * @param index the given position to insert the part
     */
    fun addPartAtIndex(part: String, index: Int) {
        mParts[index - 1] = part
        mTotalSize += part.length
        mInsertedParts++
        Log.d(TAG, "Inserting part $part at index $index")
    }

    /**
     * Tells if the profile is complete: all the needed parts have been gathered
     *
     * @return true if complete, false otherwise
     */
    val isProfileComplete: Boolean
        get() = mInsertedParts == numberOfParts

    /**
     * Builds the profile based on the gathered parts.
     *
     * @return the complete profile as a String
     */
    val completeProfile: String?
        get() = if (isProfileComplete) {
            val stringBuilder = StringBuilder(mTotalSize)
            for (part in mParts)
                stringBuilder.append(part)
            stringBuilder.toString()
        } else null

    companion object {
        val TAG = ProfileChunk::class.simpleName!!
    }
}