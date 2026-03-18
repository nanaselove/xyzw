<template>
  <div class="status-card tower-status">
    <div class="card-header">
      <img
        src="/icons/1733492491706148.png"
        alt="爬塔图标"
        class="status-icon"
      />
      <div class="status-info">
        <h3>咸将塔</h3>
        <p>一个不小心就过了</p>
      </div>
      <div class="energy-display">
        <img src="/icons/xiaoyugan.png" alt="小鱼干" class="energy-icon" />
        <span class="energy-count">{{ towerEnergy }}</span>
      </div>
    </div>

    <div class="card-content">
      <div class="tower-floor">
        <span class="label">当前层数</span>
        <span class="floor-number">{{ currentFloor }}</span>
      </div>
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
        {{ isClimbing.value ? "爬塔中..." : "开始爬塔" }}
      </button>

      <!-- 停止批量爬塔按钮，仅批量时显示 -->
      <button v-if="isClimbing" class="stop-button" @click="stopClimbing">停止爬塔</button>
      <!-- 调试用的重置按钮，只在开发环境显示 -->
      <button v-if="false" class="reset-button" @click="resetClimbingState">
        重置状态
      </button>
    </div>
  </div>
</template>

<script setup>
// 停止批量爬塔操作
let stopFlag = false;

const stopClimbing = () => {
  stopFlag = true;
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
  message.info("已手动停止批量爬塔");
};
import { computed, onMounted, ref, watch } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { useMessage } from "naive-ui";

const tokenStore = useTokenStore();
const message = useMessage();

// 响应式数据
const isClimbing = ref(false);
const climbTimeout = ref(null); // 用于超时重置状态
const lastClimbResult = ref(null); // 最后一次爬塔结果

// 计算属性 - 从gameData中获取塔相关信息
const roleInfo = computed(() => {
  const data = tokenStore.gameData?.roleInfo || null;
  return data;
});

const currentFloor = computed(() => {
  const tower = roleInfo.value?.role?.tower;

  if (!tower) {
    return "0 - 0";
  }

  if (!tower.id && tower.id !== 0) {
    return "0 - 0";
  }

  const towerId = tower.id;
  const floor = Math.floor(towerId / 10) + 1;
  const layer = (towerId % 10) + 1;
  return `${floor} - ${layer}`;
});

const towerEnergy = computed(() => {
  const tower = roleInfo.value?.role?.tower;

  const energy = tower?.energy || 0;
  return energy;
});

const canClimb = computed(() => {
  const hasEnergy = towerEnergy.value > 0;
  const notClimbing = !isClimbing.value;
  return hasEnergy && notClimbing;
});

// 方法
const startTowerClimb = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }

  if (!canClimb.value) {
    message.warning("体力不足或正在爬塔中");
    return;
  }

  // 清除之前的超时
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }

  isClimbing.value = true;
  stopFlag = false;
  let climbCount = 0;
  let maxClimb = 100; // 最多批量次数，防止死循环
  // 设置超时保护，60秒后自动重置状态
  climbTimeout.value = setTimeout(() => {
    isClimbing.value = false;
    climbTimeout.value = null;
    stopFlag = true;
    message.info("批量爬塔已超时自动停止");
  }, 60000);

  try {
    const tokenId = tokenStore.selectedToken.id;
    for (let i = 0; i < maxClimb; i++) {
      if (stopFlag) break;
      await getTowerInfo();
      // 体力判断必须每次都刷新
      const tower = roleInfo.value?.role?.tower;
      const energy = tower?.energy || 0;
      if (energy <= 0) break;
      await tokenStore.sendMessageWithPromise(
        tokenId,
        "fight_starttower",
        {},
        10000,
      );
      climbCount++;
      message.success(`第${climbCount}次爬塔命令已发送`);
      await new Promise((res) => setTimeout(res, 2000)); // 每次间隔2秒
    }
    message.success(`已自动爬塔${climbCount}次，体力已耗尽或达到上限。`);
  } catch (error) {
    message.error("批量爬塔失败: " + (error.message || "未知错误"));
  }

  // 清除超时并重置状态
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
};

// 重置爬塔状态的方法
const resetClimbingState = () => {
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
  message.info("爬塔状态已重置");
};

