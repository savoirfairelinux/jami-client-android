package net.jami.conversation;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u00ac\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\r\n\u0002\b\t\u0018\u0000 c2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001cBI\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u0012\b\b\u0001\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\u0002\u0010\u0013J\u000e\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\'J\u000e\u0010(\u001a\u00020%2\u0006\u0010)\u001a\u00020*J\u000e\u0010+\u001a\u00020%2\u0006\u0010,\u001a\u00020-J\u0006\u0010.\u001a\u00020%J\u000e\u0010/\u001a\u00020%2\u0006\u00100\u001a\u00020-J\u000e\u00101\u001a\u00020%2\u0006\u00102\u001a\u00020*J\u0016\u00103\u001a\u00020%2\u0006\u00104\u001a\u00020\u001d2\u0006\u00105\u001a\u00020!J(\u00106\u001a\u00020%2\u0006\u00107\u001a\u0002082\u0006\u00109\u001a\u00020\u00172\u0006\u0010:\u001a\u00020;2\u0006\u0010<\u001a\u00020\u0002H\u0002J \u0010=\u001a\u00020%2\u0006\u00107\u001a\u0002082\u0006\u0010>\u001a\u00020\u00172\u0006\u0010<\u001a\u00020\u0002H\u0002J\u0006\u0010?\u001a\u00020%J\u0006\u0010@\u001a\u00020%J\u0006\u0010A\u001a\u00020%J\u0006\u0010B\u001a\u00020%J\u0006\u0010C\u001a\u00020%J\u000e\u0010D\u001a\u00020%2\u0006\u0010E\u001a\u00020*J\u0006\u0010F\u001a\u00020%J\u0006\u0010G\u001a\u00020%J\u000e\u0010H\u001a\u00020%2\u0006\u0010I\u001a\u00020-J\u0006\u0010J\u001a\u00020%J\u000e\u0010K\u001a\u00020%2\u0006\u0010&\u001a\u00020\'J\u000e\u0010L\u001a\u00020%2\u0006\u0010M\u001a\u00020*J\u000e\u0010N\u001a\u00020%2\u0006\u0010I\u001a\u00020-J\u0006\u0010O\u001a\u00020%J\u000e\u0010P\u001a\u00020%2\u0006\u0010Q\u001a\u00020RJ\u0010\u0010S\u001a\u00020%2\b\u0010,\u001a\u0004\u0018\u00010!J\b\u0010T\u001a\u00020%H\u0002J\u0018\u0010U\u001a\u00020%2\u0006\u00107\u001a\u0002082\u0006\u00109\u001a\u00020\u0017H\u0002J\u000e\u0010V\u001a\u00020%2\u0006\u0010W\u001a\u00020XJ\u000e\u0010Y\u001a\u00020%2\u0006\u0010Z\u001a\u00020[J\u000e\u0010\\\u001a\u00020%2\u0006\u0010I\u001a\u00020-J\u0006\u0010]\u001a\u00020%J\u0006\u0010^\u001a\u00020%J\b\u0010_\u001a\u00020*H\u0002J\b\u0010`\u001a\u00020*H\u0002J\b\u0010a\u001a\u00020%H\u0016J\u0012\u0010b\u001a\u00020%2\b\u00109\u001a\u0004\u0018\u00010\u0017H\u0002R\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00170\u001bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001f\u001a\u000e\u0012\u0004\u0012\u00020!\u0012\u0004\u0012\u00020!0 8F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010#\u00a8\u0006d"}, d2 = {"Lnet/jami/conversation/ConversationPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/conversation/ConversationView;", "mContactService", "Lnet/jami/services/ContactService;", "mAccountService", "Lnet/jami/services/AccountService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mVCardService", "Lnet/jami/services/VCardService;", "deviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mPreferencesService", "Lnet/jami/services/PreferencesService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/ContactService;Lnet/jami/services/AccountService;Lnet/jami/services/HardwareService;Lnet/jami/services/ConversationFacade;Lnet/jami/services/VCardService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/PreferencesService;Lio/reactivex/rxjava3/core/Scheduler;)V", "getDeviceRuntimeService", "()Lnet/jami/services/DeviceRuntimeService;", "mConversation", "Lnet/jami/model/Conversation;", "mConversationDisposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "mConversationSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "mConversationUri", "Lnet/jami/model/Uri;", "mVisibilityDisposable", "path", "Lkotlin/Pair;", "", "getPath", "()Lkotlin/Pair;", "acceptFile", "", "transfer", "Lnet/jami/model/DataTransfer;", "cameraPermissionChanged", "isGranted", "", "cancelMessage", "message", "Lnet/jami/model/Interaction;", "clickOnGoingPane", "deleteConversationItem", "element", "goToCall", "audioOnly", "init", "conversationUri", "accountId", "initContact", "account", "Lnet/jami/model/Account;", "conversation", "mode", "Lnet/jami/model/Conversation$Mode;", "view", "initView", "c", "loadMore", "noSpaceLeft", "onAcceptIncomingContactRequest", "onAddContact", "onBlockIncomingContactRequest", "onComposingChanged", "hasMessage", "onRefuseIncomingContactRequest", "openContact", "openFile", "interaction", "pause", "refuseFile", "resume", "isBubble", "saveFile", "selectFile", "sendFile", "file", "Ljava/io/File;", "sendTextMessage", "sendTrustRequest", "setConversation", "setConversationColor", "color", "", "setConversationSymbol", "symbol", "", "shareFile", "shareLocation", "showPluginListHandlers", "showReadIndicator", "showTypingIndicator", "unbindView", "updateOngoingCallView", "Companion", "libringclient"})
public final class ConversationPresenter extends net.jami.mvp.RootPresenter<net.jami.conversation.ConversationView> {
    private final net.jami.services.ContactService mContactService = null;
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.ConversationFacade mConversationFacade = null;
    private final net.jami.services.VCardService mVCardService = null;
    @org.jetbrains.annotations.NotNull()
    private final net.jami.services.DeviceRuntimeService deviceRuntimeService = null;
    private final net.jami.services.PreferencesService mPreferencesService = null;
    private io.reactivex.rxjava3.core.Scheduler mUiScheduler;
    private net.jami.model.Conversation mConversation;
    private net.jami.model.Uri mConversationUri;
    private io.reactivex.rxjava3.disposables.CompositeDisposable mConversationDisposable;
    private final io.reactivex.rxjava3.disposables.CompositeDisposable mVisibilityDisposable = null;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Conversation> mConversationSubject = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.conversation.ConversationPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public ConversationPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.ContactService mContactService, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    net.jami.services.VCardService mVCardService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService deviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferencesService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.services.DeviceRuntimeService getDeviceRuntimeService() {
        return null;
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    @java.lang.Override()
    public void unbindView() {
    }
    
    private final void setConversation(net.jami.model.Account account, net.jami.model.Conversation conversation) {
    }
    
    public final void pause() {
    }
    
    public final void resume(boolean isBubble) {
    }
    
    private final void initContact(net.jami.model.Account account, net.jami.model.Conversation conversation, net.jami.model.Conversation.Mode mode, net.jami.conversation.ConversationView view) {
    }
    
    private final void initView(net.jami.model.Account account, net.jami.model.Conversation c, net.jami.conversation.ConversationView view) {
    }
    
    public final void loadMore() {
    }
    
    public final void openContact() {
    }
    
    public final void sendTextMessage(@org.jetbrains.annotations.Nullable()
    java.lang.String message) {
    }
    
    public final void selectFile() {
    }
    
    public final void sendFile(@org.jetbrains.annotations.NotNull()
    java.io.File file) {
    }
    
    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the ConversationView in order to start saving the file
     *
     * @param interaction an interaction representing a datat transfer
     */
    public final void saveFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
    }
    
