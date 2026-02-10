package com.github.anvaer.webpecker.httpclient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

@Configuration
public class HttpClientConfig {

  private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

  @Bean
  public OkHttpClient okHttpClient(
      @Value("${httpclient.timeout:120000}") int httpTimeout) {

    X509TrustManager trustAllManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {};
      }
    };
    TrustManager[] trustAllCerts = new TrustManager[] { trustAllManager };

    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      log.warn("Unable to init custom SSL context, using default OkHttpClient.", e);
      return new OkHttpClient.Builder()
          .callTimeout(httpTimeout, TimeUnit.MILLISECONDS)
          .build();
    }

    return new OkHttpClient.Builder()
        .sslSocketFactory(sslContext.getSocketFactory(), trustAllManager)
        .hostnameVerifier((hostname, session) -> true)
        .callTimeout(httpTimeout,
            TimeUnit.MILLISECONDS)
        .build();
  }
}
