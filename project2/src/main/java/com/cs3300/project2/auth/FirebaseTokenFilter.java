package com.cs3300.project2.auth;

import com.cs3300.project2.common.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Profile("!test")
public class FirebaseTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final FirebaseIdentityService identity;

    public FirebaseTokenFilter(FirebaseIdentityService identity) {
        this.identity = identity;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/") || path.equals("/api/auth/login")
                || path.equals("/api/auth/register") || path.equals("/api/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sign in to continue");
            return;
        }
        String idToken = header.substring(7);
        final String uid;
        try {
            uid = identity.verifyIdToken(idToken);
        } catch (Exception ex) {
            log.warn("Firebase ID token validation failed: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Your session is invalid or expired");
            return;
        }
        UserContext.set(uid, idToken);
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
