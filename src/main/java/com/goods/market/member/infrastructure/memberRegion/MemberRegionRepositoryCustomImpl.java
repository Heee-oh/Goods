package com.goods.market.member.infrastructure.memberRegion;

import com.goods.market.member.application.dto.MemberRegionResponse;
import com.goods.market.member.application.dto.QMemberRegionResponse;
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
    public List<MemberRegionResponse> findAllByMember(Long memberId) {

        return queryFactory
                .select(new QMemberRegionResponse(
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
                .where(memberRegion.member.id.eq(memberId),
                        memberRegion.primary.isTrue())
                .fetch();
    }
}
