import type { TransactionType } from "@/lib/transactionType";

export type RegionResponse = {
  region_id: number;
  dongnm: string;
  verified_at?: string | null;
  is_primary?: boolean;
  primary?: boolean;
};

export type RegionSearchItem = {
  region_id: number;
  full_name: string;
  dongnm: string;
};

export type MemberResponse = {
  nickname?: string;
  profile_image?: string | null;
  ProfileImage?: string | null;
  smile_score?: number;
  smileScore?: number;
};

export type ListingItem = {
  listing_id: number;
  interest_id?: number;
  title: string;
  price_amount: number;
  transaction_type: TransactionType;
  status: "DRAFT" | "PUBLISHED" | "RESERVED" | "SOLD_OUT";
  dongnm: string;
  chat_cnt: number;
  first_image: string | null;
  updated_at: string;
  distance_km?: number | null;
  distanceKm?: number | null;
};

export type WishlistListingItem = ListingItem & {
  interest_id: number;
};

export type ListingSliceResponse = {
  content: ListingItem[];
};

export type PublishedListingState = {
  refreshAt?: number;
  publishedListingId?: number;
};

export type RailIndicatorState = {
  top: number;
  height: number;
  visible: boolean;
  animate: boolean;
};

export type InterestResponse = {
  id: number;
  listingId?: number;
  listing_id?: number;
};

export type InterestSliceResponse = {
  content: InterestResponse[];
  last?: boolean;
};

export type ListingDetailPreview = {
  listing_id: number;
  seller_id?: number | string | null;
  title: string;
  price_amount: number | null;
  transaction_type: TransactionType;
  status: "DRAFT" | "PUBLISHED" | "RESERVED" | "SOLD_OUT";
  region_name: string | null;
  chat_count: number;
  images: Array<{
    image_url: string;
    sort_order: number;
  }>;
  distance_km?: number | null;
  distanceKm?: number | null;
  updated_at: string;
};

export type FeedPath =
  | "/listing"
  | "/trading"
  | "/my-listings"
  | "/wishlist"
  | "/sales-history"
  | "/purchase-history"
  | "/received-reviews"
  | "/profile";
