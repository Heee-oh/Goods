const ACCESS_TOKEN_KEY = "accessToken";
const MEMBER_ID_KEY = "memberId";
const EXPIRES_IN_KEY = "expiresIn";
const SIGNUP_REGION_ID_KEY = "signup_region_id";
const SIGNUP_REGION_NAME_KEY = "signup_region";
const SELECTED_REGION_ID_KEY = "selected_region_id";
const SELL_DRAFT_ID_KEY = "sell_draft_id";

export type SessionPayload = {
  accessToken: string;
  memberId: string;
  expiresIn: number;
};

export function getAccessToken() {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getMemberId() {
  return window.localStorage.getItem(MEMBER_ID_KEY);
}

export function hasAccessToken() {
  const token = getAccessToken();
  return Boolean(token && token.trim());
}

export function saveSession(session: SessionPayload) {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  window.localStorage.setItem(MEMBER_ID_KEY, session.memberId);
  window.localStorage.setItem(EXPIRES_IN_KEY, String(session.expiresIn));
}

export function clearSession() {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(MEMBER_ID_KEY);
  window.localStorage.removeItem(EXPIRES_IN_KEY);
  window.localStorage.removeItem(SELECTED_REGION_ID_KEY);
  window.localStorage.removeItem(SELL_DRAFT_ID_KEY);
}

export function saveSignupRegion(regionId: number, regionName: string) {
  window.localStorage.setItem(SIGNUP_REGION_ID_KEY, String(regionId));
  window.localStorage.setItem(SIGNUP_REGION_NAME_KEY, regionName);
}

export function getSignupRegion() {
  const regionId = window.localStorage.getItem(SIGNUP_REGION_ID_KEY);
  const regionName = window.localStorage.getItem(SIGNUP_REGION_NAME_KEY);

  return {
    regionId: regionId ? Number(regionId) : null,
    regionName
  };
}

export function saveSelectedRegionId(regionId: number) {
  window.localStorage.setItem(SELECTED_REGION_ID_KEY, String(regionId));
}

export function getSelectedRegionId() {
  const value = window.localStorage.getItem(SELECTED_REGION_ID_KEY);
  return value ? Number(value) : null;
}

export function saveSellDraftId(listingId: number) {
  window.localStorage.setItem(SELL_DRAFT_ID_KEY, String(listingId));
}

export function getSellDraftId() {
  const value = window.localStorage.getItem(SELL_DRAFT_ID_KEY);
  return value ? Number(value) : null;
}

export function clearSellDraftId() {
  window.localStorage.removeItem(SELL_DRAFT_ID_KEY);
}
