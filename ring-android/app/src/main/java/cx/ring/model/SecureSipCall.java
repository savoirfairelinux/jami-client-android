/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux>
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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import cx.ring.model.account.AccountDetailSrtp;


public class SecureSipCall extends SipCall {

    public interface SecureLayer {
        //int ZRTP_LAYER = 0;
        int SDES_LAYER = 1;
    }

    public final static int DISPLAY_GREEN_LOCK = 0;
    public final static int DISPLAY_RED_LOCK = 1;
    public final static int DISPLAY_CONFIRM_SAS = 2;
    public final static int DISPLAY_NONE = 3;

    int mSecureLayerUsed;
    //ZrtpModule mZrtpModule;
    SdesModule mSdesModule;

    private boolean isInitialized;

    public SecureSipCall(SipCall call) {
        super(call);
        isInitialized = false;
        String keyExchange = getAccount().getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE);
        /*if (keyExchange.contentEquals("zrtp")) {
            mSecureLayerUsed = SecureLayer.ZRTP_LAYER;
        } else */if (keyExchange.contentEquals("sdes")) {
            mSecureLayerUsed = SecureLayer.SDES_LAYER;
        }

        //mZrtpModule = new ZrtpModule();
        mSdesModule = new SdesModule();
    }

    public void setSASConfirmed(boolean confirmedSAS) {
        //mZrtpModule.needSASConfirmation = !confirmedSAS;
    }

    public String getSAS() {
        //return mZrtpModule.SAS;
        return "";
    }

    public void setSAS(String SAS) {
        //mZrtpModule.SAS = SAS;
    }

    public SecureSipCall(Parcel in) {
        super(in);
        isInitialized = in.readByte() == 1;
        mSecureLayerUsed = in.readInt();
        mSdesModule = new SdesModule(in);
        //mZrtpModule = new ZrtpModule(in);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeByte((byte) (isInitialized ? 1 : 0));
        out.writeInt(mSecureLayerUsed);
        mSdesModule.writeToParcel(out);
        //mZrtpModule.writeToParcel(out);
    }

    public static final Parcelable.Creator<SecureSipCall> CREATOR = new Parcelable.Creator<SecureSipCall>() {
        public SecureSipCall createFromParcel(Parcel in) {
            return new SecureSipCall(in);
        }

        public SecureSipCall[] newArray(int size) {
            return new SecureSipCall[size];
        }
    };

    public void sasConfirmedByZrtpLayer(int verified) {
        // Not used
    }

    public void setZrtpSupport(boolean support) {
        /*mZrtpModule.zrtpIsSupported = support;
        if (!support)
            mZrtpModule.needSASConfirmation = false;*/
    }

    public void setInitialized() {
        isInitialized = true;
    }

    /*
    * returns what state should be visible during call
    */
    public int displayModule() {
        /*if (isInitialized) {
            Log.i("SecureSIp", "needSASConfirmation" + mZrtpModule.needSASConfirmation);
            if (mZrtpModule.needSASConfirmation) {
                return DISPLAY_CONFIRM_SAS;
            } else if (mZrtpModule.zrtpIsSupported || mSdesModule.sdesIsOn) {
                return DISPLAY_GREEN_LOCK;
            } else {
                return DISPLAY_RED_LOCK;
            }
        }*/
        return DISPLAY_NONE;
    }

    public void useSecureSDES(boolean use) {
        mSdesModule.sdesIsOn = use;
        //mZrtpModule.needSASConfirmation = false;
    }

/*
    private class ZrtpModule {
        private String SAS;
        private boolean needSASConfirmation;
        private boolean zrtpIsSupported;

        // static preferences of account
        private final boolean displaySas;
        private final boolean alertIfZrtpNotSupported;
        private final boolean displaySASOnHold;

        public ZrtpModule() {
            displaySas = getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS);
            alertIfZrtpNotSupported = getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING);
            displaySASOnHold = getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING);
            needSASConfirmation = displaySas;
            zrtpIsSupported = false;
        }

        public ZrtpModule(Parcel in) {
            SAS = in.readString();
            displaySas = in.readByte() == 1;
            alertIfZrtpNotSupported = in.readByte() == 1;
            displaySASOnHold = in.readByte() == 1;
            zrtpIsSupported = in.readByte() == 1;
            needSASConfirmation = in.readByte() == 1;
        }

        public void writeToParcel(Parcel dest) {
            dest.writeString(SAS);
            dest.writeByte((byte) (displaySas ? 1 : 0));
            dest.writeByte((byte) (alertIfZrtpNotSupported ? 1 : 0));
            dest.writeByte((byte) (displaySASOnHold ? 1 : 0));
            dest.writeByte((byte) (zrtpIsSupported ? 1 : 0));
            dest.writeByte((byte) (needSASConfirmation ? 1 : 0));
        }
    }
*/
    private class SdesModule {

        private boolean sdesIsOn;

        public SdesModule() {
            sdesIsOn = false;
        }

        public SdesModule(Parcel in) {
            sdesIsOn = in.readByte() == 1;
        }

        public void writeToParcel(Parcel dest) {
            dest.writeByte((byte) (sdesIsOn ? 1 : 0));
        }
    }
}
