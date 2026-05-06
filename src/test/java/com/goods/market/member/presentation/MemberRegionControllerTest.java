package com.goods.market.member.presentation;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.common.presentation.GlobalExceptionHandler;
import com.goods.market.member.application.MemberRegionCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberRegionControllerTest {

    @Mock
    private MemberRegionCommandService memberRegionCommandService;

    @InjectMocks
    private MemberRegionController memberRegionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberRegionController)
                .setControllerAdvice(new MemberExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifyReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/members/me/regions/{region_id}/verify", 11000)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lat": 37.5665,
                                  "lng": 126.9780
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(memberRegionCommandService)
                .verifyMemberRegionByRegionId(11000, 1L, new java.math.BigDecimal("37.5665"), new java.math.BigDecimal("126.9780"));
    }

    @Test
    void verifyReturnsBadRequestWhenLatIsMissing() throws Exception {
        mockMvc.perform(post("/api/members/me/regions/{region_id}/verify", 11000)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lng": 126.9780
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void removeReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/members/me/regions/{region_id}", 11000)
                        .with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(memberRegionCommandService).removeMemberRegion(11000, 1L);
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
