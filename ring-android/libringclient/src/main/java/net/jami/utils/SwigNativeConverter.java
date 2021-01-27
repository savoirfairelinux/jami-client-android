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

package net.jami.utils;

import net.jami.daemon.MessageVect;
import net.jami.daemon.StringMap;
import net.jami.daemon.StringVect;
import net.jami.daemon.VectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.jami.daemon.Message;

public class SwigNativeConverter {

    public static net.jami.daemon.VectMap toSwig(List<Map<String, String>> creds) {
        net.jami.daemon.VectMap toReturn = new VectMap();
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
