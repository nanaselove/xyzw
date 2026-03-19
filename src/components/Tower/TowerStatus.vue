<template>
  <div class="status-card tower-status">
    <div class="card-header">
      <img src="/icons/1733492491706148.png" alt="咸将塔图标" class="status-icon" />
      <div class="status-info">
        <h3>咸将塔</h3>
        <p>当前层数与推进状态</p>
      </div>
      <div class="energy-display">
        <img src="/icons/xiaoyugan.png" alt="小鱼干" class="energy-icon" />
        <span class="energy-count">{{ towerEnergy }}</span>
      </div>
    </div>

    <div class="status-pill" :class="{ running: isClimbing || towerEnergy > 0 }">
      {{ towerStatusText }}
    </div>

    <div class="card-content">
      <div class="tower-floor">
        <span class="label">当前层数</span>
        <span class="floor-number">{{ floorDisplay }}</span>
      </div>
      <p class="floor-sub">{{ floorSubText }}</p>
    </div>

    <div class="card-actions">
      <button
        :class="[
          'climb-button',
          {
            active: canClimb,
            disabled: !canClimb,
          },
        ]"
        :disabled="!canClimb"
        @click="startTowerClimb"
      >
        {{ isClimbing ? "爬塔中..." : "开始爬塔" }}
      </button>

      <button v-if="isClimbing" class="stop-button" @click="stopClimbing">
        停止爬塔
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { useMessage } from "naive-ui";

const tokenStore = useTokenStore();
const message = useMessage();

let stopFlag = false;

const isClimbing = ref(false);
const climbTimeout = ref(null);

const roleInfo = computed(() => tokenStore.gameData?.roleInfo || null);

const floorData = computed(() => {
  const towerId = Number(roleInfo.value?.role?.tower?.id ?? 0);
  const floor = Math.floor(towerId / 10) + 1;
  const layer = (towerId % 10) + 1;
  return {
    floor: Number.isFinite(floor) ? floor : 1,
    layer: Number.isFinite(layer) ? layer : 1,
  };
});

const floorDisplay = computed(() => `${Math.max(0, floorData.value.floor)}F`);
const floorSubText = computed(() => `当前进度 第${Math.max(1, floorData.value.layer)}层`);

const towerEnergy = computed(() => {
  const tower = roleInfo.value?.role?.tower;
  return Number(tower?.energy || 0);
});

const canClimb = computed(() => towerEnergy.value > 0 && !isClimbing.value);

const towerStatusText = computed(() => {
  if (isClimbing.value) return "🟢 进行中";
  if (towerEnergy.value > 0) return "🟢 可挑战";
  return "🔴 未开始";
});

const stopClimbing = () => {
  stopFlag = true;
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
  message.info("已手动停止批量爬塔");
};

const startTowerClimb = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }

  if (!canClimb.value) {
    message.warning("体力不足或正在爬塔中");
    return;
  }

  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }

  isClimbing.value = true;
  stopFlag = false;
  let climbCount = 0;

  climbTimeout.value = setTimeout(() => {
    isClimbing.value = false;
    climbTimeout.value = null;
    stopFlag = true;
    message.info("批量爬塔超时，已自动停止");
  }, 60000);

  try {
    const tokenId = tokenStore.selectedToken.id;
    const maxClimb = 100;

    for (let i = 0; i < maxClimb; i++) {
      if (stopFlag) break;
      await getTowerInfo();
      if (towerEnergy.value <= 0) break;

      await tokenStore.sendMessageWithPromise(tokenId, "fight_starttower", {}, 10000);
      climbCount++;
      await new Promise((res) => setTimeout(res, 2000));
    }

    message.success(`已自动爬塔 ${climbCount} 次`);
  } catch (error) {
    message.error("批量爬塔失败: " + (error?.message || "未知错误"));
  }

  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
};

