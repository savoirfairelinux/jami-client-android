package cx.ring.model;

public class Preferences {

    private boolean mStatus;
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;
    private String mRootset;
    private String mUseragent;
    private boolean mAutoAnswer;
    private boolean mUpnpEnabled;

    public boolean getStatus() {
        return mStatus;
    }

    public void setStatus(boolean status) {
        mStatus = status;
    }

    public String getAlias() {
        return mAlias;
    }

    public void setAlias(String alias) {
        mAlias = alias;
    }

    public String getHostname() {
        return mHostname;
    }

    public void setHostname(String hostname) {
        mHostname = hostname;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getRootset() {
        return mRootset;
    }

    public void setRootset(String rootset) {
        mRootset = rootset;
    }

    public String getUseragent() {
        return mUseragent;
    }

    public void setUseragent(String useragent) {
        mUseragent = useragent;
    }

    public boolean isAutoAnswer() {
        return mAutoAnswer;
    }

    public void setAutoAnswer(boolean autoAnswer) {
        mAutoAnswer = autoAnswer;
    }

    public boolean isUpnpEnabled() {
        return mUpnpEnabled;
    }

    public void setUpnpEnabled(boolean upnpEnabled) {
        mUpnpEnabled = upnpEnabled;
    }
}
