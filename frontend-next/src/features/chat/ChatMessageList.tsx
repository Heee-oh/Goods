import type { ChatMessageItem } from "./floatingTypes";
import { formatMessageTime } from "./floatingUtils";

type ChatMessageListProps = {
  messages: ChatMessageItem[];
  memberId: string | null;
};

export function ChatMessageList({ messages, memberId }: ChatMessageListProps) {
  return (
    <div className="chat-float-messages">
      {messages.map((message) => {
        const mine = memberId != null && String(message.sender_id) === String(memberId);
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
