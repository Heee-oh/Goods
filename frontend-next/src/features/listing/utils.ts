import { saveSelectedRegionId } from "@/lib/auth";
import type { TransactionType } from "@/lib/transactionType";
import { SELECTED_REGION_COOKIE } from "./constants";
import type { RegionResponse } from "./types";

export function saveSelectedRegion(region: RegionResponse) {
  const snapshot = encodeURIComponent(
    JSON.stringify({
      region_id: region.region_id,
      dongnm: region.dongnm,
      verified_at: region.verified_at ?? null,
      primary: Boolean(region.primary ?? region.is_primary)
    })
  );

  document.cookie = `${SELECTED_REGION_COOKIE}=${snapshot}; Path=/; Max-Age=31536000; SameSite=Lax`;
  saveSelectedRegionId(region.region_id);
}

export function formatPrice(amount: number, transactionType: TransactionType) {
  if (transactionType === "free") {
    return "\uB098\uB214";
  }

  if (transactionType === "trade") {
    return "\uAD50\uD658";
  }

  return `${amount.toLocaleString("ko-KR")}\uC6D0`;
}

export function formatUpdatedAt(value: string) {
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

export function formatDistance(value: number | null | undefined) {
  if (value == null || Number.isNaN(Number(value))) {
    return null;
  }

  const distance = Number(value);
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))}m` : `${distance.toFixed(1)}km`;
}

export function formatSmileScore(value: number | null | undefined) {
  return `${(Number(value ?? 100) / 10).toFixed(1)}점`;
}

export function normalizeRegion(region: RegionResponse): RegionResponse {
  return {
    ...region,
    primary: Boolean(region.is_primary ?? region.primary),
    verified_at: region.verified_at ?? null
  };
}

export function pickInitialRegion(regions: RegionResponse[], savedRegionId: number | null) {
  return (
    regions.find((region) => region.region_id === savedRegionId && region.verified_at) ??
    regions.find((region) => region.primary && region.verified_at) ??
    regions.find((region) => region.verified_at) ??
    regions.find((region) => region.region_id === savedRegionId) ??
    regions[0] ??
    null
  );
}
