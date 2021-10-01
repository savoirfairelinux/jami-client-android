package net.jami.conversation;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000t\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\r\n\u0002\b\u001a\bf\u0018\u00002\u00020\u0001J \u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH&J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\fH&J\b\u0010\r\u001a\u00020\u0003H&J\b\u0010\u000e\u001a\u00020\u0003H&J\b\u0010\u000f\u001a\u00020\u0003H&J\u0010\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u0012H&J\u0010\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\u0015H&J\b\u0010\u0016\u001a\u00020\u0003H&J\u0018\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0018\u001a\u00020\u0007H&J\u0010\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u001a\u001a\u00020\u001bH&J\u0010\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u001eH&J\u0010\u0010\u001f\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u0005H&J(\u0010!\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\"\u001a\u00020\u00072\u0006\u0010#\u001a\u00020\u001bH&J\u0018\u0010$\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010%\u001a\u00020\u0007H&J\b\u0010&\u001a\u00020\u0003H&J\b\u0010\'\u001a\u00020\u0003H&J\b\u0010(\u001a\u00020\u0003H&J\b\u0010)\u001a\u00020\u0003H&J\u0018\u0010*\u001a\u00020\u00032\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020\u0005H&J\b\u0010.\u001a\u00020\u0003H&J\u0016\u0010/\u001a\u00020\u00032\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\f00H&J \u00101\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH&J\u0010\u00102\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\fH&J\b\u00103\u001a\u00020\u0003H&J\u0010\u00104\u001a\u00020\u00032\u0006\u00105\u001a\u000206H&J\u0010\u00107\u001a\u00020\u00032\u0006\u00108\u001a\u000209H&J\u0010\u0010:\u001a\u00020\u00032\u0006\u0010;\u001a\u00020<H&J\u0010\u0010=\u001a\u00020\u00032\u0006\u0010>\u001a\u00020\fH&J\u0010\u0010?\u001a\u00020\u00032\u0006\u0010@\u001a\u00020\u001bH&J\u0018\u0010A\u001a\u00020\u00032\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020\u0005H&J \u0010B\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010C\u001a\u00020\u00052\u0006\u0010D\u001a\u00020\u001bH&J\u0018\u0010E\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010F\u001a\u00020\u0005H&J\u0018\u0010G\u001a\u00020\u00032\u0006\u0010H\u001a\u00020\t2\u0006\u0010I\u001a\u00020\u0005H&J\u0018\u0010J\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010C\u001a\u00020\u0005H&J\b\u0010K\u001a\u00020\u0003H&J\b\u0010L\u001a\u00020\u0003H&J\u0010\u0010M\u001a\u00020\u00032\u0006\u0010N\u001a\u00020\u0005H&J\b\u0010O\u001a\u00020\u0003H&J\u0010\u0010P\u001a\u00020\u00032\u0006\u0010Q\u001a\u00020\u0005H&J\u0010\u0010R\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u001eH&J\u0010\u0010S\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\fH&J\u0010\u0010T\u001a\u00020\u00032\u0006\u0010U\u001a\u00020\u0005H&\u00a8\u0006V"}, d2 = {"Lnet/jami/conversation/ConversationView;", "", "acceptFile", "", "accountId", "", "conversationUri", "Lnet/jami/model/Uri;", "transfer", "Lnet/jami/model/DataTransfer;", "addElement", "e", "Lnet/jami/model/Interaction;", "askWriteExternalStoragePermission", "clearMsgEdit", "displayAccountOfflineErrorPanel", "displayContact", "conversation", "Lnet/jami/model/Conversation;", "displayErrorToast", "error", "Lnet/jami/model/Error;", "displayNetworkErrorPanel", "displayNumberSpinner", "number", "displayOnGoingCallPane", "display", "", "goToAddContact", "contact", "Lnet/jami/model/Contact;", "goToCallActivity", "conferenceId", "goToCallActivityWithResult", "contactUri", "audioOnly", "goToContactActivity", "uri", "goToHome", "hideErrorPanel", "hideMap", "hideNumberSpinner", "openFile", "path", "Ljava/io/File;", "displayName", "openFilePicker", "refreshView", "", "refuseFile", "removeElement", "scrollToEnd", "setComposingStatus", "composingStatus", "Lnet/jami/model/Account$ComposingStatus;", "setConversationColor", "integer", "", "setConversationSymbol", "symbol", "", "setLastDisplayed", "interaction", "setReadIndicatorStatus", "show", "shareFile", "showMap", "contactId", "open", "showPluginListHandlers", "peerId", "startSaveFile", "currentFile", "fileAbsolutePath", "startShareLocation", "switchToConversationView", "switchToEndedView", "switchToIncomingTrustRequestView", "message", "switchToSyncingView", "switchToUnknownView", "name", "updateContact", "updateElement", "updateLastRead", "last", "libringclient"})
public abstract interface ConversationView {
    
    public abstract void refreshView(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends net.jami.model.Interaction> conversation);
    
    public abstract void scrollToEnd();
    
    public abstract void updateContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    public abstract void displayContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation);
    
    public abstract void displayOnGoingCallPane(boolean display);
    
    public abstract void displayNumberSpinner(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri number);
    
    public abstract void displayErrorToast(@org.jetbrains.annotations.NotNull()
    net.jami.model.Error error);
    
    public abstract void hideNumberSpinner();
    
    public abstract void clearMsgEdit();
    
    public abstract void goToHome();
    
    public abstract void goToAddContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    public abstract void goToCallActivity(@org.jetbrains.annotations.NotNull()
    java.lang.String conferenceId);
    
    public abstract void goToCallActivityWithResult(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactUri, boolean audioOnly);
    
    public abstract void goToContactActivity(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri);
    
    public abstract void switchToUnknownView(@org.jetbrains.annotations.NotNull()
    java.lang.String name);
    
    public abstract void switchToIncomingTrustRequestView(@org.jetbrains.annotations.NotNull()
    java.lang.String message);
    
    public abstract void switchToConversationView();
    
    public abstract void switchToSyncingView();
    
    public abstract void switchToEndedView();
    
    public abstract void askWriteExternalStoragePermission();
    
    public abstract void openFilePicker();
    
    public abstract void acceptFile(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer);
    
    public abstract void refuseFile(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer transfer);
    
    public abstract void shareFile(@org.jetbrains.annotations.NotNull()
    java.io.File path, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName);
    
    public abstract void openFile(@org.jetbrains.annotations.NotNull()
    java.io.File path, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName);
    
    public abstract void addElement(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction e);
    
    public abstract void updateElement(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction e);
    
    public abstract void removeElement(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction e);
    
    public abstract void setComposingStatus(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account.ComposingStatus composingStatus);
    
    public abstract void setLastDisplayed(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction);
    
    public abstract void setConversationColor(int integer);
    
    public abstract void setConversationSymbol(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence symbol);
    
    public abstract void startSaveFile(@org.jetbrains.annotations.NotNull()
    net.jami.model.DataTransfer currentFile, @org.jetbrains.annotations.NotNull()
    java.lang.String fileAbsolutePath);
    
    public abstract void startShareLocation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactId);
    
    public abstract void showMap(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactId, boolean open);
    
    public abstract void hideMap();
    
    public abstract void showPluginListHandlers(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String peerId);
    
    public abstract void hideErrorPanel();
    
    public abstract void displayNetworkErrorPanel();
    
    public abstract void displayAccountOfflineErrorPanel();
    
    public abstract void setReadIndicatorStatus(boolean show);
    
    public abstract void updateLastRead(@org.jetbrains.annotations.NotNull()
    java.lang.String last);
}