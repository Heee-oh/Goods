package com.goods.market.review.application;

import com.goods.market.review.domain.ReviewRepository;
import com.goods.market.trade.application.TradeQueryService;
import com.goods.market.trade.application.dto.ReviewableTrade;
import com.goods.market.review.domain.Review;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
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
class ReviewCommandServiceTest {

    @Mock
    private TradeQueryService tradeQueryService;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewCommandService reviewCommandService;

    @Test
    void createReviewRejectsDuplicatePair() {
        ReviewableTrade trade = new ReviewableTrade(true, 20L, 100L, 200L);
        when(tradeQueryService.getReviewableTrade(10L)).thenReturn(trade);
        when(reviewRepository.existsByTradeIdAndWriterId(10L, 200L)).thenReturn(true);

        assertThatThrownBy(() -> reviewCommandService.create(10L, 200L, 5, "good"))
                .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    void createReviewStoresNewReview() {
        ReviewableTrade trade = new ReviewableTrade(true, 20L, 100L, 200L);
        when(tradeQueryService.getReviewableTrade(10L)).thenReturn(trade);
        when(reviewRepository.existsByTradeIdAndWriterId(10L, 200L)).thenReturn(false);

        reviewCommandService.create(10L, 200L, 5, "good");

        verify(reviewRepository).save(ArgumentMatchers.any(Review.class));
    }
}
