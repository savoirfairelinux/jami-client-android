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

package org.sflphone.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


public class SecureSipCall extends SipCall {

    public static String DISPLAY_SAS = "displaySAS";
    public static String DISPLAY_SAS_ONCE = "displaySasOnce";
    public static String DISPLAY_WARNING_ZRTP_NOT_SUPPORTED = "notSuppWarning";

    /*
    *
    srtp:
    enable: false
    keyExchange: sdes
    rtpFallback: false
    stunEnabled: false
    stunServer:

    tls:
    calist:
    certificate:
    ciphers:
    enable: false
    method: TLSv1
    password:
    privateKey:
    requireCertif: true
    server:
    timeout: 2
    tlsPort: 5061
    verifyClient: true
    verifyServer: true

    zrtp:
    displaySas: true
    displaySasOnce: false
    helloHashEnabled: true
    notSuppWarning: true

    *
    *
    *
    * */

    private String SAS;
    private boolean needSASConfirmation;
    private boolean zrtpNotSupported;
    private boolean alertIfZrtpNotSupported;
    private boolean displaySASOnHold;

    private boolean isInitialized;


    public SecureSipCall(SipCall call, Bundle secure){
        super(call);
        isInitialized = false;
        needSASConfirmation = secure.getBoolean(SecureSipCall.DISPLAY_SAS, false);
        alertIfZrtpNotSupported = secure.getBoolean(SecureSipCall.DISPLAY_WARNING_ZRTP_NOT_SUPPORTED, false);
        displaySASOnHold = secure.getBoolean(SecureSipCall.DISPLAY_SAS_ONCE, false);
    }

    public boolean needSASConfirmation() {
        return needSASConfirmation;
    }

    public void setSASConfirmed(boolean confirmedSAS) {
        needSASConfirmation = !confirmedSAS;
    }

    public String getSAS() {
        return SAS;
    }

    public void setSAS(String SAS) {
        this.SAS = SAS;
    }

    public SecureSipCall(Parcel in){
        super(in);
        SAS = in.readString();
        needSASConfirmation = in.readByte() == 1;
        isInitialized = in.readByte() == 1;

    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(SAS);
        out.writeByte((byte) (needSASConfirmation ? 1 : 0));
        out.writeByte((byte) (isInitialized ? 1 : 0));
    }

    public static final Parcelable.Creator<SecureSipCall> CREATOR = new Parcelable.Creator<SecureSipCall>() {
        public SecureSipCall createFromParcel(Parcel in) {
            return new SecureSipCall(in);
        }

        public SecureSipCall[] newArray(int size) {
            return new SecureSipCall[size];
        }
    };

    public void sasConfirmedByZrtpLayer(boolean verified) {

    }

    public void setZrtpNotSupported(boolean zrtpNotSupported) {
        this.zrtpNotSupported = zrtpNotSupported;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized() {
        isInitialized = true;
    }
}
