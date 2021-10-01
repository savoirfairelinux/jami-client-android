package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0007\u0018\u0000 \u00192\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u0001:\u0001\u0019B!\b\u0007\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0001\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u0012\u001a\u00020\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002H\u0016J\u0006\u0010\u0015\u001a\u00020\u0013J\u0006\u0010\u0016\u001a\u00020\u0013J\u000e\u0010\u0017\u001a\u00020\u00132\u0006\u0010\u0018\u001a\u00020\u0003R$\u0010\r\u001a\u00020\f2\u0006\u0010\u000b\u001a\u00020\f8F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lnet/jami/settings/SettingsPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/mvp/GenericView;", "Lnet/jami/model/Settings;", "mPreferencesService", "Lnet/jami/services/PreferencesService;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mUiScheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "(Lnet/jami/services/PreferencesService;Lnet/jami/services/ConversationFacade;Lio/reactivex/rxjava3/core/Scheduler;)V", "isChecked", "", "darkMode", "getDarkMode", "()Z", "setDarkMode", "(Z)V", "bindView", "", "view", "clearHistory", "loadSettings", "saveSettings", "settings", "Companion", "libringclient"})
public final class SettingsPresenter extends net.jami.mvp.RootPresenter<net.jami.mvp.GenericView<net.jami.model.Settings>> {
    private final net.jami.services.PreferencesService mPreferencesService = null;
    private final net.jami.services.ConversationFacade mConversationFacade = null;
    private final io.reactivex.rxjava3.core.Scheduler mUiScheduler = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.settings.SettingsPresenter.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public SettingsPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.PreferencesService mPreferencesService, @org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    @javax.inject.Named(value = "UiScheduler")
    io.reactivex.rxjava3.core.Scheduler mUiScheduler) {
        super();
    }
    
    @java.lang.Override()
    public void bindView(@org.jetbrains.annotations.NotNull()
    net.jami.mvp.GenericView<net.jami.model.Settings> view) {
    }
    
    public final void loadSettings() {
    }
    
    public final void saveSettings(@org.jetbrains.annotations.NotNull()
    net.jami.model.Settings settings) {
    }
    
    public final void clearHistory() {
    }
    
    public final boolean getDarkMode() {
        return false;
    }
    
    public final void setDarkMode(boolean isChecked) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lnet/jami/settings/SettingsPresenter$Companion;", "", "()V", "TAG", "", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}