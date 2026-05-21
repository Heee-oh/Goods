export const REGIONS_CACHE_PREFIX = "goods:member-regions";
export const SELECTED_REGION_COOKIE = "goods-selected-region";

export const desktopMarketplace = ["Home Feed", "Trading Hub", "My Collection"];

export const desktopMyPageLinks = [
  { label: "Wishlist", path: "/wishlist" },
  { label: "판매 기록", path: "/sales-history" },
  { label: "구매 기록", path: "/purchase-history" },
  { label: "받은 리뷰", path: "/received-reviews" },
  { label: "Profile", path: "/profile" }
] as const;

export const desktopCategories = ["Anime Figures", "Idol Photocards", "Limited Merch", "Plushies"];
export const desktopTrades = ["Waiting on @kuro", "Meetup Today"];

export const feedFilters = [
  { id: "all", label: "전체" },
  { id: "selling", label: "판매중" },
  { id: "reserved", label: "예약중" },
  { id: "free", label: "나눔" },
  { id: "chats", label: "채팅 있음" }
] as const;

export const tradeFilters = [
  { id: "all", label: "전체" },
  { id: "trade", label: "교환중" },
  { id: "reserved", label: "예약중" },
  { id: "chats", label: "채팅 있음" }
] as const;
