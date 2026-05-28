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
import { useLocation } from "@/lib/nextRouterCompat";
import { apiRequest } from "./api";
import { getAccessToken, getMemberId } from "./auth";
import { writeCachedJson } from "./cache";
import { getApiBaseUrl } from "./config";
import {
  CHAT_ROOM_PREVIEW_EVENT,
  CHAT_ROOMS_CACHE_PREFIX,
  CHAT_UNREAD_STORAGE_KEY
} from "@/features/chat/notificationConstants";
import type {
  AppointmentReminderState,
  ChatNotificationsContextValue,
  NotificationMessage,
  RawChatRoomSummary,
  SocketMessage,
  StompClientLike,
  StompSubscriptionLike,
  ToastState
} from "@/features/chat/notificationTypes";
import {
  getActiveChatRoomId,
  loadUnreadCounts,
  normalizeRoom
} from "@/features/chat/notificationUtils";

const ChatNotificationsContext = createContext<ChatNotificationsContextValue | null>(null);

export function ChatNotificationsProvider({ children }: PropsWithChildren) {
  const location = useLocation();
  const token = getAccessToken();
  const memberId = getMemberId();
  const activeChatRoomId = getActiveChatRoomId(location.pathname);

  const [toast, setToast] = useState<ToastState | null>(null);
  const [appointmentReminder, setAppointmentReminder] = useState<AppointmentReminderState | null>(null);
  const [unreadCountByRoom, setUnreadCountByRoom] = useState<Record<string, number>>({});
  const [unreadCountsLoaded, setUnreadCountsLoaded] = useState(false);
  const [roomDirectory, setRoomDirectory] = useState<Record<string, { partnerNickname: string | null }>>({});
  const [roomsVersion, setRoomsVersion] = useState(0);

  const roomDirectoryRef = useRef(roomDirectory);
  const subscriptionsRef = useRef<Map<string, StompSubscriptionLike>>(new Map());
  const memberSubscriptionRef = useRef<StompSubscriptionLike | null>(null);
  const notificationSubscriptionRef = useRef<StompSubscriptionLike | null>(null);
  const seenMessageKeysRef = useRef<Set<string>>(new Set());
  const seenNotificationKeysRef = useRef<Set<string>>(new Set());
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
    setUnreadCountByRoom(loadUnreadCounts());
    setUnreadCountsLoaded(true);
  }, []);

  useEffect(() => {
    if (!unreadCountsLoaded) {
      return;
    }

    window.localStorage.setItem(CHAT_UNREAD_STORAGE_KEY, JSON.stringify(unreadCountByRoom));
  }, [unreadCountByRoom, unreadCountsLoaded]);

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

  const refreshRooms = useCallback(async () => {
    if (!token) {
      return;
    }

    const cacheKey = memberId ? `${CHAT_ROOMS_CACHE_PREFIX}:${memberId}` : CHAT_ROOMS_CACHE_PREFIX;
    const response = await apiRequest<RawChatRoomSummary[]>("/api/chat-rooms");
    writeCachedJson(cacheKey, response);

    const rooms = response
      .map(normalizeRoom)
      .filter((room): room is NonNullable<ReturnType<typeof normalizeRoom>> => room !== null);

    registerRooms(rooms);
    setRoomsVersion((current) => current + 1);
  }, [memberId, registerRooms, token]);

  const emitRoomPreviewUpdate = useCallback((roomId: string, content: string, createdAt: string) => {
    if (typeof window === "undefined") {
      return;
    }

    window.dispatchEvent(
      new CustomEvent(CHAT_ROOM_PREVIEW_EVENT, {
        detail: {
          chatRoomId: roomId,
          content,
          createdAt
        }
      })
    );
  }, []);

  const handleIncomingChatEvent = useCallback(
    (payload: SocketMessage, fallbackRoomId?: string, refreshAfterReceive = false) => {
      const incomingRoomId = String(payload.chat_room_id ?? payload.chatRoomId ?? fallbackRoomId ?? "");
      if (!incomingRoomId) {
        return;
      }

      const senderId = String(payload.sender_id ?? payload.senderId ?? "");
      const createdAt = payload.created_at ?? payload.createdAt ?? new Date().toISOString();
      emitRoomPreviewUpdate(incomingRoomId, payload.content, createdAt);

      if (!senderId || senderId === memberIdRef.current) {
        return;
      }

      const messageId = String(payload.message_id ?? payload.messageId ?? `${senderId}:${payload.content}`);
      const messageKey = `${incomingRoomId}:${messageId}`;
      if (seenMessageKeysRef.current.has(messageKey)) {
        return;
      }

      seenMessageKeysRef.current.add(messageKey);
      if (seenMessageKeysRef.current.size > 500) {
        const oldestKey = seenMessageKeysRef.current.values().next().value;
        if (oldestKey) {
          seenMessageKeysRef.current.delete(oldestKey);
        }
      }

      if (activeChatRoomIdRef.current !== incomingRoomId) {
        setUnreadCountByRoom((current) => ({
          ...current,
          [incomingRoomId]: (current[incomingRoomId] ?? 0) + 1
        }));

        const partnerNickname =
          roomDirectoryRef.current[incomingRoomId]?.partnerNickname ?? "새 메시지";

        setToast({
          roomId: incomingRoomId,
          partnerNickname,
          content: payload.content
        });
      }
      if (refreshAfterReceive && !roomDirectoryRef.current[incomingRoomId]) {
        void refreshRooms().catch((error) => {
          console.error("[chat][notifications] failed to refresh rooms", error);
        });
      }
    },
    [emitRoomPreviewUpdate, refreshRooms]
  );

  useEffect(() => {
    if (!activeChatRoomId) {
      return;
    }

    markRoomRead(activeChatRoomId);
  }, [activeChatRoomId, markRoomRead]);

  useEffect(() => {
    if (!token) {
      setToast(null);
      setAppointmentReminder(null);
      setUnreadCountByRoom({});
      subscriptionsRef.current.forEach((subscription) => subscription.unsubscribe());
      subscriptionsRef.current.clear();
      memberSubscriptionRef.current?.unsubscribe();
      memberSubscriptionRef.current = null;
      notificationSubscriptionRef.current?.unsubscribe();
      notificationSubscriptionRef.current = null;
      void stompRef.current?.deactivate();
      stompRef.current = null;
      setRoomDirectory({});
      seenNotificationKeysRef.current.clear();
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
          handleIncomingChatEvent(payload, roomId);
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

        const apiBaseUrl = getApiBaseUrl();

        const handleNotificationEvent = (frame: { body: string }) => {
          const payload = JSON.parse(frame.body) as NotificationMessage;
          const notificationType = payload.notification_type ?? payload.notificationType;
          if (notificationType === "REVIEW_REQUEST") {
            const tradeId = payload.trade_id ?? payload.tradeId;
            const partnerNickname = payload.partner_nickname ?? payload.partnerNickname;
            if (tradeId != null && partnerNickname) {
              window.dispatchEvent(
                new CustomEvent("goods:review-prompt", {
                  detail: {
                    trade_id: String(tradeId),
                    partner_nickname: partnerNickname,
                    listing_title: payload.listing_title ?? payload.listingTitle ?? "",
                    writer_is_seller: Boolean(payload.writer_is_seller ?? payload.writerIsSeller)
                  }
                })
              );
            }
            return;
          }

          if (notificationType !== "APPOINTMENT_ALARM") {
            return;
          }

          const appointmentId = String(payload.appointment_id ?? payload.appointmentId ?? "");
          if (!appointmentId || seenNotificationKeysRef.current.has(appointmentId)) {
            return;
          }

          seenNotificationKeysRef.current.add(appointmentId);
          setAppointmentReminder({
            appointmentId,
            chatRoomId:
              payload.chat_room_id != null || payload.chatRoomId != null
                ? String(payload.chat_room_id ?? payload.chatRoomId)
                : null,
            partnerNickname: payload.partner_nickname ?? payload.partnerNickname ?? "상대방",
            meetAt: payload.meet_at ?? payload.meetAt ?? ""
          });
        };

        const client = new Client({
          webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws-chat`),
          reconnectDelay: 5000,
          connectHeaders: { Authorization: `Bearer ${token}` },
          onConnect: () => {
            if (!disposed) {
              if (memberId && !memberSubscriptionRef.current) {
                memberSubscriptionRef.current = client.subscribe(
                  `/sub/members/${memberId}/chat-events`,
                  (frame: { body: string }) => {
                    const payload = JSON.parse(frame.body) as SocketMessage;
                    handleIncomingChatEvent(payload, undefined, true);
                  }
                );
              }

              if (memberId && !notificationSubscriptionRef.current) {
                notificationSubscriptionRef.current = client.subscribe(
                  `/sub/members/${memberId}/notifications`,
                  handleNotificationEvent
                );
              }

              syncSubscriptions(client as unknown as StompClientLike);
            }
          },
          onWebSocketClose: () => {
            if (!disposed) {
              subscriptionsRef.current.clear();
              memberSubscriptionRef.current = null;
              notificationSubscriptionRef.current = null;
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
      memberSubscriptionRef.current?.unsubscribe();
      memberSubscriptionRef.current = null;
      notificationSubscriptionRef.current?.unsubscribe();
      notificationSubscriptionRef.current = null;
      void stompRef.current?.deactivate();
      stompRef.current = null;
    };
  }, [handleIncomingChatEvent, memberId, token]);

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
          handleIncomingChatEvent(payload, roomId);
        });

        subscriptionsRef.current.set(roomId, subscription);
      });
    }
  }, [handleIncomingChatEvent, roomDirectory]);

  useEffect(() => {
    if (!token) {
      return;
    }

    const loadRooms = async () => {
      try {
        await refreshRooms();
      } catch (error) {
        console.error("[chat][notifications] failed to load rooms", error);
      }
    };

    void loadRooms();
  }, [refreshRooms, token]);

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
      roomsVersion,
      toast,
      appointmentReminder,
      dismissToast: () => setToast(null),
      dismissAppointmentReminder: () => setAppointmentReminder(null),
      registerRooms,
      registerRoom,
      markRoomRead
    }),
    [appointmentReminder, markRoomRead, registerRoom, registerRooms, roomsVersion, toast, totalUnreadCount, unreadCountByRoom]
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
