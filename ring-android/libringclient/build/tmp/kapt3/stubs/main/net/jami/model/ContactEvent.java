package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001:\u0001\u0015B\u000f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0001\u00a2\u0006\u0002\u0010\u0003B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0004B\u000f\b\u0016\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007B\u0017\b\u0016\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u0011\u001a\u00020\f2\u0006\u0010\u0012\u001a\u00020\u0013H\u0002J\u000e\u0010\u0014\u001a\u00020\u00002\u0006\u0010\u000b\u001a\u00020\fR\u0012\u0010\u000b\u001a\u00020\f8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\b\u001a\u0004\u0018\u00010\tX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0016"}, d2 = {"Lnet/jami/model/ContactEvent;", "Lnet/jami/model/Interaction;", "interaction", "(Lnet/jami/model/Interaction;)V", "()V", "contact", "Lnet/jami/model/Contact;", "(Lnet/jami/model/Contact;)V", "request", "Lnet/jami/model/TrustRequest;", "(Lnet/jami/model/Contact;Lnet/jami/model/TrustRequest;)V", "event", "Lnet/jami/model/ContactEvent$Event;", "getRequest", "()Lnet/jami/model/TrustRequest;", "setRequest", "(Lnet/jami/model/TrustRequest;)V", "getEventFromStatus", "status", "Lnet/jami/model/Interaction$InteractionStatus;", "setEvent", "Event", "libringclient"})
public final class ContactEvent extends net.jami.model.Interaction {
    @org.jetbrains.annotations.Nullable()
    private net.jami.model.TrustRequest request;
    @org.jetbrains.annotations.NotNull()
    @kotlin.jvm.JvmField()
    public net.jami.model.ContactEvent.Event event;
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.TrustRequest getRequest() {
        return null;
    }
    
    public final void setRequest(@org.jetbrains.annotations.Nullable()
    net.jami.model.TrustRequest p0) {
    }
    
    public ContactEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        super();
    }
    
    public ContactEvent() {
        super();
    }
    
    public ContactEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact) {
        super();
    }
    
    public ContactEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
    net.jami.model.TrustRequest request) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.ContactEvent setEvent(@org.jetbrains.annotations.NotNull()
    net.jami.model.ContactEvent.Event event) {
        return null;
    }
    
    private final net.jami.model.ContactEvent.Event getEventFromStatus(net.jami.model.Interaction.InteractionStatus status) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\t\b\u0086\u0001\u0018\u0000 \t2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\tB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\n"}, d2 = {"Lnet/jami/model/ContactEvent$Event;", "", "(Ljava/lang/String;I)V", "UNKNOWN", "INCOMING_REQUEST", "INVITED", "ADDED", "REMOVED", "BANNED", "Companion", "libringclient"})
    public static enum Event {
        /*public static final*/ UNKNOWN /* = new UNKNOWN() */,
        /*public static final*/ INCOMING_REQUEST /* = new INCOMING_REQUEST() */,
        /*public static final*/ INVITED /* = new INVITED() */,
        /*public static final*/ ADDED /* = new ADDED() */,
        /*public static final*/ REMOVED /* = new REMOVED() */,
        /*public static final*/ BANNED /* = new BANNED() */;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.ContactEvent.Event.Companion Companion = null;
        
        Event() {
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/ContactEvent$Event$Companion;", "", "()V", "fromConversationAction", "Lnet/jami/model/ContactEvent$Event;", "action", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.ContactEvent.Event fromConversationAction(@org.jetbrains.annotations.NotNull()
            java.lang.String action) {
                return null;
            }
        }
    }
}