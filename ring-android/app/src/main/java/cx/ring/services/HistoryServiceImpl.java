/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

package cx.ring.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.HashMap;

import javax.inject.Inject;

import cx.ring.daemon.StringMap;
import cx.ring.history.DatabaseHelper;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;
import cx.ring.utils.ProfileChunk;
import cx.ring.utils.VCardUtils;

/**
 * Implements the necessary Android related methods for the {@link HistoryService}
 */
public class HistoryServiceImpl extends HistoryService {

    static public final String VCARD_COMPLETED = "vcard-completed";
    private static final String TAG = HistoryServiceImpl.class.getSimpleName();

    @Inject
    protected Context mContext;

    private DatabaseHelper historyDBHelper = null;
    private ProfileChunk mProfileChunk;

    public HistoryServiceImpl() {
    }

    @Override
    protected ConnectionSource getConnectionSource() {
        return getHelper().getConnectionSource();
    }

    @Override
    protected Dao<HistoryCall, Integer> getCallHistoryDao() {
        try {
            return getHelper().getHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a CallHistoryDao");
            return null;
        }
    }

    @Override
    protected Dao<HistoryText, Integer> getTextHistoryDao() {
        try {
            return getHelper().getTextHistoryDao();
        } catch (SQLException e) {
            cx.ring.utils.Log.e(TAG, "Unable to get a TextHistoryDao");
            return null;
        }
    }

    /**
     * Init Helper for our DB
     */
    public void initHelper() {
        if (historyDBHelper == null) {
            historyDBHelper = OpenHelperManager.getHelper(mContext, DatabaseHelper.class);
        }
    }

    /**
     * Retrieve helper for our DB
     */
    private DatabaseHelper getHelper() {
        if (historyDBHelper == null) {
            historyDBHelper = OpenHelperManager.getHelper(mContext, DatabaseHelper.class);
        }
        return historyDBHelper;
    }

    @Override
    public void updateVCard() {
        this.mProfileChunk = null;
    }

    @Override
    public void saveVCard(String from, StringMap messages) {
        final String ringProfileVCardMime = "x-ring/ring.profile.vcard";

        if (messages != null) {

            String origin = messages.keys().toString().replace("[", "");
            origin = origin.replace("]", "");
            String[] elements = origin.split(";");

            HashMap<String, String> messageKeyValue = VCardUtils.parseMimeAttributes(elements);

            if (messageKeyValue != null && messageKeyValue.containsKey(VCardUtils.VCARD_KEY_MIME_TYPE) &&
                    messageKeyValue.get(VCardUtils.VCARD_KEY_MIME_TYPE).equals(ringProfileVCardMime)) {
                int part = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_PART));
                int nbPart = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_OF));
                if (null == mProfileChunk) {
                    mProfileChunk = new ProfileChunk(nbPart);
                }
                if (messages.keys() != null && messages.keys().size() > 0) {
                    String content = messages.getRaw(messages.keys().get(0)).toJavaString();
                    mProfileChunk.addPartAtIndex(content, part);
                }
                if (mProfileChunk.isProfileComplete()) {
                    Log.d(TAG, "Complete Profile: " + mProfileChunk.getCompleteProfile());
                    String splitFrom[] = from.split("@");
                    if (splitFrom.length == 2) {
                        String filename = splitFrom[0] + ".vcf";
                        VCardUtils.savePeerProfileToDisk(mProfileChunk.getCompleteProfile(),
                                filename,
                                mContext.getApplicationContext().getFilesDir());

                        Intent intent = new Intent(VCARD_COMPLETED);
                        intent.putExtra("filename", filename);
                        mContext.sendBroadcast(intent);
                    }
                }
            }
        }
    }

    public ProfileChunk getProfileChunk() {
        return mProfileChunk;
    }
}
