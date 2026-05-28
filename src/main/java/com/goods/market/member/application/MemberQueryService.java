package com.goods.market.member.application;

import com.goods.market.member.application.dto.InterestDto;
import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.application.dto.MemberRegionDto;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface MemberQueryService {
    MemberDto getMe(Long memberId);
    List<MemberRegionDto> getMyRegions(Long memberId);
    Slice<InterestDto> getMyInterests(Long memberId, Long lastInterestId,int size);
    boolean isInterested(Long memberId, Long listingId);
}
