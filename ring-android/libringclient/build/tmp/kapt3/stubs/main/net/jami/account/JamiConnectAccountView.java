package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0010\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0010\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH&\u00a8\u0006\n"}, d2 = {"Lnet/jami/account/JamiConnectAccountView;", "", "cancel", "", "createAccount", "accountCreationModel", "Lnet/jami/model/AccountCreationModel;", "enableConnectButton", "enable", "", "libringclient"})
public abstract interface JamiConnectAccountView {
    
    public abstract void enableConnectButton(boolean enable);
    
    public abstract void createAccount(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel);
    
    public abstract void cancel();
}