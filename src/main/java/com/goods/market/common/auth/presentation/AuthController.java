package com.goods.market.common.auth.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.application.AuthService;
import com.goods.market.common.auth.application.dto.AuthTokenResponse;
import com.goods.market.common.auth.presentation.dto.request.AuthLoginRequest;
import com.goods.market.common.auth.presentation.dto.request.AuthSignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AuthSignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.signup(request.toCommand()), httpRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AuthLoginRequest request
    ) {
        AuthTokenResponse login = authService.login(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success(login, httpRequest.getRequestURI()));
    }
}

