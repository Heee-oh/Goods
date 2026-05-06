package com.goods.market.listing.application;

import com.goods.market.listing.application.dto.ListingUpdateCommand;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.listing.exception.ListingAccessDeniedException;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.exception.ListingConflictException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingCommandServiceImplTest {

    @Mock
    private ListingJpaRepository listingJpaRepository;

    @Mock
    private MemberRegionJpaRepository memberRegionJpaRepository;

    @InjectMocks
    private ListingCommandServiceImpl listingCommandService;

    @Test
    @DisplayName("임시 글을 생성하면 빈 초안이 저장되고 ID를 반환한다")
    void createDraftSuccess() {
        when(memberRegionJpaRepository.findFirstByMember_IdAndRegionIdAndVerifiedAtIsNotNull(1L, 1))
                .thenReturn(Optional.of(new MemberRegion(1, true, new BigDecimal("37.5"), new BigDecimal("126.9"))));
        when(listingJpaRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            ReflectionTestUtils.setField(listing, "id", 101L);
            return listing;
        });

        Long listingId = listingCommandService.createDraft(1L, 1);

        ArgumentCaptor<Listing> captor = ArgumentCaptor.forClass(Listing.class);
        verify(listingJpaRepository).save(captor.capture());
        Listing saved = captor.getValue();

        assertThat(listingId).isEqualTo(101L);
        assertThat(saved.getSellerId()).isEqualTo(1L);
        assertThat(saved.getStatus().name()).isEqualTo("DRAFT");
        assertThat(saved.getOriginLat()).isEqualTo(new BigDecimal("37.5"));
        assertThat(saved.getOriginLng()).isEqualTo(new BigDecimal("126.9"));
    }

    @Test
    @DisplayName("수정 요청자가 작성자가 아니면 접근 거부 예외가 발생한다")
    void updateFailsWhenRequesterIsNotSeller() {
        Listing listing = createDraftListing(2L);
        when(listingJpaRepository.findActiveByIdWithImages(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingCommandService.update(1L, 1L, fullUpdateCommand()))
                .isInstanceOf(ListingAccessDeniedException.class);
    }

    @Test
    @DisplayName("이미 게시된 글을 다시 게시하면 충돌 예외가 발생한다")
    void publishFailsWhenAlreadyPublished() {
        Listing listing = createDraftListing(1L);
        listing.publish();
        when(listingJpaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingCommandService.publish(1L, 1L))
                .isInstanceOf(ListingConflictException.class);
    }

    @Test
    @DisplayName("예약자 ID가 null이면 잘못된 요청 예외가 발생한다")
    void reserveFailsWhenBuyerIdIsNull() {
        Listing listing = createDraftListing(1L);
        listing.publish();
        when(listingJpaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingCommandService.reserve(1L, 1L, null))
                .isInstanceOf(ListingBadRequestException.class);
    }

    @Test
    @DisplayName("카테고리 ID가 유효하지 않으면 수정 시 잘못된 요청 예외가 발생한다")
    void updateFailsWhenCategoryInvalid() {
        Listing listing = createDraftListing(1L);
        when(listingJpaRepository.findActiveByIdWithImages(1L)).thenReturn(Optional.of(listing));

        ListingUpdateCommand command = new ListingUpdateCommand(
                "title",
                "desc",
                0L,
                1000L,
                TransactionType.SELL,
                11000,
                new BigDecimal("37.5"),
                new BigDecimal("126.9"),
                List.of("https://img/1.png")
        );

        assertThatThrownBy(() -> listingCommandService.update(1L, 1L, command))
                .isInstanceOf(ListingBadRequestException.class)
                .hasMessageContaining("category");
    }

    @Test
    @DisplayName("업데이트 내용이 하나라도 null이면 예외가 발생한다.")
    void updateFailsWhenCommandInvalid() {
        Listing listing = createDraftListing(1L);
        when(listingJpaRepository.findActiveByIdWithImages(1L)).thenReturn(Optional.of(listing));

        ListingUpdateCommand command = new ListingUpdateCommand(
                "title",
                "dd",
                1L,
                1000L,
                TransactionType.SELL,
                11000,
                null,
                new BigDecimal("126.9"),
                List.of("https://img/1.png")
        );

        assertThatThrownBy(() -> listingCommandService.update(1L, 1L, command))
                .isInstanceOf(ListingBadRequestException.class);
    }

    private Listing createDraftListing(Long sellerId) {
        Listing listing = Listing.createDraft(
                sellerId,
                "default title",
                "default description",
                1L,
                1000L,
                false,
                null,
                List.of()
        );
        ReflectionTestUtils.setField(listing, "id", 1L);
        return listing;
    }

    private ListingUpdateCommand fullUpdateCommand() {
        return new ListingUpdateCommand(
                "new title",
                "new desc",
                1L,
                1000L,
                TransactionType.SELL,
                11000,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                List.of("https://img/1.png")
        );
    }
}
