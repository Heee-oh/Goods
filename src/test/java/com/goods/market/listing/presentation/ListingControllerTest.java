package com.goods.market.listing.presentation;

import com.goods.market.listing.application.ListingCommandService;
import com.goods.market.listing.application.ListingImageStorageService;
import com.goods.market.listing.application.ListingQueryService;
import com.goods.market.listing.application.dto.ListingDetailDto;
import com.goods.market.listing.application.dto.ListingImageDto;
import com.goods.market.listing.application.dto.ListingItemDto;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.exception.ListingNotFoundException;
import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.common.presentation.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    @Mock
    private ListingCommandService listingCommandService;

    @Mock
    private ListingQueryService listingQueryService;

    @Mock
    private ListingImageStorageService listingImageStorageService;

    @InjectMocks
    private ListingController listingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(listingController)
                .setControllerAdvice(new ListingExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraftReturnsCreated() throws Exception {
        when(listingCommandService.createDraft(1L, 1)).thenReturn(100L);

                mockMvc.perform(post("/api/listings/drafts")
                        .param("region_id", "1")
                        .with(authenticated(1L)))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.listingId")
                        .value(100L));

        ArgumentCaptor<Long> sellerCaptor = ArgumentCaptor.forClass(Long.class);
        verify(listingCommandService).createDraft(sellerCaptor.capture(), eq(1));
        assertThat(sellerCaptor.getValue()).isEqualTo(1L);
    }

    @Test
    void uploadImageReturnsUploadedUrl() throws Exception {
        when(listingImageStorageService.store(any())).thenReturn("/uploads/listing-images/test.png");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "png-data".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/listings/images")
                        .file(file)
                        .with(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value("http://localhost/uploads/listing-images/test.png"));

        verify(listingImageStorageService).store(any());
    }

    @Test
    void getListingsUsesRequestedCursor() throws Exception {
        Slice<ListingItemDto> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 20), false);
        when(listingQueryService.getListings(1L, 11000, 88L, null, null)).thenReturn(slice);

        mockMvc.perform(get("/api/listings")
                        .param("region_id", "11000")
                        .param("last_listing_id", "88")
                        .with(authenticated(1L)))
                .andExpect(status().isOk());

        verify(listingQueryService).getListings(1L, 11000, 88L, null, null);
    }

    @Test
    void getListingsUsesDefaultCursorWhenMissingLastListingId() throws Exception {
        Slice<ListingItemDto> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 20), false);
        when(listingQueryService.getListings(1L, 11000, Long.MAX_VALUE, null, null)).thenReturn(slice);

        mockMvc.perform(get("/api/listings")
                        .param("region_id", "11000")
                        .with(authenticated(1L)))
                .andExpect(status().isOk());

        verify(listingQueryService).getListings(1L, 11000, Long.MAX_VALUE, null, null);
    }

    @Test
    void getListingsRejectsLegacyCamelCaseParams() throws Exception {
        mockMvc.perform(get("/api/listings")
                        .param("regionId", "11000")
                        .param("lastListingId", "88")
                        .with(authenticated(1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LISTING_BAD_REQUEST"));
    }

    @Test
    void getListingsReturnsBadRequestWhenRegionIsMissing() throws Exception {
        mockMvc.perform(get("/api/listings").with(authenticated(1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LISTING_BAD_REQUEST"));
    }

    @Test
    void getListingReturnsDetail() throws Exception {
        ListingDetailDto response = new ListingDetailDto(
                10L,
                1L,
                null,
                null,
                "seller",
                null,
                365,
                "Macbook",
                "almost new",
                1L,
                1200000L,
                "sell",
                false,
                "역삼동",
                0L,
                "PUBLISHED",
                11000,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                null,
                0L,
                List.of(new ListingImageDto(1L, "https://img/1.png", 0)),
                Instant.now()
        );
        when(listingQueryService.getListing(10L, null, null)).thenReturn(response);

        mockMvc.perform(get("/api/listings/{listingId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.listingId").value(10L))
                .andExpect(jsonPath("$.data.title").value("Macbook"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void publishReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/listings/{listingId}/publish", 10L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(listingCommandService).publish(1L, 10L);
    }

    @Test
    void hideReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/listings/{listingId}/hide", 10L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(listingCommandService).hide(1L, 10L);
    }

    @Test
    void unhideReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/listings/{listingId}/unhide", 10L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(listingCommandService).unhide(1L, 10L);
    }

    @Test
    void reserveReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/listings/{listingId}/reserve", 10L)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buyerId": 200
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(listingCommandService).reserve(1L, 10L, 200L);
    }

    @Test
    void cancelReserveReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/listings/{listingId}/reserve/cancel", 10L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(listingCommandService).cancelReserve(1L, 10L);
    }

    @Test
    void markSoldOutReturnsTradeId() throws Exception {
        when(listingCommandService.markSoldOut(1L, 10L, 200L)).thenReturn(300L);

        mockMvc.perform(post("/api/listings/{listingId}/sold-out", 10L)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buyerId": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(300L));

        verify(listingCommandService).markSoldOut(1L, 10L, 200L);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/listings/{listingId}", 10L).with(authenticated(1L)))
                .andExpect(status().isNoContent());

        verify(listingCommandService).remove(1L, 10L);
    }

    @Test
    void getListingReturns404WhenNotFound() throws Exception {
        when(listingQueryService.getListing(999L, null, null)).thenThrow(new ListingNotFoundException());

        mockMvc.perform(get("/api/listings/{listingId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));
    }

    @Test
    void updateReturnsNoContent() throws Exception {
        mockMvc.perform(put("/api/listings/{listingId}", 10L)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "updated title",
                                  "description": "updated desc",
                                  "categoryId": 1,
                                  "priceAmount": 1000,
                                  "transactionType": "sell",
                                  "hopeLocation": {
                                    "regionId": 11000,
                                    "lat": 37.5665,
                                    "lng": 126.9780
                                  },
                                  "imageUrls": ["https://img/1.png"]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(listingCommandService).update(any(Long.class), any(Long.class), any());
    }

    @Test
    void updateFailsWhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(put("/api/listings/{listingId}", 10L)
                        .with(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "updated title"
                                }
                                """))
                .andExpect(status().isBadRequest());
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
