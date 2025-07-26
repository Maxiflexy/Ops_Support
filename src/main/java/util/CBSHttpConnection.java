package util;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import papss.util.KeyStoreUtil;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * @author Sunday Kusoro
 * @since 13/12/2021
 */
public enum CBSHttpConnection {

    INSTANCE;

    static final private Logger LOG = LogManager.getLogger(CBSHttpConnection.class);

    private HttpClientBuilder connBuilder = null;

    final public static CBSHttpConnection getInstance() {
        return INSTANCE;
    }

    private CBSHttpConnection() {
    }

    //creates http connection client
    public CloseableHttpClient getConnection(String certPath, String alias, String storepass, String keypass, String maxConn, String idle_conn_tm) {

        CloseableHttpClient httClient = null;

        if (connBuilder == null) {

            LOG.debug("entering cnn factory creatn lock");

            synchronized (CBSHttpConnection.class) {

                LOG.debug("cnn factory locked");

                try {

                    connBuilder = getConnBuilderByPass(certPath, alias, storepass, keypass, maxConn, idle_conn_tm);

                } catch (RuntimeException e) {
                    //e.printStackTrace();
                    LOG.error(e.getMessage(), e);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                    LOG.error(e.getMessage(), e);
                }

            }

            LOG.debug("factory lock released");


        }

        try {
            httClient = connBuilder.build();
        } catch (RuntimeException e) {
            //e.printStackTrace();
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            //e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }

        return httClient;

    }

    //creates connection builder
    private final HttpClientBuilder getConnectionBuilder(String basecertPath, String alias, String storepass, String keypass, String maxConn, String idle_conn_tm) throws IOException {


        int tmp = 5;

//        try {
//            tmp = Integer.parseInt(maxConn);
//        } catch (NumberFormatException e) {
//            LOG.error(e.getMessage(), e);
//        }

        int idle_cnn_time = 60;

        try {
            idle_cnn_time = Integer.parseInt(idle_conn_tm);
        } catch (Exception e) {
        }

        HttpClientBuilder builder = HttpClients.custom();

        HttpClientConnectionManager manager = getPooledConnectionFactory(alias, storepass, keypass, tmp, idle_cnn_time);
        //manager.closeExpiredConnections();
        //manager.closeIdleConnections(idle_cnn_time, TimeUnit.SECONDS);
        builder.setConnectionManager(manager);
        builder.setConnectionManagerShared(true);

        return builder;
    }

    private final HttpClientBuilder getConnBuilderByPass(String basecertPath, String alias, String storepass, String keypass, String maxConn, String idle_conn_tm) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;

        // Create a SSL context with the trust strategy
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, trustStrategy)
                .build();

        HttpClientBuilder builder = HttpClients.custom();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
        builder.setSSLSocketFactory(sslsf);

        return builder;
    }


    //creates pooled connection factory
    private HttpClientConnectionManager getPooledConnectionFactory(String alias, String storepass, String keypass, int maxConnections, int idle_cnn_time) throws IOException {

        PoolingHttpClientConnectionManager ret = new PoolingHttpClientConnectionManager();
        ret.setDefaultMaxPerRoute(maxConnections);

        ret.setMaxTotal(maxConnections);

        return ret;

    }

    //creates SSL registry
    private Registry<ConnectionSocketFactory> getSslFactoryRegistry(String certPath, String alias, String storepass, String keypass) throws IOException {
        try {
            String tmpStorePass = storepass;
            if (storepass == null) {
                tmpStorePass = "";
            }
            String tmpKeyPass = keypass;
            if (keypass == null) {
                tmpKeyPass = "";
            }

            KeyStore keyStore = KeyStoreUtil.createKeyStore2(certPath, alias, tmpStorePass);
            SSLContext sslContext = SSLContexts.custom().useProtocol("TLS")
                    .loadKeyMaterial(keyStore, tmpKeyPass.toCharArray())
                    .loadTrustMaterial(keyStore, null)
                    .build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslsf).build();
        } catch (GeneralSecurityException e) {
            // this isn't ideal but the net effect is the same
            throw new IOException(e);
        }
    }

}
