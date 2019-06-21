/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.services;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Tuple;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import io.reactivex.Single;

public class VCardServiceImpl extends VCardService {

    private Context mContext;

    public VCardServiceImpl(Context context) {
        this.mContext = context;
    }

    public static Single<Tuple<String, Object>> loadProfile(@NonNull Account account) {
        Tuple<String, Object> ret = account.getLoadedProfile();
        if (ret == null) {
            return Single.fromCallable(() -> readData(account.getProfile()))
                    .map(profile -> {
                        account.setLoadedProfile(profile);
                        return profile;
                    });
        }
        return Single.just(ret);
    }

    @Override
    public Single<VCard> loadSmallVCard(String accountId, int maxSize) {
        return VCardUtils
                .loadLocalProfileFromDisk(mContext.getFilesDir(), accountId)
                .map(vcard -> {
                    if (!vcard.getPhotos().isEmpty()) {
                        // Reduce photo size to fit in one DHT packet
                        Bitmap photo = BitmapUtils.bytesToBitmap(vcard.getPhotos().get(0).getData(), maxSize);
                        //photo = BitmapUtils.reduceBitmap(photo, maxSize);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 88, stream);
                        vcard.removeProperties(Photo.class);
                        vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.JPEG));
                        vcard.removeProperties(RawProperty.class);
                    }
                    return vcard;
                });
    }

    @Override
    public Single<Tuple<String, Object>> loadVCardProfile(VCard vcard) {
        return Single.fromCallable(() -> readData(vcard));
    }

    public static Tuple<String, Object> readData(VCard vcard) {
        return readData(VCardUtils.readData(vcard));
    }

    public static Tuple<String, Object> readData(Tuple<String, byte[]> profile) {
        return new Tuple<>(profile.first, BitmapUtils.bytesToBitmap(profile.second));
    }

    /**
     * Migrates the user's contcts to their individual account folders under the subfolder profiles
     * @param contacts a hash map of the user's contacts
     * @param accountId the directory where the profile is stored
     */
    @Override
    public void migrateContact(Map<String, CallContact> contacts, String accountId) {
        File fileDir = mContext.getFilesDir();
        File legacyProfileFolder = new File(fileDir, "peer_profiles");
        if (!legacyProfileFolder.exists())
            return;

        File[] profiles = legacyProfileFolder.listFiles();

        if (profiles == null)
            return;

        File accountDir = new File(fileDir, accountId);
        File profilesDir = new File(accountDir, "profiles");
        profilesDir.mkdirs();


        for (File profile : profiles) {
            String filename = profile.getName();
            String contactUri = filename.lastIndexOf(".") > 0 ? filename.substring(0, filename.lastIndexOf(".")) : filename;
            if (contacts.containsKey(contactUri)) {
                File destination = new File(profilesDir, filename);
                FileUtils.copyFile(profile, destination);
            }
        }
    }

    /**
     * Migrates the user's vcards and renames them to profile.vcf
     *
     * @param accountIds the list of accounts to migrate
     */
    @Override
    public void migrateProfiles(List<String> accountIds) {
        File fileDir = mContext.getFilesDir();
        File profileDir = new File(fileDir, "profiles");

        if (!profileDir.exists())
            return;

        File[] profiles = profileDir.listFiles();

        if (profiles == null)
            return;

        profileLoop:
        for (File profile : profiles) {
            for (String account : accountIds) {
                if (profile.getName().equals(account + ".vcf")) {
                    File accountDir = new File(fileDir, account);
                    File newProfile = new File(accountDir, VCardUtils.LOCAL_USER_VCARD_NAME);
                    FileUtils.moveFile(profile, newProfile);
                    break profileLoop;
                }
            }
        }

        // necessary to delete profiles leftover by deleted accounts
        for (File profile : profiles) {
            profile.delete();
        }


        profileDir.delete();
    }

    /**
     * Deletes the legacy peer_profiles folder
     */
    @Override
    public void deleteLegacyProfiles() {
        File fileDir = mContext.getFilesDir();
        File legacyProfileFolder = new File(fileDir, "peer_profiles");
        File[] profiles = legacyProfileFolder.listFiles();
        if (profiles == null)
            return;

        for (File file : profiles) {
            file.delete();
        }

        legacyProfileFolder.delete();
    }
}
