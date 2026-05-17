package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.AuthDtos.AuthTokensResponse;
import com.cozy.notebooks.api.dto.AuthDtos.LoginRequest;
import com.cozy.notebooks.api.dto.AuthDtos.LogoutRequest;
import com.cozy.notebooks.api.dto.AuthDtos.MeResponse;
import com.cozy.notebooks.api.dto.AuthDtos.RefreshRequest;
import com.cozy.notebooks.api.dto.AuthDtos.RefreshTokensResponse;
import com.cozy.notebooks.api.dto.AuthDtos.RegisterRequest;
import com.cozy.notebooks.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthTokensResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.registerWithEmail(request, clientMeta(httpRequest));
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.loginWithEmail(request, clientMeta(httpRequest));
    }

    @PostMapping("/refresh")
    public RefreshTokensResponse refresh(@Valid @RequestBody RefreshRequest request,
                                         HttpServletRequest httpRequest) {
        return authService.refreshToken(request, clientMeta(httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.getCurrentUserMe();
    }

    private static AuthService.ClientMeta clientMeta(HttpServletRequest request) {
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        String ip = request.getRemoteAddr();
        return new AuthService.ClientMeta(truncate(ua, 512), truncate(ip, 64));
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
