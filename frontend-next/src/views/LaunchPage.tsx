import { useEffect } from "react";
import { useNavigate } from "@/lib/nextRouterCompat";
import { hasAccessToken } from "../lib/auth";

function GoodsMark() {
  return (
    <div className="welcome-brand-mark" aria-hidden="true">
      G
    </div>
  );
}

export function LaunchPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      navigate(hasAccessToken() ? "/listing" : "/welcome", { replace: true });
    }, 1100);

    return () => window.clearTimeout(timeout);
  }, [navigate]);

  return (
    <div className="main-screen launch-screen">
      <GoodsMark />
    </div>
  );
}