    public final void shareFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
    }
    
    public final void openFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
    }
    
    public final void acceptFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
    }
    
    public final void refuseFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer) {
    }
    
    public final void deleteConversationItem(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction element) {
    }
    
    public final void cancelMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction message) {
    }
    
    private final void sendTrustRequest() {
    }
    
    public final void clickOnGoingPane() {
    }
    
    public final void goToCall(boolean audioOnly) {
    }
    
    private final void updateOngoingCallView(net.jami.model.Conversation conversation) {
    }
    
    public final void onBlockIncomingContactRequest() {
    }
    
    public final void onRefuseIncomingContactRequest() {
    }
    
    public final void onAcceptIncomingContactRequest() {
    }
    
    public final void onAddContact() {
    }
    
    public final void noSpaceLeft() {
    }
    
    public final void setConversationColor(int color) {
    }
    
    public final void setConversationSymbol(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence symbol) {
    }
    
    public final void cameraPermissionChanged(boolean isGranted) {
    }
    
    public final void shareLocation() {
    }
    
    public final void showPluginListHandlers() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlin.Pair<java.lang.String, java.lang.String> getPath() {
        return null;
    }
    
    public final void onComposingChanged(boolean hasMessage) {
    }
    
    private final boolean showTypingIndicator() {
        return false;
    }
    
    private final boolean showReadIndicator() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/conversation/ConversationPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}