const getTowerInfo = async () => {
  if (!tokenStore.selectedToken) {
    return;
  }

  try {
    const tokenId = tokenStore.selectedToken.id;
    // 检查WebSocket连接状态
    const wsStatus = tokenStore.getWebSocketStatus(tokenId);

    if (wsStatus !== "connected") {
      return;
    }
    // 首先获取角色信息，这包含了塔的数据
    const roleResult = tokenStore.sendMessage(tokenId, "role_getroleinfo");
    // 直接请求塔信息
    const towerResult = tokenStore.sendMessage(tokenId, "tower_getinfo");
    if (!roleResult && !towerResult) {
    }
  } catch (error) {
    // 获取塔信息失败：静默，避免噪声
  }
};

// 监听WebSocket连接状态变化
const wsStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  return tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
});

// 监听WebSocket连接状态，连接成功后自动获取塔信息
watch(wsStatus, (newStatus, oldStatus) => {
  if (newStatus === "connected" && oldStatus !== "connected") {
    // 延迟一点时间让WebSocket完全就绪
    setTimeout(() => {
      getTowerInfo();
    }, 1000);
  }
});

// 监听选中Token变化
watch(
  () => tokenStore.selectedToken,
  (newToken, oldToken) => {
    if (newToken && newToken.id !== oldToken?.id) {
      // 检查WebSocket是否已连接
      const status = tokenStore.getWebSocketStatus(newToken.id);
      if (status === "connected") {
        getTowerInfo();
      }
    }
  },
);

// 监听爬塔结果
watch(
  () => tokenStore.gameData.towerResult,
  (newResult, oldResult) => {
    if (newResult && newResult.timestamp !== oldResult?.timestamp) {
      // 显示爬塔结果消息
      if (newResult.success) {
        message.success("咸将塔挑战成功！");

        if (newResult.autoReward) {
          setTimeout(() => {
            message.success(`自动领取第${newResult.rewardFloor}层奖励`);
          }, 1000);
        }
      } else {
        message.error("咸将塔挑战失败");
      }

      // 重置爬塔状态（仅在未批量时重置）
      if (!stopFlag) {
        setTimeout(() => {
          if (climbTimeout.value) {
            clearTimeout(climbTimeout.value);
            climbTimeout.value = null;
          }
          isClimbing.value = false;
        }, 2000);
      }
    }
  },
  { deep: true },
);

// 生命周期
onMounted(() => {
  // 检查WebSocket客户端
  if (tokenStore.selectedToken) {
    const client = tokenStore.getWebSocketClient(tokenStore.selectedToken.id);
  }

  // 组件挂载时获取塔信息
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
  margin-bottom: 12px;
}

.status-info {
  min-width: 0;
  flex: 1;
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

.card-content {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 13, 32, 0.32);
  padding: 14px;
  margin-bottom: 2px;
  flex: 1;
  display: flex;
  align-items: center;
}

.tower-floor {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;

  .label {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.74);
  }

  .floor-number {
    font-size: 36px;
    font-weight: 800;
    color: rgba(248, 250, 252, 0.96);
    font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
    letter-spacing: 0.02em;
  }
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

  &.active {
    background: linear-gradient(135deg, #6b8dff 0%, #7c6cff 100%);
    color: #f8fbff;

    &:hover {
      transform: translateY(-1px);
      filter: brightness(1.05);
      box-shadow: 0 10px 18px rgba(89, 102, 242, 0.28);
    }
  }

  &.disabled {
    background: rgba(148, 163, 184, 0.26);
    border-color: rgba(148, 163, 184, 0.2);
    color: rgba(226, 232, 240, 0.65);
    cursor: not-allowed;
  }
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

  &:hover {
    background: rgba(255, 255, 255, 0.16);
    border-color: rgba(143, 227, 255, 0.55);
    color: #ffffff;
  }
}

.reset-button {
  width: 100%;
  height: 36px;
  font-size: 18px;
  font-weight: 600;
  border: 1px solid var(--warning-color);
  border-radius: 10px;
  background: transparent;
  color: var(--warning-color);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--warning-color);
    color: white;
  }
}

.debug-info {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-xs);
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-small);
  font-family: monospace;
  word-break: break-all;

  small {
    color: var(--text-secondary);
    font-size: 10px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .tower-status {
    padding: 14px;
  }

  .card-header {
    gap: 10px;
    margin-bottom: 10px;
  }

  .card-content {
    padding: 12px;
  }

  .tower-floor .floor-number {
    font-size: 32px;
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
