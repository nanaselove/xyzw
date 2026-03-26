import { isAndroidWebView } from "./env";

const ASSET_PREFIX = "file:///android_asset/www/";

const isAbsoluteOrSpecialUrl = (value: string) =>
  /^(?:[a-zA-Z][a-zA-Z\d+\-.]*:|\/\/|#)/.test(value);

const rewriteCssUrls = (value: string) =>
  value.replace(/url\((['"]?)(\/[^'")]+)\1\)/g, (_, quote, url) => {
    return `url(${quote}${rewriteAssetUrl(url)}${quote})`;
  });

const rewriteAssetUrl = (value: string) => {
  const trimmed = value.trim();

  if (!trimmed || isAbsoluteOrSpecialUrl(trimmed) || !trimmed.startsWith("/")) {
    return trimmed;
  }

  return `${ASSET_PREFIX}${trimmed.replace(/^\/+/, "")}`;
};

const rewriteAttributeValue = (name: string, value: string) => {
  const lowerName = name.toLowerCase();

  if (
    lowerName === "src" ||
    lowerName === "href" ||
    lowerName === "poster" ||
    lowerName === "data" ||
    lowerName === "fallback-src" ||
    lowerName === "xlink:href"
  ) {
    return rewriteAssetUrl(value);
  }

  if (lowerName === "style") {
    return rewriteCssUrls(value);
  }

  return value;
};

export const installAndroidAssetRuntimeFix = () => {
  if (
    !isAndroidWebView() ||
    typeof window === "undefined" ||
    typeof document === "undefined"
  ) {
    return;
  }

  const globalWindow = window as Window & {
    __xyzwAndroidAssetRuntimeInstalled?: boolean;
  };

  if (globalWindow.__xyzwAndroidAssetRuntimeInstalled) {
    return;
  }

  globalWindow.__xyzwAndroidAssetRuntimeInstalled = true;

  const imageDescriptor = Object.getOwnPropertyDescriptor(
    HTMLImageElement.prototype,
    "src",
  );
  const elementSetAttribute = Element.prototype.setAttribute;
  const elementSetAttributeNS = Element.prototype.setAttributeNS;
  const styleSetProperty = CSSStyleDeclaration.prototype.setProperty;

  if (imageDescriptor?.set && imageDescriptor.get) {
    Object.defineProperty(HTMLImageElement.prototype, "src", {
      configurable: true,
      enumerable: imageDescriptor.enumerable,
      get: imageDescriptor.get,
      set(value: string) {
        imageDescriptor.set?.call(this, rewriteAssetUrl(String(value)));
      },
    });
  }

  Element.prototype.setAttribute = function (name: string, value: string) {
    return elementSetAttribute.call(
      this,
      name,
      rewriteAttributeValue(name, String(value)),
    );
  };

  Element.prototype.setAttributeNS = function (
    namespaceURI: string | null,
    name: string,
    value: string,
  ) {
    return elementSetAttributeNS.call(
      this,
      namespaceURI,
      name,
      rewriteAttributeValue(name, String(value)),
    );
  };

  CSSStyleDeclaration.prototype.setProperty = function (
    propertyName: string,
    value?: string | null,
    priority?: string,
  ) {
    return styleSetProperty.call(
      this,
      propertyName,
      typeof value === "string" ? rewriteCssUrls(value) : value,
      priority,
    );
  };

  const rewriteNode = (node: Node) => {
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }

    const element = node as Element;
    for (const attr of Array.from(element.attributes)) {
      const rewritten = rewriteAttributeValue(attr.name, attr.value);
      if (rewritten !== attr.value) {
        element.setAttribute(attr.name, rewritten);
      }
    }
  };

  rewriteNode(document.documentElement);

  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === "childList") {
        mutation.addedNodes.forEach((node) => rewriteNode(node));
      }
    }
  });

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true,
  });
};
