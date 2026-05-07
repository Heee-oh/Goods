import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
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

const DEFAULT_FLOAT_SIZE = { width: 380, height: 540 };
const CHAT_ROOMS_CACHE_PREFIX = "goods:chat-rooms";

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
  const { registerRooms, unreadCountByRoom, markRoomRead } = useChatNotifications();
  const [rooms, setRooms] = useState<ChatRoomSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [openRooms, setOpenRooms] = useState<OpenRoomState[]>([]);
  const [expandedListingIds, setExpandedListingIds] = useState<Set<string>>(() => new Set());
  const zIndexRef = useRef(20);

  const loadRooms = useCallback(async () => {
    try {
      setError("");

      const memberId = getMemberId();
      const cacheKey = memberId ? `${CHAT_ROOMS_CACHE_PREFIX}:${memberId}` : CHAT_ROOMS_CACHE_PREFIX;
      const cachedRooms = readCachedJson<RawChatRoomSummary[]>(cacheKey);
      if (cachedRooms) {
        const nextRooms = cachedRooms
          .map(normalizeChatRoomSummary)
          .filter((room): room is ChatRoomSummary => room !== null);

        setRooms(nextRooms);
        registerRooms(
          nextRooms.map((room) => ({
            chatRoomId: room.chatRoomId,
            partnerNickname: room.partnerNickname
          }))
        );
        setLoading(false);
        return;
      }

      setLoading(true);
      const response = await apiRequest<RawChatRoomSummary[]>("/api/chat-rooms");
      writeCachedJson(cacheKey, response);
      const nextRooms = response
        .map(normalizeChatRoomSummary)
        .filter((room): room is ChatRoomSummary => room !== null);

      setRooms(nextRooms);
      registerRooms(
        nextRooms.map((room) => ({
          chatRoomId: room.chatRoomId,
          partnerNickname: room.partnerNickname
        }))
      );
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigate("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "Failed to load chat rooms.");
    } finally {
      setLoading(false);
    }
  }, [navigate, registerRooms]);

  useEffect(() => {
    void loadRooms();
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
  const windowRoot = typeof document !== "undefined" ? document.body : null;

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

        <div className="chat-dock-list">
          {groupedEntries.map((entry) => {
            if (entry.kind === "group") {
              const isExpanded = expandedListingIds.has(entry.listingId);
              const isActive = isExpanded || openRoomIdsByListing.has(entry.listingId);
              const unreadCount = entry.rooms.reduce((sum, room) => sum + (unreadCountByRoom[room.chatRoomId] ?? 0), 0);

              return (
                <div key={`listing-${entry.listingId}`} className="chat-dock-group">
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
