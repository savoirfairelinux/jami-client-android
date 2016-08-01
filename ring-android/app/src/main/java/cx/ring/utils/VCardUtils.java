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

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;

import cx.ring.service.StringMap;

public class VCardUtils {
    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    /**
     * Parse the "stringmap" of the mime attributes to build a proper hashtable
     * @param stringMap the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    public static Hashtable<String, String> parseMimeAttributes(StringMap stringMap) {
        if (stringMap == null || stringMap.empty()) {
            return null;
        }
        Hashtable<String, String> messageKeyValue = new Hashtable<>();
        String origin = stringMap.keys().toString().replace("[","");
        origin = origin.replace("]","");
        String elements[] = origin.split(";");
        if (elements.length < 2) {
            return messageKeyValue;
        }
        messageKeyValue.put(VCARD_KEY_MIME_TYPE, elements[0]);
        String pairs[] = elements[1].split(",");
        for (String pair : pairs) {
            String kv[] = pair.split("=");
            messageKeyValue.put(kv[0].trim(), kv[1]);
        }
        return messageKeyValue;
    }

    /**
     * Saves a vcard string to an internal new vcf file.
     * @param vcard the string to save
     * @param filename the filename of the vcf
     * @param context the context used to open streams.
     */
    public static void saveToDisk(String vcard, String filename, Context context) {
        if (TextUtils.isEmpty(vcard) || TextUtils.isEmpty(filename) || context == null) {
            return;
        }
        String path = context.getFilesDir().getAbsolutePath() + File.separator + "peer_profiles";
        File peerProfilesFile = new File(path);
        if (!peerProfilesFile.exists())
            peerProfilesFile.mkdirs();
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(path+"/"+filename);
            outputStream.write(vcard.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
