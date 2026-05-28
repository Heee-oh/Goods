import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession } from "../lib/auth";
import {
  groupChatRooms,
  normalizeChatRoomSummary,
  type ChatRoomSummary,
  type RawChatRoomSummary
} from "../lib/chatRooms";
import { useChatNotifications } from "../lib/chatNotifications";
import { readCachedJson, writeCachedJson } from "../lib/cache";
import { getMemberId } from "../lib/auth";
import { ChatFloatingWindow } from "./ChatFloatingWindow";
import { ChatDockList } from "@/features/chat/ChatDockList";
import {
  CHAT_ROOM_PREVIEW_EVENT,
  CHAT_ROOMS_CACHE_PREFIX,
  OPEN_CHAT_ROOM_EVENT
} from "@/features/chat/dockConstants";
import type {
  ChatRoomPreviewUpdateEventDetail,
  OpenChatRoomEventDetail,
  OpenRoomState
} from "@/features/chat/dockTypes";
import {
  createInitialPosition,
  createInitialSize,
  registerChatDockRooms
} from "@/features/chat/dockUtils";

let chatDockSessionMemberId: string | null = null;
let chatDockSessionRooms: ChatRoomSummary[] | null = null;

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

  const toggleListingGroup = useCallback((listingId: string) => {
    setExpandedListingIds((current) => {
      const next = new Set(current);
      if (next.has(listingId)) {
        next.delete(listingId);
      } else {
        next.add(listingId);
      }
      return next;
    });
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

        <ChatDockList
          entries={groupedEntries}
          listRef={listRef}
          expandedListingIds={expandedListingIds}
          openRoomIds={openRoomIds}
          openRoomIdsByListing={openRoomIdsByListing}
          unreadCountByRoom={unreadCountByRoom}
          onToggleListing={toggleListingGroup}
          onOpenRoom={openRoom}
        />
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
