import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession } from "../lib/auth";
import {
  groupChatRooms,
  getListingPriceLabel,
  normalizeChatRoomSummary,
  type ChatRoomSummary,
  type RawChatRoomSummary
} from "../lib/chatRooms";
import { getListingStatusLabel, getListingStatusTone } from "../lib/listingStatus";
import { useChatNotifications } from "../lib/chatNotifications";
import { readCachedJson, writeCachedJson } from "../lib/cache";
import { getMemberId } from "../lib/auth";
import { ChatFloatingWindow } from "./ChatFloatingWindow";

type OpenRoomState = {
  chatRoomId: string;
  partnerNickname: string;
  minimized: boolean;
  zIndex: number;
  position: { x: number; y: number };
  size: { width: number; height: number };
};

type OpenChatRoomEventDetail = {
  chatRoomId?: number | string;
  partnerNickname?: string | null;
};

type ChatRoomPreviewUpdateEventDetail = {
  chatRoomId?: number | string;
  content?: string;
  createdAt?: string;
};

const DEFAULT_FLOAT_SIZE = { width: 380, height: 540 };
const CHAT_ROOMS_CACHE_PREFIX = "goods:chat-rooms";
const CHAT_ROOM_PREVIEW_EVENT = "goods:chat-room-preview-updated";
const OPEN_CHAT_ROOM_EVENT = "goods:open-chat-room";
let chatDockSessionMemberId: string | null = null;
let chatDockSessionRooms: ChatRoomSummary[] | null = null;

function registerChatDockRooms(rooms: ChatRoomSummary[]) {
  return rooms.map((room) => ({
    chatRoomId: room.chatRoomId,
    partnerNickname: room.partnerNickname
  }));
}

