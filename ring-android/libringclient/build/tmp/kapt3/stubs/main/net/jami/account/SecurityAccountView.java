package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H&J\b\u0010\u0007\u001a\u00020\u0003H&J#\u0010\b\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fH&\u00a2\u0006\u0002\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lnet/jami/account/SecurityAccountView;", "", "addAllCredentials", "", "credentials", "", "Lnet/jami/model/AccountCredentials;", "removeAllCredentials", "setDetails", "config", "Lnet/jami/model/AccountConfig;", "tlsMethods", "", "", "(Lnet/jami/model/AccountConfig;[Ljava/lang/String;)V", "libringclient"})
public abstract interface SecurityAccountView {
    
    public abstract void removeAllCredentials();
    
    public abstract void addAllCredentials(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.AccountCredentials> credentials);
    
    public abstract void setDetails(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountConfig config, @org.jetbrains.annotations.NotNull()
    java.lang.String[] tlsMethods);
}