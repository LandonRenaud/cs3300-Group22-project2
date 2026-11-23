package com.cs3300.project2.common;

import java.net.http.HttpClient;

/**
 * Singleton pattern: the one shared Firebase REST client for the whole application.
 *
 * <p>Firebase Authentication ({@code FirebaseIdentityService}) and the Realtime Database
 * ({@link RealtimeDatabaseDocuments}) both send their requests through this instance, so every
 * database and auth call shares one connection pool and one configuration.
 *
 * <p>The constructor is private, so no other code can create a second client. The instance is
 * created lazily and thread-safely the first time {@link #getInstance()} is called
 * (initialization-on-demand holder idiom).
 */
public final class FirebaseClient {

    private final HttpClient http;

    private FirebaseClient() {
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** The JVM loads this class, and so creates the instance, only on first access. */
    private static final class Holder {
        private static final FirebaseClient INSTANCE = new FirebaseClient();
    }

    public static FirebaseClient getInstance() {
        return Holder.INSTANCE;
    }

    /** The shared HTTP transport used for all Firebase REST requests. */
    public HttpClient http() {
        return http;
    }
}
