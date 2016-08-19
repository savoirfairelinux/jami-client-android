/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

package cx.ring.service;

interface IDRingService {

    boolean isStarted();

    Map getCallDetails(in String callID);
    String placeCall(in String account, in String number, in boolean hasVideo);
    void refuse(in String callID);
    void accept(in String callID);
    void hangUp(in String callID);
    void hold(in String callID);
    void unhold(in String callID);

    List getAccountList();
    String addAccount(in Map accountDetails);
    void removeAccount(in String accoundId);
    void setAccountOrder(in String order);
    Map getAccountDetails(in String accountID);
    Map getVolatileAccountDetails(in String accountID);
    Map getAccountTemplate(in String accountType);
    void registerAllAccounts();
    void setAccountDetails(in String accountId, in Map accountDetails);
    void setAccountActive(in String accountId, in boolean active);
    void setAccountsActive(in boolean active);
    List getCredentials(in String accountID);
    void setCredentials(in String accountID, in List creds);
    void setAudioPlugin(in String callID);
    String getCurrentAudioOutputPlugin();
    List getCodecList(in String accountID);
    void setActiveCodecList(in List codecs, in String accountID);

    Map validateCertificatePath(in String accountID, in String certificatePath, in String privateKeyPath, in String privateKeyPass);
    Map validateCertificate(in String accountID, in String certificateId);
    Map getCertificateDetailsPath(in String certificatePath);
    Map getCertificateDetails(in String certificate);

    /* Recording */
    void setRecordPath(in String path);
    String getRecordPath();
    boolean toggleRecordingCall(in String id);
    boolean startRecordedFilePlayback(in String filepath);
    void stopRecordedFilePlayback(in String filepath);

    /* Mute */
    void setMuted(boolean mute);
    boolean isCaptureMuted();

    /* Security */
    List getTlsSupportedMethods();

    /* DTMF */
    void playDtmf(in String key);

    /* IM */
    void sendTextMessage(in String callID, in String message);
    long sendAccountTextMessage(in String accountid, in String to, in String msg);


    void transfer(in String callID, in String to);
    void attendedTransfer(in String transferID, in String targetID);

    /* Video */
    void setPreviewSettings();
    void switchInput(in String call, in boolean front);
    void videoSurfaceAdded(in String call);
    void videoSurfaceRemoved(in String call);
    void videoPreviewSurfaceAdded();
    void videoPreviewSurfaceRemoved();

    /* Conference related methods */

    void removeConference(in String confID);
    void joinParticipant(in String sel_callID, in String drag_callID);

    void addParticipant(in String callID, in String confID);
    void addMainParticipant(in String confID);
    void detachParticipant(in String callID);
    void joinConference(in String sel_confID, in String drag_confID);
    void hangUpConference(in String confID);
    void holdConference(in String confID);
    void unholdConference(in String confID);
    boolean isConferenceParticipant(in String callID);
    Map getConferenceList();
    List getParticipantList(in String confID);
    String getConferenceId(in String callID);
    String getConferenceDetails(in String callID);

    Map getConference(in String id);

    int exportAccounts(in List accountIDs, in String toDir, in String password);
    int importAccounts(in String archivePath, in String password);

    void connectivityChanged();
}
