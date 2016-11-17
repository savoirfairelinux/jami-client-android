/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.model;

import java.util.HashMap;

public class SecureSipCall extends SipCall {
    private static final String TAG = SecureSipCall.class.getSimpleName();

    public interface SecureLayer {
        int SDES_LAYER = 1;
    }

    public final static int DISPLAY_GREEN_LOCK = 0;
    public final static int DISPLAY_RED_LOCK = 1;
    public final static int DISPLAY_NONE = 3;

    int mSecureLayerUsed;
    SdesModule mSdesModule;

    private String mTlsCipher = null;

    public SecureSipCall(SipCall call, String keyExchange) {
        super(call);
        if (keyExchange.contentEquals("sdes"))
            mSecureLayerUsed = SecureLayer.SDES_LAYER;

        mSdesModule = new SdesModule();
    }

    public void setDetails(HashMap<String, String> details) {
        mTlsCipher = details.get("TLS_CIPHER");
        super.setDetails(details);
    }

    /**
     * Check if SIP transport uses TLS.
     * Ring should always use SRTP if TLS is enabled.
     * @return true if the call is encrypted
     */
    public boolean isSecure() {
        return mTlsCipher != null && !mTlsCipher.isEmpty();
    }

    /*
    * returns what State should be visible during call
    */
    public int displayModule() {
        return DISPLAY_NONE;
    }

    public void useSecureSDES(boolean use) {
        mSdesModule.sdesIsOn = use;
    }

    private class SdesModule {

        private boolean sdesIsOn;

        public SdesModule() {
            sdesIsOn = false;
        }
 }
}
