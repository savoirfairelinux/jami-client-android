/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.sflphone.service.ServiceConstants;
import org.sflphone.utils.HistoryManager;

import android.os.Parcel;
import android.os.Parcelable;

public class HistoryEntry implements Parcelable {

    private CallContact contact;
    private NavigableMap<Long, HistoryCall> calls;
    private String accountID;
    int missed_sum;
    int outgoing_sum;
    int incoming_sum;

    public HistoryEntry(String account, CallContact c) {
        contact = c;
        calls = new TreeMap<Long, HistoryEntry.HistoryCall>();
        accountID = account;
        missed_sum = outgoing_sum = incoming_sum = 0;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public NavigableMap<Long, HistoryCall> getCalls() {
        return calls;
    }

    public CallContact getContact() {
        return contact;
    }

    public void setContact(CallContact contact) {
        this.contact = contact;
    }

    /**
     * Each call is associated with a contact.
     * When adding a call to an HIstoryEntry, this methods also verifies if we can update 
     * the contact (if contact is Unknown, replace it)
     * @param historyCall The call to put in this HistoryEntry 
     * @param linkedTo The associated CallContact
     */
    public void addHistoryCall(HistoryCall historyCall, CallContact linkedTo) {
        calls.put(historyCall.call_start, historyCall);
        if (historyCall.isIncoming()) {
            ++incoming_sum;
        } else {
            ++outgoing_sum;
        }
        if (historyCall.isMissed())
            missed_sum++;
        
        if(contact.isUnknown() && !linkedTo.isUnknown())
            setContact(linkedTo);
    }

    public String getNumber() {
        return calls.lastEntry().getValue().number;
    }

    public String getTotalDuration() {
        int duration = 0;
        ArrayList<HistoryCall> all_calls = new ArrayList<HistoryEntry.HistoryCall>(calls.values());
        for (int i = 0; i < all_calls.size(); ++i) {
            duration += all_calls.get(i).getDuration();
        }

        if (duration < 60)
            return duration + "s";

        return duration / 60 + "min";
    }

    public int getMissed_sum() {
        return missed_sum;
    }

    public int getOutgoing_sum() {
        return outgoing_sum;
    }

    public int getIncoming_sum() {
        return incoming_sum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeParcelable(contact, 0);

        dest.writeList(new ArrayList<HistoryCall>(calls.values()));
        dest.writeList(new ArrayList<Long>(calls.keySet()));

        dest.writeString(accountID);
        dest.writeInt(missed_sum);
        dest.writeInt(outgoing_sum);
        dest.writeInt(incoming_sum);

    }

    public static final Parcelable.Creator<HistoryEntry> CREATOR = new Parcelable.Creator<HistoryEntry>() {
        public HistoryEntry createFromParcel(Parcel in) {
            return new HistoryEntry(in);
        }

        public HistoryEntry[] newArray(int size) {
            return new HistoryEntry[size];
        }
    };

    private HistoryEntry(Parcel in) {
        contact = in.readParcelable(CallContact.class.getClassLoader());

        ArrayList<HistoryCall> values = new ArrayList<HistoryEntry.HistoryCall>();
        in.readList(values, HistoryCall.class.getClassLoader());

        ArrayList<Long> keys = new ArrayList<Long>();
        in.readList(keys, Long.class.getClassLoader());

        calls = new TreeMap<Long, HistoryEntry.HistoryCall>();
        for (int i = 0; i < keys.size(); ++i) {
            calls.put(keys.get(i), values.get(i));
        }

        accountID = in.readString();
        missed_sum = in.readInt();
        outgoing_sum = in.readInt();
        incoming_sum = in.readInt();
    }

    public static class HistoryCall implements Parcelable {
        long call_start;
        long call_end;
        String number;

        boolean missed;
        String direction;

        String recordPath;
        String timeFormatted;
        String displayName;

        public HistoryCall(HashMap<String, String> entry) {
            call_end = Long.parseLong(entry.get(ServiceConstants.history.TIMESTAMP_STOP_KEY));
            call_start = Long.parseLong(entry.get(ServiceConstants.history.TIMESTAMP_START_KEY));

            direction = entry.get(ServiceConstants.history.DIRECTION_KEY);
            missed = entry.get(ServiceConstants.history.MISSED_KEY).contentEquals("true");

            displayName = entry.get(ServiceConstants.history.DISPLAY_NAME_KEY);
            recordPath = entry.get(ServiceConstants.history.RECORDING_PATH_KEY);
            number = entry.get(ServiceConstants.history.PEER_NUMBER_KEY);
            timeFormatted = HistoryManager.timeToHistoryConst(call_start);
        }

        public String getDirection() {
            return direction;
        }

        public String getDate() {
            return timeFormatted;
        }

        public String getStartString(String format) {
            Timestamp stamp = new Timestamp(call_start * 1000); // in milliseconds
            Date date = new Date(stamp.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            String formattedDate = sdf.format(date);
            return formattedDate;

        }

        public String getDurationString() {

            long duration = call_end - call_start;
            if (duration < 60)
                return String.format(Locale.getDefault(), "%02d secs", duration);

            if (duration < 3600)
                return String.format(Locale.getDefault(), "%02d mins %02d secs", (duration % 3600) / 60, (duration % 60));

            return String.format(Locale.getDefault(), "%d h %02d mins %02d secs", duration / 3600, (duration % 3600) / 60, (duration % 60));

        }

        public long getDuration() {
            return call_end - call_start;

        }

        public String getRecordPath() {
            return recordPath;
        }

        public String getNumber() {
            return number;
        }

        public String getDisplayName() {
            return displayName.substring(0, displayName.indexOf('@'));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(call_start);
            dest.writeLong(call_end);
            dest.writeString(number);
            dest.writeByte((byte) (missed ? 1 : 0));
            dest.writeString(direction);
            dest.writeString(recordPath);
            dest.writeString(timeFormatted);
            dest.writeString(displayName);
        }

        public static final Parcelable.Creator<HistoryCall> CREATOR = new Parcelable.Creator<HistoryCall>() {
            public HistoryCall createFromParcel(Parcel in) {
                return new HistoryCall(in);
            }

            public HistoryCall[] newArray(int size) {
                return new HistoryCall[size];
            }
        };

        private HistoryCall(Parcel in) {
            call_start = in.readLong();
            call_end = in.readLong();
            number = in.readString();
            missed = in.readByte() == 1 ? true : false;
            direction = in.readString();
            recordPath = in.readString();
            timeFormatted = in.readString();
            displayName = in.readString();
        }

        public boolean hasRecord() {
            return recordPath.length() > 0;
        }

        public boolean isIncoming() {
            return direction.contentEquals(ServiceConstants.history.INCOMING_STRING);
        }

        public boolean isMissed() {
            return missed;
        }

    }

}
