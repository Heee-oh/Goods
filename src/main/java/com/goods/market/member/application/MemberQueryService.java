package com.goods.market.member.application;

import com.goods.market.member.application.dto.InterestResponse;
import com.goods.market.member.application.dto.MemberRegionResponse;
import com.goods.market.member.application.dto.MemberResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface MemberQueryService {
    MemberResponse getMe(Long memberId);
    List<MemberRegionResponse> getMyRegions(Long memberId);
    Slice<InterestResponse> getMyInterests(Long memberId, Long lastInterestId,int size);
    boolean isInterested(Long memberId, Long listingId);
}
