package com.goods.market.member.application;

import com.goods.market.member.application.dto.InterestDto;
import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.application.dto.MemberRegionDto;
import com.goods.market.member.infrastructure.Interest.InterestJpaRepository;
import com.goods.market.member.infrastructure.Interest.InterestRepositoryCustom;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.member.MemberRepositoryCustom;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRegionJpaRepository memberRegionJpaRepository;
    private final InterestJpaRepository interestJpaRepository;
    public MemberDto getMe(Long memberId) {
        return memberJpaRepository.findMember(memberId);
    }
    public List<MemberRegionDto> getMyRegions(Long memberId) {
        return memberRegionJpaRepository.findAllByMember(memberId);
    }
    public Slice<InterestDto> getMyInterests(Long memberId, Long lastInterestId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size);
        return interestJpaRepository.findAllByMemberId(memberId, lastInterestId, pageRequest);
    }
    public boolean isInterested(Long memberId, Long listingId) {
        return interestJpaRepository.existsByListingIdAndMemberId(listingId, memberId);
    }
}
