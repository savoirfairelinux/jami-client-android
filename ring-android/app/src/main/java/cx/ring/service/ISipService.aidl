package cx.ring.service;

import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Conference;

interface ISipService {
    
    Map getCallDetails(in String callID);
    String placeCall(in SipCall call);
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
    List getAudioCodecList(in String accountID);
    void setActiveCodecList(in List codecs, in String accountID);

    Map validateCertificatePath(in String accountID, in String certificatePath, in String privateKeyPath, in String privateKeyPass);
    Map validateCertificate(in String accountID, in String certificateId);
    Map getCertificateDetailsPath(in String certificatePath);
    Map getCertificateDetails(in String certificate);

    // FIXME
    void toggleSpeakerPhone(in boolean toggle);

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
    void confirmSAS(in String callID);
    List getTlsSupportedMethods();

	/* DTMF */
	void playDtmf(in String key);
    
    /* IM */
    void sendTextMessage(in String callID, in TextMessage message);
    void sendAccountTextMessage(in String accountid, in String to, in String msg);


    void transfer(in String callID, in String to);
    void attendedTransfer(in String transferID, in String targetID);
    
    /* Conference related methods */

    void removeConference(in String confID);
    void joinParticipant(in String sel_callID, in String drag_callID);

    void addParticipant(in SipCall call, in String confID);
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
    
    Conference getCurrentCall();
    List getConcurrentCalls();

    Conference getConference(in String id);

}
