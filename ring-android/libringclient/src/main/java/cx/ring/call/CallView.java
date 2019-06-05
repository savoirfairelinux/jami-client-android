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

import cx.ring.model.CallContact;
import cx.ring.model.SipCall;

public interface CallView {

    void displayContactBubble(boolean display);

    void displayVideoSurface(boolean displayVideoSurface, boolean displayPreviewContainer);

    void displayPreviewSurface(boolean display);

    void displayHangupButton(boolean display);

    void displayDialPadKeyboard();

    void switchCameraIcon(boolean isFront);

    void updateMenu();

    void updateTime(long duration);

    void updateContactBubble(CallContact contact);

    void updateCallStatus(SipCall.State callState);

    void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall);

    void initNormalStateDisplay(boolean audioOnly, boolean isSpeakerphoneOn, boolean muted);

    void initIncomingCallDisplay();

    void initOutGoingCallDisplay();

    void resetPreviewVideoSize(int previewWidth, int previewHeight, int rot);
    void resetVideoSize(int videoWidth, int videoHeight);

    void goToConversation(String accountId, String conversationId);

    void goToAddContact(CallContact callContact);

    void finish();

    void onUserLeave();

    void enterPipMode(SipCall sipCall);
}
