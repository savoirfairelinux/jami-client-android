package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\b\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0003H&J\b\u0010\u0005\u001a\u00020\u0003H&J\b\u0010\u0006\u001a\u00020\u0003H&J\b\u0010\u0007\u001a\u00020\u0003H&J\b\u0010\b\u001a\u00020\u0003H&J\b\u0010\t\u001a\u00020\u0003H&J\b\u0010\n\u001a\u00020\u0003H&\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/account/SIPCreationView;", "", "resetErrors", "", "showIP2IPWarning", "showLoading", "showPasswordError", "showRegistrationError", "showRegistrationNetworkError", "showRegistrationSuccess", "showUsernameError", "libringclient"})
public abstract interface SIPCreationView {
    
    public abstract void showUsernameError();
    
    public abstract void showLoading();
    
    public abstract void resetErrors();
    
    public abstract void showPasswordError();
    
    public abstract void showIP2IPWarning();
    
    public abstract void showRegistrationNetworkError();
    
    public abstract void showRegistrationError();
    
    public abstract void showRegistrationSuccess();
}