<template>
  <div v-if="embedded" class="identity-embedded">
    <div class="identity-card embedded profile-card">
      <template v-if="hasRole">
        <div class="profile-top">
          <div class="avatar-shell">
            <div class="avatar-container">
              <img
                :src="roleAvatar"
                :alt="roleInfo.name || '角色'"
                class="role-avatar"
                @error="handleAvatarError"
              />
            </div>
          </div>

          <div class="profile-main">
            <div class="profile-name-line">
              <h3 class="profile-name">{{ roleInfo.name || "未知角色" }}</h3>
              <n-tag
                v-if="roleInfo.legacy > 0"
                class="rank-pill"
                :style="{
                  color: '#fff',
                  backgroundColor: legacycolor[roleInfo.legacy]?.value,
                }"
                size="small"
                :bordered="false"
              >
                {{ legacycolor[roleInfo.legacy]?.name || "未知" }}
              </n-tag>
            </div>

            <div class="profile-metrics">
              <span class="level-text">Lv{{ roleInfo.level || 1 }}</span>
              <span class="separator-dot">·</span>
              <span class="power-value">战力 {{ formatPower(roleInfo.power) }}</span>
            </div>

            <div class="activity-badge" v-if="getCurrentActivityWeek">
              🔥 {{ getCurrentActivityWeek }}
            </div>
          </div>
        </div>

        <div class="core-resources">
          <div
            v-for="res in keyResList"
            :key="res.id"
            class="res-item stat-card"
            :class="`tone-${res.tone}`"
          >
            <div class="label-line">
              <span class="resource-icon" :class="`tone-${res.iconTone}`">
                <img :src="res.iconSrc" :alt="res.label" @error="onResourceIconError" />
              </span>
              <span class="label">{{ res.label }}</span>
            </div>
            <span class="value">{{ res.value }}</span>
          </div>
        </div>

        <div class="resources" :class="{ collapsed: !isExpanded }">
          <div v-for="res in detailResList" :key="res.label" class="res-item detail-item">
            <div class="detail-main">
              <span class="resource-icon detail" :class="`tone-${res.iconTone}`">
                <img
                  :src="res.iconSrc"
                  :alt="res.label"
                  :class="res.iconMask"
                  @error="onResourceIconError"
                />
              </span>
              <span class="label">{{ res.label }}</span>
            </div>
            <span class="value">{{ res.value }}</span>
          </div>
        </div>

        <div v-if="showExpand" class="resources-toggle">
          <n-button text @click="isExpanded = !isExpanded">
            {{ isExpanded ? "收起详情" : "展开全部资源" }}
          </n-button>
        </div>
      </template>

      <div v-else class="loading">正在获取角色信息...</div>
    </div>
  </div>

  <transition v-else name="drop">
    <div v-show="visible" class="identity-overlay" @click.self="emit('close')">
      <div class="identity-card floating-card">
        <div class="card-header">
          <img
            src="/icons/Ob7pyorzmHiJcbab2c25af264d0758b527bc1b61cc3b.png"
            alt="身份牌"
            class="icon"
          />
          <div class="info">
            <h3>身份牌</h3>
            <p>角色与战力概览</p>
          </div>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>

        <div v-if="hasRole" class="role-profile-header">
          <div class="role-profile-content">
            <div class="avatar-shell small">
              <div class="avatar-container small">
                <img
                  :src="roleAvatar"
                  :alt="roleInfo.name || '角色'"
                  class="role-avatar"
                  @error="handleAvatarError"
                />
              </div>
            </div>
            <div class="role-info-section">
              <div class="role-name">{{ roleInfo.name || "未知角色" }}</div>
              <div class="role-stats">
                <span>Lv{{ roleInfo.level || 1 }}</span>
                <span>战力 {{ formatPower(roleInfo.power) }}</span>
              </div>
              <div class="activity-week" v-if="getCurrentActivityWeek">
                🔥 {{ getCurrentActivityWeek }}
              </div>
            </div>
          </div>
        </div>

        <div v-else class="loading">正在获取角色信息...</div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { legacycolor as rawLegacyColor } from "@/utils/HeroList";

