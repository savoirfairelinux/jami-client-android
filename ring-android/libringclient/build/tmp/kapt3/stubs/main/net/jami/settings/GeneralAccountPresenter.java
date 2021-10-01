package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0005\u0018\u0000  2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001 B)\b\u0001\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\b\u0001\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\u0006\u0010\u000e\u001a\u00020\u000fJ\u0010\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011J\u0012\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0012\u001a\u0004\u0018\u00010\rH\u0002J\u0016\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017J\u0016\u0010\u0018\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017J\u0006\u0010\u0019\u001a\u00020\u000fJ\u000e\u0010\u001a\u001a\u00020\u000f2\u0006\u0010\u001b\u001a\u00020\u001cJ\u0016\u0010\u001d\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017J\b\u0010\u001e\u001a\u00020\u000fH\u0002J\u0016\u0010\u001f\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lnet/jami/settings/GeneralAccountPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/settings/GeneralAccountView;", "mAccountService", "Lnet/jami/services/AccountService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mPreferenceService", "Lnet/jami/services/PreferencesService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/HardwareService;Lnet/jami/services/PreferencesService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccount", "Lnet/jami/model/Account;", "init", "", "accountId", "", "account", "passwordPreferenceChanged", "configKey", "Lnet/jami/model/ConfigKey;", "newValue", "", "preferenceChanged", "removeAccount", "setEnabled", "enabled", "", "twoStatePreferenceChanged", "updateAccount", "userNameChanged", "Companion", "libringclient"})
public final class GeneralAccountPresenter extends net.jami.mvp.RootPresenter<net.jami.settings.GeneralAccountView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.PreferencesService mPreferenceService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private net.jami.model.Account mAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.settings.GeneralAccountPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public GeneralAccountPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferenceService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final void init() {
    }
    
    public final void init(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
    }
    
    private final void init(net.jami.model.Account account) {
    }
    
    public final void setEnabled(boolean enabled) {
    }
    
    public final void twoStatePreferenceChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    public final void passwordPreferenceChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    public final void userNameChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    public final void preferenceChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    private final void updateAccount() {
    }
    
    public final void removeAccount() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/settings/GeneralAccountPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}