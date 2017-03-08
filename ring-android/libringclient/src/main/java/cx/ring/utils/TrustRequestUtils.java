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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cx.ring.model.TrustRequest;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class TrustRequestUtils {
    public static final String TAG = TrustRequestUtils.class.getSimpleName();

    public static TrustRequest loadFromDisk(String accountId, String ringId, File filesDir) {
        Log.d(TAG, "loadFromDisk: " + accountId + ", " + ringId);
        if (accountId == null || accountId.isEmpty()
                || ringId == null || ringId.isEmpty()
                || filesDir == null) {
            return null;
        }

        String path = trustRequestPath(filesDir) + File.separator + accountId + ringId + ".tr";
        File file = new File(path);
        if (!file.exists()) {
            Log.d(TAG, "Pending contact request not exist " + file);
            return null;
        }

        String payload = "";
        try {
            InputStream stream = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(reader);
            String line;
            while ((line = buffer.readLine()) != null) {
                payload += line + "\n";
            }
            buffer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while loading contact request from disk", e);
            return null;
        }

        Tuple<VCard, String> tuple = parsePayload(payload);
        String message = tuple.second;
        VCard vcard = tuple.first;

        TrustRequest trustRequest = new TrustRequest(accountId, ringId);
        trustRequest.setMessage(message);
        trustRequest.setVCard(vcard);
        return trustRequest;
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

    public static void saveTrustRequestToDisk(TrustRequest trustRequest, File filesDir) {
        Log.d(TAG, "saveTrustRequestToDisk: " + trustRequest.getAccountId() + ", " + trustRequest.getContactId());
        if (trustRequest == null || filesDir == null) {
            return;
        }

        String trustrequestPath = trustRequestPath(filesDir);
        File trustRequestFile = new File(trustrequestPath);
        if (!trustRequestFile.exists()) {
            trustRequestFile.mkdirs();
        }

        String accountId = trustRequest.getAccountId();
        String ringId = trustRequest.getContactId();
        String path = trustrequestPath + File.separator + accountId + ringId + ".tr";

        String message = trustRequest.getMessage();
        VCard vCard = trustRequest.getVCard();

        String payload = compressPayload(vCard, message);

        FileOutputStream outputStream;
        try {
            File outputFile = new File(path);
            outputStream = new FileOutputStream(outputFile);
            outputStream.write(payload.getBytes());
            outputStream.close();
            Log.d(TAG, "pending contact request in saveToDisk " + payload);
        } catch (Exception e) {
            Log.e(TAG, "Error while saving pending contact request to disk", e);
        }
    }

    private static String trustRequestPath(File filesDir) {
        return filesDir.getAbsolutePath() + File.separator + "pending_contact_request";
    }
}
