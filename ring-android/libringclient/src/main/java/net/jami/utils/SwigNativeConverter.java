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

package net.jami.utils;

import net.jami.daemon.MessageVect;
import net.jami.daemon.StringMap;
import net.jami.daemon.StringVect;
import net.jami.daemon.VectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.jami.daemon.Message;
import net.jami.model.Media;

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

    public static VectMap convert(List<Media> mediaList) {
        VectMap vectMapMedia = new VectMap();
        for (Media media : mediaList) {
            vectMapMedia.add(StringMap.toSwig(media.toMap()));
        }
        return vectMapMedia;
    }

    public static ArrayList<Media> convert(VectMap mediaList) {
        ArrayList<Map<String, String>> nMediaList = mediaList == null ? new ArrayList<>() : mediaList.toNative();
        ArrayList<Media> medias = new ArrayList<>();
        for (Map<String, String> mediaMap : nMediaList) {
            medias.add(new Media(mediaMap));
        }
        return medias;
    }
}