function formatRelativeTime(value: string) {
  const createdAt = new Date(value);
  const diffMinutes = Math.max(1, Math.floor((Date.now() - createdAt.getTime()) / 60000));

  if (diffMinutes < 60) {
    return `${diffMinutes}\uBD84 \uC804`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}\uC2DC\uAC04 \uC804`;
  }

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 30) {
    return `${diffDays}\uC77C \uC804`;
  }

  return `${Math.floor(diffDays / 30)}\uB2EC \uC804`;
}

function ChatThumbnail({
  profileImage,
  partnerNickname
}: {
  profileImage: string | null;
  partnerNickname: string;
}) {
  if (profileImage) {
    return <img className="chat-list-thumb chat-list-thumb-profile" src={profileImage} alt={partnerNickname} />;
  }

  return (
    <div className="chat-list-thumb chat-list-thumb-empty">
      <div className="chat-list-thumb-mark" />
    </div>
  );
}

function ListingThumbnail({
  imageUrl,
  listingTitle
}: {
  imageUrl: string | null;
  listingTitle: string | null;
}) {
  if (imageUrl) {
    return <img className="chat-list-thumb chat-list-thumb-listing" src={imageUrl} alt={listingTitle ?? ""} />;
  }

  return (
    <div className="chat-list-thumb chat-list-thumb-listing chat-list-thumb-empty">
      <div className="chat-list-thumb-mark" />
    </div>
  );
}

function createInitialPosition(index: number) {
  if (typeof window === "undefined") {
    return { x: 40, y: 120 };
  }

  const x = Math.max(16, window.innerWidth - DEFAULT_FLOAT_SIZE.width - 24 - index * 24);
  const y = Math.max(16, 100 + index * 24);
  return { x, y };
}

function createInitialSize() {
  return { ...DEFAULT_FLOAT_SIZE };
}

export function ChatDock() {
  const navigate = useNavigate();
  const { registerRooms, roomsVersion, unreadCountByRoom, markRoomRead } = useChatNotifications();
  const initialSessionRooms =
    chatDockSessionRooms && chatDockSessionMemberId === getMemberId() ? chatDockSessionRooms : null;
  const [rooms, setRooms] = useState<ChatRoomSummary[]>(() => initialSessionRooms ?? []);
  const [loading, setLoading] = useState(() => !initialSessionRooms);
  const [error, setError] = useState("");
  const [openRooms, setOpenRooms] = useState<OpenRoomState[]>([]);
  const [expandedListingIds, setExpandedListingIds] = useState<Set<string>>(() => new Set());
  const [windowRoot, setWindowRoot] = useState<HTMLElement | null>(null);
  const zIndexRef = useRef(20);
  const listRef = useRef<HTMLDivElement | null>(null);
  const previousRectsRef = useRef<Map<string, DOMRect>>(new Map());
  const animationFrameRef = useRef<number | null>(null);
  const animationTimerRef = useRef<number | null>(null);
  const navigateRef = useRef(navigate);
  const registerRoomsRef = useRef(registerRooms);

  useEffect(() => {
    navigateRef.current = navigate;
  }, [navigate]);

  useEffect(() => {
    registerRoomsRef.current = registerRooms;
  }, [registerRooms]);

  const loadRooms = useCallback(async (options: { forceNetwork?: boolean; useCache?: boolean } = {}) => {
    const memberId = getMemberId();
    if (!options.forceNetwork && chatDockSessionRooms && chatDockSessionMemberId === memberId) {
      setRooms(chatDockSessionRooms);
      registerRoomsRef.current(registerChatDockRooms(chatDockSessionRooms));
      setLoading(false);
      return;
    }

    const useCache = options.useCache ?? true;

    try {
      setError("");

      const cacheKey = memberId ? `${CHAT_ROOMS_CACHE_PREFIX}:${memberId}` : CHAT_ROOMS_CACHE_PREFIX;
      const cachedRooms = useCache ? readCachedJson<RawChatRoomSummary[]>(cacheKey) : null;
      if (cachedRooms) {
        const nextRooms = cachedRooms
          .map(normalizeChatRoomSummary)
          .filter((room): room is ChatRoomSummary => room !== null);

        setRooms(nextRooms);
        chatDockSessionMemberId = memberId;
        chatDockSessionRooms = nextRooms;
        registerRoomsRef.current(registerChatDockRooms(nextRooms));
        setLoading(false);
      } else {
        setLoading(true);
      }

      const response = await apiRequest<RawChatRoomSummary[]>("/api/chat-rooms");
      writeCachedJson(cacheKey, response);
      const nextRooms = response
        .map(normalizeChatRoomSummary)
        .filter((room): room is ChatRoomSummary => room !== null);

      setRooms(nextRooms);
      chatDockSessionMemberId = memberId;
      chatDockSessionRooms = nextRooms;
      registerRoomsRef.current(registerChatDockRooms(nextRooms));
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigateRef.current("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "Failed to load chat rooms.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setWindowRoot(document.body);
  }, []);

  useEffect(() => {
    void loadRooms();
  }, [loadRooms]);

  useEffect(() => {
    if (roomsVersion === 0) {
      return;
    }

    void loadRooms({ forceNetwork: true, useCache: false });
  }, [loadRooms, roomsVersion]);

  useEffect(() => {
    const reloadOnFocus = () => {
      void loadRooms({ forceNetwork: true, useCache: false });
    };

    window.addEventListener("focus", reloadOnFocus);

    return () => {
      window.removeEventListener("focus", reloadOnFocus);
    };
  }, [loadRooms]);

  const openRoom = useCallback(
    (room: ChatRoomSummary) => {
      markRoomRead(room.chatRoomId);
      zIndexRef.current += 1;
      const nextZIndex = zIndexRef.current;

      setOpenRooms((current) => {
        const existing = current.find((item) => item.chatRoomId === room.chatRoomId);
        if (existing) {
          return current.map((item) =>
            item.chatRoomId === room.chatRoomId
              ? {
                  ...item,
                  minimized: false,
                  zIndex: nextZIndex
                }
              : item
          );
        }

        return [
          ...current,
          {
            chatRoomId: room.chatRoomId,
            partnerNickname: room.partnerNickname,
            minimized: false,
            zIndex: nextZIndex,
            position: createInitialPosition(current.length),
            size: createInitialSize()
          }
        ];
      });
    },
    [markRoomRead]
  );

  useEffect(() => {
    const handlePreviewUpdate = (event: Event) => {
      const customEvent = event as CustomEvent<ChatRoomPreviewUpdateEventDetail>;
      const chatRoomId = customEvent.detail?.chatRoomId;
      const content = customEvent.detail?.content;
      const createdAt = customEvent.detail?.createdAt;

      if (chatRoomId == null || !content) {
        return;
      }

      const normalizedChatRoomId = String(chatRoomId);
      setRooms((current) => {
        const nextRooms = current
          .map((room) =>
            room.chatRoomId === normalizedChatRoomId
              ? {
                  ...room,
                  lastMessage: content,
                  lastMessageAt: createdAt ?? new Date().toISOString()
                }
              : room
          )
          .sort((left, right) => new Date(right.lastMessageAt).getTime() - new Date(left.lastMessageAt).getTime());
        chatDockSessionRooms = nextRooms;
        chatDockSessionMemberId = getMemberId();
        return nextRooms;
      });
    };

    window.addEventListener(CHAT_ROOM_PREVIEW_EVENT, handlePreviewUpdate as EventListener);
    return () => window.removeEventListener(CHAT_ROOM_PREVIEW_EVENT, handlePreviewUpdate as EventListener);
  }, []);

  useLayoutEffect(() => {
    const root = listRef.current;
    if (!root) {
      return;
    }

    const elements = Array.from(root.querySelectorAll<HTMLElement>("[data-chat-dock-key]"));
    const currentRects = new Map<string, DOMRect>();

    elements.forEach((element) => {
      const key = element.dataset.chatDockKey;
      if (!key) {
        return;
      }

      currentRects.set(key, element.getBoundingClientRect());
    });

    const previousRects = previousRectsRef.current;
    if (previousRects.size > 0) {
      elements.forEach((element) => {
        const key = element.dataset.chatDockKey;
        if (!key) {
          return;
        }

        const previousRect = previousRects.get(key);
        const currentRect = currentRects.get(key);
        if (!previousRect || !currentRect) {
          return;
        }

        const deltaX = previousRect.left - currentRect.left;
        const deltaY = previousRect.top - currentRect.top;
        if (!deltaX && !deltaY) {
          return;
        }

        element.style.transition = "transform 0s";
        element.style.transform = `translate(${deltaX}px, ${deltaY}px)`;
        element.style.willChange = "transform";
      });

      if (animationFrameRef.current !== null) {
        window.cancelAnimationFrame(animationFrameRef.current);
      }

      animationFrameRef.current = window.requestAnimationFrame(() => {
        elements.forEach((element) => {
          if (element.style.transform) {
            element.style.transition = "transform 220ms cubic-bezier(0.22, 1, 0.36, 1)";
            element.style.transform = "";
          }
        });

        if (animationTimerRef.current !== null) {
          window.clearTimeout(animationTimerRef.current);
        }

        animationTimerRef.current = window.setTimeout(() => {
          elements.forEach((element) => {
            element.style.transition = "";
            element.style.willChange = "";
          });
          animationTimerRef.current = null;
        }, 240);
      });
    }

    previousRectsRef.current = currentRects;

    return () => {
      if (animationFrameRef.current !== null) {
        window.cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }

      if (animationTimerRef.current !== null) {
        window.clearTimeout(animationTimerRef.current);
        animationTimerRef.current = null;
      }
    };
  }, [rooms, expandedListingIds]);

  useEffect(() => {
    const handleOpenChatRoom = (event: Event) => {
      const customEvent = event as CustomEvent<OpenChatRoomEventDetail>;
      const chatRoomId = customEvent.detail?.chatRoomId;
      if (chatRoomId == null) {
        return;
      }

      openRoom({
        chatRoomId: String(chatRoomId),
        listingId: null,
        listingTitle: null,
        listingPrice: null,
        listingStatus: "PUBLISHED",
        listingTransactionType: "sell",
        listingFirstImage: null,
        sellerView: false,
        partnerNickname: customEvent.detail.partnerNickname ?? "상대방",
        partnerProfileImage: null,
        regionName: null,
        lastMessage: "",
        lastMessageAt: new Date().toISOString()
      });
    };

    window.addEventListener(OPEN_CHAT_ROOM_EVENT, handleOpenChatRoom as EventListener);
    return () => window.removeEventListener(OPEN_CHAT_ROOM_EVENT, handleOpenChatRoom as EventListener);
  }, [openRoom]);

  const activateRoom = useCallback((chatRoomId: string) => {
    zIndexRef.current += 1;
    const nextZIndex = zIndexRef.current;

    setOpenRooms((current) =>
      current.map((item) => (item.chatRoomId === chatRoomId ? { ...item, zIndex: nextZIndex } : item))
    );
  }, []);

  const minimizeRoom = useCallback((chatRoomId: string) => {
    setOpenRooms((current) =>
      current.map((item) => (item.chatRoomId === chatRoomId ? { ...item, minimized: true } : item))
    );
  }, []);

  const restoreRoom = useCallback((chatRoomId: string) => {
    zIndexRef.current += 1;
    const nextZIndex = zIndexRef.current;

    setOpenRooms((current) =>
      current.map((item) =>
        item.chatRoomId === chatRoomId ? { ...item, minimized: false, zIndex: nextZIndex } : item
      )
    );
  }, []);

  const resizeRoom = useCallback((chatRoomId: string, size: { width: number; height: number }) => {
    setOpenRooms((current) =>
      current.map((item) => (item.chatRoomId === chatRoomId ? { ...item, size } : item))
    );
  }, []);

  const closeRoom = useCallback((chatRoomId: string) => {
    setOpenRooms((current) => current.filter((item) => item.chatRoomId !== chatRoomId));
  }, []);

  const openRoomIds = useMemo(() => new Set(openRooms.map((item) => item.chatRoomId)), [openRooms]);
  const openRoomIdsByListing = useMemo(() => {
    const next = new Set<string>();
    rooms.forEach((room) => {
      if (!room.sellerView || !room.listingId) {
        return;
      }

      if (openRoomIds.has(room.chatRoomId)) {
        next.add(room.listingId);
      }
    });
    return next;
  }, [openRoomIds, rooms]);

  const groupedEntries = useMemo(() => groupChatRooms(rooms), [rooms]);
  return (
    <>
      <div className="chat-dock">
        <div className="chat-dock-head">
          <div>
            <h2>{"\uCC44\uD305\uBC29"}</h2>
            <p>{"\uAC00\uB85C\uB85C \uB4E4\uC5B4\uAC00 \uBCF4\uBA74 \uD55C \uBC88\uC5D0 \uD655\uC778\uD560 \uC218 \uC788\uC5B4\uC694."}</p>
          </div>
          <span>{rooms.length}</span>
        </div>

        {loading ? <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p> : null}
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="chat-dock-list" ref={listRef}>
          {groupedEntries.map((entry) => {
            if (entry.kind === "group") {
              const isExpanded = expandedListingIds.has(entry.listingId);
              const isActive = isExpanded || openRoomIdsByListing.has(entry.listingId);
              const unreadCount = entry.rooms.reduce((sum, room) => sum + (unreadCountByRoom[room.chatRoomId] ?? 0), 0);

              return (
                <div
                  key={`listing-${entry.listingId}`}
                  data-chat-dock-key={`group-${entry.listingId}`}
                  className="chat-dock-group"
                >
                  <button
                    type="button"
                    className={isActive ? "chat-dock-group-head active" : "chat-dock-group-head"}
                    onClick={() => {
                      setExpandedListingIds((current) => {
                        const next = new Set(current);
                        if (next.has(entry.listingId)) {
                          next.delete(entry.listingId);
                        } else {
                          next.add(entry.listingId);
                        }
                        return next;
                      });
                    }}
                  >
                    <div className="chat-dock-group-media">
                      <ListingThumbnail imageUrl={entry.listingFirstImage} listingTitle={entry.listingTitle} />
                      <span className={`chat-list-status-badge ${getListingStatusTone(entry.listingStatus)}`}>
                        {getListingStatusLabel(entry.listingStatus, entry.listingTransactionType)}
                      </span>
                    </div>
                    <div className="chat-dock-group-copy">
                      <strong>{entry.listingTitle ?? "\uC0C1\uD488 \uC815\uBCF4"}</strong>
                      <p>{getListingPriceLabel(entry.listingPrice, entry.listingTransactionType)}</p>
                    </div>
                    <span className="chat-dock-group-count">{entry.rooms.length}개</span>
                    {unreadCount ? <em className="chat-dock-unread">{unreadCount}</em> : null}
                  </button>

                  {isExpanded ? (
                    <div className="chat-dock-group-rooms">
                      {entry.rooms.map((room) => (
                        <button
                          key={room.chatRoomId}
                          type="button"
                          className={openRoomIds.has(room.chatRoomId) ? "chat-dock-item active" : "chat-dock-item"}
                          onClick={() => openRoom(room)}
                        >
                          <ChatThumbnail profileImage={room.partnerProfileImage} partnerNickname={room.partnerNickname} />
                          <div className="chat-dock-copy">
                            <div className="chat-dock-title-row">
                              <strong>{room.partnerNickname}</strong>
                              {room.regionName ? <span>{room.regionName}</span> : null}
                            </div>
                            <p>{room.lastMessage || "\uBA54\uC2DC\uC9C0\uAC00 \uC544\uC9C1 \uC5C6\uC5B4\uC694"}</p>
                          </div>
                          <span className="chat-dock-time">{formatRelativeTime(room.lastMessageAt)}</span>
                          {unreadCountByRoom[room.chatRoomId] ? (
                            <em className="chat-dock-unread">{unreadCountByRoom[room.chatRoomId]}</em>
                          ) : null}
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              );
            }

            const room = entry.room;
              return (
                <button
                  key={room.chatRoomId}
                  type="button"
                  data-chat-dock-key={`room-${room.chatRoomId}`}
                  className={openRoomIds.has(room.chatRoomId) ? "chat-dock-item active" : "chat-dock-item"}
                  onClick={() => openRoom(room)}
                >
                <ChatThumbnail profileImage={room.partnerProfileImage} partnerNickname={room.partnerNickname} />
                <div className="chat-dock-copy">
                  <div className="chat-dock-title-row">
                    <strong>{room.partnerNickname}</strong>
                    {room.regionName ? <span>{room.regionName}</span> : null}
                  </div>
                  <p>{room.lastMessage || "\uBA54\uC2DC\uC9C0\uAC00 \uC544\uC9C1 \uC5C6\uC5B4\uC694"}</p>
                </div>
                <span className="chat-dock-time">{formatRelativeTime(room.lastMessageAt)}</span>
                {unreadCountByRoom[room.chatRoomId] ? (
                  <em className="chat-dock-unread">{unreadCountByRoom[room.chatRoomId]}</em>
                ) : null}
              </button>
            );
          })}
        </div>
      </div>

      {windowRoot
        ? createPortal(
            <div className="chat-floating-layer">
              {openRooms.map((room) => (
                <ChatFloatingWindow
                  key={room.chatRoomId}
                  chatRoomId={room.chatRoomId}
                  partnerNickname={room.partnerNickname}
                  minimized={room.minimized}
                  zIndex={room.zIndex}
                  initialPosition={room.position}
                  size={room.size}
                  onActivate={activateRoom}
                  onMinimize={minimizeRoom}
                  onRestore={restoreRoom}
                  onResize={resizeRoom}
                  onClose={closeRoom}
                />
              ))}
            </div>,
            windowRoot
          )
        : null}
    </>
  );
}
