import type { ChatMessageItem } from "./floatingTypes";
import { formatMessageTime } from "./floatingUtils";

type ChatMessageListProps = {
  messages: ChatMessageItem[];
  memberId: string | null;
};

function formatAppointmentDateTime(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

function formatSystemMessage(content: string) {
  try {
    const parsed = JSON.parse(content) as { kind?: string; meet_at?: string; canceled_at?: string };
    if (parsed.kind === "APPOINTMENT_CREATED" && parsed.meet_at) {
      return `약속이 설정되었습니다. ${formatAppointmentDateTime(parsed.meet_at)}`;
    }
    if (parsed.kind === "APPOINTMENT_CANCELED") {
      return "약속이 취소되었습니다.";
    }
  } catch {
    // Plain system messages are rendered as-is.
  }

  return content;
}

export function ChatMessageList({ messages, memberId }: ChatMessageListProps) {
  return (
    <div className="chat-float-messages">
      {messages.map((message) => {
        const mine = memberId != null && String(message.sender_id) === String(memberId);
        if (message.type === "SYSTEM") {
          return (
            <div key={message.message_id} className="chat-system-row">
              <div className="chat-system-message">{formatSystemMessage(message.content)}</div>
            </div>
          );
        }

        return (
          <div key={message.message_id} className={mine ? "chat-message-row mine" : "chat-message-row"}>
            <div className="chat-message-stack">
              <div className={mine ? "chat-message-bubble mine" : "chat-message-bubble"}>{message.content}</div>
              <span className="chat-message-time">{formatMessageTime(message.created_at)}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
