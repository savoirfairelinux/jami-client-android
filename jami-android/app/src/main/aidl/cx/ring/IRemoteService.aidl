package cx.ring;

interface IRemoteService {
    String createAccount(String registeredName);
    String getAccountId();
    void initiateCall(String userId, ICallback callback);
    interface ICallback {
        void onSuccess();
        void onError(String error);
    }
    void hangUpCall();
    void acceptCall();
    void rejectCall();
    byte[] getUserImage();
}