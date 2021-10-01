package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0003J\u0010\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\bJ\u0006\u0010\f\u001a\u00020\nJ\u000e\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0010\u001a\u00020\n2\u0006\u0010\u0011\u001a\u00020\u000fJ\b\u0010\u0012\u001a\u00020\nH\u0002R\u0014\u0010\u0004\u001a\u00020\u00058BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0004\u0010\u0006R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lnet/jami/account/JamiLinkAccountPresenter;", "Lnet/jami/mvp/RootPresenter;", "Lnet/jami/account/JamiLinkAccountView;", "()V", "isFormValid", "", "()Z", "mAccountCreationModel", "Lnet/jami/model/AccountCreationModel;", "init", "", "accountCreationModel", "linkClicked", "passwordChanged", "password", "", "pinChanged", "pin", "showHideLinkButton", "libringclient"})
public final class JamiLinkAccountPresenter extends net.jami.mvp.RootPresenter<net.jami.account.JamiLinkAccountView> {
    private net.jami.model.AccountCreationModel mAccountCreationModel;
    
    @javax.inject.Inject()
    public JamiLinkAccountPresenter() {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.Nullable()
    net.jami.model.AccountCreationModel accountCreationModel) {
    }
    
    public final void passwordChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void pinChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
    }
    
    public final void linkClicked() {
    }
    
    private final void showHideLinkButton() {
    }
    
    private final boolean isFormValid() {
        return false;
    }
}