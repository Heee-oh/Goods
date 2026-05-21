package com.goods.market.common.auth.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.application.AuthService;
import com.goods.market.common.auth.application.dto.AuthTokenDto;
import com.goods.market.common.auth.presentation.dto.request.AuthLoginRequest;
import com.goods.market.common.auth.presentation.dto.request.AuthSignupRequest;
import com.goods.market.common.auth.presentation.dto.response.AuthTokenResponse;
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
        AuthTokenDto token = authService.signup(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AuthTokenResponse.from(token), httpRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AuthLoginRequest request
    ) {
        AuthTokenDto login = authService.login(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success(AuthTokenResponse.from(login), httpRequest.getRequestURI()));
    }
}
