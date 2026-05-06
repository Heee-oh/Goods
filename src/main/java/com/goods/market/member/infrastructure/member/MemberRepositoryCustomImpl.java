package com.goods.market.member.infrastructure.member;

import com.goods.market.member.application.dto.MemberRegionResponse;
import com.goods.market.member.application.dto.MemberResponse;
import com.goods.market.member.application.dto.QMemberRegionResponse;
import com.goods.market.member.application.dto.QMemberResponse;
import com.goods.market.member.domain.MemberStatus;
import com.goods.market.member.domain.QMember;
import com.goods.market.member.domain.QMemberRegion;
import com.goods.market.region.domain.QRegion;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QMember qMember = QMember.member;
    private final QMemberRegion memberRegion = QMemberRegion.memberRegion;
    private final QRegion region = QRegion.region;

    @Override
    public MemberResponse findMember(Long memberId) {
        MemberResponse result = queryFactory
                .select(new QMemberResponse(
                        qMember.nickname,
                        qMember.profileImageUrl,
                        qMember.smileScore
                ))
                .from(qMember)
                .where(qMember.id.eq(memberId),
                        qMember.status.eq(MemberStatus.ACTIVE))
                .fetchOne();

        if (result == null) {
            throw new EntityNotFoundException("Member not found");
        }

        return result;
    }
    @Override
    public List<MemberRegionResponse> findMemberRegion(Long memberId) {
        return queryFactory.select(new QMemberRegionResponse(
                        memberRegion.member.id,
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
