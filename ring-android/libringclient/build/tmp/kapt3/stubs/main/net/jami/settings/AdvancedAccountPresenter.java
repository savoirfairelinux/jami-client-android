package net.jami.settings;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\r\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\u0018\u0000 \u001d2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u001dB\u0017\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0002J\u0010\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0010J\u0018\u0010\u0016\u001a\u00020\u00142\b\u0010\u0017\u001a\u0004\u0018\u00010\u00182\u0006\u0010\u0011\u001a\u00020\u0019J\u0016\u0010\u001a\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0011\u001a\u00020\u0019J\u0018\u0010\u001b\u001a\u00020\u00142\b\u0010\u0017\u001a\u0004\u0018\u00010\u00182\u0006\u0010\u0011\u001a\u00020\u0019J\b\u0010\u001c\u001a\u00020\u0014H\u0002R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001e"}, d2 = {"Lnet/jami/settings/AdvancedAccountPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/settings/AdvancedAccountView;", "mConversationFacade", "Lnet/jami/services/ConversationFacade;", "mAccountService", "Lnet/jami/services/AccountService;", "(Lnet/jami/services/ConversationFacade;Lnet/jami/services/AccountService;)V", "mAccount", "Lnet/jami/model/Account;", "networkInterfaces", "Ljava/util/ArrayList;", "", "getNetworkInterfaces", "()Ljava/util/ArrayList;", "adjustRtpRange", "", "newValue", "", "init", "", "accountId", "passwordPreferenceChanged", "configKey", "Lnet/jami/model/ConfigKey;", "", "preferenceChanged", "twoStatePreferenceChanged", "updateAccount", "Companion", "libringclient"})
public final class AdvancedAccountPresenter extends net.jami.mvp.RootPresenter<net.jami.settings.AdvancedAccountView> {
    private net.jami.services.ConversationFacade mConversationFacade;
    private net.jami.services.AccountService mAccountService;
    private net.jami.model.Account mAccount;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.settings.AdvancedAccountPresenter.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    
    @javax.inject.Inject()
    public AdvancedAccountPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.ConversationFacade mConversationFacade, @org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService) {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.Nullable()
    java.lang.String accountId) {
    }
    
    public final void twoStatePreferenceChanged(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    public final void passwordPreferenceChanged(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    public final void preferenceChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.ConfigKey configKey, @org.jetbrains.annotations.NotNull()
    java.lang.Object newValue) {
    }
    
    private final void updateAccount() {
    }
    
    private final java.lang.String adjustRtpRange(int newValue) {
        return null;
    }
    
    private final java.util.ArrayList<java.lang.CharSequence> getNetworkInterfaces() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/settings/AdvancedAccountPresenter$Companion;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "libringclient"})
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