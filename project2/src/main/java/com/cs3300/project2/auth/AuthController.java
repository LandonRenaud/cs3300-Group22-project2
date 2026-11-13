package com.cs3300.project2.auth;

import com.cs3300.project2.common.UserContext;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("!test")
@RequestMapping("/api/auth")
public class AuthController {
    private final FirebaseIdentityService identity;
    public AuthController(FirebaseIdentityService identity) { this.identity = identity; }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody Credentials credentials) { return identity.register(credentials); }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody Credentials credentials) { return identity.login(credentials); }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) { return identity.refresh(request.refreshToken()); }

    @GetMapping("/me")
    public UserInfo me() { return new UserInfo(UserContext.requireUid()); }

    public record Credentials(String email, String password) { }
    public record RefreshRequest(String refreshToken) { }
    public record AuthResponse(String idToken, String refreshToken, String email, long expiresIn) { }
    public record UserInfo(String uid) { }
}
