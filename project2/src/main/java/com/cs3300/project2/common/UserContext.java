package com.cs3300.project2.common;

/** Holds the verified Firebase UID for the lifetime of the current request. */
public final class UserContext {
    private static final ThreadLocal<Identity> IDENTITY = new ThreadLocal<>();

    private UserContext() { }

    public static void set(String uid, String idToken) { IDENTITY.set(new Identity(uid, idToken)); }
    public static String requireUid() {
        Identity identity = IDENTITY.get();
        if (identity == null) throw new IllegalStateException("No authenticated user in this request");
        return identity.uid();
    }
    public static String requireIdToken() {
        Identity identity = IDENTITY.get();
        if (identity == null) throw new IllegalStateException("No authenticated user in this request");
        return identity.idToken();
    }
    public static void clear() { IDENTITY.remove(); }

    private record Identity(String uid, String idToken) { }
}
