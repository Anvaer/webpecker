package com.github.anvaer.webpecker.httpclient;

import okhttp3.OkHttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

  TrustManager[] trustAllCerts;
  SSLContext sslContext;

  @Bean
  public OkHttpClient okHttpClient(
      @Value("${httpclient.timeout:120000}") int httpTimeout) {

    trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
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
        }
    };

    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      System.out.println("unable to init custom ssl context: " + e);
    }

    return new OkHttpClient.Builder()
        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
        .hostnameVerifier((hostname, session) -> true)
        .callTimeout(httpTimeout,
            TimeUnit.MILLISECONDS)
        .build();
  }
}
