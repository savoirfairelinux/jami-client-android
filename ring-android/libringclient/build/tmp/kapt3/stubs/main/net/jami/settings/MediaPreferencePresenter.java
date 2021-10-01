package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\u0018\u0000 \u001a2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u001aB!\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0001\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tJ\u0014\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fJ\u000e\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u0013J\b\u0010\u0014\u001a\u00020\rH\u0002J\u0016\u0010\u0015\u001a\u00020\r2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lnet/jami/settings/MediaPreferencePresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/settings/MediaPreferenceView;", "mAccountService", "Lnet/jami/services/AccountService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/DeviceRuntimeService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccount", "Lnet/jami/model/Account;", "codecChanged", "", "codecs", "Ljava/util/ArrayList;", "", "init", "accountId", "", "updateAccount", "videoPreferenceChanged", "key", "Lnet/jami/model/ConfigKey;", "newValue", "", "Companion", "libringclient"})
public final class MediaPreferencePresenter extends net.jami.mvp.RootPresenter<net.jami.settings.MediaPreferenceView> {
    private net.jami.services.AccountService mAccountService;
    private net.jami.services.DeviceRuntimeService mDeviceRuntimeService;
    private io.reactivex.rxjava3.core.Scheduler mUiScheduler;
    private net.jami.model.Account mAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.settings.MediaPreferencePresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public MediaPreferencePresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    public final void codecChanged(@org.jetbrains.annotations.NotNull()
    java.util.ArrayList<java.lang.Long> codecs) {
    }
    
    public final void videoPreferenceChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey key, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    private final void updateAccount() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/settings/MediaPreferencePresenter$Companion;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTAG() {
            return null;
        }
    }
}