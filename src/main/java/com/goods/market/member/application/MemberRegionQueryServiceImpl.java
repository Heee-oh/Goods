package com.goods.market.member.application;


import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionNotFoundException;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberRegionQueryServiceImpl implements MemberRegionQueryService {

    private final MemberRegionJpaRepository memberRegionJpaRepository;

    /**
     * 인증 만료 혹은 동네 인증 데이터가 없다면 예외 반환
     * @param id
     */
    @Override
    public void validateMemberRegion(Long id, Long memberId) {
        MemberRegion memberRegion = memberRegionJpaRepository.findMemberRegionByIdAndMemberId(id, memberId)
                .orElseThrow(MemberRegionNotFoundException::new);

        memberRegion.checkVerification();
    }
}
