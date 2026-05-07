type ExchangeRoom = {
  partner_nickname?: string;
  listing_title?: string;
};

type ExchangeListing = {
  status?: string;
  reserver_id?: number | string | null;
};

type ExchangeAppointment = {
  meet_at: string;
  reminder_minutes: number | null;
} | null;

type ExchangeStateInput = {
  room: ExchangeRoom | null;
  listing: ExchangeListing | null;
  currentAppointment: ExchangeAppointment;
};

function formatAppointmentLabel(meetAt: string, reminderMinutes: number | null) {
  const date = new Date(meetAt);
  const dateLabel = date.toLocaleDateString("ko-KR", {
    month: "long",
    day: "numeric"
  });
  const timeLabel = date.toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });
  const reminderLabel = reminderMinutes == null ? "알림 없음" : `${reminderMinutes}분 전 알림`;

  return `${dateLabel} ${timeLabel} · ${reminderLabel}`;
}

export function useExchangeState({ room, listing, currentAppointment }: ExchangeStateInput) {
  const partnerNickname = room?.partner_nickname?.trim() || "상대방";
  const listingTitle = room?.listing_title?.trim() || "거래 정보";

  const statusLabel = currentAppointment
    ? "약속 설정됨"
    : listing?.status === "RESERVED"
      ? "예약중"
      : listing?.status === "SOLD_OUT"
        ? "거래 완료"
        : "대화 중";

  const appointmentLabel = currentAppointment
    ? formatAppointmentLabel(currentAppointment.meet_at, currentAppointment.reminder_minutes)
    : null;

  return {
    partnerNickname,
    listingTitle,
    statusLabel,
    appointmentLabel,
    canComplete: Boolean(currentAppointment && listing?.status !== "SOLD_OUT")
  };
}
