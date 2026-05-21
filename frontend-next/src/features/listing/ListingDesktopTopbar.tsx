import { memo, type ReactNode } from "react";

type ListingDesktopTopbarProps = {
  selectedRegionName: string;
  selectedRegionVerified: boolean;
  regionPopover: ReactNode;
  onOpenRegionSheet: () => void;
};

export const ListingDesktopTopbar = memo(function ListingDesktopTopbar({
  selectedRegionName,
  selectedRegionVerified,
  regionPopover,
  onOpenRegionSheet
}: ListingDesktopTopbarProps) {
  return (
    <header className="listing-desktop-topbar">
      <div className="listing-region-menu">
        <button type="button" className="listing-region-chip" onClick={onOpenRegionSheet}>
          <span className="listing-region-chip-label">내 지역</span>
          <strong>{selectedRegionName}</strong>
          <em>{selectedRegionVerified ? "인증됨" : "인증 필요"}</em>
        </button>
        {regionPopover}
      </div>
      <div className="listing-search-shell">
        <input type="text" readOnly placeholder="Search photocard, figures, tags..." />
      </div>
    </header>
  );
});
