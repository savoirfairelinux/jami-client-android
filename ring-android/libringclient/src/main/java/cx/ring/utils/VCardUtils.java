/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.property.FormattedName;
import ezvcard.property.Uid;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public final class VCardUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static final String MIME_RING_PROFILE_VCARD = "x-ring/ring.profile.vcard";
    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    private VCardUtils() {
        // Hidden default constructor
    }

    public static Tuple<String, byte[]> readData(VCard vcard) {
        String contactName = null;
        byte[] photo = null;
        if (vcard != null) {
            if (!vcard.getPhotos().isEmpty()) {
                try {
                    photo = vcard.getPhotos().get(0).getData();
                } catch (Exception e) {
                    Log.w(TAG, "Can't read photo from VCard", e);
                    photo = null;
                }
            }
            FormattedName fname = vcard.getFormattedName();
            if (fname != null) {
                if (!StringUtils.isEmpty(fname.getValue())) {
                    contactName = fname.getValue();
                }
            }
        }
        return new Tuple<>(contactName, photo);
    }

    /**
     * Parse the "elements" of the mime attributes to build a proper hashtable
     *
     * @param mime the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    public static HashMap<String, String> parseMimeAttributes(String mime) {
        String[] elements = mime.split(";");
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

    public static void savePeerProfileToDisk(VCard vcard, String filename, File filesDir) {
        String path = peerProfilePath(filesDir);
        saveToDisk(vcard, filename, path);
    }

    public static Single<VCard> saveLocalProfileToDisk(VCard vcard, String accountId, File filesDir) {
        return Single.fromCallable(() -> {
            String path = localProfilePath(filesDir);
            String filename = accountId + ".vcf";
            saveToDisk(vcard, filename, path);
            return vcard;
        });
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
            writer.getVObjectWriter().getFoldedLineWriter().setLineLength(null);
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

    public static Single<VCard> loadLocalProfileFromDisk(File filesDir, String accountId) {
        return Single.fromCallable(() -> {
            String path = localProfilePath(filesDir);
            try {
                if (!"".equals(path)) {
                    File vcardPath = new File(path + File.separator + accountId + ".vcf");
                    if (vcardPath.exists()) {
                        return loadFromDisk(vcardPath.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't load vcard");
            }
            return setupDefaultProfile(filesDir, accountId);
        });
    }

    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    private static VCard loadFromDisk(String path) {
        try {
            if (StringUtils.isEmpty(path)) {
                // Log.d(TAG, "Empty file or error with the context");
                return null;
            }

            File vcardPath = new File(path);
            if (!vcardPath.exists()) {
                // Log.d(TAG, "vcardPath not exist " + vcardPath);
                return null;
            }

            return Ezvcard.parse(vcardPath).first();
        } catch (Exception e) {
            Log.e(TAG, "Error while loading VCard from disk", e);
            return null;
        }
    }

    public static String vcardToString(VCard vcard) {
        StringWriter writer = new StringWriter();
        VCardWriter vcwriter = new VCardWriter(writer, VCardVersion.V2_1);
        vcwriter.getVObjectWriter().getFoldedLineWriter().setLineLength(null);
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
        vcard.setUid(new Uid(accountId));
        saveLocalProfileToDisk(vcard, accountId, filesDir)
                .subscribeOn(Schedulers.io())
                .subscribe(vc -> {}, e -> Log.e(TAG, "Error while saving vcard", e));
        return vcard;
    }
}