const legacycolor = rawLegacyColor as any;
const tokenStore = useTokenStore();
const getImgPath = (path: string) =>
  import.meta.env.BASE_URL + path.replace(/^\//, "");

const props = defineProps<{ visible?: boolean; embedded?: boolean }>();
const emit = defineEmits(["close"]);

const isExpanded = ref(false);

const wsStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  return tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
});

const roleInfo = computed(() => {
  const role = (tokenStore.gameData as any)?.roleInfo?.role;
  if (!role) return {} as any;
  return {
    roleId: role.roleId,
    name: role.name,
    headImg: role.headImg,
    level: role.level,
    power: role.power || role.fighting || 0,
    legacy: role.legacy?.color ?? 0,
    gold: role.gold ?? 0,
    diamond: role.diamond ?? 0,
    fishing: role.fishing || role.fish || null,
    items: role.items || role.itemList || role.bag?.items || role.inventory || null,
  };
});

const hasRole = computed(() => Object.keys(roleInfo.value || {}).length > 0);

const defaultAvatars = [
  "/icons/1733492491706148.png",
  "/icons/1733492491706152.png",
  "/icons/1736425783912140.png",
  "/icons/173746572831736.png",
  "/icons/174023274867420.png",
].map((path) => getImgPath(path));

const roleAvatar = ref("");
const selectedDefaultAvatar = ref("");

const formatPower = (power: number) => {
  if (!power) return "0";
  const yi = 100_000_000;
  const wan = 10_000;
  if (power >= yi) return (power / yi).toFixed(1) + "亿";
  if (power >= wan) return (power / wan).toFixed(1) + "万";
  return power.toLocaleString();
};

const formatNumber = (num: number) => {
  const n = Number(num || 0);
  const yi = 100_000_000;
  const wan = 10_000;
  if (n >= yi) return (n / yi).toFixed(1) + "亿";
  if (n >= wan) return (n / wan).toFixed(1) + "万";
  return n.toLocaleString();
};

const getItemCount = (items: any, id: number): number | null => {
  if (!items) return null;

  if (Array.isArray(items)) {
    const found = items.find((it) => Number((it as any).id ?? (it as any).itemId) === id);
    if (!found) return 0;
    return Number((found as any).num ?? (found as any).count ?? (found as any).quantity ?? 0);
  }

  const node = (items as any)[String(id)] ?? (items as any)[id];
  if (node == null) {
    const match = Object.values(items as any).find(
      (v: any) => Number(v?.itemId ?? v?.id) === id,
    );
    if (!match) return 0;
    return Number((match as any).num ?? (match as any).count ?? (match as any).quantity ?? 0);
  }

  if (typeof node === "number") return Number(node);
  if (typeof node === "object") {
    return Number((node as any).num ?? (node as any).count ?? (node as any).quantity ?? 0);
  }
  return Number(node) || 0;
};

const items = computed(() => (roleInfo.value as any).items);
const gold = computed(() => Number((roleInfo.value as any).gold ?? 0));
const diamond = computed(() => Number((roleInfo.value as any).diamond ?? 0));

const normalRodFromItems = computed(() => getItemCount(items.value, 1011));
const goldRodFromItems = computed(() => getItemCount(items.value, 1012));

const normalRod = computed(() => {
  const fromItems = normalRodFromItems.value;
  if (fromItems !== null && fromItems !== undefined) return fromItems;
  return (
    (roleInfo.value as any)?.fishing?.normalRod ??
    (roleInfo.value as any)?.fishing?.rod ??
    null
  );
});

const goldRod = computed(() => {
  const fromItems = goldRodFromItems.value;
  if (fromItems !== null && fromItems !== undefined) return fromItems;
  return (
    (roleInfo.value as any)?.fishing?.goldRod ??
    (roleInfo.value as any)?.fishing?.vipRod ??
    null
  );
});