const getTowerInfo = async () => {
  if (!tokenStore.selectedToken) return;

  try {
    const tokenId = tokenStore.selectedToken.id;
    const status = tokenStore.getWebSocketStatus(tokenId);
    if (status !== "connected") return;

    tokenStore.sendMessage(tokenId, "role_getroleinfo");
    tokenStore.sendMessage(tokenId, "tower_getinfo");
  } catch (error) {
    // ignore refresh failures
  }
};

const wsStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  return tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
});

watch(wsStatus, (newStatus, oldStatus) => {
  if (newStatus === "connected" && oldStatus !== "connected") {
    setTimeout(() => {
      getTowerInfo();
    }, 1000);
  }
});

watch(
  () => tokenStore.selectedToken,
  (newToken, oldToken) => {
    if (newToken && newToken.id !== oldToken?.id) {
      const status = tokenStore.getWebSocketStatus(newToken.id);
      if (status === "connected") getTowerInfo();
    }
  },
);

watch(
  () => tokenStore.gameData.towerResult,
  (newResult, oldResult) => {
    if (newResult && newResult.timestamp !== oldResult?.timestamp) {
      if (newResult.success) {
        message.success("咸将塔挑战成功");
      } else {
        message.error("咸将塔挑战失败");
      }

      if (!stopFlag) {
        setTimeout(() => {
          if (climbTimeout.value) {
            clearTimeout(climbTimeout.value);
            climbTimeout.value = null;
          }
          isClimbing.value = false;
        }, 1500);
      }
    }
  },
  { deep: true },
);

onMounted(() => {
  if (tokenStore.selectedToken && wsStatus.value === "connected") {
    getTowerInfo();
  }
});
</script>

<style scoped lang="scss">
.tower-status {
  display: flex;
  flex-direction: column;
  min-height: 240px;
  padding: 18px;
  gap: 12px;
}

.status-icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
  flex-shrink: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.status-info {
  min-width: 0;
  flex: 1;
}

.status-info h3 {
  margin: 0;
}

.status-info p {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.energy-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  margin-left: auto;
}

.energy-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.energy-count {
  font-size: 15px;
  font-weight: 700;
  color: rgba(248, 250, 252, 0.92);
}

.status-pill {
  align-self: flex-start;
  border-radius: 999px;
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.12);
  color: #fca5a5;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.running {
  border-color: rgba(74, 222, 128, 0.4);
  background: rgba(74, 222, 128, 0.14);
  color: #86efac;
}

.card-content {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 13, 32, 0.32);
  padding: 14px;
  margin-bottom: 2px;
}

.tower-floor {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.tower-floor .label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.74);
}

.tower-floor .floor-number {
  font-size: 38px;
  font-weight: 800;
  color: #f8fbff;
  text-shadow: 0 0 16px rgba(124, 108, 255, 0.45);
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
  letter-spacing: 0.02em;
}

.floor-sub {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.72);
  font-size: 13px;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
  padding-top: 2px;
}

.climb-button {
  width: 100%;
  height: 48px;
  font-size: 18px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.climb-button.active {
  background: linear-gradient(135deg, #6b8dff 0%, #7c6cff 100%);
  color: #f8fbff;
}

.climb-button.active:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 10px 18px rgba(89, 102, 242, 0.28);
}

.climb-button.disabled {
  background: rgba(148, 163, 184, 0.26);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.65);
  cursor: not-allowed;
}

.stop-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(248, 250, 252, 0.9);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.stop-button:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(143, 227, 255, 0.55);
  color: #ffffff;
}

@media (max-width: 768px) {
  .tower-status {
    padding: 14px;
  }

  .card-header {
    gap: 10px;
    margin-bottom: 6px;
  }

  .card-content {
    padding: 12px;
  }

  .tower-floor .floor-number {
    font-size: 34px;
  }

  .climb-button {
    height: 46px;
    font-size: 17px;
  }

  .stop-button {
    height: 42px;
    font-size: 16px;
  }
}
</style>
