package cx.ring.services;

public class PluginServiceImpl extends PluginService {

    private static final String TAG = PluginServiceImpl.class.getSimpleName();
    public PluginServiceImpl.PluginServiceInterface listener;

    public PluginServiceImpl(PluginServiceInterface listener) {
        if (listener != null)
            this.listener = listener;
    }

    public void askTrustPluginIssuer(String issuer, String companyDivision, String pluginName, String rootPath) {
        if (listener != null)
            listener.onAskTrustPluginIssuer(issuer, companyDivision, pluginName, rootPath);
    }

    public interface PluginServiceInterface {
        void onAskTrustPluginIssuer(String issuer, String companyDivision, String pluginName, String rootPath);
    }
}
