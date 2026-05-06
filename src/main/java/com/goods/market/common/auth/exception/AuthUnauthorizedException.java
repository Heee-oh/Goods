package com.goods.market.common.auth.exception;

public class AuthUnauthorizedException extends RuntimeException {
    public AuthUnauthorizedException(String message) {
        super(message);
    }
}

