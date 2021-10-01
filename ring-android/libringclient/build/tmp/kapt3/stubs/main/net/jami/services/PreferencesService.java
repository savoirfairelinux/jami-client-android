package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\"\n\u0002\b\u0006\b&\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010$\u001a\u00020\b2\u0006\u0010%\u001a\u00020&H&J\b\u0010\'\u001a\u00020\fH&J\b\u0010(\u001a\u00020)H&J\u0016\u0010*\u001a\b\u0012\u0004\u0012\u00020&0+2\u0006\u0010%\u001a\u00020&H&J\b\u0010,\u001a\u00020\u0015H$J\u0018\u0010-\u001a\u00020)2\u0006\u0010%\u001a\u00020&2\u0006\u0010.\u001a\u00020&H&J\u0018\u0010/\u001a\u00020)2\u0006\u0010%\u001a\u00020&2\u0006\u0010.\u001a\u00020&H&J\u0010\u00100\u001a\u00020)2\u0006\u0010\u0018\u001a\u00020\u0015H$R\u0012\u0010\u0007\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\nR\u0018\u0010\u000b\u001a\u00020\fX\u00a6\u000e\u00a2\u0006\f\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u0012\u0010\u0011\u001a\u00020\fX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0011\u0010\u000eR\u0012\u0010\u0012\u001a\u00020\fX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0016\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0017\u0010\nR$\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u00158F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\u0019\u0010\u001a\"\u0004\b\u001b\u0010\u001cR\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00150\u001e8F\u00a2\u0006\u0006\u001a\u0004\b\u001f\u0010 R\"\u0010\"\u001a\u0004\u0018\u00010\u00152\b\u0010!\u001a\u0004\u0018\u00010\u0015@BX\u0084\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001a\u00a8\u00061"}, d2 = {"Lnet/jami/services/PreferencesService;", "", "mAccountService", "Lnet/jami/services/AccountService;", "mDeviceService", "Lnet/jami/services/DeviceRuntimeService;", "(Lnet/jami/services/AccountService;Lnet/jami/services/DeviceRuntimeService;)V", "bitrate", "", "getBitrate", "()I", "darkMode", "", "getDarkMode", "()Z", "setDarkMode", "(Z)V", "isHardwareAccelerationEnabled", "isPushAllowed", "mSettingsSubject", "Lio/reactivex/rxjava3/subjects/Subject;", "Lnet/jami/model/Settings;", "resolution", "getResolution", "settings", "getSettings", "()Lnet/jami/model/Settings;", "setSettings", "(Lnet/jami/model/Settings;)V", "settingsSubject", "Lio/reactivex/rxjava3/core/Observable;", "getSettingsSubject", "()Lio/reactivex/rxjava3/core/Observable;", "<set-?>", "userSettings", "getUserSettings", "getMaxFileAutoAccept", "accountId", "", "hasNetworkConnected", "loadDarkMode", "", "loadRequestsPreferences", "", "loadSettings", "removeRequestPreferences", "contactId", "saveRequestPreferences", "saveSettings", "libringclient"})
public abstract class PreferencesService {
    private final net.jami.services.AccountService mAccountService = null;
    private final net.jami.services.DeviceRuntimeService mDeviceService = null;
    @org.jetbrains.annotations.Nullable()
    private net.jami.model.Settings userSettings;
    private final io.reactivex.rxjava3.subjects.Subject<net.jami.model.Settings> mSettingsSubject = null;
    
    public PreferencesService(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService, @org.jetbrains.annotations.NotNull()
    net.jami.services.DeviceRuntimeService mDeviceService) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    protected final net.jami.model.Settings getUserSettings() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    protected abstract net.jami.model.Settings loadSettings();
    
    protected abstract void saveSettings(@org.jetbrains.annotations.NotNull()
    net.jami.model.Settings settings);
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Settings getSettings() {
        return null;
    }
    
    public final void setSettings(@org.jetbrains.annotations.NotNull()
    net.jami.model.Settings settings) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<net.jami.model.Settings> getSettingsSubject() {
        return null;
    }
    
    public abstract boolean hasNetworkConnected();
    
    public abstract boolean isPushAllowed();
    
    public abstract void saveRequestPreferences(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactId);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.Set<java.lang.String> loadRequestsPreferences(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void removeRequestPreferences(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactId);
    
    public abstract int getResolution();
    
    public abstract int getBitrate();
    
    public abstract boolean isHardwareAccelerationEnabled();
    
    public abstract boolean getDarkMode();
    
    public abstract void setDarkMode(boolean p0);
    
    public abstract void loadDarkMode();
    
    public abstract int getMaxFileAutoAccept(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
}