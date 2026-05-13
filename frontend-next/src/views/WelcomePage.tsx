import { useEffect, useState } from "react";
import { Navigate, useNavigate } from "@/lib/nextRouterCompat";
import { hasAccessToken } from "../lib/auth";

export function WelcomePage() {
  const navigate = useNavigate();
  const [redirectToListing, setRedirectToListing] = useState(false);

  useEffect(() => {
    setRedirectToListing(hasAccessToken());
  }, []);

  if (redirectToListing) {
    return <Navigate to="/listing" replace />;
  }

  return (
    <div className="main-screen welcome-screen">
      <div className="welcome-content">
        <div className="welcome-brand" aria-hidden="true">
          <span className="welcome-brand-mark">T</span>
          <strong>TORA KAZE</strong>
        </div>
        <h1>굿즈 거래를 시작해보세요</h1>
        <p>
          지역 기반으로 안전하게 거래하고
          <br />
          채팅과 교환을 한 화면에서 이어가세요.
        </p>
      </div>

      <div className="welcome-actions">
        <button type="button" className="welcome-primary" onClick={() => navigate("/signup/region")}>
          시작하기
        </button>
        <button type="button" className="welcome-login" onClick={() => navigate("/login?mode=login")}>
          이미 계정이 있나요? <strong>로그인</strong>
        </button>
      </div>
    </div>
  );
}