const display = (n: number | null | undefined) => (n == null ? "-" : formatNumber(Number(n)));
const getRawValue = (n: number | null | undefined) => (n == null ? 0 : Number(n));

type ResourceIconMeta = {
  iconSrc: string;
  iconTone: string;
};

const DEFAULT_RESOURCE_ICON = getImgPath("/icons/ta.png");

const exactResourceIconMap: Record<number, string> = {
  1001: getImgPath("/icons/zml.png"),
  1003: getImgPath("/icons/resource-jinjie-stone.png"),
  1006: getImgPath("/icons/resource-jingtie.png"),
  10100: getImgPath("/icons/resource-four-saint-pearl.png"),
  10101: getImgPath("/icons/resource-four-saint-fragment.png"),
  1020: getImgPath("/icons/resource-skin-coin.png"),
  1021: getImgPath("/icons/resource-sweep-carpet.png"),
  1022: getImgPath("/icons/resource-white-jade.png"),
  1023: getImgPath("/icons/resource-color-jade.png"),
  10002: getImgPath("/icons/resource-four-saint-blue-jade.png"),
  10003: getImgPath("/icons/resource-four-saint-red-jade.png"),
  1033: getImgPath("/icons/resource-lingbei.png"),
  1034: getImgPath("/icons/resource-lingzhu.png"),
  1016: getImgPath("/icons/resource-dream-crystal.png"),
  1019: getImgPath("/icons/resource-salt-ingot.png"),
  1035: getImgPath("/icons/resource-gold-salt-ingot.png"),
  1007: getImgPath("/icons/resource-arena-ticket.png"),
  1008: getImgPath("/icons/resource-wood-torch.png"),
  1009: getImgPath("/icons/resource-bronze-torch.png"),
  1010: getImgPath("/icons/resource-divine-torch.png"),
  1014: getImgPath("/icons/resource-legion-coin.png"),
  1026: getImgPath("/icons/resource-wrench.png"),
  2101: getImgPath("/icons/resource-cheer-coin.png"),
  1013: getImgPath("/icons/resource-pearl.png"),
  1017: getImgPath("/icons/resource-revive-dan.png"),
  1011: getImgPath("/fish/ptyg.png"),
  1012: getImgPath("/fish/hjyg.png"),
  2001: getImgPath("/box/mzbx.png"),
  2002: getImgPath("/box/qtbx.png"),
  2003: getImgPath("/box/hjbx.png"),
  2004: getImgPath("/box/bjbx.png"),
  2005: getImgPath("/box/zsbx.png"),
  3001: getImgPath("/icons/resource-gold-coin-bag.png"),
  3002: getImgPath("/icons/resource-gold-brick-bag.png"),
  3005: getImgPath("/icons/resource-purple-random-fragment.png"),
  3006: getImgPath("/icons/resource-orange-random-fragment.png"),
  3007: getImgPath("/icons/resource-red-random-fragment.png"),
  3008: getImgPath("/icons/resource-jingtie-bag.png"),
  3009: getImgPath("/icons/resource-jinjie-stone-bag.png"),
  3010: getImgPath("/icons/resource-dream-bag.png"),
  3011: getImgPath("/icons/resource-white-jade-bag.png"),
  3012: getImgPath("/icons/resource-wrench-bag.png"),
  3201: getImgPath("/icons/resource-red-universal-fragment.png"),
  3302: getImgPath("/icons/resource-orange-universal-fragment.png"),
  35002: getImgPath("/icons/resource-refresh-ticket.png"),
  35009: getImgPath("/icons/resource-part.png"),
};

