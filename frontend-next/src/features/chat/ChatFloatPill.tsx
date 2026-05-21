import type { PointerEvent as ReactPointerEvent } from "react";

type ChatFloatPillProps = {
  title: string;
  position: { x: number; y: number };
  zIndex: number;
  onPointerDown: (event: ReactPointerEvent<HTMLDivElement>) => void;
  onRestore: () => void;
  onClose: () => void;
};

export function ChatFloatPill({
  title,
  position,
  zIndex,
  onPointerDown,
  onRestore,
  onClose
}: ChatFloatPillProps) {
  return (
    <div
      className="chat-float-pill"
      style={{ left: position.x, top: position.y, zIndex }}
      role="button"
      tabIndex={0}
      onPointerDown={onPointerDown}
      onClick={onRestore}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          onRestore();
        }
      }}
    >
      <span className="chat-float-pill-name">{title}</span>
      <button
        type="button"
        className="chat-float-pill-close"
        aria-label="종료"
        onClick={(event) => {
          event.stopPropagation();
          onClose();
        }}
      >
        ×
      </button>
    </div>
  );
}
