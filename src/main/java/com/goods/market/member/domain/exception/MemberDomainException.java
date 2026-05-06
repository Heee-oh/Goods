package com.goods.market.member.domain.exception;

public class MemberDomainException extends RuntimeException {

    public MemberDomainException() {
    }

    public MemberDomainException(String message) {
        super(message);
    }

    public MemberDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberDomainException(Throwable cause) {
        super(cause);
    }
}
