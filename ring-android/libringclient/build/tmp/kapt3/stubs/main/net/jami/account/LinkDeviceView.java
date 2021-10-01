package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0012\u0010\u0002\u001a\u00020\u00032\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005H&J\b\u0010\u0006\u001a\u00020\u0003H&J\b\u0010\u0007\u001a\u00020\u0003H&J\b\u0010\b\u001a\u00020\u0003H&J\b\u0010\t\u001a\u00020\u0003H&J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\fH&J\b\u0010\r\u001a\u00020\u0003H&\u00a8\u0006\u000e"}, d2 = {"Lnet/jami/account/LinkDeviceView;", "", "accountChanged", "", "account", "Lnet/jami/model/Account;", "dismissExportingProgress", "showExportingProgress", "showGenericError", "showNetworkError", "showPIN", "pin", "", "showPasswordError", "libringclient"})
public abstract interface LinkDeviceView {
    
    public abstract void showExportingProgress();
    
    public abstract void dismissExportingProgress();
    
    public abstract void accountChanged(@org.jetbrains.annotations.Nullable()
    net.jami.model.Account account);
    
    public abstract void showNetworkError();
    
    public abstract void showPasswordError();
    
    public abstract void showGenericError();
    
    public abstract void showPIN(@org.jetbrains.annotations.NotNull()
    java.lang.String pin);
}