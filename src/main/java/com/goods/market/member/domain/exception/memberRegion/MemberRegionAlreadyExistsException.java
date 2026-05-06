package com.goods.market.member.domain.exception.memberRegion;

public class MemberRegionAlreadyExistsException extends RuntimeException {

    public MemberRegionAlreadyExistsException() {
        super("이미 추가된 동네입니다.");
    }
}
