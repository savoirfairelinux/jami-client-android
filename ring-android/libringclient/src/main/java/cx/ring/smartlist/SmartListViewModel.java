package cx.ring.smartlist;

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
    private String lastInteraction;
    private String photoUri;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private int lastEntryType;

    private boolean hasChanged;

    public SmartListViewModel(Conversation conversation, Map<String, String> contactDetails) {
        this.uuid = conversation.getUuid();
        this.contactName = contactDetails.get(ContactService.CONTACT_NAME_KEY);
        this.photoUri = contactDetails.get(ContactService.CONTACT_PHOTO_KEY);
        this.lastInteractionTime = conversation.getLastInteraction().getTime();
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

        this.hasChanged = false;
    }

    public void update(Conversation conversation, Map<String, String> contactDetails) {
        String contactName = contactDetails.get(ContactService.CONTACT_NAME_KEY);
        if (contactName != null && !this.contactName.equals(contactName)) {
            this.contactName = contactName;
            this.hasChanged = true;
        }

        String photoURI = contactDetails.get(ContactService.CONTACT_PHOTO_KEY);
        if (photoURI != null && !this.photoUri.equals(photoURI)) {
            this.photoUri = photoURI;
            this.hasChanged = true;
        }
        if (this.lastInteractionTime != conversation.getLastInteraction().getTime()) {
            this.lastInteractionTime = conversation.getLastInteraction().getTime();
            this.hasChanged = true;
        }
        if (this.hasUnreadTextMessage != conversation.hasUnreadTextMessages()) {
            this.hasUnreadTextMessage = conversation.hasUnreadTextMessages();
            this.hasChanged = true;
        }
        if (this.hasOngoingCall != conversation.hasCurrentCall()) {
            this.hasOngoingCall = conversation.hasCurrentCall();
            this.hasChanged = true;
        }

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
                if (this.lastEntryType != (msg.isIncoming() ? TYPE_INCOMING_MESSAGE : TYPE_OUTGOING_MESSAGE)) {
                    this.lastEntryType = msg.isIncoming() ? TYPE_INCOMING_MESSAGE : TYPE_OUTGOING_MESSAGE;
                    this.hasChanged = true;
                }
                if (!this.lastInteraction.equals(msgString)) {
                    this.lastInteraction = msgString;
                    this.hasChanged = true;
                }
                break;
            }
            if (lastCallTimestamp > 0) {
                HistoryCall lastCall = historyEntry.getCalls().lastEntry().getValue();
                if (this.lastEntryType != (lastCall.isIncoming() ? TYPE_INCOMING_CALL : TYPE_OUTGOING_CALL)) {
                    this.lastEntryType = lastCall.isIncoming() ? TYPE_INCOMING_CALL : TYPE_OUTGOING_CALL;
                    this.hasChanged = true;
                }
                if (!this.lastInteraction.equals(lastCall.getDurationString())) {
                    this.lastInteraction = lastCall.getDurationString();
                    this.hasChanged = true;
                }
                break;
            }
        }
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

    public boolean hasChanged() {
        return hasChanged;
    }

    public void setHasChanged(boolean hasChanged) {
        this.hasChanged = hasChanged;
    }

    public int getLastEntryType() {
        return lastEntryType;
    }
}
