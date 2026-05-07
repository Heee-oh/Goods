import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent
} from "react";
import useEmblaCarousel from "embla-carousel-react";
import { useLocation, useNavigate } from "react-router-dom";
import { ApiError, apiRequest } from "../lib/api";
import { convertImageToWebpFile } from "../lib/image";
import {
  clearSellDraftId,
  clearSession,
  getSelectedRegionId,
  getSellDraftId,
  saveSelectedRegionId,
  saveSellDraftId
} from "../lib/auth";
import { getTransactionLabel, type TransactionType } from "../lib/transactionType";

type RegionResponse = {
  region_id: number;
  dongnm: string;
  verified_at?: string | null;
  is_primary?: boolean;
  primary?: boolean;
};

type DraftCreateResponse = {
  listingId?: number;
  listing_id?: number;
};

type ListingDetailResponse = {
  listing_id: number;
  title: string;
  description: string;
  category_id: number | null;
  price_amount: number | null;
  transaction_type?: TransactionType | null;
  status: string;
  images?: Array<{
    image_id: number;
    image_url: string;
    sort_order: number;
  }>;
};

type ListingImageUploadResponse = {
  imageUrl?: string;
  image_url?: string;
};

type PhotoItem = {
  id: string;
  previewUrl: string;
  remoteUrl: string | null;
  status: "uploading" | "uploaded" | "error";
};

type CategoryOption = {
  id: number;
  label: string;
};

const DEFAULT_CATEGORY_ID = 1;
const CATEGORY_OPTIONS: CategoryOption[] = [
  { id: 1, label: "피규어" },
  { id: 2, label: "포토카드" },
  { id: 3, label: "한정 굿즈" },
  { id: 4, label: "인형" },
  { id: 5, label: "CD / 앨범" }
];

const TRANSACTION_OPTIONS: { id: TransactionType; label: string }[] = [
  { id: "sell", label: "판매" },
  { id: "trade", label: "교환" },
  { id: "free", label: "나눔" }
];

function normalizeRegion(region: RegionResponse): RegionResponse {
  return {
    ...region,
    primary: Boolean(region.is_primary ?? region.primary),
    verified_at: region.verified_at ?? null
  };
}

