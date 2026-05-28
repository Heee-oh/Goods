package com.goods.market.member.infrastructure.member;

import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.application.dto.MemberRegionDto;
import com.goods.market.member.application.dto.QMemberDto;
import com.goods.market.member.application.dto.QMemberRegionDto;
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
    public MemberDto findMember(Long memberId) {
        MemberDto result = queryFactory
                .select(new QMemberDto(
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
    public List<MemberRegionDto> findMemberRegion(Long memberId) {
        return queryFactory.select(new QMemberRegionDto(
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
