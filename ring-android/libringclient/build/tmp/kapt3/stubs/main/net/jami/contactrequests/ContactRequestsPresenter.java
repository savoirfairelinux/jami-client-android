package net.jami.contactrequests;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u00192\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0019B!\b\u0001\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0001\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tJ\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0002H\u0016J\u0016\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016J\b\u0010\u0017\u001a\u00020\u0010H\u0016J\u0010\u0010\u0018\u001a\u00020\u00102\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014R<\u0010\n\u001a0\u0012\f\u0012\n \r*\u0004\u0018\u00010\f0\f \r*\u0017\u0012\f\u0012\n \r*\u0004\u0018\u00010\f0\f\u0018\u00010\u000b\u00a2\u0006\u0002\b\u000e0\u000b\u00a2\u0006\u0002\b\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lnet/jami/contactrequests/ContactRequestsPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/contactrequests/ContactRequestsView;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mAccountService", "Lnet/jami/services/AccountService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/ConversationFacade;Lnet/jami/services/AccountService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccount", "Lio/reactivex/rxjava3/subjects/BehaviorSubject;", "Lnet/jami/model/Account;", "kotlin.jvm.PlatformType", "Lio/reactivex/rxjava3/annotations/NonNull;", "bindView", "", "view", "contactRequestClicked", "accountId", "", "uri", "Lnet/jami/model/Uri;", "onDestroy", "updateAccount", "Companion", "libringclient"})
public final class ContactRequestsPresenter extends net.jami.mvp.RootPresenter<net.jami.contactrequests.ContactRequestsView> {
    private final net.jami.services.ConversationFacade mConversationFacade = null;
    private final net.jami.services.AccountService mAccountService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private final io.reactivex.rxjava3.subjects.BehaviorSubject<net.jami.model.Account> mAccount = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.contactrequests.ContactRequestsPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public ContactRequestsPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.contactrequests.ContactRequestsView view) {
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    public final void updateAccount(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
    }
    
    public final void contactRequestClicked(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/contactrequests/ContactRequestsPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}