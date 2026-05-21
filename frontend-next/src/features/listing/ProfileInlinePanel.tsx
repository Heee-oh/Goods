import type { ChangeEvent, RefObject } from "react";
import { formatSmileScore } from "./utils";

type ProfileInlinePanelProps = {
  fileInputRef: RefObject<HTMLInputElement | null>;
  nicknameInputId: string;
  nickname: string;
  profileImage: string | null;
  profileUploading: boolean;
  profileSaving: boolean;
  nicknameDraft: string;
  smileValue: number;
  smileProgress: number;
  onImageInput: (event: ChangeEvent<HTMLInputElement>) => void;
  onOpenImagePicker: () => void;
  onNicknameDraftChange: (value: string) => void;
  onNicknameSave: () => void;
};

export function ProfileInlinePanel({
  fileInputRef,
  nicknameInputId,
  nickname,
  profileImage,
  profileUploading,
  profileSaving,
  nicknameDraft,
  smileValue,
  smileProgress,
  onImageInput,
  onOpenImagePicker,
  onNicknameDraftChange,
  onNicknameSave
}: ProfileInlinePanelProps) {
  const displayName = nickname || "내 프로필";

  return (
    <section className="profile-inline-shell">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="profile-file-input"
        onChange={onImageInput}
      />

      <section className="profile-avatar-section">
        <div className="profile-avatar-wrap">
          <div className="profile-avatar-circle">
            {profileImage ? <img src={profileImage} alt={displayName} /> : <span>{displayName.slice(0, 1).toUpperCase()}</span>}
          </div>
          <button
            type="button"
            className="profile-avatar-hover"
            onClick={onOpenImagePicker}
            disabled={profileUploading}
          >
            {profileUploading ? "업로드 중.." : "이미지 변경"}
          </button>
        </div>
        <p className="profile-avatar-hint">마우스를 올리면 이미지를 바꿀 수 있어요.</p>
      </section>

      <section className="profile-smile-section">
        <div className="profile-smile-head">
          <strong>스마일 지수</strong>
          <span>{formatSmileScore(smileValue)}</span>
        </div>
        <div className="profile-smile-track" aria-hidden="true">
          <div className="profile-smile-fill" style={{ width: `${smileProgress}%` }} />
        </div>
        <p>100점 만점 기준으로 표시됩니다.</p>
      </section>

      <section className="profile-nickname-block">
        <label htmlFor={nicknameInputId}>닉네임</label>
        <div className="profile-nickname-row">
          <input
            id={nicknameInputId}
            type="text"
            value={nicknameDraft}
            onChange={(event) => onNicknameDraftChange(event.target.value)}
            placeholder="닉네임을 입력하세요"
          />
          <button type="button" onClick={onNicknameSave} disabled={profileSaving}>
            {profileSaving ? "저장 중.." : "변경"}
          </button>
        </div>
      </section>
    </section>
  );
}
