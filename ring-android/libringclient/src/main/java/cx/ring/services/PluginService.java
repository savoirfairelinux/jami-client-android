package cx.ring.services;

public abstract class PluginService {

    public abstract void askTrustPluginIssuer(String issuer, String companyDivision, String pluginName, String rootPath);
}
