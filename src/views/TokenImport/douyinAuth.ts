export const APP_AUTH_API_BASES = [
  "/api/app-auth",
  "/api/taptap-auth",
  "/api/mobile-auth",
] as const;

export const buildAuthUrlCandidates = (paths: string[]) => {
  const normalizedPaths = paths
    .map((path) => path?.trim())
    .filter((path): path is string => !!path);

  const urls: string[] = [];
  for (const base of APP_AUTH_API_BASES) {
    for (const path of normalizedPaths) {
      const normalizedPath = path.startsWith("/") ? path : `/${path}`;
      urls.push(`${base}${normalizedPath}`);
    }
  }

  return urls;
};

export type DouyinImportMethod = "dyPhone";

export interface DouyinImportRole {
  id: string;
  name: string;
  token: string;
  server: string;
  wsUrl: string;
  avatar?: string;
  sourceUrl?: string;
  importMethod: DouyinImportMethod;
}

type UnknownRecord = Record<string, any>;

const stringValue = (value: unknown) => {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value.trim();
  if (typeof value === "number") return String(value);
  return "";
};

const pickString = (...values: unknown[]) => {
  for (const value of values) {
    const normalized = stringValue(value);
    if (normalized) return normalized;
  }
  return "";
};

const safeIdPart = (value: string) => {
  const sanitized = value.replace(/[^\w-]/g, "");
  return sanitized || "unknown";
};

const unwrapPayload = (payload: unknown): UnknownRecord => {
  if (!payload || typeof payload !== "object") return {};
  const asRecord = payload as UnknownRecord;
  if (asRecord.data && typeof asRecord.data === "object") {
    return asRecord.data as UnknownRecord;
  }
  return asRecord;
};

const normalizeRoleName = (
  template: string | undefined,
  defaultNamePrefix: string,
  roleSource: UnknownRecord,
  server: string,
  index: number,
) => {
  const fallbackName =
    pickString(
      roleSource.name,
      roleSource.roleName,
      roleSource.nickName,
      roleSource.nickname,
      roleSource.characterName,
    ) || `${defaultNamePrefix}${index + 1}`;

  if (!template) {
    return fallbackName;
  }

  const roleId =
    pickString(
      roleSource.roleId,
      roleSource.id,
      roleSource.uid,
      roleSource.userId,
      roleSource.openId,
    ) || String(index + 1);

  const replaced = template
    .replace(/{name}/g, fallbackName)
    .replace(/{id}/g, roleId)
    .replace(/{index}/g, String(index + 1))
    .replace(/{server}/g, server || "未知服")
    .trim();

  return replaced || fallbackName;
};

const buildRoleId = (
  importMethod: DouyinImportMethod,
  roleSource: UnknownRecord,
  server: string,
  index: number,
) => {
  const stableId = pickString(
    roleSource.id,
    roleSource.roleId,
    roleSource.uid,
    roleSource.userId,
    roleSource.openId,
    roleSource.characterId,
  );

  if (stableId) {
    return `dy_${importMethod}_${safeIdPart(stableId)}_${safeIdPart(server || "default")}`;
  }

  return `dy_${importMethod}_${Date.now()}_${index}_${Math.random()
    .toString(36)
    .slice(2, 8)}`;
};

const normalizeSingleRole = (
  roleSource: UnknownRecord,
  rootData: UnknownRecord,
  importMethod: DouyinImportMethod,
  nameTemplate: string | undefined,
  defaultNamePrefix: string,
  index: number,
): DouyinImportRole | null => {
  const token = pickString(
    roleSource.token,
    roleSource.roleToken,
    roleSource.gameToken,
    roleSource.accessToken,
  );

  if (!token) {
    return null;
  }

  const server = pickString(
    roleSource.server,
    roleSource.serverName,
    rootData.server,
    rootData.serverName,
  );
  const wsUrl = pickString(roleSource.wsUrl, rootData.wsUrl);
  const sourceUrl = pickString(
    roleSource.sourceUrl,
    roleSource.refreshUrl,
    rootData.sourceUrl,
    rootData.refreshUrl,
  );

  return {
    id: buildRoleId(importMethod, roleSource, server, index),
    name: normalizeRoleName(
      nameTemplate,
      defaultNamePrefix,
      roleSource,
      server,
      index,
    ),
    token,
    server,
    wsUrl,
    avatar: pickString(roleSource.avatar, roleSource.headImg, rootData.avatar) || undefined,
    sourceUrl: sourceUrl || undefined,
    importMethod,
  };
};

