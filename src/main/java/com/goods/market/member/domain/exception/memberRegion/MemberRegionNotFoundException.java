package com.goods.market.member.domain.exception.memberRegion;

import com.goods.market.member.domain.exception.MemberRegionDomainException;

public class MemberRegionNotFoundException extends MemberRegionDomainException {
    public MemberRegionNotFoundException() {
        super("memberRegion Not Found");
    }

    public MemberRegionNotFoundException(String message) {
        super(message);
    }

    public MemberRegionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberRegionNotFoundException(Throwable cause) {
        super(cause);
    }
}
