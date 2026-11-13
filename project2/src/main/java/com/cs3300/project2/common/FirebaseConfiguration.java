package com.cs3300.project2.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.http.HttpClient;

/** Firebase REST configuration. No Firebase or Google client SDK is needed. */
@Configuration
@Profile("!test")
public class FirebaseConfiguration {
    @Bean
    FirebaseSettings firebaseSettings(
            @Value("${FIREBASE_WEB_API_KEY:}") String webApiKey,
            @Value("${FIREBASE_DATABASE_URL:}") String databaseUrl) {
        if (webApiKey == null || webApiKey.isBlank()) {
            throw new IllegalStateException("FIREBASE_WEB_API_KEY is required");
        }
        if (databaseUrl == null || !databaseUrl.startsWith("https://")) {
            throw new IllegalStateException("FIREBASE_DATABASE_URL must be the HTTPS URL shown for your Realtime Database");
        }
        return new FirebaseSettings(webApiKey, databaseUrl.replaceAll("/+$", ""));
    }

    @Bean
    HttpClient firebaseHttpClient() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }
}
