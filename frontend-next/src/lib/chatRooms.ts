import { type ListingStatus } from "./listingStatus";
import { type TransactionType } from "./transactionType";

export type RawChatRoomSummary = {
  chat_room_id?: number | string;
  chatRoomId?: number | string;
  listing_id?: number | string | null;
  listingId?: number | string | null;
  listing_title?: string | null;
  listingTitle?: string | null;
  listing_price?: number | string | null;
  listingPrice?: number | string | null;
  listing_status?: string | null;
  listingStatus?: string | null;
  listing_transaction_type?: string | null;
  listingTransactionType?: string | null;
  listing_first_image?: string | null;
  listingFirstImage?: string | null;
  seller_view?: boolean;
  sellerView?: boolean;
  partner_nickname?: string;
  partnerNickname?: string;
  partner_profile_image?: string | null;
  partnerProfileImage?: string | null;
  region_name?: string | null;
  regionName?: string | null;
  last_message?: string;
  lastMessage?: string;
  last_message_at?: string;
  lastMessageAt?: string;
};

export type ChatRoomSummary = {
  chatRoomId: string;
  listingId: string | null;
  listingTitle: string | null;
  listingPrice: number | null;
  listingStatus: ListingStatus;
  listingTransactionType: TransactionType;
  listingFirstImage: string | null;
  sellerView: boolean;
  partnerNickname: string;
  partnerProfileImage: string | null;
  regionName: string | null;
  lastMessage: string;
  lastMessageAt: string;
};

export type ChatRoomListEntry =
  | {
      kind: "room";
      room: ChatRoomSummary;
    }
  | {
      kind: "group";
      listingId: string;
      listingTitle: string | null;
      listingPrice: number | null;
      listingStatus: ListingStatus;
      listingTransactionType: TransactionType;
      listingFirstImage: string | null;
      rooms: ChatRoomSummary[];
      lastMessageAt: string;
    };

function resolveListingTransactionType(value: string | null | undefined, price: number | null | undefined) {
  const normalized = typeof value === "string" ? value.trim().toLowerCase() : "";
  if (normalized === "sell" || normalized === "trade" || normalized === "free") {
    return normalized as TransactionType;
  }

  return Number(price ?? 0) === 0 ? "free" : "sell";
}

export function normalizeChatRoomSummary(room: RawChatRoomSummary): ChatRoomSummary | null {
  const chatRoomId = room.chat_room_id ?? room.chatRoomId;
  if (chatRoomId == null) {
    return null;
  }

  const listingPriceValue = room.listing_price ?? room.listingPrice;
  const parsedListingPrice =
    listingPriceValue == null || listingPriceValue === ""
      ? null
      : Number(listingPriceValue);
  const listingPrice = parsedListingPrice != null && !Number.isNaN(parsedListingPrice) ? parsedListingPrice : null;
  const listingIdValue = room.listing_id ?? room.listingId;

  return {
    chatRoomId: String(chatRoomId),
    listingId: listingIdValue == null ? null : String(listingIdValue),
    listingTitle: room.listing_title ?? room.listingTitle ?? null,
    listingPrice,
    listingStatus: (room.listing_status ?? room.listingStatus ?? "PUBLISHED") as ListingStatus,
    listingTransactionType: resolveListingTransactionType(
      room.listing_transaction_type ?? room.listingTransactionType,
      listingPrice
    ),
    listingFirstImage: room.listing_first_image ?? room.listingFirstImage ?? null,
    sellerView: Boolean(room.seller_view ?? room.sellerView),
    partnerNickname: room.partner_nickname ?? room.partnerNickname ?? "\uC0C1\uB300 \uC0AC\uC6A9\uC790",
    partnerProfileImage: room.partner_profile_image ?? room.partnerProfileImage ?? null,
    regionName: room.region_name ?? room.regionName ?? null,
    lastMessage: room.last_message ?? room.lastMessage ?? "",
    lastMessageAt: room.last_message_at ?? room.lastMessageAt ?? new Date().toISOString()
  };
}

export function groupChatRooms(rooms: ChatRoomSummary[]): ChatRoomListEntry[] {
  const sortedRooms = [...rooms].sort((left, right) => {
    const leftTime = new Date(left.lastMessageAt).getTime();
    const rightTime = new Date(right.lastMessageAt).getTime();
    return rightTime - leftTime;
  });

  const grouped = new Map<string, ChatRoomListEntry & { kind: "group" }>();
  const flatEntries: ChatRoomListEntry[] = [];

  for (const room of sortedRooms) {
    if (room.sellerView && room.listingId) {
      const existing = grouped.get(room.listingId);
      if (existing) {
        existing.rooms.push(room);
        if (new Date(room.lastMessageAt).getTime() > new Date(existing.lastMessageAt).getTime()) {
          existing.lastMessageAt = room.lastMessageAt;
          existing.listingTitle = room.listingTitle ?? existing.listingTitle;
          existing.listingPrice = room.listingPrice ?? existing.listingPrice;
          existing.listingStatus = room.listingStatus;
          existing.listingTransactionType = room.listingTransactionType;
          existing.listingFirstImage = room.listingFirstImage ?? existing.listingFirstImage;
        }
        continue;
      }

      grouped.set(room.listingId, {
        kind: "group",
        listingId: room.listingId,
        listingTitle: room.listingTitle,
        listingPrice: room.listingPrice,
        listingStatus: room.listingStatus,
        listingTransactionType: room.listingTransactionType,
        listingFirstImage: room.listingFirstImage,
        rooms: [room],
        lastMessageAt: room.lastMessageAt
      });
      continue;
    }

    flatEntries.push({ kind: "room", room });
  }

  const groupedEntries = Array.from(grouped.values()).sort((left, right) => {
    const leftTime = new Date(left.lastMessageAt).getTime();
    const rightTime = new Date(right.lastMessageAt).getTime();
    return rightTime - leftTime;
  });

  return [...groupedEntries, ...flatEntries].sort((left, right) => {
    const leftTime = left.kind === "group" ? left.lastMessageAt : left.room.lastMessageAt;
    const rightTime = right.kind === "group" ? right.lastMessageAt : right.room.lastMessageAt;
    return new Date(rightTime).getTime() - new Date(leftTime).getTime();
  });
}

export function getListingPriceLabel(
  listingPrice: number | null,
  transactionType: TransactionType
) {
  if (transactionType === "free") {
    return "\uB098\uB214";
  }

  if (transactionType === "trade") {
    return "\uAD50\uD658";
  }

  return listingPrice == null ? "\uD310\uB9E4" : `${listingPrice.toLocaleString("ko-KR")}\uC6D0`;
}
