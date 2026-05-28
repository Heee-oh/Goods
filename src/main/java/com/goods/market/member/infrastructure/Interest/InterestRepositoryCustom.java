package com.goods.market.member.infrastructure.Interest;

import com.goods.market.member.application.dto.InterestDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface InterestRepositoryCustom {
    Slice<InterestDto> findAllByMemberId(Long memberId, Long lastInterestId, Pageable pageable);
}
