package com.goods.market.member.infrastructure.member;

import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.application.dto.MemberRegionDto;

import java.util.List;

public interface MemberRepositoryCustom {
    MemberDto findMember(Long memberId);
    List<MemberRegionDto> findMemberRegion(Long memberId);
}
