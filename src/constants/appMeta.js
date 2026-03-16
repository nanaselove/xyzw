const DEFAULT_APP_NAME = "XYZW 游戏管理系统";
const TITLE_SEPARATOR = " - ";

const rawDocumentTitle =
  typeof document !== "undefined" ? document.title.trim() : "";
const separatorIndex = rawDocumentTitle.lastIndexOf(TITLE_SEPARATOR);
const inferredAppName =
  separatorIndex >= 0
    ? rawDocumentTitle.slice(separatorIndex + TITLE_SEPARATOR.length).trim()
    : rawDocumentTitle;

export const APP_NAME = inferredAppName || DEFAULT_APP_NAME;

export function buildPageTitle(pageTitle) {
  const normalizedPageTitle =
    typeof pageTitle === "string" ? pageTitle.trim() : "";
  return normalizedPageTitle
    ? `${normalizedPageTitle}${TITLE_SEPARATOR}${APP_NAME}`
    : APP_NAME;
}
