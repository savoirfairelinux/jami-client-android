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

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.model.TextMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class HistoryEntry implements Parcelable {

    private CallContact contact;
    private final NavigableMap<Long, HistoryCall> calls = new TreeMap<>();
    private final NavigableMap<Long, TextMessage> text_messages = new TreeMap<>();
    private String accountID;
    int missed_sum;
    int outgoing_sum;
    int incoming_sum;

    public HistoryEntry(String account, CallContact c) {
        contact = c;
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
    public NavigableMap<Long, TextMessage> getTextMessages() {
        return text_messages;
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

    public void addTextMessage(TextMessage text) {
        text_messages.put(text.getTimestamp(), text);
        if (contact.isUnknown() && !text.getContact().isUnknown())
            setContact(text.getContact());
    }
    /*public void addTextMessage(HistoryText text) {
        TextMessage txt = new TextMessage(text);
        txt.setContact(contact);
        text_messages.put(txt.getTimestamp(), txt);
    }*/

    public String getNumber() {
        return calls.lastEntry().getValue().number;
    }

    public String getTotalDuration() {
        int duration = 0;
        ArrayList<HistoryCall> all_calls = new ArrayList<>(calls.values());
        for (HistoryCall all_call : all_calls) {
            duration += all_call.getDuration();
        }

        if (duration < 60)
            return duration + "s";

        return duration / 60 + "min";
    }

    public Date getLastCallDate() {
        /*Date d = new Date(0);
        for (Map.Entry<Long, HistoryCall> c : getCalls().entrySet()) {
            Date nd = c.getValue().getStartDate();
            if (d.compareTo(nd) < 0)
                d = nd;
        }
        return d;*/
        return new Date(calls.isEmpty() ? 0 : calls.lastEntry().getKey());
    }
    public Date getLastTextDate() {
        return new Date(text_messages.isEmpty() ? 0 : text_messages.lastEntry().getKey());
    }
    public Date getLastInteraction() {
        return new Date(Math.max(calls.isEmpty() ? 0 : calls.lastEntry().getKey(), text_messages.isEmpty() ? 0 : text_messages.lastEntry().getKey()));
    }
    public Pair<Date, String> getLastInteractionSumary(Resources res) {
        long last_txt = text_messages.isEmpty() ? 0 : text_messages.lastEntry().getKey();
        long last_call = calls.isEmpty() ? 0 : calls.lastEntry().getKey();
        if (last_txt > 0) {
            TextMessage msg = text_messages.lastEntry().getValue();
            return new Pair<>(new Date(last_txt), (msg.isIncoming() ? "" : res.getText(R.string.you_txt_prefix)) + " " + msg.getMessage());
        }
        if (last_call > 0) {
            return new Pair<>(new Date(last_call), calls.lastEntry().getValue().getDescription(res));
        }
        return null;
    }

    public HistoryCall getLastOutgoingCall() {
        for (HistoryCall c : calls.descendingMap().values())
            if (!c.isIncoming())
                return c;
        return null;
    }
    public TextMessage getLastOutgoingText() {
        for (TextMessage c : text_messages.descendingMap().values())
            if (c.isOutgoing())
                return c;
        return null;
    }
    public HistoryCall getLastIncomingCall() {
        for (HistoryCall c : calls.descendingMap().values())
            if (c.isIncoming())
                return c;
        return null;
    }
    public TextMessage getLastIncomingText() {
        for (TextMessage c : text_messages.descendingMap().values())
            if (c.isIncoming())
                return c;
        return null;
    }

    public String getLastNumberUsed() {
        HistoryCall call = getLastOutgoingCall();
        TextMessage text = getLastOutgoingText();
        if (call == null && text == null) {
            call = getLastIncomingCall();
            text = getLastIncomingText();
            if (call == null && text == null)
                return null;
        }
        if (call == null)
            return text.getNumber();
        if (text == null)
            return call.getNumber();
        if (call.call_start < text.getTimestamp())
            return text.getNumber();
        else
            return call.getNumber();
    }

    public int getMissedSum() {
        return missed_sum;
    }

    public int getOutgoingSum() {
        return outgoing_sum;
    }

    public int getIncomingSum() {
        return incoming_sum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeParcelable(contact, 0);

        dest.writeList(new ArrayList<>(calls.values()));
        dest.writeList(new ArrayList<>(calls.keySet()));
        dest.writeList(new ArrayList<>(text_messages.values()));
        dest.writeList(new ArrayList<>(text_messages.keySet()));

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

        ArrayList<HistoryCall> values = new ArrayList<>();
        in.readList(values, HistoryCall.class.getClassLoader());
        ArrayList<Long> keys = new ArrayList<>();
        in.readList(keys, Long.class.getClassLoader());
        for (int i = 0; i < keys.size(); ++i) {
            calls.put(keys.get(i), values.get(i));
        }
        ArrayList<TextMessage> tvalues = new ArrayList<>();
        in.readList(tvalues, TextMessage.class.getClassLoader());
        ArrayList<Long> tkeys = new ArrayList<>();
        in.readList(tkeys, Long.class.getClassLoader());
        for (int i = 0; i < keys.size(); ++i) {
            text_messages.put(tkeys.get(i), tvalues.get(i));
        }

        accountID = in.readString();
        missed_sum = in.readInt();
        outgoing_sum = in.readInt();
        incoming_sum = in.readInt();
    }
}
