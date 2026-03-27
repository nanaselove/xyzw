export const isBrowser = () => typeof window !== "undefined";

export const getAndroidBridge = () => {
  if (!isBrowser()) {
    return undefined;
  }

  return window.AndroidBridge;
};

const isAndroidWebViewByUserAgent = () => {
  if (!isBrowser()) {
    return false;
  }

  const userAgent = navigator.userAgent || "";
  if (!/Android/i.test(userAgent)) {
    return false;
  }

  // Android WebView 常见特征是带 wv/Version 标记，普通 Android 浏览器通常没有。
  return /\bwv\b/i.test(userAgent) || /Version\/\d+\.\d+/i.test(userAgent);
};

export const isAndroidWebView = () =>
  Boolean(getAndroidBridge()) || isAndroidWebViewByUserAgent();

const isAbsoluteOrSpecialUrl = (value: string) =>
  /^(?:[a-zA-Z][a-zA-Z\d+\-.]*:|\/\/|#)/.test(value);

export const getAssetUrl = (value: string) => {
  const trimmed = value.trim();

  if (!trimmed || isAbsoluteOrSpecialUrl(trimmed)) {
    return trimmed;
  }

  const normalized = trimmed.replace(/^\/+/, "");

  if (isAndroidAssetPage()) {
    return `file:///android_asset/www/${normalized}`;
  }

  return `/${normalized}`;
};

export const isAndroidAssetPage = () => {
  if (!isAndroidWebView() || !isBrowser()) {
    return false;
  }

  return window.location.href.startsWith("file:///android_asset/");
};
