package net.jami.call;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0010\b\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0003H&J\u0010\u0010\u0007\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\b\u001a\u00020\u0005H&J\u0010\u0010\t\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0018\u0010\n\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u00052\u0006\u0010\u000b\u001a\u00020\u0005H&J\u0010\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\u000eH&J\b\u0010\u000f\u001a\u00020\u0003H&J\u0010\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u0012H&J\u0018\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0011\u001a\u00020\u0012H&J\u0018\u0010\u0015\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u0017H&J\u0010\u0010\u0018\u001a\u00020\u00032\u0006\u0010\u0019\u001a\u00020\u0005H&J\b\u0010\u001a\u001a\u00020\u0003H&J0\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u001c\u001a\u00020\u00052\u0006\u0010\u001d\u001a\u00020\u00052\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u00052\u0006\u0010 \u001a\u00020\u0005H&J\u0018\u0010!\u001a\u00020\u00032\u0006\u0010\"\u001a\u00020\u00052\u0006\u0010#\u001a\u00020\u0005H&J\b\u0010$\u001a\u00020\u0003H&J\b\u0010%\u001a\u00020\u0003H&J\u0010\u0010&\u001a\u00020\u00032\u0006\u0010\'\u001a\u00020\u0005H&J \u0010(\u001a\u00020\u00032\u0006\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020*2\u0006\u0010,\u001a\u00020*H&J \u0010-\u001a\u00020\u00032\u0006\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020*2\u0006\u0010,\u001a\u00020*H&J\u0018\u0010.\u001a\u00020\u00032\u0006\u0010/\u001a\u00020*2\u0006\u00100\u001a\u00020*H&J\u0010\u00101\u001a\u00020\u00032\u0006\u00102\u001a\u00020\u000eH&J\u0010\u00103\u001a\u00020\u00032\u0006\u00104\u001a\u00020\u0005H&J \u00105\u001a\u00020\u00032\u0006\u00106\u001a\u00020\u000e2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u00107\u001a\u00020\u0005H&J\u0010\u00108\u001a\u00020\u00032\u0006\u00109\u001a\u00020:H&J\u0010\u0010;\u001a\u00020\u00032\u0006\u0010<\u001a\u00020=H&J\u0016\u0010>\u001a\u00020\u00032\f\u0010?\u001a\b\u0012\u0004\u0012\u00020A0@H&J\u0016\u0010B\u001a\u00020\u00032\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020C0@H&J\b\u0010D\u001a\u00020\u0003H&J\u0016\u0010E\u001a\u00020\u00032\f\u0010F\u001a\b\u0012\u0004\u0012\u00020\u00120GH&J\u0010\u0010H\u001a\u00020\u00032\u0006\u0010I\u001a\u00020JH&\u00a8\u0006K"}, d2 = {"Lnet/jami/call/CallView;", "", "displayContactBubble", "", "display", "", "displayDialPadKeyboard", "displayHangupButton", "displayPluginsButton", "displayPreviewSurface", "displayVideoSurface", "displayPreviewContainer", "enterPipMode", "callId", "", "finish", "goToAddContact", "contact", "Lnet/jami/model/Contact;", "goToContact", "accountId", "goToConversation", "conversationId", "Lnet/jami/model/Uri;", "handleCallWakelock", "isAudioOnly", "initIncomingCallDisplay", "initMenu", "isSpeakerOn", "displayFlip", "canDial", "showPluginBtn", "onGoingCall", "initNormalStateDisplay", "audioOnly", "muted", "initOutGoingCallDisplay", "onUserLeave", "prepareCall", "isIncoming", "resetPluginPreviewVideoSize", "previewWidth", "", "previewHeight", "rot", "resetPreviewVideoSize", "resetVideoSize", "videoWidth", "videoHeight", "startAddParticipant", "conferenceId", "switchCameraIcon", "isFront", "toggleCallMediaHandler", "id", "toggle", "updateAudioState", "state", "Lnet/jami/services/HardwareService$AudioState;", "updateCallStatus", "callState", "Lnet/jami/model/Call$CallStatus;", "updateConfInfo", "info", "", "Lnet/jami/model/Conference$ParticipantInfo;", "updateContactBubble", "Lnet/jami/model/Call;", "updateMenu", "updateParticipantRecording", "contacts", "", "updateTime", "duration", "", "libringclient"})
public abstract interface CallView {
    
    public abstract void displayContactBubble(boolean display);
    
    public abstract void displayVideoSurface(boolean displayVideoSurface, boolean displayPreviewContainer);
    
    public abstract void displayPreviewSurface(boolean display);
    
    public abstract void displayHangupButton(boolean display);
    
    public abstract void displayDialPadKeyboard();
    
    public abstract void switchCameraIcon(boolean isFront);
    
    public abstract void updateAudioState(@org.jetbrains.annotations.NotNull()
    net.jami.services.HardwareService.AudioState state);
    
    public abstract void updateMenu();
    
    public abstract void updateTime(long duration);
    
    public abstract void updateContactBubble(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Call> contact);
    
    public abstract void updateCallStatus(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call.CallStatus callState);
    
    public abstract void initMenu(boolean isSpeakerOn, boolean displayFlip, boolean canDial, boolean showPluginBtn, boolean onGoingCall);
    
    public abstract void initNormalStateDisplay(boolean audioOnly, boolean muted);
    
    public abstract void initIncomingCallDisplay();
    
    public abstract void initOutGoingCallDisplay();
    
    public abstract void resetPreviewVideoSize(int previewWidth, int previewHeight, int rot);
    
    public abstract void resetPluginPreviewVideoSize(int previewWidth, int previewHeight, int rot);
    
    public abstract void resetVideoSize(int videoWidth, int videoHeight);
    
    public abstract void goToConversation(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationId);
    
    public abstract void goToAddContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    public abstract void startAddParticipant(@org.jetbrains.annotations.NotNull()
    java.lang.String conferenceId);
    
    public abstract void finish();
    
    public abstract void onUserLeave();
    
    public abstract void enterPipMode(@org.jetbrains.annotations.NotNull()
    java.lang.String callId);
    
    public abstract void prepareCall(boolean isIncoming);
    
    public abstract void handleCallWakelock(boolean isAudioOnly);
    
    public abstract void goToContact(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact);
    
    public abstract boolean displayPluginsButton();
    
    public abstract void updateConfInfo(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Conference.ParticipantInfo> info);
    
    public abstract void updateParticipantRecording(@org.jetbrains.annotations.NotNull()
    java.util.Set<net.jami.model.Contact> contacts);
    
    public abstract void toggleCallMediaHandler(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String callId, boolean toggle);
}