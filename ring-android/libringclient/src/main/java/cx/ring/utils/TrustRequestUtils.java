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

public class TrustRequestUtils {
    public static final String TAG = VCardUtils.class.getSimpleName();

    public static TrustRequest loadTrustRequestFromDisk(String accountId, String ringId, File filesDir){
        if(accountId == null || accountId.isEmpty()
                || ringId == null || ringId.isEmpty()
                || filesDir == null) {
            return null;
        }

        String pathVCard = trustRequestPath(filesDir) + File.separator + accountId + ringId + ".vcf";
        String pathMessage = trustRequestPath(filesDir) + File.separator + accountId + ringId + ".tr";

        TrustRequest trustRequest = new TrustRequest(accountId, ringId);
        trustRequest.setMessage(loadFromDisk(pathMessage));
        trustRequest.setVCard(VCardUtils.loadFromDisk(pathVCard));
        return trustRequest;
    }

    private static String loadFromDisk(String path) {
        try {
            if (path == null || path.isEmpty()) {
                Log.d(TAG, "Empty file or error with the context");
                return null;
            }

            File file = new File(path);
            if (!file.exists()) {
                Log.d(TAG, "Pending contact request not exist " + file);
                return null;
            }

            String message = "";
            InputStream stream = new FileInputStream(path);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(reader);
            String line;
            while ((line = buffer.readLine()) != null){
                message += line + "\n";
            }
            buffer.close();
            return message;
        } catch (IOException e) {
            Log.e(TAG, "Error while loading contact request from disk", e);
            return null;
        }
    }

    public static void saveTrustRequestToDisk(TrustRequest trustRequest, File filesDir) {
        if (trustRequest == null || filesDir == null) {
            return;
        }

        String path = trustRequestPath(filesDir);
        File trustRequestFile = new File(path);
        if (!trustRequestFile.exists()) {
            trustRequestFile.mkdirs();
        }

        String accountId = trustRequest.getAccountId();
        String ringId = trustRequest.getContactId();
        String pathVCard = accountId + ringId + ".vcf";
        String pathMessage = path + File.separator + accountId + ringId + ".tr";

        String message = trustRequest.getMessage();

        VCardUtils.saveToDisk(trustRequest.getVCard(), pathVCard,path);
        saveToDisk(message, pathMessage);
    }

    private static void saveToDisk(String information, String path){
        if(path == null || path.isEmpty() || information == null) {
            return;
        }

        FileOutputStream outputStream;
        try {
            File outputFile = new File(path);
            outputStream = new FileOutputStream(outputFile);
            outputStream.write(information.getBytes());
            outputStream.close();
            Log.d(TAG, "pending contact request in saveToDisk " + information);
        } catch (Exception e) {
            Log.e(TAG, "Error while saving pending contact request to disk", e);
        }
    }

    private static String trustRequestPath(File filesDir) {
        return filesDir.getAbsolutePath() + File.separator + "pending_contact_request";
    }
}
