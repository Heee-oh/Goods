package com.goods.market.member.application;


import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionNotFoundException;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberRegionQueryServiceImpl implements MemberRegionQueryService {

    private final MemberRegionJpaRepository memberRegionJpaRepository;

    /**
     * 인증 만료 혹은 동네 인증 데이터가 없다면 예외 반환
     */
    @Override
    public void validateMemberRegionByRegionId(Long memberId, Integer regionId) {
        MemberRegion memberRegion = memberRegionJpaRepository
                .findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNullAndPrimaryTrue(memberId, regionId)
                .orElseThrow(MemberRegionNotFoundException::new);

        memberRegion.checkVerification();

        log.info("Member Region verification has been successfully completed");
    }
}
