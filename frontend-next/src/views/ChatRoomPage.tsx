import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession, getAccessToken, getMemberId } from "../lib/auth";
import { useChatNotifications } from "../lib/chatNotifications";
import { getApiBaseUrl } from "../lib/config";
import {
  AppointmentInfoPanel,
  type AppointmentInfoPanelHandle
} from "../components/AppointmentInfoPanel";
import { CompletionActionSheet } from "../components/CompletionActionSheet";
import { useExchangeState } from "../components/useExchangeState";
import { getTransactionLabel, type TransactionType } from "../lib/transactionType";

type ChatMessageItem = {
  message_id: number | string;
  sender_id: string;
  type: "TEXT" | "IMAGE" | "SYSTEM";
  content: string;
  created_at: string;
};

type CurrentAppointment = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
};

type ChatRoomDetail = {
  chat_room_id: number | string;
  listing_id: number | string;
  listing_first_image: string | null;
  listing_status: string | null;
  listing_reserver_id: number | string | null;
  listing_transaction_type: TransactionType | null;
  seller_id: number | string;
  partner_id: number | string;
  partner_nickname: string;
  partner_profile_image: string | null;
  partner_smile_score: number | null;
  listing_title: string;
  listing_price: number | null;
  current_appointment: CurrentAppointment | null;
  messages: ChatMessageItem[];
};

type SocketMessage = {
  message_id?: number | string;
  messageId?: number | string;
  sender_id?: number | string;
  senderId?: number | string;
  type: "TEXT" | "IMAGE" | "SYSTEM";
  content: string;
  created_at?: string;
  createdAt?: string;
};

type AppointmentResponse = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
};

type AppointmentCreatedPayload = {
  kind: "APPOINTMENT_CREATED";
  meet_at: string;
  reminder_minutes: number | null;
  partner_nickname: string;
};

type AppointmentCanceledPayload = {
  kind: "APPOINTMENT_CANCELED";
  canceled_at: string;
  partner_nickname: string;
};

type AppointmentEvent = AppointmentCreatedPayload | AppointmentCanceledPayload;

type DatedMessage = ChatMessageItem & {
  showDateLabel: boolean;
  dateLabel: string;
  mine: boolean;
  appointmentEvent: AppointmentEvent | null;
};

type StompClientLike = {
  connected?: boolean;
  activate: () => void;
  deactivate: () => Promise<void> | void;
  subscribe: (destination: string, callback: (frame: { body: string }) => void) => void;
  publish: (options: { destination: string; body: string }) => void;
};

function formatSmileScore(value: number | null) {
  if (value == null) {
    return "10.0\uC810";
  }

  return `${(value / 10).toFixed(1)}\uC810`;
}

function formatPrice(price: number | null) {
  if (price == null) {
    return "가격 미정";
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

function formatMessageTime(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });
}

function formatDateLabel(value: string) {
  return new Date(value).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric"
  });
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

function normalizeSocketMessage(message: SocketMessage): ChatMessageItem {
  return {
    message_id: String(message.message_id ?? message.messageId ?? Date.now()),
    sender_id: String(message.sender_id ?? message.senderId ?? ""),
    type: message.type,
    content: message.content,
    created_at: message.created_at ?? message.createdAt ?? new Date().toISOString()
  };
}

function buildAppointmentCreatedMessage(payload: AppointmentCreatedPayload) {
  return JSON.stringify(payload);
}

function buildAppointmentCanceledMessage(payload: AppointmentCanceledPayload) {
  return JSON.stringify(payload);
}

