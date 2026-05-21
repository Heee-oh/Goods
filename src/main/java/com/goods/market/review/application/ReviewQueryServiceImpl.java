package com.goods.market.review.application;

import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.review.application.dto.ReviewPromptDto;
import com.goods.market.review.domain.Review;
import com.goods.market.review.infrastructure.ReviewRepository;
import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TradeStatus;
import com.goods.market.trade.infrastructure.TradeRepository;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final TradeRepository tradeRepository;
    private final ListingJpaRepository listingJpaRepository;
    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Review> findByTradeIdAndWriterIdAndTargetId(Long tradeId, Long writerId, Long targetId) {
        return reviewRepository.findByTradeIdAndWriterIdAndTargetId(tradeId, writerId, targetId);
    }

    // TODO 위험함 메모리 터질 수 있음 내일 고치자
    @Override
    public Optional<ReviewPromptDto> getReviewPrompt(Long memberId) {
        List<Trade> completedPurchases = tradeRepository.findByBuyerIdAndStatusOrderByCompletedAtDesc(
                memberId,
                TradeStatus.COMPLETED
        );

        for (Trade trade : completedPurchases) {
            if (reviewRepository.existsByTradeIdAndWriterIdAndTargetId(trade.getId(), memberId, trade.getSellerId())) {
                continue;
            }

            String listingTitle = listingJpaRepository.findByIdAndDeletedAtIsNull(trade.getListingId())
                    .map(listing -> listing.getTitle() == null ? "" : listing.getTitle())
                    .orElse("");
            String partnerNickname = memberJpaRepository.findById(trade.getSellerId())
                    .map(Member::getNickname)
                    .orElse("상대방");

            return Optional.of(new ReviewPromptDto(
                    trade.getId(),
                    listingTitle,
                    partnerNickname,
                    false
            ));
        }

        return Optional.empty();
    }
}
