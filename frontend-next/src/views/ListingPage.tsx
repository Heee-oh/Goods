import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "@/lib/nextRouterCompat";
import type { ChangeEvent } from "react";
import { ApiError, apiRequest } from "../lib/api";
import { convertImageToWebpFile } from "../lib/image";
import { clearSession, getMemberId, getSelectedRegionId } from "../lib/auth";
import { readCachedJson, writeCachedJson } from "../lib/cache";
import {
  OPEN_REGION_SHEET_EVENT,
  type OpenRegionSheetState
} from "@/lib/regionVerification";
import { getTransactionLabel } from "../lib/transactionType";
import { Spinner } from "@/components/ui/spinner";
import { REGIONS_CACHE_PREFIX } from "@/features/listing/constants";
import { ListingCardList } from "@/features/listing/ListingCardList";
import { ListingDesktopTopbar } from "@/features/listing/ListingDesktopTopbar";
import { ListingFeedFilters } from "@/features/listing/ListingFeedFilters";
import { ListingLeftRail } from "@/features/listing/ListingLeftRail";
import { ProfileInlinePanel } from "@/features/listing/ProfileInlinePanel";
import { RegionDeleteConfirm } from "@/features/listing/RegionDeleteConfirm";
import { RegionPopover } from "@/features/listing/RegionPopover";
import { RegionSearchLayer } from "@/features/listing/RegionSearchLayer";
import { ReviewHistoryPanel } from "@/features/listing/ReviewHistoryPanel";
import { TradeHistoryPanel } from "@/features/listing/TradeHistoryPanel";
import type {
  FeedPath,
  InterestSliceResponse,
  ListingDetailPreview,
  ListingItem,
  ListingSliceResponse,
  MemberResponse,
  PublishedListingState,
  RailIndicatorState,
  RegionResponse,
  RegionSearchItem,
  WishlistListingItem
} from "@/features/listing/types";
import {
  formatPrice,
  formatSmileScore,
  normalizeRegion,
  pickInitialRegion,
  saveSelectedRegion
} from "@/features/listing/utils";

let lastRailIndicatorPosition: Pick<RailIndicatorState, "top" | "height"> | null = null;

type ListingPageProps = {
  tradeRailOpen?: boolean;
  initialSelectedRegion?: RegionResponse | null;
};

function getRegionsCacheKey() {
  const memberId = getMemberId();
  return memberId ? `${REGIONS_CACHE_PREFIX}:${memberId}` : REGIONS_CACHE_PREFIX;
}

function markRegionVerificationRequired(regions: RegionResponse[], regionId: number) {
  return regions.map((region) =>
    region.region_id === regionId
      ? {
          ...region,
          verified_at: null
        }
      : region
  );
}

