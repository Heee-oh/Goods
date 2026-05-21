"use client";

import { memo } from "react";
import { usePathname } from "next/navigation";
import { ChatDock } from "./ChatDock";
import { useTradeRail } from "../lib/tradeRail";

const LISTING_CHROME_PATH = /^\/(listing|trading|my-listings|wishlist|sales-history|purchase-history|received-reviews|profile)(?:\/|$)/;

const ListingRightRail = memo(function ListingRightRail({
  tradeRailOpen,
  onToggleTradeRail
}: {
  tradeRailOpen: boolean;
  onToggleTradeRail: () => void;
}) {
  return (
    <aside className={["listing-right-rail", tradeRailOpen ? "" : "collapsed"].filter(Boolean).join(" ")}>
      <button
        type="button"
        className={tradeRailOpen ? "trade-rail-toggle open" : "trade-rail-toggle closed"}
        onClick={onToggleTradeRail}
        aria-label={tradeRailOpen ? "접기" : "펼치기"}
      >
        <span aria-hidden="true" className="trade-rail-toggle-icon" />
      </button>

      <div className="trade-rail-body">
        <ChatDock />
      </div>
    </aside>
  );
});

export function PersistentListingRightRail() {
  const pathname = usePathname() ?? "/";
  const { tradeRailOpen, toggleTradeRail } = useTradeRail();

  if (!LISTING_CHROME_PATH.test(pathname)) {
    return null;
  }

  return <ListingRightRail tradeRailOpen={tradeRailOpen} onToggleTradeRail={toggleTradeRail} />;
}
