"use client";

import type { PropsWithChildren } from "react";
import { ChatNotificationsProvider } from "../lib/chatNotifications";
import { TradeRailProvider } from "../lib/tradeRail";
import { PersistentListingRightRail } from "./PersistentListingRightRail";

export function AppProviders({
  children,
  initialTradeRailOpen = false
}: PropsWithChildren<{
  initialTradeRailOpen?: boolean;
}>) {
  return (
    <TradeRailProvider initialOpen={initialTradeRailOpen}>
      <ChatNotificationsProvider>
        {children}
        <PersistentListingRightRail />
      </ChatNotificationsProvider>
    </TradeRailProvider>
  );
}
