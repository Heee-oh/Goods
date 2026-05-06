package com.goods.market.member.domain.exception.memberRegion;

import com.goods.market.member.domain.exception.MemberRegionDomainException;

public class RegionVerificationExpiredException extends MemberRegionDomainException {
    public RegionVerificationExpiredException() {
    }

    public RegionVerificationExpiredException(String message) {
        super(message);
    }

    public RegionVerificationExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegionVerificationExpiredException(Throwable cause) {
        super(cause);
    }
}
