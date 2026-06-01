import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { apiRequest } from "../lib/api";
import { getAccessToken, getMemberId } from "../lib/auth";
import { useChatNotifications } from "../lib/chatNotifications";
import { getApiBaseUrl } from "../lib/config";
import { useLocation, useNavigate } from "@/lib/nextRouterCompat";
import { APPOINTMENT_TEXT } from "@/lib/appointmentText";
import {
  AppointmentInfoPanel,
  type AppointmentInfoPanelHandle
} from "./AppointmentInfoPanel";
import { ChatFloatPill } from "@/features/chat/ChatFloatPill";
import { ChatFloatingProductPanel } from "@/features/chat/ChatFloatingProductPanel";
import { ChatFloatingSkeleton } from "@/features/chat/ChatFloatingSkeleton";
import { ChatMessageList } from "@/features/chat/ChatMessageList";
import type {
  AppointmentResponse,
  ChatRoomDetail,
  SocketMessage,
  StompClientLike
} from "@/features/chat/floatingTypes";
import {
  clamp,
  formatPrice,
  normalizeSocketMessage
} from "@/features/chat/floatingUtils";

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

function buildAppointmentCreatedMessage(payload: {
  kind: "APPOINTMENT_CREATED";
  meet_at: string;
  reminder_minutes: number | null;
  partner_nickname: string;
}) {
  return JSON.stringify(payload);
}

function buildAppointmentCanceledMessage(payload: {
  kind: "APPOINTMENT_CANCELED";
  canceled_at: string;
  partner_nickname: string;
}) {
  return JSON.stringify(payload);
}

export type ChatFloatingWindowProps = {
  chatRoomId: string;
  partnerNickname: string;
  minimized: boolean;
  zIndex: number;
  initialPosition: { x: number; y: number };
  size: { width: number; height: number };
  onMinimize: (chatRoomId: string) => void;
  onRestore: (chatRoomId: string) => void;
  onClose: (chatRoomId: string) => void;
  onActivate: (chatRoomId: string) => void;
  onResize: (chatRoomId: string, size: { width: number; height: number }) => void;
};

