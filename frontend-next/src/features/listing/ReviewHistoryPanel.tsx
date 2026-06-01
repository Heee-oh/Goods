import { useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api";
import { clearSession } from "@/lib/auth";
import { useLocation, useNavigate } from "@/lib/nextRouterCompat";

type ReviewHistoryItem = {
  review_id: number | string;
  comment: string | null;
  rating: number;
  listing_id: number | string;
  listing_title?: string | null;
  listing_image_url: string | null;
};

type ReviewHistorySlice = {
  content?: ReviewHistoryItem[];
};

function formatRating(rating: number) {
  return `${rating}점`;
}

export function ReviewHistoryPanel() {
  const navigate = useNavigate();
  const location = useLocation();
  const [items, setItems] = useState<ReviewHistoryItem[]>([]);
  const [selectedReviewId, setSelectedReviewId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let disposed = false;

    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const response = await apiRequest<ReviewHistorySlice>("/api/reviews");
        if (!disposed) {
          setItems(response.content ?? []);
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          clearSession();
          navigate("/welcome", { replace: true });
          return;
        }

        if (!disposed) {
          setError(err instanceof Error ? err.message : "받은 리뷰를 불러오지 못했습니다.");
        }
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      disposed = true;
    };
  }, [navigate]);

  if (loading) {
    return <p className="region-status">받은 리뷰를 불러오는 중...</p>;
  }

  if (error) {
    return <p className="auth-error">{error}</p>;
  }

  if (items.length === 0) {
    return <div className="profile-page-empty">아직 받은 리뷰가 없어요. 거래 후 상대방이 리뷰를 남기면 여기에 표시됩니다.</div>;
  }

  const selectedItem = items.find((item) => String(item.review_id) === selectedReviewId) ?? null;

  return (
    <section className="trade-history-list">
      {items.map((item) => (
        <article key={String(item.review_id)} className="trade-history-item review-history-item">
          <button
            type="button"
            className="review-history-summary"
            onClick={() =>
              setSelectedReviewId((current) =>
                current === String(item.review_id) ? null : String(item.review_id)
              )
            }
          >
            <div className="trade-history-thumb">
              {item.listing_image_url ? <img src={item.listing_image_url} alt="" /> : null}
            </div>
            <div className="trade-history-copy">
              <strong>{formatRating(item.rating)}</strong>
              <span>{item.listing_title?.trim() || `거래 상품 #${item.listing_id}`}</span>
              <p>{item.comment?.trim() || "작성된 코멘트가 없습니다."}</p>
            </div>
          </button>
          {selectedItem && String(selectedItem.review_id) === String(item.review_id) ? (
            <div className="review-history-detail">
              <p>{selectedItem.comment?.trim() || "작성된 코멘트가 없습니다."}</p>
              <button
                type="button"
                className="trade-history-review-button"
                onClick={() => {
                  navigate(`/received-reviews/${selectedItem.listing_id}`, {
                    state: { backgroundLocation: location }
                  });
                }}
              >
                판매글 보기
              </button>
            </div>
          ) : null}
        </article>
      ))}
    </section>
  );
}
