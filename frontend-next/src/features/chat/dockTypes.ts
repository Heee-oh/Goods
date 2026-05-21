export type OpenRoomState = {
  chatRoomId: string;
  partnerNickname: string;
  minimized: boolean;
  zIndex: number;
  position: { x: number; y: number };
  size: { width: number; height: number };
};

export type OpenChatRoomEventDetail = {
  chatRoomId?: number | string;
  partnerNickname?: string | null;
};

export type ChatRoomPreviewUpdateEventDetail = {
  chatRoomId?: number | string;
  content?: string;
  createdAt?: string;
};