export const hasDouyinTokenPayload = (payload: unknown) => {
  const data = unwrapPayload(payload);
  const roleLists = [data.roles, data.roleList, data.accounts, data.tokens];

  const hasRoleToken = roleLists.some(
    (roleList) =>
      Array.isArray(roleList) &&
      roleList.some(
        (role) =>
          pickString(role?.token, role?.roleToken, role?.gameToken, role?.accessToken),
      ),
  );

  return (
    hasRoleToken ||
    !!pickString(data.token, data.roleToken, data.gameToken, data.accessToken)
  );
};

export const extractDouyinRoles = (
  payload: unknown,
  options: {
    importMethod: DouyinImportMethod;
    nameTemplate?: string;
    defaultNamePrefix?: string;
  },
) => {
  const data = unwrapPayload(payload);
  const nestedCandidates = [
    data,
    unwrapPayload(data.result),
    unwrapPayload(data.payload),
  ].filter((candidate) => Object.keys(candidate).length > 0);

  let rawRoles: UnknownRecord[] = [];
  for (const candidate of nestedCandidates) {
    const roleList = [candidate.roles, candidate.roleList, candidate.accounts, candidate.tokens].find(
      (list) => Array.isArray(list) && list.length > 0,
    ) as UnknownRecord[] | undefined;
    if (roleList && roleList.length) {
      rawRoles = roleList;
      break;
    }
  }

  if (!rawRoles.length) {
    const singleRoleSource = nestedCandidates.find((candidate) =>
      pickString(candidate.token, candidate.roleToken, candidate.gameToken, candidate.accessToken),
    );
    if (singleRoleSource) {
      rawRoles = [singleRoleSource];
    }
  }

  if (!rawRoles.length) {
    return [];
  }

  const defaultNamePrefix = options.defaultNamePrefix || "App角色";
  const nameTemplate = options.nameTemplate?.trim() || undefined;

  return rawRoles
    .map((role, index) =>
      normalizeSingleRole(
        role,
        data,
        options.importMethod,
        nameTemplate,
        defaultNamePrefix,
        index,
      ),
    )
    .filter((role): role is DouyinImportRole => !!role);
};

const extractErrorMessage = (payload: unknown) => {
  const data = unwrapPayload(payload);
  return pickString(
    data.message,
    data.msg,
    data.errMsg,
    data.error,
    data.meta?.errMsg,
  );
};

const extractBusinessError = (payload: unknown) => {
  const data = unwrapPayload(payload);
  if (data.success === false) {
    return extractErrorMessage(payload) || "请求失败";
  }
  if (typeof data.errCode === "number" && data.errCode !== 0) {
    return extractErrorMessage(payload) || `请求失败(errCode=${data.errCode})`;
  }
  if (
    data.meta &&
    typeof data.meta.errCode === "number" &&
    data.meta.errCode !== 0
  ) {
    return extractErrorMessage(payload) || `请求失败(errCode=${data.meta.errCode})`;
  }
  return "";
};

export const requestJson = async <T = unknown>(
  url: string,
  init: RequestInit = {},
) => {
  const headers = new Headers(init.headers || {});
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, { ...init, headers });
  const rawText = await response.text();
  let data: unknown = {};

  if (rawText) {
    try {
      data = JSON.parse(rawText);
    } catch {
      data = { message: rawText };
    }
  }

  if (!response.ok) {
    const error = new Error(
      extractErrorMessage(data) ||
        `请求失败: ${response.status} ${response.statusText}`,
    );
    (error as Error & { status?: number }).status = response.status;
    (error as Error & { url?: string }).url = url;
    throw error;
  }

  const businessError = extractBusinessError(data);
  if (businessError) {
    const error = new Error(businessError);
    (error as Error & { status?: number }).status = response.status;
    (error as Error & { url?: string }).url = url;
    throw error;
  }

  return data as T;
};

export const requestJsonWithFallback = async <T = unknown>(
  urls: string[],
  init: RequestInit = {},
) => {
  const triedUrls: string[] = [];
  let lastError: Error | null = null;

  for (const url of urls) {
    triedUrls.push(url);
    try {
      return await requestJson<T>(url, init);
    } catch (error: any) {
      lastError = error;
      const statusCode = Number(error?.status);
      if (statusCode !== 404) {
        throw error;
      }
    }
  }

  if (lastError) {
    const errorMessage = [
      lastError.message || "请求失败",
      "可用接口未找到（404）",
      `已尝试: ${triedUrls.join(" , ")}`,
      "请检查后端路由或代理配置（开发环境可配置 APP_AUTH_PROXY_TARGET 或 TAPTAP_AUTH_PROXY_TARGET）",
    ].join("；");
    const error = new Error(errorMessage);
    (error as Error & { status?: number }).status = 404;
    throw error;
  }

  throw new Error("未提供可用请求地址");
};
