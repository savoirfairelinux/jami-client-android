/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package net.jami.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public final class VCardUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static final String MIME_PROFILE_VCARD = "x-ring/ring.profile.vcard";
    public static final String VCARD_KEY_MIME_TYPE = "mimeType";
    public static final String VCARD_KEY_PART = "part";
    public static final String VCARD_KEY_OF = "of";

    public static final String LOCAL_USER_VCARD_NAME = "profile.vcf";
    private static final long VCARD_MAX_SIZE = 1024 * 1024 * 8;

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

    public static VCard writeData(String uri, String displayName, byte[] picture) {
        VCard vcard = new VCard();
        vcard.setFormattedName(new FormattedName(displayName));
        vcard.setUid(new Uid(uri));
        if (picture != null) {
            vcard.addPhoto(new Photo(picture, ImageType.JPEG));
        }
        vcard.removeProperties(RawProperty.class);
        return vcard;
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

    public static void savePeerProfileToDisk(VCard vcard, String accountId, String filename, File filesDir) {
        saveToDisk(vcard, filename, peerProfilePath(filesDir, accountId));
    }

    public static Single<VCard> saveLocalProfileToDisk(VCard vcard, String accountId, File filesDir) {
        return Single.fromCallable(() -> {
            saveToDisk(vcard, LOCAL_USER_VCARD_NAME, localProfilePath(filesDir, accountId));
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
    private static void saveToDisk(VCard vcard, String filename, File path) {
        if (vcard == null || StringUtils.isEmpty(filename)) {
            return;
        }
        if (!path.exists()) {
            path.mkdirs();
        }

        File file = new File(path, filename);
        try (VCardWriter writer = new VCardWriter(file, VCardVersion.V2_1)) {
            writer.getVObjectWriter().getFoldedLineWriter().setLineLength(null);
            writer.write(vcard);
        } catch (Exception e) {
            Log.e(TAG, "Error while saving VCard to disk", e);
        }
    }

    public static VCard loadPeerProfileFromDisk(File filesDir, String filename, String accountId) throws IOException {
        File profileFolder = peerProfilePath(filesDir, accountId);
        return loadFromDisk(new File(profileFolder, filename));
    }

    public static Single<VCard> loadLocalProfileFromDisk(File filesDir, String accountId) {
        return Single.fromCallable(() -> {
            String path = localProfilePath(filesDir, accountId).getAbsolutePath();
            return loadFromDisk(new File(path, LOCAL_USER_VCARD_NAME));
        });
    }

    public static Single<VCard> loadLocalProfileFromDiskWithDefault(File filesDir, String accountId) {
        return loadLocalProfileFromDisk(filesDir, accountId)
                .onErrorReturn(e -> setupDefaultProfile(filesDir, accountId));
    }

    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    private static VCard loadFromDisk(File path) throws IOException {
        if (path == null || !path.exists()) {
            Log.d(TAG, "vcardPath not exist " + path);
            return null;
        }
        if (path.length() > VCARD_MAX_SIZE) {
            Log.w(TAG, "vcardPath too big: " + path.length() / 1024 + " kB");
            return null;
        }
        return Ezvcard.parse(path).first();
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

    public static boolean isEmpty(VCard vCard) {
        FormattedName name = vCard.getFormattedName();
        return (name == null || name.getValue().isEmpty()) && vCard.getPhotos().isEmpty();
    }

    private static File peerProfilePath(File filesDir, String accountId) {
        File accountDir = new File(filesDir, accountId);
        File profileDir = new File(accountDir, "profiles");
        profileDir.mkdirs();
        return profileDir;
    }

    private static File localProfilePath(File filesDir, String accountId) {
        File accountDir = new File(filesDir, accountId);
        accountDir.mkdir();
        return accountDir;
    }

    private static VCard setupDefaultProfile(File filesDir, String accountId) {
        VCard vcard = new VCard();
        vcard.setUid(new Uid(accountId));
        saveLocalProfileToDisk(vcard, accountId, filesDir)
                .subscribeOn(Schedulers.io())
                .subscribe(vc -> {}, e -> Log.e(TAG, "Error while saving vcard", e));
        return vcard;
    }

    public static Single<VCard> peerProfileReceived(File filesDir, String accountId, String peerId, File vcard) {
        return Single.fromCallable(() -> {
            String filename = peerId + ".vcf";
            File peerProfilePath = VCardUtils.peerProfilePath(filesDir, accountId);
            File file = new File(peerProfilePath, filename);
            FileUtils.moveFile(vcard, file);
            return VCardUtils.loadFromDisk(file);
        }).subscribeOn(Schedulers.io());
    }
}
