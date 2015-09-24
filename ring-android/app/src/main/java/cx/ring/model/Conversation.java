package cx.ring.model;

import android.database.ContentObservable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.history.HistoryText;
import cx.ring.model.account.Account;


public class Conversation extends ContentObservable implements Parcelable
{
    static final String TAG = Conversation.class.getSimpleName();

    public CallContact contact;
    //private HistoryEntry history;
    /** accountId -> histroy entries */
    final private Map<String, HistoryEntry> history = new HashMap<>();

    //private Conference current_call = null;
    public final ArrayList<Conference> current_calls;

    public String getLastNumberUsed(String accountID) {
        HistoryEntry he = history.get(accountID);
        if (he == null)
            return null;
        return he.getLastNumberUsed();
    }

    public class ConversationElement {
        public HistoryCall call = null;
        public TextMessage text = null;
        public ConversationElement(HistoryCall c) {
            call = c;
        }
        public ConversationElement(TextMessage t) {
            text = t;
        }
        public long getDate() {
            if (text != null)
                return text.getTimestamp();
            else if (call != null)
                return call.call_start;
            return 0;
        }
    }

    public Conversation(CallContact c) {
        contact = c;
        current_calls = new ArrayList<>();
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
        if (!current_calls.isEmpty()) {
            return new Date();
        }
        Date d = new Date(0);

        //for (Map.Entry<String, HistoryEntry> e : history.entrySet()) {
        for (HistoryEntry e : history.values()) {
            Date nd = e.getLastInteraction();
            if (d.compareTo(nd) < 0)
                d = nd;
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
    public void addTextMessage(TextMessage txt) {
        //HistoryText txt = new HistoryText(accountId, from, msg, contact, incoming);
        if (txt.getContact() == null)
            txt.setContact(contact);
        String accountId = txt.getAccount();
        if (history.containsKey(accountId))
            history.get(accountId).addTextMessage(txt);
        else {
            HistoryEntry e = new HistoryEntry(accountId, contact);
            e.addTextMessage(txt);
            history.put(accountId, e);
        }
    }

    public HistoryEntry getHistory(String account_id) {
        return history.get(account_id);
    }

    public ArrayList<ConversationElement> getHistory() {
        ArrayList<ConversationElement> all = new ArrayList<>();
        for (HistoryEntry e : history.values()) {
            for (HistoryCall c : e.getCalls().values())
                all.add(new ConversationElement(c));
            for (TextMessage t : e.getTextMessages().values())
                all.add(new ConversationElement(t));
        }
        Collections.sort(all, new Comparator<ConversationElement>() {
            @Override
            public int compare(ConversationElement lhs, ConversationElement rhs) {
                return (int)((lhs.getDate() - rhs.getDate())/1000l);
            }
        });
        return all;
    }

    public Set<String> getAccountsUsed() {
        return history.keySet();
    }

    public String getLastAccountUsed() {
        String last = null;
        Date d = new Date(0);
        for (Map.Entry<String, HistoryEntry> e : history.entrySet()) {
            Date nd = e.getValue().getLastInteraction();
            if (d.compareTo(nd) < 0) {
                d = nd;
                last = e.getKey();
            }
        }
        return last;
    }

    public Conference getCurrentCall() {
        if (current_calls.isEmpty())
            return null;
        return current_calls.get(0);
    }
    public void setCurrentCall(Conference c) {
        current_calls.add(c);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(contact, flags);
        //dest.writeParcelable(current_call, flags);
        dest.writeList(current_calls);
        dest.writeList(new ArrayList<>(history.values()));
        dest.writeList(new ArrayList<>(history.keySet()));
    }

    protected Conversation(Parcel in) {
        contact = in.readParcelable(CallContact.class.getClassLoader());
        //current_call = in.readParcelable(Conference.class.getClassLoader());
        current_calls = in.readArrayList(Conference.class.getClassLoader());

        ArrayList<HistoryEntry> values = new ArrayList<>();
        in.readList(values, HistoryEntry.class.getClassLoader());
        ArrayList<String> keys = new ArrayList<>();
        in.readList(keys, String.class.getClassLoader());
        for (int i = 0; i < keys.size(); ++i)
            history.put(keys.get(i), values.get(i));
    }

    public ArrayList<TextMessage> getTextMessages() {
        ArrayList<TextMessage> texts = new ArrayList<>();
        for (HistoryEntry h : history.values()) {
            texts.addAll(h.getTextMessages().values());
        }
        Collections.sort(texts, new Comparator<TextMessage>() {
            @Override
            public int compare(TextMessage lhs, TextMessage rhs) {
                return (int)((lhs.getTimestamp() - rhs.getTimestamp())/1000l);
            }
        });
        return texts;
    }

}
