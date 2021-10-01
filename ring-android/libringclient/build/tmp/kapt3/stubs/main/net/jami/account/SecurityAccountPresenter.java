package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0000\n\u0000\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005J\u001a\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u000bJ\u0018\u0010\r\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u000bJ\u001a\u0010\u000e\u001a\u00020\t2\b\u0010\u000f\u001a\u0004\u0018\u00010\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012J\u000e\u0010\u0013\u001a\u00020\t2\u0006\u0010\u0014\u001a\u00020\u0012J\u001a\u0010\u0015\u001a\u00020\t2\b\u0010\u000f\u001a\u0004\u0018\u00010\u00102\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lnet/jami/account/SecurityAccountPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/SecurityAccountView;", "mAccountService", "Lnet/jami/services/AccountService;", "(Lnet/jami/services/AccountService;)V", "mAccount", "Lnet/jami/model/Account;", "credentialAdded", "", "old", "Lnet/jami/model/AccountCredentials;", "newCreds", "credentialEdited", "fileActivityResult", "key", "Lnet/jami/model/ConfigKey;", "filePath", "", "init", "accountId", "tlsChanged", "newValue", "", "libringclient"})
public final class SecurityAccountPresenter extends net.jami.mvp.RootPresenter<net.jami.account.SecurityAccountView> {
    private final net.jami.services.AccountService mAccountService = null;
    private net.jami.model.Account mAccount;
    
    @javax.inject.Inject()
    public SecurityAccountPresenter(@org.jetbrains.annotations.NotNull()
    net.jami.services.AccountService mAccountService) {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
    }
    
    public final void credentialEdited(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCredentials old, @org.jetbrains.annotations.Nullable()
    net.jami.model.AccountCredentials newCreds) {
    }
    
    public final void credentialAdded(@org.jetbrains.annotations.Nullable()
    net.jami.model.AccountCredentials old, @org.jetbrains.annotations.Nullable()
    net.jami.model.AccountCredentials newCreds) {
    }
    
    public final void tlsChanged(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConfigKey key, @org.jetbrains.annotations.Nullable()
    java.lang.Object newValue) {
    }
    
    public final void fileActivityResult(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConfigKey key, @org.jetbrains.annotations.Nullable()
    java.lang.String filePath) {
    }
}