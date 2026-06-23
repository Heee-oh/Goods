package com.goods.market.review.presentation;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.common.presentation.GlobalExceptionHandler;
import com.goods.market.review.application.ReviewCommandService;
import com.goods.market.review.domain.Review;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewCommandService reviewCommandService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new ReviewExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void createReviewReturnsCreatedReview() throws Exception {
        when(reviewCommandService.create(10L, 100L, 5, "good"))
                .thenReturn(Review.create(10L, 20L, 100L, 200L,  5, "good"));

        mockMvc.perform(post("/api/reviews")
                        .with(authenticated(100L))
                        .contentType("application/json")
                        .content("""
                                {
                                  "tradeId": 10,
                                  "rating": 5,
                                  "comment": "good"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tradeId").value(10L))
                .andExpect(jsonPath("$.data.targetId").value(200L));
    }

    @Test
    void createReviewReturnsConflictWhenDuplicateExists() throws Exception {
        when(reviewCommandService.create(10L, 100L, 5, "good"))
                .thenThrow(new ReviewAlreadyExistsException("리뷰가 이미 존재합니다."));

        mockMvc.perform(post("/api/reviews")
                        .with(authenticated(100L))
                        .contentType("application/json")
                        .content("""
                                {
                                  "tradeId": 10,
                                  "rating": 5,
                                  "comment": "good"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REVIEW_ALREADY_EXISTS"));
    }

    private RequestPostProcessor authenticated(Long memberId) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(new AuthPrincipal(memberId), null));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
