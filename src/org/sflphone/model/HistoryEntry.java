package org.sflphone.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.sflphone.service.ServiceConstants;
import org.sflphone.utils.HistoryManager;

public class HistoryEntry {

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

    public static class HistoryCall {
        long call_start;
        long call_end;
        String number;
        String state;
        String recordPath;
        String formatted;

        public String getState() {
            return state;
        }

        public HistoryCall(HashMap<String, String> entry) {
            call_end = Long.parseLong(entry.get(ServiceConstants.history.TIMESTAMP_STOP_KEY));
            call_start = Long.parseLong(entry.get(ServiceConstants.history.TIMESTAMP_START_KEY));
            state = entry.get(ServiceConstants.history.STATE_KEY);
            recordPath = entry.get(ServiceConstants.history.RECORDING_PATH_KEY);
            number = entry.get(ServiceConstants.history.PEER_NUMBER_KEY);
            formatted = HistoryManager.timeToHistoryConst(call_start);
        }

        public String getDate(String format) {
            return formatted;
        }

        public String getDurationString() {
            long duration = call_end - call_start;
            if (duration < 60)
                return duration + "s";

            return duration / 60 + "min";

        }

        public long getDuration() {
            return call_end - call_start;

        }

        public String getRecordPath() {
            return recordPath;
        }

    }

    public CallContact getContact() {
        return contact;
    }

    public void setContact(CallContact contact) {
        this.contact = contact;
    }

    public void addHistoryCall(HistoryCall historyCall) {
        calls.put(historyCall.call_start, historyCall);
        if (historyCall.getState().contentEquals(ServiceConstants.history.MISSED_STRING)) {
            ++missed_sum;
        } else if (historyCall.getState().contentEquals(ServiceConstants.history.INCOMING_STRING)) {
            ++incoming_sum;
        } else {
            ++outgoing_sum;
        }
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
}
