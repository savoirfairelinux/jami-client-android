/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.Call;

import cx.ring.model.SipCall;

public interface CallView {

    void displayContactBubble(boolean display);

    void initNormalStateDisplay(boolean hasVideo);

    void initContactDisplay(SipCall call);

    void initIncomingCallDisplay();

    void initOutGoingCallDisplay();

    void updateCallStatus(int callState);

    void displayVideoSurface(boolean display);

    void updateTime(long duration);

    void finish();

    void updateContactBubbleWithVCard(final String contactName, final byte[] photo);
}
