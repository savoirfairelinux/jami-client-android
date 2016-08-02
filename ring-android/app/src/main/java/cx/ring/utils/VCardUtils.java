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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;

import cx.ring.service.StringMap;

public class VCardUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    public static final String VCARD_KEY_PHOTO = "PHOTO";

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
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(vcard.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the vcard file from the disk
     * @param filename the filename of the vcard
     * @param context the contact used to open a fileinputstream
     * @return the vcard content as a string
     */
    public static String loadFromDisk(String filename, Context context) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a hashtable from the vcard content.
     * @param content the vcard as a string
     * @return the hashtable vcard
     */
    public static Hashtable<String,String> toHashtable(String content) {
        if (TextUtils.isEmpty(content)) {
            return new Hashtable<>();
        }
        Hashtable<String,String> vcard = new Hashtable<>();
        String lines[] = content.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!TextUtils.isEmpty(trimmedLine)) {
                int separatorIndex = line.indexOf(":");
                String key = line.substring(0,separatorIndex);
                String value = "";
                if (separatorIndex + 1 < line.length()) {
                    value = line.substring(separatorIndex + 1, line.length());
                }
                Log.d(TAG, "Key: " + key + " / Value: " + value);
                vcard.put(key,value);
            }
        }
        return vcard;
    }

    /**
     * Converts the string version of the image to a Bitmap.
     * @param vcard the complete vcard as a string.
     * @return the Bitmap representing the profile picture.
     */
    @Nullable
    public static Bitmap getImage(String vcard) {
        if (!TextUtils.isEmpty(vcard)) {
            Hashtable<String,String> formattedCard = toHashtable(vcard);
            return getImage(formattedCard);
        }
        return null;
    }

    /**
     * Converts the string version of the image to a Bitmap.
     * @param vcard the vcard as a Hashtable
     * @return the Bitmap representing the profile picture.
     */
    public static Bitmap getImage(Hashtable<String,String> vcard) {
        Bitmap result = null;
        if (vcard != null) {
            Enumeration<String> keys = vcard.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.contains(VCARD_KEY_PHOTO)) {
                    String base64Image = vcard.get(key);
                    if (!TextUtils.isEmpty(base64Image)) {
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        result = BitmapFactory.decodeByteArray(decodedString,
                                0, decodedString.length);
                    }
                    break;
                }
            }
        }
        return result;
    }
}
