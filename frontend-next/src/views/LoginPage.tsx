import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "@/lib/nextRouterCompat";
import { ApiError, apiRequest } from "../lib/api";
import { getSignupRegion, saveSession } from "../lib/auth";

type AuthResponse = {
  member_id: string;
  access_token: string;
  expires_in: number;
};

function normalizePhone(value: string) {
  return value.replace(/\D/g, "");
}

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get("mode") === "signup" ? "signup" : "login";
  const [phone, setPhone] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [signupRegion, setSignupRegion] = useState<{ regionId: number | null; regionName: string | null }>({
    regionId: null,
    regionName: null
  });

  const normalizedPhone = useMemo(() => normalizePhone(phone), [phone]);
  const isValidPhone = normalizedPhone.length >= 10;

  useEffect(() => {
    setSignupRegion(getSignupRegion());
  }, []);

  const handleSubmit = async () => {
    if (!isValidPhone || submitting) {
      return;
    }

    try {
      setSubmitting(true);
      setError("");

      const response =
        mode === "signup"
          ? await apiRequest<AuthResponse>("/api/auth/signup", {
              method: "POST",
              auth: false,
              body: JSON.stringify({
                phone_number: normalizedPhone,
                region_id: signupRegion.regionId
              })
            })
          : await apiRequest<AuthResponse>("/api/auth/login", {
              method: "POST",
              auth: false,
              body: JSON.stringify({
                phone_number: normalizedPhone
              })
            });

      saveSession({
        accessToken: response.access_token,
        memberId: response.member_id,
        expiresIn: response.expires_in
      });

      navigate("/listing", { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("요청에 실패했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="main-screen auth-screen">
      <div className="auth-topbar">
        <button type="button" className="back-button" onClick={() => navigate(-1)}>
          ‹
        </button>
      </div>

      <div className="auth-body">
        <h1>휴대폰 번호를 입력해주세요.</h1>

        <div className="phone-field">
          <span className="phone-country">🇰🇷 +82</span>
          <input
            type="tel"
            inputMode="numeric"
            placeholder="000 0000 0000"
            value={phone}
            onChange={(event) => {
              setPhone(event.target.value);
              setError("");
            }}
          />
        </div>

        {mode === "login" ? (
          <p className="auth-help">
            휴대폰 번호가 변경되었나요? <u>내 계정찾기</u>
          </p>
        ) : (
          <p className="auth-help">
            선택한 동네:{" "}
            <strong>{signupRegion.regionName ?? "-"}</strong>
          </p>
        )}

        {error ? <p className="auth-error">{error}</p> : null}
      </div>

      <div className="auth-footer">
        <button
          type="button"
          className="auth-confirm"
          disabled={!isValidPhone || submitting}
          onClick={handleSubmit}
        >
          {submitting ? "처리 중..." : "확인"}
        </button>
      </div>
    </div>
  );
}
