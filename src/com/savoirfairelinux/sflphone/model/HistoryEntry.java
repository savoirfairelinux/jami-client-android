package com.savoirfairelinux.sflphone.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

public class HistoryEntry {

    private CallContact contact;
    private NavigableMap<Long, HistoryCall> calls;
    private String accountID;

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public NavigableMap<Long, HistoryCall> getCalls() {
        return calls;
    }

    public HistoryEntry(String account, CallContact c, long call_start, long call_end, String number) {
        contact = c;
        calls = new TreeMap<Long, HistoryEntry.HistoryCall>();
        calls.put(call_start, new HistoryCall(call_start, call_end, number));
        accountID = account;
    }

    public static class HistoryCall {
        long call_start;
        long call_end;
        String number;

        public HistoryCall(long start, long end, String n) {
            call_start = start;
            call_end = end;
            number = n;
        }

        public String getDate(String format) {
            Calendar cal = Calendar.getInstance();
            TimeZone tz = cal.getTimeZone();
            SimpleDateFormat objFormatter = new SimpleDateFormat(format, Locale.CANADA);
            objFormatter.setTimeZone(tz);

            Calendar objCalendar = Calendar.getInstance(tz);
            objCalendar.setTimeInMillis(call_start * 1000);
            String result = objFormatter.format(objCalendar.getTime());
            objCalendar.clear();
            return result;
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

    }

    public CallContact getContact() {
        return contact;
    }

    public void setContact(CallContact contact) {
        this.contact = contact;
    }

    public void addHistoryCall(HistoryCall historyCall) {
        calls.put(historyCall.call_start, historyCall);

    }

    public String getNumber() {
        return calls.lastEntry().getValue().number;
    }

    public String getTotalDuration() {
        int duration = 0;
        ArrayList<HistoryCall> all_calls = new ArrayList<HistoryEntry.HistoryCall>(calls.values());
        for(int i = 0 ; i < all_calls.size() ; ++i){
            duration += all_calls.get(i).getDuration();
        }
        
        if (duration < 60)
            return duration + "s";

        return duration / 60 + "min";
    }
}
