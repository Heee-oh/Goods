import type { TransactionType } from "@/lib/transactionType";

export type ChatMessageItem = {
  message_id: number | string;
  sender_id: string;
  type: "TEXT" | "IMAGE" | "SYSTEM";
  content: string;
  created_at: string;
};

export type ChatRoomDetail = {
  chat_room_id: number | string;
  listing_id: number | string;
  listing_first_image: string | null;
  listing_status?: string | null;
  listing_reserver_id?: number | string | null;
  listing_transaction_type: TransactionType | null;
  seller_id?: number | string;
  partner_id?: number | string;
  partner_nickname: string;
  partner_profile_image: string | null;
  partner_smile_score: number | null;
  listing_title: string;
  listing_price: number | null;
  region_name?: string | null;
  current_appointment?: {
    appointment_id: number | string;
    meet_at: string;
    reminder_minutes: number | null;
  } | null;
  messages: ChatMessageItem[];
};

export type AppointmentResponse = {
  appointment_id: number | string;
  meet_at: string;
  reminder_minutes: number | null;
};

export type SocketMessage = {
  message_id?: number | string;
  messageId?: number | string;
  sender_id?: number | string;
  senderId?: number | string;
  type: "TEXT" | "IMAGE" | "SYSTEM";
  content: string;
  created_at?: string;
  createdAt?: string;
};

export type StompClientLike = {
  connected?: boolean;
  activate: () => void;
  deactivate: () => Promise<void> | void;
  subscribe: (destination: string, callback: (frame: { body: string }) => void) => void;
  publish: (options: { destination: string; body: string }) => void;
};
