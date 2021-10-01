package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0003H&J\b\u0010\u0005\u001a\u00020\u0003H&J\b\u0010\u0006\u001a\u00020\u0003H&J\b\u0010\u0007\u001a\u00020\u0003H&J\u0010\u0010\b\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\nH&J\b\u0010\u000b\u001a\u00020\u0003H&J\u0010\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\nH&J\b\u0010\u000e\u001a\u00020\u0003H&J\u0010\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u0011H&J\b\u0010\u0012\u001a\u00020\u0003H&J\u001e\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u00142\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0010\u001a\u00020\u0011H&\u00a8\u0006\u0018"}, d2 = {"Lnet/jami/account/AccountWizardView;", "", "blockOrientation", "", "displayCannotBeFoundError", "displayCreationError", "displayGenericError", "displayNetworkError", "displayProgress", "display", "", "displaySuccessDialog", "finish", "affinity", "goToHomeCreation", "goToProfileCreation", "accountCreationModel", "Lnet/jami/model/AccountCreationModel;", "goToSipCreation", "saveProfile", "Lio/reactivex/rxjava3/core/Single;", "Lezvcard/VCard;", "account", "Lnet/jami/model/Account;", "libringclient"})
public abstract interface AccountWizardView {
    
    public abstract void goToHomeCreation();
    
    public abstract void goToSipCreation();
    
    public abstract void displayProgress(boolean display);
    
    public abstract void displayCreationError();
    
    public abstract void blockOrientation();
    
    public abstract void finish(boolean affinity);
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.reactivex.rxjava3.core.Single<ezvcard.VCard> saveProfile(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel);
    
    public abstract void displayGenericError();
    
    public abstract void displayNetworkError();
    
    public abstract void displayCannotBeFoundError();
    
    public abstract void displaySuccessDialog();
    
    public abstract void goToProfileCreation(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel);
}