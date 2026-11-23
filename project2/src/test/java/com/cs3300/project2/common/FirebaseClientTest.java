package com.cs3300.project2.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class FirebaseClientTest {

    @Test
    void getInstanceAlwaysReturnsTheSameClient() {
        assertSame(FirebaseClient.getInstance(), FirebaseClient.getInstance());
    }

    @Test
    void sharesOneHttpTransport() {
        HttpClient http = FirebaseClient.getInstance().http();

        assertNotNull(http);
        assertSame(http, FirebaseClient.getInstance().http());
    }

    @Test
    void httpTransportFollowsRedirects() {
        assertEquals(HttpClient.Redirect.NORMAL, FirebaseClient.getInstance().http().followRedirects());
    }

    @Test
    void cannotBeInstantiatedOrSubclassedFromOutside() {
        Constructor<?>[] constructors = FirebaseClient.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
        assertTrue(Modifier.isFinal(FirebaseClient.class.getModifiers()));
    }

    @Test
    void concurrentCallersReceiveTheSameInstance() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<FirebaseClient>> calls = IntStream.range(0, 64)
                    .<Callable<FirebaseClient>>mapToObj(i -> FirebaseClient::getInstance)
                    .toList();
            Set<FirebaseClient> seen = ConcurrentHashMap.newKeySet();
            for (Future<FirebaseClient> result : pool.invokeAll(calls)) {
                seen.add(result.get());
            }
            assertEquals(1, seen.size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void springBeanExposesTheSingletonTransport() {
        assertSame(FirebaseClient.getInstance().http(), new FirebaseConfiguration().firebaseHttpClient());
    }
}
