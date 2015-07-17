package cx.ring.model;

import android.database.ContentObservable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.model.account.Account;


public class Conversation extends ContentObservable implements Parcelable
{
    static final String TAG = Conversation.class.getSimpleName();

    final CallContact contact;
    //private HistoryEntry history;
    /** accountId -> histroy entries */
    final private Map<String, HistoryEntry> history = new HashMap<>();
    private Conference current_call = null;

    Conversation(CallContact c) {
        contact = c;
    }

    public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
        @Override
        public Conversation createFromParcel(Parcel in) {
            return new Conversation(in);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };

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

    public HistoryEntry getHistory(String account_id) {
        return history.get(account_id);
    }

    public Set<String> getAccountsUsed() {
        return history.keySet();
    }

    public Conference getCurrentCall() {
        return current_call;
    }
    public void setCurrentCall(Conference c) {
        current_call = c;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(contact, flags);
        dest.writeParcelable(current_call, flags);
        dest.writeList(new ArrayList<>(history.values()));
        dest.writeList(new ArrayList<>(history.keySet()));
    }

    protected Conversation(Parcel in) {
        contact = in.readParcelable(CallContact.class.getClassLoader());
        current_call = in.readParcelable(Conference.class.getClassLoader());

        ArrayList<HistoryEntry> values = new ArrayList<>();
        in.readList(values, HistoryEntry.class.getClassLoader());
        ArrayList<String> keys = new ArrayList<>();
        in.readList(keys, String.class.getClassLoader());
        for (int i = 0; i < keys.size(); ++i)
            history.put(keys.get(i), values.get(i));
    }

}
