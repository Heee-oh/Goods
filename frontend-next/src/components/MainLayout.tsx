import type { PropsWithChildren } from "react";
import { BottomTabBar } from "./BottomTabBar";

type MainLayoutProps = PropsWithChildren<{
  activeTab: string;
  onChangeTab: (tabId: string) => void;
}>;

export function MainLayout({ activeTab, onChangeTab, children }: MainLayoutProps) {
  return (
    <>
      <main className="screen-content">{children}</main>
      <BottomTabBar activeTab={activeTab} onChange={onChangeTab} />
    </>
  );
}
