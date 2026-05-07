type CompletionActionSheetProps = {
  partnerNickname: string;
  listingTitle: string;
  busy: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

export function CompletionActionSheet({
  partnerNickname,
  listingTitle,
  busy,
  onConfirm,
  onClose
}: CompletionActionSheetProps) {
  return (
    <div className="chat-modal-overlay" onClick={onClose}>
      <div className="chat-confirm-modal" onClick={(event) => event.stopPropagation()}>
        <p>
          <strong>{partnerNickname}</strong>와의 거래를 완료할까요?
        </p>
        <p>{listingTitle}를 거래 완료 상태로 변경합니다.</p>
        <button type="button" className="chat-confirm-primary" onClick={onConfirm} disabled={busy}>
          {busy ? "처리 중..." : "거래 완료"}
        </button>
        <button type="button" className="chat-confirm-secondary" onClick={onClose} disabled={busy}>
          취소
        </button>
      </div>
    </div>
  );
}
