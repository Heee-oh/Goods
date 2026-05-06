package com.goods.market.review.application;

import com.goods.market.trade.domain.Trade;
import com.goods.market.trade.domain.TradeStatus;
import com.goods.market.trade.infrastructure.TradeRepository;
import com.goods.market.review.domain.Review;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
import com.goods.market.review.infrastructure.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceImplTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewCommandServiceImpl reviewCommandService;

    @Test
    void createReviewRejectsDuplicatePair() {
        Trade trade = tradeCompleted(100L, 200L);
        when(tradeRepository.findById(10L)).thenReturn(java.util.Optional.of(trade));
        when(reviewRepository.existsByTradeIdAndWriterIdAndTargetId(10L, 100L, 200L)).thenReturn(true);

        assertThatThrownBy(() -> reviewCommandService.create(10L, 100L, true, 5, "good"))
                .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    void createReviewStoresNewReview() {
        Trade trade = tradeWithListing(20L, 100L, 200L);
        when(tradeRepository.findById(10L)).thenReturn(java.util.Optional.of(trade));
        when(reviewRepository.existsByTradeIdAndWriterIdAndTargetId(10L, 100L, 200L)).thenReturn(false);

        reviewCommandService.create(10L, 100L, true, 5, "good");

        verify(reviewRepository).save(ArgumentMatchers.any(Review.class));
    }

    private Trade tradeCompleted(Long sellerId, Long buyerId) {
        Trade trade = org.mockito.Mockito.mock(Trade.class);
        when(trade.getSellerId()).thenReturn(sellerId);
        when(trade.getBuyerId()).thenReturn(buyerId);
        when(trade.getStatus()).thenReturn(TradeStatus.COMPLETED);
        return trade;
    }

    private Trade tradeWithListing(Long listingId, Long sellerId, Long buyerId) {
        Trade trade = tradeCompleted(sellerId, buyerId);
        when(trade.getListingId()).thenReturn(listingId);
        return trade;
    }
}
