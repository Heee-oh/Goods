import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren
} from "react";
import { useLocation } from "react-router-dom";
import { apiRequest } from "./api";
import { getAccessToken, getMemberId } from "./auth";
import { readCachedJson, writeCachedJson } from "./cache";

type RawChatRoomSummary = {
  chat_room_id?: number | string;
  chatRoomId?: number | string;
  partner_nickname?: string;
  partnerNickname?: string;
};

type SocketMessage = {
  chat_room_id?: number | string;
  chatRoomId?: number | string;
  sender_id?: number | string;
  senderId?: number | string;
  content: string;
};

type ToastState = {
  roomId: string;
  partnerNickname: string;
  content: string;
};

type StompSubscriptionLike = {
  unsubscribe: () => void;
};

type StompClientLike = {
  connected?: boolean;
  activate: () => void;
  deactivate: () => Promise<void> | void;
  subscribe: (destination: string, callback: (frame: { body: string }) => void) => StompSubscriptionLike;
};

type ChatNotificationsContextValue = {
  totalUnreadCount: number;
  unreadCountByRoom: Record<string, number>;
  toast: ToastState | null;
  dismissToast: () => void;
  registerRooms: (rooms: Array<{ chatRoomId: string; partnerNickname?: string | null }>) => void;
  registerRoom: (room: { chatRoomId: string; partnerNickname?: string | null }) => void;
  markRoomRead: (chatRoomId: string) => void;
};

const STORAGE_KEY = "chat_unread_counts";
const CHAT_ROOMS_CACHE_PREFIX = "goods:chat-rooms";

const ChatNotificationsContext = createContext<ChatNotificationsContextValue | null>(null);

function loadUnreadCounts() {
  if (typeof window === "undefined") {
    return {};
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {};
    }

    const parsed = JSON.parse(raw) as Record<string, number>;
    return Object.fromEntries(
      Object.entries(parsed).filter(
        ([roomId, count]) => typeof roomId === "string" && typeof count === "number" && count > 0
      )
    );
  } catch {
    return {};
  }
}

function normalizeRoom(summary: RawChatRoomSummary) {
  const chatRoomId = summary.chat_room_id ?? summary.chatRoomId;
  if (chatRoomId == null) {
    return null;
  }

  return {
    chatRoomId: String(chatRoomId),
    partnerNickname: summary.partner_nickname ?? summary.partnerNickname ?? null
  };
}

function getActiveChatRoomId(pathname: string) {
  const match = pathname.match(/^\/chatting\/([^/]+)$/);
  return match ? decodeURIComponent(match[1]) : null;
}