export function ListingPage({ tradeRailOpen = false, initialSelectedRegion = null }: ListingPageProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const isTradingHub = location.pathname.startsWith("/trading");
  const isMyCollection = location.pathname.startsWith("/my-listings");
  const isWishlist = location.pathname.startsWith("/wishlist");
  const isSalesHistory = location.pathname.startsWith("/sales-history");
  const isPurchaseHistory = location.pathname.startsWith("/purchase-history");
  const isReceivedReviews = location.pathname.startsWith("/received-reviews");
  const isProfile = location.pathname.startsWith("/profile");
  const isMyPageSection = isWishlist || isSalesHistory || isPurchaseHistory || isReceivedReviews || isProfile;
  const leftRailRef = useRef<HTMLElement | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const profileFileInputRef = useRef<HTMLInputElement | null>(null);
  const marketplaceItemRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const myPageItemRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const handledOpenRegionSheetRequestRef = useRef<number | null>(null);
  const [regionSheetOpen, setRegionSheetOpen] = useState(false);
  const [regionSearchOpen, setRegionSearchOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<RegionResponse | null>(null);
  const [regions, setRegions] = useState<RegionResponse[]>(() =>
    initialSelectedRegion ? [normalizeRegion(initialSelectedRegion)] : []
  );
  const regionsLoadedRef = useRef(false);
  const [me, setMe] = useState<MemberResponse | null>(null);
  const [profileNicknameDraft, setProfileNicknameDraft] = useState("");
  const [profileImageDraft, setProfileImageDraft] = useState<string | null>(null);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileUploading, setProfileUploading] = useState(false);
  const [selectedRegionId, setSelectedRegionId] = useState<number | null>(initialSelectedRegion?.region_id ?? null);
  const [reverifyRegionId, setReverifyRegionId] = useState<number | null>(null);
  const [verificationFailedRegionId, setVerificationFailedRegionId] = useState<number | null>(null);
  const initialSelectedRegionIdRef = useRef<number | null>(initialSelectedRegion?.region_id ?? null);
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
  const [railIndicator, setRailIndicator] = useState<RailIndicatorState>(() =>
    lastRailIndicatorPosition
      ? {
          ...lastRailIndicatorPosition,
          visible: true,
          animate: true
        }
      : {
          top: 0,
          height: 0,
          visible: false,
          animate: false
        }
  );
  const railIndicatorMeasuredRef = useRef(Boolean(lastRailIndicatorPosition));
  const profileSmileValue = Number(me?.smile_score ?? me?.smileScore ?? 100);
  const profileSmileProgress = Math.max(0, Math.min(100, profileSmileValue / 10));
  const marketplaceActiveIndex = !isTradingHub && !isMyCollection && !isMyPageSection ? 0 : isTradingHub ? 1 : isMyCollection ? 2 : -1;
  const myPageActiveIndex = isWishlist ? 0 : isSalesHistory ? 1 : isPurchaseHistory ? 2 : isReceivedReviews ? 3 : isProfile ? 4 : -1;

  useEffect(() => {
    const sync = () => {
      const rail = leftRailRef.current;
      const item =
        marketplaceActiveIndex >= 0
          ? marketplaceItemRefs.current[marketplaceActiveIndex]
          : myPageActiveIndex >= 0
            ? myPageItemRefs.current[myPageActiveIndex]
            : null;

      if (!rail || !item) {
        setRailIndicator((prev) => (prev.visible ? { ...prev, visible: false, animate: railIndicatorMeasuredRef.current } : prev));
        return;
      }

      const railRect = rail.getBoundingClientRect();
      const itemRect = item.getBoundingClientRect();
      const nextState = {
        top: itemRect.top - railRect.top + rail.scrollTop,
        height: itemRect.height,
        visible: true,
        animate: railIndicatorMeasuredRef.current
      };

      setRailIndicator((prev) =>
        prev.top === nextState.top &&
        prev.height === nextState.height &&
        prev.visible === nextState.visible &&
        prev.animate === nextState.animate
          ? prev
          : nextState
      );
      railIndicatorMeasuredRef.current = true;
      lastRailIndicatorPosition = {
        top: nextState.top,
        height: nextState.height
      };
    };

    sync();
    window.addEventListener("resize", sync);

    return () => {
      window.removeEventListener("resize", sync);
    };
  }, [marketplaceActiveIndex, myPageActiveIndex]);

  const loadRegions = async (options?: { force?: boolean }) => {
    const cacheKey = getRegionsCacheKey();

    if (!options?.force) {
      const cachedRegions = readCachedJson<RegionResponse[]>(cacheKey);
      if (cachedRegions && cachedRegions.length > 0) {
        const normalizedCached = cachedRegions.map(normalizeRegion);
        setRegions(normalizedCached);
        regionsLoadedRef.current = true;
        return normalizedCached;
      }
    }

    const myRegions = await apiRequest<RegionResponse[]>("/api/members/me/regions");
    const normalized = myRegions.map(normalizeRegion);
    setRegions(normalized);
    regionsLoadedRef.current = true;
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
    if (isProfile || isSalesHistory || isPurchaseHistory || isReceivedReviews) {
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
    if (isProfile || isSalesHistory || isPurchaseHistory || isReceivedReviews) {
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
    if (isProfile || isSalesHistory || isPurchaseHistory || isReceivedReviews) {
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
  }, [
    activeFeedFilter,
    isMyCollection,
    isTradingHub,
    isWishlist,
    isProfile,
    isSalesHistory,
    isPurchaseHistory,
    isReceivedReviews,
    listings
  ]);

  const isInitialListingLoading = loading && filteredListings.length === 0;
  const isProfileLoading = isProfile && loading && me == null;
  const hasFilteredData = filteredListings.length > 0;
  const showFilteredEmptyState = !loading && filteredListings.length === 0;

  useEffect(() => {
    const load = async () => {
      const shouldShowLoading = !isProfile || me == null;
      try {
        if (shouldShowLoading) {
          setLoading(true);
        }
        setError("");

        const normalized = regionsLoadedRef.current ? regions : await loadRegions();
        if (isWishlist) {
          const savedRegionId = getSelectedRegionId() ?? initialSelectedRegionIdRef.current;
          const initialRegion = pickInitialRegion(normalized, savedRegionId);
          if (initialRegion) {
            setSelectedRegionId(initialRegion.region_id);
            saveSelectedRegion(initialRegion);
          }
          await loadCurrentFeed(initialRegion?.region_id);
          return;
        }

        if (normalized.length === 0) {
          setListings([]);
          return;
        }

        const savedRegionId = getSelectedRegionId() ?? initialSelectedRegionIdRef.current;
        const initialRegion = pickInitialRegion(normalized, savedRegionId);

        if (initialRegion) {
          setSelectedRegionId(initialRegion.region_id);
          saveSelectedRegion(initialRegion);
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
          } else if (isSalesHistory || isPurchaseHistory || isReceivedReviews) {
            setListings([]);
            setHasMore(false);
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
  }, [navigate, isTradingHub, isWishlist, isProfile, isSalesHistory, isPurchaseHistory, isReceivedReviews]);

  useEffect(() => {
    const { refreshAt, publishedListingId } = (location.state as PublishedListingState | null) ?? {};
    if (!refreshAt || loading || (!selectedRegionId && !isWishlist) || (isMyPageSection && !isWishlist)) {
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
  }, [location.state, loading, selectedRegionId, isTradingHub, isWishlist, isMyPageSection]);

  useEffect(() => {
    setActiveFeedFilter("all");
  }, [isTradingHub, isWishlist, isProfile, isSalesHistory, isPurchaseHistory, isReceivedReviews]);

  useEffect(() => {
    if (!regionSearchOpen) {
      return;
    }

    const query = regionSearchQuery.trim();
    if (query.length === 0) {
      setRegionSearchLoading(false);
      setRegionSearchResults([]);
      return;
    }

    const timeout = window.setTimeout(async () => {
      try {
        setRegionSearchLoading(true);

        const data = await apiRequest<RegionSearchItem[]>(
          `/api/regions/search?query=${encodeURIComponent(query)}`,
          { auth: false }
        );
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
    if (
      (!selectedRegionId && !isWishlist) ||
      !loadMoreRef.current ||
      loading ||
      loadingMore ||
      !hasMore ||
      (isMyPageSection && !isWishlist)
    ) {
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
  }, [selectedRegionId, listings, loading, loadingMore, hasMore, isWishlist, isMyPageSection]);

  const selectedRegion = useMemo(
    () => regions.find((region) => region.region_id === selectedRegionId) ?? null,
    [regions, selectedRegionId]
  );

  const requireRegionReverification = useCallback((regionId: number) => {
    const cacheKey = getRegionsCacheKey();
    const nextRegions = markRegionVerificationRequired(regions, regionId);
    const cachedRegions = readCachedJson<RegionResponse[]>(cacheKey);
    const nextCachedRegions = cachedRegions
      ? markRegionVerificationRequired(cachedRegions.map(normalizeRegion), regionId)
      : null;
    const nextRegionsCache =
      nextCachedRegions && nextCachedRegions.length > nextRegions.length ? nextCachedRegions : nextRegions;

    if (nextRegions.length > 0) {
      setRegions(nextRegions);
    }
    if (nextRegionsCache.length > 0) {
      writeCachedJson(cacheKey, nextRegionsCache);
    }

    const nextSelectedRegion =
      nextRegions.find((region) => region.region_id === regionId) ??
      nextCachedRegions?.find((region) => region.region_id === regionId) ??
      null;
    const currentSelectedRegionId = selectedRegionId ?? getSelectedRegionId();
    if (currentSelectedRegionId === regionId && nextSelectedRegion) {
      saveSelectedRegion(nextSelectedRegion);
    }

    setReverifyRegionId(regionId);
  }, [regions, selectedRegionId]);

  const canAddRegion = regions.filter((region) => region.primary).length < 2;
  const highlightedListing = listings[0] ?? null;
  const highlightedListingDeal = highlightedListing
    ? highlightedListing.transaction_type === "sell" && highlightedListing.price_amount > 0
      ? formatPrice(highlightedListing.price_amount, highlightedListing.transaction_type)
      : getTransactionLabel(highlightedListing.transaction_type)
    : "";
  const feedTitle = isProfile
    ? "Profile"
    : isWishlist
      ? "Wishlist"
      : isSalesHistory
        ? "판매 기록"
        : isPurchaseHistory
          ? "구매 기록"
          : isReceivedReviews
            ? "받은 리뷰"
            : isTradingHub
              ? "Trading Hub"
              : "Recommended Matches";
  const feedSummary = isWishlist
    ? `${filteredListings.length}개의 저장한 굿즈`
    : isProfile
      ? `${me?.nickname?.trim() || "내 프로필"} · 스마일지수 ${formatSmileScore(me?.smile_score ?? me?.smileScore ?? 100)}`
      : isSalesHistory
        ? "거래 완료된 판매 내역을 확인할 수 있어요."
        : isPurchaseHistory
          ? "구매한 거래 내역을 확인할 수 있어요."
          : isReceivedReviews
            ? "상대방이 남긴 후기를 모아볼 수 있어요."
      : isTradingHub
        ? "교환 게시글을 보고 있어요."
        : isMyCollection
          ? "내 게시글을 보고 있어요."
          : "거래 및 나눔 게시글을 보고 있어요.";
  const isBootLoading = isProfileLoading || isInitialListingLoading;
  const isFeedLoadingOverlay = loading && !isBootLoading && !loadingMore;

  const handleSelectRegion = async (regionId: number) => {
    try {
      setError("");
      setVerificationFailedRegionId(null);
      const targetRegion = regions.find((region) => region.region_id === regionId) ?? null;
      const needsVerification = targetRegion && (!targetRegion.verified_at || reverifyRegionId === regionId);

      if (needsVerification) {
        try {
          await verifyRegion(regionId);
        } catch {
          requireRegionReverification(regionId);
          setVerificationFailedRegionId(regionId);
          return;
        }

        const reloaded = await loadRegions({ force: true });
        const verifiedRegion = reloaded.find((region) => region.region_id === regionId) ?? targetRegion;
        setRegions(reloaded);
        setSelectedRegionId(verifiedRegion.region_id);
        saveSelectedRegion(verifiedRegion);
        setReverifyRegionId((current) => (current === regionId ? null : current));
        setVerificationFailedRegionId(null);
      } else {
        setSelectedRegionId(regionId);
        setVerificationFailedRegionId(null);
        if (targetRegion) {
          saveSelectedRegion(targetRegion);
        }
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
        saveSelectedRegion(nextSelectedRegion);
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
        setRegionSearchMessage("현재 위치에서 인증할 수 없는 지역이에요.");
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
      saveSelectedRegion(nextRegion);
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

  const handleLogout = () => {
    clearSession();
    navigate("/welcome", { replace: true });
  };

  const handleWriteClick = () => {
    navigate("/sell", { state: { backgroundLocation: location } });
  };

  const openFeedPage = useCallback((path: FeedPath) => {
    if (location.pathname !== path) {
      navigate(path);
    }
  }, [location.pathname, navigate]);

  const openRegionSheet = useCallback((options?: { requireReverification?: boolean }) => {
    setVerificationFailedRegionId(null);
    if (options?.requireReverification) {
      const regionId = selectedRegionId ?? getSelectedRegionId();
      if (regionId != null) {
        requireRegionReverification(regionId);
      }
    }
    setRegionSearchOpen(false);
    setRegionSheetOpen(true);
  }, [requireRegionReverification, selectedRegionId]);

  const openRegionSearch = useCallback(() => {
    setRegionSearchQuery("");
    setRegionSearchResults([]);
    setRegionSearchLoading(false);
    setRegionSearchMessage("");
    setVerificationFailedRegionId(null);
    setRegionSearchOpen(true);
    setRegionSheetOpen(false);
  }, []);

  const closeRegionSearchToSheet = useCallback(() => {
    setRegionSearchMessage("");
    setRegionSearchOpen(false);
    setRegionSheetOpen(true);
  }, []);

  useEffect(() => {
    const handleOpenRegionSheet = () => {
      openRegionSheet({ requireReverification: true });
    };

    window.addEventListener(OPEN_REGION_SHEET_EVENT, handleOpenRegionSheet);

    return () => {
      window.removeEventListener(OPEN_REGION_SHEET_EVENT, handleOpenRegionSheet);
    };
  }, [openRegionSheet]);

  useEffect(() => {
    const state = location.state as OpenRegionSheetState | null;
    if (!state?.openRegionSheet) {
      return;
    }

    const requestKey = state.openRegionSheetRequestedAt ?? 0;
    if (handledOpenRegionSheetRequestRef.current === requestKey) {
      return;
    }

    handledOpenRegionSheetRequestRef.current = requestKey;
    openRegionSheet({ requireReverification: true });
  }, [location.state, openRegionSheet]);

  const openListingDetail = (listingId: number) => {
    const basePath = isTradingHub
      ? "/trading"
      : isMyCollection
        ? "/my-listings"
        : isWishlist
          ? "/wishlist"
          : isSalesHistory
            ? "/sales-history"
            : isPurchaseHistory
              ? "/purchase-history"
              : isReceivedReviews
                ? "/received-reviews"
                : "/listing";
    navigate(`${basePath}/${listingId}`, { state: { backgroundLocation: location } });
  };

  const regionPopover = (
    <RegionPopover
      open={regionSheetOpen}
      regions={regions}
      selectedRegionId={selectedRegionId}
      reverifyRegionId={reverifyRegionId}
      verificationFailedRegionId={verificationFailedRegionId}
      canAddRegion={canAddRegion}
      onSelectRegion={(regionId) => void handleSelectRegion(regionId)}
      onRequestDelete={setDeleteTarget}
      onOpenRegionSearch={openRegionSearch}
    />
  );

  return (
    <div className="page page-listing">
      <div className="listing-desktop-shell">
        <ListingLeftRail
          leftRailRef={leftRailRef}
          marketplaceItemRefs={marketplaceItemRefs}
          myPageItemRefs={myPageItemRefs}
          railIndicator={railIndicator}
          isTradingHub={isTradingHub}
          isMyCollection={isMyCollection}
          isWishlist={isWishlist}
          isSalesHistory={isSalesHistory}
          isPurchaseHistory={isPurchaseHistory}
          isReceivedReviews={isReceivedReviews}
          isProfile={isProfile}
          onOpenFeedPage={openFeedPage}
        />

          <div className="listing-center-rail">
            <ListingDesktopTopbar
              selectedRegionName={selectedRegion?.dongnm ?? "지역 선택"}
              selectedRegionVerified={Boolean(selectedRegion?.verified_at)}
              regionPopover={regionPopover}
              onOpenRegionSheet={openRegionSheet}
            />

          <div className="listing-desktop-grid">
            <main className={isFeedLoadingOverlay ? "listing-feed-panel is-loading" : "listing-feed-panel"}>
              {isBootLoading ? (
                <section
                  className="listing-list listing-list-loading listing-list-loading-initial"
                  aria-label={isProfileLoading ? "프로필 불러오는 중" : "목록 불러오는 중"}
                >
                  <Spinner className="listing-list-spinner" />
                </section>
              ) : (
                <div className="listing-feed-content">
                  <div className="listing-feed-head">
                    <h1>{feedTitle}</h1>
                  </div>

                  <ListingFeedFilters
                    activeFilter={activeFeedFilter}
                    isTradingHub={isTradingHub}
                    hideFilters={isMyPageSection}
                    onChangeFilter={setActiveFeedFilter}
                  />
                  <p className="listing-feed-summary">{feedSummary}</p>

                  {error ? <p className="auth-error">{error}</p> : null}

                  {isProfile ? (
                    <ProfileInlinePanel
                      fileInputRef={profileFileInputRef}
                      nicknameInputId="profile-nickname-desktop"
                      nickname={me?.nickname?.trim() || "나"}
                      profileImage={profileImageDraft}
                      profileUploading={profileUploading}
                      profileSaving={profileSaving}
                      nicknameDraft={profileNicknameDraft}
                      smileValue={profileSmileValue}
                      smileProgress={profileSmileProgress}
                      onImageInput={(event) => void handleProfileImageInput(event)}
                      onOpenImagePicker={openProfileImagePicker}
                      onNicknameDraftChange={setProfileNicknameDraft}
                      onNicknameSave={() => void handleProfileNicknameSave()}
                      onLogout={handleLogout}
                    />
                  ) : isSalesHistory ? (
                    <TradeHistoryPanel mode="sales" />
                  ) : isPurchaseHistory ? (
                    <TradeHistoryPanel mode="purchases" />
                  ) : isReceivedReviews ? (
                    <ReviewHistoryPanel />
                  ) : hasFilteredData ? (
                    <ListingCardList items={filteredListings} onOpenListingDetail={openListingDetail} />
                  ) : showFilteredEmptyState ? (
                    <p className="region-status">필터에 맞는 굿즈가 없어요.</p>
                  ) : null}
                </div>
              )}

              {isFeedLoadingOverlay ? (
                <div className="listing-feed-loading-overlay" aria-label="목록 갱신 중">
                  <Spinner className="listing-list-spinner" />
                </div>
              ) : null}

              {!loading && listings.length > 0 ? <div ref={loadMoreRef} className="listing-load-trigger" /> : null}
              {loadingMore ? (
                <div className="listing-list listing-list-loading listing-list-loading-more" aria-label="더 불러오는 중">
                  <Spinner className="listing-list-spinner" />
                </div>
              ) : null}
            </main>

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
            {regionPopover}
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

        <ListingFeedFilters
          activeFilter={activeFeedFilter}
          isTradingHub={isTradingHub}
          hideFilters={isMyPageSection}
          onChangeFilter={setActiveFeedFilter}
        />

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
              <ProfileInlinePanel
                fileInputRef={profileFileInputRef}
                nicknameInputId="profile-nickname-mobile"
                nickname={me?.nickname?.trim() || "나"}
                profileImage={profileImageDraft}
                profileUploading={profileUploading}
                profileSaving={profileSaving}
                nicknameDraft={profileNicknameDraft}
                smileValue={profileSmileValue}
                smileProgress={profileSmileProgress}
                onImageInput={(event) => void handleProfileImageInput(event)}
                onOpenImagePicker={openProfileImagePicker}
                onNicknameDraftChange={setProfileNicknameDraft}
                onNicknameSave={() => void handleProfileNicknameSave()}
                onLogout={handleLogout}
              />
            ) : isSalesHistory ? (
              <TradeHistoryPanel mode="sales" />
            ) : isPurchaseHistory ? (
              <TradeHistoryPanel mode="purchases" />
            ) : isReceivedReviews ? (
              <ReviewHistoryPanel />
            ) : hasFilteredData ? (
              <ListingCardList items={filteredListings} onOpenListingDetail={openListingDetail} />
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

      <RegionDeleteConfirm
        target={deleteTarget}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void handleDeleteRegion()}
      />
      {regionSheetOpen ? (
        <button
          type="button"
          className="region-popover-dismiss"
          onClick={() => setRegionSheetOpen(false)}
          aria-label="close region menu"
        />
      ) : null}

      <RegionSearchLayer
        open={regionSearchOpen}
        query={regionSearchQuery}
        message={regionSearchMessage}
        loading={regionSearchLoading}
        results={regionSearchResults}
        addingRegionId={addingRegionId}
        onBack={closeRegionSearchToSheet}
        onQueryChange={setRegionSearchQuery}
        onUseCurrentLocation={handleUseCurrentLocation}
        onAddRegion={(region) => void handleAddRegion(region)}
      />
    </div>
  );
}
