package com.goods.market.review.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.review.application.ReviewCommandService;
import com.goods.market.review.application.ReviewQueryService;
import com.goods.market.review.presentation.dto.request.ReviewCreateRequest;
import com.goods.market.review.presentation.dto.response.ReviewPromptResponse;
import com.goods.market.review.presentation.dto.response.ReviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    @GetMapping("/review-prompt")
    public ResponseEntity<?> getReviewPrompt(@AuthenticationPrincipal AuthPrincipal principal) {
        Optional<com.goods.market.review.application.dto.ReviewPromptDto> prompt =
                reviewQueryService.getReviewPrompt(principal.memberId());
        return prompt.<ResponseEntity<?>>map(dto -> ResponseEntity.ok(ReviewPromptResponse.from(dto)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{trade_id}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @PathVariable("trade_id") Long tradeId,
            @Valid @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ReviewResponse.from(reviewCommandService.create(
                                tradeId,
                                principal.memberId(),
                                reviewCreateRequest.isSeller(),
                                reviewCreateRequest.rating(),
                                reviewCreateRequest.comment()
                        )),
                        request.getRequestURI()
                ));
    }
}
