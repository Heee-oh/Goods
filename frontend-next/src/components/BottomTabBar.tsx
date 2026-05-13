import { useChatNotifications } from "../lib/chatNotifications";

const tabs = [
  { id: "listing", label: "홈", icon: "home" },
  { id: "chatting", label: "채팅", icon: "chat" },
  { id: "mypage", label: "마이페이지", icon: "grid" }
];

type BottomTabBarProps = {
  activeTab: string;
  onChange: (tabId: string) => void;
};

function TabIcon({ icon }: { icon: string }) {
  if (icon === "home") {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 4.2 4 10v9h5.7v-5.4h4.6V19H20v-9z" />
      </svg>
    );
  }

  if (icon === "chat") {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v7A2.5 2.5 0 0 1 17.5 15H9l-4.5 4v-4.4A2.49 2.49 0 0 1 4 12.5z" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 4h7v7H4zm9 0h7v7h-7zM4 13h7v7H4zm9 0h7v7h-7z" />
    </svg>
  );
}

export function BottomTabBar({ activeTab, onChange }: BottomTabBarProps) {
  const { totalUnreadCount } = useChatNotifications();

  return (
    <nav className="tab-bar">
      {tabs.map((tab) => {
        const badge = tab.id === "chatting" && totalUnreadCount > 0
          ? String(totalUnreadCount > 99 ? "99+" : totalUnreadCount)
          : null;

        return (
          <button
            key={tab.id}
            type="button"
            className={tab.id === activeTab ? "tab-item active" : "tab-item"}
            onClick={() => onChange(tab.id)}
          >
            <span className="tab-icon-wrap">
              <TabIcon icon={tab.icon} />
              {badge ? <em className="tab-badge">{badge}</em> : null}
            </span>
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
