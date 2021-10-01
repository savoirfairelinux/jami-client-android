package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0012\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0011\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0000\u00a2\u0006\u0002\u0010\u0003J\u000e\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u0005J\u000e\u0010 \u001a\u00020\u001e2\u0006\u0010!\u001a\u00020\u0005R\u001e\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0004\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u001a\u0010\b\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\u0007\"\u0004\b\t\u0010\nR\u001a\u0010\u000b\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\u0007\"\u0004\b\f\u0010\nR\u001a\u0010\r\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u0007\"\u0004\b\u000e\u0010\nR\u001a\u0010\u000f\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0007\"\u0004\b\u0010\u0010\nR\u001a\u0010\u0011\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0007\"\u0004\b\u0012\u0010\nR\u001a\u0010\u0013\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0007\"\u0004\b\u0014\u0010\nR\u001e\u0010\u0015\u001a\u00020\u00052\u0006\u0010\u0004\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0007R\u000e\u0010\u0016\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0017\u001a\u00020\u0018X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\u001a\"\u0004\b\u001b\u0010\u001c\u00a8\u0006\""}, d2 = {"Lnet/jami/model/Settings;", "", "s", "(Lnet/jami/model/Settings;)V", "<set-?>", "", "isAllowOnStartup", "()Z", "isAllowPersistentNotification", "setAllowPersistentNotification", "(Z)V", "isAllowPlaceSystemCalls", "setAllowPlaceSystemCalls", "isAllowPushNotifications", "setAllowPushNotifications", "isAllowReadIndicator", "setAllowReadIndicator", "isAllowSystemContacts", "setAllowSystemContacts", "isAllowTypingIndicator", "setAllowTypingIndicator", "isRecordingBlocked", "mHwEncoding", "notificationVisibility", "", "getNotificationVisibility", "()I", "setNotificationVisibility", "(I)V", "setAllowRingOnStartup", "", "allowRingOnStartup", "setBlockRecordIndicator", "checked", "libringclient"})
public final class Settings {
    private boolean isAllowPushNotifications = false;
    private boolean isAllowPersistentNotification = false;
    private boolean isAllowSystemContacts = false;
    private boolean isAllowPlaceSystemCalls = false;
    private boolean isAllowOnStartup = false;
    private boolean isAllowTypingIndicator = false;
    private boolean isAllowReadIndicator = false;
    private boolean isRecordingBlocked = false;
    private boolean mHwEncoding = false;
    private int notificationVisibility = 0;
    
    public Settings() {
        super();
    }
    
    public Settings(@org.jetbrains.annotations.Nullable()
    net.jami.model.Settings s) {
        super();
    }
    
    public final boolean isAllowPushNotifications() {
        return false;
    }
    
    public final void setAllowPushNotifications(boolean p0) {
    }
    
    public final boolean isAllowPersistentNotification() {
        return false;
    }
    
    public final void setAllowPersistentNotification(boolean p0) {
    }
    
    public final boolean isAllowSystemContacts() {
        return false;
    }
    
    public final void setAllowSystemContacts(boolean p0) {
    }
    
    public final boolean isAllowPlaceSystemCalls() {
        return false;
    }
    
    public final void setAllowPlaceSystemCalls(boolean p0) {
    }
    
    public final boolean isAllowOnStartup() {
        return false;
    }
    
    public final boolean isAllowTypingIndicator() {
        return false;
    }
    
    public final void setAllowTypingIndicator(boolean p0) {
    }
    
    public final boolean isAllowReadIndicator() {
        return false;
    }
    
    public final void setAllowReadIndicator(boolean p0) {
    }
    
    public final boolean isRecordingBlocked() {
        return false;
    }
    
    public final int getNotificationVisibility() {
        return 0;
    }
    
    public final void setNotificationVisibility(int p0) {
    }
    
    public final void setAllowRingOnStartup(boolean allowRingOnStartup) {
    }
    
    public final void setBlockRecordIndicator(boolean checked) {
    }
}