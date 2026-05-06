package com.goods.market.member.domain.exception.memberRegion;

public class MemberRegionLimitExceededException extends RuntimeException {

    public MemberRegionLimitExceededException() {
        super("동네는 최대 2개까지 추가할 수 있습니다.");
    }
}
