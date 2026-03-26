export const isBrowser = () => typeof window !== "undefined";

export const getAndroidBridge = () => {
  if (!isBrowser()) {
    return undefined;
  }

  return window.AndroidBridge;
};

export const isAndroidWebView = () => Boolean(getAndroidBridge());
