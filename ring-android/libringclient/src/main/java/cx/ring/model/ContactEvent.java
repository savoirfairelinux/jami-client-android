package cx.ring.model;

public class ContactEvent extends Interaction {

    public CallContact contact;
    public TrustRequest request;
    public Event event;


    public ContactEvent(Interaction interaction) {
        mId = interaction.getId();
        mConversation = interaction.getConversation();
        mAuthor = interaction.getAuthor();
        mType = InteractionType.CONTACT.toString();
        mTimestamp = interaction.getTimestamp();
        mStatus = interaction.getStatus() == null ? null : interaction.getStatus().toString();
        isRead = 1;
        updateEvent();
    }

    public ContactEvent() {
        mAuthor = null;
        event = Event.ADDED;
        mType = InteractionType.CONTACT.toString();
        mTimestamp = System.currentTimeMillis();
        mStatus = null;
        isRead = 1;
    }

    public ContactEvent(CallContact contact) {
        this.contact = contact;
        mAuthor = contact.getPrimaryUri().getUri();
        mType = InteractionType.CONTACT.toString();
        event = Event.ADDED;
        mStatus = InteractionStatus.SUCCEEDED.toString();
        mTimestamp = contact.getAddedDate().getTime();
        isRead = 1;
    }

    public ContactEvent(CallContact contact, TrustRequest request) {
        this.contact = contact;
        this.request = request;
        mAuthor = contact.getPrimaryUri().getUri();
        mTimestamp = request.getTimestamp();
        mType = InteractionType.CONTACT.toString();
        event = Event.INCOMING_REQUEST;
        mStatus = InteractionStatus.UNKNOWN.toString();
        isRead = 1;
    }

    public enum Event {
        INCOMING_REQUEST,
        ADDED,
        REMOVED,
        BANNED
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setContact(CallContact contact) {
        this.contact = contact;
    }


    public void setRequest(TrustRequest request) {
        this.request = request;
    }

    // todo banned and removed?
    public void updateEvent() {
        if (mStatus == null)
            this.event = Event.ADDED;
        else if (mStatus.equals(InteractionStatus.SUCCEEDED.toString()))
            this.event = Event.ADDED;
        else if (mStatus.equals(InteractionStatus.UNKNOWN.toString()))
            this.event = Event.INCOMING_REQUEST;

    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }


}
