/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.call;

import java.util.List;

import cx.ring.model.CallContact;
import cx.ring.model.SipCall.CallStatus;
import cx.ring.model.SipCall;
import cx.ring.services.HardwareService;


public interface CallView {

    void displayContactBubble(boolean display);

    void displayVideoSurface(boolean displayVideoSurface, boolean displayPreviewContainer);

    void displayPreviewSurface(boolean display);

    void displayHangupButton(boolean display);

    void displayDialPadKeyboard();

    void switchCameraIcon(boolean isFront);
    void updateAudioState(HardwareService.AudioState state);

    void updateMenu();

    void updateTime(long duration);

    void updateContactBubble(List<SipCall> contact);

    void updateCallStatus(CallStatus callState);

    void initMenu(boolean isSpeakerOn, boolean displayFlip, boolean canDial, boolean onGoingCall);

    void initNormalStateDisplay(boolean audioOnly, boolean muted);

    void initIncomingCallDisplay();

    void initOutGoingCallDisplay();

    void resetPreviewVideoSize(int previewWidth, int previewHeight, int rot);
    void resetVideoSize(int videoWidth, int videoHeight);

    void goToConversation(String accountId, String conversationId);

    void goToAddContact(CallContact callContact);

    void startAddParticipant(String conferenceId);

    void finish();

    void onUserLeave();

    void enterPipMode(String callId);

    void prepareCall(boolean isIncoming);

    void handleCallWakelock(boolean isAudioOnly);

    void goToContact(String accountId, CallContact contact);
}
