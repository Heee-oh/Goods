import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { apiRequest } from "../lib/api";
import { getAccessToken, getMemberId, getSelectedRegionId } from "../lib/auth";
import { useChatNotifications } from "../lib/chatNotifications";
import { useLocation, useNavigate } from "react-router-dom";
import {
  AppointmentInfoPanel,
  type AppointmentInfoPanelHandle
} from "./AppointmentInfoPanel";
import { APPOINTMENT_TEXT } from "../lib/appointmentText";
import { getTransactionLabel, type TransactionType } from "../lib/transactionType";

type ChatMessageItem = {
  message_id: number | string;
  sender_id: string;
  type: "TEXT" | "IMAGE" | "SYSTEM";
  content: string;
  created_at: string;
};

type ChatRoomDetail = {
  chat_room_id: number | string;
  listing_id: number | string;
  partner_nickname: string;
  partner_profile_image: string | null;
  partner_smile_score: number | null;
  listing_title: string;
  listing_price: number | null;
  region_name?: string | null;
  current_appointment?: {
    appointment_id: number | string;
    meet_at: string;
    reminder_minutes: number | null;
  } | null;
  messages: ChatMessageItem[];
};

type ListingDetailResponse = {
  title?: string;
  price_amount?: number | null;
  transaction_type?: TransactionType;
  images?: Array<{
    image_url?: string;
    sort_order?: number;
  }>;
  distance_km?: number | null;
  distanceKm?: number | null;
};

type AppointmentResponse = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
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

type StompClientLike = {
  connected?: boolean;
  activate: () => void;
  deactivate: () => Promise<void> | void;
  subscribe: (destination: string, callback: (frame: { body: string }) => void) => void;
  publish: (options: { destination: string; body: string }) => void;
};

function normalizeSocketMessage(message: SocketMessage): ChatMessageItem {
  return {
    message_id: String(message.message_id ?? message.messageId ?? Date.now()),
    sender_id: String(message.sender_id ?? message.senderId ?? ""),
    type: message.type,
    content: message.content,
    created_at: message.created_at ?? message.createdAt ?? new Date().toISOString()
  };
}

function formatMessageTime(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });
}

