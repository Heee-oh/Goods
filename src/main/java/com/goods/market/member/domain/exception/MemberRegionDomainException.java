package com.goods.market.member.domain.exception;

public class MemberRegionDomainException extends RuntimeException {

    public MemberRegionDomainException() {
    }

    public MemberRegionDomainException(String message) {
        super(message);
    }

    public MemberRegionDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberRegionDomainException(Throwable cause) {
        super(cause);
    }
}
