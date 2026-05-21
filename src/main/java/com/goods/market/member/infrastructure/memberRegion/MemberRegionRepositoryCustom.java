package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.application.dto.MemberRegionDto;

import java.util.List;

public interface MemberRegionRepositoryCustom {
    List<MemberRegionDto> findAllByMember(Long memberId);
}
