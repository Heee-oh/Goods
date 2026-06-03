package com.goods.market.common.interceptor;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.member.application.MemberRegionQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionVerificationInterceptor implements HandlerInterceptor {

    private final MemberRegionQueryService memberRegionQueryService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("region verification preHandle");
        String param = request.getParameter("region_id");
        // 지역 id가 필요없는 요청의 경우 패스
        if (param == null || param.isBlank()) return true;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return true;
        }

        Integer regionId;
        try {
            regionId = Integer.valueOf(param);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid region_id");
        }

        memberRegionQueryService.validateMemberRegionByRegionId(principal.memberId(), regionId);

        return true;
    }
}
