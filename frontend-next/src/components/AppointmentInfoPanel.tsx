import { forwardRef, useImperativeHandle, useRef, useState } from "react";
import { APPOINTMENT_TEXT } from "../lib/appointmentText";
import { ApiError } from "../lib/api";

type CurrentAppointment = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
};

type AppointmentResponse = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
};

type AppointmentInfoPanelProps = {
  partnerNickname: string;
  currentAppointment: CurrentAppointment | null;
  busy: boolean;
  canComplete: boolean;
  appointmentBlockedMessage?: string;
  overlayMode?: "page" | "inline";
  onCreateAppointment: (payload: {
    meetAt: string;
    reminderMinutes: number | null;
  }) => Promise<AppointmentResponse>;
  onCancelAppointment: (appointmentId: number | string) => Promise<void>;
  onOpenCompletion: () => void;
  onAppointmentBlocked?: (message: string) => void;
};

export type AppointmentInfoPanelHandle = {
  openComposer: () => void;
  openViewer: (appointment: CurrentAppointment) => void;
};

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function toTimeInputValue(date: Date) {
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(Math.floor(date.getMinutes() / 5) * 5).padStart(2, "0");
  return `${hours}:${minutes}`;
}

function roundToNextFiveMinutes(date: Date) {
  const next = new Date(date);
  next.setSeconds(0, 0);

  const minutes = Math.ceil(next.getMinutes() / 5) * 5;
  if (minutes === 60) {
    next.setHours(next.getHours() + 1, 0, 0, 0);
    return next;
  }

  next.setMinutes(minutes);
  return next;
}

function fromDateAndTime(dateValue: string, timeValue: string) {
  const [year, month, day] = dateValue.split("-").map(Number);
  const [hours, minutes] = timeValue.split(":").map(Number);
  return new Date(year, month - 1, day, hours, minutes, 0, 0);
}

function formatAppointmentTime(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });
}

function formatAppointmentDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "short"
  });
}

function formatReminderText(value: number | null) {
  return APPOINTMENT_TEXT.reminders.find((option) => option.value === value)?.label ?? APPOINTMENT_TEXT.reminders[0].label;
}

function getReminderValidationError(
  dateValue: string,
  timeValue: string,
  reminderMinutes: number | null
) {
  if (reminderMinutes == null) {
    return null;
  }

  const meetAt = fromDateAndTime(dateValue, timeValue);
  if (Number.isNaN(meetAt.getTime())) {
    return "약속 시간을 다시 확인해 주세요.";
  }

  const reminderAt = meetAt.getTime() - reminderMinutes * 60_000;
  if (reminderAt < Date.now()) {
    return "선택한 알림 시간이 이미 지났습니다.";
  }

  return null;
}

