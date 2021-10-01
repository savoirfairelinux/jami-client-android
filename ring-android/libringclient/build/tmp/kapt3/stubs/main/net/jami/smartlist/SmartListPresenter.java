package net.jami.smartlist;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010 \n\u0002\b\u0005\u0018\u0000 -2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001-B\u0019\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\b\b\u0001\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u0010\u0010\u0019\u001a\u00020\u00162\u0006\u0010\u001a\u001a\u00020\u0002H\u0016J\u0010\u0010\u001b\u001a\u00020\u00162\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dJ\u000e\u0010\u001b\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010\u001e\u001a\u00020\u0016J\u000e\u0010\u001f\u001a\u00020\u00162\u0006\u0010 \u001a\u00020\u0018J\u000e\u0010!\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\"\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010#\u001a\u00020\u0016J\u000e\u0010$\u001a\u00020\u00162\u0006\u0010%\u001a\u00020\u000eJ\u000e\u0010&\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u001dJ\u000e\u0010&\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\"\u0010\'\u001a\u00020\u00162\u0018\u0010(\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00180\t0)0\tH\u0002J\u001a\u0010*\u001a\u00020\u00162\u0006\u0010+\u001a\u00020\u000e2\b\u0010,\u001a\u0004\u0018\u00010\u001dH\u0002J\u000e\u0010*\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u001dR\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R<\u0010\u000f\u001a0\u0012\f\u0012\n \u0010*\u0004\u0018\u00010\u000e0\u000e \u0010*\u0017\u0012\f\u0012\n \u0010*\u0004\u0018\u00010\u000e0\u000e\u0018\u00010\t\u00a2\u0006\u0002\b\u00110\t\u00a2\u0006\u0002\b\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lnet/jami/smartlist/SmartListPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/smartlist/SmartListView;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/ConversationFacade;Lio/reactivex/rxjava3/core/Scheduler;)V", "accountSubject", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/model/Account;", "mAccount", "mCurrentQuery", "Lio/reactivex/rxjava3/subjects/Subject;", "", "mDebouncedQuery", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "mQuery", "mQueryDisposable", "Lio/reactivex/rxjava3/disposables/Disposable;", "banContact", "", "smartListViewModel", "Lnet/jami/smartlist/SmartListViewModel;", "bindView", "view", "clearConversation", "uri", "Lnet/jami/model/Uri;", "clickQRSearch", "conversationClicked", "viewModel", "conversationLongClicked", "copyNumber", "fabButtonClicked", "queryTextChanged", "query", "removeConversation", "showConversations", "conversations", "", "startConversation", "accountId", "conversationUri", "Companion", "libringclient"})
public final class SmartListPresenter extends net.jami.mvp.RootPresenter<net.jami.smartlist.SmartListView> {
    private final net.jami.services.ConversationFacade mConversationFacade = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private io.reactivex.rxjava3.disposables.Disposable mQueryDisposable;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.String> mCurrentQuery = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.lang.String> mQuery = null;
    private final io.reactivex.rxjava3.core.Observable<java.lang.String> mDebouncedQuery = null;
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> accountSubject = null;
    private net.jami.model.Account mAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.smartlist.SmartListPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public SmartListPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListView view) {
    }
    
    public final void queryTextChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    public final void conversationClicked(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel viewModel) {
    }
    
    public final void conversationLongClicked(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel) {
    }
    
    public final void fabButtonClicked() {
    }
    
    private final void startConversation(java.lang.String accountId, net.jami.model.Uri conversationUri) {
    }
    
    public final void startConversation(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
    }
    
    public final void copyNumber(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel) {
    }
    
    public final void clearConversation(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel) {
    }
    
    public final void clearConversation(@org.jetbrains.annotations.Nullable()
    net.jami.model.Uri uri) {
    }
    
    public final void removeConversation(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel) {
    }
    
    public final void removeConversation(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
    }
    
    public final void banContact(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel smartListViewModel) {
    }
    
    public final void clickQRSearch() {
    }
    
    private final void showConversations(io.reactivex.rxjava3.core.Observable<java.util.List<io.reactivex.rxjava3.core.Observable<net.jami.smartlist.SmartListViewModel>>> conversations) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0016\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/smartlist/SmartListPresenter$Companion;", "", "()V", "TAG", "", "kotlin.jvm.PlatformType", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}