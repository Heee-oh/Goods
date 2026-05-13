const ACCESS_TOKEN_KEY = "accessToken";
const MEMBER_ID_KEY = "memberId";
const EXPIRES_IN_KEY = "expiresIn";
const SIGNUP_REGION_ID_KEY = "signup_region_id";
const SIGNUP_REGION_NAME_KEY = "signup_region";
const SELECTED_REGION_ID_KEY = "selected_region_id";
const SELECTED_REGION_COOKIE = "goods-selected-region";
const SELL_DRAFT_ID_KEY = "sell_draft_id";

export type SessionPayload = {
  accessToken: string;
  memberId: string;
  expiresIn: number;
};

function getLocalStorage() {
  return typeof window === "undefined" ? null : window.localStorage;
}

export function getAccessToken() {
  return getLocalStorage()?.getItem(ACCESS_TOKEN_KEY) ?? null;
}

export function getMemberId() {
  return getLocalStorage()?.getItem(MEMBER_ID_KEY) ?? null;
}

export function hasAccessToken() {
  const token = getAccessToken();
  return Boolean(token && token.trim());
}

export function saveSession(session: SessionPayload) {
  const storage = getLocalStorage();
  storage?.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  storage?.setItem(MEMBER_ID_KEY, session.memberId);
  storage?.setItem(EXPIRES_IN_KEY, String(session.expiresIn));
}

export function clearSession() {
  const storage = getLocalStorage();
  storage?.removeItem(ACCESS_TOKEN_KEY);
  storage?.removeItem(MEMBER_ID_KEY);
  storage?.removeItem(EXPIRES_IN_KEY);
  storage?.removeItem(SELECTED_REGION_ID_KEY);
  storage?.removeItem(SELL_DRAFT_ID_KEY);

  if (typeof document !== "undefined") {
    document.cookie = `${SELECTED_REGION_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  }
}

export function saveSignupRegion(regionId: number, regionName: string) {
  const storage = getLocalStorage();
  storage?.setItem(SIGNUP_REGION_ID_KEY, String(regionId));
  storage?.setItem(SIGNUP_REGION_NAME_KEY, regionName);
}

export function getSignupRegion() {
  const storage = getLocalStorage();
  const regionId = storage?.getItem(SIGNUP_REGION_ID_KEY);
  const regionName = storage?.getItem(SIGNUP_REGION_NAME_KEY) ?? null;

  return {
    regionId: regionId ? Number(regionId) : null,
    regionName
  };
}

export function saveSelectedRegionId(regionId: number) {
  getLocalStorage()?.setItem(SELECTED_REGION_ID_KEY, String(regionId));
}

export function getSelectedRegionId() {
  const value = getLocalStorage()?.getItem(SELECTED_REGION_ID_KEY);
  return value ? Number(value) : null;
}

export function saveSellDraftId(listingId: number) {
  getLocalStorage()?.setItem(SELL_DRAFT_ID_KEY, String(listingId));
}

export function getSellDraftId() {
  const value = getLocalStorage()?.getItem(SELL_DRAFT_ID_KEY);
  return value ? Number(value) : null;
}

export function clearSellDraftId() {
  getLocalStorage()?.removeItem(SELL_DRAFT_ID_KEY);
}
