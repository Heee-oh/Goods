import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate, type Location } from "react-router-dom";
import { MainLayout } from "./components/MainLayout";
import { ApiError, apiRequest } from "./lib/api";
import { hasAccessToken } from "./lib/auth";
import { ChatNotificationsProvider, useChatNotifications } from "./lib/chatNotifications";
import { ChatRoomPage } from "./pages/ChatRoomPage";
import { ChatListPage } from "./pages/ChattingPage";
import { LaunchPage } from "./pages/LaunchPage";
import { ListingDetailPage } from "./pages/ListingDetailPage";
import { ListingPage } from "./pages/ListingPage";
import { LoginPage } from "./pages/LoginPage";
import { MyPage } from "./pages/MyPage";
import { RegionSelectPage } from "./pages/RegionSelectPage";
import { ListingEditorPage } from "./pages/SellPage";
import { WelcomePage } from "./pages/WelcomePage";

type ReviewPrompt = {
  partner_nickname: string;
  listing_title: string;
};

type TradeCompletionPrompt = {
  appointment_id: number | string;
  listing_id: number | string;
  chat_room_id: number | string;
  buyer_id: number | string;
  partner_nickname: string;
};

const REVIEW_PROMPT_EVENT = "goods:review-prompt";

const tabs = [
  { id: "listing", path: "/listing" },
  { id: "trading", path: "/trading" },
  { id: "chatting", path: "/chatting" },
  { id: "mypage", path: "/mypage" }
] as const;

function ChatToastViewport() {
  const navigate = useNavigate();
  const { toast, dismissToast, markRoomRead } = useChatNotifications();

  if (!toast) {
    return null;
  }

  return (
    <button
      type="button"
      className="chat-toast"
      onClick={() => {
        markRoomRead(toast.roomId);
        dismissToast();
        navigate(`/chatting/${toast.roomId}`);
      }}
    >
      <span className="chat-toast-icon" aria-hidden="true">
        梨꾪똿
      </span>
      <span className="chat-toast-copy">
        <strong>{toast.partnerNickname}</strong>
        <span>{toast.content}</span>
      </span>
      <span className="chat-toast-close" aria-hidden="true">
        ?リ린
      </span>
    </button>
  );
}

function AppointmentReminderViewport() {
  const navigate = useNavigate();
  const { appointmentReminder, dismissAppointmentReminder } = useChatNotifications();

  if (!appointmentReminder) {
    return null;
  }

  const reminderTime = appointmentReminder.meetAt
    ? new Date(appointmentReminder.meetAt).toLocaleString("ko-KR", {
        month: "long",
        day: "numeric",
        weekday: "short",
        hour: "numeric",
        minute: "2-digit"
      })
    : "";

  const handleOpenChat = () => {
    if (appointmentReminder.chatRoomId) {
      navigate(`/chatting/${appointmentReminder.chatRoomId}`);
    }
    dismissAppointmentReminder();
  };

  return (
    <div className="trade-prompt-overlay" onClick={dismissAppointmentReminder}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{appointmentReminder.partnerNickname}</strong>님과의 약속 시간이 다가옵니다.
        </p>
        {reminderTime ? <p>{reminderTime}에 예정된 약속입니다.</p> : null}
        <button type="button" className="chat-confirm-primary" onClick={handleOpenChat}>
          {appointmentReminder.chatRoomId ? "채팅방 열기" : "확인"}
        </button>
        <button type="button" className="chat-confirm-secondary" onClick={dismissAppointmentReminder}>
          닫기
        </button>
      </div>
    </div>
  );
}

function ReviewPromptViewport() {
  const [prompt, setPrompt] = useState<ReviewPrompt | null>(null);

  useEffect(() => {
    const handleReviewPrompt = (event: Event) => {
      const customEvent = event as CustomEvent<ReviewPrompt>;
      if (!customEvent.detail?.partner_nickname || !customEvent.detail?.listing_title) {
        return;
      }

      setPrompt(customEvent.detail);
    };

    window.addEventListener(REVIEW_PROMPT_EVENT, handleReviewPrompt as EventListener);

    return () => {
      window.removeEventListener(REVIEW_PROMPT_EVENT, handleReviewPrompt as EventListener);
    };
  }, []);

  if (!prompt) {
    return null;
  }

  const dismissPrompt = () => {
    setPrompt(null);
  };

  return (
    <div className="trade-prompt-overlay" onClick={dismissPrompt}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{prompt.partner_nickname}</strong>님과의 거래가 완료되었습니다.
        </p>
        <p>{prompt.listing_title}에 리뷰를 남겨주세요.</p>
        <button type="button" className="chat-confirm-primary" onClick={dismissPrompt}>
          리뷰 남기기
        </button>
        <button type="button" className="chat-confirm-secondary" onClick={dismissPrompt}>
          나중에
        </button>
      </div>
    </div>
  );
}

