package net.jami.smartlist;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u0000 >2\u00020\u0001:\u0002>?B!\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\bB+\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\nB%\b\u0016\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\u0002\u0010\u0011B\u0017\b\u0016\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\u0002\u0010\u0012B\u000f\b\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0014\u00a2\u0006\u0002\u0010\u0015J\u0013\u00109\u001a\u00020\u00102\b\u0010:\u001a\u0004\u0018\u00010\u0001H\u0096\u0002J\b\u0010;\u001a\u0004\u0018\u00010\u0005J\u0006\u0010\u001c\u001a\u00020\u0010J\u0006\u0010\u001d\u001a\u00020\u0010J\u000e\u0010<\u001a\u00020=2\u0006\u0010\u001c\u001a\u00020\u0010J\u0006\u00102\u001a\u00020\u0010R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u0018\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0017R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u000e\u0010\u001c\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u001e\u001a\u00020\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u001a\u0010!\u001a\u00020\u0010X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b!\u0010\"\"\u0004\b#\u0010$R\u001e\u0010&\u001a\u00020\u00102\u0006\u0010%\u001a\u00020\u0010@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\"R\u0011\u0010\'\u001a\u00020\u00108F\u00a2\u0006\u0006\u001a\u0004\b\'\u0010\"R\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0011\u0010*\u001a\u00020+8F\u00a2\u0006\u0006\u001a\u0004\b,\u0010-R.\u0010/\u001a\n\u0012\u0004\u0012\u00020\u0010\u0018\u00010.2\u000e\u0010%\u001a\n\u0012\u0004\u0012\u00020\u0010\u0018\u00010.@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u00101R\u000e\u00102\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u00103\u001a\u000204\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u00106R\u0013\u00107\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u0010\u0017\u00a8\u0006@"}, d2 = {"Lnet/jami/smartlist/SmartListViewModel;", "", "accountId", "", "contact", "Lnet/jami/model/Contact;", "lastEvent", "Lnet/jami/model/Interaction;", "(Ljava/lang/String;Lnet/jami/model/Contact;Lnet/jami/model/Interaction;)V", "id", "(Ljava/lang/String;Lnet/jami/model/Contact;Ljava/lang/String;Lnet/jami/model/Interaction;)V", "conversation", "Lnet/jami/model/Conversation;", "contacts", "", "presence", "", "(Lnet/jami/model/Conversation;Ljava/util/List;Z)V", "(Lnet/jami/model/Conversation;Z)V", "title", "Lnet/jami/smartlist/SmartListViewModel$Title;", "(Lnet/jami/smartlist/SmartListViewModel$Title;)V", "getAccountId", "()Ljava/lang/String;", "contactName", "getContactName", "getContacts", "()Ljava/util/List;", "hasOngoingCall", "hasUnreadTextMessage", "headerTitle", "getHeaderTitle", "()Lnet/jami/smartlist/SmartListViewModel$Title;", "isChecked", "()Z", "setChecked", "(Z)V", "<set-?>", "isOnline", "isSwarm", "getLastEvent", "()Lnet/jami/model/Interaction;", "lastInteractionTime", "", "getLastInteractionTime", "()J", "Lio/reactivex/rxjava3/core/Observable;", "selected", "getSelected", "()Lio/reactivex/rxjava3/core/Observable;", "showPresence", "uri", "Lnet/jami/model/Uri;", "getUri", "()Lnet/jami/model/Uri;", "uuid", "getUuid", "equals", "other", "getContact", "setHasOngoingCall", "", "Companion", "Title", "libringclient"})
public final class SmartListViewModel {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String accountId = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Uri uri = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<net.jami.model.Contact> contacts = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String uuid = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String contactName = null;
    private final boolean hasUnreadTextMessage = false;
    private boolean hasOngoingCall = false;
    private final boolean showPresence = false;
    private boolean isOnline = false;
    private boolean isChecked = false;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Observable<java.lang.Boolean> selected;
    @org.jetbrains.annotations.Nullable()
    private final net.jami.model.Interaction lastEvent = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.smartlist.SmartListViewModel.Title headerTitle = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.smartlist.SmartListViewModel.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel> TITLE_CONVERSATIONS = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel> TITLE_PUBLIC_DIR = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.reactivex.rxjava3.core.Single<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> EMPTY_LIST = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.smartlist.SmartListViewModel>> EMPTY_RESULTS = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAccountId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Uri getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.model.Contact> getContacts() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUuid() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getContactName() {
        return null;
    }
    
    public final boolean isOnline() {
        return false;
    }
    
    public final boolean isChecked() {
        return false;
    }
    
    public final void setChecked(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Boolean> getSelected() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Interaction getLastEvent() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.smartlist.SmartListViewModel.Title getHeaderTitle() {
        return null;
    }
    
    public SmartListViewModel(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, @org.jetbrains.annotations.Nullable()
    net.jami.model.Interaction lastEvent) {
        super();
    }
    
    public SmartListViewModel(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, @org.jetbrains.annotations.Nullable()
    java.lang.String id, @org.jetbrains.annotations.Nullable()
    net.jami.model.Interaction lastEvent) {
        super();
    }
    
    public SmartListViewModel(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Contact> contacts, boolean presence) {
        super();
    }
    
    public SmartListViewModel(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, boolean presence) {
        super();
    }
    
    private SmartListViewModel(net.jami.smartlist.SmartListViewModel.Title title) {
        super();
    }
    
    public final boolean isSwarm() {
        return false;
    }
    
    /**
     * Used to get contact for one to one or legacy conversations
     */
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact getContact() {
        return null;
    }
    
    public final long getLastInteractionTime() {
        return 0L;
    }
    
    public final boolean hasUnreadTextMessage() {
        return false;
    }
    
    public final boolean hasOngoingCall() {
        return false;
    }
    
    public final boolean showPresence() {
        return false;
    }
    
    public final void setHasOngoingCall(boolean hasOngoingCall) {
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/smartlist/SmartListViewModel$Title;", "", "(Ljava/lang/String;I)V", "None", "Conversations", "PublicDirectory", "libringclient"})
    public static enum Title {
        /*public static final*/ None /* = new None() */,
        /*public static final*/ Conversations /* = new Conversations() */,
        /*public static final*/ PublicDirectory /* = new PublicDirectory() */;
        
        Title() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\b\u0007\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R#\u0010\u0003\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u00060\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u001d\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000b0\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\r\u00a8\u0006\u0012"}, d2 = {"Lnet/jami/smartlist/SmartListViewModel$Companion;", "", "()V", "EMPTY_LIST", "Lio/reactivex/rxjava3/core/Single;", "", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/smartlist/SmartListViewModel;", "getEMPTY_LIST", "()Lio/reactivex/rxjava3/core/Single;", "EMPTY_RESULTS", "", "getEMPTY_RESULTS", "()Lio/reactivex/rxjava3/core/Observable;", "TITLE_CONVERSATIONS", "getTITLE_CONVERSATIONS", "TITLE_PUBLIC_DIR", "getTITLE_PUBLIC_DIR", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel> getTITLE_CONVERSATIONS() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel> getTITLE_PUBLIC_DIR() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.reactivex.rxjava3.core.Single<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getEMPTY_LIST() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.smartlist.SmartListViewModel>> getEMPTY_RESULTS() {
            return null;
        }
    }
}