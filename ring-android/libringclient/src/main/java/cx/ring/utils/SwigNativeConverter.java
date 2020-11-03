/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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

package cx.ring.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cx.ring.daemon.Message;
import cx.ring.daemon.MessageVect;
import cx.ring.daemon.StringMap;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.VectMap;

public class SwigNativeConverter {

    public static VectMap toSwig(List<Map<String, String>> creds) {
        VectMap toReturn = new VectMap();
        toReturn.reserve(creds.size());
        for (Map<String, String> aTodecode : creds) {
            toReturn.add(StringMap.toSwig(aTodecode));
        }
        return toReturn;
    }

    public static ArrayList<String> toJava(StringVect vector) {
        return new ArrayList<>(vector);
    }

    public static ArrayList<Message> toJava(MessageVect vector) {
        int size = vector.size();
        ArrayList<Message> toReturn = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            toReturn.add(vector.get(i));
        return toReturn;
    }

}
