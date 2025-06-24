package br.com.hugobenicio.mycerts.core.tls;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * The classical "insecurely" empty trust'em all TrustManager. Use this with care.
 */
public class InsecureX509TrustManager implements X509TrustManager {

    public static TrustManager[] newTrustManagers() {
        return new TrustManager[]{ new InsecureX509TrustManager() };
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) {
        // noop
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        // noop
    }
}
