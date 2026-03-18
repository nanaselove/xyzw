<template>
  <div class="status-card team-formation-card">
    <div class="card-header">
      <div class="header-info">
        <img
          src="/icons/Ob7pyorzmHiJcbab2c25af264d0758b527bc1b61cc3b.png"
          alt="阵容"
          class="icon"
        />
        <div class="info">
          <h3>阵容</h3>
          <p>当前使用的战斗阵容</p>
        </div>
      </div>

      <div class="team-selector">
        <button
          v-for="teamId in availableTeams"
          :key="teamId"
          :disabled="loading || switching"
          :class="['team-button', { active: currentTeam === teamId }]"
          @click="selectTeam(teamId)"
        >
          {{ teamId }}
        </button>
        <button
          class="refresh-button"
          :disabled="loading"
          title="刷新队伍数据"
          @click="refreshTeamData(true)"
        >
          <svg
            class="refresh-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
            <path d="M21 3v5h-5" />
            <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
            <path d="M3 21v-5h5" />
          </svg>
          <span class="refresh-text">刷新</span>
        </button>
      </div>
    </div>

    <div class="card-content">
      <div class="current-team-info">
        <span class="label">当前阵容</span>
        <span class="team-number">
          <template v-if="!loading">阵容 {{ currentTeam }}</template>
          <template v-else>加载中...</template>
        </span>
      </div>

      <div class="heroes-container">
        <div v-if="!loading" class="heroes-formation">
          <div class="formation-row front-row">
            <div
              v-for="(hero, index) in frontRowSlots"
              :key="hero?.id ? `front-${hero.id}-${index}` : `front-slot-${index}`"
              class="hero-slot"
              :class="{ filled: !!hero }"
            >
              <div class="hero-circle" :class="{ active: !!hero }">
                <img
                  v-if="hero?.avatar"
                  :src="hero.avatar"
                  :alt="hero.name"
                  class="hero-avatar"
                />
                <div v-else class="hero-placeholder">
                  {{ hero?.name?.substring(0, 2) || index + 1 }}
                </div>
              </div>
              <span class="hero-name">{{ hero?.name || "空位" }}</span>
            </div>
          </div>

          <div class="formation-row back-row">
            <div
              v-for="(hero, index) in backRowSlots"
              :key="hero?.id ? `back-${hero.id}-${index}` : `back-slot-${index}`"
              class="hero-slot"
              :class="{ filled: !!hero }"
            >
              <div class="hero-circle" :class="{ active: !!hero }">
                <img
                  v-if="hero?.avatar"
                  :src="hero.avatar"
                  :alt="hero.name"
                  class="hero-avatar"
                />
                <div v-else class="hero-placeholder">
                  {{ hero?.name?.substring(0, 2) || index + 3 }}
                </div>
              </div>
              <span class="hero-name">{{ hero?.name || "空位" }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-team">
          <p>正在加载队伍信息…</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { useMessage } from "naive-ui";
import { HERO_DICT } from "@/utils/HeroList.js";

const tokenStore = useTokenStore();
const message = useMessage();

const loading = ref(false);
const switching = ref(false);
const currentTeam = ref(1);
const availableTeams = ref<number[]>([1, 2, 3, 4]);



const wsStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  return tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
});

const presetTeamRaw = computed(() => tokenStore.gameData?.presetTeam ?? null);

function normalizePresetTeam(raw: any) {
  if (!raw)
    return {
      useTeamId: 1,
      teams: {} as Record<number, { teamInfo: Record<string, any> }>,
    };
  const root = raw.presetTeamInfo ?? raw;
  const findUseIdRec = (obj: any): number | null => {
    if (!obj || typeof obj !== "object") return null;
    if (typeof obj.useTeamId === "number") return obj.useTeamId;
    for (const k of Object.keys(obj)) {
      const v = findUseIdRec(obj[k]);
      if (v) return v;
    }
    return null;
  };
  const useTeamId =
    root.useTeamId ?? root.presetTeamInfo?.useTeamId ?? findUseIdRec(root) ?? 1;

  const dict = root.presetTeamInfo ?? root;
  const teams: Record<number, { teamInfo: Record<string, any> }> = {};
  const ids = Object.keys(dict || {}).filter((k) => /^\d+$/.test(k));
  for (const idStr of ids) {
    const id = Number(idStr);
    const node = dict[idStr];
    if (!node) {
      teams[id] = { teamInfo: {} };
      continue;
    }
    if (node.teamInfo) {
      teams[id] = { teamInfo: node.teamInfo };
    } else if (node.heroes) {
      const ti: Record<string, any> = {};
      node.heroes.forEach((h: any, idx: number) => {
        ti[String(idx + 1)] = h;
      });
      teams[id] = { teamInfo: ti };
    } else if (typeof node === "object") {
      const hasHero = Object.values(node).some(
        (v: any) => v && typeof v === "object" && "heroId" in v,
      );
      teams[id] = { teamInfo: hasHero ? node : {} };
    } else {
      teams[id] = { teamInfo: {} };
    }
  }
  return { useTeamId: Number(useTeamId) || 1, teams };
}

