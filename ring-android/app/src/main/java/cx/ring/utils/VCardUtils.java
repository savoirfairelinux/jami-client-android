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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import cx.ring.R;
import cx.ring.service.StringMap;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.FormattedName;

public class VCardUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    private VCardUtils() {
        // Hidden default constructor
    }

    /**
     * Parse the "stringmap" of the mime attributes to build a proper hashtable
     *
     * @param stringMap the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    public static HashMap<String, String> parseMimeAttributes(StringMap stringMap) {
        if (stringMap == null || stringMap.empty()) {
            return null;
        }
        HashMap<String, String> messageKeyValue = new HashMap<>();
        String origin = stringMap.keys().toString().replace("[", "");
        origin = origin.replace("]", "");
        String elements[] = origin.split(";");
        if (elements.length < 2) {
            return messageKeyValue;
        }
        messageKeyValue.put(VCARD_KEY_MIME_TYPE, elements[0]);
        String[] pairs = elements[1].split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            messageKeyValue.put(kv[0].trim(), kv[1]);
        }
        return messageKeyValue;
    }

    public static void savePeerProfileToDisk(String vcard, String filename, Context context) {
        String path = peerProfilePath(context);
        saveToDisk(vcard, filename, path, context);
    }

    public static void saveLocalProfileToDisk(String vcard, Context context) {
        String path = localProfilePath(context);
        File vcardPath = new File(path);
        String filename;
        if (vcardPath.exists() && vcardPath.listFiles().length > 0) {
            filename = vcardPath.listFiles()[0].getName();
        } else {
            filename = String.valueOf(System.currentTimeMillis()) + ".vcf";
        }
        saveToDisk(vcard, filename, path, context);
    }

    /**
     * Saves a vcard string to an internal new vcf file.
     *
     * @param vcard    the string to save
     * @param filename the filename of the vcf
     * @param path     the path of the vcf
     * @param context  the context used to open streams.
     */
    private static void saveToDisk(String vcard, String filename, String path, Context context) {
        if (TextUtils.isEmpty(vcard) || TextUtils.isEmpty(filename) || context == null) {
            return;
        }
        File peerProfilesFile = new File(path);
        if (!peerProfilesFile.exists()) {
            peerProfilesFile.mkdirs();
        }
        FileOutputStream outputStream;
        try {
            File outputFile = new File(path + File.separator + filename);
            outputStream = new FileOutputStream(outputFile);
            outputStream.write(vcard.getBytes());
            outputStream.close();
            Log.d(TAG, "vcard in saveToDisk " + vcard);
        } catch (Exception e) {
            Log.e(TAG, "Error while saving VCard to disk", e);
        }
    }

    public static VCard loadPeerProfileFromDisk(@Nullable String filename, @Nullable Context context) {
        String path = peerProfilePath(context) + File.separator + filename;
        return loadFromDisk(path, context);
    }

    public static VCard loadLocalProfileFromDisk(@Nullable Context context) {
        VCard vcard = null;
        String path = localProfilePath(context);
        File vcardPath = new File(path);
        if (vcardPath.exists()) {
            File[] listvCard = vcardPath.listFiles();
            if (listvCard.length > 0) {
                vcard = loadFromDisk(listvCard[0].toString(), context);
            }
        }

        if (vcard == null) {
            Log.d(TAG, "load default profile");
            vcard = setupDefaultProfile(context);
        }

        return vcard;
    }

    /**
     * Loads the vcard file from the disk
     *
     * @param path    the filename of the vcard
     * @param context the contact used to open a fileinputstream
     * @return the VCard or null
     */
    @Nullable
    private static VCard loadFromDisk(@Nullable String path, @Nullable Context context) {
        try {
            if (TextUtils.isEmpty(path) || context == null) {
                Log.d(TAG, "Empty file or error with the context");
                return null;
            }

            File vcardPath = new File(path);
            if (!vcardPath.exists()) {
                Log.d(TAG, "vcardPath not exist " + vcardPath);
                return null;
            }
            VCard vcard = Ezvcard.parse(vcardPath).first();

            Log.d(TAG, "vcard in loadFromDisk " + Ezvcard.write(vcard).go());
            return vcard;
        } catch (IOException e) {
            Log.e(TAG, "Error while loading VCard from disk", e);
            return null;
        }
    }

    private static String peerProfilePath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "peer_profiles";
    }

    private static String localProfilePath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "profiles";
    }

    private static VCard setupDefaultProfile(Context context) {
        VCard vcard = new VCard();
        vcard.setFormattedName(new FormattedName(context.getString(R.string.unknown)));
        saveLocalProfileToDisk(Ezvcard.write(vcard).version(VCardVersion.V2_1).go(), context);
        return vcard;
    }
}
