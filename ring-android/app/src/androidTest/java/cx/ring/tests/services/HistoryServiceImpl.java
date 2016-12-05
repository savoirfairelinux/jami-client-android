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

package cx.ring.tests.services;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryText;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.services.HistoryService;

public class HistoryServiceImpl extends HistoryService {
    private static final String TAG = HistoryServiceImpl.class.getSimpleName();

    public HistoryServiceImpl() {
    }

    @Override
    protected ConnectionSource getConnectionSource() {
        return null;
    }

    @Override
    protected Dao<HistoryCall, Integer> getCallHistoryDao() {
        return null;
    }

    @Override
    protected Dao<HistoryText, Integer> getTextHistoryDao() {
        return null;
    }

    public boolean insertNewEntry(Conference toInsert) {
        return true;
    }

    public boolean insertNewTextMessage(HistoryText txt) {
        return true;
    }

    public boolean insertNewTextMessage(TextMessage txt) {
        HistoryText htxt = new HistoryText(txt);
        if (!insertNewTextMessage(htxt))
            return false;
        txt.setID(htxt.id);
        return true;
    }

    public boolean updateTextMessage(HistoryText txt) {
        return true;
    }

    /*
    * Necessary when user hang up a call in a Conference
    * The call creates an HistoryCall, but the conference still goes on
    */
    public boolean insertNewEntry(SipCall toInsert) {
        return true;
    }

    public List<HistoryCall> getAll() throws SQLException {
        return new ArrayList<>();
    }

    public List<HistoryText> getAllTextMessages() throws SQLException {
        return new ArrayList<>();
    }

    /**
     * Removes all the text messages and call histories from the database.
     *
     * @param conversation The conversation containing the elements to delete.
     */
    public void clearHistoryForConversation(Conversation conversation) {

    }

    @Override
    public void clearHistory() {
    }
}
