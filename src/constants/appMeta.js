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

export function getClubTitle(gameData) {
  const clubName = gameData?.legionInfo?.info?.name?.trim();
  if (!clubName) {
    return APP_NAME;
  }

  const serverId = Number(gameData?.legionInfo?.info?.serverId || 0);
  if (!serverId) {
    return clubName;
  }

  const displayServerId = serverId > 27 ? serverId - 27 : serverId;
  return `✨${displayServerId} ${clubName}✨`;
}

export function buildPageTitle(pageTitle, suffix = APP_NAME) {
  const normalizedPageTitle =
    typeof pageTitle === "string" ? pageTitle.trim() : "";
  const normalizedSuffix =
    typeof suffix === "string" && suffix.trim() ? suffix.trim() : APP_NAME;
  return normalizedPageTitle
    ? `${normalizedPageTitle}${TITLE_SEPARATOR}${normalizedSuffix}`
    : normalizedSuffix;
}
