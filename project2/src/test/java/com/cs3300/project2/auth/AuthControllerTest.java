package com.cs3300.project2.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthControllerTest {

    private final FirebaseIdentityService identity = mock(FirebaseIdentityService.class);
    private final AuthController controller = new AuthController(identity);
    private final AuthController.Credentials credentials = new AuthController.Credentials("test@test.com", "test-password");

    @Test
    void registerReturnsTheServiceResponse() {
        var response = new AuthController.AuthResponse(
                "test-id-token", "test-refresh-token", credentials.email(), 3600);

        when(identity.register(credentials)).thenReturn(response);

        assertEquals(response, controller.register(credentials));
        verify(identity).register(credentials);
    }

    @Test
    void loginReturnsTheServiceResponse() {
        var response = new AuthController.AuthResponse(
                "test-id-token", "test-refresh-token", credentials.email(), 3600);

        when(identity.login(credentials)).thenReturn(response);

        assertEquals(response, controller.login(credentials));
        verify(identity).login(credentials);
    }

}
