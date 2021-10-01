package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0084\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\n\u0018\u0000 z2\u00020\u0001:\u0002z{B\u0019\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006B!\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\u0006\u0010\t\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\nJ(\u0010^\u001a\u00020_2\u0006\u0010`\u001a\u00020\b2\u0006\u0010a\u001a\u00020b2\b\u0010c\u001a\u0004\u0018\u00010\b2\u0006\u0010d\u001a\u00020eJ(\u0010^\u001a\u00020_2\u0006\u0010`\u001a\u00020\u00032\u0006\u0010a\u001a\u00020b2\b\u0010c\u001a\u0004\u0018\u00010\b2\u0006\u0010d\u001a\u00020eJ \u0010f\u001a\u00020_2\u0006\u0010`\u001a\u00020\u00032\u0006\u0010a\u001a\u00020b2\b\u0010c\u001a\u0004\u0018\u00010\bJ\u000e\u0010g\u001a\u00020\u00052\u0006\u0010h\u001a\u00020\bJ\u0010\u0010g\u001a\u00020\u00052\b\u0010h\u001a\u0004\u0018\u00010\u0003J\u0006\u0010i\u001a\u00020\u0005J\u000e\u0010j\u001a\u00020\u00052\u0006\u0010k\u001a\u00020\bJ\u000e\u0010l\u001a\u00020_2\u0006\u0010\u0011\u001a\u00020\u0003J\u0016\u0010m\u001a\u00020_2\u000e\u0010n\u001a\n\u0012\u0004\u0012\u00020\u0005\u0018\u000104J\u001a\u0010o\u001a\u00020_2\b\u0010p\u001a\u0004\u0018\u00010\b2\b\u0010>\u001a\u0004\u0018\u00010\u0001J\u0010\u0010o\u001a\u00020_2\b\u0010q\u001a\u0004\u0018\u00010rJ\u0006\u0010s\u001a\u00020_J(\u0010t\u001a\u00020_2\u0006\u0010\u001f\u001a\u00020 2\b\u0010u\u001a\u0004\u0018\u00010\b2\u0006\u0010\u001a\u001a\u00020\b2\u0006\u0010v\u001a\u00020 J\u000e\u0010w\u001a\u00020_2\u0006\u0010\u001f\u001a\u00020 J\u0010\u0010x\u001a\u00020\u00052\b\u0010p\u001a\u0004\u0018\u00010\bJ\b\u0010y\u001a\u00020\bH\u0016R\u001c\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u00128F\u00a2\u0006\u0006\u001a\u0004\b\u0013\u0010\u0014R\u001a\u0010\u0015\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0019R$\u0010\u001a\u001a\u00020\b2\u0006\u0010\u001a\u001a\u00020\b8F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\u001b\u0010\u001c\"\u0004\b\u001d\u0010\u001eR\u001a\u0010\u001f\u001a\u00020 X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b!\u0010\"\"\u0004\b#\u0010$R\u0017\u0010%\u001a\b\u0012\u0004\u0012\u00020\b0&8F\u00a2\u0006\u0006\u001a\u0004\b\'\u0010(R\u0011\u0010)\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b)\u0010\u0017R\u001a\u0010*\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b*\u0010\u0017\"\u0004\b+\u0010\u0019R$\u0010-\u001a\u00020\u00052\u0006\u0010,\u001a\u00020\u00058F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b-\u0010\u0017\"\u0004\b.\u0010\u0019R\u001e\u00100\u001a\u00020\u00052\u0006\u0010/\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010\u0017R\u0011\u00101\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b1\u0010\u0017R\u0011\u0010\t\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0017R\u001e\u00102\u001a\u00020\u00052\u0006\u0010/\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010\u0017R\u0016\u00103\u001a\n\u0012\u0004\u0012\u00020\u0005\u0018\u000104X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u00105\u001a\b\u0012\u0004\u0012\u00020\u000006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u00107\u001a\b\u0012\u0004\u0012\u00020\u000308X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u00109\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010:\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010;\u001a\b\u0012\u0004\u0012\u00020<0&\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010(R\u001c\u0010>\u001a\u0004\u0018\u00010\u0001X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b?\u0010@\"\u0004\bA\u0010BR\u001e\u0010C\u001a\u00020 2\u0006\u0010/\u001a\u00020 @BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\bD\u0010\"R\"\u0010E\u001a\n\u0012\u0004\u0012\u00020\u0005\u0018\u00010\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bF\u0010\u0014\"\u0004\bG\u0010HR\u0011\u0010I\u001a\u00020\b8F\u00a2\u0006\u0006\u001a\u0004\bJ\u0010\u001cR\u001c\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bK\u0010\u001c\"\u0004\bL\u0010\u001eR\u0011\u0010M\u001a\u00020\b8F\u00a2\u0006\u0006\u001a\u0004\bN\u0010\u001cR\u001a\u0010O\u001a\u00020PX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bQ\u0010R\"\u0004\bS\u0010TR\"\u0010U\u001a\n\u0012\u0004\u0012\u00020\u0000\u0018\u00010\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bV\u0010\u0014\"\u0004\bW\u0010HR\u0017\u0010X\u001a\b\u0012\u0004\u0012\u00020\u00000\u00128F\u00a2\u0006\u0006\u001a\u0004\bY\u0010\u0014R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\bZ\u0010[R\"\u0010\\\u001a\u0004\u0018\u00010\b2\b\u0010/\u001a\u0004\u0018\u00010\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b]\u0010\u001c\u00a8\u0006|"}, d2 = {"Lnet/jami/model/Contact;", "", "uri", "Lnet/jami/model/Uri;", "user", "", "(Lnet/jami/model/Uri;Z)V", "profileName", "", "isUser", "(Lnet/jami/model/Uri;Ljava/lang/String;Z)V", "addedDate", "Ljava/util/Date;", "getAddedDate", "()Ljava/util/Date;", "setAddedDate", "(Ljava/util/Date;)V", "conversationUri", "Lio/reactivex/rxjava3/core/Observable;", "getConversationUri", "()Lio/reactivex/rxjava3/core/Observable;", "detailsLoaded", "getDetailsLoaded", "()Z", "setDetailsLoaded", "(Z)V", "displayName", "getDisplayName", "()Ljava/lang/String;", "setDisplayName", "(Ljava/lang/String;)V", "id", "", "getId", "()J", "setId", "(J)V", "ids", "Ljava/util/ArrayList;", "getIds", "()Ljava/util/ArrayList;", "isBanned", "isFromSystem", "setFromSystem", "present", "isOnline", "setOnline", "<set-?>", "isStared", "isUnknown", "isUsernameLoaded", "mContactPresenceEmitter", "Lio/reactivex/rxjava3/core/Emitter;", "mContactUpdates", "Lio/reactivex/rxjava3/subjects/Subject;", "mConversationUri", "Lio/reactivex/rxjava3/subjects/BehaviorSubject;", "mLookupKey", "mOnline", "phones", "Lnet/jami/model/Phone;", "getPhones", "photo", "getPhoto", "()Ljava/lang/Object;", "setPhoto", "(Ljava/lang/Object;)V", "photoId", "getPhotoId", "presenceUpdates", "getPresenceUpdates", "setPresenceUpdates", "(Lio/reactivex/rxjava3/core/Observable;)V", "primaryNumber", "getPrimaryNumber", "getProfileName", "setProfileName", "ringUsername", "getRingUsername", "status", "Lnet/jami/model/Contact$Status;", "getStatus", "()Lnet/jami/model/Contact$Status;", "setStatus", "(Lnet/jami/model/Contact$Status;)V", "updates", "getUpdates", "setUpdates", "updatesSubject", "getUpdatesSubject", "getUri", "()Lnet/jami/model/Uri;", "username", "getUsername", "addNumber", "", "tel", "cat", "", "label", "type", "Lnet/jami/model/Phone$NumberType;", "addPhoneNumber", "hasNumber", "number", "hasPhoto", "matches", "query", "setConversationUri", "setPresenceEmitter", "emitter", "setProfile", "name", "profile", "Lnet/jami/model/Profile;", "setStared", "setSystemContactInfo", "k", "photo_id", "setSystemId", "setUsername", "toString", "Companion", "Status", "libringclient"})
public final class Contact {
    @org.jetbrains.annotations.NotNull()
    private final net.jami.model.Uri uri = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String profileName;
    private final boolean isUser = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String username;
    private long photoId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.util.ArrayList<net.jami.model.Phone> phones = null;
    private boolean isStared = false;
    private boolean isFromSystem = false;
    @org.jetbrains.annotations.NotNull()
    private net.jami.model.Contact.Status status = net.jami.model.Contact.Status.NO_REQUEST;
    @org.jetbrains.annotations.Nullable()
    private java.util.Date addedDate;
    private boolean mOnline = false;
    private long id = 0L;
    private java.lang.String mLookupKey;
    private boolean isUsernameLoaded = false;
    private boolean detailsLoaded = false;
    private final io.reactivex.rxjava3.subjects.BehaviorSubject<net.jami.model.Uri> mConversationUri = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Object photo;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Contact> mContactUpdates = null;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Observable<net.jami.model.Contact> updates;
    @org.jetbrains.annotations.Nullable()
    private io.reactivex.rxjava3.core.Observable<java.lang.Boolean> presenceUpdates;
    private io.reactivex.rxjava3.core.Emitter<java.lang.Boolean> mContactPresenceEmitter;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Contact.Companion Companion = null;
    private static final java.lang.String TAG = null;
    public static final int UNKNOWN_ID = -1;
    public static final int DEFAULT_ID = 0;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String PREFIX_RING = "ring:";
    
    private Contact(net.jami.model.Uri uri, java.lang.String profileName, boolean isUser) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Uri getUri() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProfileName() {
        return null;
    }
    
    public final void setProfileName(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean isUser() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUsername() {
        return null;
    }
    
    public final long getPhotoId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.ArrayList<net.jami.model.Phone> getPhones() {
        return null;
    }
    
    public final boolean isStared() {
        return false;
    }
    
    public final boolean isFromSystem() {
        return false;
    }
    
    public final void setFromSystem(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Contact.Status getStatus() {
        return null;
    }
    
    public final void setStatus(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact.Status p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.Date getAddedDate() {
        return null;
    }
    
    public final void setAddedDate(@org.jetbrains.annotations.Nullable()
    java.util.Date p0) {
    }
    
    public final long getId() {
        return 0L;
    }
    
    public final void setId(long p0) {
    }
    
    public final boolean isUsernameLoaded() {
        return false;
    }
    
    public final boolean getDetailsLoaded() {
        return false;
    }
    
    public final void setDetailsLoaded(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getPhoto() {
        return null;
    }
    
    public final void setPhoto(@org.jetbrains.annotations.Nullable()
    java.lang.Object p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Contact> getUpdates() {
        return null;
    }
    
    public final void setUpdates(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Observable<net.jami.model.Contact> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.reactivex.rxjava3.core.Observable<java.lang.Boolean> getPresenceUpdates() {
        return null;
    }
    
    public final void setPresenceUpdates(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Observable<java.lang.Boolean> p0) {
    }
    
    public Contact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri, boolean user) {
        super();
    }
    
    public final void setConversationUri(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Uri> getConversationUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Contact> getUpdatesSubject() {
        return null;
    }
    
    public final void setPresenceEmitter(@org.jetbrains.annotations.Nullable()
    io.reactivex.rxjava3.core.Emitter<java.lang.Boolean> emitter) {
    }
    
    public final boolean matches(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
        return false;
    }
    
    public final boolean isOnline() {
        return false;
    }
    
    public final void setOnline(boolean present) {
    }
    
    public final void setSystemId(long id) {
    }
    
    public final void setSystemContactInfo(long id, @org.jetbrains.annotations.Nullable()
    java.lang.String k, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName, long photo_id) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.ArrayList<java.lang.String> getIds() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    public final void setDisplayName(@org.jetbrains.annotations.NotNull()
    java.lang.String displayName) {
    }
    
    public final boolean hasNumber(@org.jetbrains.annotations.NotNull()
    java.lang.String number) {
        return false;
    }
    
    public final boolean hasNumber(@org.jetbrains.annotations.Nullable()
    net.jami.model.Uri number) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String toString() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPrimaryNumber() {
        return null;
    }
    
    public final void setStared() {
    }
    
    public final void addPhoneNumber(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri tel, int cat, @org.jetbrains.annotations.Nullable()
    java.lang.String label) {
    }
    
    public final void addNumber(@org.jetbrains.annotations.NotNull()
    java.lang.String tel, int cat, @org.jetbrains.annotations.Nullable()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    net.jami.model.Phone.NumberType type) {
    }
    
    public final void addNumber(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri tel, int cat, @org.jetbrains.annotations.Nullable()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    net.jami.model.Phone.NumberType type) {
    }
    
    public final boolean hasPhoto() {
        return false;
    }
    
    public final boolean isBanned() {
        return false;
    }
    
    public final boolean isUnknown() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRingUsername() {
        return null;
    }
    
    public final boolean setUsername(@org.jetbrains.annotations.Nullable()
    java.lang.String name) {
        return false;
    }
    
    public final void setProfile(@org.jetbrains.annotations.Nullable()
    net.jami.model.Profile profile) {
    }
    
    public final void setProfile(@org.jetbrains.annotations.Nullable()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.Object photo) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/Contact$Status;", "", "(Ljava/lang/String;I)V", "BANNED", "REQUEST_SENT", "CONFIRMED", "NO_REQUEST", "libringclient"})
    public static enum Status {
        /*public static final*/ BANNED /* = new BANNED() */,
        /*public static final*/ REQUEST_SENT /* = new REQUEST_SENT() */,
        /*public static final*/ CONFIRMED /* = new CONFIRMED() */,
        /*public static final*/ NO_REQUEST /* = new NO_REQUEST() */;
        
        Status() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00062\b\b\u0002\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\u0010J\u0012\u0010\u0011\u001a\u0004\u0018\u00010\u00062\b\u0010\u0012\u001a\u0004\u0018\u00010\u0006J\u000e\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0006R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lnet/jami/model/Contact$Companion;", "", "()V", "DEFAULT_ID", "", "PREFIX_RING", "", "TAG", "UNKNOWN_ID", "build", "Lnet/jami/model/Contact;", "uri", "isUser", "", "buildSIP", "to", "Lnet/jami/model/Uri;", "canonicalNumber", "number", "contactIdFromId", "", "id", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Contact buildSIP(@org.jetbrains.annotations.NotNull()
        net.jami.model.Uri to) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Contact build(@org.jetbrains.annotations.NotNull()
        java.lang.String uri, boolean isUser) {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String canonicalNumber(@org.jetbrains.annotations.Nullable()
        java.lang.String number) {
            return null;
        }
        
        public final long contactIdFromId(@org.jetbrains.annotations.NotNull()
        java.lang.String id) {
            return 0L;
        }
    }
}