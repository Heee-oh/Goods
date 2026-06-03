import { useEffect, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api";
import { clearSession } from "@/lib/auth";
import { useNavigate } from "@/lib/nextRouterCompat";
import { getTransactionLabel, type TransactionType } from "@/lib/transactionType";

type TradeHistoryMode = "sales" | "purchases";

type RawTradeHistoryItem = {
  trade_id?: number | string;
  tradeId?: number | string;
  listing_id?: number | string;
  listingId?: number | string;
  listing_image_url?: string | null;
  listingImageUrl?: string | null;
  title?: string;
  price_amount?: number | string;
  priceAmount?: number | string;
  transaction_type?: TransactionType;
  transactionType?: TransactionType;
  partner_id?: number | string;
  partnerId?: number | string;
  partner_nickname?: string;
  partnerNickname?: string;
  traded_at?: string;
  tradedAt?: string;
  review_written?: boolean;
  reviewWritten?: boolean;
};

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
  content?: RawTradeHistoryItem[];
};

type TradeHistoryPanelProps = {
  mode: TradeHistoryMode;
};

const REVIEW_SUBMITTED_EVENT = "goods:review-submitted";

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

function normalizeTradeHistoryItem(item: RawTradeHistoryItem): TradeHistoryItem {
  const price = item.price_amount ?? item.priceAmount ?? 0;

  return {
    trade_id: item.trade_id ?? item.tradeId ?? "",
    listing_id: item.listing_id ?? item.listingId ?? "",
    listing_image_url: item.listing_image_url ?? item.listingImageUrl ?? null,
    title: item.title ?? "상품 정보",
    price_amount: typeof price === "number" ? price : Number(price),
    transaction_type: item.transaction_type ?? item.transactionType ?? "sell",
    partner_id: item.partner_id ?? item.partnerId ?? "",
    partner_nickname: item.partner_nickname ?? item.partnerNickname ?? "상대 사용자",
    traded_at: item.traded_at ?? item.tradedAt ?? new Date().toISOString(),
    review_written: Boolean(item.review_written ?? item.reviewWritten)
  };
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
          setItems((response.content ?? []).map(normalizeTradeHistoryItem));
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

  useEffect(() => {
    const handleReviewSubmitted = (event: Event) => {
      const customEvent = event as CustomEvent<{ trade_id?: number | string; tradeId?: number | string }>;
      const tradeId = customEvent.detail?.trade_id ?? customEvent.detail?.tradeId;
      if (tradeId == null) {
        return;
      }

      setItems((current) =>
        current.map((item) =>
          String(item.trade_id) === String(tradeId)
            ? {
                ...item,
                review_written: true
              }
            : item
        )
      );
    };

    window.addEventListener(REVIEW_SUBMITTED_EVENT, handleReviewSubmitted as EventListener);
    return () => {
      window.removeEventListener(REVIEW_SUBMITTED_EVENT, handleReviewSubmitted as EventListener);
    };
  }, []);

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
