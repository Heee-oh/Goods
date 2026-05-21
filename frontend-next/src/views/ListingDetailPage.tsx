import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "@/lib/nextRouterCompat";
import { Pagination } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import { ApiError, apiRequest } from "../lib/api";
import { clearSession, getMemberId, getSelectedRegionId } from "../lib/auth";
import { getListingStatusLabel } from "../lib/listingStatus";
import { getTransactionLabel, type TransactionType } from "../lib/transactionType";
import "swiper/css";
import "swiper/css/pagination";

type ListingImage = {
  image_id: number;
  image_url: string;
  sort_order: number;
};

type RawListingDetail = {
  listing_id: number;
  seller_id: string | number;
  buyer_id: string | number | null;
  reserver_id: string | number | null;
  seller_nickname: string;
  seller_profile_image: string | null;
  seller_smile_score: number;
  title: string;
  description: string;
  category_id: number | null;
  price_amount: number | null;
  transaction_type: TransactionType;
  interested: boolean;
  status: "DRAFT" | "PUBLISHED" | "RESERVED" | "SOLD_OUT";
  region_name: string | null;
  chat_count: number;
  hope_region_id: number | null;
  hope_lat: number | null;
  hope_lng: number | null;
  distance_km?: number | null;
  distanceKm?: number | null;
  view_count: number;
  images: ListingImage[];
  updated_at: string;
};

type ListingDetail = Omit<RawListingDetail, "seller_id" | "buyer_id" | "reserver_id"> & {
  seller_id: string;
  buyer_id: string | null;
  reserver_id: string | null;
};

type ChatRoomCreateResponse = {
  chat_room_id?: number | string;
  chatRoomId?: number | string;
};

function formatPrice(amount: number | null, transactionType: TransactionType) {
  if (transactionType === "free") {
    return "\uB098\uB214";
  }

  if (transactionType === "trade") {
    return "\uAD50\uD658";
  }

  return `${(amount ?? 0).toLocaleString("ko-KR")}\uC6D0`;
}

function formatSmileScore(value: number | null | undefined) {
  const score = Number(value ?? 100);
  return `${(score / 10).toFixed(1)}\uC810`;
}

