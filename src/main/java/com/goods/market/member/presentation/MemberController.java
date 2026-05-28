package com.goods.market.member.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.listing.application.ListingImageStorageService;
import com.goods.market.member.application.MemberCommandService;
import com.goods.market.member.application.MemberQueryService;
import com.goods.market.member.application.dto.InterestDto;
import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.presentation.dto.request.MemberUpdateRequest;
import com.goods.market.member.presentation.dto.request.NicknameRequest;
import com.goods.market.member.presentation.dto.response.InterestResponse;
import com.goods.market.member.presentation.dto.response.MemberRegionResponse;
import com.goods.market.member.presentation.dto.response.MemberResponse;
import com.goods.market.member.presentation.dto.response.ProfileImageUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final ListingImageStorageService listingImageStorageService;

    @GetMapping("/me/regions")
    public ResponseEntity<ApiResponse<List<MemberRegionResponse>>> myRegion(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request
    ) {
        List<MemberRegionResponse> myRegions = memberQueryService.getMyRegions(principal.memberId())
                .stream()
                .map(MemberRegionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(myRegions, request.getRequestURI()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> me(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request
    ) {
        MemberDto me = memberQueryService.getMe(principal.memberId());
        return ResponseEntity.ok(ApiResponse.success(MemberResponse.from(me), request.getRequestURI()));
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMemberInfo(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody MemberUpdateRequest request
    ) {
        memberCommandService.updateMemberInfo(principal.memberId(), request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/me/profile-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageUploadResponse>> updateProfileImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        String storedPath = listingImageStorageService.store(file, "profile-images");
        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(storedPath)
                .toUriString();
        memberCommandService.updateProfileImage(principal.memberId(), imageUrl);
        return ResponseEntity.ok(ApiResponse.success(new ProfileImageUploadResponse(imageUrl), request.getRequestURI()));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<?> updateNickname(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody NicknameRequest request
    ) {
        memberCommandService.updateNickname(principal.memberId(), request.nickname());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/interests/{listing_id}")
    public ResponseEntity<?> addInterest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        memberCommandService.addInterest(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/interests/{listing_id}")
    public ResponseEntity<?> deleteInterest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("listing_id") Long listingId
    ) {
        memberCommandService.deleteInterest(principal.memberId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/interests")
    public ResponseEntity<ApiResponse<?>> interests(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @RequestParam(required = false) Long lastInterestId,
            @RequestParam(defaultValue = "20") int size
    ) {
        long cursor = lastInterestId == null ? Long.MAX_VALUE : lastInterestId;
        Slice<InterestDto> interests = memberQueryService.getMyInterests(principal.memberId(), cursor, size);
        return ResponseEntity.ok(ApiResponse.success(
                toInterestResponseSlice(interests, size),
                request.getRequestURI()
        ));
    }

    @GetMapping("/me/interests/{listing_id}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> interestStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            @PathVariable("listing_id") Long listingId
    ) {
        boolean interested = memberQueryService.isInterested(principal.memberId(), listingId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("interested", interested), request.getRequestURI()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal AuthPrincipal principal) {
        memberCommandService.withdraw(principal.memberId());
        return ResponseEntity.noContent().build();
    }

    private Slice<InterestResponse> toInterestResponseSlice(Slice<InterestDto> slice, int size) {
        return new SliceImpl<>(
                slice.getContent().stream().map(InterestResponse::from).toList(),
                PageRequest.of(0, size),
                slice.hasNext()
        );
    }
}