function parseAppointmentEvent(content: string): AppointmentEvent | null {
  try {
    const parsed = JSON.parse(content) as Partial<AppointmentEvent> & { kind?: string };

    if (parsed.kind === "APPOINTMENT_CREATED" && typeof parsed.meet_at === "string") {
      return {
        kind: "APPOINTMENT_CREATED",
        meet_at: parsed.meet_at,
        reminder_minutes: typeof parsed.reminder_minutes === "number" ? parsed.reminder_minutes : null,
        partner_nickname:
          typeof parsed.partner_nickname === "string" ? parsed.partner_nickname : "상대방"
      };
    }

    if (parsed.kind === "APPOINTMENT_CANCELED" && typeof parsed.canceled_at === "string") {
      return {
        kind: "APPOINTMENT_CANCELED",
        canceled_at: parsed.canceled_at,
        partner_nickname:
          typeof parsed.partner_nickname === "string" ? parsed.partner_nickname : "상대방"
      };
    }

    return null;
  } catch {
    return null;
  }
}

function formatReminderText(value: number | null) {
  switch (value) {
    case 5:
      return "5분 전";
    case 15:
      return "15분 전";
    case 30:
      return "30분 전";
    case 60:
      return "1시간 전";
    case 120:
      return "2시간 전";
    default:
      return "없음";
  }
}

function normalizeId(value: number | string | null | undefined) {
  return value == null ? null : String(value);
}

function getAppointmentBlockedMessage(room: ChatRoomDetail | null, memberId: string | null) {
  if (!room) {
    return "";
  }

  if (room.listing_status === "SOLD_OUT") {
    return "거래 완료된 게시글입니다.";
  }

  if (room.listing_status !== "RESERVED") {
    return "";
  }

  const reserverId = normalizeId(room.listing_reserver_id);
  if (!reserverId || !memberId) {
    return "다른 사람과 거래중입니다.";
  }

  const participantBuyerId = normalizeId(room.seller_id) === memberId
    ? normalizeId(room.partner_id)
    : memberId;

  return participantBuyerId === reserverId ? "" : "다른 사람과 거래중입니다.";
}

