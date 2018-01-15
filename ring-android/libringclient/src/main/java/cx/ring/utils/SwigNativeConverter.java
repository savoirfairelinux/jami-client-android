/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

package cx.ring.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.VectMap;

public class SwigNativeConverter {

    public static VectMap convertFromNativeToSwig(List creds) {
        ArrayList<HashMap<String, String>> todecode = (ArrayList<HashMap<String, String>>) creds;
        VectMap toReturn = new VectMap();

        for (HashMap<String, String> aTodecode : todecode) {
            toReturn.add(StringMap.toSwig(aTodecode));
        }
        return toReturn;
    }

    public static ArrayList<String> convertSwigToNative(StringVect vector) {
        ArrayList<String> toReturn = new ArrayList<>();
        toReturn.addAll(vector);
        return toReturn;
    }
}