const resourceToneMap: Record<number, string> = {
  1001: "ticket",
  1003: "gem",
  1006: "tool",
  1007: "ticket",
  1008: "tool",
  1009: "tool",
  1010: "tool",
  1011: "fish",
  1012: "fish",
  1013: "pearl",
  1014: "badge",
  1016: "gem",
  1017: "potion",
  1019: "gem",
  1020: "ticket",
  1021: "ticket",
  1022: "gem",
  1023: "gem",
  1026: "tool",
  1033: "shell",
  1034: "gem",
  1035: "gem",
  10002: "gem",
  10003: "gem",
  10100: "gem",
  10101: "fragment",
  2001: "box",
  2002: "box",
  2003: "box",
  2004: "box",
  2005: "box",
  2101: "badge",
  3001: "bag",
  3002: "bag",
  3005: "fragment",
  3006: "fragment",
  3007: "fragment",
  3008: "bag",
  3009: "bag",
  3010: "bag",
  3011: "bag",
  3012: "bag",
  3020: "bag",
  3021: "bag",
  3201: "fragment",
  3302: "fragment",
  35002: "ticket",
  35009: "tool",
};

const coreResourceIconMap: Record<string, ResourceIconMeta> = {
  gold: { iconSrc: getImgPath("/icons/resource-gold-coin.png"), iconTone: "gold" },
  diamond: { iconSrc: getImgPath("/icons/resource-gold-brick.png"), iconTone: "diamond" },
  normalRod: { iconSrc: getImgPath("/fish/ptyg.png"), iconTone: "fish" },
  goldRod: { iconSrc: getImgPath("/fish/hjyg.png"), iconTone: "fish" },
};

const getResourceIconMeta = (resourceId?: number | null): ResourceIconMeta => {
  if (!resourceId) {
    return { iconSrc: DEFAULT_RESOURCE_ICON, iconTone: "default" };
  }
  return {
    iconSrc: exactResourceIconMap[resourceId] || DEFAULT_RESOURCE_ICON,
    iconTone: resourceToneMap[resourceId] || "default",
  };
};

const onResourceIconError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;
  if (!target) return;
  if (target.src !== DEFAULT_RESOURCE_ICON) {
    target.src = DEFAULT_RESOURCE_ICON;
  }
};

const iconMaskClassMap: Record<number, string> = {
  10003: "icon-mask-circle",
  10100: "icon-mask-circle",
  10101: "icon-mask-frag",
  3002: "icon-mask-bag",
  3005: "icon-mask-bag",
  3006: "icon-mask-bag",
  3007: "icon-mask-bag",
  3008: "icon-mask-bag",
  3009: "icon-mask-bag",
  3010: "icon-mask-bag",
  3011: "icon-mask-bag",
  3012: "icon-mask-bag",
  3201: "icon-mask-bag",
  3302: "icon-mask-bag",
  35002: "icon-mask-ticket",
  35009: "icon-mask-part",
};

const detailDefs = [
  { label: "珍珠", id: 1013 },
  { label: "复活丹", id: 1017 },
  { label: "招募令", id: 1001 },
  { label: "精铁", id: 1006 },
  { label: "彩玉", id: 1023 },
  { label: "进阶石", id: 1003 },
  { label: "白玉", id: 1022 },
  { label: "四圣蓝玉", id: 10002 },
  { label: "四圣红玉", id: 10003 },
  { label: "四圣宝珠", id: 10100 },
  { label: "四圣宝珠碎片", id: 10101 },
  { label: "金币袋子", id: 3001 },
  { label: "金砖福袋", id: 3002 },
  { label: "紫色随机碎片", id: 3005 },
  { label: "橙色随机碎片", id: 3006 },
  { label: "红色随机碎片", id: 3007 },
  { label: "精铁袋子", id: 3008 },
  { label: "进阶袋子", id: 3009 },
  { label: "梦魇袋子", id: 3010 },
  { label: "白玉袋子", id: 3011 },
  { label: "扳手袋子", id: 3012 },
  { label: "红色万能碎片", id: 3201 },
  { label: "橙色万能碎片", id: 3302 },
  { label: "盐锭", id: 1019 },
  { label: "晶石", id: 1016 },
  { label: "皮肤币", id: 1020 },
  { label: "扫荡魔毯", id: 1021 },
  { label: "灵贝", id: 1033 },
  { label: "灵珠", id: 1034 },
  { label: "金盐锭", id: 1035 },
  { label: "竞技场门票", id: 1007 },
  { label: "木制宝箱", id: 2001 },
  { label: "青铜宝箱", id: 2002 },
  { label: "黄金宝箱", id: 2003 },
  { label: "铂金宝箱", id: 2004 },
  { label: "钻石宝箱", id: 2005 },
  { label: "刷新券", id: 35002 },
  { label: "零件", id: 35009 },
  { label: "木柴火把", id: 1008 },
  { label: "青铜火把", id: 1009 },
  { label: "咸神火把", id: 1010 },
  { label: "军团币", id: 1014 },
  { label: "扳手", id: 1026 },
  { label: "助威币", id: 2101 },
];

