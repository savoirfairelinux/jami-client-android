package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u0000 52\b\u0012\u0004\u0012\u00020\u00020\u0001:\u00015B)\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\b\u0001\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\u0006\u0010\u0015\u001a\u00020\u0016J\u0016\u0010\u0017\u001a\u00020\u00162\u0006\u0010\u0018\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u0011J\u0018\u0010\u001a\u001a\u00020\u00162\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0011J\u000e\u0010\u001e\u001a\u00020\u00162\u0006\u0010\u001f\u001a\u00020 J\u0006\u0010!\u001a\u00020\u0016J\u0006\u0010\"\u001a\u00020\u0016J\u0006\u0010#\u001a\u00020\u0016J\u0006\u0010$\u001a\u00020\u0016J\u0006\u0010%\u001a\u00020\u0016J\u001a\u0010&\u001a\u00020\u00162\b\u0010\'\u001a\u0004\u0018\u00010\u00112\b\u0010\u001d\u001a\u0004\u0018\u00010\u0011J\u000e\u0010(\u001a\u00020\u00162\u0006\u0010)\u001a\u00020\u0011J\u001a\u0010*\u001a\u00020\u00162\b\u0010+\u001a\u0004\u0018\u00010\u00112\b\u0010\u001d\u001a\u0004\u0018\u00010\u0011J\u001e\u0010,\u001a\u00020\u00162\b\u0010-\u001a\u0004\u0018\u00010\u00112\f\u0010.\u001a\b\u0012\u0004\u0012\u0002000/J\u0010\u00101\u001a\u00020\u00162\b\u0010-\u001a\u0004\u0018\u00010\u0011J\u000e\u00102\u001a\u00020\u00162\u0006\u00103\u001a\u00020\u0011J\u0010\u00104\u001a\u00020\u00162\b\u0010\u001d\u001a\u0004\u0018\u00010\u0011R\u0013\u0010\f\u001a\u0004\u0018\u00010\r8F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u00118F\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0013R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00066"}, d2 = {"Lnet/jami/account/JamiAccountSummaryPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/JamiAccountSummaryView;", "mAccountService", "Lnet/jami/services/AccountService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mVcardService", "Lnet/jami/services/VCardService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/VCardService;Lio/reactivex/rxjava3/core/Scheduler;)V", "account", "Lnet/jami/model/Account;", "getAccount", "()Lnet/jami/model/Account;", "deviceName", "", "getDeviceName", "()Ljava/lang/String;", "mAccountID", "cameraClicked", "", "changePassword", "oldPassword", "newPassword", "downloadAccountsArchive", "dest", "Ljava/io/File;", "password", "enableAccount", "newValue", "", "galleryClicked", "goToAccount", "goToAdvanced", "goToMedia", "goToSystem", "registerName", "name", "renameDevice", "newName", "revokeDevice", "deviceId", "saveVCard", "username", "photo", "Lio/reactivex/rxjava3/core/Single;", "Lezvcard/property/Photo;", "saveVCardFormattedName", "setAccountId", "accountId", "startAccountExport", "Companion", "libringclient"})
public final class JamiAccountSummaryPresenter extends net.jami.mvp.RootPresenter<net.jami.account.JamiAccountSummaryView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.VCardService mVcardService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private java.lang.String mAccountID;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.account.JamiAccountSummaryPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public JamiAccountSummaryPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.VCardService mVcardService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final void registerName(@org.jetbrains.annotations.Nullable()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String password) {
    }
    
    public final void startAccountExport(@org.jetbrains.annotations.Nullable()
    java.lang.String password) {
    }
    
    public final void setAccountId(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    public final void enableAccount(boolean newValue) {
    }
    
    public final void changePassword(@org.jetbrains.annotations.NotNull()
    java.lang.String oldPassword, @org.jetbrains.annotations.NotNull()
    java.lang.String newPassword) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDeviceName() {
        return null;
    }
    
    public final void downloadAccountsArchive(@org.jetbrains.annotations.NotNull()
    java.io.File dest, @org.jetbrains.annotations.Nullable()
    java.lang.String password) {
    }
    
    public final void saveVCardFormattedName(@org.jetbrains.annotations.Nullable()
    java.lang.String username) {
    }
    
    public final void saveVCard(@org.jetbrains.annotations.Nullable()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Single<ezvcard.property.Photo> photo) {
    }
    
    public final void cameraClicked() {
    }
    
    public final void galleryClicked() {
    }
    
    public final void goToAccount() {
    }
    
    public final void goToMedia() {
    }
    
    public final void goToSystem() {
    }
    
    public final void goToAdvanced() {
    }
    
    public final void revokeDevice(@org.jetbrains.annotations.Nullable()
    java.lang.String deviceId, @org.jetbrains.annotations.Nullable()
    java.lang.String password) {
    }
    
    public final void renameDevice(@org.jetbrains.annotations.NotNull()
    java.lang.String newName) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Account getAccount() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/account/JamiAccountSummaryPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}