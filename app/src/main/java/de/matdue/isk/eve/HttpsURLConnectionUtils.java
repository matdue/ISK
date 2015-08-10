/**
 * Copyright 2015 Matthias Düsterhöft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.matdue.isk.eve;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Helper class for some HTTP utils, useful for debugging purpose.
 */
public class HttpsURLConnectionUtils {

    /**
     * Lets <code>connection</code> trust all SSL connections.
     * Example: <code>HttpsURLConnectionUtils.trustAllCertificates(connection);</code>
     *
     * @param connection HTTPS connection
     */
    public static void trustAllCertificates(HttpsURLConnection connection) {
        TrustManager trustAllCerts = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Not implemented
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustAllCerts }, new SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a proxy.
     * Example: <code>connection = (HttpsURLConnection) requestURL.openConnection(HttpsURLConnectionUtils.buildProxy("10.0.2.2", 8888));</code>
     *
     * @param hostIp IP address of proxy; 10.0.2.2 is the local machine if you use the emulator
     * @param port Port of proxy
     * @return Proxy
     */
    public static Proxy buildProxy(String hostIp, int port) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostIp, port));
    }

}
