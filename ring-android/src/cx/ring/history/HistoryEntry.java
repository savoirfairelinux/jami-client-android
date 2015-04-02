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

package cx.ring.history;

import android.os.Parcel;
import android.os.Parcelable;
import cx.ring.model.CallContact;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;

public class HistoryEntry implements Parcelable {

    private CallContact contact;
    private NavigableMap<Long, HistoryCall> calls;
    private String accountID;
    int missed_sum;
    int outgoing_sum;
    int incoming_sum;

    public HistoryEntry(String account, CallContact c) {
        contact = c;
        calls = new TreeMap<Long, HistoryCall>();
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
     *
     * @param historyCall The call to put in this HistoryEntry
     * @param linkedTo    The associated CallContact
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

        if (contact.isUnknown() && !linkedTo.isUnknown())
            setContact(linkedTo);
    }

    public String getNumber() {
        return calls.lastEntry().getValue().number;
    }

    public String getTotalDuration() {
        int duration = 0;
        ArrayList<HistoryCall> all_calls = new ArrayList<HistoryCall>(calls.values());
        for (HistoryCall all_call : all_calls) {
            duration += all_call.getDuration();
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

        ArrayList<HistoryCall> values = new ArrayList<HistoryCall>();
        in.readList(values, HistoryCall.class.getClassLoader());

        ArrayList<Long> keys = new ArrayList<Long>();
        in.readList(keys, Long.class.getClassLoader());

        calls = new TreeMap<Long, HistoryCall>();
        for (int i = 0; i < keys.size(); ++i) {
            calls.put(keys.get(i), values.get(i));
        }

        accountID = in.readString();
        missed_sum = in.readInt();
        outgoing_sum = in.readInt();
        incoming_sum = in.readInt();
    }

}
