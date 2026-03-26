import { getAndroidBridge, isAndroidWebView } from "./env";

export interface NativeTextRequestOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: string;
  timeoutMs?: number;
}

export interface NativeTextResponse {
  status: number;
  body: string;
  url?: string;
  headers?: Record<string, string>;
  error?: string;
}

type PendingRequest = {
  resolve: (response: NativeTextResponse) => void;
  reject: (reason: unknown) => void;
  timer: number | null;
};

const DEFAULT_TIMEOUT_MS = 15000;
const pendingRequests = new Map<string, PendingRequest>();

const normalizeHeaders = (headers?: Record<string, string>) => {
  const normalized: Record<string, string> = {};

  Object.entries(headers ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }

    const trimmedKey = key.trim();
    if (!trimmedKey) {
      return;
    }

    normalized[trimmedKey] = String(value);
  });

  return normalized;
};

const createRequestId = () => {
  const crypto = globalThis.crypto as Crypto | undefined;

  if (crypto?.randomUUID) {
    return crypto.randomUUID();
  }

  return `native_${Date.now()}_${Math.random().toString(36).slice(2)}`;
};

const ensureNativeReceiver = () => {
  if (typeof window === "undefined") {
    return;
  }

  const globalWindow = window as Window & {
    __xyzwNativeHttpReceive?: (requestId: string, payloadJson: string) => void;
  };

  if (globalWindow.__xyzwNativeHttpReceive) {
    return;
  }

  globalWindow.__xyzwNativeHttpReceive = (requestId, payloadJson) => {
    const pending = pendingRequests.get(requestId);
    if (!pending) {
      return;
    }

    pendingRequests.delete(requestId);
    if (pending.timer !== null) {
      window.clearTimeout(pending.timer);
    }

    try {
      const response = JSON.parse(payloadJson) as NativeTextResponse;
      if (response?.error && response.status < 0) {
        pending.reject(new Error(response.error));
        return;
      }

      pending.resolve(response);
    } catch (error) {
      pending.reject(error instanceof Error ? error : new Error(String(error)));
    }
  };
};

const requestTextInBrowser = (
  url: string,
  options: NativeTextRequestOptions,
) => {
  const method = (options.method || "GET").toUpperCase();
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;

  return new Promise<NativeTextResponse>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url, true);
    xhr.timeout = timeoutMs;

    Object.entries(options.headers ?? {}).forEach(([key, value]) => {
      if (value === undefined || value === null) {
        return;
      }

      try {
        xhr.setRequestHeader(key, String(value));
      } catch {
        // Browsers may block some restricted headers; ignore them.
      }
    });

    xhr.onload = () => {
      resolve({
        status: xhr.status,
        body: xhr.responseText,
        url: xhr.responseURL || url,
      });
    };
    xhr.onerror = () => reject(new Error("Network request failed"));
    xhr.ontimeout = () => reject(new Error("Network request timed out"));
    xhr.send(options.body ?? null);
  });
};

const requestTextInAndroid = (
  url: string,
  options: NativeTextRequestOptions,
) => {
  const bridge = getAndroidBridge();
  if (!bridge?.requestText) {
    throw new Error("AndroidBridge.requestText is not available");
  }

  ensureNativeReceiver();

  const requestId = createRequestId();
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;

  return new Promise<NativeTextResponse>((resolve, reject) => {
    const timer = window.setTimeout(() => {
      pendingRequests.delete(requestId);
      reject(new Error("Network request timed out"));
    }, timeoutMs + 1000);

    pendingRequests.set(requestId, {
      resolve,
      reject,
      timer,
    });

    try {
      bridge.requestText(
        requestId,
        url,
        (options.method || "GET").toUpperCase(),
        JSON.stringify(normalizeHeaders(options.headers)),
        options.body ?? "",
        timeoutMs,
      );
    } catch (error) {
      pendingRequests.delete(requestId);
      window.clearTimeout(timer);
      reject(error instanceof Error ? error : new Error(String(error)));
    }
  });
};

export const requestText = (
  url: string,
  options: NativeTextRequestOptions = {},
) => {
  if (isAndroidWebView()) {
    return requestTextInAndroid(url, options);
  }

  return requestTextInBrowser(url, options);
};
