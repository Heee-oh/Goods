package com.goods.market.listing.integration;

import com.goods.market.listing.application.ListingCommandService;
import com.goods.market.listing.application.ListingQueryService;
import com.goods.market.listing.application.dto.ListingDetailDto;
import com.goods.market.listing.application.dto.ListingUpdateCommand;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.listing.exception.ListingNotFoundException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.MemberRegion;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ListingServiceIntegrationTest {

    @Autowired
    private ListingCommandService listingCommandService;

    @Autowired
    private ListingQueryService listingQueryService;

    @Autowired
    private ListingJpaRepository listingJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository memberRegionJpaRepository;

    @Test
    @DisplayName("데이터베이스 연동: 임시 글 생성 후 수정으로 값 저장이 가능하다")
    void createDraftAndUpdateListing() {
        saveMemberWithRegion(100L, "seller", "01012345678", 1, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        Long listingId = listingCommandService.createDraft(100L,1);
        Listing draft = listingJpaRepository.findByIdAndDeletedAtIsNull(listingId).orElseThrow();
        assertThat(draft.getOriginLat()).isEqualTo(new BigDecimal("37.5665"));
        assertThat(draft.getOriginLng()).isEqualTo(new BigDecimal("126.9780"));

        listingCommandService.update(100L, listingId, new ListingUpdateCommand(
                "iPhone",
                "good condition",
                1L,
                700000L,
                TransactionType.SELL,
                11000,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                List.of("https://img/1.png", "https://img/2.png")
        ));

        ListingDetailDto detail = listingQueryService.getListing(listingId, 100L);

        assertThat(detail.listingId()).isEqualTo(listingId);
        assertThat(detail.sellerId()).isEqualTo(100L);
        assertThat(detail.title()).isEqualTo("iPhone");
        assertThat(detail.priceAmount()).isEqualTo(700000L);
        assertThat(detail.images()).hasSize(2);
    }

    @Test
    @DisplayName("데이터베이스 연동: 게시에서 예약 후 판매 완료까지 상태 전이가 영속화된다")
    void statusFlowIsPersisted() {
        saveMemberWithRegion(100L, "seller", "01012345678", 1, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        Long listingId = listingCommandService.createDraft(100L, 1);

        listingCommandService.update(100L, listingId, new ListingUpdateCommand(
                "Macbook",
                "almost new",
                1L,
                1200000L,
                TransactionType.SELL,
                11000,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                List.of("https://img/1.png")
        ));

        listingCommandService.publish(100L, listingId);
        listingCommandService.reserve(100L, listingId, 200L);
        listingCommandService.markSoldOut(100L, listingId, 200L);

        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(listingId).orElseThrow();

        assertThat(listing.getStatus()).isEqualTo(Status.SOLD_OUT);
        assertThat(listing.getBuyerId()).isEqualTo(200L);
        assertThat(listing.getReserverId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("데이터베이스 연동: 삭제된 글은 활성 조회에서 제외된다")
    void removedListingIsInvisible() {
        saveMemberWithRegion(100L, "seller", "01012345678", 1, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        Long listingId = listingCommandService.createDraft(100L,1);

        listingCommandService.remove(100L, listingId);

        assertThat(listingJpaRepository.findByIdAndDeletedAtIsNull(listingId)).isEmpty();
        assertThatThrownBy(() -> listingQueryService.getListing(listingId, 100L))
                .isInstanceOf(ListingNotFoundException.class);
    }

    private void saveMember(Long memberId, String nickname, String phoneNumber) {
        Member member = new Member(nickname, new PhoneNumber(phoneNumber));
        ReflectionTestUtils.setField(member, "id", memberId);
        memberJpaRepository.save(member);
    }

    private void saveMemberWithRegion(Long memberId, String nickname, String phoneNumber, Integer regionId, BigDecimal lat, BigDecimal lng) {
        Member member = new Member(nickname, new PhoneNumber(phoneNumber));
        ReflectionTestUtils.setField(member, "id", memberId);
        MemberRegion memberRegion = new MemberRegion(regionId, true, lat, lng);
        member.addRegion(memberRegion);
        memberJpaRepository.save(member);
    }
}
