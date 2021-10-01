package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010$\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u0000 \u001d2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u001dB!\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0001\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tJ\u0014\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\rH\u0002J\u001c\u0010\u000f\u001a\u00020\u00102\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u000e0\u0012H\u0002J\u0006\u0010\u0013\u001a\u00020\u0010J\u0010\u0010\u0014\u001a\u00020\u00102\u0006\u0010\u0015\u001a\u00020\u000eH\u0002J6\u0010\u0016\u001a\u00020\u00102\b\u0010\u0017\u001a\u0004\u0018\u00010\u000e2\b\u0010\u0018\u001a\u0004\u0018\u00010\u000e2\b\u0010\u0019\u001a\u0004\u0018\u00010\u000e2\b\u0010\u001a\u001a\u0004\u0018\u00010\u000e2\u0006\u0010\u001b\u001a\u00020\u001cR\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lnet/jami/account/SIPCreationPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/SIPCreationView;", "mAccountService", "Lnet/jami/services/AccountService;", "mDeviceService", "Lnet/jami/services/DeviceRuntimeService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/DeviceRuntimeService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccount", "Lnet/jami/model/Account;", "initAccountDetails", "", "", "registerAccount", "", "accountDetails", "", "removeAccount", "saveProfile", "accountID", "startCreation", "hostname", "proxy", "username", "password", "bypassWarning", "", "Companion", "libringclient"})
public final class SIPCreationPresenter extends net.jami.mvp.RootPresenter<net.jami.account.SIPCreationView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceService = null;
    private io.reactivex.rxjava3.core.Scheduler mUiScheduler;
    private net.jami.model.Account mAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.account.SIPCreationPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public SIPCreationPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    /**
     * Attempts to register the account specified by the form. If there are form errors (invalid or missing fields, etc.), the
     * errors are presented and no actual creation attempt is made.
     *
     * @param hostname      hostname account value
     * @param username      username account value
     * @param password      password account value
     * @param bypassWarning Report eventual warning to the user
     */
    public final void startCreation(@org.jetbrains.annotations.Nullable()
    java.lang.String hostname, @org.jetbrains.annotations.Nullable()
    java.lang.String proxy, @org.jetbrains.annotations.Nullable()
    java.lang.String username, @org.jetbrains.annotations.Nullable()
    java.lang.String password, boolean bypassWarning) {
    }
    
    public final void removeAccount() {
    }
    
    private final void registerAccount(java.util.Map<java.lang.String, java.lang.String> accountDetails) {
    }
    
    private final java.util.Map<java.lang.String, java.lang.String> initAccountDetails() {
        return null;
    }
    
    private final void saveProfile(java.lang.String accountID) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/account/SIPCreationPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}