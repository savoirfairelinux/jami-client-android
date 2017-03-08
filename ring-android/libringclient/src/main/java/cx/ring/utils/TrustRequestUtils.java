/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ezvcard.Ezvcard;
import ezvcard.VCard;

public class TrustRequestUtils {

    public static final String TAG = TrustRequestUtils.class.getSimpleName();

    public static ArrayList<String> loadFromDisk(String accountId, File filesDir) {
        if (accountId == null || filesDir == null) {
            return null;
        }

        String path = trustRequestPath(filesDir) + File.separator + accountId + ".txt";
        File file = new File(path);
        if (!file.exists()) {
            Log.d(TAG, "no pending trust requests for this account");
            return null;
        }

        ArrayList<String> requests = new ArrayList<>();
        try {
            InputStream stream = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(reader);
            String line;
            while ((line = buffer.readLine()) != null) {
                requests.add(line);
            }
            buffer.close();
        } catch (IOException e) {
            Log.e(TAG, "error while loading contact requests from disk", e);
            return null;
        }

        return requests;
    }

    //TODO : modify this function when the payload protocol will be make
    public static Tuple<VCard, String> parsePayload(String payload) {
        String message = "";
        VCard vCard = Ezvcard.parse(payload).first();
        return new Tuple<>(vCard, message);
    }

    //TODO : modify this function when the payload protocol will be make
    public static String compressPayload(VCard vCard, String message) {
        return VCardUtils.vcardToString(vCard);
    }

    public static void saveToDisk(String accountId, String contactId, File filesDir) {
        if (accountId == null || contactId == null || filesDir == null) {
            return;
        }

        String path = trustRequestPath(filesDir);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        try {
            FileWriter writer = new FileWriter(path + File.separator + accountId + ".txt", true);
            writer.write(contactId + "\n");
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the contact request to the disk", e);
        }
    }

    public static void saveRequestsToDisk(String accountId, ArrayList<String> requests, File filesDir) {
        if (accountId == null || requests == null || filesDir == null) {
            return;
        }

        String path = trustRequestPath(filesDir);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        try {
            FileWriter writer = new FileWriter(path + File.separator + accountId + ".txt");
            for (String contactId : requests) {
                writer.write(contactId + "\n");
            }
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the contact request to the disk", e);
        }
    }

    public static void removeRequestToDisk(String accountId, String contactId, File filesDir) {
        ArrayList<String> requests = loadFromDisk(accountId, filesDir);
        if (!requests.contains(contactId)) {
            return;
        }

        requests.remove(contactId);
        saveRequestsToDisk(accountId, requests, filesDir);
    }

    private static String trustRequestPath(File filesDir) {
        return filesDir.getAbsolutePath() + File.separator + "pending_contact_request";
    }
}
