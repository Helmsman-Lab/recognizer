package com.rec.recognizer.http;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class HttpClientFactory implements DisposableBean {
    private final static Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

    private CloseableHttpClient httpClient;

    private PoolingHttpClientConnectionManager connectionManager;

    private HttpIdleConnectionMonitorThread httpIdleConnectionMonitorThread;

    public HttpClientFactory() {
        this.connectionManager = getCoonectionManager();
        httpIdleConnectionMonitorThread = new HttpIdleConnectionMonitorThread(this.connectionManager);
        httpIdleConnectionMonitorThread.start();
    }

    private PoolingHttpClientConnectionManager getCoonectionManager() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
        registryBuilder.register("http", plainSF);
        // 指定信任密钥存储对象和连接套接字工厂
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            // 信任任何链接
            TrustStrategy anyTrustStrategy = new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            };
            SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, anyTrustStrategy)
                    .build();
            LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registryBuilder.register("https", sslSF);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        PoolingHttpClientConnectionManager cnnManager = new PoolingHttpClientConnectionManager(registry);
        cnnManager.setMaxTotal(2000);
        cnnManager.setDefaultMaxPerRoute(200);

        return cnnManager;
    }

    public CloseableHttpClient getClient(RequestConfig requestConfig, SocketConfig socketConfig) {
        if (this.httpClient == null) {
            synchronized (this) {
                if (this.httpClient == null) {
                    Stopwatch sw = Stopwatch.createStarted();
                    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setConnectionManager(this.connectionManager)
                            .setConnectionManagerShared(true);
                    httpClientBuilder.setDefaultRequestConfig(requestConfig);
                    httpClientBuilder.setDefaultSocketConfig(socketConfig);

                    CloseableHttpClient x = httpClientBuilder.build();
                    System.err.println(sw);

                    this.httpClient = x;
                }
            }
        }

        return this.httpClient;
    }

    @Override
    public void destroy() throws Exception {
        this.connectionManager.shutdown();
    }

    static class HttpIdleConnectionMonitorThread extends Thread {
        private final PoolingHttpClientConnectionManager pm;

        public HttpIdleConnectionMonitorThread(PoolingHttpClientConnectionManager pm) {
            this.pm = pm;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
                    if (pm != null) {
                        pm.closeExpiredConnections();
                        pm.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                } catch (Exception e) {
                    logger.error("close expired or idle connection failed, cause: {}", e);
                }
            }
        }
    }
}
