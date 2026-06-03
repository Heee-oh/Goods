import { getAccessToken } from "./auth";
import { getApiBaseUrl } from "./config";
import {
  MEMBER_REGION_VERIFICATION_FAILED_CODE,
  REGION_VERIFICATION_EXPIRED_CODE,
  REGION_VERIFICATION_EXPIRED_EVENT
} from "./regionVerification";

const API_BASE_URL = getApiBaseUrl();

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
  return isRecord(value) && "success" in value && ("data" in value || "error" in value);
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
  const code = isRecord(payload) && typeof payload.code === "string" ? payload.code : undefined;
  const message =
    code === MEMBER_REGION_VERIFICATION_FAILED_CODE
      ? "현재 지역이 아닙니다."
      : extractMessage(payload, fallbackMessage);
  return new ApiError(message, status, { code, fieldErrors });
}

function emitGlobalApiError(error: ApiError) {
  if (error.code !== REGION_VERIFICATION_EXPIRED_CODE || typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(
    new CustomEvent(REGION_VERIFICATION_EXPIRED_EVENT, {
      detail: {
        code: REGION_VERIFICATION_EXPIRED_CODE,
        message: error.message,
        status: error.status
      }
    })
  );
}

function throwApiError(payload: unknown, status: number, fallbackMessage: string): never {
  const error = toApiError(payload, status, fallbackMessage);
  emitGlobalApiError(error);
  throw error;
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
      throwApiError(errorPayload.error, response.status, "요청에 실패했습니다.");
    }
    throwApiError(errorPayload, response.status, "요청에 실패했습니다.");
  }

  if (response.status === 204) {
    return null as T;
  }

  const payload = await readJsonResponse(response);
  if (isApiEnvelope<T>(payload)) {
    if (!payload.success) {
      throwApiError(payload.error, response.status, "요청에 실패했습니다.");
    }
    return payload.data as T;
  }

  return payload as T;
}
