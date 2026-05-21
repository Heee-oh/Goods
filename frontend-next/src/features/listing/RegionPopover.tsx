import type { RegionResponse } from "./types";

type RegionPopoverProps = {
  open: boolean;
  regions: RegionResponse[];
  selectedRegionId: number | null;
  canAddRegion: boolean;
  onSelectRegion: (regionId: number) => void;
  onRequestDelete: (region: RegionResponse) => void;
  onOpenRegionSearch: () => void;
};

export function RegionPopover({
  open,
  regions,
  selectedRegionId,
  canAddRegion,
  onSelectRegion,
  onRequestDelete,
  onOpenRegionSearch
}: RegionPopoverProps) {
  if (!open) {
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
              onClick={() => onSelectRegion(region.region_id)}
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
              onClick={() => onRequestDelete(region)}
              disabled={regions.length <= 1}
              aria-label={`${region.dongnm} 삭제`}
            >
              {"\u00D7"}
            </button>
          </div>
        ))}
      </div>

      {canAddRegion ? (
        <button type="button" className="region-add-button" onClick={onOpenRegionSearch}>
          {"+ 지역 추가"}
        </button>
      ) : (
        <p className="region-limit-note">{"이미 2개라면 더 이상 추가할 수 없어요."}</p>
      )}
    </div>
  );
}
