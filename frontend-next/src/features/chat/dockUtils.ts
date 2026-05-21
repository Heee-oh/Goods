import type { ChatRoomSummary } from "@/lib/chatRooms";
import { DEFAULT_FLOAT_SIZE } from "./dockConstants";

export function registerChatDockRooms(rooms: ChatRoomSummary[]) {
  return rooms.map((room) => ({
    chatRoomId: room.chatRoomId,
    partnerNickname: room.partnerNickname
  }));
}

export function formatRelativeTime(value: string) {
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

export function createInitialPosition(index: number) {
  if (typeof window === "undefined") {
    return { x: 40, y: 120 };
  }

  const x = Math.max(16, window.innerWidth - DEFAULT_FLOAT_SIZE.width - 24 - index * 24);
  const y = Math.max(16, 100 + index * 24);
  return { x, y };
}

export function createInitialSize() {
  return { ...DEFAULT_FLOAT_SIZE };
}
