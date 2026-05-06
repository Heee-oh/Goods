package com.goods.market.common.auth.presentation.dto.request;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank String phoneNumber
) {
    public AuthLoginCommand toCommand() {
        return new AuthLoginCommand(phoneNumber);
    }
}

