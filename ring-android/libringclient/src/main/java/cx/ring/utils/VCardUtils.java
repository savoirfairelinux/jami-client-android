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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.property.FormattedName;
import ezvcard.property.Uid;

public class VCardUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    private VCardUtils() {
        // Hidden default constructor
    }

    /**
     * Parse the "elements" of the mime attributes to build a proper hashtable
     *
     * @param elements the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    public static HashMap<String, String> parseMimeAttributes(String[] elements) {

        if (elements == null) {
            return null;
        }

        HashMap<String, String> messageKeyValue = new HashMap<>();

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

    public static void savePeerProfileToDisk(String vcard, String filename, File filesDir) {
        String path = peerProfilePath(filesDir);
        saveToDisk(vcard, filename, path);
    }

    public static void savePeerProfileToDisk(VCard vcard, String filename, File filesDir) {
        String path = peerProfilePath(filesDir);
        saveToDisk(vcard, filename, path);
    }

    public static void saveLocalProfileToDisk(String vcard, String accountId, File filesDir) {
        String path = localProfilePath(filesDir);
        File vcardPath = new File(path);
        String filename = accountId + ".vcf";

        saveToDisk(vcard, filename, path);
    }

    public static void saveLocalProfileToDisk(VCard vcard, String accountId, File filesDir) {
        String path = localProfilePath(filesDir);
        File vcardPath = new File(path);
        String filename = accountId + ".vcf";

        saveToDisk(vcard, filename, path);
    }

    /**
     * Saves a vcard string to an internal new vcf file.
     *
     * @param vcard    the string to save
     * @param filename the filename of the vcf
     * @param path     the path of the vcf
     */
    private static void saveToDisk(String vcard, String filename, String path) {
        if (vcard == null || vcard.equals("") || filename == null || filename.equals("")) {
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

    /**
     * Saves a vcard string to an internal new vcf file.
     *
     * @param vcard    the VCard to save
     * @param filename the filename of the vcf
     * @param path     the path of the vcf
     */
    private static void saveToDisk(VCard vcard, String filename, String path) {
        if (vcard == null || filename == null || filename.equals("")) {
            return;
        }
        File peerProfilesFile = new File(path);
        if (!peerProfilesFile.exists()) {
            peerProfilesFile.mkdirs();
        }

        File file = new File(path + File.separator + filename);
        try {
            VCardWriter writer = new VCardWriter(file, VCardVersion.V2_1);
            writer.getRawWriter().getFoldedLineWriter().setLineLength(null);
            writer.getRawWriter().getFoldedLineWriter().setNewline("\n");
            writer.write(vcard);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "Error while saving VCard to disk", e);
        }
    }

    public static VCard loadPeerProfileFromDisk(File filesDir, String filename) {
        String path = peerProfilePath(filesDir) + File.separator + filename;
        return loadFromDisk(path);
    }

    public static VCard loadLocalProfileFromDisk(File filesDir, String accountId) {
        VCard vcard = null;
        String path = localProfilePath(filesDir);
        if (!"".equals(path)) {
            File vcardPath = new File(path + File.separator + accountId + ".vcf");
            if (vcardPath.exists()) {
                vcard = loadFromDisk(vcardPath.getAbsolutePath());
            }
        }

        if (vcard == null) {
            Log.d(TAG, "load default profile");
            vcard = setupDefaultProfile(filesDir, accountId);
        }

        return vcard;
    }

    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    private static VCard loadFromDisk(String path) {
        try {
            if (path == null || path.isEmpty()) {
                Log.d(TAG, "Empty file or error with the context");
                return null;
            }

            File vcardPath = new File(path);
            if (!vcardPath.exists()) {
                Log.d(TAG, "vcardPath not exist " + vcardPath);
                return null;
            }

            return Ezvcard.parse(vcardPath).first();
        } catch (IOException e) {
            Log.e(TAG, "Error while loading VCard from disk", e);
            return null;
        }
    }

    public static String vcardToString(VCard vcard) {
        if(vcard == null){
            return "";
        }
        
        StringWriter writer = new StringWriter();
        VCardWriter vcwriter = new VCardWriter(writer, VCardVersion.V2_1);
        vcwriter.getRawWriter().getFoldedLineWriter().setLineLength(null);
        vcwriter.getRawWriter().getFoldedLineWriter().setNewline("\n");
        String stringVCard;
        try {
            vcwriter.write(vcard);
            stringVCard = writer.toString();
            vcwriter.close();
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "Error while converting VCard to String", e);
            stringVCard = null;
        }

        return stringVCard;
    }

    private static String peerProfilePath(File filesDir) {
        return filesDir.getAbsolutePath() + File.separator + "peer_profiles";
    }

    private static String localProfilePath(File filesDir) {
        return filesDir.getAbsolutePath() + File.separator + "profiles";
    }

    private static VCard setupDefaultProfile(File filesDir, String accountId) {
        VCard vcard = new VCard();
        vcard.setUid(new Uid(String.valueOf(System.currentTimeMillis())));
        saveLocalProfileToDisk(vcard, accountId, filesDir);
        return vcard;
    }
}
