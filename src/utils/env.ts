export const isBrowser = () => typeof window !== "undefined";

export const getAndroidBridge = () => {
  if (!isBrowser()) {
    return undefined;
  }

  return window.AndroidBridge;
};

export const isAndroidWebView = () => Boolean(getAndroidBridge());

export const isAndroidAssetPage = () => {
  if (!isAndroidWebView() || !isBrowser()) {
    return false;
  }

  return window.location.href.startsWith("file:///android_asset/");
};
