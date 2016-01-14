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
import java.util.Map;
import java.util.Set;

import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.model.account.AccountDetailSrtp;
import cx.ring.model.account.AccountDetailTls;
import cx.ring.service.Blob;
import cx.ring.service.ServiceConstants;
import cx.ring.service.StringMap;
import cx.ring.service.StringVect;
import cx.ring.service.VectMap;

public class SwigNativeConverter {

    public static VectMap convertFromNativeToSwig(List creds) {
        ArrayList<HashMap<String, String>> todecode = (ArrayList<HashMap<String, String>>) creds;
        VectMap toReturn = new VectMap();

        for (HashMap<String, String> aTodecode : todecode) {
            toReturn.add(StringMap.toSwig(aTodecode));
        }
        return toReturn;
    }

    public static Blob convertFromNativeToSwig(byte[] data) {
        Blob toReturn = new Blob();
        toReturn.reserve(data.length);
        for (int i=0; i<data.length; i++)
            toReturn.add(data[i]);
        return toReturn;
    }

    private static String tryToGet(StringMap smap, String key) {
        if (smap.has_key(key)) {
            return smap.get(key);
        } else {
            return "";
        }
    }

    public static ArrayList<String> convertSwigToNative(StringVect vector) {
        ArrayList<String> toReturn = new ArrayList<>();
        for (int i = 0; i < vector.size(); ++i) {
            toReturn.add(vector.get(i));
        }
        return toReturn;
    }
}
