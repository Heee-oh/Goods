import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiRequest, ApiError } from "../lib/api";
import { saveSignupRegion } from "../lib/auth";

type RegionItem = {
  region_id: number;
  full_name: string;
  dongnm: string;
};

export function RegionSelectPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [regions, setRegions] = useState<RegionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const timeout = window.setTimeout(async () => {
      try {
        setLoading(true);
        setError("");
        const data = await apiRequest<RegionItem[]>(
          `/api/regions/search?query=${encodeURIComponent(query)}`,
          { auth: false }
        );
        setRegions(data);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError("동네 목록을 불러오지 못했습니다.");
        }
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [query]);

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError("현재 위치를 사용할 수 없습니다.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          setLoading(true);
          setError("");
          const data = await apiRequest<RegionItem[]>(
            `/api/regions/nearby?lat=${position.coords.latitude}&lng=${position.coords.longitude}`,
            { auth: false }
          );
          setRegions(data);
        } catch (err) {
          setError(
            err instanceof Error
              ? err.message
              : "현재 위치 동네를 불러오지 못했습니다."
          );
        } finally {
          setLoading(false);
        }
      },
      () => setError("위치 권한이 필요합니다.")
    );
  };

  const handleSelectRegion = (region: RegionItem) => {
    saveSignupRegion(region.region_id, region.dongnm);
    navigate("/login?mode=signup");
  };

  return (
    <div className="main-screen region-screen">
      <div className="auth-topbar">
        <button type="button" className="back-button" onClick={() => navigate(-1)}>
          ‹
        </button>
        <input
          className="region-search"
          type="text"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="동명(읍, 면)으로 검색 (ex. 서초동)"
        />
      </div>

      <div className="region-body">
        <button
          type="button"
          className="region-current-button"
          onClick={handleUseCurrentLocation}
        >
          ⊕ 현재위치로 찾기
        </button>

        <h2>근처 동네</h2>

        {error ? <p className="auth-error">{error}</p> : null}
        {loading ? <p className="region-status">불러오는 중...</p> : null}

        <div className="region-list">
          {regions.map((region) => (
            <button
              key={region.region_id}
              type="button"
              className="region-item"
              onClick={() => handleSelectRegion(region)}
            >
              {region.full_name}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
