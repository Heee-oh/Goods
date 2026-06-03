import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession } from "../lib/auth";
import {
  formatChatRoomLastMessage,
  groupChatRooms,
  getListingPriceLabel,
  normalizeChatRoomSummary,
  type ChatRoomSummary,
  type RawChatRoomSummary
} from "../lib/chatRooms";
import { getListingStatusLabel, getListingStatusTone } from "../lib/listingStatus";
import { useChatNotifications } from "../lib/chatNotifications";

const chatFilters = [
  "\uC804\uCCB4",
  "\uD310\uB9E4",
  "\uAD6C\uB9E4/\uAD50\uD658",
  "\uC548\uC77D\uC74C"
] as const;
const CHAT_ROOM_PREVIEW_EVENT = "goods:chat-room-preview-updated";

type ChatRoomPreviewUpdateEventDetail = {
  chatRoomId?: number | string;
  content?: string;
  createdAt?: string;
};

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

  const diffMonths = Math.floor(diffDays / 30);
  return `${diffMonths}\uB2EC \uC804`;
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

export function ChatListPage() {
  const navigate = useNavigate();
  const { registerRooms, roomsVersion, unreadCountByRoom, markRoomRead } = useChatNotifications();
  const [filter, setFilter] = useState<(typeof chatFilters)[number]>("\uC804\uCCB4");
  const [rooms, setRooms] = useState<ChatRoomSummary[]>([]);
  const [expandedListingIds, setExpandedListingIds] = useState<Set<string>>(() => new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const hasSeenRoomsVersionRef = useRef(false);

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

  const loadRooms = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await apiRequest<RawChatRoomSummary[]>("/api/chat-rooms");
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

  useEffect(() => {
    if (roomsVersion === 0) {
      return;
    }

    if (!hasSeenRoomsVersionRef.current) {
      hasSeenRoomsVersionRef.current = true;
      return;
    }

    void loadRooms();
  }, [loadRooms, roomsVersion]);

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
      setRooms((current) =>
        current
          .map((room) =>
            room.chatRoomId === normalizedChatRoomId
              ? {
                  ...room,
                  lastMessage: formatChatRoomLastMessage(content),
                  lastMessageAt: createdAt ?? new Date().toISOString()
                }
              : room
          )
          .sort((left, right) => new Date(right.lastMessageAt).getTime() - new Date(left.lastMessageAt).getTime())
      );
    };

    window.addEventListener(CHAT_ROOM_PREVIEW_EVENT, handlePreviewUpdate as EventListener);

    return () => {
      window.removeEventListener(CHAT_ROOM_PREVIEW_EVENT, handlePreviewUpdate as EventListener);
    };
  }, []);

  const visibleEntries = useMemo(() => {
    const filteredRooms = rooms.filter((room) => {
      if (filter === "\uD310\uB9E4") {
        return room.sellerView;
      }

      if (filter === "\uAD6C\uB9E4/\uAD50\uD658") {
        return !room.sellerView;
      }

      if (filter === "\uC548\uC77D\uC74C") {
        return (unreadCountByRoom[room.chatRoomId] ?? 0) > 0;
      }

      return true;
    });

    return groupChatRooms(filteredRooms);
  }, [filter, rooms, unreadCountByRoom]);

  return (
    <div className="page page-chat-list">
      <header className="chat-list-header">
        <h1>{"\uCC44\uD305"}</h1>
        <div className="chat-list-header-actions">
          <button type="button" aria-label="alerts">
            <span className="icon-bell" aria-hidden="true" />
            <span className="chat-dot" />
          </button>
          <button type="button" aria-label="settings">
            {"\u2699"}
          </button>
        </div>
      </header>

      <section className="chat-filter-row">
        <button type="button" className="chat-filter-icon" aria-label="filter">
          {"\u2630"}
        </button>
        {chatFilters.map((item) => (
          <button
            key={item}
            type="button"
            className={item === filter ? "chat-filter active" : "chat-filter"}
            onClick={() => setFilter(item)}
          >
            {item}
          </button>
        ))}
      </section>

      <section className="chat-notice">
        <div>
          <strong>{"\uC54C\uB9BC\uC744 \uCF1C\uC8FC\uC138\uC694"}</strong>
          {" "}{"\uC911\uC694\uD55C \uBA54\uC2DC\uC9C0\uB97C \uB193\uCE58\uC9C0 \uC54A\uB3C4\uB85D OS \uC124\uC815\uC5D0\uC11C \uC54C\uB9BC\uC744 \uCF1C\uC8FC\uC138\uC694."}
        </div>
        <span>{"\u203A"}</span>
      </section>

      {loading ? <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p> : null}
      {error ? <p className="auth-error">{error}</p> : null}

      <section className="chat-list">
        {visibleEntries.map((entry) => {
          if (entry.kind === "group") {
            const isExpanded = expandedListingIds.has(entry.listingId);
            const unreadCount = entry.rooms.reduce((sum, room) => sum + (unreadCountByRoom[room.chatRoomId] ?? 0), 0);

            return (
              <div key={`listing-${entry.listingId}`} className="chat-list-group">
                <button
                  type="button"
                  className={isExpanded ? "chat-list-group-item active" : "chat-list-group-item"}
                  onClick={() => toggleListingGroup(entry.listingId)}
                >
                  <div className="chat-list-group-media">
                    <ListingThumbnail imageUrl={entry.listingFirstImage} listingTitle={entry.listingTitle} />
                    <span className={`chat-list-status-badge ${getListingStatusTone(entry.listingStatus)}`}>
                      {getListingStatusLabel(entry.listingStatus, entry.listingTransactionType)}
                    </span>
                  </div>
                  <div className="chat-list-group-copy">
                    <strong>{entry.listingTitle ?? "\uC0C1\uD488 \uC815\uBCF4"}</strong>
                    <p>{getListingPriceLabel(entry.listingPrice, entry.listingTransactionType)}</p>
                  </div>
                  <span className="chat-list-group-count">{entry.rooms.length}개</span>
                  {unreadCount ? <span className="chat-list-unread">{unreadCount}</span> : null}
                </button>

                {isExpanded ? (
                  <div className="chat-list-group-rooms">
                    {entry.rooms.map((room) => (
                      <button
                        key={room.chatRoomId}
                        type="button"
                        className="chat-list-item chat-list-item-nested"
                        onClick={() => {
                          markRoomRead(room.chatRoomId);
                          navigate(`/chatting/${room.chatRoomId}`);
                        }}
                      >
                        <ChatThumbnail profileImage={room.partnerProfileImage} partnerNickname={room.partnerNickname} />
                        <div className="chat-list-copy">
                          <div className="chat-list-title-row">
                            <strong>{room.partnerNickname}</strong>
                            {room.regionName ? <span>{room.regionName}</span> : null}
                            <span>{"\u00B7"}</span>
                            <span>{formatRelativeTime(room.lastMessageAt)}</span>
                          </div>
                          <p className="chat-list-last-message">
                            {formatChatRoomLastMessage(room.lastMessage) || "\uBA54\uC2DC\uC9C0\uAC00 \uC544\uC9C1 \uC5C6\uC5B4\uC694"}
                          </p>
                        </div>
                        {unreadCountByRoom[room.chatRoomId] ? (
                          <span className="chat-list-unread">{unreadCountByRoom[room.chatRoomId]}</span>
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
              className="chat-list-item"
              onClick={() => {
                markRoomRead(room.chatRoomId);
                navigate(`/chatting/${room.chatRoomId}`);
              }}
            >
              <ChatThumbnail profileImage={room.partnerProfileImage} partnerNickname={room.partnerNickname} />
              <div className="chat-list-copy">
                <div className="chat-list-title-row">
                  <strong>{room.partnerNickname}</strong>
                  {room.regionName ? <span>{room.regionName}</span> : null}
                  <span>{"\u00B7"}</span>
                  <span>{formatRelativeTime(room.lastMessageAt)}</span>
                </div>
                <p className="chat-list-last-message">
                  {formatChatRoomLastMessage(room.lastMessage) || "\uBA54\uC2DC\uC9C0\uAC00 \uC544\uC9C1 \uC5C6\uC5B4\uC694"}
                </p>
              </div>
              {unreadCountByRoom[room.chatRoomId] ? (
                <span className="chat-list-unread">{unreadCountByRoom[room.chatRoomId]}</span>
              ) : null}
            </button>
          );
        })}
      </section>

      {!loading && !error && visibleEntries.length === 0 ? (
        <p className="region-status">{"\uC544\uC9C1 \uCC44\uD305\uBC29\uC774 \uC5C6\uC5B4\uC694"}</p>
      ) : null}
    </div>
  );
}
