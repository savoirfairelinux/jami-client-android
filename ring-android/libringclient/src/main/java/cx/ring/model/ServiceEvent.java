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

public class ServiceEvent {

    public enum EventType {
        CALL_STATE_CHANGED,
        INCOMING_CALL,
        INCOMING_MESSAGE,
        CONFERENCE_CREATED,
        CONFERENCE_REMOVED,
        CONFERENCE_CHANGED,
        DATA_TRANSFER,
        RECORD_PLAYBACK_FILEPATH,
        RTCP_REPORT_RECEIVED,
        VOLUME_CHANGED,
        ACCOUNT_ADDED,
        ACCOUNTS_CHANGED,
        STUN_STATUS_FAILURE,
        REGISTRATION_STATE_CHANGED,
        INCOMING_ACCOUNT_MESSAGE,
        ACCOUNT_MESSAGE_STATUS_CHANGED,
        ERROR_ALERT,
        KNOWN_DEVICES_CHANGED,
        EXPORT_ON_RING_ENDED,
        NAME_REGISTRATION_ENDED,
        REGISTERED_NAME_FOUND,
        DECODING_STARTED,
        DECODING_STOPPED,
        GET_CAMERA_INFO,
        START_CAPTURE,
        STOP_CAPTURE,
        SET_PARAMETERS,
        CONTACTS_CHANGED,
        MIGRATION_ENDED,
        DEVICE_REVOCATION_ENDED,
        INCOMING_TRUST_REQUEST,
        CONTACT_ADDED,
        CONTACT_REMOVED,
        CONVERSATIONS_CHANGED,
        HISTORY_LOADED,
        HISTORY_MODIFIED,
        NEW_SERVER_SUBSCRIPTION_REQUEST,
        SERVER_ERROR,
        NEW_BUDDY_NOTIFICATION,
        SUBSCRIPTION_STATE_CHANGED,
        USERNAME_CHANGED,
        VIDEO_EVENT
    }

    public enum EventInput {
        ID,
        ACCOUNT_ID,
        CALL,
        CALL_ID,
        CONF_ID,
        MESSAGE_ID,
        CAMERA_ID,
        FROM,
        TO,
        DETAILS,
        DETAIL_CODE,
        DETAIL_STRING,
        STATE,
        MESSAGES,
        STATS,
        DEVICE,
        DEVICES,
        VALUE,
        ALERT,
        FORMATS,
        NAME,
        PATHS,
        CODE,
        PIN,
        ADDRESS,
        WIDTH,
        HEIGHT,
        IS_MIXER,
        SIZES,
        RATES,
        TIME,
        MESSAGE,
        CONFIRMED,
        BANNED,
        HISTORY_CALLS,
        HISTORY_TEXTS,
        REMOTE,
        ERROR,
        BUDDY_URI,
        LINE_STATE,
        VIDEO_START,
        VIDEO_STARTED,
        VIDEO_WIDTH,
        VIDEO_HEIGHT,
        TRANSFER_ID,
        TRANSFER_EVENT_CODE,
        VIDEO_CALL
    }

    private EventType mType;
    private Map<EventInput, Object> mInputs = new HashMap<>();

    public ServiceEvent(EventType type) {
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

    public <T> T getEventInput(EventInput input, Class<T> clazz, T defaultValue) {
        Object value = mInputs.get(input);
        if (value != null && value.getClass().isAssignableFrom(clazz)) {
            return (T) mInputs.get(input);
        }

        return defaultValue;
    }

    public String getString(EventInput input) {
        return getEventInput(input, String.class);
    }

    public int getInt(EventInput input) {
        return getEventInput(input, Integer.class);
    }

}
