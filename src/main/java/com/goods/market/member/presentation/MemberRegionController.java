package com.goods.market.member.presentation;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.member.application.MemberRegionCommandService;
import com.goods.market.member.presentation.dto.request.MemberRegionAddRequest;
import com.goods.market.member.presentation.dto.request.MemberRegionVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/regions")
@RequiredArgsConstructor
public class MemberRegionController {

    private final MemberRegionCommandService memberRegionCommandService;

    @PostMapping
    public ResponseEntity<Void> add(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MemberRegionAddRequest request
    ) {
        memberRegionCommandService.addMemberRegion(request.regionId(), principal.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{region_id}/verify")
    public ResponseEntity<Void> verify(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("region_id") Integer regionId,
            @Valid @RequestBody MemberRegionVerifyRequest request
    ) {
        memberRegionCommandService.verifyMemberRegionByRegionId(regionId, principal.memberId(), request.lat(), request.lng());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{region_id}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("region_id") Integer regionId
    ) {
        memberRegionCommandService.removeMemberRegion(regionId, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
