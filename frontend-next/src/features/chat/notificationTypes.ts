export type RawChatRoomSummary = {
  chat_room_id?: number | string;
  chatRoomId?: number | string;
  partner_nickname?: string;
  partnerNickname?: string;
};

export type SocketMessage = {
  message_id?: number | string;
  messageId?: number | string;
  chat_room_id?: number | string;
  chatRoomId?: number | string;
  sender_id?: number | string;
  senderId?: number | string;
  created_at?: string;
  createdAt?: string;
  content: string;
};

export type NotificationMessage = {
  notification_type?: string;
  notificationType?: string;
  trade_id?: number | string;
  tradeId?: number | string;
  appointment_id?: number | string;
  appointmentId?: number | string;
  chat_room_id?: number | string | null;
  chatRoomId?: number | string | null;
  partner_nickname?: string;
  partnerNickname?: string;
  listing_title?: string;
  listingTitle?: string;
  writer_is_seller?: boolean;
  writerIsSeller?: boolean;
  meet_at?: string;
  meetAt?: string;
};

export type ToastState = {
  roomId: string;
  partnerNickname: string;
  content: string;
};

export type AppointmentReminderState = {
  appointmentId: string;
  chatRoomId: string | null;
  partnerNickname: string;
  meetAt: string;
};

export type StompSubscriptionLike = {
  unsubscribe: () => void;
};

export type StompClientLike = {
  connected?: boolean;
  activate: () => void;
  deactivate: () => Promise<void> | void;
  subscribe: (destination: string, callback: (frame: { body: string }) => void) => StompSubscriptionLike;
};

export type ChatNotificationsContextValue = {
  totalUnreadCount: number;
  unreadCountByRoom: Record<string, number>;
  roomsVersion: number;
  toast: ToastState | null;
  appointmentReminder: AppointmentReminderState | null;
  dismissToast: () => void;
  dismissAppointmentReminder: () => void;
  registerRooms: (rooms: Array<{ chatRoomId: string; partnerNickname?: string | null }>) => void;
  registerRoom: (room: { chatRoomId: string; partnerNickname?: string | null }) => void;
  markRoomRead: (chatRoomId: string) => void;
};
