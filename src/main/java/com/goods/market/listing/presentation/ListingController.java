package com.goods.market.listing.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.listing.application.ListingCommandService;
import com.goods.market.listing.application.ListingImageStorageService;
import com.goods.market.listing.application.ListingQueryService;
import com.goods.market.listing.application.dto.ListingDetailDto;
import com.goods.market.listing.application.dto.ListingItemDto;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.presentation.dto.request.ListingReserveRequest;
import com.goods.market.listing.presentation.dto.request.ListingSoldOutRequest;
import com.goods.market.listing.presentation.dto.request.ListingUpdateRequest;
import com.goods.market.listing.presentation.dto.response.ListingDetailResponse;
import com.goods.market.listing.presentation.dto.response.ListingDraftResponse;
import com.goods.market.listing.presentation.dto.response.ListingImageUploadResponse;
import com.goods.market.listing.presentation.dto.response.ListingResponse;
import com.goods.market.listing.presentation.dto.response.ListingSoldOutResponse;
import com.goods.market.common.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingCommandService listingCommandService;
    private final ListingQueryService listingQueryService;
    private final ListingImageStorageService listingImageStorageService;


    @GetMapping()
    public ResponseEntity<ApiResponse<Slice<ListingResponse>>> getListings(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam(name = "region_id", required = false) Integer regionId,
            @RequestParam(name = "last_listing_id", required = false) Long lastListingId,
            @RequestParam(name = "transaction_type", required = false) String transactionType,
            @RequestParam(name = "seller_id", required = false) Long sellerId
    ) {
        if (regionId == null) {
            throw new ListingBadRequestException("region_id is required");
        }
        long cursor = lastListingId == null ? Long.MAX_VALUE : lastListingId;
        Slice<ListingItemDto> listings = listingQueryService.getListings(principal.memberId(), regionId, cursor, transactionType, sellerId);
        return ResponseEntity.ok(ApiResponse.success(toListingResponseSlice(listings), request.getRequestURI()));
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<ListingDraftResponse>> createDraft(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam("region_id") Integer regionId
    ) {
        Long listingId = listingCommandService.createDraft(principal.memberId(), regionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ListingDraftResponse(listingId), request.getRequestURI()));
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ListingImageUploadResponse>> uploadImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        String storedPath = listingImageStorageService.store(file);
        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(storedPath)
                .toUriString();
        return ResponseEntity.ok(ApiResponse.success(new ListingImageUploadResponse(imageUrl), request.getRequestURI()));
    }

    @GetMapping("/{listing_id}")
    public ResponseEntity<ApiResponse<ListingDetailResponse>> getListing(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @PathVariable("listing_id") Long listingId,
            @RequestParam(name = "region_id", required = false) Integer regionId
    ) {
        Long viewerMemberId = principal == null ? null : principal.memberId();
        ListingDetailDto listing = listingQueryService.getListing(listingId, viewerMemberId, regionId);
        return ResponseEntity.ok(ApiResponse.success(ListingDetailResponse.from(listing), request.getRequestURI()));
    }

    @PutMapping("/{listing_id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId,
            @Valid @RequestBody ListingUpdateRequest request
    ) {
        listingCommandService.update(principal.memberId(), listingId, request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/publish")
    public ResponseEntity<?> publish(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        listingCommandService.publish(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/hide")
    public ResponseEntity<?> hide(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        listingCommandService.hide(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/unhide")
    public ResponseEntity<?> unhide(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        listingCommandService.unhide(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/reserve")
    public ResponseEntity<?> reserve(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId,
            @Valid @RequestBody ListingReserveRequest request
    ) {
        listingCommandService.reserve(principal.memberId(), listingId, request.buyerId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/reserve/cancel")
    public ResponseEntity<?> cancelReserve(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        listingCommandService.cancelReserve(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listing_id}/sold-out")
    public ResponseEntity<ApiResponse<ListingSoldOutResponse>> markSoldOut(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpServletRequest,
            @PathVariable("listing_id") Long listingId,
            @Valid @RequestBody ListingSoldOutRequest request
    ) {
        Long tradeId = listingCommandService.markSoldOut(principal.memberId(), listingId, request.buyerId());
        return ResponseEntity.ok(ApiResponse.success(new ListingSoldOutResponse(tradeId), httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{listing_id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        listingCommandService.remove(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    private Slice<ListingResponse> toListingResponseSlice(Slice<ListingItemDto> slice) {
        List<ListingResponse> responses = slice.getContent().stream()
                .map(ListingResponse::from)
                .toList();
        return new org.springframework.data.domain.SliceImpl<>(
                responses,
                org.springframework.data.domain.PageRequest.of(slice.getNumber(), slice.getSize(), slice.getSort()),
                slice.hasNext()
        );
    }
}
