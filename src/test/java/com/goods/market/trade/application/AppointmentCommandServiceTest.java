package com.goods.market.trade.application;

import com.goods.market.chat.application.ChatQueryService;
import com.goods.market.chat.application.dto.AppointmentChatRoomDto;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.listing.application.ListingQueryService;
import com.goods.market.listing.application.dto.AppointmentListingDto;
import com.goods.market.member.application.MemberQueryService;
import com.goods.market.trade.application.dto.AppointmentDto;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.exception.AppointmentBadRequestException;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentCommandServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ChatQueryService chatQueryService;

    @Mock
    private ListingQueryService listingQueryService;

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private AppointmentCommandService appointmentCommandService;

    @Test
    void scheduleCancelsExistingAppointmentAndPublishesEvent() {
        Instant meetAt = Instant.parse("2099-05-01T10:00:00Z");
        AppointmentChatRoomDto chatRoom = new AppointmentChatRoomDto(20L, 10L, 1L, 2L);
        Appointment existing = Appointment.schedule(10L, 1L, 2L, meetAt.minusSeconds(3600), 10);

        when(chatQueryService.getParticipatingAppointmentChatRoom(1L, 20L)).thenReturn(chatRoom);
        when(listingQueryService.getAppointmentListing(10L))
                .thenReturn(new AppointmentListingDto(10L, "title", "PUBLISHED", null));
        when(appointmentRepository.findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                10L,
                2L,
                AppointmentStatus.SCHEDULED
        )).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            ReflectionTestUtils.setField(appointment, "id", 99L);
            return appointment;
        });

        AppointmentDto response = appointmentCommandService.schedule(1L, 20L, meetAt, 30);

        assertThat(existing.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(response.appointmentId()).isEqualTo(99L);
        assertThat(response.meetAt()).isEqualTo(meetAt);
        assertThat(response.reminderMinutes()).isEqualTo(30);

        ArgumentCaptor<TradeAppointmentScheduledEvent> eventCaptor =
                ArgumentCaptor.forClass(TradeAppointmentScheduledEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().appointmentId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().listingId()).isEqualTo(10L);
    }

    @Test
    void scheduleRejectsReservedListingForOtherBuyer() {
        Instant meetAt = Instant.parse("2099-05-01T10:00:00Z");
        AppointmentChatRoomDto chatRoom = new AppointmentChatRoomDto(20L, 10L, 1L, 2L);

        when(chatQueryService.getParticipatingAppointmentChatRoom(2L, 20L)).thenReturn(chatRoom);
        when(listingQueryService.getAppointmentListing(10L))
                .thenReturn(new AppointmentListingDto(10L, "title", "RESERVED", 3L));

        assertThatThrownBy(() -> appointmentCommandService.schedule(2L, 20L, meetAt, 30))
                .isInstanceOf(AppointmentBadRequestException.class)
                .hasMessageContaining("다른 사람과 거래중입니다.");
    }

    @Test
    void scheduleRejectsSoldOutListing() {
        Instant meetAt = Instant.parse("2099-05-01T10:00:00Z");
        AppointmentChatRoomDto chatRoom = new AppointmentChatRoomDto(20L, 10L, 1L, 2L);

        when(chatQueryService.getParticipatingAppointmentChatRoom(2L, 20L)).thenReturn(chatRoom);
        when(listingQueryService.getAppointmentListing(10L))
                .thenReturn(new AppointmentListingDto(10L, "title", "SOLD_OUT", 2L));

        assertThatThrownBy(() -> appointmentCommandService.schedule(2L, 20L, meetAt, 30))
                .isInstanceOf(AppointmentBadRequestException.class)
                .hasMessageContaining("거래 완료된 게시글입니다.");
    }

    @Test
    void cancelScheduledAppointmentByParticipant() {
        Instant meetAt = Instant.parse("2099-05-01T10:00:00Z");
        Appointment appointment = Appointment.schedule(10L, 1L, 2L, meetAt, 30);

        when(appointmentRepository.findByIdAndStatus(99L, AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.of(appointment));

        appointmentCommandService.cancel(2L, 99L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
    }

}
