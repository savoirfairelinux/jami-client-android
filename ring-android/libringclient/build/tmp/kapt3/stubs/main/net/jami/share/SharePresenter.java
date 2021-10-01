package net.jami.share;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u0001B\u0019\b\u0007\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\t\u001a\u00020\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002H\u0016J\u0010\u0010\f\u001a\u00020\n2\u0006\u0010\r\u001a\u00020\u0003H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lnet/jami/share/SharePresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/mvp/GenericView;", "Lnet/jami/share/ShareViewModel;", "mAccountService", "Lnet/jami/services/AccountService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lio/reactivex/rxjava3/core/Scheduler;)V", "bindView", "", "view", "loadContactInformation", "model", "libringclient"})
public final class SharePresenter extends net.jami.mvp.RootPresenter<net.jami.mvp.GenericView<net.jami.share.ShareViewModel>> {
    private final net.jami.services.AccountService mAccountService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    
    @javax.inject.Inject()
    public SharePresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.mvp.GenericView<net.jami.share.ShareViewModel> view) {
    }
    
    private final void loadContactInformation(net.jami.share.ShareViewModel model) {
    }
}