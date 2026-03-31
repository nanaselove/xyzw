<template>
  <MyCard class="lineup-saver" :statusClass="{ active: state.isRunning }">
    <template #icon>
      <img
        src="/icons/Ob7pyorzmHiJcbab2c25af264d0758b527bc1b61cc3b.png"
        alt="阵容图标"
      />
    </template>
    <template #title>
      <h3>阵容助手</h3>
      <p>保存阵容、快速切换</p>
    </template>
    <template #badge>
      <span>{{ state.isRunning ? "运行中" : "已停止" }}</span>
    </template>
    <template #default>
      <div class="lineup-container">
        <div class="toolbar">
          <n-button
            class="toolbar-btn"
            :class="{ 'is-refreshing': loading }"
            type="primary"
            size="small"
            @click="refreshTeamInfo"
            :disabled="!!activeLineupKey || state.isRunning || loading"
          >
            {{ loading ? "刷新中..." : "刷新数据" }}
          </n-button>
          <n-button
            class="toolbar-btn"
            size="small"
            @click="saveCurrentLineup"
            :disabled="
              editingHeroes.length === 0 || !!activeLineupKey || state.isRunning
            "
          >
            保存阵容
          </n-button>
          <n-button
            class="toolbar-btn"
            type="success"
            size="small"
            @click="openAddHeroModal"
            :disabled="
              editingHeroes.length >= 5 || !!activeLineupKey || state.isRunning
            "
          >
            上阵英雄
          </n-button>
          <n-button
            class="toolbar-btn"
            type="info"
            size="small"
            @click="savedLineupsModalVisible = true"
            :disabled="!!activeLineupKey || state.isRunning"
          >
            已保存阵容 ({{ savedLineups.length }})
          </n-button>
        </div>

        <div class="quick-switch-section">
          <h4>阵容槽位</h4>
          <div
            class="team-selector"
            :style="{
              gridTemplateColumns: `repeat(${teamSelectorColumns}, minmax(0, 1fr))`,
            }"
          >
            <n-button
              class="team-slot-btn"
              v-for="teamId in availableTeams"
              :key="teamId"
              :type="currentTeamId === teamId ? 'primary' : 'default'"
              size="small"
              @click="switchTeam(teamId)"
              :loading="switchingTeamId === teamId"
              :disabled="!!activeLineupKey || state.isRunning"
            >
              阵容{{ teamId }}
            </n-button>
          </div>
        </div>

        <div class="current-team-section" v-if="currentTeamInfo">
          <h4>
            编辑阵容 (阵容槽位{{ currentTeamId }})
            <span class="drag-tip">拖拽调整站位</span>
          </h4>
          <div class="heroes-grid">
            <div
              v-for="(hero, index) in editingHeroes"
              :key="hero.heroId + '-' + hero.position"
              class="hero-item"
              :class="{
                dragging: draggedHeroId === hero.heroId,
                'drag-over': dragOverPosition === hero.position,
              }"
              draggable="true"
              @dragstart="onDragStart($event, hero)"
              @dragend="onDragEnd"
              @dragover.prevent="onDragOver($event, hero)"
              @dragleave="onDragLeave"
              @drop="onDrop($event, hero)"
            >
              <div class="hero-position">{{ hero.position + 1 }}</div>
              <div class="hero-avatar" @click="showHeroRefineModal(hero)">
                <img
                  v-if="getHeroAvatar(hero.heroId)"
                  :src="getHeroAvatar(hero.heroId)"
                  :alt="getHeroName(hero.heroId)"
                />
                <div v-else class="hero-placeholder">
                  {{ getHeroName(hero.heroId)?.substring(0, 2) || "?" }}
                </div>
              </div>
              <div class="hero-info" @click="showHeroRefineModal(hero)">
                <div class="hero-name">
                  {{ getHeroName(hero.heroId) || `武将${hero.heroId}` }}
                </div>
              </div>
              <n-button
                class="exchange-btn"
                size="tiny"
                type="warning"
                @click.stop="openExchangeModal(hero)"
              >
                更换
              </n-button>
              <n-button
                class="remove-btn"
                size="tiny"
                type="error"
                @click.stop="removeHero(hero)"
              >
                下阵
              </n-button>
            </div>
          </div>
        </div>
      </div>

      <n-modal
        v-model:show="savedLineupsModalVisible"
        preset="card"
        title="已保存的阵容"
        style="width: 800px; max-width: 90vw"
        :bordered="false"
      >
        <div class="saved-lineups-modal-toolbar">
          <div class="saved-lineups-modal-hint">
            可以导出当前 Token 的阵容备份，或从 JSON 文件恢复。
          </div>
          <div class="saved-lineups-modal-actions">
            <n-button
              size="small"
              secondary
              @click="exportSavedLineups"
              :disabled="
                savedLineups.length === 0 ||
                !tokenStore.selectedToken ||
                !!activeLineupKey ||
                state.isRunning
              "
            >
              导出备份
            </n-button>
            <n-button
              size="small"
              type="primary"
              secondary
              @click="openSavedLineupsImportDialog"
              :disabled="
                !tokenStore.selectedToken ||
                !!activeLineupKey ||
                state.isRunning
              "
            >
              导入备份
            </n-button>
          </div>
        </div>
        <div v-if="activeLineupKey" class="saved-lineups-progress">
          <div class="saved-lineups-progress-head">
            <span>正在应用阵容</span>
            <span>{{ applyProgress.percent }}%</span>
          </div>
          <n-progress
            type="line"
            :percentage="applyProgress.percent"
            :show-indicator="false"
          />
          <div class="saved-lineups-progress-text">
            {{ applyProgress.text }}
          </div>
        </div>
        <input
          ref="lineupImportInput"
          class="saved-lineups-import-input"
          type="file"
          accept=".json,application/json"
          @change="handleSavedLineupsImport"
        />
        <div v-if="savedLineups.length === 0" class="empty-tip">
          暂无保存的阵容，点击"保存阵容"开始使用
        </div>
        <div v-else class="saved-lineups-modal-content">
          <div class="team-tabs">
            <div
              v-for="teamId in availableTeams"
              :key="teamId"
              class="team-tab"
              :class="{ active: selectedTeamTab === teamId }"
              @click="selectedTeamTab = teamId"
            >
              槽位{{ teamId }}
              <span class="tab-count"
                >({{ getLineupsByTeamId(teamId).length }})</span
              >
            </div>
          </div>
          <div class="lineups-list">
            <div
              v-for="lineup in getLineupsByTeamId(selectedTeamTab)"
              :key="getLineupKey(lineup)"
              class="lineup-card"
            >
              <div class="lineup-title-bar" @click="toggleLineupExpand(lineup)">
                <div class="lineup-title-left">
                  <span class="expand-icon">{{
                    expandedLineup === lineup ? "▼" : "▶"
                  }}</span>
                  <span class="lineup-name">{{ lineup.name }}</span>
                  <span class="lineup-time">{{
                    formatTime(lineup.savedAt)
                  }}</span>
                </div>
                <div class="lineup-quick-actions">
                  <n-button
                    type="primary"
                    size="tiny"
                    @click.stop="applyLineup(lineup)"
                    :loading="lineup.applying"
                    :disabled="
                      lineup.teamId !== currentTeamId ||
                      !!activeLineupKey ||
                      state.isRunning
                    "
                  >
                    应用
                  </n-button>
                </div>
              </div>
              <div v-if="expandedLineup === lineup" class="lineup-detail">
                <div class="lineup-heroes-detail">
                  <div
                    v-for="(hero, hIdx) in lineup.heroes"
                    :key="hIdx"
                    class="lineup-hero-item"
                  >
                    <span class="hero-pos">{{ hero.position + 1 }}.</span>
                    <span class="hero-name">{{
                      getHeroName(hero.heroId) || `武将${hero.heroId}`
                    }}</span>
                    <span
                      class="hero-artifact"
                      v-if="hero.artifactId && hero.artifactId !== -1"
                    >
                      (装备:{{ hero.artifactId }})
                    </span>
                  </div>
                </div>
                <div class="lineup-actions">
                  <n-button
                    size="small"
                    @click="renameLineup(lineup)"
                    :disabled="!!activeLineupKey || state.isRunning"
                  >
                    重命名
                  </n-button>
                  <n-button
                    type="error"
                    size="small"
                    @click="deleteLineup(lineup)"
                    :disabled="!!activeLineupKey || state.isRunning"
                  >
                    删除
                  </n-button>
                </div>
              </div>
            </div>
            <div
              v-if="getLineupsByTeamId(selectedTeamTab).length === 0"
              class="no-lineup-tip"
            >
              暂无保存的阵容
            </div>
          </div>
        </div>
      </n-modal>

      <n-modal
        v-model:show="refineModalVisible"
        preset="card"
        :title="refineModalTitle"
        style="width: 600px; max-width: 90vw"
        :bordered="false"
      >
        <n-spin :show="refineModalLoading">
          <div v-if="selectedHeroEquipment" class="refine-modal-content">
            <div
              v-for="partId in [1, 2, 3, 4]"
              :key="partId"
              class="equip-refine-section"
            >
              <div class="equip-header">
                <span class="equip-name">{{ partMap[partId] }}</span>
                <span class="equip-level">
                  Lv.{{ selectedHeroEquipment[partId]?.level || 0 }}
                </span>
                <span class="equip-bonus" v-if="selectedHeroEquipment[partId]">
                  +{{ getEquipBonus(partId) }}
                  {{ partId === 1 ? "攻击" : partId === 3 ? "防御" : "血量" }}
                </span>
              </div>
              <div class="slots-container">
                <div
                  v-for="slot in getEquipSlots(partId)"
                  :key="slot.id"
                  class="slot-item"
                  :class="{
                    locked: slot.isLocked,
                    [`color-${slot.colorId}`]: slot.colorId > 0,
                  }"
                >
                  <span class="slot-label">孔{{ slot.id }}</span>
                  <div v-if="slot.attrId" class="slot-attr">
                    <span class="attr-name">{{
                      getAttrName(slot.attrId)
                    }}</span>
                    <span class="attr-value">+{{ slot.attrNum }}%</span>
                  </div>
                  <div v-else class="slot-empty">未淬炼</div>
                  <n-tag v-if="slot.isLocked" size="small" type="warning"
                    >锁定</n-tag
                  >
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-equipment">暂无装备数据</div>
        </n-spin>
      </n-modal>

      <n-modal
        v-model:show="exchangeModalVisible"
        preset="card"
        :title="exchangeMode === 'add' ? '上阵英雄' : '更换武将'"
        style="width: 700px; max-width: 90vw"
        :bordered="false"
      >
        <div class="exchange-modal-content">
          <div v-if="exchangeMode === 'exchange'" class="current-hero-info">
            <span>当前武将：</span>
            <n-tag type="primary" size="large">
              {{
                getHeroName(exchangeHero?.heroId) ||
                `武将${exchangeHero?.heroId}`
              }}
            </n-tag>
          </div>
          <div v-else class="current-hero-info">
            <span>上阵位置：</span>
            <n-tag type="success" size="large">
              位置 {{ getFirstEmptySlot() + 1 }}
            </n-tag>
          </div>
          <n-input
            v-model:value="heroSearchKeyword"
            placeholder="搜索武将名称..."
            clearable
            style="margin-bottom: 12px"
          />
          <div class="hero-filter-section">
            <div class="filter-label">品质：</div>
            <div class="filter-tags">
              <n-tag
                v-for="q in heroQualities"
                :key="q"
                :type="selectedQuality === q ? 'primary' : 'default'"
                :bordered="false"
                style="cursor: pointer; margin-right: 8px"
                @click="selectedQuality = selectedQuality === q ? '全部' : q"
              >
                {{ q }}
              </n-tag>
            </div>
          </div>
          <div class="hero-filter-section">
            <div class="filter-label">国家：</div>
            <div class="filter-tags">
              <n-tag
                v-for="t in heroCountries"
                :key="t"
                :type="selectedCountry === t ? 'primary' : 'default'"
                :bordered="false"
                style="cursor: pointer; margin-right: 8px"
                @click="selectedCountry = selectedCountry === t ? '全部' : t"
              >
                {{ t }}
              </n-tag>
            </div>
          </div>
          <n-spin :show="exchangeLoading">
            <div class="hero-select-grid">
              <div
                v-for="hero in filteredHeroList"
                :key="hero.id"
                class="hero-select-item"
                :class="{
                  selected: exchangeTargetHeroId === hero.id,
                  'quality-red': hero.quality === '红将',
                  'quality-orange': hero.quality === '橙将',
                  'quality-purple': hero.quality === '紫将',
                }"
                @click="selectExchangeHero(hero)"
              >
                <div class="hero-select-avatar">
                  <img v-if="hero.avatar" :src="hero.avatar" :alt="hero.name" />
                  <div v-else class="hero-placeholder">
                    {{ hero.name?.substring(0, 2) || "?" }}
                  </div>
                </div>
                <div class="hero-select-name">{{ hero.name }}</div>
                <div class="hero-select-tags">
                  <n-tag
                    size="small"
                    :bordered="false"
                    :type="
                      hero.quality === '红将'
                        ? 'error'
                        : hero.quality === '橙将'
                          ? 'warning'
                          : hero.quality === '紫将'
                            ? 'info'
                            : 'default'
                    "
                  >
                    {{ hero.quality }}
                  </n-tag>
                  <n-tag size="small" :bordered="false" type="default">
                    {{ hero.type }}
                  </n-tag>
                </div>
              </div>
            </div>
          </n-spin>
        </div>
        <template #footer>
          <div style="display: flex; justify-content: flex-end; gap: 8px">
            <n-button @click="exchangeModalVisible = false">取消</n-button>
            <n-button
              type="primary"
              @click="confirmHeroAction"
              :loading="exchangeLoading"
              :disabled="!exchangeTargetHeroId"
            >
              {{ exchangeMode === "add" ? "确认上阵" : "确认更换" }}
            </n-button>
          </div>
        </template>
      </n-modal>
    </template>
  </MyCard>
