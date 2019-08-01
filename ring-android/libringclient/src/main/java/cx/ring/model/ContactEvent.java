package cx.ring.model;

public class ContactEvent extends Interaction {

    public TrustRequest request;
    public Event event;


    public ContactEvent(Interaction interaction) {
        mId = interaction.getId();
        mConversation = interaction.getConversation();
        mAuthor = interaction.getAuthor();
        mType = InteractionType.CONTACT.toString();
        mTimestamp = interaction.getTimestamp();
        mStatus = interaction.getStatus() == InteractionStatus.INVALID ? null : interaction.getStatus().toString();
        mIsRead = 1;
        mContact = interaction.getContact();
        updateEvent();
    }

    public ContactEvent() {
        mAuthor = null;
        event = Event.ADDED;
        mType = InteractionType.CONTACT.toString();
        mTimestamp = System.currentTimeMillis();
        mStatus = null;
        mIsRead = 1;
    }

    public ContactEvent(CallContact contact) {
        mContact = contact;
        mAuthor = contact.getPrimaryUri().getUri();
        mType = InteractionType.CONTACT.toString();
        event = Event.ADDED;
        mStatus = InteractionStatus.SUCCEEDED.toString();
        mTimestamp = contact.getAddedDate().getTime();
        mIsRead = 1;
    }

    public ContactEvent(CallContact contact, TrustRequest request) {
        this.request = request;
        mContact = contact;
        mAuthor = contact.getPrimaryUri().getUri();
        mTimestamp = request.getTimestamp();
        mType = InteractionType.CONTACT.toString();
        event = Event.INCOMING_REQUEST;
        mStatus = InteractionStatus.UNKNOWN.toString();
        mIsRead = 1;
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

    public void setRequest(TrustRequest request) {
        this.request = request;
    }

    private void updateEvent() {
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
