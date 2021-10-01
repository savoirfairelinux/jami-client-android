package net.jami.contactrequests;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&J\u0010\u0010\b\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\nH&J\u001e\u0010\u000b\u001a\u00020\u00032\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\n0\r2\u0006\u0010\u000e\u001a\u00020\u000fH&\u00a8\u0006\u0010"}, d2 = {"Lnet/jami/contactrequests/ContactRequestsView;", "", "goToConversation", "", "accountId", "", "contactId", "Lnet/jami/model/Uri;", "updateItem", "item", "Lnet/jami/smartlist/SmartListViewModel;", "updateView", "list", "", "disposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "libringclient"})
public abstract interface ContactRequestsView {
    
    public abstract void updateView(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.smartlist.SmartListViewModel> list, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.disposables.CompositeDisposable disposable);
    
    public abstract void updateItem(@org.jetbrains.annotations.NotNull()
    net.jami.smartlist.SmartListViewModel item);
    
    public abstract void goToConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri contactId);
}