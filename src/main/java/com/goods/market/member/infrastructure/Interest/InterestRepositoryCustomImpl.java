package com.goods.market.member.infrastructure.Interest;

import com.goods.market.member.application.dto.InterestResponse;
import com.goods.market.member.application.dto.QInterestResponse;
import com.goods.market.member.domain.QInterest;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class InterestRepositoryCustomImpl implements InterestRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QInterest qInterest = QInterest.interest;


    @Override
    public Slice<InterestResponse> findAllByMemberId(Long memberId, Long lastInterestId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<InterestResponse> contents = queryFactory
                .select(new QInterestResponse(
                        qInterest.id,
                        qInterest.listingId
                ))
                .from(qInterest)
                .where(qInterest.member.id.eq(memberId),
                        qInterest.id.lt(lastInterestId) // 최신순으로 non-offset
                )
                .orderBy(qInterest.id.desc())
                .limit(pageSize + 1L)
                .fetch();

        boolean hasNext = contents.size() > pageSize;

        if (hasNext) {
            contents.remove(pageSize);
        }

        return new SliceImpl<>(contents, pageable, hasNext);
    }
}
