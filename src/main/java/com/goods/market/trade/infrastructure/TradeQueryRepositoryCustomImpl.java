package com.goods.market.trade.infrastructure;

import com.goods.market.listing.domain.QListing;
import com.goods.market.listing.domain.QListingImage;
import com.goods.market.member.domain.QMember;
import com.goods.market.review.domain.QReview;
import com.goods.market.trade.application.dto.*;
import com.goods.market.trade.domain.QTrade;
import com.goods.market.trade.domain.TradeStatus;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeQueryRepositoryCustomImpl implements TradeQueryRepositoryCustom {

    private final JPAQueryFactory factory;
    private static final QTrade qTrade = QTrade.trade;
    private static final QListing qListing = QListing.listing;
    private static final QListingImage qListingImage = QListingImage.listingImage;
    private static final QMember qMember = QMember.member;
    private static final QReview qReview = QReview.review;

    @Override
    public Slice<SaleHistoryItemDto> findCompletedSalesBySellerId(Long sellerId, Long lastTradeId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<SaleHistoryItemDto> fetch = factory.select(new QSaleHistoryItemDto(
                        qTrade.id,
                        qTrade.listingId,
                        qListingImage.imageUrl,
                        qListing.title,
                        qTrade.price,
                        qTrade.buyerId,
                        qMember.nickname,
                        qTrade.completedAt
                ))
                .from(qTrade)
                .join(qListing).on(qTrade.listingId.eq(qListing.id))
                .join(qMember).on(qTrade.buyerId.eq(qMember.id)) // 구매자 정보
                .leftJoin(qListingImage).on(
                        qListingImage.listing.id.eq(qListing.id)
                                .and(qListingImage.sortOrder.eq(0))
                )
                .where(qTrade.sellerId.eq(sellerId),
                        qTrade.id.lt(lastTradeId),
                        qTrade.status.eq(TradeStatus.COMPLETED))
                .orderBy(qTrade.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = fetch.size() > pageSize;
        if (hasNext) {
            fetch.removeLast();
        }

        return new SliceImpl<>(fetch, pageable, hasNext);
    }

    @Override
    public Slice<PurchaseHistoryItemDto> findCompletedPurchasesByBuyerId(Long buyerId, Long lastTradeId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<PurchaseHistoryItemDto> fetch = factory.select(new QPurchaseHistoryItemDto(
                        qTrade.id,
                        qTrade.listingId,
                        qListingImage.imageUrl,
                        qListing.title,
                        qTrade.price,
                        qTrade.sellerId,
                        qMember.nickname,
                        qTrade.completedAt,
                        JPAExpressions.selectOne()
                                .from(qReview)
                                .where(
                                        qReview.tradeId.eq(qTrade.id),
                                        qReview.writerId.eq(qTrade.buyerId),
                                        qReview.targetId.eq(qTrade.sellerId)
                                )
                                .exists()
                ))
                .from(qTrade)
                .join(qListing).on(qTrade.listingId.eq(qListing.id))
                .join(qMember).on(qTrade.sellerId.eq(qMember.id)) // 판매자 정보
                .leftJoin(qListingImage).on(
                        qListingImage.listing.id.eq(qListing.id)
                                .and(qListingImage.sortOrder.eq(0))
                )
                .where(qTrade.buyerId.eq(buyerId),
                        qTrade.id.lt(lastTradeId),
                        qTrade.status.eq(TradeStatus.COMPLETED))
                .orderBy(qTrade.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = fetch.size() > pageSize;
        if (hasNext) {
            fetch.removeLast();
        }

        return new SliceImpl<>(fetch, pageable, hasNext);
    }

    @Override
    public Optional<ReviewableTrade> findReviewableTradeById(Long tradeId) {
        ReviewableTrade reviewableTrade = factory.select(new QReviewableTrade(
                        qTrade.status.eq(TradeStatus.COMPLETED),
                        qTrade.listingId,
                        qTrade.sellerId,
                        qTrade.buyerId
                )).from(qTrade)
                .where(qTrade.id.eq(tradeId))
                .fetchFirst();
        return Optional.ofNullable(reviewableTrade);
    }
}