const keyResList = computed(() => [
  {
    id: "gold",
    label: "金币",
    value: formatNumber(gold.value),
    iconSrc: coreResourceIconMap.gold.iconSrc,
    iconTone: coreResourceIconMap.gold.iconTone,
    tone: "gold",
  },
  {
    id: "diamond",
    label: "金砖",
    value: formatNumber(diamond.value),
    iconSrc: coreResourceIconMap.diamond.iconSrc,
    iconTone: coreResourceIconMap.diamond.iconTone,
    tone: "diamond",
  },
  {
    id: "normalRod",
    label: "普通鱼竿",
    value: display(normalRod.value as any),
    iconSrc: coreResourceIconMap.normalRod.iconSrc,
    iconTone: coreResourceIconMap.normalRod.iconTone,
    tone: "rod",
  },
  {
    id: "goldRod",
    label: "黄金鱼竿",
    value: display(goldRod.value as any),
    iconSrc: coreResourceIconMap.goldRod.iconSrc,
    iconTone: coreResourceIconMap.goldRod.iconTone,
    tone: "vip",
  },
]);

const detailResList = computed(() => {
  return detailDefs.map((def) => {
    const raw = getItemCount(items.value, def.id);
    const iconMeta = getResourceIconMeta(def.id);
    return {
      id: def.id,
      label: def.label,
      value: display(raw as any),
      iconSrc: iconMeta.iconSrc,
      iconTone: iconMeta.iconTone,
      iconMask: iconMaskClassMap[def.id] || "",
      raw: getRawValue(raw as any),
    };
  });
});

const showExpand = computed(() => detailResList.value.length > 8);

const getCurrentActivityWeek = computed(() => {
  const now = new Date();
  const start = new Date("2025-12-12T12:00:00");
  const weekDuration = 7 * 24 * 60 * 60 * 1000;
  const cycleDuration = 3 * weekDuration;
  const elapsed = now - start;
  if (elapsed < 0) return null;
  const cyclePosition = elapsed % cycleDuration;
  if (cyclePosition < weekDuration) return "黑市周";
  if (cyclePosition < 2 * weekDuration) return "招募周";
  return "宝箱周";
});

const initializeAvatar = () => {
  if (roleInfo.value && (roleInfo.value as any).headImg) {
    roleAvatar.value = (roleInfo.value as any).headImg;
    return;
  }

  if (!selectedDefaultAvatar.value) {
    const key = (roleInfo.value as any).roleId || (roleInfo.value as any).name || "default";
    const hash = Array.from(String(key)).reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
    selectedDefaultAvatar.value = defaultAvatars[hash % defaultAvatars.length] || defaultAvatars[0]!;
  }

  roleAvatar.value = selectedDefaultAvatar.value;
};

const handleAvatarError = () => {
  if (!selectedDefaultAvatar.value) {
    const idx = Math.floor(Math.random() * defaultAvatars.length);
    selectedDefaultAvatar.value = defaultAvatars[idx] || defaultAvatars[0]!;
  }
  roleAvatar.value = selectedDefaultAvatar.value;
};

onMounted(async () => {
  initializeAvatar();
  if (tokenStore.selectedToken && wsStatus.value === "connected") {
    try {
      await tokenStore.sendMessage(tokenStore.selectedToken.id, "role_getroleinfo");
    } catch {
      // silent
    }
  }
});

