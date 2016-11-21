/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.model;

import java.util.HashMap;
import java.util.Map;

public class DaemonEvent {

    public enum EventType {
        CALL_STATE_CHANGED,
        INCOMING_CALL,
        INCOMING_MESSAGE,
        CONFERENCE_CREATED,
        CONFERENCE_REMOVED,
        CONFERENCE_CHANGED,
        RECORD_PLAYBACK_FILEPATH,
        RTCP_REPORT_RECEIVED
    }

    public enum EventInput {
        ACCOUNT_ID,
        CALL_ID,
        CONF_ID,
        FROM,
        DETAIL_CODE,
        STATE,
        MESSAGES,
        STATS
    }

    private EventType mType;
    private Map<EventInput, Object> mInputs = new HashMap<>();

    public DaemonEvent(EventType type) {
        mType = type;
    }

    public EventType getEventType() {
        return mType;
    }

    public void addEventInput(EventInput input, Object value) {
        mInputs.put(input, value);
    }

    public <T> T getEventInput(EventInput input, Class<T> clazz) {
        Object value = mInputs.get(input);
        if (value != null && value.getClass().isAssignableFrom(clazz)) {
            return (T) mInputs.get(input);
        }

        return null;
    }
}
