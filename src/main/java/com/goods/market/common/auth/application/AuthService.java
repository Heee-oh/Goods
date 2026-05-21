package com.goods.market.common.auth.application;

import com.goods.market.common.auth.application.dto.AuthLoginCommand;
import com.goods.market.common.auth.application.dto.AuthSignupCommand;
import com.goods.market.common.auth.application.dto.AuthTokenDto;

public interface AuthService {
    AuthTokenDto signup(AuthSignupCommand command);

    AuthTokenDto login(AuthLoginCommand command);
}
