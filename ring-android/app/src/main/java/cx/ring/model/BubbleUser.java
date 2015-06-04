/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.model;

import android.content.Context;
import cx.ring.R;

public class BubbleUser extends Bubble {

    public Conference associated_call;

    public BubbleUser(Context context, CallContact m, Conference conf, float x, float y, float size) {
        super(context, m, x, y, size);
        isUser = true;
        associated_call = conf;
    }

    @Override
    public boolean getHoldStatus() {
        return associated_call.isOnHold();
    }

    @Override
    public boolean getRecordStatus() {
        return associated_call.isRecording();
    }

    public Conference getConference() {
        return associated_call;
    }

    public void setConference(Conference c) {
        associated_call = c;
    }

    @Override
    public String getName() {
        return mContext.getResources().getString(R.string.me);
    }

    @Override
    public boolean callIDEquals(String call) {
        return associated_call.getId().contentEquals(call);
    }

    @Override
    public String getCallID() {
        if (associated_call.hasMultipleParticipants())
            return associated_call.getId();
        else
            return associated_call.getParticipants().get(0).getCallId();
    }

    @Override
    public boolean isConference() {
        return associated_call.hasMultipleParticipants();
    }

}
