import { memo, type MutableRefObject, type RefObject } from "react";
import {
  desktopCategories,
  desktopMarketplace,
  desktopMyPageLinks,
  desktopTrades
} from "./constants";
import type { FeedPath, RailIndicatorState } from "./types";

type ListingLeftRailProps = {
  leftRailRef: RefObject<HTMLElement | null>;
  marketplaceItemRefs: MutableRefObject<Array<HTMLButtonElement | null>>;
  myPageItemRefs: MutableRefObject<Array<HTMLButtonElement | null>>;
  railIndicator: RailIndicatorState;
  isTradingHub: boolean;
  isMyCollection: boolean;
  isWishlist: boolean;
  isSalesHistory: boolean;
  isPurchaseHistory: boolean;
  isReceivedReviews: boolean;
  isProfile: boolean;
  onOpenFeedPage: (path: FeedPath) => void;
};

export const ListingLeftRail = memo(function ListingLeftRail({
  leftRailRef,
  marketplaceItemRefs,
  myPageItemRefs,
  railIndicator,
  isTradingHub,
  isMyCollection,
  isWishlist,
  isSalesHistory,
  isPurchaseHistory,
  isReceivedReviews,
  isProfile,
  onOpenFeedPage
}: ListingLeftRailProps) {
  return (
    <aside className="listing-left-rail" ref={leftRailRef}>
      <span
        aria-hidden="true"
        className={railIndicator.animate ? "listing-rail-indicator" : "listing-rail-indicator no-motion"}
        style={{
          opacity: railIndicator.visible ? 1 : 0,
          transform: `translate3d(0, ${railIndicator.top}px, 0)`,
          height: `${railIndicator.height}px`
        }}
      />
      <div className="listing-brand-row">
        <span className="listing-brand-mark">T</span>
        <strong>TORA KAZE</strong>
      </div>

      <div className="listing-rail-group">
        <span className="listing-rail-label">MARKETPLACE</span>
        <div className="listing-rail-stack">
          {desktopMarketplace.map((item, index) => (
            <button
              key={item}
              ref={(node) => {
                marketplaceItemRefs.current[index] = node;
              }}
              type="button"
              className={
                (index === 0 &&
                  !isTradingHub &&
                  !isMyCollection &&
                  !isWishlist &&
                  !isSalesHistory &&
                  !isPurchaseHistory &&
                  !isReceivedReviews &&
                  !isProfile) ||
                (index === 1 && isTradingHub) ||
                (index === 2 && isMyCollection)
                  ? "listing-rail-item active"
                  : "listing-rail-item"
              }
              onClick={() => onOpenFeedPage(index === 1 ? "/trading" : index === 2 ? "/my-listings" : "/listing")}
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      <div className="listing-rail-group">
        <span className="listing-rail-label">MY PAGE</span>
        <div className="listing-rail-stack">
          {desktopMyPageLinks.map((item, index) => (
            <button
              key={item.path}
              ref={(node) => {
                myPageItemRefs.current[index] = node;
              }}
              type="button"
              className={
                (item.path === "/wishlist" && isWishlist) ||
                (item.path === "/sales-history" && isSalesHistory) ||
                (item.path === "/purchase-history" && isPurchaseHistory) ||
                (item.path === "/received-reviews" && isReceivedReviews) ||
                (item.path === "/profile" && isProfile)
                  ? "listing-rail-item active"
                  : "listing-rail-item"
              }
              onClick={() => onOpenFeedPage(item.path)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="listing-rail-group">
        <span className="listing-rail-label">CATEGORIES</span>
        {desktopCategories.map((item) => (
          <button key={item} type="button" className="listing-rail-item">
            {item}
          </button>
        ))}
      </div>

      <div className="listing-rail-group">
        <span className="listing-rail-label">ACTIVE TRADES</span>
        {desktopTrades.map((item, index) => (
          <button key={item} type="button" className="listing-rail-item trade">
            <span>{item}</span>
            {index === 0 ? <em>1</em> : null}
          </button>
        ))}
      </div>
    </aside>
  );
});
