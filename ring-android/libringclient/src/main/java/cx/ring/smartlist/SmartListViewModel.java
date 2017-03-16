package cx.ring.smartlist;

import java.util.Map;

import cx.ring.model.Conversation;
import cx.ring.model.HistoryEntry;
import cx.ring.services.ContactService;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListViewModel {

    private int uuid;
    private String contactName;
    private HistoryEntry lastInteraction;
    private String photoUri;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;

    public SmartListViewModel(Conversation conversation, Map<String, String> contactDetails) {
        this.uuid = conversation.getUuid();
        this.contactName = contactDetails.get(ContactService.CONTACT_NAME_KEY);
        this.photoUri = contactDetails.get(ContactService.CONTACT_PHOTO_KEY);

        for (HistoryEntry historyEntry : conversation.getHistory().values()) {
            long lastTextTimestamp = historyEntry.getTextMessages().isEmpty() ? 0 : historyEntry.getTextMessages().lastEntry().getKey();
            long lastCallTimestamp = historyEntry.getCalls().isEmpty() ? 0 : historyEntry.getCalls().lastEntry().getKey();
            if (lastTextTimestamp > 0 && lastTextTimestamp > lastCallTimestamp) {
                this.lastInteraction = historyEntry;
                break;
            }
            if (lastCallTimestamp > 0) {
                this.lastInteraction = historyEntry;
                break;
            }
        }

        this.lastInteractionTime = conversation.getLastInteraction().getTime();
        this.hasUnreadTextMessage = conversation.hasUnreadTextMessages();
        this.hasOngoingCall = conversation.hasCurrentCall();
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public HistoryEntry getLastInteraction() {
        return lastInteraction;
    }

    public void setLastInteraction(HistoryEntry lastInteraction) {
        this.lastInteraction = lastInteraction;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public void setLastInteractionTime(long lastInteractionTime) {
        this.lastInteractionTime = lastInteractionTime;
    }

    public boolean hasUnreadTextMessage() {
        return hasUnreadTextMessage;
    }

    public void setHasUnreadTextMessage(boolean hasUnreadTextMessage) {
        this.hasUnreadTextMessage = hasUnreadTextMessage;
    }

    public boolean hasOngoingCall() {
        return hasOngoingCall;
    }

    public void setHasOngoingCall(boolean hasOngoingCall) {
        this.hasOngoingCall = hasOngoingCall;
    }

    public int getUuid() {
        return uuid;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
    }
}
