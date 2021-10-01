package net.jami.account;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0010$\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&J\b\u0010\b\u001a\u00020\u0003H&J\b\u0010\t\u001a\u00020\u0003H&J\u0018\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH&J\u0010\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u0011H&J\u0010\u0010\u0012\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\fH&J\u0010\u0010\u0014\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\fH&J\b\u0010\u0015\u001a\u00020\u0003H&J\u0010\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\fH&J\u0010\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0013\u001a\u00020\fH&J\b\u0010\u0018\u001a\u00020\u0003H&J\u0010\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u001a\u001a\u00020\u001bH&J\u0010\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u001d\u001a\u00020\u0003H&J\b\u0010\u001e\u001a\u00020\u0003H&J\b\u0010\u001f\u001a\u00020\u0003H&J\u0010\u0010 \u001a\u00020\u00032\u0006\u0010!\u001a\u00020\fH&J\b\u0010\"\u001a\u00020\u0003H&J\b\u0010#\u001a\u00020\u0003H&J\b\u0010$\u001a\u00020\u0003H&J$\u0010%\u001a\u00020\u00032\u0012\u0010&\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\'2\u0006\u0010(\u001a\u00020\fH&J\u0018\u0010)\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&\u00a8\u0006*"}, d2 = {"Lnet/jami/account/JamiAccountSummaryView;", "", "accountChanged", "", "account", "Lnet/jami/model/Account;", "profile", "Lnet/jami/model/Profile;", "askCameraPermission", "askGalleryPermission", "deviceRevocationEnded", "device", "", "status", "", "displayCompleteArchive", "dest", "Ljava/io/File;", "goToAccount", "accountId", "goToAdvanced", "goToGallery", "goToMedia", "goToSystem", "gotToImageCapture", "passwordChangeEnded", "ok", "", "setSwitchStatus", "showExportingProgressDialog", "showGenericError", "showNetworkError", "showPIN", "pin", "showPasswordError", "showPasswordProgressDialog", "showRevokingProgressDialog", "updateDeviceList", "devices", "", "currentDeviceId", "updateUserView", "libringclient"})
public abstract interface JamiAccountSummaryView {
    
    public abstract void showExportingProgressDialog();
    
    public abstract void showPasswordProgressDialog();
    
    public abstract void accountChanged(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.Profile profile);
    
    public abstract void showNetworkError();
    
    public abstract void showPasswordError();
    
    public abstract void showGenericError();
    
    public abstract void showPIN(@org.jetbrains.annotations.NotNull()
    java.lang.String pin);
    
    public abstract void passwordChangeEnded(boolean ok);
    
    public abstract void displayCompleteArchive(@org.jetbrains.annotations.NotNull()
    java.io.File dest);
    
    public abstract void gotToImageCapture();
    
    public abstract void askCameraPermission();
    
    public abstract void goToGallery();
    
    public abstract void askGalleryPermission();
    
    public abstract void updateUserView(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account, @org.jetbrains.annotations.NotNull()
    net.jami.model.Profile profile);
    
    public abstract void goToMedia(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void goToSystem(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void goToAdvanced(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void goToAccount(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    public abstract void setSwitchStatus(@org.jetbrains.annotations.NotNull()
    net.jami.model.Account account);
    
    public abstract void showRevokingProgressDialog();
    
    public abstract void deviceRevocationEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String device, int status);
    
    public abstract void updateDeviceList(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> devices, @org.jetbrains.annotations.NotNull()
    java.lang.String currentDeviceId);
}