export const REGION_VERIFICATION_EXPIRED_CODE = "REGION_VERIFICATION_EXPIRED";
export const MEMBER_REGION_VERIFICATION_FAILED_CODE = "MEMBER_REGION_VERIFICATION_FAILED";
export const REGION_VERIFICATION_EXPIRED_EVENT = "goods:region-verification-expired";
export const OPEN_REGION_SHEET_EVENT = "goods:open-region-sheet";

export type RegionVerificationExpiredDetail = {
  code: typeof REGION_VERIFICATION_EXPIRED_CODE;
  message: string;
  status: number;
};

export type OpenRegionSheetState = {
  openRegionSheet?: boolean;
  openRegionSheetRequestedAt?: number;
};
