package com.goods.market.listing.infrastructure;

import com.goods.market.listing.application.dto.ListingResponse;
import com.goods.market.listing.application.dto.QListingResponse;
import com.goods.market.listing.domain.QListing;
import com.goods.market.listing.domain.QListingImage;
import com.goods.market.listing.domain.Status;
import com.goods.market.region.domain.QRegion;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class ListingJpaRepositoryCustomImpl implements ListingJpaRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    private static final QListing listing = QListing.listing;
    private static final QRegion region = QRegion.region;
    private static final QListingImage image = QListingImage.listingImage;
    private static final QRegion base = new QRegion("base");


    /**
     * 내 행정동 3KM 범위 내 판매글 조회 (작성중 x, 슬라이스)
     * @param memberId 본인 id
     * @param regionId 해당 멤버의 행정동 id
     * @param lastListingId
     * @param size
     * @param pageable
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Slice<ListingResponse> findListings(
            Long memberId,
            Integer regionId,
            BigDecimal originLat,
            BigDecimal originLng,
            Long lastListingId,
            String transactionType,
            Long sellerId,
            int size,
            Pageable pageable
    ) {
        // 반경 7~10km 주의의 행정동을 뽑고,
        // 해당 행정동의 게시글들을 조회

        // 행정동의 3KM 반경의 행정동 판별
        BooleanExpression isWithin7Km = Expressions.booleanTemplate(
                "function('ST_DWithin', {0}, {1}, {2}) = true",
                region.geom,
                base.geom,
                3000.0d
        );
        NumberExpression<Double> distanceKm = distanceInKm(originLat, originLng);
        StringExpression resolvedTransactionType = Expressions.stringTemplate(
                "coalesce(lower({0}), case when {1} = 0 then 'free' else 'sell' end)",
                listing.price.transactionType,
                listing.price.priceAmount
        );

        List<ListingResponse> fetch
                = queryFactory.select(new QListingResponse(
                        listing.id,
                        listing.sellerId,
                        listing.title,
                        listing.categoryId,
                        listing.price.priceAmount,
                        resolvedTransactionType,
                        listing.status,
                        region.dongnm,
                        listing.hopeLocation.regionId,
                        listing.hopeLocation.lat,
                        listing.hopeLocation.lng,
                        listing.originRegionId,
                        listing.originLat,
                        listing.originLng,
                        listing.viewCount,
                        listing.viewCount,
                        image.imageUrl,
                        listing.updatedAt,
                        distanceKm
                ))
                .from(listing)
                .leftJoin(image)
                .on(listing.id.eq(image.listing.id)
                        .and(image.sortOrder.isNull()
                                .or(image.sortOrder.eq(0)))
                )
                .join(region).on(region.id.eq(listing.regionId))
                .join(base).on(base.id.eq(regionId))
                .where(
                        isWithin7Km,
                        matchSeller(memberId, sellerId),
                        matchesTransactionType(transactionType, resolvedTransactionType),
                        listing.id.lt(lastListingId),
                        listing.isHidden.isFalse(),
                        listing.status.ne(Status.DRAFT)
                )
                .orderBy(listing.updatedAt.desc())
                .limit(size + 1)
                .fetch();


        int pageSize = pageable.getPageSize();
        boolean hasNext = fetch.size() > pageSize;

        if (hasNext) {
            fetch.removeLast();
        }

        return new SliceImpl<>(fetch, pageable, hasNext);
    }

    private NumberExpression<Double> distanceInKm(BigDecimal originLat, BigDecimal originLng) {
        NumberExpression<BigDecimal> listingLat = effectiveLat();
        NumberExpression<BigDecimal> listingLng = effectiveLng();

        return Expressions.numberTemplate(
                Double.class,
                """
                        case
                            when {0} is null or {1} is null or {2} is null or {3} is null then null
                            else (6371.0 * acos(least(1, greatest(-1,
                                cos(radians({0})) * cos(radians({2})) * cos(radians({3}) - radians({1}))
                                + sin(radians({0})) * sin(radians({2}))
                            ))))
                        end
                        """,
                originLat,
                originLng,
                listingLat,
                listingLng
        );
    }

    private NumberExpression<BigDecimal> effectiveLat() {
        return Expressions.numberTemplate(
                BigDecimal.class,
                "coalesce({0}, {1})",
                listing.originLat,
                listing.hopeLocation.lat
        );
    }

    private NumberExpression<BigDecimal> effectiveLng() {
        return Expressions.numberTemplate(
                BigDecimal.class,
                "coalesce({0}, {1})",
                listing.originLng,
                listing.hopeLocation.lng
        );
    }

    private BooleanExpression matchSeller(Long memberId, Long sellerId) {
        if (sellerId != null) {
            return listing.sellerId.eq(sellerId);
        }

        return memberId == null ? null : listing.sellerId.ne(memberId);
    }

    private BooleanExpression matchesTransactionType(String transactionType, StringExpression resolvedTransactionType) {
        if (transactionType == null || transactionType.isBlank()) {
            return null;
        }

        String normalized = transactionType.trim().toLowerCase();
        return resolvedTransactionType.eq(normalized);
    }



}
