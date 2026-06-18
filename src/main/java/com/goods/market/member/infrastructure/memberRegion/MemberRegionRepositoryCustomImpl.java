package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.application.dto.MemberRegionDto;
import com.goods.market.member.application.dto.QMemberRegionDto;
import com.goods.market.member.domain.QMemberRegion;
import com.goods.market.region.domain.QRegion;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberRegionRepositoryCustomImpl implements MemberRegionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QMemberRegion memberRegion = QMemberRegion.memberRegion;
    private final QRegion region = QRegion.region;

    @Override
    public List<MemberRegionDto> findAllByMember(Long memberId) {

        return queryFactory
                .select(new QMemberRegionDto(
                        memberRegion.id,
                        memberRegion.regionId,
                        memberRegion.verifiedAt,
                        memberRegion.primary,
                        region.dongnm,
                        memberRegion.lat,
                        memberRegion.lng
                ))
                .from(memberRegion)
                .join(region).on(memberRegion.regionId.eq(region.id))
                .where(memberRegion.memberId.eq(memberId),
                        memberRegion.primary.isTrue())
                .fetch();
    }
}
