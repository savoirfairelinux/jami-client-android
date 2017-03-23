package cx.ring.smartlist;

import java.util.Date;
import java.util.Map;

import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.TextMessage;
import cx.ring.services.ContactService;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListViewModel {

    public static final int TYPE_INCOMING_MESSAGE = 0;
    public static final int TYPE_OUTGOING_MESSAGE = 1;
    public static final int TYPE_INCOMING_CALL = 2;
    public static final int TYPE_OUTGOING_CALL = 3;

    private int uuid;
    private String contactName;
    private String lastInteraction = "";
    private String photoUri;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private int lastEntryType;

    public SmartListViewModel(Conversation conversation, Map<String, String> contactDetails) {
        this.uuid = conversation.getUuid();
        this.contactName = contactDetails.get(ContactService.CONTACT_NAME_KEY);
        this.photoUri = contactDetails.get(ContactService.CONTACT_PHOTO_KEY);
        this.lastInteractionTime = new Date().getTime() - conversation.getLastInteraction().getTime();
        this.hasUnreadTextMessage = conversation.hasUnreadTextMessages();
        this.hasOngoingCall = conversation.hasCurrentCall();

        for (HistoryEntry historyEntry : conversation.getHistory().values()) {
            long lastTextTimestamp = historyEntry.getTextMessages().isEmpty() ? 0 : historyEntry.getTextMessages().lastEntry().getKey();
            long lastCallTimestamp = historyEntry.getCalls().isEmpty() ? 0 : historyEntry.getCalls().lastEntry().getKey();
            if (lastTextTimestamp > 0 && lastTextTimestamp > lastCallTimestamp) {
                TextMessage msg = historyEntry.getTextMessages().lastEntry().getValue();
                String msgString = msg.getMessage();
                if (msgString != null && !msgString.isEmpty() && msgString.contains("\n")) {
                    int lastIndexOfChar = msgString.lastIndexOf("\n");
                    if (lastIndexOfChar + 1 < msgString.length()) {
                        msgString = msgString.substring(msgString.lastIndexOf("\n") + 1);
                    }
                }
                this.lastEntryType = msg.isIncoming() ? TYPE_INCOMING_MESSAGE : TYPE_OUTGOING_MESSAGE;
                this.lastInteraction = msgString;
                break;
            }
            if (lastCallTimestamp > 0) {
                HistoryCall lastCall = historyEntry.getCalls().lastEntry().getValue();
                this.lastEntryType = lastCall.isIncoming() ? TYPE_INCOMING_CALL : TYPE_OUTGOING_CALL;
                this.lastInteraction = lastCall.getDurationString();
                break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SmartListViewModel) {
            SmartListViewModel slvm = (SmartListViewModel) o;
            return this.uuid == slvm.getUuid()
                    && this.contactName.equals(slvm.getContactName())
                    && this.lastInteraction.equals(slvm.lastInteraction)
                    && this.photoUri.equals(slvm.getPhotoUri())
                    && this.lastInteractionTime == slvm.getLastInteractionTime()
                    && this.hasUnreadTextMessage == slvm.hasUnreadTextMessage()
                    && this.hasOngoingCall == slvm.hasOngoingCall()
                    && this.lastEntryType == slvm.getLastEntryType();
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        byte result = 42;
        return 37 * result + this.uuid;
    }

    public String getContactName() {
        return contactName;
    }

    public String getLastInteraction() {
        return lastInteraction;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public boolean hasUnreadTextMessage() {
        return hasUnreadTextMessage;
    }

    public boolean hasOngoingCall() {
        return hasOngoingCall;
    }

    public int getUuid() {
        return uuid;
    }

    public int getLastEntryType() {
        return lastEntryType;
    }
}