export function ChatFloatingWindow({
  chatRoomId,
  partnerNickname,
  minimized,
  zIndex,
  initialPosition,
  size,
  onMinimize,
  onRestore,
  onClose,
  onActivate,
  onResize
}: ChatFloatingWindowProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const memberId = getMemberId();
  const { markRoomRead } = useChatNotifications();
  const appointmentInfoRef = useRef<AppointmentInfoPanelHandle | null>(null);
  const appointmentMenuRef = useRef<HTMLDivElement | null>(null);
  const stompRef = useRef<StompClientLike | null>(null);
  const dragRef = useRef<{ offsetX: number; offsetY: number } | null>(null);
  const resizeRef = useRef<{ startX: number; startY: number; startWidth: number; startHeight: number } | null>(
    null
  );
  const draggedRef = useRef(false);
  const sizeRef = useRef(size);
  const [room, setRoom] = useState<ChatRoomDetail | null>(null);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [connected, setConnected] = useState(false);
  const [position, setPosition] = useState(initialPosition);
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [showAppointmentMenu, setShowAppointmentMenu] = useState(false);
  const [appointmentToast, setAppointmentToast] = useState("");

  useEffect(() => {
    sizeRef.current = size;
  }, [size]);

  const title = room?.partner_nickname ?? partnerNickname;
  const subtitle = useMemo(() => {
    if (!room) {
      return "Chat room";
    }

    const price = formatPrice(room.listing_price);
    return room.region_name ? `${room.region_name} · ${price}` : price;
  }, [room]);
  const currentAppointment = room?.current_appointment ?? null;
  const showLoadingShell = loading && !room;
  const listingImageUrl = room?.listing_first_image ?? null;
  const listingTransactionType = room?.listing_transaction_type ?? "sell";
  const appointmentBlockedMessage = getAppointmentBlockedMessage(room, memberId);
  const canSendMessage = Boolean(room && room.listing_status !== "SOLD_OUT");

  const handlePublishSystemMessage = (content: string) => {
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

  useEffect(() => {
    if (minimized) {
      return;
    }

    let disposed = false;

    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const response = await apiRequest<ChatRoomDetail>(`/api/chat-rooms/${chatRoomId}`);
        if (disposed) {
          return;
        }

        setRoom(response);
        markRoomRead(chatRoomId);
      } catch (err) {
        if (!disposed) {
          setError(err instanceof Error ? err.message : "Failed to load chat room.");
        }
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      disposed = true;
    };
  }, [chatRoomId, markRoomRead, minimized]);

  useEffect(() => {
    if (!showAppointmentMenu) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (appointmentMenuRef.current?.contains(event.target as Node)) {
        return;
      }

      setShowAppointmentMenu(false);
    };

    window.addEventListener("pointerdown", handlePointerDown);
    return () => window.removeEventListener("pointerdown", handlePointerDown);
  }, [showAppointmentMenu]);

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

  const handleCreateAppointment = async (payload: { meetAt: string; reminderMinutes: number | null }) => {
    if (!room) {
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

      return response;
    } catch (err) {
      throw new Error(err instanceof Error ? err.message : "약속을 만들지 못했습니다.");
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

  const handleLocationCheck = () => {
    if (!room?.listing_id) {
      return;
    }

    setShowAppointmentMenu(false);
    navigate(`/listing/${room.listing_id}`, { state: { backgroundLocation: location } });
  };

  useEffect(() => {
    if (minimized) {
      void stompRef.current?.deactivate();
      stompRef.current = null;
      setConnected(false);
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

              markRoomRead(chatRoomId);
            });
          },
          onDisconnect: () => {
            if (!disposed) {
              setConnected(false);
            }
          }
        });

        stompRef.current = client as unknown as StompClientLike;
        client.activate();
      } catch (err) {
        if (!disposed) {
          setError(err instanceof Error ? err.message : "Failed to connect chat.");
        }
      }
    };

    void connect();

    return () => {
      disposed = true;
      void stompRef.current?.deactivate();
      stompRef.current = null;
      setConnected(false);
    };
  }, [chatRoomId, markRoomRead, minimized]);

  useEffect(() => {
    if (minimized) {
      return;
    }

    const lastMessage = room?.messages[room.messages.length - 1];
    if (!lastMessage) {
      return;
    }

    const chatBody = document.querySelector(`[data-chat-room="${chatRoomId}"] .chat-float-messages`);
    if (chatBody) {
      (chatBody as HTMLElement).scrollTop = (chatBody as HTMLElement).scrollHeight;
    }
  }, [chatRoomId, minimized, room?.messages]);

  useEffect(() => {
    const onMove = (event: PointerEvent) => {
      if (resizeRef.current) {
        const viewportMaxWidth = Math.max(320, window.innerWidth - 32);
        const viewportMaxHeight = Math.max(360, window.innerHeight - 32);
        const nextWidth = clamp(
          resizeRef.current.startWidth + (event.clientX - resizeRef.current.startX),
          320,
          viewportMaxWidth
        );
        const nextHeight = clamp(
          resizeRef.current.startHeight + (event.clientY - resizeRef.current.startY),
          360,
          viewportMaxHeight
        );

        onResize(chatRoomId, { width: nextWidth, height: nextHeight });
        return;
      }

      if (!dragRef.current) {
        return;
      }

      draggedRef.current = true;
      setPosition({
        x: clamp(event.clientX - dragRef.current.offsetX, 16, window.innerWidth - sizeRef.current.width),
        y: clamp(event.clientY - dragRef.current.offsetY, 16, window.innerHeight - sizeRef.current.height)
      });
    };

    const onUp = () => {
      dragRef.current = null;
      resizeRef.current = null;
    };

    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);

    return () => {
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onUp);
    };
  }, []);

  const handleHeaderPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.target instanceof HTMLElement && event.target.closest("button")) {
      return;
    }

    event.preventDefault();
    draggedRef.current = false;
    onActivate(chatRoomId);
    dragRef.current = {
      offsetX: event.clientX - position.x,
      offsetY: event.clientY - position.y
    };
  };

  const handlePillPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.target instanceof HTMLElement && event.target.closest("button")) {
      return;
    }

    event.preventDefault();
    draggedRef.current = false;
    onActivate(chatRoomId);
    dragRef.current = {
      offsetX: event.clientX - position.x,
      offsetY: event.clientY - position.y
    };
  };

  const handleResizePointerDown = (event: ReactPointerEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    draggedRef.current = false;
    onActivate(chatRoomId);
    resizeRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      startWidth: sizeRef.current.width,
      startHeight: sizeRef.current.height
    };
  };

  const handleSend = () => {
    if (!canSendMessage) {
      setError("거래 완료된 게시글은 채팅할 수 없습니다.");
      return;
    }

    const content = draft.trim();
    if (!content || !stompRef.current?.connected) {
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

  if (minimized) {
    return (
      <ChatFloatPill
        title={title}
        position={position}
        zIndex={zIndex}
        onPointerDown={handlePillPointerDown}
        onRestore={() => {
          if (draggedRef.current) {
            draggedRef.current = false;
            return;
          }

          onRestore(chatRoomId);
        }}
        onClose={() => onClose(chatRoomId)}
      />
    );
  }

  return (
    <div
      className="chat-float-window"
      style={{ left: position.x, top: position.y, width: size.width, height: size.height, zIndex }}
      data-chat-room={chatRoomId}
      onMouseDown={() => onActivate(chatRoomId)}
    >
      <header className="chat-float-header" onPointerDown={handleHeaderPointerDown}>
        <div className="chat-float-header-copy">
          <strong>{title}</strong>
          <span>{subtitle}</span>
        </div>
        <div className="chat-float-header-actions">
          <button type="button" className="chat-float-minimize" aria-label="닫기" onClick={() => onMinimize(chatRoomId)}>
            &minus;
          </button>
          <button type="button" className="chat-float-close" aria-label="종료" onClick={() => onClose(chatRoomId)}>
            ×
          </button>
        </div>
      </header>

      <div className="chat-float-body">
        {showLoadingShell ? (
          <ChatFloatingSkeleton />
        ) : room ? (
          <ChatFloatingProductPanel
            room={room}
            listingImageUrl={listingImageUrl}
            listingTransactionType={listingTransactionType}
            showAppointmentMenu={showAppointmentMenu}
            appointmentActionLabel={currentAppointment ? "약속 보기" : APPOINTMENT_TEXT.quick.openMenu}
            appointmentMenuRef={appointmentMenuRef}
            onToggleAppointmentMenu={() => setShowAppointmentMenu((current) => !current)}
            onOpenAppointment={() => {
              setShowAppointmentMenu(false);
              if (currentAppointment) {
                appointmentInfoRef.current?.openViewer(currentAppointment);
                return;
              }

              appointmentInfoRef.current?.openComposer();
            }}
            onLocationCheck={handleLocationCheck}
          />
        ) : null}
        {room ? (
          <AppointmentInfoPanel
            ref={appointmentInfoRef}
            partnerNickname={room.partner_nickname ?? partnerNickname}
            currentAppointment={currentAppointment}
            busy={statusUpdating}
            canComplete={false}
            appointmentBlockedMessage={appointmentBlockedMessage}
            overlayMode="inline"
            onCreateAppointment={handleCreateAppointment}
            onCancelAppointment={handleCancelAppointment}
            onOpenCompletion={() => {}}
            onAppointmentBlocked={setAppointmentToast}
          />
        ) : null}
        {error ? <p className="auth-error">{error}</p> : null}

        {!loading && !error ? (
          <ChatMessageList messages={room?.messages ?? []} memberId={memberId} />
        ) : null}
      </div>

      <footer className="chat-float-compose">
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={!canSendMessage ? "거래 완료된 채팅입니다" : connected ? "메시지를 입력하세요" : "연결 중.."}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              handleSend();
            }
          }}
        />
        <button type="button" onClick={handleSend} disabled={!draft.trim() || !connected || !canSendMessage}>
          전송
        </button>
      </footer>
      {appointmentToast ? <div className="chat-float-toast">{appointmentToast}</div> : null}
      <button
        type="button"
        className="chat-float-resize-handle"
        aria-label="resize chat window"
        onPointerDown={handleResizePointerDown}
      />
    </div>
  );
}
