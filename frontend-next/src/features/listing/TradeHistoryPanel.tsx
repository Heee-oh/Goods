import { useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api";
import { clearSession } from "@/lib/auth";
import { useNavigate } from "@/lib/nextRouterCompat";
import { getTransactionLabel, type TransactionType } from "@/lib/transactionType";

type TradeHistoryMode = "sales" | "purchases";

type TradeHistoryItem = {
  trade_id: number | string;
  listing_id: number | string;
  listing_image_url: string | null;
  title: string;
  price_amount: number;
  transaction_type: TransactionType;
  partner_id: number | string;
  partner_nickname: string;
  traded_at: string;
  review_written?: boolean;
};

type TradeHistorySlice = {
  content?: TradeHistoryItem[];
};

type TradeHistoryPanelProps = {
  mode: TradeHistoryMode;
};

function formatTradePrice(price: number, transactionType: TransactionType) {
  if (transactionType !== "sell") {
    return getTransactionLabel(transactionType);
  }

  return `${price.toLocaleString("ko-KR")}원`;
}

function formatTradeDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

export function TradeHistoryPanel({ mode }: TradeHistoryPanelProps) {
  const navigate = useNavigate();
  const [items, setItems] = useState<TradeHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let disposed = false;

    const load = async () => {
      try {
        setLoading(true);
        setError("");
        const response = await apiRequest<TradeHistorySlice>(`/api/trades/${mode}?size=20`);
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
          setError(err instanceof Error ? err.message : "거래 기록을 불러오지 못했습니다.");
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
  }, [mode, navigate]);

  if (loading) {
    return <p className="region-status">거래 기록을 불러오는 중...</p>;
  }

  if (error) {
    return <p className="auth-error">{error}</p>;
  }

  if (items.length === 0) {
    return (
      <div className="profile-page-empty">
        {mode === "sales"
          ? "아직 판매 기록이 없어요. 판매 완료된 거래가 생기면 여기에 표시됩니다."
          : "아직 구매 기록이 없어요. 구매 완료된 거래가 생기면 여기에 표시됩니다."}
      </div>
    );
  }

  return (
    <section className="trade-history-list">
      {items.map((item) => (
        <article key={String(item.trade_id)} className="trade-history-item">
          <div className="trade-history-thumb">
            {item.listing_image_url ? <img src={item.listing_image_url} alt={item.title} /> : null}
          </div>
          <div className="trade-history-copy">
            <strong>{item.title}</strong>
            <span>{formatTradePrice(item.price_amount, item.transaction_type)}</span>
            <p>
              {mode === "sales" ? "구매자" : "판매자"} {item.partner_nickname} · {formatTradeDate(item.traded_at)}
            </p>
          </div>
          {mode === "purchases" && !item.review_written ? (
            <button
              type="button"
              className="trade-history-review-button"
              onClick={() => {
                window.dispatchEvent(
                  new CustomEvent("goods:review-prompt", {
                    detail: {
                      trade_id: item.trade_id,
                      partner_nickname: item.partner_nickname,
                      listing_title: item.title,
                      writer_is_seller: false
                    }
                  })
                );
              }}
            >
              리뷰 작성하기
            </button>
          ) : null}
        </article>
      ))}
    </section>
  );
}