export const AppointmentInfoPanel = forwardRef<AppointmentInfoPanelHandle, AppointmentInfoPanelProps>(function AppointmentInfoPanel({
  partnerNickname,
  currentAppointment,
  busy,
  canComplete,
  appointmentBlockedMessage = "",
  overlayMode = "page",
  onCreateAppointment,
  onCancelAppointment,
  onOpenCompletion,
  onAppointmentBlocked
}, ref) {
  const [showComposer, setShowComposer] = useState(false);
  const [showViewer, setShowViewer] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<CurrentAppointment | null>(null);
  const initialDateTime = roundToNextFiveMinutes(new Date());
  const [appointmentDate, setAppointmentDate] = useState(toDateInputValue(initialDateTime));
  const [appointmentTime, setAppointmentTime] = useState(toTimeInputValue(initialDateTime));
  const [appointmentReminder, setAppointmentReminder] = useState<number | null>(30);
  const [actionError, setActionError] = useState("");
  const errorRef = useRef<HTMLParagraphElement | null>(null);
  const reminderValidationError = getReminderValidationError(appointmentDate, appointmentTime, appointmentReminder);
  const visibleError = actionError || reminderValidationError;

  const scrollToError = () => {
    window.setTimeout(() => {
      errorRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
    }, 0);
  };

  const openComposer = () => {
    if (appointmentBlockedMessage) {
      onAppointmentBlocked?.(appointmentBlockedMessage);
      return;
    }

    const baseDate = currentAppointment ? new Date(currentAppointment.meet_at) : new Date();
    const nextDateTime = roundToNextFiveMinutes(baseDate);
    setAppointmentDate(toDateInputValue(nextDateTime));
    setAppointmentTime(toTimeInputValue(nextDateTime));
    setAppointmentReminder(currentAppointment?.reminder_minutes ?? 30);
    setActionError("");
    setShowComposer(true);
  };

  const openViewer = (appointment: CurrentAppointment) => {
    setSelectedAppointment(appointment);
    setShowViewer(true);
  };

  useImperativeHandle(
    ref,
    () => ({
      openComposer,
      openViewer
    }),
    [appointmentBlockedMessage, currentAppointment, onAppointmentBlocked]
  );

  const handleCreate = async () => {
    if (reminderValidationError) {
      setActionError(reminderValidationError);
      scrollToError();
      return;
    }

    const meetAt = fromDateAndTime(appointmentDate, appointmentTime);
    if (Number.isNaN(meetAt.getTime())) {
      setActionError("약속 시간을 다시 확인해 주세요.");
      scrollToError();
      return;
    }

    try {
      const created = await onCreateAppointment({
        meetAt: meetAt.toISOString(),
        reminderMinutes: appointmentReminder
      });

      setActionError("");
      setShowComposer(false);
      openViewer(created);
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.length) {
        setActionError(err.fieldErrors.map((fieldError) => fieldError.message).join(" / "));
        scrollToError();
        return;
      }

      setActionError(err instanceof Error ? err.message : "약속을 저장하지 못했습니다.");
      scrollToError();
    }
  };

  const handleCancel = async () => {
    if (!selectedAppointment) {
      return;
    }

    await onCancelAppointment(selectedAppointment.appointment_id);
    setShowViewer(false);
    setSelectedAppointment(null);
  };

  const appointmentButtonLabel = currentAppointment
    ? "약속 보기"
    : APPOINTMENT_TEXT.quick.open;

  return (
    <>
      <div className="chat-room-quick-actions">
        <button
          type="button"
          className="chat-room-quick-button"
          onClick={() => {
            if (currentAppointment) {
              openViewer(currentAppointment);
            } else {
              openComposer();
            }
          }}
        >
          <span aria-hidden="true">{APPOINTMENT_TEXT.quick.label}</span>
          <span>{appointmentButtonLabel}</span>
        </button>
        <button
          type="button"
          className="chat-room-quick-button"
          disabled={!canComplete}
          onClick={onOpenCompletion}
        >
          <span aria-hidden="true">{APPOINTMENT_TEXT.quick.completePrefix}</span>
          <span>{APPOINTMENT_TEXT.quick.completeSuffix}</span>
        </button>
        <button type="button" className="chat-room-quick-button" disabled>
          <span aria-hidden="true">{APPOINTMENT_TEXT.quick.placePrefix}</span>
          <span>{APPOINTMENT_TEXT.quick.placeSuffix}</span>
        </button>
      </div>

      {showComposer ? (
        <div
          className={overlayMode === "inline" ? "chat-modal-overlay chat-modal-overlay-inline" : "chat-modal-overlay"}
          onClick={() => setShowComposer(false)}
        >
          <div
            className={overlayMode === "inline" ? "chat-sheet chat-sheet-inline" : "chat-sheet"}
            onClick={(event) => event.stopPropagation()}
          >
            {overlayMode === "inline" ? null : <div className="chat-sheet-grabber" />}
            <div className="chat-sheet-inline-scroll">
              <div className="chat-sheet-header">
                <strong>
                  {partnerNickname}
                  {APPOINTMENT_TEXT.composer.titleSuffix}
                </strong>
              </div>
              <div className="chat-sheet-section">
                <label className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.composer.date}</span>
                  <input
                    type="date"
                    value={appointmentDate}
                    onChange={(event) => {
                      setActionError("");
                      setAppointmentDate(event.target.value);
                    }}
                  />
                </label>
                <label className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.composer.time}</span>
                  <input
                    type="time"
                    value={appointmentTime}
                    step={300}
                    onChange={(event) => {
                      setActionError("");
                      setAppointmentTime(event.target.value);
                    }}
                  />
                </label>
                <div className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.composer.place}</span>
                  <span className="chat-sheet-muted">{APPOINTMENT_TEXT.common.placePreparing}</span>
                </div>
                <div className="chat-sheet-row chat-sheet-row-stack">
                  <span>{APPOINTMENT_TEXT.composer.reminder}</span>
                  <div className="chat-sheet-reminders">
                    {APPOINTMENT_TEXT.reminders.map((option) => (
                      <button
                        key={option.label}
                      type="button"
                      className={
                        appointmentReminder === option.value
                          ? "chat-reminder-chip active"
                          : "chat-reminder-chip"
                      }
                      onClick={() => {
                        setActionError("");
                        setAppointmentReminder(option.value);
                      }}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
                {visibleError ? <p ref={errorRef} className="chat-sheet-error">{visibleError}</p> : null}
              </div>
            </div>
            <div className="chat-sheet-actions">
              <button
                type="button"
                className="chat-sheet-secondary"
                onClick={() => setShowComposer(false)}
              >
                {APPOINTMENT_TEXT.composer.cancel}
              </button>
              <button
                type="button"
                className="chat-sheet-primary"
                onClick={() => void handleCreate()}
                disabled={busy}
              >
                {busy ? APPOINTMENT_TEXT.common.loading : APPOINTMENT_TEXT.composer.submit}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showViewer && selectedAppointment ? (
        <div
          className={overlayMode === "inline" ? "chat-modal-overlay chat-modal-overlay-inline" : "chat-modal-overlay"}
          onClick={() => {
            setShowViewer(false);
            setSelectedAppointment(null);
          }}
        >
          <div
            className={overlayMode === "inline" ? "chat-sheet chat-sheet-inline" : "chat-sheet"}
            onClick={(event) => event.stopPropagation()}
          >
            {overlayMode === "inline" ? null : <div className="chat-sheet-grabber" />}
            <div className="chat-sheet-inline-scroll">
              <div className="chat-sheet-header">
                <strong>
                  {partnerNickname}
                  {APPOINTMENT_TEXT.viewer.titleSuffix}
                </strong>
              </div>
              <div className="chat-sheet-section">
                <div className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.viewer.date}</span>
                  <strong>{formatAppointmentDate(selectedAppointment.meet_at)}</strong>
                </div>
                <div className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.viewer.time}</span>
                  <strong>{formatAppointmentTime(selectedAppointment.meet_at)}</strong>
                </div>
                <div className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.viewer.place}</span>
                  <strong className="chat-sheet-muted">{APPOINTMENT_TEXT.common.placePreparing}</strong>
                </div>
                <div className="chat-sheet-row">
                  <span>{APPOINTMENT_TEXT.viewer.reminder}</span>
                  <strong>{formatReminderText(selectedAppointment.reminder_minutes)}</strong>
                </div>
              </div>
            </div>
            <div className="chat-sheet-actions">
              <button
                type="button"
                className="chat-sheet-secondary"
                onClick={() => void handleCancel()}
                disabled={busy}
              >
                {busy ? APPOINTMENT_TEXT.common.loading : APPOINTMENT_TEXT.viewer.cancel}
              </button>
              <button
                type="button"
                className="chat-sheet-primary"
                onClick={() => {
                  setShowViewer(false);
                  setSelectedAppointment(null);
                }}
              >
                {APPOINTMENT_TEXT.viewer.close}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
});
