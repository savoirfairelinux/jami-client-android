package com.savoirfairelinux.sflphone.service;

interface ISipService {
    void placeCall(String accountID, in String callID, in String to);
    void refuse(in String callID);
    void accept(in String callID);
    void hangUp(in String callID);
    void hold(in String callID);
    void unhold(in String callID);
    List getAccountList();
    String addAccount(in Map accountDetails);
    void removeAccount(in String accoundId);
    Map getAccountDetails(in String accountID);
    void setAccountDetails(in String accountId, in Map accountDetails);
    void setAudioPlugin(in String callID);
    String getCurrentAudioOutputPlugin();
}
