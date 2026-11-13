package com.cs3300.project2.auth;

public class FirebaseAuthException extends IllegalArgumentException {
    private final String firebaseCode;
    private final int firebaseStatus;

    public FirebaseAuthException(String message, String firebaseCode, int firebaseStatus) {
        super(message);
        this.firebaseCode = firebaseCode;
        this.firebaseStatus = firebaseStatus;
    }

    public String firebaseCode() { return firebaseCode; }
    public int firebaseStatus() { return firebaseStatus; }
}
