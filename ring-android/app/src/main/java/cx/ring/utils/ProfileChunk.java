/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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

package cx.ring.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import cx.ring.service.StringVect;

public class ProfileChunk {
    public final static String TAG = ProfileChunk.class.getSimpleName();

    private long mNumberOfParts;
    private long mInsertedParts;
    private StringVect mParts;

    /**
     * Constructor
     * @param numberOfParts Number of part to complete the Profile
     */
    public ProfileChunk(long numberOfParts) {
        Log.d(TAG, "Create ProfileChink of size " + numberOfParts);
        this.mInsertedParts = 0;
        this.mNumberOfParts = numberOfParts;
        this.mParts = new StringVect(mNumberOfParts + 1);
    }

    /**
     * Inserts a profile part in the data structure, at a given position
     * @param part the part to insert
     * @param index the given position to insert the part
     */
    public void addPartAtIndex(@NonNull String part, int index) {
        this.mParts.set(index, part);
        this.mInsertedParts++;
        Log.d(TAG, "Inserting part " + part + " at index " + index);
    }

    /**
     * Tells if the profile is complete: all the needed parts have been gathered
     * @return true if complete, false otherwise
     */
    public boolean isProfileComplete() {
        return this.mInsertedParts == this.mNumberOfParts;
    }

    /**
     * Builds the profile based on the gathered parts.
     * @return the complete profile as a String
     */
    @Nullable
    public String getCompleteProfile() {
        if (this.isProfileComplete()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < this.mParts.size(); ++i) {
                stringBuilder.append(this.mParts.get(i));
            }
            return stringBuilder.toString();
        }
        else {
            return null;
        }
    }
}
