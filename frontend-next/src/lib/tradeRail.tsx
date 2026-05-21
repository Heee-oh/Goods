"use client";

import { createContext, useCallback, useContext, useMemo, useState, type PropsWithChildren } from "react";

const TRADE_RAIL_OPEN_KEY = "goods:trade-rail-open";
const TRADE_RAIL_OPEN_COOKIE = "goods-trade-rail-open";

type TradeRailContextValue = {
  tradeRailOpen: boolean;
  toggleTradeRail: () => void;
};

const TradeRailContext = createContext<TradeRailContextValue | null>(null);

function saveTradeRailOpen(open: boolean) {
  const value = open ? "open" : "closed";
  document.cookie = `${TRADE_RAIL_OPEN_COOKIE}=${value}; Path=/; Max-Age=31536000; SameSite=Lax`;
  window.localStorage.setItem(TRADE_RAIL_OPEN_KEY, value);
}

export function TradeRailProvider({
  children,
  initialOpen = false
}: PropsWithChildren<{
  initialOpen?: boolean;
}>) {
  const [tradeRailOpen, setTradeRailOpen] = useState(initialOpen);

  const toggleTradeRail = useCallback(() => {
    setTradeRailOpen((current) => {
      const nextValue = !current;
      saveTradeRailOpen(nextValue);
      return nextValue;
    });
  }, []);

  const value = useMemo(
    () => ({
      tradeRailOpen,
      toggleTradeRail
    }),
    [toggleTradeRail, tradeRailOpen]
  );

  return <TradeRailContext.Provider value={value}>{children}</TradeRailContext.Provider>;
}

export function useTradeRail() {
  const context = useContext(TradeRailContext);
  if (!context) {
    throw new Error("useTradeRail must be used within TradeRailProvider");
  }

  return context;
}
