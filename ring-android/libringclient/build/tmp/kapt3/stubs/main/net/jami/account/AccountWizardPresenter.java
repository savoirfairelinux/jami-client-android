package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010%\n\u0002\b\u0003\u0018\u0000 -2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001-B)\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\b\u0001\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ*\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0018\u0010\u0018\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\u001a0\u0019H\u0002J*\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00140\u00132\u0006\u0010\u001c\u001a\u00020\r2\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\u001aH\u0002J\u0006\u0010\u001e\u001a\u00020\u0016J\u000e\u0010\u001f\u001a\u00020\u00162\u0006\u0010 \u001a\u00020\u000fJ\u001a\u0010!\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\"0\u0019H\u0002J\u0016\u0010#\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u000fJ\u0016\u0010%\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u000fJ\u0016\u0010&\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u000fJ\"\u0010\'\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\"0\u00192\u0006\u0010$\u001a\u00020\u000fH\u0002J\u0016\u0010(\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0006\u0010)\u001a\u00020\u0011J$\u0010*\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\r2\u0012\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0+H\u0002J\u0006\u0010,\u001a\u00020\u0016R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n\u0012\u0004\u0012\u00020\u0014\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lnet/jami/account/AccountWizardPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/AccountWizardView;", "mAccountService", "Lnet/jami/services/AccountService;", "mPreferences", "Lnet/jami/services/PreferencesService;", "mDeviceService", "Lnet/jami/services/DeviceRuntimeService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/PreferencesService;Lnet/jami/services/DeviceRuntimeService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccountCreationModel", "Lnet/jami/model/AccountCreationModel;", "mAccountType", "", "mCreatingAccount", "", "newAccount", "Lio/reactivex/rxjava3/core/Observable;", "Lnet/jami/model/Account;", "createAccount", "", "accountCreationModel", "details", "Lio/reactivex/rxjava3/core/Single;", "", "createNewAccount", "model", "accountDetails", "errorDialogClosed", "init", "accountType", "initAccountDetails", "Ljava/util/HashMap;", "initJamiAccountConnect", "defaultAccountName", "initJamiAccountCreation", "initJamiAccountLink", "initRingAccountDetails", "profileCreated", "saveProfile", "setProxyDetails", "", "successDialogClosed", "Companion", "libringclient"})
public final class AccountWizardPresenter extends net.jami.mvp.RootPresenter<net.jami.account.AccountWizardView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.PreferencesService mPreferences = null;
    private final net.jami.services.DeviceRuntimeService mDeviceService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private boolean mCreatingAccount = false;
    private java.lang.String mAccountType;
    private net.jami.model.AccountCreationModel mAccountCreationModel;
    private io.reactivex.rxjava3.core.Observable<net.jami.model.Account> newAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.account.AccountWizardPresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public AccountWizardPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferences, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    java.lang.String accountType) {
    }
    
    private final void setProxyDetails(net.jami.model.AccountCreationModel accountCreationModel, java.util.Map<java.lang.String, java.lang.String> details) {
    }
    
    public final void initJamiAccountConnect(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel, @org.jetbrains.annotations.NotNull()
    java.lang.String defaultAccountName) {
    }
    
    public final void initJamiAccountCreation(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel, @org.jetbrains.annotations.NotNull()
    java.lang.String defaultAccountName) {
    }
    
    public final void initJamiAccountLink(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel, @org.jetbrains.annotations.NotNull()
    java.lang.String defaultAccountName) {
    }
    
    private final void createAccount(net.jami.model.AccountCreationModel accountCreationModel, io.reactivex.rxjava3.core.Single<java.util.Map<java.lang.String, java.lang.String>> details) {
    }
    
    public final void successDialogClosed() {
    }
    
    private final io.reactivex.rxjava3.core.Single<java.util.HashMap<java.lang.String, java.lang.String>> initRingAccountDetails(java.lang.String defaultAccountName) {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Single<java.util.HashMap<java.lang.String, java.lang.String>> initAccountDetails() {
        return null;
    }
    
    private final io.reactivex.rxjava3.core.Observable<net.jami.model.Account> createNewAccount(net.jami.model.AccountCreationModel model, java.util.Map<java.lang.String, java.lang.String> accountDetails) {
        return null;
    }
    
    public final void profileCreated(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel, boolean saveProfile) {
    }
    
    public final void errorDialogClosed() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/account/AccountWizardPresenter$Companion;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "libringclient"})
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