const presetTeam = computed(() => normalizePresetTeam(presetTeamRaw.value));

const currentTeamHeroes = computed(() => {
  const team = (presetTeam.value.teams as any)[currentTeam.value]?.teamInfo;
  if (!team) return [] as any[];
  const heroes: any[] = [];
  for (const [pos, hero] of Object.entries(team)) {
    const hid = (hero as any)?.heroId ?? (hero as any)?.id;
    if (!hid) continue;
    const meta = HERO_DICT[Number(hid)];
    const avatarPath = meta?.avatar;
    const fullAvatarPath = avatarPath
      ? import.meta.env.BASE_URL + avatarPath.replace(/^\//, "")
      : undefined;
    heroes.push({
      id: Number(hid),
      name: meta?.name ?? `英雄${hid}`,
      type: meta?.type ?? "",
      position: Number(pos),
      level: (hero as any)?.level ?? 1,
      avatar: fullAvatarPath,
    });
  }
  heroes.sort((a, b) => a.position - b.position);
  return heroes;
});

const formationSlots = computed(() => {
  const heroes = currentTeamHeroes.value.slice(0, 5);
  return Array.from({ length: 5 }, (_, index) => heroes[index] || null);
});

const frontRowSlots = computed(() => formationSlots.value.slice(0, 2));
const backRowSlots = computed(() => formationSlots.value.slice(2, 5));

const executeGameCommand = async (
  tokenId: string | number,
  cmd: string,
  params = {},
  description = "",
  timeout = 8000,
) => {
  try {
    return await tokenStore.sendMessageWithPromise(
      String(tokenId),
      cmd,
      params,
      timeout,
    );
  } catch (error: any) {
    if (description)
      message.error(`${description}失败：${error?.message ?? error}`);
    throw error;
  }
};

const getTeamInfoWithCache = async (force = false) => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return null;
  }
  const tokenId = tokenStore.selectedToken.id;
  if (!force) {
    const cached = (tokenStore.gameData as any)?.presetTeam?.presetTeamInfo;
    if (cached) return cached;
  }
  loading.value = true;
  try {
    const result = await executeGameCommand(
      tokenId,
      "presetteam_getinfo",
      {},
      "获取阵容信息",
    );
    tokenStore.$patch((state: any) => {
      state.gameData = { ...(state.gameData ?? {}), presetTeam: result };
    });
    return result?.presetTeamInfo ?? null;
  } catch (e) {
    console.error("获取阵容信息失败:", e);
    return null;
  } finally {
    loading.value = false;
  }
};

const updateAvailableTeams = () => {
  const ids = Object.keys(presetTeam.value.teams)
    .map(Number)
    .filter((n) => !Number.isNaN(n))
    .sort((a, b) => a - b);
  availableTeams.value = ids.length ? ids : [1, 2, 3, 4];
};
const updateCurrentTeam = () => {
  currentTeam.value = (presetTeam.value as any).useTeamId || 1;
};

const selectTeam = async (teamId: number) => {
  if (switching.value || loading.value) return;
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }
  const prev = currentTeam.value;
  switching.value = true;
  try {
    await executeGameCommand(
      tokenStore.selectedToken.id,
      "presetteam_saveteam",
      { teamId },
      `切换到阵容 ${teamId}`,
    );
    currentTeam.value = teamId;
    message.success(`已切换到阵容 ${teamId}`);
    await refreshTeamData(true);
  } catch (e) {
    currentTeam.value = prev;
  } finally {
    switching.value = false;
  }
};

const refreshTeamData = async (force = false) => {
  await getTeamInfoWithCache(force);
};

