"use client";

import { useEffect, useState } from "react";
import { useNavigate } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "@/lib/api";
import { hasAccessToken } from "@/lib/auth";
import { useChatNotifications } from "@/lib/chatNotifications";

type ReviewPrompt = {
  trade_id: number | string;
  partner_nickname: string;
  listing_title: string;
  writer_is_seller: boolean;
};

type TradeCompletionPrompt = {
  appointment_id: number | string;
  listing_id: number | string;
  chat_room_id: number | string;
  buyer_id: number | string;
  partner_nickname: string;
  listing_title: string;
};

const REVIEW_PROMPT_EVENT = "goods:review-prompt";
const OPEN_CHAT_ROOM_EVENT = "goods:open-chat-room";

function ChatToastViewport() {
  const { toast, dismissToast, markRoomRead } = useChatNotifications();

  if (!toast) {
    return null;
  }

  return (
    <button
      type="button"
      className="chat-toast"
      onClick={() => {
        markRoomRead(toast.roomId);
        dismissToast();
        window.dispatchEvent(
          new CustomEvent(OPEN_CHAT_ROOM_EVENT, {
            detail: {
              chatRoomId: toast.roomId,
              partnerNickname: toast.partnerNickname
            }
          })
        );
      }}
    >
      <span className="chat-toast-icon" aria-hidden="true">
        {"\uD1A1"}
      </span>
      <span className="chat-toast-copy">
        <strong>{toast.partnerNickname}</strong>
        <span>{toast.content}</span>
      </span>
      <span className="chat-toast-close" aria-hidden="true">
        {"\uC5F4\uAE30"}
      </span>
    </button>
  );
}

function AppointmentReminderViewport() {
  const navigate = useNavigate();
  const { appointmentReminder, dismissAppointmentReminder } = useChatNotifications();

  if (!appointmentReminder) {
    return null;
  }

  const reminderTime = appointmentReminder.meetAt
    ? new Date(appointmentReminder.meetAt).toLocaleString("ko-KR", {
        month: "long",
        day: "numeric",
        weekday: "short",
        hour: "numeric",
        minute: "2-digit"
      })
    : "";

  const handleOpenChat = () => {
    if (appointmentReminder.chatRoomId) {
      const isDesktop = typeof window !== "undefined" && window.matchMedia("(min-width: 1200px)").matches;
      const hasTradeRail = typeof document !== "undefined" && document.querySelector(".listing-right-rail");

      if (isDesktop && hasTradeRail) {
        window.dispatchEvent(
          new CustomEvent(OPEN_CHAT_ROOM_EVENT, {
            detail: {
              chatRoomId: appointmentReminder.chatRoomId,
              partnerNickname: appointmentReminder.partnerNickname
            }
          })
        );
      } else {
        navigate(`/chatting/${appointmentReminder.chatRoomId}`);
      }
    }
    dismissAppointmentReminder();
  };

  return (
    <div className="trade-prompt-overlay" onClick={dismissAppointmentReminder}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{appointmentReminder.partnerNickname}</strong>님과의 약속 시간이 다가옵니다.
        </p>
        {reminderTime ? <p>{reminderTime}에 예정된 약속입니다.</p> : null}
        <button type="button" className="chat-confirm-primary" onClick={handleOpenChat}>
          {appointmentReminder.chatRoomId ? "채팅방 열기" : "확인"}
        </button>
        <button type="button" className="chat-confirm-secondary" onClick={dismissAppointmentReminder}>
          닫기
        </button>
      </div>
    </div>
  );
}

