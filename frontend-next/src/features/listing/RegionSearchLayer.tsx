import type { RegionSearchItem } from "./types";

type RegionSearchLayerProps = {
  open: boolean;
  query: string;
  message: string;
  loading: boolean;
  results: RegionSearchItem[];
  addingRegionId: number | null;
  onBack: () => void;
  onQueryChange: (query: string) => void;
  onUseCurrentLocation: () => void;
  onAddRegion: (region: RegionSearchItem) => void;
};

export function RegionSearchLayer({
  open,
  query,
  message,
  loading,
  results,
  addingRegionId,
  onBack,
  onQueryChange,
  onUseCurrentLocation,
  onAddRegion
}: RegionSearchLayerProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="region-search-layer">
      <button type="button" className="region-search-dismiss" onClick={onBack} aria-label="close region search" />
      <div className="region-search-popover">
        <div className="region-search-pointer" />
        <div className="region-search-head">
          <button type="button" className="region-search-back" onClick={onBack} aria-label="back to regions">
            {"←"}
          </button>
          <div>
            <h2>{"지역 검색"}</h2>
            <p>{"현재 위치로 지역을 추가하고 인증할 수 있어요."}</p>
          </div>
        </div>

        {message ? <p className="region-search-message">{message}</p> : null}
        <div className="region-search-input-row">
          <input
            className="region-search"
            type="text"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder={"지역명으로 검색 (ex. 서초동)"}
          />
          <button type="button" className="region-current-button" onClick={onUseCurrentLocation}>
            {"현재 위치"}
          </button>
        </div>

        {loading ? <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p> : null}
        <div className="region-search-results">
          {results.map((region) => (
            <button
              key={region.region_id}
              type="button"
              className="region-item"
              disabled={addingRegionId === region.region_id}
              onClick={() => onAddRegion(region)}
            >
              {addingRegionId === region.region_id ? "추가 및 인증 중.." : region.full_name}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
