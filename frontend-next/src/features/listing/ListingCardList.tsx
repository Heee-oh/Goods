import {
  getListingStatusLabel,
  getListingStatusTone,
  shouldShowStatusBadge
} from "@/lib/listingStatus";
import type { ListingItem } from "./types";
import { formatDistance, formatPrice, formatUpdatedAt } from "./utils";

function Thumbnail({ imageUrl, tone }: { imageUrl: string | null; tone: string }) {
  if (imageUrl) {
    return <img className={`listing-thumb real-image ${tone}`} src={imageUrl} alt="" />;
  }

  return <div className={`listing-thumb ${tone}`} />;
}

type ListingCardListProps = {
  items: ListingItem[];
  onOpenListingDetail: (listingId: number) => void;
};

export function ListingCardList({ items, onOpenListingDetail }: ListingCardListProps) {
  return (
    <section className="listing-list">
      {items.map((item, index) => {
        const distanceLabel = formatDistance(item.distance_km ?? item.distanceKm);

        return (
          <article
            key={item.listing_id}
            className="listing-card"
            onClick={() => onOpenListingDetail(item.listing_id)}
            role="button"
            tabIndex={0}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onOpenListingDetail(item.listing_id);
              }
            }}
          >
            <Thumbnail imageUrl={item.first_image} tone={["sunset", "olive", "midnight", "sand"][index % 4]} />
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
}
