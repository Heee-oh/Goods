package com.goods.market.trade.application;

import com.goods.market.chat.domain.ChatRoom;
import com.goods.market.chat.infrastructure.ChatRoomRepository;
import com.goods.market.common.event.DomainEventPublisher;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.trade.application.dto.AppointmentResponse;
import com.goods.market.trade.domain.Appointment;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentCommandServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ListingJpaRepository listingJpaRepository;

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private AppointmentCommandServiceImpl appointmentCommandService;

    @Test
    void scheduleCancelsExistingAppointmentAndPublishesEvent() {
        Instant meetAt = Instant.parse("2026-05-01T10:00:00Z");
        ChatRoom chatRoom = ChatRoom.create(10L, 1L, 2L);
        Appointment existing = Appointment.schedule(10L, 1L, 2L, meetAt.minusSeconds(3600), 10);

        when(chatRoomRepository.findById(20L)).thenReturn(Optional.of(chatRoom));
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

        AppointmentResponse response = appointmentCommandService.schedule(1L, 20L, meetAt, 30);

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
    void cancelScheduledAppointmentByParticipant() {
        Instant meetAt = Instant.parse("2026-05-01T10:00:00Z");
        Appointment appointment = Appointment.schedule(10L, 1L, 2L, meetAt, 30);

        when(appointmentRepository.findByIdAndStatus(99L, AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.of(appointment));

        appointmentCommandService.cancel(2L, 99L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
    }
}
