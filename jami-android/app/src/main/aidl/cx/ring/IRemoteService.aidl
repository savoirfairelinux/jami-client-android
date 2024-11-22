package cx.ring;

interface IRemoteService {
    String createAccount(String registeredName);
    String getAccountId();
    void initiateCall(String userId);
    void hangUpCall();
    void acceptCall();
    void rejectCall();
    byte[] getUserImage();
}