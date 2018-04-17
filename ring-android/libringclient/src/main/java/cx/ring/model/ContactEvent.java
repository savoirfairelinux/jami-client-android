package cx.ring.model;

public class ContactEvent extends ConversationElement {
    public enum Event {
        INCOMING_REQUEST,
        ADDED
    };
    public CallContact contact;
    public TrustRequest request;
    public Event event;

    ContactEvent(CallContact contact) {
        this.contact = contact;
        event = Event.ADDED;
    }

    ContactEvent(CallContact contact, TrustRequest request) {
        this.contact = contact;
        this.request = request;
        event = Event.INCOMING_REQUEST;
    }

    @Override
    public CEType getType() {
        return CEType.CONTACT;
    }

    @Override
    public long getDate() {
        return (event == Event.ADDED) ? contact.getAddedDate().getTime() : request.getTimestamp();
    }

    @Override
    public Uri getContactNumber() {
        return contact.getPrimaryUri();
    }

    @Override
    public boolean isRead() {
        return false;
    }

    @Override
    public long getId() {
        return contact.getAddedDate().getTime();
    }
}
