package com.goods.market.member.presentation;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.listing.application.ListingImageStorageService;
import com.goods.market.member.application.MemberCommandService;
import com.goods.market.member.application.MemberQueryService;
import com.goods.market.member.application.dto.InterestDto;
import com.goods.market.member.application.dto.MemberDto;
import com.goods.market.member.application.dto.MemberRegionDto;
import com.goods.market.member.application.dto.MemberUpdateCommand;
import com.goods.market.member.presentation.dto.request.MemberUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberCommandService memberCommandService;

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private ListingImageStorageService listingImageStorageService;

    @InjectMocks
    private MemberController memberController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meReturnsMemberResponse() throws Exception {
        when(memberQueryService.getMe(1L)).thenReturn(new MemberDto("nick", "img.png", 365));

        mockMvc.perform(get("/api/members/me").with(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("nick"));

        verify(memberQueryService).getMe(1L);
    }

    @Test
    void myRegionsReturnsList() throws Exception {
        when(memberQueryService.getMyRegions(1L)).thenReturn(List.of(
                new MemberRegionDto(10L, 11000, Instant.now(), true, "Seocho", null, null)
        ));

        mockMvc.perform(get("/api/members/me/regions").with(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].regionId").value(11000));

        verify(memberQueryService).getMyRegions(1L);
    }

    @Test
    void updateMemberInfoReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/members/me")
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberUpdateRequest("newNick", "img2.png"))))
                .andExpect(status().isNoContent());

        verify(memberCommandService).updateMemberInfo(1L, new MemberUpdateCommand("newNick", "img2.png"));
    }

    @Test
    void updateProfileImageReturnsOk() throws Exception {
        when(listingImageStorageService.store(any(), any())).thenReturn("/uploads/profile-images/test.png");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/members/me/profile-image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileImage").value("http://localhost/uploads/profile-images/test.png"));

        verify(listingImageStorageService).store(any(), org.mockito.ArgumentMatchers.eq("profile-images"));
        verify(memberCommandService).updateProfileImage(1L, "http://localhost/uploads/profile-images/test.png");
    }

    @Test
    void updateNicknameReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/members/me/nickname")
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "next"}
                                """))
                .andExpect(status().isNoContent());

        verify(memberCommandService).updateNickname(1L, "next");
    }

    @Test
    void addInterestReturnsNoContent() throws Exception {
        mockMvc.perform(put("/api/members/me/interests/{listingId}", 77L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(memberCommandService).addInterest(1L, 77L);
    }

    @Test
    void deleteInterestReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/members/me/interests/{listingId}", 77L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(memberCommandService).deleteInterest(1L, 77L);
    }

    @Test
    void interestsUsesDefaultCursorAndSize() throws Exception {
        Slice<InterestDto> slice = new SliceImpl<>(
                List.of(new InterestDto(10L, 900L)),
                PageRequest.of(0, 20),
                false
        );
        when(memberQueryService.getMyInterests(1L, Long.MAX_VALUE, 20)).thenReturn(slice);

        mockMvc.perform(get("/api/members/me/interests").with(authenticated(1L)))
                .andExpect(status().isOk());

        verify(memberQueryService).getMyInterests(1L, Long.MAX_VALUE, 20);
    }

    @Test
    void withdrawReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/members/me").with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(memberCommandService).withdraw(1L);
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