function TradeCompletionPromptViewport() {
  const [prompt, setPrompt] = useState<TradeCompletionPrompt | null>(null);
  const [busy, setBusy] = useState(false);
  const [snoozedPrompt, setSnoozedPrompt] = useState<{ appointmentId: string; until: number } | null>(null);

  useEffect(() => {
    let disposed = false;

    const loadPrompt = async () => {
      if (!hasAccessToken() || busy) {
        return;
      }

      try {
        const response = await apiRequest<TradeCompletionPrompt | null>("/api/appointments/trade-prompt");
        if (disposed || !response) {
          return;
        }

        const appointmentId = String(response.appointment_id);
        if (snoozedPrompt?.appointmentId === appointmentId && snoozedPrompt.until > Date.now()) {
          return;
        }

        setPrompt(response);
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          setPrompt(null);
        }
      }
    };

    void loadPrompt();
    const intervalId = window.setInterval(() => void loadPrompt(), 60_000);

    return () => {
      disposed = true;
      window.clearInterval(intervalId);
    };
  }, [busy, snoozedPrompt]);

  if (!prompt) {
    return null;
  }

  const handleComplete = async () => {
    try {
      setBusy(true);
      await apiRequest(`/api/listings/${prompt.listing_id}/sold-out`, {
        method: "POST",
        body: JSON.stringify({
          buyer_id: String(prompt.buyer_id)
        })
      });
      setPrompt(null);
    } finally {
      setBusy(false);
    }
  };

  const handleLater = () => {
    setSnoozedPrompt({
      appointmentId: String(prompt.appointment_id),
      until: Date.now() + 5 * 60_000
    });
    setPrompt(null);
  };

  return (
    <div className="trade-prompt-overlay" onClick={handleLater}>
      <div className="trade-prompt-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{prompt.partner_nickname}</strong>님과의 거래를 완료했나요?
        </p>
        <p>
          완료하면 게시글이 판매완료로 변경되고<br />
          구매자에게 리뷰 요청 알림이 전송됩니다.
        </p>
        <button type="button" className="chat-confirm-primary" disabled={busy} onClick={() => void handleComplete()}>
          {busy ? "처리 중..." : "거래 완료"}
        </button>
        <button type="button" className="chat-confirm-secondary" disabled={busy} onClick={handleLater}>
          나중에
        </button>
      </div>
    </div>
  );
}

function RoutedLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const activeTab = location.pathname.startsWith("/profile")
    || location.pathname.startsWith("/mypage")
    ? "mypage"
    : location.pathname.startsWith("/my-listings")
      || location.pathname.startsWith("/wishlist")
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
      <ListingPage />
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

function AppRoutes() {
  const location = useLocation();
  const state = location.state as { backgroundLocation?: Location } | null;
  const backgroundLocation = state?.backgroundLocation;

  return (
    <>
      <Routes location={backgroundLocation ?? location}>
        <Route path="/" element={<LaunchPage />} />
        <Route path="/welcome" element={<WelcomePage />} />
        <Route path="/signup/region" element={<RegionSelectPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/chatting/:chatRoomId" element={<ChatRoomPage />} />
        <Route path="/listing/:listingId" element={<ListingDetailPage />} />
        <Route path="/trading/:listingId" element={<ListingDetailPage />} />
        <Route path="/my-listings/:listingId" element={<ListingDetailPage />} />
        <Route path="/wishlist/:listingId" element={<ListingDetailPage />} />
        <Route path="/sell" element={<ListingEditorPage />} />
        <Route path="/chatting" element={<TabLayout activeTab="chatting" />} />
        <Route path="/mypage" element={<TabLayout activeTab="mypage" />} />
        <Route path="/*" element={<RoutedLayout />} />
      </Routes>
      {backgroundLocation ? (
        <Routes>
          <Route path="/listing/:listingId" element={<ListingDetailPage />} />
          <Route path="/trading/:listingId" element={<ListingDetailPage />} />
          <Route path="/my-listings/:listingId" element={<ListingDetailPage />} />
          <Route path="/wishlist/:listingId" element={<ListingDetailPage />} />
          <Route path="/sell" element={<ListingEditorPage />} />
        </Routes>
      ) : null}
      <ChatToastViewport />
      <AppointmentReminderViewport />
      <TradeCompletionPromptViewport />
      <ReviewPromptViewport />
    </>
  );
}

export default function App() {
  return (
    <ChatNotificationsProvider>
      <AppRoutes />
    </ChatNotificationsProvider>
  );
}