watch(
  () => roleInfo.value,
  () => initializeAvatar(),
  { deep: true },
);
</script>

<style scoped lang="scss">
.loading {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  height: 80px;
  line-height: 80px;
}

.identity-embedded {
  grid-column: 1 / -1;
}

.identity-card.embedded.profile-card {
  width: 100%;
  position: relative;
  border-radius: 28px;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.14),
    rgba(255, 255, 255, 0.06)
  );
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 18px 36px rgba(13, 18, 39, 0.22);
}

.identity-card.embedded.profile-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 88% -8%, rgba(111, 231, 255, 0.2), transparent 42%),
    radial-gradient(circle at 0% 100%, rgba(124, 108, 255, 0.16), transparent 38%);
}

.profile-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.avatar-shell {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  padding: 2px;
  background: linear-gradient(135deg, #7c6cff, #6fe7ff);
  box-shadow: 0 0 16px rgba(124, 108, 255, 0.55);
}

.avatar-shell.small {
  width: 66px;
  height: 66px;
}

.avatar-container {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(16, 21, 42, 0.55);
}

.avatar-container.small {
  width: 62px;
  height: 62px;
}

.role-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.68);
}

.profile-main {
  min-width: 0;
  flex: 1;
}

.profile-name-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-name {
  margin: 0;
  font-size: clamp(1.4rem, 2.8vw, 1.9rem);
  font-weight: 800;
  letter-spacing: 0.3px;
  background: linear-gradient(135deg, #ffffff, #b8f0ff 55%, #9cb5ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.rank-pill {
  border-radius: 999px;
  box-shadow: 0 0 12px rgba(124, 108, 255, 0.35);
}

.profile-metrics {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.84);
}

.level-text {
  font-size: 16px;
  font-weight: 700;
}

.separator-dot {
  opacity: 0.7;
}

.power-value {
  font-size: 18px;
  font-weight: 800;
  color: #d5f6ff;
  text-shadow: 0 0 12px rgba(143, 227, 255, 0.42);
}

.activity-badge {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #f7fbff;
  background: linear-gradient(
    135deg,
    rgba(124, 108, 255, 0.28),
    rgba(79, 70, 229, 0.2)
  );
  border: 1px solid rgba(156, 146, 255, 0.48);
  box-shadow: 0 0 16px rgba(124, 108, 255, 0.32);
}

.core-resources {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.res-item {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.stat-card {
  padding: 12px 14px;
  min-height: 82px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.14),
    rgba(255, 255, 255, 0.04)
  );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 10px 18px rgba(10, 15, 35, 0.16);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(143, 227, 255, 0.45);
  box-shadow: 0 10px 20px rgba(12, 16, 36, 0.24);
}

.stat-card .label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.76);
}

.label-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.resource-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent !important;
  border: none;
  box-shadow: none;
  line-height: 0;
}

.resource-icon img {
  width: 27px;
  height: 27px;
  object-fit: contain;
}

.resource-icon img.icon-mask-circle {
  clip-path: circle(39% at 50% 50%);
}

.resource-icon img.icon-mask-frag {
  clip-path: inset(9% round 16%);
}

.resource-icon img.icon-mask-bag {
  clip-path: inset(13% round 22%);
}

.resource-icon img.icon-mask-ticket {
  clip-path: polygon(14% 11%, 70% 8%, 90% 28%, 88% 78%, 60% 92%, 13% 86%, 6% 43%);
}

.resource-icon img.icon-mask-part {
  clip-path: polygon(17% 12%, 73% 12%, 91% 31%, 83% 88%, 21% 87%, 6% 54%, 8% 29%);
}

.resource-icon.detail {
  background: transparent !important;
}

.resource-icon.tone-gold {
  background: transparent !important;
}

.resource-icon.tone-diamond {
  background: transparent !important;
}

.resource-icon.tone-fish {
  background: transparent !important;
}

.resource-icon.tone-ticket {
  background: transparent !important;
}

.resource-icon.tone-gem {
  background: transparent !important;
}

