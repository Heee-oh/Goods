import type { RefObject } from "react";
import type { ChatRoomListEntry, ChatRoomSummary } from "@/lib/chatRooms";
import { getListingPriceLabel } from "@/lib/chatRooms";
import { getListingStatusLabel, getListingStatusTone } from "@/lib/listingStatus";
import { ChatThumbnail, ListingThumbnail } from "./ChatDockThumbnails";
import { formatRelativeTime } from "./dockUtils";

type ChatDockListProps = {
  entries: ChatRoomListEntry[];
  listRef: RefObject<HTMLDivElement | null>;
  expandedListingIds: Set<string>;
  openRoomIds: Set<string>;
  openRoomIdsByListing: Set<string>;
  unreadCountByRoom: Record<string, number>;
  onToggleListing: (listingId: string) => void;
  onOpenRoom: (room: ChatRoomSummary) => void;
};

export function ChatDockList({
  entries,
  listRef,
  expandedListingIds,
  openRoomIds,
  openRoomIdsByListing,
  unreadCountByRoom,
  onToggleListing,
  onOpenRoom
}: ChatDockListProps) {
  return (
    <div className="chat-dock-list" ref={listRef}>
      {entries.map((entry) => {
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
                onClick={() => onToggleListing(entry.listingId)}
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
                    <ChatDockRoomButton
                      key={room.chatRoomId}
                      room={room}
                      active={openRoomIds.has(room.chatRoomId)}
                      unreadCount={unreadCountByRoom[room.chatRoomId] ?? 0}
                      onOpenRoom={onOpenRoom}
                    />
                  ))}
                </div>
              ) : null}
            </div>
          );
        }

        const room = entry.room;
        return (
          <ChatDockRoomButton
            key={room.chatRoomId}
            room={room}
            active={openRoomIds.has(room.chatRoomId)}
            unreadCount={unreadCountByRoom[room.chatRoomId] ?? 0}
            dataKey={`room-${room.chatRoomId}`}
            onOpenRoom={onOpenRoom}
          />
        );
      })}
    </div>
  );
}

function ChatDockRoomButton({
  room,
  active,
  unreadCount,
  dataKey,
  onOpenRoom
}: {
  room: ChatRoomSummary;
  active: boolean;
  unreadCount: number;
  dataKey?: string;
  onOpenRoom: (room: ChatRoomSummary) => void;
}) {
  return (
    <button
      type="button"
      data-chat-dock-key={dataKey}
      className={active ? "chat-dock-item active" : "chat-dock-item"}
      onClick={() => onOpenRoom(room)}
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
      {unreadCount ? <em className="chat-dock-unread">{unreadCount}</em> : null}
    </button>
  );
}
