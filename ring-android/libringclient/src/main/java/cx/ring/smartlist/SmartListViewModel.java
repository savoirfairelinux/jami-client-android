package cx.ring.smartlist;

import cx.ring.model.HistoryEntry;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListViewModel {

    private String contactName;
    private HistoryEntry lastInteraction;
    private String photoUri;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;

    public SmartListViewModel() {
    }

    public SmartListViewModel(String contactName, HistoryEntry lastInteraction, String photoUri,
                              long lastInteractionTime, boolean hasUnreadTextMessage, boolean hasOngoingCall) {
        this.contactName = contactName;
        this.lastInteraction = lastInteraction;
        this.photoUri = photoUri;
        this.lastInteractionTime = lastInteractionTime;
        this.hasUnreadTextMessage = hasUnreadTextMessage;
        this.hasOngoingCall = hasOngoingCall;
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
}
