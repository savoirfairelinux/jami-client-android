package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0010\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0010\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH&J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\tH&\u00a8\u0006\f"}, d2 = {"Lnet/jami/account/JamiLinkAccountView;", "", "cancel", "", "createAccount", "accountCreationModel", "Lnet/jami/model/AccountCreationModel;", "enableLinkButton", "enable", "", "showPin", "show", "libringclient"})
public abstract interface JamiLinkAccountView {
    
    public abstract void enableLinkButton(boolean enable);
    
    public abstract void showPin(boolean show);
    
    public abstract void createAccount(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel);
    
    public abstract void cancel();
}