function ReviewPromptViewport() {
  const [prompt, setPrompt] = useState<ReviewPrompt | null>(null);
  const [isWriting, setIsWriting] = useState(false);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [snoozedPrompt, setSnoozedPrompt] = useState<{ tradeId: string; until: number } | null>(null);

  useEffect(() => {
    const openPrompt = (nextPrompt: ReviewPrompt) => {
      const tradeId = String(nextPrompt.trade_id);
      if (snoozedPrompt?.tradeId === tradeId && snoozedPrompt.until > Date.now()) {
        return;
      }

      setPrompt(nextPrompt);
      setIsWriting(false);
      setRating(5);
      setComment("");
      setError("");
    };

    const handleReviewPrompt = (event: Event) => {
      const customEvent = event as CustomEvent<ReviewPrompt>;
      if (!customEvent.detail?.trade_id || !customEvent.detail?.partner_nickname) {
        return;
      }

      openPrompt(customEvent.detail);
    };

    window.addEventListener(REVIEW_PROMPT_EVENT, handleReviewPrompt as EventListener);

    return () => {
      window.removeEventListener(REVIEW_PROMPT_EVENT, handleReviewPrompt as EventListener);
    };
  }, [snoozedPrompt]);

  useEffect(() => {
    let disposed = false;

    const loadPrompt = async () => {
      if (!hasAccessToken() || busy) {
        return;
      }

      try {
        const response = await apiRequest<ReviewPrompt | null>("/api/trades/review-prompt");
        if (disposed || !response?.trade_id || !response.partner_nickname) {
          return;
        }

        const tradeId = String(response.trade_id);
        if (snoozedPrompt?.tradeId === tradeId && snoozedPrompt.until > Date.now()) {
          return;
        }

        setPrompt(response);
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          setPrompt(null);
        }
      }
    };

    void loadPrompt();
    const intervalId = window.setInterval(() => void loadPrompt(), 15_000);

    return () => {
      disposed = true;
      window.clearInterval(intervalId);
    };
  }, [busy, snoozedPrompt]);

  if (!prompt) {
    return null;
  }

  const dismissPrompt = () => {
    if (busy) {
      return;
    }
    setPrompt(null);
  };

  const handleSubmit = async () => {
    if (!prompt) {
      return;
    }

    try {
      setBusy(true);
      setError("");
      await apiRequest(`/api/trades/${prompt.trade_id}/reviews`, {
        method: "POST",
        body: JSON.stringify({
          is_seller: prompt.writer_is_seller,
          rating,
          comment: comment.trim() ? comment.trim() : null
        })
      });
      setPrompt(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "리뷰를 등록하지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="trade-prompt-overlay" onClick={dismissPrompt}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        {!isWriting ? (
          <>
            <p>
              <strong>{prompt.partner_nickname}</strong>님과의 거래가 완료되었습니다.
            </p>
            <p>{prompt.listing_title}에 리뷰를 남겨주세요.</p>
            <button type="button" className="chat-confirm-primary" onClick={() => setIsWriting(true)}>
              리뷰 남기기
            </button>
            <button
              type="button"
              className="chat-confirm-secondary"
              onClick={() => {
                setSnoozedPrompt({
                  tradeId: String(prompt.trade_id),
                  until: Date.now() + 5 * 60_000
                });
                dismissPrompt();
              }}
            >
              나중에
            </button>
          </>
        ) : (
          <>
            <p>
              <strong>{prompt.partner_nickname}</strong>님은 이번 거래가 어땠나요?
            </p>
            {prompt.listing_title ? <p>{prompt.listing_title}</p> : null}
            <div className="review-rating-row">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  key={value}
                  type="button"
                  className={rating === value ? "review-rating-chip active" : "review-rating-chip"}
                  disabled={busy}
                  onClick={() => setRating(value)}
                >
                  {value}점
                </button>
              ))}
            </div>
            <textarea
              className="review-prompt-textarea"
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              placeholder="거래 경험을 남겨주세요."
              maxLength={500}
              disabled={busy}
            />
            {error ? <p className="review-prompt-error">{error}</p> : null}
            <button type="button" className="chat-confirm-primary" disabled={busy} onClick={() => void handleSubmit()}>
              {busy ? "등록 중..." : "리뷰 등록"}
            </button>
            <button
              type="button"
              className="chat-confirm-secondary"
              disabled={busy}
              onClick={() => {
                setIsWriting(false);
                setError("");
              }}
            >
              뒤로
            </button>
          </>
        )}
      </div>
    </div>
  );
}

function TradeCompletionPromptViewport() {
  const [prompt, setPrompt] = useState<TradeCompletionPrompt | null>(null);
  const [busy, setBusy] = useState(false);
  const [snoozedPrompt, setSnoozedPrompt] = useState<{ appointmentId: string; until: number } | null>(null);

  useEffect(() => {
    let disposed = false;

    const loadPrompt = async () => {
      if (!hasAccessToken() || busy) {
        return;
      }

      try {
        const response = await apiRequest<TradeCompletionPrompt | null>("/api/appointments/trade-prompt");
        if (disposed || !response) {
          return;
        }

        const appointmentId = String(response.appointment_id);
        if (snoozedPrompt?.appointmentId === appointmentId && snoozedPrompt.until > Date.now()) {
          return;
        }

        setPrompt(response);
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          setPrompt(null);
        }
      }
    };

    void loadPrompt();
    const intervalId = window.setInterval(() => void loadPrompt(), 60_000);

    return () => {
      disposed = true;
      window.clearInterval(intervalId);
    };
  }, [busy, snoozedPrompt]);

  if (!prompt) {
    return null;
  }

  const handleComplete = async () => {
    try {
      setBusy(true);
      await apiRequest(`/api/listings/${prompt.listing_id}/sold-out`, {
        method: "POST",
        body: JSON.stringify({
          buyer_id: String(prompt.buyer_id)
        })
      });
      setPrompt(null);
    } finally {
      setBusy(false);
    }
  };

  const handleNoTrade = async () => {
    try {
      setBusy(true);
      await apiRequest(`/api/listings/${prompt.listing_id}/reserve/cancel`, {
        method: "POST"
      });
      await apiRequest(`/api/appointments/${prompt.appointment_id}/trade-prompt/dismiss`, {
        method: "POST"
      });
      setPrompt(null);
    } finally {
      setBusy(false);
    }
  };

  const handleLater = () => {
    setSnoozedPrompt({
      appointmentId: String(prompt.appointment_id),
      until: Date.now() + 5 * 60_000
    });
    setPrompt(null);
  };

  return (
    <div className="trade-prompt-overlay" onClick={handleLater}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{prompt.partner_nickname}</strong>님과의 거래를 완료했나요?
        </p>
        <p>
          완료하면 게시글이 판매완료로 변경되고<br />
          구매자에게 리뷰 요청 알림이 전송됩니다.
        </p>
        <button type="button" className="chat-confirm-primary" disabled={busy} onClick={() => void handleComplete()}>
          {busy ? "처리 중..." : "거래 완료"}
        </button>
        <button type="button" className="chat-confirm-secondary" disabled={busy} onClick={() => void handleNoTrade()}>
          {busy ? "처리 중..." : "거래하지 않음"}
        </button>
        <button type="button" className="chat-confirm-secondary" disabled={busy} onClick={handleLater}>
          나중에
        </button>
      </div>
    </div>
  );
}

export function AppOverlays() {
  return (
    <>
      <ChatToastViewport />
      <AppointmentReminderViewport />
      <TradeCompletionPromptViewport />
      <ReviewPromptViewport />
    </>
  );
}