</template>

<script setup>
import { ref, computed, onMounted, watch, h } from "vue";
import { useMessage, useDialog, NInput } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";
import MyCard from "../Common/MyCard.vue";
import { HERO_DICT } from "@/utils/HeroList.js";
import { saveBlob } from "@/utils/nativeExport";

const tokenStore = useTokenStore();
const message = useMessage();
const dialog = useDialog();

const normalizeArtifactId = (artifactId) => {
  const value = Number(artifactId);
  return Number.isFinite(value) && value > 0 ? value : null;
};

const loading = ref(false);
const switchingTeamId = ref(null);
const currentTeamId = ref(1);
const availableTeams = ref([1, 2, 3, 4, 5, 6]);
const currentTeamInfo = ref(null);
const presetTeamData = ref(null);
const savedLineups = ref([]);
const allHeroesData = ref({});
const roleHeroesData = ref({});
const editingTeamHeroes = ref({});

const state = ref({
  isRunning: false,
});

const refineModalVisible = ref(false);
const refineModalLoading = ref(false);
const refineModalTitle = ref("");
const selectedHeroEquipment = ref(null);

const exchangeModalVisible = ref(false);
const exchangeLoading = ref(false);
const exchangeMode = ref("exchange");
const exchangeHero = ref(null);
const exchangeTargetHeroId = ref(null);
const heroSearchKeyword = ref("");
const selectedQuality = ref("全部");
const selectedCountry = ref("全部");
const savedLineupsModalVisible = ref(false);
const selectedTeamTab = ref(1);
const expandedLineup = ref(null);
const lineupImportInput = ref(null);
const activeLineupKey = ref(null);
const applyProgress = ref({
  visible: false,
  text: "",
  percent: 0,
});