export function ChatRoomPage() {
  const navigate = useNavigate();
  const { chatRoomId } = useParams();
  const memberId = getMemberId();
  const { markRoomRead, registerRoom } = useChatNotifications();
  const stompRef = useRef<StompClientLike | null>(null);
  const appointmentInfoRef = useRef<AppointmentInfoPanelHandle | null>(null);
  const [room, setRoom] = useState<ChatRoomDetail | null>(null);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [showReserveConfirm, setShowReserveConfirm] = useState(false);
  const [showCompletionSheet, setShowCompletionSheet] = useState(false);
  const [appointmentToast, setAppointmentToast] = useState("");
  const [statusUpdating, setStatusUpdating] = useState(false);

  useEffect(() => {
    if (!chatRoomId) {
      return;
    }

    markRoomRead(chatRoomId);
  }, [chatRoomId, markRoomRead]);

  useEffect(() => {
    const load = async () => {
      if (!chatRoomId) {
        navigate("/chatting", { replace: true });
        return;
      }

      try {
        setLoading(true);
        setError("");
        const response = await apiRequest<ChatRoomDetail>(`/api/chat-rooms/${chatRoomId}`);
        setRoom(response);
        registerRoom({
          chatRoomId: String(response.chat_room_id),
          partnerNickname: response.partner_nickname
        });
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          clearSession();
          navigate("/welcome", { replace: true });
          return;
        }

        setError(err instanceof Error ? err.message : "채팅방을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [chatRoomId, navigate, registerRoom]);

  useEffect(() => {
    if (!chatRoomId) {
      return;
    }

    let disposed = false;

    const connect = async () => {
      try {
        if (typeof globalThis !== "undefined" && !("global" in globalThis)) {
          (globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;
        }

        const [{ Client }, { default: SockJS }] = await Promise.all([
          import("@stomp/stompjs"),
          import("sockjs-client")
        ]);

        if (disposed) {
          return;
        }

        const token = getAccessToken();
        const apiBaseUrl = getApiBaseUrl();

        const client = new Client({
          webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws-chat`),
          reconnectDelay: 5000,
          connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
          onConnect: () => {
            if (disposed) {
              return;
            }

            setConnected(true);
            client.subscribe(`/sub/chat/room/${chatRoomId}`, (frame: { body: string }) => {
              const payload = JSON.parse(frame.body) as SocketMessage;
              const nextMessage = normalizeSocketMessage(payload);

              setRoom((current) => {
                if (
                  !current ||
                  current.messages.some(
                    (message) => String(message.message_id) === String(nextMessage.message_id)
                  )
                ) {
                  return current;
                }

                if (
                  nextMessage.type === "SYSTEM" &&
                  current.messages.some(
                    (message) =>
                      message.type === "SYSTEM" &&
                      message.content === nextMessage.content &&
                      String(message.sender_id) === String(nextMessage.sender_id)
                  )
                ) {
                  return current;
                }

                return {
                  ...current,
                  messages: [...current.messages, nextMessage]
                };
              });

              markRoomRead(String(chatRoomId));
            });
          },
          onDisconnect: () => {
            if (!disposed) {
              setConnected(false);
            }
          },
          onStompError: (frame: { headers: Record<string, string> }) => {
            if (!disposed) {
              setError(frame.headers.message ?? "채팅 연결에 실패했습니다.");
            }
          },
          onWebSocketError: () => {
            if (!disposed) {
              setConnected(false);
            }
          }
        });

        stompRef.current = client as unknown as StompClientLike;
        client.activate();
      } catch (err) {
        if (!disposed) {
          setConnected(false);
          setError(err instanceof Error ? err.message : "채팅 연결에 실패했습니다.");
        }
      }
    };

    void connect();

    return () => {
      disposed = true;
      setConnected(false);
      void stompRef.current?.deactivate();
      stompRef.current = null;
    };
  }, [chatRoomId, markRoomRead]);

  useEffect(() => {
    if (!appointmentToast) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setAppointmentToast("");
    }, 2200);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [appointmentToast]);

  const currentAppointment = room?.current_appointment ?? null;
  const exchange = useExchangeState({
    room,
    currentAppointment
  });
  const listingImageUrl = room?.listing_first_image ?? null;
  const listingTransactionType = room?.listing_transaction_type ?? "sell";
  const listingPriceAmount = room?.listing_price ?? null;
  const listingPriceLabel =
    listingTransactionType !== "sell" || listingPriceAmount == null || listingPriceAmount === 0
      ? getTransactionLabel(listingTransactionType)
      : formatPrice(listingPriceAmount);
  const appointmentBlockedMessage = getAppointmentBlockedMessage(room, memberId);
  const canSendMessage = Boolean(room && room.listing_status !== "SOLD_OUT");

  const isSeller = useMemo(() => {
    if (!room || memberId == null) {
      return false;
    }

    return String(room.seller_id ?? "") === String(memberId);
  }, [room, memberId]);

  const datedMessages = useMemo<DatedMessage[]>(() => {
    if (!room) {
      return [];
    }

    const baseMessages = room.messages.map((message) => {
      const appointmentEvent = message.type === "SYSTEM" ? parseAppointmentEvent(message.content) : null;

      return {
        ...message,
        appointmentEvent
      };
    });

    const hasCurrentAppointmentCard =
      currentAppointment != null &&
      baseMessages.some(
        (message) =>
          message.appointmentEvent?.kind === "APPOINTMENT_CREATED" &&
          message.appointmentEvent.meet_at === currentAppointment.meet_at
      );

    const messagesWithFallback =
      currentAppointment != null && !hasCurrentAppointmentCard
        ? [
            ...baseMessages,
            {
              message_id: `appointment-fallback-${currentAppointment.appointment_id}`,
              sender_id: "",
              type: "SYSTEM" as const,
              content: buildAppointmentCreatedMessage({
                kind: "APPOINTMENT_CREATED",
                meet_at: currentAppointment.meet_at,
                reminder_minutes: currentAppointment.reminder_minutes,
                partner_nickname: room.partner_nickname
              }),
              created_at: currentAppointment.meet_at,
              appointmentEvent: {
                kind: "APPOINTMENT_CREATED" as const,
                meet_at: currentAppointment.meet_at,
                reminder_minutes: currentAppointment.reminder_minutes,
                partner_nickname: room.partner_nickname
              }
            }
          ]
        : baseMessages;

    return messagesWithFallback.map((message, index) => {
      const currentDate = formatDateLabel(message.created_at);
      const prevDate = index > 0 ? formatDateLabel(messagesWithFallback[index - 1].created_at) : null;

      return {
        ...message,
        showDateLabel: currentDate !== prevDate,
        dateLabel: currentDate,
        mine: memberId != null && String(message.sender_id) === String(memberId),
        appointmentEvent: message.appointmentEvent
      };
    });
  }, [currentAppointment, memberId, room]);

  const handlePublishSystemMessage = (content: string) => {
    if (!chatRoomId) {
      return;
    }

    setRoom((current) =>
      current
        ? {
            ...current,
            messages: [
              ...current.messages,
              {
                message_id: `local-${Date.now()}`,
                sender_id: String(memberId ?? ""),
                type: "SYSTEM",
                content,
                created_at: new Date().toISOString()
              }
            ]
          }
        : current
    );

    if (!stompRef.current?.connected) {
      return;
    }

    stompRef.current.publish({
      destination: "/pub/chat/message",
      body: JSON.stringify({
        chat_room_id: String(chatRoomId),
        type: "SYSTEM",
        content
      })
    });
  };

  const handleSend = () => {
    if (!canSendMessage) {
      setError("거래 완료된 게시글은 채팅할 수 없습니다.");
      return;
    }

    const content = draft.trim();
    if (!content || !chatRoomId || !stompRef.current?.connected) {
      return;
    }

    stompRef.current.publish({
      destination: "/pub/chat/message",
      body: JSON.stringify({
        chat_room_id: String(chatRoomId),
        type: "TEXT",
        content
      })
    });

    setDraft("");
  };

  const handleCreateAppointment = async (payload: {
    meetAt: string;
    reminderMinutes: number | null;
  }) => {
    if (!chatRoomId || !room) {
      throw new Error("채팅방 정보를 불러오지 못했습니다.");
    }
    if (appointmentBlockedMessage) {
      throw new Error(appointmentBlockedMessage);
    }

    try {
      setStatusUpdating(true);
      const response = await apiRequest<AppointmentResponse>(`/api/chat-rooms/${chatRoomId}/appointment`, {
        method: "POST",
        body: JSON.stringify({
          meet_at: payload.meetAt,
          reminder_minutes: payload.reminderMinutes
        })
      });

      setRoom((current) =>
        current
          ? {
              ...current,
              current_appointment: response
            }
          : current
      );

      handlePublishSystemMessage(
        buildAppointmentCreatedMessage({
          kind: "APPOINTMENT_CREATED",
          meet_at: response.meet_at,
          reminder_minutes: response.reminder_minutes,
          partner_nickname: room.partner_nickname
        })
      );

      if (isSeller && room.listing_status === "PUBLISHED") {
        setShowReserveConfirm(true);
      }

      return response;
    } catch (err) {
      throw new Error(err instanceof Error ? err.message : "약속을 저장하지 못했습니다.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleCancelAppointment = async (appointmentId: number | string) => {
    if (!room) {
      throw new Error("채팅방 정보를 불러오지 못했습니다.");
    }

    try {
      setStatusUpdating(true);
      await apiRequest(`/api/appointments/${appointmentId}`, {
        method: "DELETE"
      });

      setRoom((current) =>
        current
          ? {
              ...current,
              current_appointment: null
            }
          : current
      );

      handlePublishSystemMessage(
        buildAppointmentCanceledMessage({
          kind: "APPOINTMENT_CANCELED",
          canceled_at: new Date().toISOString(),
          partner_nickname: room.partner_nickname
        })
      );
    } catch (err) {
      throw new Error(err instanceof Error ? err.message : "약속을 취소하지 못했습니다.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleReserveListing = async () => {
    if (!room) {
      return;
    }

    try {
      setStatusUpdating(true);
      await apiRequest(`/api/listings/${room.listing_id}/reserve`, {
        method: "POST",
        body: JSON.stringify({
          buyer_id: String(room.partner_id)
        })
      });

      setRoom((current) =>
        current
          ? {
              ...current,
              listing_status: "RESERVED"
            }
          : current
      );
      handlePublishSystemMessage("예약이 성공적으로 잡혔습니다.");
      setShowReserveConfirm(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "예약 상태로 변경하지 못했습니다.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleOpenCompletion = () => {
    if (!exchange.canComplete) {
      return;
    }

    setShowCompletionSheet(true);
  };

  const handleCompleteExchange = async () => {
    if (!room) {
      return;
    }

    try {
      setStatusUpdating(true);
      await apiRequest(`/api/listings/${room.listing_id}/sold-out`, {
        method: "POST",
        body: JSON.stringify({
          buyer_id: String(room.partner_id)
        })
      });

      setRoom((current) =>
        current
          ? {
              ...current,
              listing_status: "SOLD_OUT"
            }
          : current
      );
      setShowCompletionSheet(false);
      setShowReserveConfirm(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "거래 완료 처리에 실패했습니다.");
    } finally {
      setStatusUpdating(false);
    }
  };

  return (
    <div className="main-screen chat-room-screen">
      <header className="chat-room-header">
        <button type="button" className="chat-room-back" onClick={() => navigate("/chatting")}>
          {"<"}
        </button>
        <div className="chat-room-header-copy">
          <strong>{room?.partner_nickname ?? "채팅"}</strong>
          {room ? <span className="chat-room-score">{formatSmileScore(room.partner_smile_score)}</span> : null}
        </div>
        <div className="chat-room-header-actions">
          <button type="button" aria-label="more">
            ...
          </button>
        </div>
      </header>

      {loading ? <p className="region-status">불러오는 중..</p> : null}
      {error ? <p className="auth-error">{error}</p> : null}

      {room ? (
        <>
          <section className="chat-room-product">
            <div className="chat-room-product-media">
              {listingImageUrl ? (
                <img src={listingImageUrl} alt={exchange.listingTitle} />
              ) : (
                <div className="chat-room-product-placeholder" />
              )}
            </div>
            <div className="chat-room-product-copy">
              <strong>{exchange.listingTitle}</strong>
              <p>{listingPriceLabel}</p>
              <p className="chat-room-exchange-state">
                {exchange.statusLabel}
                {exchange.appointmentLabel ? ` · ${exchange.appointmentLabel}` : ""}
              </p>
            </div>
          </section>

          <section className="chat-room-appointment-panel">
            <AppointmentInfoPanel
              ref={appointmentInfoRef}
              partnerNickname={exchange.partnerNickname}
              currentAppointment={currentAppointment}
              busy={statusUpdating}
              canComplete={exchange.canComplete}
              appointmentBlockedMessage={appointmentBlockedMessage}
              onCreateAppointment={handleCreateAppointment}
              onCancelAppointment={handleCancelAppointment}
              onOpenCompletion={handleOpenCompletion}
              onAppointmentBlocked={setAppointmentToast}
            />
          </section>

          <section className="chat-room-messages">
            {datedMessages.length === 0 ? <div className="chat-date-divider">대화가 시작되지 않았습니다.</div> : null}

            {datedMessages.map((message) => (
              <div key={String(message.message_id)}>
                {message.showDateLabel ? <div className="chat-date-divider">{message.dateLabel}</div> : null}

                {message.appointmentEvent?.kind === "APPOINTMENT_CREATED" ? (
                  <div className={message.mine ? "chat-appointment-row mine" : "chat-appointment-row"}>
                    <div className="chat-appointment-card">
                      <strong>약속이 설정되었습니다</strong>
                      <p>날짜: {formatAppointmentDate(message.appointmentEvent.meet_at)}</p>
                      <p>시간: {formatAppointmentTime(message.appointmentEvent.meet_at)}</p>
                      <button
                        type="button"
                        onClick={() => {
                          if (
                            currentAppointment &&
                            currentAppointment.meet_at ===
                              (message.appointmentEvent as AppointmentCreatedPayload).meet_at
                          ) {
                            appointmentInfoRef.current?.openViewer(currentAppointment);
                            return;
                          }

                          setAppointmentToast("현재 약속입니다.");
                        }}
                      >
                        약속 보기
                      </button>
                    </div>
                    {message.appointmentEvent.reminder_minutes != null ? (
                      <div className="chat-appointment-note">
                        약속 {formatReminderText(message.appointmentEvent.reminder_minutes)}에 알림이 울립니다.
                      </div>
                    ) : null}
                  </div>
                ) : message.appointmentEvent?.kind === "APPOINTMENT_CANCELED" ? (
                  <div className="chat-system-row">
                    <div className="chat-system-message">약속이 취소되었습니다.</div>
                  </div>
                ) : message.type === "SYSTEM" ? (
                  <div className="chat-system-row">
                    <div className="chat-system-message">{message.content}</div>
                  </div>
                ) : (
                  <div className={message.mine ? "chat-message-row mine" : "chat-message-row"}>
                    {!message.mine ? (
                      room.partner_profile_image ? (
                        <img
                          className="chat-message-avatar real-image"
                          src={room.partner_profile_image}
                          alt={room.partner_nickname}
                        />
                      ) : (
                        <div className="chat-message-avatar" />
                      )
                    ) : null}
                    <div className="chat-message-stack">
                      <div className={message.mine ? "chat-message-bubble mine" : "chat-message-bubble"}>
                        {message.content}
                      </div>
                      <span className="chat-message-time">{formatMessageTime(message.created_at)}</span>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </section>

          <footer className="chat-compose">
            <button type="button" className="chat-compose-plus" aria-label="add">
              +
            </button>
            <div className="chat-compose-field">
              <input
                type="text"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    handleSend();
                  }
                }}
                placeholder={!canSendMessage ? "거래 완료된 채팅입니다" : connected ? "메시지 보내기" : "연결 중.."}
              />
              <button type="button" className="chat-compose-emoji" aria-label="emoji">
                :)
              </button>
            </div>
            <button
              type="button"
              className="chat-compose-send"
              disabled={!draft.trim() || !connected || !canSendMessage}
              onClick={handleSend}
              aria-label="send"
            >
              ^
            </button>
          </footer>
        </>
      ) : null}

      {showReserveConfirm && room ? (
        <div className="chat-modal-overlay" onClick={() => setShowReserveConfirm(false)}>
          <div className="chat-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <p>
              <strong>{room.partner_nickname}</strong>와 약속을 만들었습니다.
            </p>
            <p>게시글을 예약중으로 변경할까요?</p>
            <button
              type="button"
              className="chat-confirm-primary"
              onClick={() => void handleReserveListing()}
              disabled={statusUpdating}
            >
              {statusUpdating ? "변경 중..." : "변경"}
            </button>
            <button
              type="button"
              className="chat-confirm-secondary"
              onClick={() => setShowReserveConfirm(false)}
              disabled={statusUpdating}
            >
              취소
            </button>
          </div>
        </div>
      ) : null}

      {showCompletionSheet && room ? (
        <CompletionActionSheet
          partnerNickname={room.partner_nickname}
          listingTitle={exchange.listingTitle}
          busy={statusUpdating}
          onConfirm={() => void handleCompleteExchange()}
          onClose={() => setShowCompletionSheet(false)}
        />
      ) : null}

      {appointmentToast ? <div className="chat-inline-toast">{appointmentToast}</div> : null}
    </div>
  );
}