.resource-icon.tone-potion {
  background: transparent !important;
}

.resource-icon.tone-shell {
  background: transparent !important;
}

.resource-icon.tone-fragment {
  background: transparent !important;
}

.resource-icon.tone-box {
  background: transparent !important;
}

.resource-icon.tone-bag {
  background: transparent !important;
}

.resource-icon.tone-tool {
  background: transparent !important;
}

.resource-icon.tone-badge {
  background: transparent !important;
}

.resource-icon.tone-default {
  background: transparent !important;
}

.stat-card .value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.1;
}

.stat-card.tone-gold .value {
  color: #ffe58f;
  text-shadow: 0 0 10px rgba(255, 229, 143, 0.4);
}

.stat-card.tone-diamond .value {
  color: #9fe7c3;
  text-shadow: 0 0 10px rgba(159, 231, 195, 0.4);
}

.stat-card.tone-rod .value {
  color: #8fd7ff;
  text-shadow: 0 0 10px rgba(143, 215, 255, 0.38);
}

.stat-card.tone-vip .value {
  color: #c4bcff;
  text-shadow: 0 0 10px rgba(196, 188, 255, 0.36);
}

.resources {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 8px;
  --res-item-height: 44px;
}

.resources.collapsed {
  max-height: calc((var(--res-item-height) + 8px) * 2 + 8px);
  overflow: hidden;
}

.detail-item {
  padding: 8px 10px;
  min-height: var(--res-item-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: rgba(255, 255, 255, 0.07);
}

.detail-main {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-item .label {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-item .value {
  color: rgba(255, 255, 255, 0.95);
  font-size: 13px;
  font-weight: 700;
}

.resources-toggle {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.resources-toggle :deep(.n-button) {
  color: rgba(199, 236, 255, 0.94);
  font-weight: 700;
}

.identity-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(2, 6, 23, 0.28);
}

.identity-card.floating-card {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  width: min(420px, calc(100vw - 24px));
  background: rgba(15, 23, 42, 0.92);
  border-radius: 18px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 20px 40px rgba(2, 6, 23, 0.38);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.info h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.info p {
  margin: 0;
  color: rgba(226, 232, 240, 0.76);
  font-size: 12px;
}

.close-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  color: rgba(241, 245, 249, 0.9);
  cursor: pointer;
  font-size: 16px;
}

.role-profile-header {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(30, 41, 59, 0.55);
  padding: 12px;
}

.role-profile-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-name {
  font-weight: 700;
}

.role-stats {
  margin-top: 4px;
  display: flex;
  gap: 10px;
  color: rgba(226, 232, 240, 0.8);
  font-size: 13px;
}

.activity-week {
  margin-top: 6px;
  font-size: 12px;
  color: #c7d2fe;
}

.drop-enter-from .floating-card {
  transform: translate(-50%, -120%);
}

.drop-enter-active .floating-card,
.drop-leave-active .floating-card {
  transition: transform 0.35s ease;
}

.drop-enter-to .floating-card {
  transform: translate(-50%, 0);
}

.drop-leave-to .floating-card {
  transform: translate(-50%, -120%);
}

@media (max-width: 768px) {
  .identity-card.embedded.profile-card {
    border-radius: 22px;
    padding: 14px;
  }

  .profile-top {
    align-items: flex-start;
    gap: 12px;
  }

  .avatar-shell {
    width: 66px;
    height: 66px;
  }

  .avatar-container {
    width: 62px;
    height: 62px;
  }

  .profile-name {
    font-size: 1.42rem;
  }

  .profile-metrics {
    font-size: 14px;
  }

  .power-value {
    font-size: 16px;
  }

  .core-resources {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-card {
    min-height: 76px;
  }

  .stat-card .value {
    font-size: 20px;
  }

  .resources {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
  }

  .detail-item {
    min-height: 40px;
    padding: 6px 8px;
  }

  .detail-item .label {
    font-size: 11px;
  }

  .detail-item .value {
    font-size: 12px;
  }
}
</style>
