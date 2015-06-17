package cx.ring.model;

import android.database.ContentObservable;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;


public class Conversation extends ContentObservable
{
    static final String TAG = Conversation.class.getSimpleName();

    final CallContact contact;
    //private HistoryEntry history;
    final private Map<String, HistoryEntry> history = new HashMap<>();
    private Conference current_call = null;

    Conversation(CallContact c) {
        contact = c;
    }

    public CallContact getContact() {
        return contact;
    }

    public Date getLastInteraction() {
        if (current_call != null) {
            return new Date();
        }
        Date d = new Date(0);
        for (Map.Entry<String, HistoryEntry> e : history.entrySet()) {
            for (Map.Entry<Long, HistoryCall> c : e.getValue().getCalls().entrySet()) {
                Date nd = c.getValue().getStartDate();
                if (d.compareTo(nd) < 0)
                    d = nd;
            }
        }
        return d;
    }

    public void addHistoryCall(HistoryCall c) {
        String accountId = c.getAccountID();
        if (history.containsKey(accountId))
            history.get(accountId).addHistoryCall(c, contact);
        else {
            HistoryEntry e = new HistoryEntry(accountId, contact);
            e.addHistoryCall(c, contact);
            history.put(accountId, e);
        }
    }

    public Conference getCurrentCall() {
        return current_call;
    }
    public void setCurrentCall(Conference c) {
        current_call = c;
    }
}
