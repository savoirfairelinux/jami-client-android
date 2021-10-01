package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\u0018\u0000 \u001d2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u001dB!\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0001\u0010\u0007\u001a\u00020\b\u00a2\u0006\u0002\u0010\tJ\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u0013J\u0006\u0010\u0014\u001a\u00020\rJ\u000e\u0010\u0015\u001a\u00020\r2\u0006\u0010\u0016\u001a\u00020\u000bJ\u0006\u0010\u0017\u001a\u00020\rJ\u0014\u0010\u0018\u001a\u00020\r2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aJ\u0006\u0010\u001c\u001a\u00020\rR\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lnet/jami/account/ProfileCreationPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/ProfileCreationView;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/HardwareService;Lio/reactivex/rxjava3/core/Scheduler;)V", "mAccountCreationModel", "Lnet/jami/model/AccountCreationModel;", "cameraClick", "", "cameraPermissionChanged", "isGranted", "", "fullNameUpdated", "fullName", "", "galleryClick", "initPresenter", "accountCreationModel", "nextClick", "photoUpdated", "bitmap", "Lio/reactivex/rxjava3/core/Single;", "", "skipClick", "Companion", "libringclient"})
public final class ProfileCreationPresenter extends net.jami.mvp.RootPresenter<net.jami.account.ProfileCreationView> {
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    private net.jami.model.AccountCreationModel mAccountCreationModel;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.account.ProfileCreationPresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public ProfileCreationPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    public final void initPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel) {
    }
    
    public final void fullNameUpdated(@org.jetbrains.annotations.NotNull()
    java.lang.String fullName) {
    }
    
    public final void photoUpdated(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Single<java.lang.Object> bitmap) {
    }
    
    public final void galleryClick() {
    }
    
    public final void cameraClick() {
    }
    
    public final void cameraPermissionChanged(boolean isGranted) {
    }
    
    public final void nextClick() {
    }
    
    public final void skipClick() {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/account/ProfileCreationPresenter$Companion;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "libringclient"})
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