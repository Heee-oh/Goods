package com.goods.market.common.auth.presentation.dto.request;

import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import jakarta.validation.constraints.NotBlank;

public record AuthSignupRequest(
        @NotBlank String phoneNumber,
        String nickname,
        Integer regionId
) {
    public AuthSignupCommand toCommand() {
        return new AuthSignupCommand(phoneNumber, nickname, regionId);
    }
}

