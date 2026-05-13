import { useEffect } from "react";
import { useNavigate } from "@/lib/nextRouterCompat";
import { hasAccessToken } from "../lib/auth";

function DaangnMark() {
  return (
    <div className="daangn-mark" aria-hidden="true">
      <div className="daangn-leaf" />
      <div className="daangn-body">
        <div className="daangn-hole" />
      </div>
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
      <DaangnMark />
    </div>
  );
}
