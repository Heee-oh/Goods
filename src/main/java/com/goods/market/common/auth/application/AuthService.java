package com.goods.market.common.auth.application;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenResponse;

public interface AuthService {
    AuthTokenResponse signup(AuthSignupCommand command);

    AuthTokenResponse login(AuthLoginCommand command);
}

