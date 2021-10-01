package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0003H&J\u0010\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H&J\b\u0010\b\u001a\u00020\u0003H&J\u0018\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH&J\b\u0010\u000e\u001a\u00020\u0003H&J\u0010\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u000bH&\u00a8\u0006\u0011"}, d2 = {"Lnet/jami/account/ProfileCreationView;", "", "askPhotoPermission", "", "askStoragePermission", "displayProfileName", "profileName", "", "goToGallery", "goToNext", "accountCreationModel", "Lnet/jami/model/AccountCreationModel;", "saveProfile", "", "goToPhotoCapture", "setProfile", "model", "libringclient"})
public abstract interface ProfileCreationView {
    
    public abstract void displayProfileName(@org.jetbrains.annotations.NotNull()
    java.lang.String profileName);
    
    public abstract void goToGallery();
    
    public abstract void goToPhotoCapture();
    
    public abstract void askStoragePermission();
    
    public abstract void askPhotoPermission();
    
    public abstract void goToNext(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel accountCreationModel, boolean saveProfile);
    
    public abstract void setProfile(@org.jetbrains.annotations.NotNull()
    net.jami.model.AccountCreationModel model);
}