package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.application.dto.MemberRegionResponse;

import java.util.List;

public interface MemberRegionRepositoryCustom {
    List<MemberRegionResponse> findAllByMember(Long memberId);
}
