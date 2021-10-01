package net.jami.smartlist;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0010\r\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u001b\u0010\u0006\u001a\u00020\u00032\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH&\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\u0005H&J\u0010\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\u000fH&J\u0010\u0010\u0010\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\u0005H&J\b\u0010\u0011\u001a\u00020\u0003H&J\b\u0010\u0012\u001a\u00020\u0003H&J \u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\u00052\u0006\u0010\u0016\u001a\u00020\u0015H&J\u0018\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\u0005H&J\b\u0010\u0018\u001a\u00020\u0003H&J\b\u0010\u0019\u001a\u00020\u0003H&J\b\u0010\u001a\u001a\u00020\u0003H&J\b\u0010\u001b\u001a\u00020\u0003H&J\u0010\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u001eH&J\u0010\u0010\u001f\u001a\u00020\u00032\u0006\u0010 \u001a\u00020!H&J\u0010\u0010\u001f\u001a\u00020\u00032\u0006\u0010\"\u001a\u00020\u000fH&J \u0010#\u001a\u00020\u00032\u000e\u0010$\u001a\n\u0012\u0004\u0012\u00020\u000f\u0018\u00010%2\u0006\u0010&\u001a\u00020\'H&\u00a8\u0006("}, d2 = {"Lnet/jami/smartlist/SmartListView;", "", "copyNumber", "", "uri", "Lnet/jami/model/Uri;", "displayChooseNumberDialog", "numbers", "", "", "([Ljava/lang/CharSequence;)V", "displayClearDialog", "conversationUri", "displayConversationDialog", "smartListViewModel", "Lnet/jami/smartlist/SmartListViewModel;", "displayDeleteDialog", "displayMenuItem", "displayNoConversationMessage", "goToCallActivity", "accountId", "", "contactId", "goToConversation", "goToQRFragment", "hideList", "hideNoConversationMessage", "scrollToTop", "setLoading", "loading", "", "update", "position", "", "model", "updateList", "smartListViewModels", "", "parentDisposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "libringclient"})
public abstract interface SmartListView {
    
    public abstract void displayChooseNumberDialog(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence[] numbers);
    
    public abstract void displayNoConversationMessage();
    
    public abstract void displayConversationDialog(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel);
    
    public abstract void displayClearDialog(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri);
    
    public abstract void displayDeleteDialog(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri);
    
    public abstract void copyNumber(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri);
    
    public abstract void setLoading(boolean loading);
    
    public abstract void displayMenuItem();
    
    public abstract void hideList();
    
    public abstract void hideNoConversationMessage();
    
    public abstract void updateList(@org.jetbrains.annotations.Nullable()
    java.util.List<net.jami.smartlist.SmartListViewModel> smartListViewModels, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.disposables.CompositeDisposable parentDisposable);
    
    public abstract void update(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel model);
    
    public abstract void update(int position);
    
    public abstract void goToConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri);
    
    public abstract void goToCallActivity(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String contactId);
    
    public abstract void goToQRFragment();
    
    public abstract void scrollToTop();
}