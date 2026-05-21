"use client";

import {
  LocationOverrideProvider,
  useLocation,
  useNavigate,
  type Location
} from "@/lib/nextRouterCompat";
import { MainLayout } from "./components/MainLayout";
import { useTradeRail } from "./lib/tradeRail";
import { AppOverlays } from "./features/app/AppOverlays";
import { ChatRoomPage } from "./views/ChatRoomPage";
import { ChatListPage } from "./views/ChattingPage";
import { LaunchPage } from "./views/LaunchPage";
import { ListingDetailPage } from "./views/ListingDetailPage";
import { ListingPage } from "./views/ListingPage";
import { LoginPage } from "./views/LoginPage";
import { MyPage } from "./views/MyPage";
import { RegionSelectPage } from "./views/RegionSelectPage";
import { ListingEditorPage } from "./views/SellPage";
import { WelcomePage } from "./views/WelcomePage";

const tabs = [
  { id: "listing", path: "/listing" },
  { id: "trading", path: "/trading" },
  { id: "chatting", path: "/chatting" },
  { id: "mypage", path: "/mypage" }
] as const;

type AppProps = {
  initialSelectedRegion?: {
    region_id: number;
    dongnm: string;
    verified_at: string | null;
    primary?: boolean;
  } | null;
};

function RoutedLayout({
  initialSelectedRegion = null
}: {
  initialSelectedRegion?: AppProps["initialSelectedRegion"];
}) {
  const location = useLocation();
  const navigate = useNavigate();
  const { tradeRailOpen } = useTradeRail();

  const activeTab = location.pathname.startsWith("/profile")
    || location.pathname.startsWith("/mypage")
    ? "mypage"
    : location.pathname.startsWith("/my-listings")
      || location.pathname.startsWith("/wishlist")
      || location.pathname.startsWith("/sales-history")
      || location.pathname.startsWith("/purchase-history")
      || location.pathname.startsWith("/received-reviews")
      ? "listing"
      : tabs.find((tab) => location.pathname.startsWith(tab.path))?.id ?? "listing";

  return (
    <MainLayout
      activeTab={activeTab}
      onChangeTab={(tabId) => {
        const nextTab = tabs.find((tab) => tab.id === tabId);
        if (nextTab) {
          navigate(nextTab.path);
        }
      }}
    >
      <ListingPage tradeRailOpen={tradeRailOpen} initialSelectedRegion={initialSelectedRegion} />
    </MainLayout>
  );
}

function TabLayout({
  activeTab
}: {
  activeTab: "chatting" | "mypage";
}) {
  const navigate = useNavigate();

  return (
    <MainLayout
      activeTab={activeTab}
      onChangeTab={(tabId) => {
        const nextTab = tabs.find((tab) => tab.id === tabId);
        if (nextTab) {
          navigate(nextTab.path);
        }
      }}
    >
      {activeTab === "chatting" ? <ChatListPage /> : <MyPage />}
    </MainLayout>
  );
}

function AppRoutes({ initialSelectedRegion = null }: AppProps) {
  const location = useLocation();
  const state = location.state as { backgroundLocation?: Location } | null;
  const backgroundLocation = state?.backgroundLocation;
  const visibleLocation = backgroundLocation ?? location;

  const renderRoute = (pathname: string) => {
    if (pathname === "/") {
      return <LaunchPage />;
    }

    if (pathname === "/welcome") {
      return <WelcomePage />;
    }

    if (pathname === "/signup/region") {
      return <RegionSelectPage />;
    }

    if (pathname === "/login") {
      return <LoginPage />;
    }

    if (/^\/chatting\/[^/]+/.test(pathname)) {
      return <ChatRoomPage />;
    }

    if (/^\/(listing|trading|my-listings|wishlist|sales-history|purchase-history|received-reviews)\/[^/]+/.test(pathname)) {
      return <ListingDetailPage />;
    }

    if (pathname === "/sell") {
      return <ListingEditorPage />;
    }

    if (pathname === "/chatting") {
      return <TabLayout activeTab="chatting" />;
    }

    if (pathname === "/mypage") {
      return <TabLayout activeTab="mypage" />;
    }

    return <RoutedLayout initialSelectedRegion={initialSelectedRegion} />;
  };

  return (
    <>
      <LocationOverrideProvider location={visibleLocation}>
        {renderRoute(visibleLocation.pathname)}
      </LocationOverrideProvider>
      {backgroundLocation ? (
        <LocationOverrideProvider location={location}>
          {renderRoute(location.pathname)}
        </LocationOverrideProvider>
      ) : null}
      <AppOverlays />
    </>
  );
}

export default function App({ initialSelectedRegion = null }: AppProps) {
  return <AppRoutes initialSelectedRegion={initialSelectedRegion} />;
}
