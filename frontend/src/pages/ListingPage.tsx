import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import type { ChangeEvent } from "react";
import { ApiError, apiRequest } from "../lib/api";
import { convertImageToWebpFile } from "../lib/image";
import { clearSession, getMemberId, getSelectedRegionId, saveSelectedRegionId } from "../lib/auth";
import { readCachedJson, writeCachedJson } from "../lib/cache";
import {
  getListingStatusLabel,
  getListingStatusTone,
  shouldShowStatusBadge
} from "../lib/listingStatus";
import { getTransactionLabel, type TransactionType } from "../lib/transactionType";
import { ChatDock } from "../components/ChatDock";
import { Spinner } from "@/components/ui/spinner";

type RegionResponse = {
  region_id: number;
  dongnm: string;
  verified_at?: string | null;
  is_primary?: boolean;
  primary?: boolean;
};

type RegionSearchItem = {
  region_id: number;
  full_name: string;
  dongnm: string;
};

type MemberResponse = {
  nickname?: string;
  profile_image?: string | null;
  ProfileImage?: string | null;
  smile_score?: number;
  smileScore?: number;
};

type ListingItem = {
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

type WishlistListingItem = ListingItem & {
  interest_id: number;
};

type ListingSliceResponse = {
  content: ListingItem[];
};

type PublishedListingState = {
  refreshAt?: number;
  publishedListingId?: number;
};

const REGIONS_CACHE_PREFIX = "goods:member-regions";

type InterestResponse = {
  id: number;
  listingId?: number;
  listing_id?: number;
};

type InterestSliceResponse = {
  content: InterestResponse[];
  last?: boolean;
};

type ListingDetailPreview = {
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

const desktopMarketplace = ["Home Feed", "Trading Hub", "My Collection"];
const desktopMyPageLinks = [
  { label: "Wishlist", path: "/wishlist" },
  { label: "Profile", path: "/profile" }
] as const;
const desktopCategories = ["Anime Figures", "Idol Photocards", "Limited Merch", "Plushies"];
const desktopTrades = ["Waiting on @kuro", "Meetup Today"];
const desktopFilters = ["All", "WTT (Trade)", "WTS (Sell)"];
const feedFilters = [
  { id: "all", label: "전체" },
  { id: "selling", label: "판매중" },
  { id: "reserved", label: "예약중" },
  { id: "free", label: "나눔" },
  { id: "chats", label: "채팅 있음" }
] as const;
const tradeFilters = [
  { id: "all", label: "전체" },
  { id: "trade", label: "교환중" },
  { id: "reserved", label: "예약중" },
  { id: "chats", label: "채팅 있음" }
] as const;
const TRADE_RAIL_OPEN_KEY = "goods:trade-rail-open";
const tradeMessages = [
  {
    mine: false,
    text: "Hi! I saw you have the Radio EVA Asuka. Would you trade for my Miku?"
  },
  {
    mine: true,
    text: "Hey! Yes, I'v筠 been looking for that one. Is the box included?"
  },
  {
    mine: false,
    text: "Yep, completely unopened. Box is 9/10. Are you local to downtown?"
  }
];

function formatPrice(amount: number, transactionType: TransactionType) {
  if (transactionType === "free") {
    return "\uB098\uB214";
  }

  if (transactionType === "trade") {
    return "\uAD50\uD658";
  }

  return `${amount.toLocaleString("ko-KR")}\uC6D0`;
}

function formatUpdatedAt(value: string) {
  const updatedAt = new Date(value);
  const diffMinutes = Math.max(1, Math.floor((Date.now() - updatedAt.getTime()) / 60000));

  if (diffMinutes < 60) {
    return `${diffMinutes}\uBD84 \uC804`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}\uC2DC\uAC04 \uC804`;
  }

  return `${Math.floor(diffHours / 24)}\uC77C \uC804`;
}

function formatDistance(value: number | null | undefined) {
  if (value == null || Number.isNaN(Number(value))) {
    return null;
  }

  const distance = Number(value);
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))}m` : `${distance.toFixed(1)}km`;
}

function Thumbnail({ imageUrl, tone }: { imageUrl: string | null; tone: string }) {
  if (imageUrl) {
    return <img className={`listing-thumb real-image ${tone}`} src={imageUrl} alt="" />;
  }

  return <div className={`listing-thumb ${tone}`} />;
}

function formatSmileScore(value: number | null | undefined) {
  return `${((Number(value ?? 100) / 10)).toFixed(1)}점`;
}

function normalizeRegion(region: RegionResponse): RegionResponse {
  return {
    ...region,
    primary: Boolean(region.is_primary ?? region.primary),
    verified_at: region.verified_at ?? null
  };
}

function pickInitialRegion(regions: RegionResponse[], savedRegionId: number | null) {
  return (
    regions.find((region) => region.region_id === savedRegionId && region.verified_at) ??
    regions.find((region) => region.primary && region.verified_at) ??
    regions.find((region) => region.verified_at) ??
    regions.find((region) => region.region_id === savedRegionId) ??
    regions[0] ??
    null
  );
}

export function ListingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const isTradingHub = location.pathname.startsWith("/trading");
  const isMyCollection = location.pathname.startsWith("/my-listings");
  const isWishlist = location.pathname.startsWith("/wishlist");
  const isProfile = location.pathname.startsWith("/profile");
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const profileFileInputRef = useRef<HTMLInputElement | null>(null);
  const [regionSheetOpen, setRegionSheetOpen] = useState(false);
  const [regionSearchOpen, setRegionSearchOpen] = useState(false);
  const [tradeRailOpen, setTradeRailOpen] = useState(() => {
    if (typeof window === "undefined") {
      return false;
    }

    return window.localStorage.getItem(TRADE_RAIL_OPEN_KEY) === "true";
  });
  const [deleteTarget, setDeleteTarget] = useState<RegionResponse | null>(null);
  const [regions, setRegions] = useState<RegionResponse[]>([]);
  const [me, setMe] = useState<MemberResponse | null>(null);
  const [profileNicknameDraft, setProfileNicknameDraft] = useState("");
  const [profileImageDraft, setProfileImageDraft] = useState<string | null>(null);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileUploading, setProfileUploading] = useState(false);
  const [selectedRegionId, setSelectedRegionId] = useState<number | null>(null);
  const [regionSearchQuery, setRegionSearchQuery] = useState("");
  const [regionSearchResults, setRegionSearchResults] = useState<RegionSearchItem[]>([]);
  const [regionSearchMessage, setRegionSearchMessage] = useState("");
  const [listings, setListings] = useState<ListingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [regionSearchLoading, setRegionSearchLoading] = useState(false);
  const [addingRegionId, setAddingRegionId] = useState<number | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState("");
  const [activeFeedFilter, setActiveFeedFilter] = useState<string>("all");
  const profileSmileValue = Number(me?.smile_score ?? me?.smileScore ?? 100);
  const profileSmileProgress = Math.max(0, Math.min(100, profileSmileValue / 10));

  const loadRegions = async (options?: { force?: boolean }) => {
    const memberId = getMemberId();
    const cacheKey = memberId ? `${REGIONS_CACHE_PREFIX}:${memberId}` : REGIONS_CACHE_PREFIX;

    if (!options?.force) {
      const cachedRegions = readCachedJson<RegionResponse[]>(cacheKey);
      if (cachedRegions && cachedRegions.length > 0) {
        const normalizedCached = cachedRegions.map(normalizeRegion);
        setRegions(normalizedCached);
        return normalizedCached;
      }
    }

    const myRegions = await apiRequest<RegionResponse[]>("/api/members/me/regions");
    const normalized = myRegions.map(normalizeRegion);
    setRegions(normalized);
    writeCachedJson(cacheKey, normalized);
    return normalized;
  };

  const loadMe = async () => {
    const response = await apiRequest<MemberResponse>("/api/members/me");
    setMe(response);
    setProfileNicknameDraft(response.nickname?.trim() || "");
    setProfileImageDraft(response.profile_image ?? response.ProfileImage ?? null);
    return response;
  };

  const fetchListings = async (regionId: number, lastListingId?: number, append = false) => {
    if (isProfile) {
      setListings([]);
      setHasMore(false);
      return;
    }

    const query = new URLSearchParams({ region_id: String(regionId) });
    if (lastListingId) {
      query.set("last_listing_id", String(lastListingId));
    }
    if (isTradingHub) {
      query.set("transaction_type", "trade");
    }
    if (isMyCollection) {
      const memberId = getMemberId();
      if (memberId) {
        query.set("seller_id", memberId);
      }
    }

    const response = await apiRequest<ListingSliceResponse>(`/api/listings?${query.toString()}`);
    const nextItems = response.content ?? [];
    setListings((current) => (append ? [...current, ...nextItems] : nextItems));
    setHasMore(nextItems.length >= 20);
  };

  const fetchWishlist = async (_regionId: number, lastInterestId?: number, append = false) => {
    const query = new URLSearchParams({ size: "20" });
    if (lastInterestId) {
      query.set("lastInterestId", String(lastInterestId));
    }

    const response = await apiRequest<InterestSliceResponse>(`/api/members/me/interests?${query.toString()}`);
    const interests = response.content ?? [];
    const nextItems: WishlistListingItem[] = [];
    for (const interest of interests) {
      try {
        const listingId = interest.listingId ?? interest.listing_id;
        if (!listingId) {
          continue;
        }

        const detail = await apiRequest<ListingDetailPreview>(`/api/listings/${listingId}`);
        nextItems.push({
          listing_id: detail.listing_id,
          interest_id: interest.id,
          title: detail.title,
          price_amount: detail.price_amount ?? 0,
          transaction_type: detail.transaction_type,
          status: detail.status,
          dongnm: detail.region_name ?? "",
          chat_cnt: detail.chat_count,
          first_image: detail.images?.slice().sort((a, b) => a.sort_order - b.sort_order)[0]?.image_url ?? null,
          updated_at: detail.updated_at,
          distance_km: detail.distance_km ?? detail.distanceKm ?? null
        });
      } catch {
        // Skip entries whose listing was removed or is no longer readable.
      }
    }
    setListings((current) => (append ? [...current, ...nextItems] : nextItems));
    setHasMore(response.last === false && nextItems.length > 0);
  };

  const loadCurrentFeed = async (regionId?: number, cursorId?: number, append = false) => {
    if (isProfile) {
      setListings([]);
      setHasMore(false);
      return;
    }

    if (isWishlist) {
      await fetchWishlist(regionId ?? 0, cursorId, append);
      return;
    }

    if (regionId == null) {
      setListings([]);
      setHasMore(false);
      return;
    }

    await fetchListings(regionId, cursorId, append);
  };

  const verifyRegion = async (regionId: number) => {
    if (!navigator.geolocation) {
      throw new Error("현재 위치를 사용할 수 없습니다.");
    }

    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, () => reject(new Error("위치 권한이 필요합니다.")));
    });

    await apiRequest(`/api/members/me/regions/${regionId}/verify`, {
      method: "POST",
      body: JSON.stringify({
        lat: position.coords.latitude,
        lng: position.coords.longitude
      })
    });
  };

  const prependPublishedListing = async (listingId: number) => {
    if (isWishlist) {
      return;
    }

    const regionId = getSelectedRegionId();
    const detail = await apiRequest<ListingDetailPreview>(
      `/api/listings/${listingId}${regionId ? `?region_id=${regionId}` : ""}`
    );
    if (isTradingHub && detail.transaction_type !== "trade") {
      return;
    }
    if (isMyCollection) {
      const memberId = getMemberId();
      if (memberId && String(detail.seller_id ?? "") !== memberId) {
        return;
      }
    }
    const nextItem: ListingItem = {
      listing_id: detail.listing_id,
      title: detail.title,
      price_amount: detail.price_amount ?? 0,
      transaction_type: detail.transaction_type,
      status: detail.status,
      dongnm: detail.region_name ?? "",
      chat_cnt: detail.chat_count,
      first_image: detail.images?.slice().sort((a, b) => a.sort_order - b.sort_order)[0]?.image_url ?? null,
      updated_at: detail.updated_at,
      distance_km: detail.distance_km ?? detail.distanceKm ?? null
    };

    setListings((current) => {
      const withoutCurrent = current.filter((item) => item.listing_id !== nextItem.listing_id);
      return [nextItem, ...withoutCurrent];
    });
  };

  const filteredListings = useMemo(() => {
    if (isProfile) {
      return [];
    }

    if (isWishlist) {
      return listings;
    }

    return listings.filter((item) => {
      if (isTradingHub) {
        if (item.transaction_type !== "trade") {
          return false;
        }
      } else if (!isMyCollection && item.transaction_type === "trade") {
        return false;
      }

      switch (activeFeedFilter) {
        case "selling":
          return item.status === "PUBLISHED" && item.transaction_type === "sell";
        case "reserved":
          return item.status === "RESERVED";
        case "free":
          return item.transaction_type === "free";
        case "chats":
          return item.chat_cnt > 0;
        case "trade":
          return item.transaction_type === "trade";
        case "all":
        default:
          return true;
      }
    });
  }, [activeFeedFilter, isMyCollection, isTradingHub, isWishlist, isProfile, listings]);

  const isInitialListingLoading = loading && filteredListings.length === 0;
  const isProfileLoading = isProfile && loading && me == null;
  const hasFilteredData = filteredListings.length > 0;
  const showFilteredEmptyState = !loading && filteredListings.length === 0;

  const renderListingCards = (items: ListingItem[]) => (
    <section className="listing-list">
      {items.map((item, index) => {
        const distanceLabel = formatDistance(item.distance_km ?? item.distanceKm);

        return (
        <article
          key={item.listing_id}
          className="listing-card"
          onClick={() => openListingDetail(item.listing_id)}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              openListingDetail(item.listing_id);
            }
          }}
        >
          <Thumbnail
            imageUrl={item.first_image}
            tone={["sunset", "olive", "midnight", "sand"][index % 4]}
          />
          <div className="listing-copy">
            <div className="listing-row">
              <h3>{item.title}</h3>
              <button type="button" className="more-button" aria-label="more">
                {"\u22EE"}
              </button>
            </div>
            <p className="listing-sub">
              {item.dongnm}
              {distanceLabel ? ` \u00B7 ${distanceLabel}` : ""}
              {" \u00B7 "}
              {formatUpdatedAt(item.updated_at)}
            </p>
            <div className="listing-price-row">
              {shouldShowStatusBadge(item.status) ? (
                <span className={`listing-status-badge ${getListingStatusTone(item.status)}`}>
                  {getListingStatusLabel(item.status, item.transaction_type)}
                </span>
              ) : null}
              <strong>{formatPrice(item.price_amount, item.transaction_type)}</strong>
            </div>
            <div className="listing-meta">
              {item.chat_cnt > 0 ? <span>{`\uCC44\uD305 ${item.chat_cnt}`}</span> : null}
            </div>
          </div>
        </article>
        );
      })}
    </section>
  );

  const renderFeedFilters = () => (
    isWishlist || isProfile ? null : (
    <div className="listing-feed-filters" aria-label="세부 필터">
      {(isTradingHub ? tradeFilters : feedFilters).map((filter) => (
        <button
          key={filter.id}
          type="button"
          className={activeFeedFilter === filter.id ? "listing-feed-filter active" : "listing-feed-filter"}
          onClick={() => setActiveFeedFilter(filter.id)}
        >
          {filter.label}
        </button>
      ))}
    </div>
    )
  );

  useEffect(() => {
    const load = async () => {
      const shouldShowLoading = !isProfile || me == null;
      try {
        if (shouldShowLoading) {
          setLoading(true);
        }
        setError("");

        const normalized = regions.length > 0 ? regions : await loadRegions();
        if (isWishlist) {
          const savedRegionId = getSelectedRegionId();
          const initialRegion = pickInitialRegion(normalized, savedRegionId);
          if (initialRegion) {
            setSelectedRegionId(initialRegion.region_id);
            saveSelectedRegionId(initialRegion.region_id);
          }
          await loadCurrentFeed(initialRegion?.region_id);
          return;
        }

        if (normalized.length === 0) {
          setListings([]);
          return;
        }

        const savedRegionId = getSelectedRegionId();
        const initialRegion = pickInitialRegion(normalized, savedRegionId);

        if (initialRegion) {
          setSelectedRegionId(initialRegion.region_id);
          saveSelectedRegionId(initialRegion.region_id);
          if (isProfile) {
            setListings([]);
            setHasMore(false);
            if (me == null) {
              await loadMe().catch((profileErr) => {
                if (profileErr instanceof ApiError && profileErr.status === 401) {
                  clearSession();
                  navigate("/welcome", { replace: true });
                  return;
                }

                setMe(null);
              });
            }
          } else {
            await loadCurrentFeed(initialRegion.region_id);
          }
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          clearSession();
          navigate("/welcome", { replace: true });
          return;
        }

        setError(err instanceof Error ? err.message : "Failed to load listings.");
      } finally {
        if (shouldShowLoading) {
          setLoading(false);
        }
      }
    };

    void load();
  }, [navigate, isTradingHub, isWishlist, isProfile]);

  useEffect(() => {
    const { refreshAt, publishedListingId } = (location.state as PublishedListingState | null) ?? {};
    if (!refreshAt || loading || (!selectedRegionId && !isWishlist) || isProfile) {
      return;
    }

    void (async () => {
      try {
        await loadCurrentFeed(selectedRegionId ?? undefined);
        if (publishedListingId) {
          await prependPublishedListing(publishedListingId);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load listings.");
      }
    })();
  }, [location.state, loading, selectedRegionId, isTradingHub, isWishlist, isProfile]);

  useEffect(() => {
    setActiveFeedFilter("all");
  }, [isTradingHub, isWishlist, isProfile]);

  useEffect(() => {
    if (!regionSearchOpen) {
      return;
    }

    const timeout = window.setTimeout(async () => {
      try {
        setRegionSearchLoading(true);

        const path = regionSearchQuery.trim().length > 0
          ? `/api/regions/search?query=${encodeURIComponent(regionSearchQuery)}`
          : "/api/regions/search?query=";

        const data = await apiRequest<RegionSearchItem[]>(path, { auth: false });
        setRegionSearchResults(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load regions.");
      } finally {
        setRegionSearchLoading(false);
      }
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [regionSearchOpen, regionSearchQuery]);

  useEffect(() => {
    if ((!selectedRegionId && !isWishlist) || !loadMoreRef.current || loading || loadingMore || !hasMore || isProfile) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (!entry?.isIntersecting || loading || loadingMore || !hasMore) {
          return;
        }

        const lastListingId = isWishlist
          ? listings[listings.length - 1]?.interest_id
          : listings[listings.length - 1]?.listing_id;
        if (!lastListingId) {
          return;
        }

        setLoadingMore(true);
        void loadCurrentFeed(selectedRegionId ?? undefined, lastListingId, true)
          .catch((err) => {
            setError(err instanceof Error ? err.message : "Failed to load more listings.");
          })
          .finally(() => setLoadingMore(false));
      },
      { rootMargin: "200px 0px" }
    );

    observer.observe(loadMoreRef.current);

    return () => observer.disconnect();
  }, [selectedRegionId, listings, loading, loadingMore, hasMore, isWishlist, isProfile]);

  const selectedRegion = useMemo(
    () => regions.find((region) => region.region_id === selectedRegionId) ?? null,
    [regions, selectedRegionId]
  );

  const canAddRegion = regions.filter((region) => region.primary).length < 2;
  const highlightedListing = listings[0] ?? null;
  const highlightedListingDeal = highlightedListing
    ? highlightedListing.transaction_type === "sell" && highlightedListing.price_amount > 0
      ? formatPrice(highlightedListing.price_amount, highlightedListing.transaction_type)
      : getTransactionLabel(highlightedListing.transaction_type)
    : "";
  const feedTitle = isProfile ? "Profile" : isWishlist ? "Wishlist" : isTradingHub ? "Trading Hub" : "Recommended Matches";
  const feedSummary = isWishlist
    ? `${filteredListings.length}개의 저장한 굿즈`
    : isProfile
      ? `${me?.nickname?.trim() || "내 프로필"} · 스마일지수 ${formatSmileScore(me?.smile_score ?? me?.smileScore ?? 100)}`
      : activeFeedFilter === "all"
        ? "전체 게시글을 보고 있어요."
        : `${filteredListings.length}개의 결과`;
  const isBootLoading = isProfileLoading || isInitialListingLoading;

  const handleSelectRegion = async (regionId: number) => {
    try {
      const targetRegion = regions.find((region) => region.region_id === regionId) ?? null;

      if (targetRegion && !targetRegion.verified_at) {
        await verifyRegion(regionId);
        const reloaded = await loadRegions({ force: true });
        const verifiedRegion = reloaded.find((region) => region.region_id === regionId) ?? targetRegion;
        setRegions(reloaded);
        setSelectedRegionId(verifiedRegion.region_id);
        saveSelectedRegionId(verifiedRegion.region_id);
      } else {
        setSelectedRegionId(regionId);
        saveSelectedRegionId(regionId);
      }

      setRegionSheetOpen(false);
      setLoading(true);
      setHasMore(true);
      await loadCurrentFeed(regionId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load listings.");
    } finally {
      setLoading(false);
    }
  };

  const renderRegionPopover = () => {
    if (!regionSheetOpen) {
      return null;
    }

    return (
      <div className="region-popover">
        <div className="region-popover-pointer" />
        <h2>{"지역 인증"}</h2>
        <p>{"현재 위치로 지역을 추가하고 인증할 수 있어요."}</p>

        <div className="region-sheet-list">
          {regions.map((region) => (
            <div key={region.region_id} className="region-sheet-item">
              <button
                type="button"
                className="region-sheet-select"
                onClick={() => void handleSelectRegion(region.region_id)}
              >
                <span className={region.region_id === selectedRegionId ? "region-radio active" : "region-radio"} />
                <span>{region.dongnm}</span>
                <span className={region.verified_at ? "region-sheet-state" : "region-sheet-state pending"}>
                  {region.verified_at ? "인증됨" : "인증 필요"}
                </span>
              </button>
              <button
                type="button"
                className={regions.length <= 1 ? "region-remove disabled" : "region-remove"}
                onClick={() => setDeleteTarget(region)}
                disabled={regions.length <= 1}
                aria-label={`${region.dongnm} 삭제`}
              >
                {"\u00D7"}
              </button>
            </div>
          ))}
        </div>

        {canAddRegion ? (
            <button
              type="button"
              className="region-add-button"
              onClick={() => {
                setRegionSearchMessage("");
                setRegionSearchOpen(true);
                setRegionSheetOpen(false);
              }}
          >
            {"지역 추가하고 인증"}
          </button>
        ) : (
          <p className="region-limit-note">{"이미 2개라면 더 이상 추가할 수 없어요."}</p>
        )}
      </div>
    );
  };

  const handleDeleteRegion = async () => {
    if (!deleteTarget) {
      return;
    }

    if (regions.length <= 1) {
      setError("지역은 최소 1개는 유지해야 해요.");
      setDeleteTarget(null);
      setRegionSheetOpen(true);
      return;
    }

    try {
      await apiRequest(`/api/members/me/regions/${deleteTarget.region_id}`, {
        method: "DELETE"
      });

      const reloaded = await loadRegions({ force: true });
      const preservedSelected = reloaded.find((region) => region.region_id === selectedRegionId) ?? null;
      const nextSelectedRegion =
        deleteTarget.region_id === selectedRegionId
          ? pickInitialRegion(reloaded, null)
          : preservedSelected ?? pickInitialRegion(reloaded, null);

      setRegions(reloaded);
      setSelectedRegionId(nextSelectedRegion?.region_id ?? null);
      if (nextSelectedRegion) {
        saveSelectedRegionId(nextSelectedRegion.region_id);
        setLoading(true);
        setHasMore(true);
      void loadCurrentFeed(nextSelectedRegion.region_id)
          .catch((err) => {
            setError(err instanceof Error ? err.message : "Failed to load listings.");
          })
          .finally(() => setLoading(false));
      } else {
        setListings([]);
      }

      setRegionSheetOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to remove region.");
    } finally {
      setDeleteTarget(null);
    }
  };

  const handleAddRegion = async (region: RegionSearchItem) => {
    if (!canAddRegion) {
      return;
    }

    let regionInserted = false;

    try {
      setAddingRegionId(region.region_id);
      setError("");

      await apiRequest("/api/members/me/regions", {
        method: "POST",
        body: JSON.stringify({ region_id: region.region_id })
      });
      regionInserted = true;

      try {
        await verifyRegion(region.region_id);
      } catch {
        if (regionInserted) {
          try {
            await apiRequest(`/api/members/me/regions/${region.region_id}`, {
              method: "DELETE"
            });
          } catch {
            // Ignore rollback errors and fall through with the verification failure message.
          }
        }

        const reloadedAfterRollback = await loadRegions({ force: true });
        setRegions(reloadedAfterRollback);
        setRegionSearchMessage("현재 지역이 아닙니다.");
        return;
      }

      const reloaded = await loadRegions({ force: true });
      const nextRegion = reloaded.find((item) => item.region_id === region.region_id) ?? {
        region_id: region.region_id,
        dongnm: region.dongnm,
        verified_at: null,
        primary: false,
        is_primary: false
      };

      setRegions(reloaded);
      setSelectedRegionId(nextRegion.region_id);
      saveSelectedRegionId(nextRegion.region_id);
      setRegionSearchMessage("");
      setRegionSearchOpen(false);
      setRegionSheetOpen(true);
      setRegionSearchQuery("");

      setLoading(true);
      setHasMore(true);

      void loadCurrentFeed(nextRegion.region_id)
        .catch((fetchError) => {
          setError(fetchError instanceof Error ? fetchError.message : "Failed to load listings.");
        })
        .finally(() => setLoading(false));
    } catch (err) {
      setRegionSearchMessage(err instanceof Error ? err.message : "Failed to add region.");
    } finally {
      setAddingRegionId(null);
    }
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError("Current location is not available.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          setRegionSearchLoading(true);
          const data = await apiRequest<RegionSearchItem[]>(
            `/api/regions/nearby?lat=${position.coords.latitude}&lng=${position.coords.longitude}`,
            { auth: false }
          );
          setRegionSearchResults(data);
        } catch (err) {
          setError(err instanceof Error ? err.message : "Failed to load nearby regions.");
        } finally {
          setRegionSearchLoading(false);
        }
      },
      () => setError("Location permission is required.")
    );
  };

  const handleProfileNicknameSave = async () => {
    const nextNickname = profileNicknameDraft.trim();
    if (!nextNickname) {
      setError("닉네임을 입력해주세요.");
      return;
    }

    try {
      setProfileSaving(true);
      setError("");
      await apiRequest("/api/members/me/nickname", {
        method: "PATCH",
        body: JSON.stringify({ nickname: nextNickname })
      });
      setMe((current) => (current ? { ...current, nickname: nextNickname } : current));
    } catch (err) {
      setError(err instanceof Error ? err.message : "닉네임 변경에 실패했습니다.");
    } finally {
      setProfileSaving(false);
    }
  };

  const handleProfileImageUpload = async (file: File) => {
    const convertedFile = await convertImageToWebpFile(file, "profile-image");
    const formData = new FormData();
    formData.append("file", convertedFile);

    const uploadResponse = await apiRequest<{ profileImage?: string; profile_image?: string }>(
      "/api/members/me/profile-image",
      {
        method: "PUT",
        body: formData
      }
    );

    const imageUrl = uploadResponse.profileImage ?? uploadResponse.profile_image ?? null;
    if (!imageUrl) {
      throw new Error("이미지 업로드에 실패했습니다.");
    }

    setProfileImageDraft(imageUrl);
    setMe((current) =>
      current
        ? {
            ...current,
            profile_image: imageUrl,
            ProfileImage: imageUrl
          }
        : current
    );
  };

  const handleProfileImageInput = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";

    if (!file) {
      return;
    }

    try {
      setProfileUploading(true);
      setError("");
      await handleProfileImageUpload(file);
    } catch (err) {
      setError(err instanceof Error ? err.message : "프로필 이미지를 변경하지 못했습니다.");
    } finally {
      setProfileUploading(false);
    }
  };

  const openProfileImagePicker = () => {
    profileFileInputRef.current?.click();
  };

  const handleWriteClick = () => {
    navigate("/sell", { state: { backgroundLocation: location } });
  };

  const openFeedPage = (path: "/listing" | "/trading" | "/my-listings" | "/wishlist" | "/profile") => {
    if (location.pathname !== path) {
      navigate(path);
    }
  };

  const openListingDetail = (listingId: number) => {
    const basePath = isTradingHub ? "/trading" : isMyCollection ? "/my-listings" : isWishlist ? "/wishlist" : "/listing";
    navigate(`${basePath}/${listingId}`, { state: { backgroundLocation: location } });
  };

  return (
    <div className="page page-listing">
      <div className="listing-desktop-shell">
        <aside className="listing-left-rail">
          <div className="listing-brand-row">
            <span className="listing-brand-mark">T</span>
            <strong>TORA KAZE</strong>
          </div>

          <div className="listing-rail-group">
            <span className="listing-rail-label">MARKETPLACE</span>
            {desktopMarketplace.map((item, index) => (
              <button
                key={item}
                type="button"
                className={
                  (index === 0 && !isTradingHub && !isMyCollection && !isWishlist && !isProfile) ||
                  (index === 1 && isTradingHub) ||
                  (index === 2 && isMyCollection)
                    ? "listing-rail-item active"
                    : "listing-rail-item"
                }
                onClick={() => openFeedPage(index === 1 ? "/trading" : index === 2 ? "/my-listings" : "/listing")}
              >
                {item}
              </button>
            ))}
          </div>

          <div className="listing-rail-group">
            <span className="listing-rail-label">MY PAGE</span>
            {desktopMyPageLinks.map((item) => (
              <button
                key={item.path}
                type="button"
                className={
                  (item.path === "/wishlist" && isWishlist) ||
                  (item.path === "/profile" && location.pathname.startsWith("/profile"))
                    ? "listing-rail-item active"
                    : "listing-rail-item"
                }
                onClick={() => openFeedPage(item.path)}
              >
                {item.label}
              </button>
            ))}
          </div>

          <div className="listing-rail-group">
            <span className="listing-rail-label">CATEGORIES</span>
            {desktopCategories.map((item) => (
              <button key={item} type="button" className="listing-rail-item">
                {item}
              </button>
            ))}
          </div>

          <div className="listing-rail-group">
            <span className="listing-rail-label">ACTIVE TRADES</span>
            {desktopTrades.map((item, index) => (
              <button key={item} type="button" className="listing-rail-item trade">
                <span>{item}</span>
                {index === 0 ? <em>1</em> : null}
              </button>
            ))}
          </div>
        </aside>

          <div className="listing-center-rail">
            <header className="listing-desktop-topbar">
              <div className="listing-region-menu">
                <button type="button" className="listing-region-chip" onClick={() => setRegionSheetOpen(true)}>
                  <span className="listing-region-chip-label">내 지역</span>
                  <strong>{selectedRegion?.dongnm ?? "지역 선택"}</strong>
                  <em>{selectedRegion?.verified_at ? "인증됨" : "인증 필요"}</em>
                </button>
                {renderRegionPopover()}
              </div>
              <div className="listing-search-shell">
                <input type="text" readOnly placeholder="Search photocard, figures, tags..." />
              </div>
            </header>

          <div className="listing-desktop-grid">
            <main className="listing-feed-panel">
              {isBootLoading ? (
                <section
                  className="listing-list listing-list-loading listing-list-loading-initial"
                  aria-label={isProfileLoading ? "프로필 불러오는 중" : "목록 불러오는 중"}
                >
                  <Spinner className="listing-list-spinner" />
                </section>
              ) : (
                <>
                  <div className="listing-feed-head">
                    <h1>{feedTitle}</h1>
                  </div>

                  {renderFeedFilters()}
                  <p className="listing-feed-summary">{feedSummary}</p>

                  {error ? <p className="auth-error">{error}</p> : null}

                  {isProfile ? (
                    <section className="profile-inline-shell">
                      <input
                        ref={profileFileInputRef}
                        type="file"
                        accept="image/*"
                        className="profile-file-input"
                        onChange={(event) => void handleProfileImageInput(event)}
                      />

                      <section className="profile-avatar-section">
                        <div className="profile-avatar-wrap">
                          <div className="profile-avatar-circle">
                            {profileImageDraft ? (
                              <img src={profileImageDraft} alt={me?.nickname?.trim() || "내 프로필"} />
                            ) : (
                              <span>{(me?.nickname?.trim() || "나").slice(0, 1).toUpperCase()}</span>
                            )}
                          </div>
                          <button
                            type="button"
                            className="profile-avatar-hover"
                            onClick={openProfileImagePicker}
                            disabled={profileUploading}
                          >
                            {profileUploading ? "업로드 중.." : "이미지 변경"}
                          </button>
                        </div>
                        <p className="profile-avatar-hint">마우스를 올리면 이미지를 바꿀 수 있어요.</p>
                      </section>

                      <section className="profile-smile-section">
                        <div className="profile-smile-head">
                          <strong>스마일 지수</strong>
                          <span>{formatSmileScore(profileSmileValue)}</span>
                        </div>
                        <div className="profile-smile-track" aria-hidden="true">
                          <div className="profile-smile-fill" style={{ width: `${profileSmileProgress}%` }} />
                        </div>
                        <p>100점 만점 기준으로 표시됩니다.</p>
                      </section>

                      <section className="profile-nickname-block">
                        <label htmlFor="profile-nickname">닉네임</label>
                        <div className="profile-nickname-row">
                          <input
                            id="profile-nickname"
                            type="text"
                            value={profileNicknameDraft}
                            onChange={(event) => setProfileNicknameDraft(event.target.value)}
                            placeholder="닉네임을 입력하세요"
                          />
                          <button type="button" onClick={() => void handleProfileNicknameSave()} disabled={profileSaving}>
                            {profileSaving ? "저장 중.." : "변경"}
                          </button>
                        </div>
                      </section>
                    </section>
                  ) : hasFilteredData ? (
                    renderListingCards(filteredListings)
                  ) : showFilteredEmptyState ? (
                    <p className="region-status">필터에 맞는 굿즈가 없어요.</p>
                  ) : null}
                </>
              )}

              {!loading && listings.length > 0 ? <div ref={loadMoreRef} className="listing-load-trigger" /> : null}
              {loadingMore ? (
                <div className="listing-list listing-list-loading listing-list-loading-more" aria-label="더 불러오는 중">
                  <Spinner className="listing-list-spinner" />
                </div>
              ) : null}
            </main>

            <aside className={tradeRailOpen ? "listing-right-rail" : "listing-right-rail collapsed"}>
              <button
                type="button"
                className={tradeRailOpen ? "trade-rail-toggle open" : "trade-rail-toggle closed"}
                onClick={() =>
                  setTradeRailOpen((current) => {
                    const nextValue = !current;
                    window.localStorage.setItem(TRADE_RAIL_OPEN_KEY, String(nextValue));
                    return nextValue;
                  })
                }
                aria-label={tradeRailOpen ? "접기" : "펼치기"}
              >
                <span aria-hidden="true" className="trade-rail-toggle-icon" />
              </button>

              <div className="trade-rail-body">
                <ChatDock />
              </div>
            </aside>
          </div>
        </div>
      </div>

      <div className="listing-mobile-shell">
        <header className="top-bar">
          <div className="listing-region-menu">
            <button
              type="button"
              className="location-chip plain"
              onClick={() => setRegionSheetOpen(true)}
            >
              <span className="location-chip-label">내 지역</span>
              <strong>{selectedRegion?.dongnm ?? "지역 선택"}</strong>
              <em>{selectedRegion?.verified_at ? "인증됨" : "인증 필요"}</em>
              <span className="chevron">&#x2304;</span>
            </button>
            {renderRegionPopover()}
          </div>
          <div className="top-actions">
            <button type="button" aria-label="menu">
              <span className="icon-menu" aria-hidden="true" />
            </button>
            <button type="button" aria-label="search">
              <span className="icon-search" aria-hidden="true" />
            </button>
            <button type="button" aria-label="alerts">
              <span className="icon-bell" aria-hidden="true" />
            </button>
          </div>
        </header>

        <section className="category-strip">
          <button type="button" className="category-pill">{"\uC54C\uBC14"}</button>
          <button type="button" className="category-pill">{"\uBD80\uB3D9\uC0B0"}</button>
          <button type="button" className="category-pill">{"\uC911\uACE0\uCC28"}</button>
        </section>

        {renderFeedFilters()}

        {isBootLoading ? (
          <section
            className="listing-list listing-list-loading listing-list-loading-initial"
            aria-label={isProfileLoading ? "프로필 불러오는 중" : "목록 불러오는 중"}
          >
            <Spinner className="listing-list-spinner" />
          </section>
        ) : (
          <>
            {error ? <p className="auth-error">{error}</p> : null}
            {isProfile ? (
              <section className="profile-inline-shell">
                <input
                  ref={profileFileInputRef}
                  type="file"
                  accept="image/*"
                  className="profile-file-input"
                  onChange={(event) => void handleProfileImageInput(event)}
                />

                <section className="profile-avatar-section">
                  <div className="profile-avatar-wrap">
                    <div className="profile-avatar-circle">
                      {profileImageDraft ? (
                        <img src={profileImageDraft} alt={me?.nickname?.trim() || "내 프로필"} />
                      ) : (
                        <span>{(me?.nickname?.trim() || "나").slice(0, 1).toUpperCase()}</span>
                      )}
                    </div>
                    <button
                      type="button"
                      className="profile-avatar-hover"
                      onClick={openProfileImagePicker}
                      disabled={profileUploading}
                    >
                      {profileUploading ? "업로드 중.." : "이미지 변경"}
                    </button>
                  </div>
                  <p className="profile-avatar-hint">마우스를 올리면 이미지를 바꿀 수 있어요.</p>
                </section>

                <section className="profile-smile-section">
                  <div className="profile-smile-head">
                    <strong>스마일 지수</strong>
                    <span>{formatSmileScore(profileSmileValue)}</span>
                  </div>
                  <div className="profile-smile-track" aria-hidden="true">
                    <div className="profile-smile-fill" style={{ width: `${profileSmileProgress}%` }} />
                  </div>
                  <p>100점 만점 기준으로 표시됩니다.</p>
                </section>

                <section className="profile-nickname-block">
                  <label htmlFor="profile-nickname">닉네임</label>
                  <div className="profile-nickname-row">
                    <input
                      id="profile-nickname"
                      type="text"
                      value={profileNicknameDraft}
                      onChange={(event) => setProfileNicknameDraft(event.target.value)}
                      placeholder="닉네임을 입력하세요"
                    />
                    <button type="button" onClick={() => void handleProfileNicknameSave()} disabled={profileSaving}>
                      {profileSaving ? "저장 중.." : "변경"}
                    </button>
                  </div>
                </section>
              </section>
            ) : hasFilteredData ? (
              renderListingCards(filteredListings)
            ) : showFilteredEmptyState ? (
              <p className="region-status">필터에 맞는 굿즈가 없어요.</p>
            ) : null}
          </>
        )}

        {!loading && listings.length > 0 ? <div ref={loadMoreRef} className="listing-load-trigger" /> : null}
        {loadingMore ? (
          <div className="listing-list listing-list-loading listing-list-loading-more" aria-label="더 불러오는 중">
            <Spinner className="listing-list-spinner" />
          </div>
        ) : null}
      </div>

      <button
        type="button"
        className={tradeRailOpen ? "floating-button desktop-trade-open" : "floating-button desktop-trade-closed"}
        onClick={handleWriteClick}
      >
        {"+ 굿즈 등록"}
      </button>

      {deleteTarget ? (
        <div className="overlay">
          <div className="overlay-dim" />
          <div className="confirm-modal">
            <p>{`'${deleteTarget.dongnm}'\uC744 \uC0AD\uC81C\uD560\uAE4C\uC694?`}</p>
            <div className="confirm-actions">
              <button type="button" className="confirm-cancel" onClick={() => setDeleteTarget(null)}>
                {"\uCDE8\uC18C"}
              </button>
              <button type="button" className="confirm-delete" onClick={() => void handleDeleteRegion()}>
                {"\uC0AD\uC81C"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {regionSheetOpen ? (
        <button
          type="button"
          className="region-popover-dismiss"
          onClick={() => setRegionSheetOpen(false)}
          aria-label="close region menu"
        />
      ) : null}

      {regionSearchOpen ? (
        <div className="region-search-layer">
          <button
            type="button"
            className="region-search-dismiss"
            onClick={() => {
              setRegionSearchMessage("");
              setRegionSearchOpen(false);
              setRegionSheetOpen(true);
            }}
            aria-label="close region search"
          />
          <div className="region-search-popover">
            <div className="region-search-pointer" />
            <div className="region-search-head">
              <button
                type="button"
                className="region-search-back"
                onClick={() => {
                  setRegionSearchMessage("");
                  setRegionSearchOpen(false);
                  setRegionSheetOpen(true);
                }}
                aria-label="back to regions"
              >
                {"←"}
              </button>
              <div>
                <h2>{"지역 검색"}</h2>
                <p>{"현재 위치로 지역을 추가하고 인증할 수 있어요."}</p>
              </div>
            </div>

            {regionSearchMessage ? <p className="region-search-message">{regionSearchMessage}</p> : null}
            <div className="region-search-input-row">
              <input
                className="region-search"
                type="text"
                value={regionSearchQuery}
                onChange={(event) => setRegionSearchQuery(event.target.value)}
                placeholder={"지역명으로 검색 (ex. 서초동)"}
              />
              <button type="button" className="region-current-button" onClick={handleUseCurrentLocation}>
                {"현재 위치"}
              </button>
            </div>

            {regionSearchLoading ? <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p> : null}
            <div className="region-search-results">
              {regionSearchResults.map((region) => (
                <button
                  key={region.region_id}
                  type="button"
                  className="region-item"
                  disabled={addingRegionId === region.region_id}
                  onClick={() => void handleAddRegion(region)}
                >
                  {addingRegionId === region.region_id ? "추가 및 인증 중.." : region.full_name}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
