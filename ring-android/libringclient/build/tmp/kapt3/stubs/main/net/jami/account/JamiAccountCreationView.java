package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001:\u0001\u0010J\b\u0010\u0002\u001a\u00020\u0003H&J\u0010\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0010\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH&J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\u0006H&J\u0010\u0010\f\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\u0006H&J\u0010\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\u000fH&\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/account/JamiAccountCreationView;", "", "cancel", "", "enableNextButton", "enabled", "", "goToAccountCreation", "accountCreationModel", "Lnet/jami/model/AccountCreationModel;", "showInvalidPasswordError", "display", "showNonMatchingPasswordError", "updateUsernameAvailability", "status", "Lnet/jami/account/JamiAccountCreationView$UsernameAvailabilityStatus;", "UsernameAvailabilityStatus", "libringclient"})
public abstract interface JamiAccountCreationView {
    
    public abstract void updateUsernameAvailability(@org.jetbrains.annotations.NotNull()
    net.jami.account.JamiAccountCreationView.UsernameAvailabilityStatus status);
    
    public abstract void showInvalidPasswordError(boolean display);
    
    public abstract void showNonMatchingPasswordError(boolean display);
    
    public abstract void enableNextButton(boolean enabled);
    
    public abstract void goToAccountCreation(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel);
    
    public abstract void cancel();
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lnet/jami/account/JamiAccountCreationView$UsernameAvailabilityStatus;", "", "(Ljava/lang/String;I)V", "ERROR_USERNAME_TAKEN", "ERROR_USERNAME_INVALID", "ERROR", "LOADING", "AVAILABLE", "RESET", "libringclient"})
    public static enum UsernameAvailabilityStatus {
        /*public static final*/ ERROR_USERNAME_TAKEN /* = new ERROR_USERNAME_TAKEN() */,
        /*public static final*/ ERROR_USERNAME_INVALID /* = new ERROR_USERNAME_INVALID() */,
        /*public static final*/ ERROR /* = new ERROR() */,
        /*public static final*/ LOADING /* = new LOADING() */,
        /*public static final*/ AVAILABLE /* = new AVAILABLE() */,
        /*public static final*/ RESET /* = new RESET() */;
        
        UsernameAvailabilityStatus() {
        }
    }
}