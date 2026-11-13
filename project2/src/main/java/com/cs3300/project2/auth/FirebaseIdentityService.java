package com.cs3300.project2.auth;

import com.cs3300.project2.common.FirebaseSettings;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Profile("!test")
public class FirebaseIdentityService {
    private static final Logger log = LoggerFactory.getLogger(FirebaseIdentityService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final FirebaseSettings settings;
    private final JsonMapper mapper;
    private final java.net.http.HttpClient http;

    public FirebaseIdentityService(FirebaseSettings settings, JsonMapper mapper, java.net.http.HttpClient http) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = http;
    }

    public AuthController.AuthResponse register(AuthController.Credentials credentials) {
        String email = normalizeEmail(credentials);
        JsonNode result = post("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + settings.webApiKey(),
                Map.of("email", email, "password", password(credentials), "returnSecureToken", true), Operation.SIGN_UP);
        return response(result, email);
    }

    public AuthController.AuthResponse login(AuthController.Credentials credentials) {
        String email = normalizeEmail(credentials);
        JsonNode result = post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + settings.webApiKey(),
                Map.of("email", email, "password", password(credentials), "returnSecureToken", true), Operation.SIGN_IN);
        return response(result, email);
    }

    public AuthController.AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new IllegalArgumentException("Refresh token is required");
        JsonNode result = post("https://securetoken.googleapis.com/v1/token?key=" + settings.webApiKey(),
                Map.of("grant_type", "refresh_token", "refresh_token", refreshToken), Operation.REFRESH);
        String email = result.path("email").asText("");
        if (email.isBlank()) {
            try { email = accountForToken(result.path("id_token").asText()).path("email").asText(""); }
            catch (Exception ignored) { }
        }
        return new AuthController.AuthResponse(result.path("id_token").asText(),
                result.path("refresh_token").asText(), email, result.path("expires_in").asLong());
    }

    public String verifyIdToken(String idToken) {
        JsonNode account = accountForToken(idToken);
        String uid = account.path("localId").asText("");
        if (uid.isBlank()) throw new IllegalArgumentException("Firebase token did not resolve to a user");
        return uid;
    }

    private JsonNode accountForToken(String idToken) {
        JsonNode result = post("https://identitytoolkit.googleapis.com/v1/accounts:lookup?key="
                        + settings.webApiKey(), Map.of("idToken", idToken), Operation.LOOKUP);
        JsonNode users = result.path("users");
        if (!users.isArray() || users.isEmpty()) throw new IllegalArgumentException("Firebase user was not found");
        return users.get(0);
    }

    private JsonNode post(String url, Map<String, ?> payload, Operation operation) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = mapper.readTree(response.body());
            if (response.statusCode() >= 400) {
                String firebaseCode = body.path("error").path("message").asText("");
                String friendlyMessage = messageFor(firebaseCode, operation, response.statusCode());
                log.warn("Firebase Authentication {} failed: HTTP {}, Firebase code='{}', message='{}'",
                        operation, response.statusCode(), firebaseCode, friendlyMessage);
                throw new FirebaseAuthException(friendlyMessage, firebaseCode, response.statusCode());
            }
            return body;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Firebase Authentication {} request failed before receiving a valid response ({})",
                    operation, ex.getClass().getSimpleName());
            throw new IllegalStateException("Firebase Authentication is unavailable", ex);
        }
    }

    private static String messageFor(String code, Operation operation, int status) {
        if (code.contains("EMAIL_EXISTS")) {
            return "An account already exists for this email. Log in instead, or sign up with another email.";
        }
        if (code.contains("WEAK_PASSWORD")) return "Choose a password with at least 6 characters.";
        if (code.contains("INVALID_EMAIL") || code.contains("MISSING_EMAIL")) return "Enter a valid email address.";
        if (code.contains("OPERATION_NOT_ALLOWED") || code.contains("ADMIN_ONLY_OPERATION")
                || code.contains("CONFIGURATION_NOT_FOUND")) {
            return "Email and password sign-in is not enabled for this Firebase project. Enable the Email/Password provider in Firebase Authentication settings.";
        }
        if (code.contains("INVALID_API_KEY") || code.contains("API_KEY_INVALID")) {
            return "Firebase rejected the configured API key. Check FIREBASE_WEB_API_KEY and confirm it belongs to this Firebase project.";
        }
        if (operation == Operation.SIGN_IN && (code.contains("INVALID_PASSWORD") || code.contains("EMAIL_NOT_FOUND")
                || code.contains("INVALID_LOGIN_CREDENTIALS") || code.contains("USER_DISABLED"))) {
            return "Email or password is incorrect.";
        }
        if (operation == Operation.REFRESH || operation == Operation.LOOKUP) return "Your session expired. Please log in again.";
        if (operation == Operation.SIGN_UP) {
            return "Firebase could not create the account (" + (code.isBlank() ? "HTTP " + status : code)
                    + "). Check the Firebase project and Email/Password provider setup.";
        }
        return "Email or password is incorrect.";
    }

    private enum Operation { SIGN_UP, SIGN_IN, REFRESH, LOOKUP }

    private String normalizeEmail(AuthController.Credentials credentials) {
        if (credentials == null || credentials.email() == null) throw new IllegalArgumentException("Email is required");
        String email = credentials.email().strip();
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return email;
    }

    private String password(AuthController.Credentials credentials) {
        if (credentials.password() == null || credentials.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        return credentials.password();
    }

    private AuthController.AuthResponse response(JsonNode result, String email) {
        return new AuthController.AuthResponse(result.path("idToken").asText(), result.path("refreshToken").asText(),
                email, result.path("expiresIn").asLong());
    }
}
