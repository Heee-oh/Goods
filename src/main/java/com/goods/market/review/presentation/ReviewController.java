package com.goods.market.review.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.review.application.ReviewCommandService;
import com.goods.market.review.application.ReviewQueryService;
import com.goods.market.review.presentation.dto.request.ReviewCreateRequest;
import com.goods.market.review.presentation.dto.response.ReviewHistoryItemResponse;
import com.goods.market.review.presentation.dto.response.ReviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    @GetMapping
    public ResponseEntity<?> findAllReviews(@AuthenticationPrincipal AuthPrincipal principal,
                                            HttpServletRequest request,
                                             @RequestParam(name = "last_review_id", required = false) Long lastReviewId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewQueryService
                                .findReviewsHistoryItems(principal.memberId(), lastReviewId == null ? Long.MAX_VALUE : lastReviewId)
                                .map(ReviewHistoryItemResponse::from),
                        request.getRequestURI()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @Valid @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ReviewResponse.from(reviewCommandService.create(
                                reviewCreateRequest.tradeId(),
                                principal.memberId(),
                                reviewCreateRequest.rating(),
                                reviewCreateRequest.comment()
                        )),
                        request.getRequestURI()
                ));
    }
}
