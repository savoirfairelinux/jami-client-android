package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00d2\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u001e\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u000b\u0018\u0000 n2\u00020\u0001:\u0001nBE\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\u0002\u0010\u0012J\u0016\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020-J\u0016\u0010.\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\u0006\u0010/\u001a\u00020-J*\u00100\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\u0006\u00101\u001a\u00020-2\b\u00102\u001a\u0004\u0018\u00010+2\b\u00103\u001a\u0004\u0018\u00010+J\u000e\u00104\u001a\u00020)2\u0006\u00105\u001a\u000206J\u0006\u00107\u001a\u000208J\u0016\u00109\u001a\u0002082\u0006\u0010*\u001a\u00020+2\u0006\u0010:\u001a\u00020-J\"\u0010;\u001a\b\u0012\u0004\u0012\u00020\u001b0<2\u0006\u0010*\u001a\u00020+2\f\u0010=\u001a\b\u0012\u0004\u0012\u00020?0>J\u0016\u0010@\u001a\u00020)2\u0006\u0010A\u001a\u00020\u001b2\u0006\u0010B\u001a\u000206J\u0016\u0010C\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\u0006\u0010:\u001a\u00020-J\u0014\u0010D\u001a\b\u0012\u0004\u0012\u00020 0<2\u0006\u0010*\u001a\u00020+J \u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00150\u00142\f\u0010E\u001a\b\u0012\u0004\u0012\u00020 0\u0014J\u0016\u0010F\u001a\b\u0012\u0004\u0012\u00020\u001b0<2\u0006\u0010A\u001a\u00020\u001bH\u0002J<\u0010G\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00142\f\u0010E\u001a\b\u0012\u0004\u0012\u00020 0\u00142\f\u0010H\u001a\b\u0012\u0004\u0012\u00020+0\u00142\u0006\u0010I\u001a\u00020JJ&\u0010%\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00142\f\u0010E\u001a\b\u0012\u0004\u0012\u00020 0\u0014J0\u0010K\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00142\u0006\u0010L\u001a\u00020 2\f\u0010H\u001a\b\u0012\u0004\u0012\u00020+0\u0014H\u0002J*\u0010K\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0<2\u0006\u0010L\u001a\u00020 2\u0006\u0010H\u001a\u00020+H\u0002J.\u0010M\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00142\f\u0010E\u001a\b\u0012\u0004\u0012\u00020 0\u00142\u0006\u0010I\u001a\u00020JJ \u0010M\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00142\u0006\u0010I\u001a\u00020JJ\u0016\u0010N\u001a\b\u0012\u0004\u0012\u00020 0<2\u0006\u0010L\u001a\u00020 H\u0002J\u0010\u0010O\u001a\u00020)2\u0006\u0010P\u001a\u00020QH\u0002J\u001c\u0010R\u001a\b\u0012\u0004\u0012\u00020\u001b0<2\u0006\u0010L\u001a\u00020 2\u0006\u0010/\u001a\u00020-J\u0016\u0010S\u001a\b\u0012\u0004\u0012\u00020 0<2\u0006\u0010L\u001a\u00020 H\u0002J&\u0010T\u001a\b\u0012\u0004\u0012\u00020\u00160\u00142\u0006\u0010L\u001a\u00020 2\u0006\u0010A\u001a\u00020\u001b2\u0006\u0010I\u001a\u00020JH\u0002J\u0010\u0010U\u001a\u00020)2\u0006\u0010V\u001a\u00020WH\u0002J\u0010\u0010X\u001a\u00020)2\u0006\u0010Y\u001a\u00020ZH\u0002J\u0010\u0010[\u001a\u00020)2\u0006\u0010\\\u001a\u00020]H\u0002J\u0018\u0010^\u001a\u0004\u0018\u00010+2\u0006\u0010*\u001a\u00020+2\u0006\u0010:\u001a\u00020-J \u0010^\u001a\u0004\u0018\u00010+2\u0006\u0010L\u001a\u00020 2\u0006\u0010A\u001a\u00020\u001b2\u0006\u0010_\u001a\u00020JJ\u0012\u0010^\u001a\u0004\u0018\u00010+2\u0006\u0010A\u001a\u00020\u001bH\u0002J\u0016\u0010`\u001a\u0002082\u0006\u0010*\u001a\u00020+2\u0006\u0010/\u001a\u00020-J\u001e\u0010a\u001a\u0002082\u0006\u0010A\u001a\u00020\u001b2\u0006\u0010b\u001a\u00020-2\u0006\u0010c\u001a\u00020dJ\u001e\u0010e\u001a\u00020)2\u0006\u0010f\u001a\u00020\u001b2\u0006\u0010g\u001a\u00020Z2\u0006\u0010\\\u001a\u00020+J \u0010e\u001a\u0002082\u0006\u0010f\u001a\u00020\u001b2\u0006\u0010b\u001a\u00020-2\b\u0010\\\u001a\u0004\u0018\u00010+J\u001e\u0010h\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\u0006\u0010/\u001a\u00020-2\u0006\u0010i\u001a\u00020JJ\u001c\u0010j\u001a\b\u0012\u0004\u0012\u00020\u001b0<2\u0006\u0010*\u001a\u00020+2\u0006\u0010k\u001a\u00020-J\u001c\u0010l\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\f\u0010m\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001dR\u001d\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00150\u00148F\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\u0018R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001d0\u00148F\u00a2\u0006\u0006\u001a\u0004\b\u001e\u0010\u0018R\u0017\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020 0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0018R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010$\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00140\u001d0\u00148F\u00a2\u0006\u0006\u001a\u0004\b%\u0010\u0018R\u0017\u0010&\u001a\b\u0012\u0004\u0012\u00020\u001b0\u00148F\u00a2\u0006\u0006\u001a\u0004\b\'\u0010\u0018\u00a8\u0006o"}, d2 = {"Lnet/jami/services/ConversationFacade;", "", "mHistoryService", "Lnet/jami/services/HistoryService;", "mCallService", "Lnet/jami/services/CallService;", "mAccountService", "Lnet/jami/services/AccountService;", "mContactService", "Lnet/jami/services/ContactService;", "mNotificationService", "Lnet/jami/services/NotificationService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mPreferencesService", "Lnet/jami/services/PreferencesService;", "(Lnet/jami/services/HistoryService;Lnet/jami/services/CallService;Lnet/jami/services/AccountService;Lnet/jami/services/ContactService;Lnet/jami/services/NotificationService;Lnet/jami/services/HardwareService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/PreferencesService;)V", "contactList", "Lio/reactivex/rxjava3/core/Observable;", "", "Lnet/jami/smartlist/SmartListViewModel;", "getContactList", "()Lio/reactivex/rxjava3/core/Observable;", "conversationSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "Lnet/jami/model/Conversation;", "conversationsSubject", "", "getConversationsSubject", "currentAccountSubject", "Lnet/jami/model/Account;", "getCurrentAccountSubject", "mDisposableBag", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "pendingList", "getPendingList", "updatedConversation", "getUpdatedConversation", "acceptRequest", "", "accountId", "", "contactUri", "Lnet/jami/model/Uri;", "banConversation", "conversationUri", "cancelFileTransfer", "conversationId", "messageId", "fileId", "cancelMessage", "message", "Lnet/jami/model/Interaction;", "clearAllHistory", "Lio/reactivex/rxjava3/core/Completable;", "clearHistory", "contact", "createConversation", "Lio/reactivex/rxjava3/core/Single;", "currentSelection", "", "Lnet/jami/model/Contact;", "deleteConversationItem", "conversation", "element", "discardRequest", "getAccountSubject", "currentAccount", "getConversationHistory", "getFullList", "query", "hasPresence", "", "getSearchResults", "account", "getSmartList", "getSmartlist", "handleDataTransferEvent", "transfer", "Lnet/jami/model/DataTransfer;", "loadConversationHistory", "loadSmartlist", "observeConversation", "onCallStateChange", "call", "Lnet/jami/model/Call;", "onConfStateChange", "conference", "Lnet/jami/model/Conference;", "parseNewMessage", "txt", "Lnet/jami/model/TextMessage;", "readMessages", "cancelNotification", "removeConversation", "sendFile", "to", "file", "Ljava/io/File;", "sendTextMessage", "c", "conf", "setIsComposing", "isComposing", "startConversation", "contactId", "updateTextNotifications", "conversations", "Companion", "libringclient"})
public final class ConversationFacade {
    private final net.jami.services.HistoryService mHistoryService = null;
    private final net.jami.services.CallService mCallService = null;
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.ContactService mContactService = null;
    private final net.jami.services.NotificationService mNotificationService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.PreferencesService mPreferencesService = null;
    private final io.reactivex.rxjava3.disposables.CompositeDisposable mDisposableBag = null;
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccountSubject = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Conversation> conversationSubject = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.ConversationFacade.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    public ConversationFacade(@org.jetbrains.annotations.NotNull()
    net.jami.services.HistoryService mHistoryService, @org.jetbrains.annotations.NotNull()
    net.jami.services.CallService mCallService, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.ContactService mContactService, @org.jetbrains.annotations.NotNull()
    net.jami.services.NotificationService mNotificationService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferencesService) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> getCurrentAccountSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Conversation> getUpdatedConversation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> startConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Account> getAccountSubject(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Conversation>> getConversationsSubject() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String readMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contact) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String readMessages(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, boolean cancelNotification) {
        return null;
    }
    
    private final java.lang.String readMessages(net.jami.model.Conversation conversation) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable sendTextMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation c, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri to, @org.jetbrains.annotations.Nullable()
    java.lang.String txt) {
        return null;
    }
    
    public final void sendTextMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation c, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conference conf, @org.jetbrains.annotations.NotNull()
    java.lang.String txt) {
    }
    
    public final void setIsComposing(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, boolean isComposing) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable sendFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri to, @org.jetbrains.annotations.NotNull()
    java.io.File file) {
        return null;
    }
    
    public final void deleteConversationItem(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction element) {
    }
    
    public final void cancelMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction message) {
    }
    
    /**
     * Loads the smartlist from cache or database
     *
     * @param account the user account
     * @return an account single
     */
    private final io.reactivex.rxjava3.core.Single<net.jami.model.Account> loadSmartlist(net.jami.model.Account account) {
        return null;
    }
    
    /**
     * Loads history for a specific conversation from cache or database
     *
     * @param account         the user account
     * @param conversationUri the conversation
     * @return a conversation single
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> loadConversationHistory(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel> observeConversation(net.jami.model.Account account, net.jami.model.Conversation conversation, boolean hasPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getSmartList(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccount, boolean hasPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.smartlist.SmartListViewModel>> getContactList(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccount) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getPendingList(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccount) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getSmartList(boolean hasPresence) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getPendingList() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.smartlist.SmartListViewModel>> getContactList() {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Single<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getSearchResults(net.jami.model.Account account, java.lang.String query) {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getSearchResults(net.jami.model.Account account, io.reactivex.rxjava3.core.Observable<java.lang.String> query) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> getFullList(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Observable<net.jami.model.Account> currentAccount, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Observable<java.lang.String> query, boolean hasPresence) {
        return null;
    }
    
    /**
     * Loads the smartlist from the database and updates the view
     *
     * @param account the user account
     */
    private final io.reactivex.rxjava3.core.Single<net.jami.model.Account> getSmartlist(net.jami.model.Account account) {
        return null;
    }
    
    /**
     * Loads a conversation's history from the database
     *
     * @param conversation a conversation object with a valid conversation ID
     * @return a conversation single
     */
    private final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> getConversationHistory(net.jami.model.Conversation conversation) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable clearHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contact) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable clearAllHistory() {
        return null;
    }
    
    public final void updateTextNotifications(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Conversation> conversations) {
    }
    
    private final void parseNewMessage(net.jami.model.TextMessage txt) {
    }
    
    public final void acceptRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactUri) {
    }
    
    public final void discardRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contact) {
    }
    
    private final void handleDataTransferEvent(net.jami.model.DataTransfer transfer) {
    }
    
    private final void onConfStateChange(net.jami.model.Conference conference) {
    }
    
    private final void onCallStateChange(net.jami.model.Call call) {
    }
    
    public final void cancelFileTransfer(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationId, @org.jetbrains.annotations.Nullable()
    java.lang.String messageId, @org.jetbrains.annotations.Nullable()
    java.lang.String fileId) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable removeConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
        return null;
    }
    
    public final void banConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.Conversation> createConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.util.Collection<net.jami.model.Contact> currentSelection) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/services/ConversationFacade$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}