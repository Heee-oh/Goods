import { CHAT_UNREAD_STORAGE_KEY } from "./notificationConstants";
import type { RawChatRoomSummary } from "./notificationTypes";

export function loadUnreadCounts() {
  if (typeof window === "undefined") {
    return {};
  }

  try {
    const raw = window.localStorage.getItem(CHAT_UNREAD_STORAGE_KEY);
    if (!raw) {
      return {};
    }

    const parsed = JSON.parse(raw) as Record<string, number>;
    return Object.fromEntries(
      Object.entries(parsed).filter(
        ([roomId, count]) => typeof roomId === "string" && typeof count === "number" && count > 0
      )
    );
  } catch {
    return {};
  }
}

export function normalizeRoom(summary: RawChatRoomSummary) {
  const chatRoomId = summary.chat_room_id ?? summary.chatRoomId;
  if (chatRoomId == null) {
    return null;
  }

  return {
    chatRoomId: String(chatRoomId),
    partnerNickname: summary.partner_nickname ?? summary.partnerNickname ?? null
  };
}

export function getActiveChatRoomId(pathname: string) {
  const match = pathname.match(/^\/chatting\/([^/]+)$/);
  return match ? decodeURIComponent(match[1]) : null;
}
