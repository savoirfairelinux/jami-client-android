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
        mStatus = interaction.getStatus().toString();
        mIsRead = 1;
        mContact = interaction.getContact();
        event = getEventFromStatus(interaction.getStatus());
    }

    public ContactEvent() {
        mAuthor = null;
        event = Event.ADDED;
        mType = InteractionType.CONTACT.toString();
        mTimestamp = System.currentTimeMillis();
        mStatus = InteractionStatus.SUCCESS.toString();
        mIsRead = 1;
    }

    public ContactEvent(CallContact contact) {
        mContact = contact;
        mAuthor = contact.getPrimaryUri().getUri();
        mType = InteractionType.CONTACT.toString();
        event = Event.ADDED;
        mStatus = InteractionStatus.SUCCESS.toString();
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
        UNKNOWN,
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

    private Event getEventFromStatus(InteractionStatus status) {
        // success for added contacts
        if (status == InteractionStatus.SUCCESS)
            return Event.ADDED;
        // storage is unknown status for trust requests
        else if (status == InteractionStatus.UNKNOWN)
            return Event.INCOMING_REQUEST;

        return Event.UNKNOWN;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }


}
