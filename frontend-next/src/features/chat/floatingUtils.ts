import { getTransactionLabel, type TransactionType } from "@/lib/transactionType";
import type { ChatMessageItem, SocketMessage } from "./floatingTypes";

export function normalizeSocketMessage(message: SocketMessage): ChatMessageItem {
  return {
    message_id: String(message.message_id ?? message.messageId ?? Date.now()),
    sender_id: String(message.sender_id ?? message.senderId ?? ""),
    type: message.type,
    content: message.content,
    created_at: message.created_at ?? message.createdAt ?? new Date().toISOString()
  };
}

export function formatMessageTime(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "numeric",
    minute: "2-digit"
  });
}

export function formatPrice(price: number | null) {
  if (price == null) {
    return "Price unknown";
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

export function formatListingLabel(price: number | null, transactionType: TransactionType) {
  if (transactionType !== "sell" || price == null || price === 0) {
    return getTransactionLabel(transactionType);
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

export function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}