export function ChatNotificationsProvider({ children }: PropsWithChildren) {
  const location = useLocation();
  const token = getAccessToken();
  const memberId = getMemberId();
  const activeChatRoomId = getActiveChatRoomId(location.pathname);

  const [toast, setToast] = useState<ToastState | null>(null);
  const [unreadCountByRoom, setUnreadCountByRoom] = useState<Record<string, number>>(loadUnreadCounts);
  const [roomDirectory, setRoomDirectory] = useState<Record<string, { partnerNickname: string | null }>>({});

  const roomDirectoryRef = useRef(roomDirectory);
  const subscriptionsRef = useRef<Map<string, StompSubscriptionLike>>(new Map());
  const stompRef = useRef<StompClientLike | null>(null);
  const activeChatRoomIdRef = useRef(activeChatRoomId);
  const memberIdRef = useRef(memberId);

  useEffect(() => {
    roomDirectoryRef.current = roomDirectory;
  }, [roomDirectory]);

  useEffect(() => {
    activeChatRoomIdRef.current = activeChatRoomId;
  }, [activeChatRoomId]);

  useEffect(() => {
    memberIdRef.current = memberId;
  }, [memberId]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(unreadCountByRoom));
  }, [unreadCountByRoom]);

  const registerRooms = useCallback(
    (rooms: Array<{ chatRoomId: string; partnerNickname?: string | null }>) => {
      if (!rooms.length) {
        return;
      }

      setRoomDirectory((current) => {
        const next = { ...current };
        let changed = false;

        rooms.forEach((room) => {
          const existing = next[room.chatRoomId];
          const partnerNickname = room.partnerNickname ?? null;

          if (!existing || existing.partnerNickname !== partnerNickname) {
            next[room.chatRoomId] = { partnerNickname };
            changed = true;
          }
        });

        return changed ? next : current;
      });
    },
    []
  );

  const registerRoom = useCallback(
    (room: { chatRoomId: string; partnerNickname?: string | null }) => {
      registerRooms([room]);
    },
    [registerRooms]
  );

  const markRoomRead = useCallback((chatRoomId: string) => {
    setUnreadCountByRoom((current) => {
      if (!current[chatRoomId]) {
        return current;
      }

      const next = { ...current };
      delete next[chatRoomId];
      return next;
    });
  }, []);

  useEffect(() => {
    if (!activeChatRoomId) {
      return;
    }

    markRoomRead(activeChatRoomId);
  }, [activeChatRoomId, markRoomRead]);

  useEffect(() => {
    if (!token) {
      setToast(null);
      setUnreadCountByRoom({});
      subscriptionsRef.current.forEach((subscription) => subscription.unsubscribe());
      subscriptionsRef.current.clear();
      void stompRef.current?.deactivate();
      stompRef.current = null;
      setRoomDirectory({});
      return;
    }

    let disposed = false;

    const syncSubscriptions = (client: StompClientLike) => {
      const nextRoomIds = new Set(Object.keys(roomDirectoryRef.current));

      subscriptionsRef.current.forEach((subscription, roomId) => {
        if (!nextRoomIds.has(roomId)) {
          subscription.unsubscribe();
          subscriptionsRef.current.delete(roomId);
        }
      });

      nextRoomIds.forEach((roomId) => {
        if (subscriptionsRef.current.has(roomId)) {
          return;
        }

        const subscription = client.subscribe(`/sub/chat/room/${roomId}`, (frame: { body: string }) => {
          const payload = JSON.parse(frame.body) as SocketMessage;
          const incomingRoomId = String(payload.chat_room_id ?? payload.chatRoomId ?? "");
          if (!incomingRoomId || incomingRoomId !== roomId) {
            return;
          }

          const senderId = String(payload.sender_id ?? payload.senderId ?? "");
          if (!senderId || senderId === memberIdRef.current) {
            return;
          }

          if (activeChatRoomIdRef.current === roomId) {
            return;
          }

          setUnreadCountByRoom((current) => ({
            ...current,
            [roomId]: (current[roomId] ?? 0) + 1
          }));

          const partnerNickname =
            roomDirectoryRef.current[roomId]?.partnerNickname ?? "새 메시지";

          setToast({
            roomId,
            partnerNickname,
            content: payload.content
          });
        });

        subscriptionsRef.current.set(roomId, subscription);
      });
    };

    const connect = async () => {
      try {
        if (typeof globalThis !== "undefined" && !("global" in globalThis)) {
          (globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;
        }

        const [{ Client }, { default: SockJS }] = await Promise.all([
          import("@stomp/stompjs"),
          import("sockjs-client")
        ]);

        if (disposed) {
          return;
        }

        const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

        const client = new Client({
          webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws-chat`),
          reconnectDelay: 5000,
          connectHeaders: { Authorization: `Bearer ${token}` },
          onConnect: () => {
            if (!disposed) {
              syncSubscriptions(client as unknown as StompClientLike);
            }
          },
          onWebSocketClose: () => {
            if (!disposed) {
              subscriptionsRef.current.clear();
            }
          }
        });

        stompRef.current = client as unknown as StompClientLike;
        client.activate();
      } catch (error) {
        console.error("[chat][notifications] connection failed", error);
      }
    };

    void connect();

    return () => {
      disposed = true;
      subscriptionsRef.current.forEach((subscription) => subscription.unsubscribe());
      subscriptionsRef.current.clear();
      void stompRef.current?.deactivate();
      stompRef.current = null;
    };
  }, [token]);

  useEffect(() => {
    if (stompRef.current?.connected) {
      const client = stompRef.current;
      const nextRoomIds = Object.keys(roomDirectory);

      subscriptionsRef.current.forEach((subscription, roomId) => {
        if (!nextRoomIds.includes(roomId)) {
          subscription.unsubscribe();
          subscriptionsRef.current.delete(roomId);
        }
      });

      nextRoomIds.forEach((roomId) => {
        if (subscriptionsRef.current.has(roomId)) {
          return;
        }

        const subscription = client.subscribe(`/sub/chat/room/${roomId}`, (frame: { body: string }) => {
          const payload = JSON.parse(frame.body) as SocketMessage;
          const incomingRoomId = String(payload.chat_room_id ?? payload.chatRoomId ?? "");
          if (!incomingRoomId || incomingRoomId !== roomId) {
            return;
          }

          const senderId = String(payload.sender_id ?? payload.senderId ?? "");
          if (!senderId || senderId === memberIdRef.current) {
            return;
          }

          if (activeChatRoomIdRef.current === roomId) {
            return;
          }

          setUnreadCountByRoom((current) => ({
            ...current,
            [roomId]: (current[roomId] ?? 0) + 1
          }));

          const partnerNickname =
            roomDirectoryRef.current[roomId]?.partnerNickname ?? "새 메시지";

          setToast({
            roomId,
            partnerNickname,
            content: payload.content
          });
        });

        subscriptionsRef.current.set(roomId, subscription);
      });
    }
  }, [roomDirectory]);

  useEffect(() => {
    if (!token) {
      return;
    }
    if (!location.pathname.startsWith("/chatting")) {
      return;
    }

    const loadRooms = async () => {
      try {
        const cacheKey = memberId ? `${CHAT_ROOMS_CACHE_PREFIX}:${memberId}` : CHAT_ROOMS_CACHE_PREFIX;
        const cachedRooms = readCachedJson<RawChatRoomSummary[]>(cacheKey);
        if (cachedRooms) {
          const rooms = cachedRooms
            .map(normalizeRoom)
            .filter((room): room is NonNullable<ReturnType<typeof normalizeRoom>> => room !== null);

          registerRooms(rooms);
          return;
        }

        const response = await apiRequest<RawChatRoomSummary[]>("/api/chat-rooms");
        writeCachedJson(cacheKey, response);
        const rooms = response
          .map(normalizeRoom)
          .filter((room): room is NonNullable<ReturnType<typeof normalizeRoom>> => room !== null);

        registerRooms(rooms);
      } catch (error) {
        console.error("[chat][notifications] failed to load rooms", error);
      }
    };

    void loadRooms();
  }, [location.pathname, memberId, registerRooms, token]);

  useEffect(() => {
    if (!toast) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setToast((current) => (current?.roomId === toast.roomId ? null : current));
    }, 3500);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [toast]);

  const totalUnreadCount = useMemo(
    () => Object.values(unreadCountByRoom).reduce((sum, count) => sum + count, 0),
    [unreadCountByRoom]
  );

  const value = useMemo<ChatNotificationsContextValue>(
    () => ({
      totalUnreadCount,
      unreadCountByRoom,
      toast,
      dismissToast: () => setToast(null),
      registerRooms,
      registerRoom,
      markRoomRead
    }),
    [markRoomRead, registerRoom, registerRooms, toast, totalUnreadCount, unreadCountByRoom]
  );

  return (
    <ChatNotificationsContext.Provider value={value}>
      {children}
    </ChatNotificationsContext.Provider>
  );
}

export function useChatNotifications() {
  const context = useContext(ChatNotificationsContext);
  if (!context) {
    throw new Error("useChatNotifications must be used within ChatNotificationsProvider");
  }

  return context;
}
