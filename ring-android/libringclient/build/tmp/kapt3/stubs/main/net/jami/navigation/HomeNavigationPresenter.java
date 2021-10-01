package net.jami.navigation;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\r\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 #2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001#B1\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\b\b\u0001\u0010\u000b\u001a\u00020\f\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0002H\u0016J\u0006\u0010\u0011\u001a\u00020\u000fJ\u000e\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014J\u0006\u0010\u0015\u001a\u00020\u000fJ\u0018\u0010\u0016\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bJ&\u0010\u001c\u001a\u00020\u000f2\u0006\u0010\u0018\u001a\u00020\u00192\b\u0010\u001d\u001a\u0004\u0018\u00010\u00172\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020 0\u001fJ\u0010\u0010!\u001a\u00020\u000f2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0017J\u0014\u0010\"\u001a\u00020\u000f2\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020 0\u001fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006$"}, d2 = {"Lnet/jami/navigation/HomeNavigationPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/navigation/HomeNavigationView;", "mAccountService", "Lnet/jami/services/AccountService;", "mHardwareService", "Lnet/jami/services/HardwareService;", "mDeviceRuntimeService", "Lnet/jami/services/DeviceRuntimeService;", "mVCardService", "Lnet/jami/services/VCardService;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/AccountService;Lnet/jami/services/HardwareService;Lnet/jami/services/DeviceRuntimeService;Lnet/jami/services/VCardService;Lio/reactivex/rxjava3/core/Scheduler;)V", "bindView", "", "view", "cameraClicked", "cameraPermissionChanged", "isGranted", "", "galleryClicked", "getUri", "", "account", "Lnet/jami/model/Account;", "defaultNameSip", "", "saveVCard", "username", "photo", "Lio/reactivex/rxjava3/core/Single;", "Lezvcard/property/Photo;", "saveVCardFormattedName", "saveVCardPhoto", "Companion", "libringclient"})
public final class HomeNavigationPresenter extends net.jami.mvp.RootPresenter<net.jami.navigation.HomeNavigationView> {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.HardwareService mHardwareService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService = null;
    private final net.jami.services.VCardService mVCardService = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.navigation.HomeNavigationPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public HomeNavigationPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService mHardwareService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceRuntimeService, @org.jetbrains.annotations.NotNull()
    net.jami.services.VCardService mVCardService, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.navigation.HomeNavigationView view) {
    }
    
    public final void saveVCardPhoto(@org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Single<ezvcard.property.Photo> photo) {
    }
    
    public final void saveVCardFormattedName(@org.jetbrains.annotations.Nullable()
    java.lang.String username) {
    }
    
    public final void saveVCard(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.Nullable()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    io.reactivex.rxjava3.core.Single<ezvcard.property.Photo> photo) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUri(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    java.lang.CharSequence defaultNameSip) {
        return null;
    }
    
    public final void cameraClicked() {
    }
    
    public final void galleryClicked() {
    }
    
    public final void cameraPermissionChanged(boolean isGranted) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/navigation/HomeNavigationPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}