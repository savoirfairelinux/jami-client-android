package cx.ring;
/*
Copyright 2024 Generation Reach GmbH

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.
*/

interface IRemoteService {
    String createAccount(in Map<String, String> accountData);
    List<String> listAccounts();
    String getJamiUri(String account);
    void addContact(String contactId);
    boolean isContactExist(String contactId);
    void sendTrustRequest(String contactId);
    String getAccountId();
    void initiateCall(String fromAccount, String userId, ICallback callback);
    void registerCallStateCallback(StateCallback callback);
    void unregisterCallStateCallback(StateCallback callback);
    interface StateCallback {
        void newCallState(String state);
    }
    interface ICallback {
        void onSuccess();
        void onError(String error);
    }
    void hangUpCall();
    void acceptCall();
    void rejectCall();
    Bitmap getCallerImage(String userId);
    void setProfileData(String peerId, @nullable String name, @nullable String imageUri, @nullable String fileType);
    void registerEventListener(IEventListener listener);
    void unregisterEventListener(IEventListener listener);
    interface IEventListener {
        void onEventReceived(String name, in @nullable Map<String, String> data);
    }
    Map<String, String> getAccountInfo(String account);
    String getPushToken();
    interface IConnectionMonitor {
        void onMessages(in List<String> line);
    }
    void registerConnectionMonitor(IConnectionMonitor monitor);
    void unregisterConnectionMonitor(IConnectionMonitor monitor);
    // null if the accounts haven't been loaded yet
    @nullable String getOrCreateAccount(in Map<String, String> accountData);
    void connectivityChanged(boolean isConnected);
    void setAccountData(String accountId, in Map<String, String> accountData);
    void startConversation(String accountId, in @nullable List<String> initialMembers);
    void addConversationMember(String accountId, String conversationId, String contactUri);
    void removeConversationMember(String accountId, String conversationId, String contactUri);
    void sendMessage(String accountId, String conversationId, String message, String replyTo, int flag);
}