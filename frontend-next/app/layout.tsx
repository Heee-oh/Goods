import type { Metadata } from "next";
import { cookies } from "next/headers";
import { AppProviders } from "../src/components/AppProviders";
import "../src/tailwind.css";
import "../src/styles.css";

export const metadata: Metadata = {
  title: "Goods",
  description: "Goods marketplace"
};

const TRADE_RAIL_OPEN_COOKIE = "goods-trade-rail-open";

export default async function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  const cookieStore = await cookies();
  const initialTradeRailOpen = cookieStore.get(TRADE_RAIL_OPEN_COOKIE)?.value === "open";

  return (
    <html lang="ko">
      <body>
        <AppProviders initialTradeRailOpen={initialTradeRailOpen}>{children}</AppProviders>
      </body>
    </html>
  );
}
