"use client";

import { usePathname, useRouter, useSearchParams as useNextSearchParams } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

export type Location<State = unknown> = {
  pathname: string;
  search: string;
  hash: string;
  state: State | null;
};

type NavigateOptions = {
  replace?: boolean;
  state?: unknown;
};

const LocationOverrideContext = createContext<Location | null>(null);
const inMemoryState = new Map<string, unknown>();

function normalizePath(to: string) {
  if (typeof window === "undefined") {
    return to;
  }

  return new URL(to, window.location.origin).pathname + new URL(to, window.location.origin).search;
}

function writeState(path: string, state: unknown) {
  if (typeof state === "undefined") {
    return;
  }

  inMemoryState.set(path, state);
}

export function useLocation<State = unknown>(): Location<State> {
  const override = useContext(LocationOverrideContext);
  const pathname = usePathname() ?? "/";
  const searchParams = useNextSearchParams();
  const search = searchParams.size > 0 ? `?${searchParams.toString()}` : "";
  const path = `${pathname}${search}`;
  const [storedState, setStoredState] = useState<{ path: string; state: State | null }>(() => ({
    path,
    state: (inMemoryState.get(path) as State | undefined) ?? null
  }));
  const [hash, setHash] = useState("");

  useEffect(() => {
    setStoredState({
      path,
      state: ((inMemoryState.get(path) as State | undefined) ?? null) as State | null
    });
    setHash(window.location.hash);
  }, [path]);

  const currentState = storedState.path === path ? storedState.state : ((inMemoryState.get(path) as State | undefined) ?? null);

  const currentLocation = useMemo(
    () => ({
      pathname,
      search,
      hash,
      state: currentState
    }),
    [currentState, hash, pathname, search]
  );

  return (override ?? currentLocation) as Location<State>;
}

export function useNavigate() {
  const router = useRouter();

  return useCallback((to: string | number, options: NavigateOptions = {}) => {
    if (typeof to === "number") {
      window.history.go(to);
      return;
    }

    const path = normalizePath(to);
    writeState(path, options.state);

    if (options.replace) {
      router.replace(to);
      return;
    }

    router.push(to);
  }, [router]);
}

export function useParams(): Record<string, string | undefined> {
  const pathname = usePathname() ?? "/";
  const segments = pathname.split("/").filter(Boolean);

  if (segments[0] === "chatting" && segments[1]) {
    return { chatRoomId: decodeURIComponent(segments[1]) };
  }

  if (["listing", "trading", "my-listings", "wishlist"].includes(segments[0]) && segments[1]) {
    return { listingId: decodeURIComponent(segments[1]) };
  }

  return {};
}

export function useSearchParams(): [URLSearchParams] {
  const searchParams = useNextSearchParams();

  return useMemo(() => [new URLSearchParams(searchParams.toString())], [searchParams]);
}

export function Navigate({
  to,
  replace = false
}: {
  to: string;
  replace?: boolean;
}) {
  const navigate = useNavigate();

  useEffect(() => {
    navigate(to, { replace });
  }, [navigate, replace, to]);

  return null;
}

export function LocationOverrideProvider({
  location,
  children
}: {
  location: Location | null;
  children: ReactNode;
}) {
  return <LocationOverrideContext.Provider value={location}>{children}</LocationOverrideContext.Provider>;
}
