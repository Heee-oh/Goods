import type { RegionResponse } from "./types";

type RegionDeleteConfirmProps = {
  target: RegionResponse | null;
  onCancel: () => void;
  onConfirm: () => void;
};

export function RegionDeleteConfirm({ target, onCancel, onConfirm }: RegionDeleteConfirmProps) {
  if (!target) {
    return null;
  }

  return (
    <div className="overlay">
      <div className="overlay-dim" />
      <div className="confirm-modal">
        <p>{`'${target.dongnm}'\uC744 \uC0AD\uC81C\uD560\uAE4C\uC694?`}</p>
        <div className="confirm-actions">
          <button type="button" className="confirm-cancel" onClick={onCancel}>
            {"\uCDE8\uC18C"}
          </button>
          <button type="button" className="confirm-delete" onClick={onConfirm}>
            {"\uC0AD\uC81C"}
          </button>
        </div>
      </div>
    </div>
  );
}
