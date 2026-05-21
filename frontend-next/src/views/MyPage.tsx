import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession } from "../lib/auth";

const quickItems = [
  { icon: "♡", label: "관심목록" },
  { icon: "◷", label: "최근 본 글" },
  { icon: "✉", label: "혜택" }
];

const tradeItems = ["판매관리", "구매내역", "관심목록", "모아보기"];

type RawMemberResponse = {
  nickname?: string;
  profile_image?: string | null;
  ProfileImage?: string | null;
  smile_score?: number;
  smileScore?: number;
};

function formatSmileScore(value: number | null | undefined) {
  const score = Number(value ?? 100);
  return `${(score / 10).toFixed(1)}점`;
}

export function MyPage() {
  const navigate = useNavigate();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [nickname, setNickname] = useState("굿즈 사용자");
  const [profileImage, setProfileImage] = useState<string | null>(null);
  const [smileScore, setSmileScore] = useState<number>(100);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        setError("");
        const response = await apiRequest<RawMemberResponse>("/api/members/me");
        setNickname(response.nickname?.trim() || "굿즈 사용자");
        setProfileImage(response.profile_image ?? response.ProfileImage ?? null);
        setSmileScore(response.smile_score ?? response.smileScore ?? 100);
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          clearSession();
          navigate("/welcome", { replace: true });
          return;
        }

        setError(err instanceof Error ? err.message : "내 정보를 불러오지 못했어요.");
      }
    };

    void load();
  }, [navigate]);

  const formattedSmileScore = useMemo(() => formatSmileScore(smileScore), [smileScore]);

  const handleLogout = () => {
    clearSession();
    navigate("/welcome", { replace: true });
  };

  return (
    <div className="page page-mypage page-my-profile">
      <header className="my-profile-header">
        <h1>마이페이지</h1>
        <button
          type="button"
          className="my-profile-settings"
          aria-label="설정"
          onClick={() => setSettingsOpen((open) => !open)}
        >
          ⚙
        </button>
      </header>

      {settingsOpen ? (
        <section className="settings-panel">
          <button type="button" className="logout-button" onClick={handleLogout}>
            로그아웃
          </button>
        </section>
      ) : null}

      {error ? <p className="auth-error">{error}</p> : null}

      <section className="my-card my-profile-card">
        <div className="my-profile-main">
          {profileImage ? <img className="my-profile-avatar real-image" src={profileImage} alt={nickname} /> : <div className="my-profile-avatar" />}
          <div className="my-profile-copy">
            <strong>{nickname}</strong>
            <div className="my-score-badge">{formattedSmileScore}</div>
          </div>
          <span className="my-chevron">›</span>
        </div>
      </section>

      <section className="my-card my-banner-card">
        <div className="my-banner-copy">
          <strong>4월까지 20만원 쓰면 17만원 캐시백!</strong>
          <span>이벤트 자세히 보기</span>
        </div>
        <button type="button" className="my-banner-close" aria-label="닫기">
          ×
        </button>
      </section>

      <section className="my-card my-quick-card">
        {quickItems.map((item) => (
          <button type="button" key={item.label} className="my-quick-item">
            <span className="my-quick-icon" aria-hidden="true">
              {item.icon}
            </span>
            <span>{item.label}</span>
          </button>
        ))}
      </section>

      <section className="my-card my-trade-card">
        <div className="my-section-title-row">
          <h2>나의 거래</h2>
        </div>
        <div className="my-trade-list">
          {tradeItems.map((item) => (
            <button type="button" key={item} className="my-trade-item">
              <span>{item}</span>
              <span className="my-chevron">›</span>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
