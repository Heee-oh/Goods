package com.goods.market.trade.infrastructure;

import com.goods.market.listing.domain.QListing;
import com.goods.market.listing.domain.QListingImage;
import com.goods.market.member.domain.QMember;
import com.goods.market.trade.application.dto.PurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.QPurchaseHistoryItemDto;
import com.goods.market.trade.application.dto.QSaleHistoryItemDto;
import com.goods.market.trade.application.dto.SaleHistoryItemDto;
import com.goods.market.trade.domain.QTrade;
import com.goods.market.trade.domain.TradeStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeQueryRepositoryCustomImpl implements TradeQueryRepositoryCustom {

    private final JPAQueryFactory factory;
    private static final QTrade qTrade = QTrade.trade;
    private static final QListing qListing = QListing.listing;
    private static final QListingImage qListingImage = QListingImage.listingImage;
    private static final QMember qMember = QMember.member;

    @Override
    public Slice<SaleHistoryItemDto> findCompletedSalesBySellerId(Long sellerId, Long lastTradeId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<SaleHistoryItemDto> fetch = factory.select(new QSaleHistoryItemDto(
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
                        qTrade.listingId,
                        qListingImage.imageUrl,
                        qListing.title,
                        qTrade.price,
                        qTrade.sellerId,
                        qMember.nickname,
                        qTrade.completedAt
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
}
