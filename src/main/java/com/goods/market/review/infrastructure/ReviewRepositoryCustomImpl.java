package com.goods.market.review.infrastructure;

import com.goods.market.listing.domain.QListing;
import com.goods.market.listing.domain.QListingImage;
import com.goods.market.review.application.dto.QReviewHistoryItemDto;
import com.goods.market.review.application.dto.ReviewHistoryItemDto;
import com.goods.market.review.domain.QReview;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory factory;
    private static final QReview review = QReview.review;
    private static final QListing listing = QListing.listing;
    private static final QListingImage listingImage = QListingImage.listingImage;



    // TODO 거래글의 타이틀과 거래종류도 필요함
    @Override
    @Transactional(readOnly = true)
    public Slice<ReviewHistoryItemDto> findReviewHistoryItems(Long memberId, Long lastReviewId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ReviewHistoryItemDto> fetch = factory
                .select(
                    new QReviewHistoryItemDto(
                                review.id,
                                review.comment,
                                review.rating,
                                review.listingId,
                                listingImage.imageUrl
                    )
                ).from(review)
                .leftJoin(listingImage)
                .on(review.listingId.eq(listingImage.listing.id)
                        .and(listingImage.sortOrder.eq(0))
                )
                .where(review.id.lt(lastReviewId),
                        review.targetId.eq(memberId))
                .orderBy(review.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = fetch.size() > pageSize;
        if (hasNext) {
            fetch.removeLast();
        }

        return new SliceImpl<>(fetch, pageable, hasNext);
    }
}