const draggedHeroId = ref(null);
const dragOverPosition = ref(null);

const STORAGE_KEY = "saved_lineups";

const partMap = {
  1: "武器",
  2: "铠甲",
  3: "头冠",
  4: "坐骑",
};

const attrMap = {
  1: "攻击",
  2: "血量",
  3: "防御",
  4: "速度",
  5: "破甲",
  6: "破甲抵抗",
  7: "精准",
  8: "格挡",
  9: "减伤",
  10: "暴击",
  11: "暴击抵抗",
  12: "爆伤",
  13: "爆伤抵抗",
  14: "技能伤害",
  15: "免控",
  16: "眩晕免疫",
  17: "冰冻免疫",
  18: "沉默免疫",
  19: "流血免疫",
  20: "中毒免疫",
  21: "灼烧免疫",
};

const heroQualities = computed(() => {
  return ["全部", "红将", "橙将", "紫将"];
});

const heroCountries = computed(() => {
  const countries = new Set(["全部"]);
  Object.values(HERO_DICT).forEach((hero) => {
    if (hero.type) countries.add(hero.type);
  });
  return Array.from(countries);
});

const teamSelectorColumns = computed(() => {
  return Math.max(1, availableTeams.value.length);
});

const getHeroQuality = (heroId) => {
  const prefix = Math.floor(heroId / 100);
  if (prefix === 1) return "红将";
  if (prefix === 2) return "橙将";
  if (prefix === 3) return "紫将";
  return "其他";
};

const allHeroList = computed(() => {
  const heroes = Object.entries(roleHeroesData.value).map(([id, hero]) => {
    const heroInfo = HERO_DICT[hero.heroId] || {};
    return {
      id: Number(hero.heroId),
      name: heroInfo.name || `武将${hero.heroId}`,
      type: heroInfo.type || "未知",
      avatar: heroInfo.avatar || null,
      quality: getHeroQuality(Number(hero.heroId)),
      artifactId: normalizeArtifactId(hero.artifactId),
      heroData: hero,
    };
  });

  const countryOrder = { 魏国: 1, 蜀国: 2, 吴国: 3, 群雄: 4 };
  const qualityOrder = { 红将: 1, 橙将: 2, 紫将: 3, 其他: 4 };

  return heroes.sort((a, b) => {
    const countryA = countryOrder[a.type] || 99;
    const countryB = countryOrder[b.type] || 99;
    if (countryA !== countryB) return countryA - countryB;

    const qualityA = qualityOrder[a.quality] || 99;
    const qualityB = qualityOrder[b.quality] || 99;
    if (qualityA !== qualityB) return qualityA - qualityB;

    return a.id - b.id;
  });
});

const filteredHeroList = computed(() => {
  let list = allHeroList.value;

  if (selectedQuality.value !== "全部") {
    list = list.filter((hero) => hero.quality === selectedQuality.value);
  }

  if (selectedCountry.value !== "全部") {
    list = list.filter((hero) => hero.type === selectedCountry.value);
  }

  if (heroSearchKeyword.value) {
    const keyword = heroSearchKeyword.value.toLowerCase();
    list = list.filter((hero) => hero.name.toLowerCase().includes(keyword));
  }

  return list;
});

const currentTeamHeroes = computed(() => {
  if (!currentTeamInfo.value) return [];
  const teamInfo = currentTeamInfo.value;
  return Object.entries(teamInfo)
    .map(([key, hero]) => ({
      position: hero?.battleTeamSlot ?? Number(key),
      heroId: hero?.heroId || hero?.id,
      artifactId: normalizeArtifactId(hero?.artifactId),
    }))
    .filter((h) => h.heroId)
    .sort((a, b) => a.position - b.position);
});

const editingHeroes = computed(() => {
  if (Object.keys(editingTeamHeroes.value).length > 0) {
    return Object.entries(editingTeamHeroes.value)
      .sort((a, b) => Number(a[0]) - Number(b[0]))
      .map(([pos, hero]) => ({
        position: Number(pos),
        heroId: hero?.heroId,
        artifactId: normalizeArtifactId(hero?.artifactId),
      }))
      .filter((h) => h.heroId);
  }
  return currentTeamHeroes.value;
});

const getFirstEmptySlot = () => {
  for (let i = 0; i < 5; i++) {
    const hero = editingHeroes.value.find((h) => h.position === i);
    if (!hero) return i;
  }
  return 0;
};

const getLineupsByTeamId = (teamId) => {
  return savedLineups.value.filter((lineup) => lineup.teamId === teamId);
};

const getLineupKey = (lineup) =>
  `${lineup?.teamId ?? "team"}-${lineup?.savedAt ?? "time"}-${lineup?.name ?? ""}`;

const findLineupIndex = (lineup) => {
  const key = getLineupKey(lineup);
  return savedLineups.value.findIndex((item) => getLineupKey(item) === key);
};

const toggleLineupExpand = (lineup) => {
  if (activeLineupKey.value) return;
  if (expandedLineup.value === lineup) {
    expandedLineup.value = null;
  } else {
    expandedLineup.value = lineup;
  }
};

const hasEditingChanges = computed(() => {
  if (Object.keys(editingTeamHeroes.value).length === 0) return false;
  const current = JSON.stringify(
    currentTeamHeroes.value
      .map((h) => `${h.position}:${h.heroId}`)
      .sort()
      .join(","),
  );
  const editing = JSON.stringify(
    editingHeroes.value
      .map((h) => `${h.position}:${h.heroId}`)
      .sort()
      .join(","),
  );
  return current !== editing;
});

const getHeroName = (heroId) => {
  if (!heroId) return null;
  return HERO_DICT[heroId]?.name || null;
};

const getHeroAvatar = (heroId) => {
  if (!heroId) return null;
  return HERO_DICT[heroId]?.avatar || null;
};

