import { getAccessToken } from "./auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type RequestOptions = RequestInit & {
  auth?: boolean;
};

export type ApiFieldError = {
  field: string;
  message: string;
};

type ApiErrorPayload = {
  code?: unknown;
  message?: unknown;
  fieldErrors?: unknown;
};

type ApiEnvelope<T = unknown> = {
  success?: unknown;
  data?: T;
  error?: ApiErrorPayload | null;
  timestamp?: unknown;
  path?: unknown;
};

export class ApiError extends Error {
  status: number;
  code?: string;
  fieldErrors?: ApiFieldError[];

  constructor(message: string, status: number, options: { code?: string; fieldErrors?: ApiFieldError[] } = {}) {
    super(message);
    this.status = status;
    this.code = options.code;
    this.fieldErrors = options.fieldErrors;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isApiEnvelope<T = unknown>(value: unknown): value is ApiEnvelope<T> {
  return isRecord(value) && "success" in value && "data" in value;
}

function toFieldErrors(value: unknown): ApiFieldError[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const fieldErrors = value
    .map((item) => {
      if (!isRecord(item)) {
        return null;
      }

      const field = typeof item.field === "string" ? item.field : "";
      const message = typeof item.message === "string" ? item.message : "";
      if (!field || !message) {
        return null;
      }

      return { field, message };
    })
    .filter((item): item is ApiFieldError => item !== null);

  return fieldErrors.length > 0 ? fieldErrors : undefined;
}

function extractMessage(payload: unknown, fallback: string) {
  if (!isRecord(payload)) {
    return fallback;
  }

  if (typeof payload.message === "string" && payload.message.trim().length > 0) {
    return payload.message;
  }

  if (typeof payload.error === "string" && payload.error.trim().length > 0) {
    return payload.error;
  }

  if (isRecord(payload.error) && typeof payload.error.message === "string" && payload.error.message.trim().length > 0) {
    return payload.error.message;
  }

  return fallback;
}

function toApiError(payload: unknown, status: number, fallbackMessage: string) {
  const fieldErrors = isRecord(payload) ? toFieldErrors(payload.fieldErrors) : undefined;
  const message = extractMessage(payload, fallbackMessage);
  const code = isRecord(payload) && typeof payload.code === "string" ? payload.code : undefined;
  return new ApiError(message, status, { code, fieldErrors });
}

async function readJsonResponse(response: Response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers(options.headers);
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  const isBlob = typeof Blob !== "undefined" && options.body instanceof Blob;
  const isArrayBuffer = typeof ArrayBuffer !== "undefined" && options.body instanceof ArrayBuffer;

  if (!isFormData && !isBlob && !isArrayBuffer) {
    headers.set("Content-Type", "application/json");
  }

  if (options.auth !== false) {
    const token = getAccessToken();
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    const errorPayload = await readJsonResponse(response);
    if (isApiEnvelope(errorPayload) && errorPayload.error) {
      throw toApiError(errorPayload.error, response.status, "요청에 실패했습니다.");
    }
    throw toApiError(errorPayload, response.status, "요청에 실패했습니다.");
  }

  if (response.status === 204) {
    return null as T;
  }

  const payload = await readJsonResponse(response);
  if (isApiEnvelope<T>(payload)) {
    if (!payload.success) {
      throw toApiError(payload.error, response.status, "요청에 실패했습니다.");
    }
    return payload.data as T;
  }

  return payload as T;
}