export function ListingEditorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const photoInputRef = useRef<HTMLInputElement | null>(null);
  const photoItemsRef = useRef<PhotoItem[]>([]);
  const [emblaRef, emblaApi] = useEmblaCarousel({
    align: "start",
    containScroll: "trimSnaps",
    dragFree: true,
    loop: false,
    slidesToScroll: "auto"
  });
  const [regions, setRegions] = useState<RegionResponse[]>([]);
  const [selectedRegionId, setSelectedRegionId] = useState<number | null>(null);
  const [draftId, setDraftId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [wishlistText, setWishlistText] = useState("");
  const [priceText, setPriceText] = useState("");
  const [categoryId, setCategoryId] = useState(DEFAULT_CATEGORY_ID);
  const [transactionType, setTransactionType] = useState<TransactionType>("sell");
  const [photoItems, setPhotoItems] = useState<PhotoItem[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingPhotos, setUploadingPhotos] = useState(false);
  const [activePhotoIndex, setActivePhotoIndex] = useState(0);
  const [photoSnapCount, setPhotoSnapCount] = useState(1);
  const [showVerifyPopup, setShowVerifyPopup] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);

  useEffect(() => {
    photoItemsRef.current = photoItems;
  }, [photoItems]);

  useEffect(() => {
    return () => {
      for (const item of photoItemsRef.current) {
        if (item.previewUrl.startsWith("blob:")) {
          URL.revokeObjectURL(item.previewUrl);
        }
      }
    };
  }, []);

  useEffect(() => {
    if (!emblaApi) {
      return;
    }

    const syncPhotoState = () => {
      const snapCount = Math.max(1, emblaApi.scrollSnapList().length);
      setPhotoSnapCount(snapCount);
      setActivePhotoIndex(emblaApi.selectedScrollSnap());
    };

    syncPhotoState();
    emblaApi.on("select", syncPhotoState);
    emblaApi.on("reInit", syncPhotoState);

    return () => {
      emblaApi.off("select", syncPhotoState);
      emblaApi.off("reInit", syncPhotoState);
    };
  }, [emblaApi]);

  useEffect(() => {
    emblaApi?.reInit();
  }, [emblaApi, photoItems.length]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await apiRequest<RegionResponse[]>("/api/members/me/regions");
        const normalized = response.map(normalizeRegion);
        const savedRegionId = getSelectedRegionId();
        const selected =
          normalized.find((region) => region.region_id === savedRegionId) ??
          normalized.find((region) => region.primary) ??
          normalized[0] ??
          null;

        setRegions(normalized);
        setSelectedRegionId(selected?.region_id ?? null);
        if (selected) {
          saveSelectedRegionId(selected.region_id);
        }

        const storedDraftId = getSellDraftId();
        if (storedDraftId) {
          const draft = await apiRequest<ListingDetailResponse>(`/api/listings/${storedDraftId}`);
          if (draft.status === "DRAFT") {
            setDraftId(draft.listing_id);
            setTitle(draft.title === "Draft" ? "" : draft.title ?? "");
            setDescription(draft.description ?? "");
            setCategoryId(draft.category_id ?? DEFAULT_CATEGORY_ID);
            setTransactionType(draft.transaction_type ?? "sell");
            setPriceText(draft.price_amount ? String(draft.price_amount) : "");
            setPhotoItems(
              [...(draft.images ?? [])]
                .sort((left, right) => left.sort_order - right.sort_order)
                .map((image) => ({
                  id: `${image.image_id}`,
                  previewUrl: image.image_url,
                  remoteUrl: image.image_url,
                  status: "uploaded" as const
                }))
            );
          } else {
            clearSellDraftId();
          }
        }
        if (selected && !selected.verified_at) {
          setShowVerifyPopup(true);
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          clearSession();
          navigate("/welcome", { replace: true });
          return;
        }

        clearSellDraftId();
        setError(err instanceof Error ? err.message : "새 상품 등록 페이지를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [navigate]);

  const selectedRegion = useMemo(
    () => regions.find((region) => region.region_id === selectedRegionId) ?? null,
    [regions, selectedRegionId]
  );

  const selectedCategory = useMemo(
    () => CATEGORY_OPTIONS.find((option) => option.id === categoryId) ?? CATEGORY_OPTIONS[0],
    [categoryId]
  );
  const transactionSegmentIndex = Math.max(
    0,
    TRANSACTION_OPTIONS.findIndex((option) => option.id === transactionType)
  );

  const isModal = Boolean((location.state as { backgroundLocation?: unknown } | null)?.backgroundLocation);
  const hasSavedDraft = Boolean(draftId ?? getSellDraftId());
  const hasAtLeastOnePhoto = photoItems.length > 0;
  const hasPendingUploads = photoItems.some((item) => item.status !== "uploaded");
  const canSubmit =
    title.trim().length > 0 &&
    description.trim().length > 0 &&
    hasAtLeastOnePhoto &&
    (transactionType !== "sell" || Number(priceText) > 0) &&
    !hasPendingUploads;

  const createDraft = async () => {
    if (!selectedRegionId) {
      throw new Error("저장할 동네를 먼저 선택해주세요.");
    }

    const created = await apiRequest<DraftCreateResponse>(`/api/listings/drafts?region_id=${selectedRegionId}`, {
      method: "POST"
    });
    const listingId = created.listingId ?? created.listing_id;
    if (!listingId) {
      throw new Error("초안 ID를 받아오지 못했습니다.");
    }

    setDraftId(listingId);
    saveSellDraftId(listingId);
    return listingId;
  };

  const syncDraft = async (targetDraftId?: number) => {
    const resolvedDraftId = targetDraftId ?? draftId ?? (await createDraft());
    const normalizedDescription =
      transactionType === "trade" && wishlistText.trim()
        ? `${description.trim()}\n\nWishlist: ${wishlistText.trim()}`
        : description.trim();

    await apiRequest(`/api/listings/${resolvedDraftId}`, {
      method: "PUT",
      body: JSON.stringify({
        title: title.trim(),
        description: normalizedDescription,
        category_id: categoryId,
        price_amount: transactionType === "sell" ? Number(priceText) : 0,
        transaction_type: transactionType,
        hope_location: null,
        image_urls: photoItems
          .map((item) => item.remoteUrl)
          .filter((url): url is string => Boolean(url))
      })
    });

    return resolvedDraftId;
  };

  const scrollToPhotoSlide = (index: number) => {
    if (!emblaApi) {
      return;
    }

    emblaApi.scrollTo(index);
    setActivePhotoIndex(index);
  };

  const handleVerifyRegion = async () => {
    if (!selectedRegionId) {
      return;
    }

    if (!navigator.geolocation) {
      setError("현재 위치를 사용할 수 없습니다.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          setError("");
          await apiRequest(`/api/members/me/regions/${selectedRegionId}/verify`, {
            method: "POST",
            body: JSON.stringify({
              lat: position.coords.latitude,
              lng: position.coords.longitude
            })
          });

          const response = await apiRequest<RegionResponse[]>("/api/members/me/regions");
          const normalized = response.map(normalizeRegion);
          const savedRegionId = getSelectedRegionId();
          const selected =
            normalized.find((region) => region.region_id === savedRegionId) ??
            normalized.find((region) => region.primary) ??
            normalized[0] ??
            null;

          setRegions(normalized);
          setSelectedRegionId(selected?.region_id ?? null);
          if (selected) {
            saveSelectedRegionId(selected.region_id);
          }
          setShowVerifyPopup(false);
        } catch (err) {
          setError(err instanceof Error ? err.message : "동네 인증에 실패했습니다.");
        }
      },
      () => setError("위치 권한이 필요합니다.")
    );
  };

  const handleTempSave = async () => {
    try {
      if (!hasAtLeastOnePhoto) {
        setError("사진을 최소 1장 이상 추가해주세요.");
        return;
      }
      if (hasPendingUploads) {
        setError("이미지 업로드가 완료된 후 저장해주세요.");
        return;
      }
      setSaving(true);
      setError("");
      await syncDraft();
    } catch (err) {
      setError(err instanceof Error ? err.message : "임시 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async () => {
    if (!canSubmit) {
      setError(
        !hasAtLeastOnePhoto
          ? "사진을 최소 1장 이상 추가해주세요."
          : "제목, 설명, 거래 유형, 사진 업로드 상태를 확인해주세요."
      );
      return;
    }

    try {
      setSaving(true);
      setError("");
      const targetDraftId = await syncDraft();
      await apiRequest(`/api/listings/${targetDraftId}/publish`, {
        method: "POST"
      });
      clearSellDraftId();
      navigate("/listing", {
        replace: true,
        state: {
          refreshAt: Date.now(),
          publishedListingId: targetDraftId
        }
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "게시에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteDraft = async () => {
    try {
      if (draftId) {
        await apiRequest(`/api/listings/${draftId}`, {
          method: "DELETE"
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "초안 삭제에 실패했습니다.");
      return;
    }

    clearSellDraftId();
    navigate("/listing", { replace: true });
  };

  const handleKeepDraft = () => {
    if (draftId) {
      saveSellDraftId(draftId);
    }
    navigate("/listing", { replace: true });
  };

  const handleClose = () => {
    if (hasSavedDraft) {
      setShowExitModal(true);
      return;
    }

    navigate("/listing", { replace: true });
  };

  const handlePhotoFiles = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) {
      return;
    }

    const remainingSlots = Math.max(0, 10 - photoItemsRef.current.length);
    if (remainingSlots === 0) {
      return;
    }

    const selectedFiles = files.slice(0, remainingSlots);

    void (async () => {
      try {
        setUploadingPhotos(true);
        setError("");

        const preparedItems = await Promise.all(
          selectedFiles.map(async (file) => {
            const convertedFile = await convertImageToWebpFile(file);
            const previewUrl = URL.createObjectURL(convertedFile);

            return {
              id: crypto.randomUUID(),
              previewUrl,
              convertedFile
            };
          })
        );

        setPhotoItems((current) => [
          ...current,
          ...preparedItems.map((item) => ({
            id: item.id,
            previewUrl: item.previewUrl,
            remoteUrl: null,
            status: "uploading" as const
          }))
        ].slice(0, 10));

        for (const item of preparedItems) {
          const formData = new FormData();
          formData.append("file", item.convertedFile, item.convertedFile.name);

          const response = await apiRequest<ListingImageUploadResponse>("/api/listings/images", {
            method: "POST",
            body: formData
          });

            const uploadedUrl = response.imageUrl ?? response.image_url ?? null;
            if (!uploadedUrl) {
              throw new Error("이미지 업로드 응답이 올바르지 않습니다.");
            }

            setPhotoItems((current) =>
              current.map((photo) =>
                photo.id === item.id
                  ? {
                      ...photo,
                      remoteUrl: uploadedUrl,
                      status: "uploaded"
                    }
                  : photo
              )
            );
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "이미지 업로드에 실패했습니다.");
        setPhotoItems((current) =>
          current.map((photo) =>
            photo.status === "uploading"
              ? {
                  ...photo,
                  status: "error"
                }
              : photo
          )
        );
      } finally {
        setUploadingPhotos(false);
      }
    })();
  };

  const removePhoto = (index: number) => {
    setPhotoItems((current) => {
      const removed = current[index];
      if (removed?.previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(removed.previewUrl);
      }
      const next = current.filter((_, currentIndex) => currentIndex !== index);
      return next;
    });
  };

  const screen = (
      <div className="main-screen sell-screen">
        <header className="sell-header">
          <button type="button" className="sell-close" onClick={handleClose} aria-label="뒤로가기">
            ←
          </button>
          <h1>새 상품 등록</h1>
          <button
            type="button"
            className="sell-draft"
            disabled={saving || uploadingPhotos || hasPendingUploads}
            onClick={() => void handleTempSave()}
          >
            임시 저장
          </button>
        </header>

      {loading ? <p className="region-status">遺덈윭?ㅻ뒗 以?.</p> : null}
      {error ? <p className="auth-error">{error}</p> : null}

      <section className="sell-photo-card">
        <div className="sell-photo-viewport" ref={emblaRef}>
          <div className="sell-photo-rail" aria-label="사진 캐러셀">
            <button
              type="button"
              className="sell-photo-add"
              onClick={() => photoInputRef.current?.click()}
              disabled={uploadingPhotos}
            >
              <span className="sell-photo-plus">+</span>
              <strong>굿즈 등록</strong>
              <span>{uploadingPhotos ? "업로드 중..." : `(${photoItems.length}/10)`}</span>
            </button>

            {photoItems.map((photo, index) => (
              <article key={photo.id} className="sell-photo-thumb">
                <img src={photo.previewUrl} alt={`미리보기 ${index + 1}`} />
                <button type="button" className="sell-photo-remove" onClick={() => removePhoto(index)} aria-label="사진 삭제">
                  X
                </button>
                {photo.status !== "uploaded" ? (
                  <span className="sell-photo-status">
                    {photo.status === "uploading" ? "업로드 중" : "업로드 실패"}
                  </span>
                ) : null}
              </article>
            ))}
          </div>
        </div>

        {!hasAtLeastOnePhoto ? (
          <div className="sell-photo-warning">게시하려면 사진을 최소 1장 이상 추가해야 합니다.</div>
        ) : null}

        <input
          ref={photoInputRef}
          type="file"
          accept="image/*"
          multiple
          hidden
          onChange={(event) => void handlePhotoFiles(event)}
        />

        <div className="sell-photo-dots" aria-label="사진 이동">
          {Array.from({ length: photoSnapCount }).map((_, index) => (
            <button
              key={index}
              type="button"
              className={index === activePhotoIndex ? "sell-dot active" : "sell-dot"}
              onClick={() => scrollToPhotoSlide(index)}
              aria-label={`사진 ${index + 1}로 이동`}
            />
          ))}
        </div>
      </section>

      <section className="sell-form-card">
        <section className="sell-section">
          <h2>상품 정보</h2>
          <input
            className="sell-input"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="상품명 (예: Radio EVA 아스카 피규어)"
          />
        </section>

        <section className="sell-section">
          <label className="sell-label" htmlFor="sell-category">
            카테고리
          </label>
          <select
            id="sell-category"
            className="sell-select"
            value={categoryId}
            onChange={(event) => setCategoryId(Number(event.target.value))}
          >
            {CATEGORY_OPTIONS.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
          <p className="sell-hint">현재: {selectedCategory.label}</p>
        </section>

        <section className="sell-section">
          <label className="sell-label" htmlFor="sell-description">
            상세 설명
          </label>
          <textarea
            id="sell-description"
            className="sell-textarea"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="상태, 박스 여부, 스크래치 등 구매자가 알아야 할 내용을 적어주세요."
          />
        </section>

        <section className="sell-section">
          <h2>거래 정보</h2>
          <div className="sell-segmented" data-transaction={TRANSACTION_OPTIONS[transactionSegmentIndex]?.id ?? "sell"}>
            <div className="sell-segment-indicator" aria-hidden="true" />
            {TRANSACTION_OPTIONS.map((option) => (
              <button
                key={option.id}
                type="button"
                className={transactionType === option.id ? "sell-segment active" : "sell-segment"}
                onClick={() => setTransactionType(option.id)}
              >
                {option.label}
              </button>
            ))}
          </div>

          {transactionType === "sell" ? (
            <input
              className="sell-input"
              inputMode="numeric"
              value={priceText}
              onChange={(event) => setPriceText(event.target.value.replace(/[^\d]/g, ""))}
              placeholder="가격"
            />
          ) : null}

          {transactionType === "trade" ? (
            <textarea
              className="sell-textarea sell-trade-textarea"
              value={wishlistText}
              onChange={(event) => setWishlistText(event.target.value)}
              placeholder="원하는 교환 품목 / 요청 사항"
            />
          ) : null}

          {transactionType !== "sell" ? (
            <div className="sell-notice">{getTransactionLabel(transactionType)} 거래가 선택되었습니다.</div>
          ) : null}

          <div className="sell-region-row">
            <button type="button" className="sell-row-button" disabled>
              거래 장소
              <span>위치 설정 &gt;</span>
            </button>
            {!selectedRegion?.verified_at ? (
              <div className="sell-region-warning">
                <strong>{selectedRegion?.dongnm ?? "선택한 동네"}</strong>는 게시 전에 인증이 필요합니다.
              </div>
            ) : null}
          </div>
        </section>

        <div className="sell-actions">
          <button
            type="button"
            className="sell-secondary"
            disabled={saving || uploadingPhotos || hasPendingUploads || !hasAtLeastOnePhoto}
            onClick={() => void handleTempSave()}
          >
            임시 저장
          </button>
          <button
            type="button"
            className="sell-submit"
            disabled={!canSubmit || saving || uploadingPhotos || hasPendingUploads}
            onClick={() => void handlePublish()}
          >
            게시하기
          </button>
        </div>
      </section>

      {showVerifyPopup ? (
        <div className="overlay">
          <div className="overlay-dim" />
          <div className="confirm-modal">
            <h3>동네 인증</h3>
            <p className="verify-copy">
              {`${selectedRegion?.dongnm ?? "선택한 동네"} 인증이 있어야 게시할 수 있습니다.`}
            </p>
            <div className="confirm-actions">
              <button type="button" className="confirm-cancel" onClick={() => navigate("/listing", { replace: true })}>
                취소
              </button>
              <button type="button" className="confirm-delete" onClick={() => void handleVerifyRegion()}>
                인증하기
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showExitModal ? (
        <div className="overlay">
          <div className="overlay-dim" />
          <div className="confirm-modal">
            <h3>임시 저장을 버릴까요?</h3>
            <p className="verify-copy">이 임시 저장은 삭제하면 되돌릴 수 없습니다.</p>
            <div className="confirm-actions">
              <button type="button" className="confirm-cancel" onClick={() => void handleDeleteDraft()}>
                삭제
              </button>
              <button type="button" className="confirm-delete" onClick={handleKeepDraft}>
                유지
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );

  if (!isModal) {
    return screen;
  }

  return (
    <div className="sell-modal-overlay">
      <button type="button" className="sell-modal-backdrop" onClick={() => navigate(-1)} aria-label="판매 글 작성창 닫기" />
      <div className="sell-modal-shell">{screen}</div>
    </div>
  );
}









