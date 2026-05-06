package com.goods.market.listing;

import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.domain.TransactionType;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.exception.ListingConflictException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListingTest {

    @Test
    @DisplayName("ì´ì ìì± ì ê¸°ë³¸ ìíê°ì ê°ì§ë¤")
    void draftDefaultState() {
        Listing listing = Listing.draftPrice(10L, false);

        Assertions.assertThat(listing.getStatus()).isEqualTo(Status.DRAFT);
        Assertions.assertThat(listing.isHidden()).isFalse();
        Assertions.assertThat(listing.getPrice().getPriceAmount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("ê²ìíë©´ ìíê° ì´ììì ê²ìì¤ì¼ë¡ ë³ê²½ëë¤")
    void publishSuccess() {
        Listing listing = Listing.draftPrice(10L, false);

        listing.publish();

        Assertions.assertThat(listing.getStatus()).isEqualTo(Status.PUBLISHED);
    }

    @Test
    @DisplayName("ì´ìì´ ìë ìíìì ê²ìíë©´ ì¶©ë ìì¸ê° ë°ìíë¤")
    void publishFailWhenNotDraft() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();

        Assertions.assertThatThrownBy(listing::publish)
                .isInstanceOf(ListingConflictException.class);
    }

    @Test
    @DisplayName("ê²ìë ê¸ì ì¨ê¹ê³¼ ì¨ê¹ í´ì ë¥¼ ìíí  ì ìë¤")
    void hideUnhideSuccess() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();

        listing.hide();
        Assertions.assertThat(listing.isHidden()).isTrue();

        listing.unHide();
        Assertions.assertThat(listing.isHidden()).isFalse();
    }

    @Test
    @DisplayName("ìì½ì ìì´ëê° ë¹ì´ ìì¼ë©´ ìì½ì ì¤í¨íë¤")
    void reserveFailWhenBuyerNull() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();

        Assertions.assertThatThrownBy(() -> listing.reserve(null))
                .isInstanceOf(ListingBadRequestException.class);
    }

    @Test
    @DisplayName("ìì½ í ìì½ ì·¨ì ì ìíê° ì ì ì ì´ëë¤")
    void reserveAndCancelSuccess() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();

        listing.reserve(123L);
        Assertions.assertThat(listing.getStatus()).isEqualTo(Status.RESERVED);
        Assertions.assertThat(listing.getReserverId()).isEqualTo(123L);

        listing.cancelReserve();
        Assertions.assertThat(listing.getStatus()).isEqualTo(Status.PUBLISHED);
        Assertions.assertThat(listing.getReserverId()).isNull();
    }

    @Test
    @DisplayName("íë§¤ ìë£ë ìì½ë êµ¬ë§¤ìë§ íì í  ì ìë¤")
    void soldOutValidation() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();
        listing.reserve(123L);

        Assertions.assertThatThrownBy(() -> listing.markSoldOut(456L))
                .isInstanceOf(ListingBadRequestException.class);

        listing.markSoldOut(123L);
        Assertions.assertThat(listing.getStatus()).isEqualTo(Status.SOLD_OUT);
        Assertions.assertThat(listing.getBuyerId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("ê°ê²© ìë ¥ì´ ì í¨íì§ ìì¼ë©´ ê°ê²© ìì ì ì¤í¨íë¤")
    void updatePriceInvalid() {
        Listing listing = Listing.draft();

        Assertions.assertThatThrownBy(() -> listing.updatePrice(-1L, false))
                .isInstanceOf(ListingBadRequestException.class);
    }

    @Test
    @DisplayName("ìì½ ìíììë ì­ì ë¥¼ ìíí  ì ìë¤")
    void removeFailWhenReserved() {
        Listing listing = Listing.draftPrice(10L, false);
        listing.publish();
        listing.reserve(1L);

        Assertions.assertThatThrownBy(listing::remove)
                .isInstanceOf(ListingConflictException.class);
    }

    @Test
    @DisplayName("나눔은 거래 타입이 FREE로 저장되고 가격은 0원이다")
    void freeTransactionTypeIsPersistedWithZeroPrice() {
        Listing listing = Listing.draftPrice(0L, TransactionType.FREE);

        Assertions.assertThat(listing.getPrice().resolveTransactionType()).isEqualTo(TransactionType.FREE);
        Assertions.assertThat(listing.getPrice().getPriceAmount()).isEqualTo(0L);
    }
}