function formatUpdatedAt(value: string) {
  const updatedAt = new Date(value);
  const diffMinutes = Math.max(1, Math.floor((Date.now() - updatedAt.getTime()) / 60000));

  if (diffMinutes < 60) {
    return `${diffMinutes}\uBD84 \uC804`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}\uC2DC\uAC04 \uC804`;
  }

  return `${Math.floor(diffHours / 24)}\uC77C \uC804`;
}

function normalizeId(value: string | number | null | undefined) {
  if (value == null) {
    return null;
  }

  return String(value);
}

function normalizeListingDetail(detail: RawListingDetail): ListingDetail {
  return {
    ...detail,
    seller_id: normalizeId(detail.seller_id) ?? "",
    buyer_id: normalizeId(detail.buyer_id),
    reserver_id: normalizeId(detail.reserver_id)
  };
}

function ListingImageCarousel({ images, title }: { images: ListingImage[]; title: string }) {
  const sortedImages = [...images].sort((a, b) => a.sort_order - b.sort_order);

  if (sortedImages.length === 0) {
    return <div className="listing-detail-image listing-detail-image-empty" />;
  }

  return (
    <Swiper
      className="listing-detail-swiper"
      modules={[Pagination]}
      pagination={{ clickable: true }}
      centeredSlides
      slidesPerView={1}
      spaceBetween={0}
    >
      {sortedImages.map((image) => (
        <SwiperSlide key={image.image_id}>
          <img className="listing-detail-image" src={image.image_url} alt={title} />
        </SwiperSlide>
      ))}
    </Swiper>
  );
}

export function ListingDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { listingId } = useParams();
  const [detail, setDetail] = useState<ListingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creatingChat, setCreatingChat] = useState(false);
  const [interested, setInterested] = useState(false);
  const [interestLoading, setInterestLoading] = useState(false);
  const [statusSheetOpen, setStatusSheetOpen] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [closing, setClosing] = useState(false);
  const closeTimerRef = useRef<number | null>(null);

  const loadDetail = useCallback(async () => {
    if (!listingId) {
      navigate("/listing", { replace: true });
      return;
    }

    try {
      setLoading(true);
      setError("");

      const memberId = getMemberId();
      const selectedRegionId = getSelectedRegionId();
      const listingResponse = await apiRequest<RawListingDetail>(
        `/api/listings/${listingId}${selectedRegionId ? `?region_id=${selectedRegionId}` : ""}`
      );
      const normalized = normalizeListingDetail(listingResponse);
      setDetail(normalized);
      setInterested(memberId !== normalized.seller_id && Boolean(normalized.interested));
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigate("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "Failed to load listing detail.");
    } finally {
      setLoading(false);
    }
  }, [listingId, navigate]);

  useEffect(() => {
    void loadDetail();
  }, [loadDetail]);

  useEffect(() => {
    return () => {
      if (closeTimerRef.current !== null) {
        window.clearTimeout(closeTimerRef.current);
      }
    };
  }, []);

  const currentMemberId = getMemberId();
  const canChat = detail ? currentMemberId !== detail.seller_id : false;
  const isOwner = detail ? currentMemberId === detail.seller_id : false;
  const canToggleInterest = detail ? currentMemberId !== detail.seller_id : false;
  const showHopeLocation = Boolean(detail?.hope_region_id && detail?.hope_lat && detail?.hope_lng);
  const sellerNickname = detail?.seller_nickname?.trim() || "Seller";
  const sellerRegionName = detail?.region_name?.trim() || "\uC9C0\uC5ED \uC815\uBCF4 \uC5C6\uC74C";
  const listingTransactionType = detail?.transaction_type ?? "sell";

  const isModal = Boolean((location.state as { backgroundLocation?: unknown } | null)?.backgroundLocation);
  const sellerDisplayName = sellerNickname.replace(/_/g, " ");
  const showDetailStatusBadge = detail ? detail.status === "PUBLISHED" || detail.status === "RESERVED" || detail.status === "SOLD_OUT" : false;
  const modalHeroTag = detail ? getListingStatusLabel(detail.status, listingTransactionType) : "";
  const modalHeroTone = detail?.status === "RESERVED" ? "reserved" : detail?.status === "SOLD_OUT" ? "soldout" : "active";
  const modalConditionTag = detail
    ? listingTransactionType === "free"
      ? "Free / Unopened"
      : listingTransactionType === "trade"
        ? "WTT / Wanted"
        : detail.status === "PUBLISHED"
          ? "Mint / Opened"
          : "Local Meetup"
    : "";

  const handleClose = useCallback(() => {
    if (closing) {
      return;
    }

    setClosing(true);
    closeTimerRef.current = window.setTimeout(() => {
      navigate(-1);
    }, 160);
  }, [closing, navigate]);

  const handleChat = async () => {
    if (!detail || !canChat) {
      return;
    }

    try {
      setCreatingChat(true);

      if (!currentMemberId) {
        navigate("/welcome");
        return;
      }

      const response = await apiRequest<ChatRoomCreateResponse>("/api/chat-rooms", {
        method: "POST",
        body: JSON.stringify({
          listing_id: detail.listing_id
        })
      });

      const chatRoomId = response.chat_room_id ?? response.chatRoomId;
      if (!chatRoomId) {
        throw new Error("Missing chat room id.");
      }

      window.dispatchEvent(
        new CustomEvent("goods:open-chat-room", {
          detail: {
            chatRoomId: String(chatRoomId),
            partnerNickname: sellerNickname
          }
        })
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to open chat room.");
    } finally {
      setCreatingChat(false);
    }
  };

  const handleInterest = async () => {
    if (!detail || !listingId || interestLoading || !canToggleInterest) {
      return;
    }

    if (!currentMemberId) {
      navigate("/welcome");
      return;
    }

    try {
      setInterestLoading(true);
      setError("");

      if (interested) {
        await apiRequest(`/api/members/me/interests/${listingId}`, {
          method: "DELETE"
        });
        setInterested(false);
        return;
      }

      await apiRequest(`/api/members/me/interests/${listingId}`, {
        method: "PUT"
      });
      setInterested(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigate("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "Failed to update interest.");
    } finally {
      setInterestLoading(false);
    }
  };

  const handleCancelReserve = async () => {
    if (!detail || !listingId || !isOwner || statusUpdating) {
      return;
    }

    try {
      setStatusUpdating(true);
      setError("");
      await apiRequest(`/api/listings/${listingId}/reserve/cancel`, {
        method: "POST"
      });
      setDetail({
        ...detail,
        status: "PUBLISHED",
        reserver_id: null
      });
      setStatusSheetOpen(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigate("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "\uC608\uC57D \uCDE8\uC18C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleMarkSoldOut = async () => {
    if (!detail || !listingId || !isOwner || statusUpdating) {
      return;
    }

    const buyerId = detail.reserver_id ?? detail.buyer_id;
    if (!buyerId) {
      setError("\uC608\uC57D\uB41C \uC0C1\uB300\uAC00 \uC5C6\uC5B4 \uD310\uB9E4\uC644\uB8CC\uB85C \uBCC0\uACBD\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
      return;
    }

    try {
      setStatusUpdating(true);
      setError("");
      await apiRequest(`/api/listings/${listingId}/sold-out`, {
        method: "POST",
        body: JSON.stringify({
          buyer_id: buyerId
        })
      });
      setDetail({
        ...detail,
        status: "SOLD_OUT",
        buyer_id: buyerId
      });
      setStatusSheetOpen(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        clearSession();
        navigate("/welcome", { replace: true });
        return;
      }

      setError(err instanceof Error ? err.message : "\uD310\uB9E4\uC644\uB8CC \uBCC0\uACBD\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
    } finally {
      setStatusUpdating(false);
    }
  };

  const statusSheet = detail && statusSheetOpen ? (
    <div className="listing-status-sheet" role="dialog" aria-label="\uD310\uB9E4\uAE00 \uC0C1\uD0DC \uBCC0\uACBD">
      {detail.status === "PUBLISHED" ? (
        <button type="button" className="listing-status-option" disabled>
          {"\uC608\uC57D\uC911\uC740 \uCC44\uD305\uBC29\uC5D0\uC11C \uC0C1\uB300\uB97C \uC120\uD0DD\uD574 \uBCC0\uACBD\uD574\uC8FC\uC138\uC694"}
        </button>
      ) : null}
      {detail.status === "RESERVED" ? (
        <>
          <button
            type="button"
            className="listing-status-option active"
            disabled={statusUpdating}
            onClick={() => void handleMarkSoldOut()}
          >
            {statusUpdating ? "\uBCC0\uACBD \uC911..." : `${getTransactionLabel(listingTransactionType)}\uC644\uB8CC\uB85C \uBCC0\uACBD`}
          </button>
          <button
            type="button"
            className="listing-status-option"
            disabled={statusUpdating}
            onClick={() => void handleCancelReserve()}
          >
            {"\uC608\uC57D \uCDE8\uC18C"}
          </button>
        </>
      ) : null}
      {detail.status === "SOLD_OUT" ? (
        <button type="button" className="listing-status-option" disabled>
          {`${getTransactionLabel(listingTransactionType)}\uC644\uB8CC \uC0C1\uD0DC\uC785\uB2C8\uB2E4`}
        </button>
      ) : null}
      <button type="button" className="listing-status-close" disabled={statusUpdating} onClick={() => setStatusSheetOpen(false)}>
        {"\uB2EB\uAE30"}
      </button>
    </div>
  ) : null;

  if (isModal) {
    return (
      <div className={closing ? "listing-detail-modal-overlay is-closing" : "listing-detail-modal-overlay"}>
        <button type="button" className="listing-detail-modal-backdrop" onClick={() => void handleClose()} />
        <div className="listing-detail-modal-shell">
          <div className="listing-detail-modal-screen">
            <section className="listing-detail-modal-hero">
              <div className="listing-detail-hero-media">
                {detail ? <ListingImageCarousel images={detail.images} title={detail.title} /> : <div className="listing-detail-image listing-detail-image-empty" />}
              </div>

              <div className="listing-detail-topbar listing-detail-topbar-modal">
                <button type="button" onClick={() => void handleClose()} aria-label="back">
                  {"\u2190"}
                </button>
                <div className="listing-detail-topbar-spacer" />
                <button
                  type="button"
                  onClick={() => void handleInterest()}
                  aria-label={interested ? "remove interest" : "add interest"}
                  aria-pressed={interested}
                  disabled={!canToggleInterest || interestLoading}
                  className={interested ? "listing-detail-heart-toggle active" : "listing-detail-heart-toggle"}
                >
                  {interested ? "\u2665" : "\u2661"}
                </button>
              </div>

            </section>

            <div className="listing-detail-modal-badges">
              {showDetailStatusBadge ? (
                <span className={`listing-detail-status-chip modal ${modalHeroTone}`}>{modalHeroTag}</span>
              ) : null}
              <span className="listing-detail-status-chip modal muted">{modalConditionTag}</span>
            </div>

            <section className="listing-detail-modal-card">
              {detail ? (
                <>
                  <h1>{detail.title}</h1>
                  {listingTransactionType === "sell" ? (
                    <strong className="listing-detail-modal-price">
                      {formatPrice(detail.price_amount, listingTransactionType)}
                    </strong>
                  ) : (
                    <strong className="listing-detail-modal-price listing-detail-modal-price-free">
                      {getTransactionLabel(listingTransactionType)}
                    </strong>
                  )}

                  <div className="listing-detail-description modal">
                    {detail.description.split("\n").map((line, index) => (
                      <p key={`${index}-${line}`}>{line}</p>
                    ))}
                  </div>

                  <div className="listing-detail-seller-row">
                    <div className="listing-detail-seller-left">
                      {detail.seller_profile_image ? (
                        <img src={detail.seller_profile_image} alt={sellerNickname} />
                      ) : (
                        <div className="listing-detail-seller-avatar modal">
                          {sellerDisplayName.slice(0, 1).toUpperCase()}
                        </div>
                      )}
                      <div className="listing-detail-seller-info">
                        <strong>{sellerDisplayName}</strong>
                        <span>
                          {"\u2605"} {formatSmileScore(detail.seller_smile_score)} ({detail.chat_count} Trades)
                        </span>
                      </div>
                    </div>
                    <span className="listing-detail-seller-local">{detail.region_name ? "LOCAL" : "ONLINE"}</span>
                  </div>
<div className="listing-detail-modal-actions">
                    <button
                      type="button"
                      className="listing-detail-modal-message"
                      disabled={!canChat || creatingChat}
                      onClick={() => void handleChat()}
                    >
                      {canChat ? "\uBA54\uC2DC\uC9C0" : "\uB0B4 \uAC8C\uC2DC\uAE00"}
                    </button>
                    <button
                      type="button"
                      className="listing-detail-modal-trade"
                      disabled={isOwner ? statusUpdating : !canChat || creatingChat}
                      onClick={() => {
                        if (isOwner) {
                          setStatusSheetOpen(true);
                          return;
                        }
                        void handleChat();
                      }}
                    >
                      {isOwner ? "\uC0C1\uD0DC \uBCC0\uACBD" : "\uAD50\uD658 \uC81C\uC548"}
                    </button>
                  </div>
                </>
              ) : (
                <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p>
              )}
            </section>
            {statusSheet}
          </div>
        </div>
      </div>
    );
  }

  const content = (
    <div className="main-screen listing-detail-screen">
      {loading ? <p className="region-status">{"\uBD88\uB7EC\uC624\uB294 \uC911.."}</p> : null}
      {error ? <p className="auth-error">{error}</p> : null}

      {detail ? (
        <>
          <section className="listing-detail-hero">
            <div className="listing-detail-hero-media">
              <ListingImageCarousel images={detail.images} title={detail.title} />
            </div>

            <div className="listing-detail-topbar">
              <button type="button" onClick={() => void handleClose()} aria-label="back">
                {"\u2190"}
              </button>
              <button type="button" onClick={() => navigate("/listing")} aria-label="home">
                {"\u2302"}
              </button>
              <div className="listing-detail-topbar-spacer" />
              <button type="button" aria-label="share">
                {"\u2197"}
              </button>
              <button type="button" aria-label="more">
                {"\u22EF"}
              </button>
            </div>

          </section>

          <section className="listing-detail-body">
            <div className="listing-detail-page-badges">
              {showDetailStatusBadge ? (
                <span className={`listing-detail-status-chip modal ${modalHeroTone}`}>{modalHeroTag}</span>
              ) : null}
              <span className="listing-detail-status-chip modal muted">{modalConditionTag}</span>
            </div>

            <div className="listing-seller-card">
              <div className="listing-seller-profile">
                {detail.seller_profile_image ? (
                  <img src={detail.seller_profile_image} alt={sellerNickname} />
                ) : (
                  <div className="listing-seller-avatar" />
                )}
                <div className="listing-seller-meta">
                  <strong>{sellerNickname}</strong>
                  <p>{sellerRegionName}</p>
                </div>
              </div>
              <div className="listing-seller-score">
                <strong>{formatSmileScore(detail.seller_smile_score)}</strong>
                <span>{"\uC2A4\uB9C8\uC77C\uC9C0\uC218"}</span>
              </div>
            </div>

            <section className="listing-detail-copy">
              <h1>{detail.title}</h1>
              <strong>{formatPrice(detail.price_amount, listingTransactionType)}</strong>
              <p className="listing-detail-meta">
                {detail.region_name ? `${detail.region_name} \u00B7 ` : ""}
                {formatUpdatedAt(detail.updated_at)}
              </p>
              <div className="listing-detail-description">
                {detail.description.split("\n").map((line, index) => (
                  <p key={`${index}-${line}`}>{line}</p>
                ))}
              </div>
            </section>

            {showHopeLocation ? (
              <section className="listing-detail-hope">
                <div className="listing-detail-hope-title">
                  <strong>{"\uAC70\uB798 \uD76C\uB9DD \uC7A5\uC18C"}</strong>
                </div>
                <div className="listing-detail-map">
                  <div className="listing-detail-map-pin" />
                  <button type="button" className="listing-detail-map-button">
                    {"\uC9C0\uB3C4 \uBCF4\uAE30"}
                  </button>
                </div>
              </section>
            ) : null}

            <section className="listing-detail-stats">
              <span>{`\uCC44\uD305 ${detail.chat_count}`}</span>
              <span>{`\uC870\uD68C ${detail.view_count}`}</span>
            </section>

            <button type="button" className="listing-detail-report">
              {"\uC774 \uAC8C\uC2DC\uAE00 \uC2E0\uACE0\uD558\uAE30"}
            </button>
          </section>

          <footer className="listing-detail-footer">
            {canToggleInterest ? (
              <button
                type="button"
                className={`listing-detail-heart${interested ? " active" : ""}`}
                aria-label={interested ? "remove interest" : "add interest"}
                aria-pressed={interested}
                disabled={interestLoading}
                onClick={() => void handleInterest()}
              >
                {interested ? "\u2665" : "\u2661"}
              </button>
            ) : (
              <div className="listing-detail-heart-placeholder" aria-hidden="true" />
            )}
            <button
              type="button"
              className="listing-detail-chat"
              disabled={isOwner ? statusUpdating : !canChat || creatingChat}
              onClick={() => {
                if (isOwner) {
                  setStatusSheetOpen(true);
                  return;
                }
                void handleChat();
              }}
            >
              {isOwner
                ? "\uC0C1\uD0DC \uBCC0\uACBD"
                : canChat
                ? (creatingChat ? "\uC5EC\uB294 \uC911.." : "\uCC44\uD305\uD558\uAE30")
                : "\uB0B4 \uAC8C\uC2DC\uAE00"}
            </button>
          </footer>
          {statusSheet}
        </>
      ) : null}
    </div>
  );

  return content;
}