onMounted(async () => {
  if (tokenStore.selectedToken && wsStatus.value === "connected") {
    await refreshTeamData(false);
    updateAvailableTeams();
    updateCurrentTeam();
    if (!presetTeamRaw.value) {
      await refreshTeamData(true);
      updateAvailableTeams();
      updateCurrentTeam();
    }
  }
});

watch(wsStatus, (newStatus, oldStatus) => {
  if (
    newStatus === "connected" &&
    oldStatus !== "connected" &&
    tokenStore.selectedToken
  ) {
    setTimeout(async () => {
      await refreshTeamData(false);
      updateAvailableTeams();
      updateCurrentTeam();
      if (!presetTeamRaw.value) {
        await refreshTeamData(true);
        updateAvailableTeams();
        updateCurrentTeam();
      }
    }, 1000);
  }
});

watch(
  () => tokenStore.selectedToken,
  async (newToken, oldToken) => {
    if (newToken && newToken.id !== (oldToken as any)?.id) {
      const status = tokenStore.getWebSocketStatus(newToken.id);
      if (status === "connected") {
        await refreshTeamData(true);
        updateAvailableTeams();
        updateCurrentTeam();
      }
    }
  },
);

watch(
  () => presetTeamRaw.value,
  () => {
    updateAvailableTeams();
    updateCurrentTeam();
  },
  { deep: true },
);
</script>

<style scoped lang="scss">
.team-formation-card {
  min-height: 260px;
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.12),
    rgba(255, 255, 255, 0.05)
  );
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 22px;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
  flex-shrink: 0;
}

.info h3 {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 800;
}

.info p {
  margin: 0;
  color: var(--text-secondary, rgba(255, 255, 255, 0.72));
  font-size: 12px;
}

.team-selector {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.team-button {
  min-width: 42px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.84);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.team-button:hover {
  transform: translateY(-1px);
  border-color: rgba(143, 227, 255, 0.55);
  box-shadow: 0 8px 14px rgba(11, 17, 36, 0.2);
}

.team-button.active {
  color: #f8fcff;
  border-color: rgba(124, 108, 255, 0.62);
  background: linear-gradient(135deg, #6b8dff 0%, #7c6cff 100%);
  box-shadow: 0 8px 16px rgba(124, 108, 255, 0.36);
}

.team-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-button {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.refresh-button:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(143, 227, 255, 0.55);
  color: rgba(255, 255, 255, 0.98);
  transform: translateY(-1px);
}

.refresh-button:active {
  transform: translateY(0);
}

.refresh-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.refresh-icon {
  width: 14px;
  height: 14px;
  transition: transform 0.2s ease;
}

.refresh-button:not(:disabled):hover .refresh-icon {
  transform: rotate(180deg);
}

.refresh-button:disabled .refresh-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

.card-content .current-team-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.card-content .label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
}

.card-content .team-number {
  font-size: 1.08rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.96);
}

.heroes-container {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 13, 32, 0.3);
  padding: 12px;
  min-height: 180px;
}

.heroes-formation {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.formation-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.hero-circle {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 2px solid rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 6px 14px rgba(8, 10, 22, 0.28);
}

.hero-circle.active {
  border-color: rgba(124, 108, 255, 0.65);
  box-shadow:
    0 0 0 3px rgba(124, 108, 255, 0.18),
    0 0 14px rgba(124, 108, 255, 0.48);
}

.hero-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-placeholder {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.8);
}

.hero-name {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.front-row .hero-slot,
.back-row .hero-slot {
  width: 86px;
}

.hero-slot.filled .hero-name {
  color: rgba(255, 255, 255, 0.94);
}

.empty-team {
  min-height: 144px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .header-info {
    justify-content: center;
  }

  .team-selector {
    justify-content: center;
  }

  .team-button {
    min-width: 40px;
    height: 32px;
  }

  .heroes-container {
    padding: 10px;
    min-height: 168px;
  }

  .heroes-formation {
    gap: 10px;
  }

  .formation-row {
    gap: 10px;
  }

  .front-row .hero-slot,
  .back-row .hero-slot {
    width: 76px;
  }

  .hero-circle {
    width: 54px;
    height: 54px;
  }

  .hero-name {
    max-width: 74px;
  }
}

@media (max-width: 420px) {
  .hero-circle {
    width: 48px;
    height: 48px;
  }

  .hero-name {
    font-size: 10px;
    max-width: 64px;
  }

  .front-row .hero-slot,
  .back-row .hero-slot {
    width: 68px;
  }
}
</style>

