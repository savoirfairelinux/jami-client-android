/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

public interface CallView {

    void blockScreenRotation();

    void displayContactBubble(boolean display);

    void displayVideoSurface(boolean display);

    void displayHangupButton(boolean display);

    void displayDialPadKeyboard();

    void switchCameraIcon(boolean isFront);

    void updateMenu();

    void changeScreenRotation();

    void updateTime(long duration);

    void updateContactBubble(CallContact contact);

    void updateCallStatus(int callState);

    void initMenu(boolean isSpeakerOn, boolean hasContact, boolean displayFlip, boolean canDial, boolean onGoingCall);

    void initNormalStateDisplay(boolean audioOnly);

    void initIncomingCallDisplay();

    void initOutGoingCallDisplay();

    void resetVideoSize(int videoWidth, int videoHeight, int previewWidth, int previewHeight);

    void goToConversation(String accountId, String conversationId);

    void goToAddContact(CallContact callContact);

    void finish();
}
