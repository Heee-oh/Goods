import { feedFilters, tradeFilters } from "./constants";

type ListingFeedFiltersProps = {
  activeFilter: string;
  isTradingHub: boolean;
  hideFilters: boolean;
  onChangeFilter: (filterId: string) => void;
};

export function ListingFeedFilters({
  activeFilter,
  isTradingHub,
  hideFilters,
  onChangeFilter
}: ListingFeedFiltersProps) {
  if (hideFilters) {
    return null;
  }

  return (
    <div className="listing-feed-filters" aria-label="세부 필터">
      {(isTradingHub ? tradeFilters : feedFilters).map((filter) => (
        <button
          key={filter.id}
          type="button"
          className={activeFilter === filter.id ? "listing-feed-filter active" : "listing-feed-filter"}
          onClick={() => onChangeFilter(filter.id)}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
}