const formatTime = (timestamp) => {
  if (!timestamp) return "";
  const date = new Date(timestamp);
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, "0")}`;
};

const getAttrName = (attrId) => {
  return attrMap[attrId] || `属性${attrId}`;
};

const getEquipBonus = (partId) => {
  if (!selectedHeroEquipment.value || !selectedHeroEquipment.value[partId]) {
    return 0;
  }
  const equip = selectedHeroEquipment.value[partId];
  const bonusType =
    partId === 1
      ? "quenchAttackExt"
      : partId === 3
        ? "quenchDefenseExt"
        : "quenchHpExt";
  return equip[bonusType] || 0;
};

const getEquipSlots = (partId) => {
  if (!selectedHeroEquipment.value || !selectedHeroEquipment.value[partId]) {
    return [];
  }
  const quenches = selectedHeroEquipment.value[partId].quenches || {};
  const slotList = [];
  const slotKeys = Object.keys(quenches).sort((a, b) => Number(a) - Number(b));

  for (const key of slotKeys) {
    const slotId = Number(key);
    const slot = quenches[key];
    slotList.push({
      id: slotId,
      attrId: slot.attrId || null,
      attrNum: slot.attrNum || 0,
      isLocked: slot.isLocked || slot.locked || false,
      colorId: slot.colorId || 0,
    });
  }

  return slotList;
};

const showHeroRefineModal = async (hero) => {
  refineModalTitle.value = `${getHeroName(hero.heroId) || `武将${hero.heroId}`} - 装备洗练`;
  refineModalVisible.value = true;
  refineModalLoading.value = true;
  selectedHeroEquipment.value = null;

  const token = tokenStore.selectedToken;
  if (!token) {
    message.warning("请先选择Token");
    refineModalLoading.value = false;
    return;
  }

  const tokenId = token.id;
  const status = tokenStore.getWebSocketStatus(tokenId);
  if (status !== "connected") {
    message.error("WebSocket未连接，无法获取装备信息");
    refineModalLoading.value = false;
    return;
  }

  try {
    const heroData = allHeroesData.value[String(hero.heroId)];
    if (heroData?.equipment) {
      selectedHeroEquipment.value = heroData.equipment;
    } else {
      const roleInfo = await tokenStore.sendMessageWithPromise(
        tokenId,
        "role_getroleinfo",
        {},
      );
      const role = roleInfo?.role || roleInfo;
      const heroes = role?.heroes || {};
      allHeroesData.value = heroes;

      const currentHero = heroes[String(hero.heroId)];
      selectedHeroEquipment.value = currentHero?.equipment || null;
    }

    if (!selectedHeroEquipment.value) {
      message.warning("未找到该武将的装备数据");
    }
  } catch (error) {
    message.error(`获取装备信息失败: ${error.message}`);
  } finally {
    refineModalLoading.value = false;
  }
};

const openExchangeModal = (hero) => {
  exchangeMode.value = "exchange";
  exchangeHero.value = hero;
  exchangeTargetHeroId.value = null;
  heroSearchKeyword.value = "";
  selectedQuality.value = "全部";
  selectedCountry.value = "全部";
  exchangeModalVisible.value = true;
};

const openAddHeroModal = () => {
  if (editingHeroes.value.length >= 5) {
    message.warning("阵容已满，无法上阵更多英雄");
    return;
  }
  exchangeMode.value = "add";
  exchangeHero.value = null;
  exchangeTargetHeroId.value = null;
  heroSearchKeyword.value = "";
  selectedQuality.value = "全部";
  selectedCountry.value = "全部";
  exchangeModalVisible.value = true;
};

const selectExchangeHero = (hero) => {
  exchangeTargetHeroId.value = hero.id;
};

const confirmHeroAction = () => {
  if (!exchangeTargetHeroId.value) {
    message.warning("请选择武将");
    return;
  }

  if (Object.keys(editingTeamHeroes.value).length === 0) {
    currentTeamHeroes.value.forEach((h) => {
      editingTeamHeroes.value[h.position] = {
        heroId: h.heroId,
        artifactId: normalizeArtifactId(h.artifactId),
      };
    });
  }

  if (exchangeMode.value === "add") {
    const slot = getFirstEmptySlot();
    const targetHeroData =
      roleHeroesData.value[String(exchangeTargetHeroId.value)];
    editingTeamHeroes.value[slot] = {
      heroId: exchangeTargetHeroId.value,
      artifactId: normalizeArtifactId(targetHeroData?.artifactId),
    };
    message.success(
      `${getHeroName(exchangeTargetHeroId.value)} 已上阵到位置 ${slot + 1}`,
    );
  } else {
    if (!exchangeHero.value) {
      message.warning("请选择要更换的武将");
      return;
    }
    const originalArtifactId = exchangeHero.value.artifactId;
    editingTeamHeroes.value[exchangeHero.value.position] = {
      heroId: exchangeTargetHeroId.value,
      artifactId: normalizeArtifactId(originalArtifactId),
    };
    message.success(
      `已将 ${getHeroName(exchangeHero.value.heroId)} 更换为 ${getHeroName(exchangeTargetHeroId.value)}`,
    );
  }

  exchangeModalVisible.value = false;
};

const removeHero = (hero) => {
  if (Object.keys(editingTeamHeroes.value).length === 0) {
    currentTeamHeroes.value.forEach((h) => {
      editingTeamHeroes.value[h.position] = {
        heroId: h.heroId,
        artifactId: normalizeArtifactId(h.artifactId),
      };
    });
  }

  delete editingTeamHeroes.value[hero.position];
  message.success(`${getHeroName(hero.heroId)} 已下阵`);
};

const onDragStart = (event, hero) => {
  draggedHeroId.value = hero.heroId;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", JSON.stringify(hero));
};

const onDragEnd = () => {
  draggedHeroId.value = null;
  dragOverPosition.value = null;
};

const onDragOver = (event, hero) => {
  if (draggedHeroId.value !== hero.heroId) {
    dragOverPosition.value = hero.position;
  }
};

const onDragLeave = () => {
  dragOverPosition.value = null;
};

const onDrop = (event, targetHero) => {
  event.preventDefault();
  dragOverPosition.value = null;

  if (!draggedHeroId.value || draggedHeroId.value === targetHero.heroId) {
    return;
  }

  const draggedHero = editingHeroes.value.find(
    (h) => h.heroId === draggedHeroId.value,
  );
  if (!draggedHero) return;

  if (Object.keys(editingTeamHeroes.value).length === 0) {
    currentTeamHeroes.value.forEach((h) => {
      editingTeamHeroes.value[h.position] = {
        heroId: h.heroId,
        artifactId: normalizeArtifactId(h.artifactId),
      };
    });
  }

  const draggedPos = draggedHero.position;
  const targetPos = targetHero.position;

  const draggedHeroData = editingTeamHeroes.value[draggedPos];
  const targetHeroData = editingTeamHeroes.value[targetPos];

  editingTeamHeroes.value[draggedPos] = targetHeroData;
  editingTeamHeroes.value[targetPos] = draggedHeroData;

  message.success(
    `已将 ${getHeroName(draggedHero.heroId)} 与 ${getHeroName(targetHero.heroId)} 交换位置`,
  );

  draggedHeroId.value = null;
};

const loadSavedLineups = () => {
  try {
    const token = tokenStore.selectedToken;
    if (!token) return;
    const key = `${STORAGE_KEY}_${token.id}`;
    const data = localStorage.getItem(key);
    if (data) {
      savedLineups.value = JSON.parse(data);
    } else {
      savedLineups.value = [];
    }
  } catch (e) {
    console.error("加载保存的阵容失败:", e);
    savedLineups.value = [];
  }
};

const saveLineupsToStorage = () => {
  try {
    const token = tokenStore.selectedToken;
    if (!token) return;
    const key = `${STORAGE_KEY}_${token.id}`;
    localStorage.setItem(key, JSON.stringify(savedLineups.value));
  } catch (e) {
    console.error("保存阵容到缓存失败:", e);
    message.error("保存阵容失败");
  }
};

const padBackupTimestamp = (value) => String(value).padStart(2, "0");

const formatBackupTimestamp = (date = new Date()) =>
  `${date.getFullYear()}${padBackupTimestamp(date.getMonth() + 1)}${padBackupTimestamp(date.getDate())}_${padBackupTimestamp(date.getHours())}${padBackupTimestamp(date.getMinutes())}${padBackupTimestamp(date.getSeconds())}`;

const sanitizeBackupFileNamePart = (value) =>
  String(value || "token")
    .trim()
    .replace(/[\\/:*?"<>|]+/g, "_")
    .replace(/\s+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "") || "token";

const buildBackupFileName = (token) => {
  const tokenLabel = sanitizeBackupFileNamePart(token?.name || token?.id || "token");
  return `XYZW_阵容备份_${tokenLabel}_${formatBackupTimestamp()}.json`;
};

const exportSavedLineups = async () => {
  try {
    const token = tokenStore.selectedToken;
    if (!token) {
      message.warning("请先选择Token");
      return;
    }

    if (savedLineups.value.length === 0) {
      message.warning("暂无可导出的阵容");
      return;
    }

    const exportData = {
      schema: "xyzw.unlimitedlineup.backup",
      version: 1,
      exportedAt: new Date().toISOString(),
      tokenId: token.id,
      currentTeamId: currentTeamId.value,
      availableTeams: [...availableTeams.value],
      savedLineups: savedLineups.value.map((lineup) => ({
        name: lineup.name,
        teamId: lineup.teamId,
        savedAt: lineup.savedAt,
        applying: false,
        heroes: Array.isArray(lineup.heroes)
          ? lineup.heroes.map((hero) => ({
              heroId: hero.heroId,
              artifactId: normalizeArtifactId(hero.artifactId),
              position: hero.position,
            }))
          : [],
      })),
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: "application/json;charset=utf-8",
    });
    const fileName = buildBackupFileName(token);
    await saveBlob(blob, fileName, "application/json;charset=utf-8");

    message.success(`已导出 ${savedLineups.value.length} 个阵容`);
  } catch (error) {
    console.error("导出阵容失败:", error);
    message.error(`导出失败: ${error.message}`);
  }
};

const openSavedLineupsImportDialog = () => {
  const input = lineupImportInput.value;
  if (!input) {
    message.error("无法打开文件选择器");
    return;
  }

  input.value = "";
  input.click();
};

const normalizeImportedHeroes = (heroes) => {
  if (!Array.isArray(heroes)) return [];

  return heroes
    .map((hero, heroIndex) => {
      if (!hero || typeof hero !== "object") return null;

      const heroId = Number(hero.heroId);
      const position = Number(
        hero.position !== undefined ? hero.position : heroIndex,
      );
      const artifactId = normalizeArtifactId(hero.artifactId);

      if (!Number.isFinite(heroId) || !Number.isFinite(position)) {
        return null;
      }

      return {
        heroId,
        artifactId,
        position,
      };
    })
    .filter(Boolean)
    .sort((a, b) => a.position - b.position);
};

const normalizeImportedLineups = (payload) => {
  const rawLineups = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.savedLineups)
      ? payload.savedLineups
      : Array.isArray(payload?.lineups)
        ? payload.lineups
        : null;

  if (!rawLineups) {
    throw new Error("文件中未找到阵容数据");
  }

  const normalized = rawLineups
    .map((lineup, index) => {
      if (!lineup || typeof lineup !== "object") {
        throw new Error(`第 ${index + 1} 条阵容数据无效`);
      }

      const teamId = Number(lineup.teamId);
      const heroes = normalizeImportedHeroes(lineup.heroes);

      if (!Number.isFinite(teamId)) {
        throw new Error(`第 ${index + 1} 条阵容缺少有效阵容槽位`);
      }

      if (heroes.length === 0) {
        throw new Error(`第 ${index + 1} 条阵容没有有效英雄数据`);
      }

      return {
        name: String(lineup.name || `阵容${teamId}-${index + 1}`),
        teamId,
        savedAt: Number(lineup.savedAt) || Date.now(),
        applying: false,
        heroes,
      };
    })
    .filter(Boolean);

  if (normalized.length === 0) {
    throw new Error("文件中未找到可导入的阵容");
  }

  return normalized;
};

const lineupDelay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const isTooFastError = (err) =>
  String(err?.message || "").includes("200400");

const isRecoverableLineupError = (err) => {
  const message = String(err?.message || "");
  return message.includes("200400") || message.includes("200020");
};

const sendLineupCommandWithRetry = async (
  tokenId,
  cmd,
  params = {},
  retryCount = 2,
  retryDelayMs = 3000,
) => {
  let lastError = null;

  for (let attempt = 0; attempt <= retryCount; attempt += 1) {
    try {
      return await tokenStore.sendMessageWithPromise(tokenId, cmd, params);
    } catch (error) {
      lastError = error;
      if (!isRecoverableLineupError(error) || attempt === retryCount) {
        throw error;
      }

      await lineupDelay(retryDelayMs * (attempt + 1));
    }
  }

  throw lastError || new Error(`发送 ${cmd} 失败`);
};

const buildLineupTeamInfo = (heroes) =>
  heroes.reduce((teamInfo, hero) => {
    teamInfo[hero.position] = {
      heroId: hero.heroId,
      id: hero.heroId,
      battleTeamSlot: hero.position,
      artifactId: normalizeArtifactId(hero.artifactId),
    };
    return teamInfo;
  }, {});

const getPresetTeamState = () => {
  const storePreset = tokenStore.gameData?.presetTeam?.presetTeamInfo;
  if (storePreset && typeof storePreset === "object") {
    return storePreset;
  }

  if (presetTeamData.value && typeof presetTeamData.value === "object") {
    return presetTeamData.value;
  }

  return {
    useTeamId: currentTeamId.value || 1,
    presetTeamInfo: {},
  };
};

const applyLocalTeamSnapshot = (heroes, teamId = currentTeamId.value) => {
  const normalizedHeroes = Array.isArray(heroes)
    ? heroes
        .map((hero) => ({
          position: Number(hero.position),
          heroId: Number(hero.heroId),
          artifactId: normalizeArtifactId(hero.artifactId),
        }))
        .filter((hero) => Number.isFinite(hero.position) && Number.isFinite(hero.heroId))
        .sort((a, b) => a.position - b.position)
    : [];

  const nextTeamId = Number(teamId) || currentTeamId.value || 1;
  const nextTeamInfo = buildLineupTeamInfo(normalizedHeroes);
  const currentPresetState = getPresetTeamState();
  const nextPresetTeamState = {
    ...currentPresetState,
    useTeamId: nextTeamId,
    presetTeamInfo: {
      ...(currentPresetState.presetTeamInfo || {}),
      [nextTeamId]: {
        ...(currentPresetState.presetTeamInfo?.[nextTeamId] || {}),
        teamInfo: nextTeamInfo,
      },
    },
  };

  currentTeamId.value = nextTeamId;
  currentTeamInfo.value = nextTeamInfo;
  editingTeamHeroes.value = {};
  presetTeamData.value = nextPresetTeamState;

  const teamIds = Object.keys(nextPresetTeamState.presetTeamInfo || {})
    .filter((key) => /^\d+$/.test(key))
    .map(Number)
    .sort((a, b) => a - b);
  if (teamIds.length > 0) {
    availableTeams.value = teamIds;
  }

  tokenStore.$patch((state) => {
    state.gameData = {
      ...(state.gameData ?? {}),
      presetTeam: {
        ...(state.gameData?.presetTeam ?? {}),
        presetTeamInfo: nextPresetTeamState,
      },
    };
  });
};

const fetchLatestData = async ({
  forceTeamSwitch = true,
  updateState = true,
} = {}) => {
  const token = tokenStore.selectedToken;
  if (!token) {
    message.warning("请先选择Token");
    return null;
  }

  const tokenId = token.id;
  const status = tokenStore.getWebSocketStatus(tokenId);
  if (status !== "connected") {
    message.error("WebSocket未连接，无法获取数据");
    return null;
  }

  const availableTeamIds =
    availableTeams.value.length > 0
      ? availableTeams.value
      : [1, 2, 3, 4, 5, 6];

  let targetTeamId = currentTeamId.value;
  if (!availableTeamIds.includes(targetTeamId)) {
    targetTeamId = availableTeamIds[0];
  }

  if (forceTeamSwitch) {
    const currentIndex = availableTeamIds.indexOf(targetTeamId);
    const otherTeamId =
      availableTeamIds[currentIndex === 0 ? 1 : currentIndex - 1] ||
      availableTeamIds[0];

    if (otherTeamId !== targetTeamId) {
      await sendLineupCommandWithRetry(tokenId, "presetteam_saveteam", {
        teamId: otherTeamId,
      });
      await lineupDelay(700);
      await sendLineupCommandWithRetry(tokenId, "presetteam_saveteam", {
        teamId: targetTeamId,
      });
      await lineupDelay(700);
    }
  }

  const presetTeamResult = await sendLineupCommandWithRetry(
    tokenId,
    "presetteam_getinfo",
    {},
    1,
    2000,
  );
  await lineupDelay(forceTeamSwitch ? 350 : 200);
  const roleInfo = await sendLineupCommandWithRetry(
    tokenId,
    "role_getroleinfo",
    {},
    1,
    2000,
  );

  const role = roleInfo?.role || roleInfo;
  const roleHeroes = role?.heroes || {};
  const presetInfo = presetTeamResult?.presetTeamInfo || null;
  const resolvedTeamId = presetInfo?.useTeamId || targetTeamId || 1;
  const teams = presetInfo?.presetTeamInfo || {};
  const currentTeam = teams[resolvedTeamId] || teams[String(resolvedTeamId)];
  const teamInfo = currentTeam?.teamInfo || {};

  if (updateState) {
    roleHeroesData.value = roleHeroes;
    allHeroesData.value = roleHeroes;
    presetTeamData.value = presetInfo;

    if (presetTeamResult) {
      tokenStore.$patch((state) => {
        state.gameData = {
          ...(state.gameData ?? {}),
          presetTeam: presetTeamResult,
        };
      });
    }

    if (presetInfo) {
      currentTeamId.value = resolvedTeamId;

      const teamIds = Object.keys(teams)
        .filter((k) => /^\d+$/.test(k))
        .map(Number)
        .sort((a, b) => a - b);
      availableTeams.value = teamIds.length ? teamIds : availableTeams.value;
      currentTeamInfo.value = teamInfo;
      editingTeamHeroes.value = {};
    }
  }

  return {
    tokenId,
    roleInfo,
    role,
    heroes: roleHeroes,
    presetTeamResult,
    presetInfo,
    teamInfo,
    currentTeamId: resolvedTeamId,
  };
};

const syncCurrentTeamInfo = async () => {
  return await fetchLatestData({
    forceTeamSwitch: false,
    updateState: false,
  });
};

const handleSavedLineupsImport = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    try {
      const text = String(reader.result || "").replace(/^\uFEFF/, "");
      const payload = JSON.parse(text);
      const importedLineups = normalizeImportedLineups(payload);

      dialog.warning({
        title: "导入阵容备份",
        content: `将用文件中的 ${importedLineups.length} 个阵容覆盖当前已保存阵容，是否继续？`,
        positiveText: "导入",
        negativeText: "取消",
        onPositiveClick: () => {
          savedLineups.value = importedLineups;
          saveLineupsToStorage();

          const importedTeamIds = [
            ...new Set(importedLineups.map((lineup) => lineup.teamId)),
          ].sort((a, b) => a - b);
          const nextSelectedTeam =
            importedTeamIds.find((teamId) =>
              availableTeams.value.includes(teamId),
            ) ?? currentTeamId.value ?? availableTeams.value[0] ?? 1;

          selectedTeamTab.value = nextSelectedTeam;
          expandedLineup.value = null;

          message.success(`已导入 ${importedLineups.length} 个阵容`);
        },
      });
    } catch (error) {
      console.error("导入阵容失败:", error);
      message.error(`导入失败: ${error.message}`);
    }
  };
  reader.onerror = () => {
    message.error("读取阵容备份文件失败");
  };
  reader.readAsText(file);
};

const refreshTeamInfo = async () => {
  if (activeLineupKey.value) {
    message.warning("阵容正在应用中，请稍后再刷新");
    return;
  }

  loading.value = true;
  try {
    const latest = await fetchLatestData({
      forceTeamSwitch: true,
      updateState: true,
    });
    if (!latest) return;
    message.success("数据已刷新");
  } catch (error) {
    message.error(`获取数据失败: ${error.message}`);
  } finally {
    loading.value = false;
  }
};

const saveCurrentLineup = () => {
  if (editingHeroes.value.length === 0) {
    message.warning("当前阵容为空，无法保存");
    return;
  }

  const lineupName = `阵容${currentTeamId.value} - ${new Date().toLocaleTimeString()}`;

  savedLineups.value.unshift({
    name: lineupName,
    heroes: editingHeroes.value.map((hero) => ({
      ...hero,
      artifactId: normalizeArtifactId(hero.artifactId),
    })),
    teamId: currentTeamId.value,
    savedAt: Date.now(),
    applying: false,
  });

  saveLineupsToStorage();
  message.success(`阵容已保存: ${lineupName}`);
};

const applyLineup = async (lineup) => {
  const token = tokenStore.selectedToken;
  if (!token) {
    message.warning("请先选择Token");
    return;
  }

  const tokenId = token.id;
  const status = tokenStore.getWebSocketStatus(tokenId);
  if (status !== "connected") {
    message.error("WebSocket未连接，无法应用阵容");
    return;
  }

  if (lineup.teamId !== currentTeamId.value) {
    message.warning(
      `此阵容仅适用于阵容槽位 ${lineup.teamId}，当前槽位为 ${currentTeamId.value}`,
    );
    return;
  }

  if (activeLineupKey.value) {
    message.warning("已有阵容正在应用，请稍后再试");
    return;
  }

  const lineupKey = `${lineup.teamId}-${lineup.savedAt}-${lineup.name}`;
  activeLineupKey.value = lineupKey;
  applyProgress.value = {
    visible: true,
    text: `开始应用阵容：${lineup.name}`,
    percent: 0,
  };

  lineup.applying = true;
  state.value.isRunning = true;
  const errors = [];

  const getTeamHeroes = (teamInfo) => {
    if (!teamInfo) return [];
    return Object.entries(teamInfo)
      .map(([key, hero]) => ({
        position: hero?.battleTeamSlot ?? Number(key),
        heroId: hero?.heroId || hero?.id,
        artifactId: normalizeArtifactId(hero?.artifactId),
      }))
      .filter((h) => h.heroId)
      .sort((a, b) => a.position - b.position);
  };

  const setApplyProgress = (text, percent) => {
    applyProgress.value = {
      visible: true,
      text,
      percent,
    };
  };

  const isSameLineupSnapshot = (leftHeroes, rightHeroes) => {
    const normalize = (heroes) =>
      heroes
        .map((hero) => ({
          position: Number(hero.position),
          heroId: Number(hero.heroId),
          artifactId: normalizeArtifactId(hero.artifactId),
        }))
        .filter((hero) => Number.isFinite(hero.position) && Number.isFinite(hero.heroId))
        .sort((a, b) => a.position - b.position);

    const left = normalize(leftHeroes || []);
    const right = normalize(rightHeroes || []);

    if (left.length !== right.length) {
      return false;
    }

    return left.every((hero, index) => {
      const other = right[index];
      return (
        hero.position === other.position &&
        hero.heroId === other.heroId &&
        hero.artifactId === other.artifactId
      );
    });
  };

  try {
    const targetHeroes = [...lineup.heroes];
    setApplyProgress("分析当前阵容", 10);

    const latestData = await fetchLatestData({
      forceTeamSwitch: false,
      updateState: false,
    });
    if (!latestData) {
      throw new Error("获取当前阵容失败");
    }
    let { heroes, teamInfo } = latestData;
    let currentHeroes = getTeamHeroes(teamInfo);
    const artifactHolderHeroIds = new Set();

    for (const targetHero of targetHeroes) {
      if (targetHero.artifactId == null) continue;

      for (const [id, hero] of Object.entries(heroes)) {
        if (normalizeArtifactId(hero.artifactId) === targetHero.artifactId) {
          const heroId = Number(id);
          if (Number.isFinite(heroId) && heroId !== targetHero.heroId) {
            artifactHolderHeroIds.add(heroId);
          }
          break;
        }
      }
    }

    setApplyProgress("交换装备", 20);
    for (const targetHero of targetHeroes) {
      if (targetHero.artifactId == null) continue;

      let heroWithArtifact = null;
      for (const [id, hero] of Object.entries(heroes)) {
        if (normalizeArtifactId(hero.artifactId) === targetHero.artifactId) {
          heroWithArtifact = Number(id);
          break;
        }
      }

      if (heroWithArtifact && heroWithArtifact !== targetHero.heroId) {
        try {
          setApplyProgress(
            `正在交换装备：${getHeroName(targetHero.heroId) || targetHero.heroId}`,
            22,
          );
          await sendLineupCommandWithRetry(tokenId, "hero_exchange", {
            heroId: heroWithArtifact,
            targetHeroId: targetHero.heroId,
          });
          await lineupDelay(600);
        } catch (err) {
          if (!isRecoverableLineupError(err)) {
            errors.push(
              `${getHeroName(targetHero.heroId) || targetHero.heroId} 装备交换失败: ${err.message}`,
            );
          }
        }
      }
    }

    setApplyProgress("同步交换后的阵容", 35);
    const afterExchange = await fetchLatestData({
      forceTeamSwitch: false,
      updateState: false,
    });
    if (afterExchange) {
      heroes = afterExchange.heroes;
      currentHeroes = getTeamHeroes(afterExchange.teamInfo);
    }

    setApplyProgress("清理多余武将", 50);
    for (const hero of currentHeroes) {
      const targetHero = targetHeroes.find((h) => h.heroId === hero.heroId);
      if (!targetHero && !artifactHolderHeroIds.has(hero.heroId)) {
        try {
          setApplyProgress(
            `正在下阵：${getHeroName(hero.heroId) || hero.heroId}`,
            52,
          );
          await sendLineupCommandWithRetry(tokenId, "hero_gobackbattle", {
            slot: hero.position,
          });
          await lineupDelay(600);
        } catch (err) {
          if (!isRecoverableLineupError(err)) {
            errors.push(`${getHeroName(hero.heroId) || hero.heroId} 下阵失败: ${err.message}`);
          }
        }
      }
    }

    setApplyProgress("同步下阵后的阵容", 65);
    const afterRemoval = await fetchLatestData({
      forceTeamSwitch: false,
      updateState: false,
    });
    if (afterRemoval) {
      currentHeroes = getTeamHeroes(afterRemoval.teamInfo);
    }

    setApplyProgress("校正上阵位置", 80);
    const heroesToDeploy = targetHeroes.filter((targetHero) => {
      const currentHero = currentHeroes.find(
        (hero) => hero.heroId === targetHero.heroId,
      );
      return !currentHero || currentHero.position !== targetHero.position;
    });

    for (const targetHero of heroesToDeploy) {
      try {
        setApplyProgress(
          `正在上阵：${getHeroName(targetHero.heroId) || targetHero.heroId}`,
          85,
        );
        await sendLineupCommandWithRetry(tokenId, "hero_gointobattle", {
          heroId: targetHero.heroId,
          slot: targetHero.position,
        });
        await lineupDelay(600);
      } catch (err) {
        if (!isRecoverableLineupError(err)) {
          errors.push(
            `${getHeroName(targetHero.heroId) || targetHero.heroId} 上阵失败: ${err.message}`,
          );
        }
      }
    }

    if (errors.length > 0) {
      message.warning(`阵容已应用，但有部分错误:\n${errors.join("\n")}`);
    } else {
      applyLocalTeamSnapshot(targetHeroes);
      message.success(`阵容 "${lineup.name}" 已应用`);
    }

    setApplyProgress("同步最终状态", 95);
    const synced = await syncCurrentTeamInfo();
    if (synced) {
      const syncedHeroes = getTeamHeroes(synced.teamInfo);
      if (isSameLineupSnapshot(syncedHeroes, targetHeroes)) {
        applyLocalTeamSnapshot(syncedHeroes, synced.currentTeamId);
      } else {
        applyLocalTeamSnapshot(targetHeroes);
      }
    } else {
      applyLocalTeamSnapshot(targetHeroes);
    }
    setApplyProgress("阵容应用完成", 100);
    savedLineupsModalVisible.value = false;
  } catch (error) {
    message.error(`应用阵容失败: ${error.message}`);
  } finally {
    lineup.applying = false;
    activeLineupKey.value = null;
    applyProgress.value = {
      visible: false,
      text: "",
      percent: 0,
    };
    state.value.isRunning = false;
  }
};

const renameLineup = (lineup) => {
  const index = findLineupIndex(lineup);
  if (index === -1) return;
  const currentName = savedLineups.value[index].name;
  let newName = currentName;
  dialog.create({
    title: "重命名阵容",
    content: () =>
      h(NInput, {
        defaultValue: currentName,
        onInput: (val) => {
          newName = val;
        },
        placeholder: "请输入阵容名称",
      }),
    positiveText: "确定",
    negativeText: "取消",
    onPositiveClick: () => {
      if (newName && newName.trim()) {
        savedLineups.value[index].name = newName.trim();
        saveLineupsToStorage();
        message.success("阵容名称已更新");
      }
    },
  });
};

const deleteLineup = (lineup) => {
  const index = findLineupIndex(lineup);
  if (index === -1) return;
  dialog.warning({
    title: "删除阵容",
    content: `确定要删除阵容 "${savedLineups.value[index].name}" 吗？`,
    positiveText: "删除",
    negativeText: "取消",
    onPositiveClick: () => {
      savedLineups.value.splice(index, 1);
      saveLineupsToStorage();
      message.success("阵容已删除");
    },
  });
};

const switchTeam = async (teamId) => {
  if (teamId === currentTeamId.value) return;

  if (activeLineupKey.value) {
    message.warning("阵容正在应用中，请稍后再切换");
    return;
  }

  const token = tokenStore.selectedToken;
  if (!token) {
    message.warning("请先选择Token");
    return;
  }

  const tokenId = token.id;
  const status = tokenStore.getWebSocketStatus(tokenId);
  if (status !== "connected") {
    message.error("WebSocket未连接，无法切换阵容");
    return;
  }

  switchingTeamId.value = teamId;
  state.value.isRunning = true;

  try {
    await sendLineupCommandWithRetry(tokenId, "presetteam_saveteam", {
      teamId,
    });

    await lineupDelay(1000);

    currentTeamId.value = teamId;
    message.success(`已切换到阵容 ${teamId}`);

    await refreshTeamInfo();
  } catch (error) {
    message.error(`切换阵容失败: ${error.message}`);
  } finally {
    switchingTeamId.value = null;
    state.value.isRunning = false;
  }
};

watch(
  () => tokenStore.selectedToken,
  (newToken, oldToken) => {
    if (newToken && newToken.id !== oldToken?.id) {
      loadSavedLineups();
      currentTeamInfo.value = null;
      presetTeamData.value = null;
      allHeroesData.value = {};
      currentTeamId.value = 1;

      const status = tokenStore.getWebSocketStatus(newToken.id);
      if (status === "connected") {
        refreshTeamInfo();
      }
    }
  },
);

watch(
  () =>
    tokenStore.selectedToken
      ? tokenStore.getWebSocketStatus(tokenStore.selectedToken.id)
      : null,
  (newStatus, oldStatus) => {
    if (
      newStatus === "connected" &&
      oldStatus !== "connected" &&
      tokenStore.selectedToken
    ) {
      setTimeout(() => {
        refreshTeamInfo();
      }, 500);
    }
  },
);

onMounted(() => {
  loadSavedLineups();

  const token = tokenStore.selectedToken;
  if (token) {
    const status = tokenStore.getWebSocketStatus(token.id);
    if (status === "connected") {
      refreshTeamInfo();
    }
  }
});
</script>

<style scoped lang="scss">
.lineup-saver {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  min-height: 300px;
}

.lineup-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  width: 100%;
  min-width: 0;
}

.toolbar {
  display: flex;
  gap: var(--spacing-sm);
  align-items: stretch;
  width: 100%;
  min-width: 0;

  :deep(.toolbar-btn) {
    position: relative;
    overflow: hidden;
    flex: 1 1 0;
    min-width: 0;
    width: 0;
    transition: transform 180ms ease, filter 180ms ease, opacity 180ms ease;
  }

  :deep(.toolbar-btn.is-refreshing) {
    cursor: progress;
    opacity: 0.94;
    filter: saturate(1.08);
  }

  :deep(.toolbar-btn.is-refreshing::after) {
    content: "";
    position: absolute;
    inset: 0;
    background: linear-gradient(
      120deg,
      transparent 0%,
      rgba(255, 255, 255, 0.08) 38%,
      rgba(255, 255, 255, 0.22) 50%,
      rgba(255, 255, 255, 0.08) 62%,
      transparent 100%
    );
    transform: translateX(-120%);
    animation: toolbarRefreshShine 1.1s ease-in-out infinite;
    pointer-events: none;
  }
}

@keyframes toolbarRefreshShine {
  0% {
    transform: translateX(-120%);
  }
  100% {
    transform: translateX(120%);
  }
}

.current-team-section {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  padding: var(--spacing-md);

  h4 {
    margin: 0 0 var(--spacing-sm) 0;
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  .drag-tip {
    font-size: var(--font-size-xs);
    color: var(--text-tertiary);
    font-weight: normal;
  }
}

.heroes-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.hero-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  background: var(--bg-primary);
  border-radius: var(--border-radius-small);
  padding: var(--spacing-xs) var(--spacing-sm);
  min-width: 100px;
  transition: all 0.2s;
  cursor: grab;
  border: 2px solid transparent;

  &:hover {
    background: var(--primary-color-light);
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }

  &.dragging {
    opacity: 0.5;
    cursor: grabbing;
  }

  &.drag-over {
    border-color: var(--primary-color);
    background: var(--primary-color-light);
  }
}

.hero-position {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  font-size: 10px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.hero-placeholder {
  font-size: 12px;
  color: var(--text-secondary);
}

.hero-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  flex: 1;
}

.hero-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.hero-artifact {
  font-size: var(--font-size-xs);
}

.exchange-btn {
  flex-shrink: 0;
}

.remove-btn {
  flex-shrink: 0;
}

.saved-lineups-section {
  h4 {
    margin: 0 0 var(--spacing-sm) 0;
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
  }
}

.empty-tip {
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
  text-align: center;
  padding: var(--spacing-lg);
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
}

.saved-lineups-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.saved-lineup-item {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  padding: var(--spacing-sm);
}

.lineup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xs);
}

.lineup-name {
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.lineup-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.lineup-heroes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.lineup-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.quick-switch-section {
  h4 {
    margin: 0 0 var(--spacing-sm) 0;
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
  }
}

.team-selector {
  display: grid;
  gap: var(--spacing-xs);
  width: 100%;
  min-width: 0;

  :deep(.team-slot-btn) {
    width: 100%;
    min-width: 0;
  }
}

.refine-modal-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.equip-refine-section {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  padding: var(--spacing-sm);
}

.equip-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  padding-bottom: var(--spacing-xs);
  border-bottom: 1px solid var(--border-light);
}

.equip-name {
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.equip-level {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.equip-bonus {
  font-size: var(--font-size-xs);
  color: var(--primary-color);
  margin-left: auto;
}

.slots-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.slot-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--bg-primary);
  border-radius: var(--border-radius-small);
  border-left: 3px solid var(--border-light);

  &.locked {
    border-left-color: var(--primary-color);
    background: var(--primary-color-light);
  }

  &.color-1 {
    border-left-color: #ffffff;
    background: rgba(255, 255, 255, 0.1);
  }

  &.color-2 {
    border-left-color: #4caf50;
    background: rgba(76, 175, 80, 0.1);
  }

  &.color-3 {
    border-left-color: #2196f3;
    background: rgba(33, 150, 243, 0.1);
  }

  &.color-4 {
    border-left-color: #9c27b0;
    background: rgba(156, 39, 176, 0.1);
  }

  &.color-5 {
    border-left-color: #ff9800;
    background: rgba(255, 152, 0, 0.1);
  }

  &.color-6 {
    border-left-color: #f44336;
    background: rgba(244, 67, 54, 0.1);
  }
}

.slot-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  min-width: 30px;
}

.slot-attr {
  flex: 1;
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm);
}

.attr-name {
  color: var(--text-primary);
}

.attr-value {
  color: var(--primary-color);
  font-weight: var(--font-weight-medium);
}

.slot-empty {
  flex: 1;
  color: var(--text-tertiary);
  font-size: var(--font-size-xs);
}

.no-equipment {
  text-align: center;
  color: var(--text-secondary);
  padding: var(--spacing-lg);
}

.exchange-modal-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.current-hero-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
}

.hero-filter-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);

  .filter-label {
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    flex-shrink: 0;
  }

  .filter-tags {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-xs);
  }
}

.hero-select-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--spacing-sm);
  max-height: 400px;
  overflow-y: auto;
  padding: var(--spacing-xs);
}

.hero-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm);
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;

  &:hover {
    background: var(--bg-secondary);
    transform: translateY(-2px);
  }

  &.selected {
    border-color: var(--primary-color);
    background: var(--primary-color-light);
  }

  &.quality-red {
    border-color: rgba(245, 34, 45, 0.3);

    &:hover {
      border-color: rgba(245, 34, 45, 0.6);
      background: rgba(245, 34, 45, 0.1);
    }

    &.selected {
      border-color: #f5222d;
      background: rgba(245, 34, 45, 0.15);
    }
  }

  &.quality-orange {
    border-color: rgba(250, 173, 20, 0.3);

    &:hover {
      border-color: rgba(250, 173, 20, 0.6);
      background: rgba(250, 173, 20, 0.1);
    }

    &.selected {
      border-color: #faad14;
      background: rgba(250, 173, 20, 0.15);
    }
  }

  &.quality-purple {
    border-color: rgba(114, 46, 209, 0.3);

    &:hover {
      border-color: rgba(114, 46, 209, 0.6);
      background: rgba(114, 46, 209, 0.1);
    }

    &.selected {
      border-color: #722ed1;
      background: rgba(114, 46, 209, 0.15);
    }
  }
}

.hero-select-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.hero-select-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  text-align: center;
}

.hero-select-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: center;
}

.hero-artifact-id {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.saved-lineups-modal-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.saved-lineups-modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  flex-wrap: wrap;
}

.saved-lineups-modal-hint {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.saved-lineups-modal-actions {
  display: flex;
  gap: var(--spacing-xs);
  margin-left: auto;
}

.saved-lineups-progress {
  background: linear-gradient(180deg, rgba(110, 94, 219, 0.18), rgba(26, 22, 52, 0.9));
  border: 1px solid rgba(132, 160, 255, 0.22);
  border-radius: var(--border-radius-medium);
  padding: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.16);
}

.saved-lineups-progress-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.saved-lineups-progress-text {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.saved-lineups-import-input {
  display: none;
}

.team-tabs {
  display: flex;
  gap: var(--spacing-xs);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: var(--spacing-sm);
}

.team-tab {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius-medium);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  transition: all 0.2s;

  &:hover {
    background: var(--bg-tertiary);
    color: var(--text-primary);
  }

  &.active {
    background: var(--primary-color);
    color: white;
  }
}

.tab-count {
  font-size: var(--font-size-xs);
  opacity: 0.8;
}

.lineups-list {
  max-height: 50vh;
  overflow-y: auto;
}

.lineup-card {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  margin-bottom: var(--spacing-sm);
  overflow: hidden;
}

.lineup-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm);
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--bg-secondary);
  }
}

.lineup-title-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.expand-icon {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  width: 12px;
}

.lineup-name {
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.lineup-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.lineup-quick-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.lineup-detail {
  padding: var(--spacing-sm);
  padding-top: 0;
  border-top: 1px solid var(--border-color);
}

.lineup-heroes-detail {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.lineup-hero-item {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: var(--border-radius-small);
}

.hero-pos {
  color: var(--text-tertiary);
}

.hero-name {
  color: var(--text-primary);
}

.hero-artifact {
  color: var(--primary-color);
  font-size: var(--font-size-xs);
}

.lineup-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.no-lineup-tip {
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
  text-align: center;
  padding: var(--spacing-lg);
}
</style>

