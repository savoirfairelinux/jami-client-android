package cx.ring;

interface IRemoteService {
    String createAccount(String registeredName);
    void addContact(String accountId, String contactId);
    void sendTrustRequest(String accountId, String contactId);
    String getAccountId();
    void initiateCall(String userId, ICallback callback);
    interface ICallback {
        void onSuccess();
        void onError(String error);
    }
    void hangUpCall();
    void acceptCall();
    void rejectCall();
    Bitmap getCallerImage(String userId);
}