function formatPrice(price: number | null) {
  if (price == null) {
    return "Price unknown";
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

function formatListingLabel(price: number | null, transactionType: TransactionType) {
  if (transactionType !== "sell" || price == null || price === 0) {
    return getTransactionLabel(transactionType);
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
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
  const [listingImageUrl, setListingImageUrl] = useState<string | null>(null);
  const [listingTransactionType, setListingTransactionType] = useState<TransactionType>("sell");
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [showAppointmentMenu, setShowAppointmentMenu] = useState(false);

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
    if (minimized || !room?.listing_id) {
      return;
    }

    let disposed = false;
    setListingImageUrl(null);
    setListingTransactionType("sell");

    const loadListing = async () => {
      try {
        const selectedRegionId = getSelectedRegionId();
        const response = await apiRequest<ListingDetailResponse>(
          `/api/listings/${room.listing_id}${selectedRegionId ? `?region_id=${selectedRegionId}` : ""}`
        );
        if (disposed) {
          return;
        }

        setListingImageUrl(response.images?.[0]?.image_url ?? null);
        setListingTransactionType(response.transaction_type ?? "sell");
      } catch {
        if (!disposed) {
          setListingImageUrl(null);
          setListingTransactionType("sell");
        }
      }
    };

    void loadListing();

    return () => {
      disposed = true;
    };
  }, [minimized, room?.listing_id]);

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

  const handleCreateAppointment = async (payload: { meetAt: string; reminderMinutes: number | null }) => {
    if (!room) {
      throw new Error("채팅방 정보를 불러오지 못했습니다.");
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

      return response;
    } catch (err) {
      throw new Error(err instanceof Error ? err.message : "약속을 만들지 못했습니다.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleCancelAppointment = async (appointmentId: number | string) => {
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
        const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

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
      <div
        className="chat-float-pill"
        style={{ left: position.x, top: position.y, zIndex }}
        role="button"
        tabIndex={0}
        onPointerDown={handlePillPointerDown}
        onClick={() => {
          if (draggedRef.current) {
            draggedRef.current = false;
            return;
          }

          onRestore(chatRoomId);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onRestore(chatRoomId);
          }
        }}
      >
        <span className="chat-float-pill-name">{title}</span>
        <button
          type="button"
          className="chat-float-pill-close"
          aria-label="종료"
          onClick={(event) => {
            event.stopPropagation();
            onClose(chatRoomId);
          }}
        >
          ×
        </button>
      </div>
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
          <>
            <section className="chat-float-product chat-float-product-skeleton" aria-hidden="true">
              <div className="chat-float-product-media chat-float-skeleton-box" />
              <div className="chat-float-product-copy">
                <div className="chat-float-skeleton-line chat-float-skeleton-line-title" />
                <div className="chat-float-skeleton-line chat-float-skeleton-line-price" />
              </div>
              <div className="chat-float-skeleton-pill" />
            </section>
            <div className="chat-float-messages chat-float-messages-skeleton" aria-hidden="true">
              <div className="chat-float-message-skeleton mine" />
              <div className="chat-float-message-skeleton" />
              <div className="chat-float-message-skeleton mine short" />
            </div>
          </>
        ) : room ? (
          <section className="chat-float-product">
            <div className="chat-float-product-media">
              {listingImageUrl ? (
                <img src={listingImageUrl} alt={room.listing_title} />
              ) : (
                <div className="chat-float-product-placeholder" />
              )}
            </div>
            <div className="chat-float-product-copy">
              <strong>{room.listing_title}</strong>
              <p>{formatListingLabel(room.listing_price, listingTransactionType)}</p>
            </div>
            <div className="chat-float-action-wrap" ref={appointmentMenuRef}>
              <button
                type="button"
                className="chat-float-action-toggle"
                onClick={() => setShowAppointmentMenu((current) => !current)}
              >
                <span>{APPOINTMENT_TEXT.quick.label}</span>
                <span aria-hidden="true">{showAppointmentMenu ? "▴" : "▾"}</span>
              </button>
              {showAppointmentMenu ? (
                <div className="chat-float-action-menu">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAppointmentMenu(false);
                      if (currentAppointment) {
                        appointmentInfoRef.current?.openViewer(currentAppointment);
                        return;
                      }

                      appointmentInfoRef.current?.openComposer();
                    }}
                  >
                    {APPOINTMENT_TEXT.quick.openMenu}
                  </button>
                  <button type="button" onClick={handleLocationCheck}>
                    {APPOINTMENT_TEXT.quick.placePrefix} {APPOINTMENT_TEXT.quick.placeSuffix}
                  </button>
                </div>
              ) : null}
            </div>
          </section>
        ) : null}
        {room ? (
          <AppointmentInfoPanel
            ref={appointmentInfoRef}
            partnerNickname={room.partner_nickname ?? partnerNickname}
            currentAppointment={currentAppointment}
            busy={statusUpdating}
            canComplete={false}
            overlayMode="inline"
            onCreateAppointment={handleCreateAppointment}
            onCancelAppointment={handleCancelAppointment}
            onOpenCompletion={() => {}}
          />
        ) : null}
        {error ? <p className="auth-error">{error}</p> : null}

        {!loading && !error ? (
          <div className="chat-float-messages">
            {(room?.messages ?? []).map((message) => {
              const mine = memberId != null && String(message.sender_id) === String(memberId);
              return (
                <div key={message.message_id} className={mine ? "chat-message-row mine" : "chat-message-row"}>
                  <div className="chat-message-stack">
                    <div className={mine ? "chat-message-bubble mine" : "chat-message-bubble"}>{message.content}</div>
                    <span className="chat-message-time">{formatMessageTime(message.created_at)}</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : null}
      </div>

      <footer className="chat-float-compose">
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={connected ? "메시지를 입력하세요" : "연결 중.."}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              handleSend();
            }
          }}
        />
        <button type="button" onClick={handleSend} disabled={!draft.trim() || !connected}>
          전송
        </button>
      </footer>
      <button
        type="button"
        className="chat-float-resize-handle"
        aria-label="resize chat window"
        onPointerDown={handleResizePointerDown}
      />
    </div>
  );
}

