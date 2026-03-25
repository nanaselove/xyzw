<template>
  <div class="status-card automation-center">
    <div class="card-header">
      <img src="/icons/174061875626614.png" alt="自动化中心" class="status-icon" />
      <div class="status-info">
        <h3>自动化中心</h3>
        <p>挂机系统与挂机机器人统一管理</p>
      </div>
      <div class="status-badge" :class="{ active: hangUp.isActive || bottle.isRunning }">
        <div class="status-dot" />
        <span>{{ centerStatusText }}</span>
      </div>
    </div>

    <div class="automation-grid">
      <section class="automation-block">
        <div class="block-head">
          <h4>挂机系统</h4>
          <span class="mini-badge" :class="{ active: hangUp.isActive }">
            {{ hangUp.isActive ? "运行中" : "待领取" }}
          </span>
        </div>
        <div class="meta-row">
          <span>已挂机 {{ formatTime(hangUp.elapsedTime) }}</span>
          <strong>{{ formatTime(hangUp.remainingTime) }}</strong>
        </div>
        <div class="block-actions">
          <button
            class="action-button secondary"
            :disabled="hangUp.isExtending"
            @click="extendHangUp"
          >
            {{ hangUp.isExtending ? "加钟中..." : "加钟" }}
          </button>
          <button
            class="action-button primary"
            :disabled="hangUp.isClaiming"
            @click="claimHangUpReward"
          >
            {{ hangUp.isClaiming ? "领取中..." : "领取奖励" }}
          </button>
        </div>
      </section>

      <section class="automation-block">
        <div class="block-head">
          <h4>挂机机器人</h4>
          <span class="mini-badge" :class="{ active: bottle.isRunning }">
            {{ bottle.isRunning ? "运行中" : "已停止" }}
          </span>
        </div>
        <div class="meta-row">
          <span>剩余时间</span>
          <strong>{{ formatTime(bottle.remainingTime) }}</strong>
        </div>
        <div class="block-actions single">
          <button class="action-button primary blue" @click="handleBottleHelper">
            {{ bottle.isRunning ? "重启服务" : "启动服务" }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useMessage } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";

const tokenStore = useTokenStore();
const message = useMessage();
const roleInfo = computed(() => tokenStore.gameData?.roleInfo || null);

const bottle = ref({
  isRunning: false,
  remainingTime: 0,
  stopTime: 0,
});

const hangUp = ref({
  isActive: false,
  remainingTime: 0,
  elapsedTime: 0,
  lastTime: 0,
  hangUpTime: 0,
  isExtending: false,
  isClaiming: false,
});

const centerStatusText = computed(() => {
  if (hangUp.value.isActive || bottle.value.isRunning) return "自动化运行中";
  return "等待执行";
});

const formatTime = (seconds) => {
  const total = Math.floor(Number(seconds) || 0);
  if (total <= 0) return "00:00:00";
  const h = Math.floor(total / 3600)
    .toString()
    .padStart(2, "0");
  const m = Math.floor((total % 3600) / 60)
    .toString()
    .padStart(2, "0");
  const s = (total % 60).toString().padStart(2, "0");
  return `${h}:${m}:${s}`;
};

const syncFromRole = () => {
  const role = roleInfo.value?.role;
  if (!role) return;

  if (role.bottleHelpers) {
    const now = Date.now() / 1000;
    bottle.value.stopTime = role.bottleHelpers.helperStopTime;
    bottle.value.isRunning = role.bottleHelpers.helperStopTime > now;
    bottle.value.remainingTime = Math.max(
      0,
      Math.floor(role.bottleHelpers.helperStopTime - now),
    );
  }

  if (role.hangUp) {
    const now = Date.now() / 1000;
    hangUp.value.lastTime = role.hangUp.lastTime;
    hangUp.value.hangUpTime = role.hangUp.hangUpTime;
    const elapsed = now - hangUp.value.lastTime;
    if (elapsed <= hangUp.value.hangUpTime) {
      hangUp.value.remainingTime = Math.floor(hangUp.value.hangUpTime - elapsed);
      hangUp.value.isActive = true;
    } else {
      hangUp.value.remainingTime = 0;
      hangUp.value.isActive = false;
    }
    hangUp.value.elapsedTime = Math.floor(
      hangUp.value.hangUpTime - hangUp.value.remainingTime,
    );
  }
};

watch(roleInfo, syncFromRole, { deep: true, immediate: true });

let timer = null;
onMounted(() => {
  timer = setInterval(() => {
    if (bottle.value.isRunning && bottle.value.remainingTime > 0) {
      bottle.value.remainingTime = Math.max(0, bottle.value.remainingTime - 1);
      if (bottle.value.remainingTime <= 0) bottle.value.isRunning = false;
    }
    if (hangUp.value.isActive && hangUp.value.remainingTime > 0) {
      hangUp.value.remainingTime = Math.max(0, hangUp.value.remainingTime - 1);
      hangUp.value.elapsedTime = hangUp.value.elapsedTime + 1;
      if (hangUp.value.remainingTime <= 0) hangUp.value.isActive = false;
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const handleBottleHelper = () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  const tokenId = tokenStore.selectedToken.id;
  tokenStore.sendMessage(tokenId, "bottlehelper_stop");
  setTimeout(() => {
    tokenStore.sendMessage(tokenId, "bottlehelper_start");
    tokenStore.sendMessage(tokenId, "role_getroleinfo");
  }, 500);
  message.info(bottle.value.isRunning ? "正在重启挂机机器人" : "正在启动挂机机器人");
};

const extendHangUp = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  const tokenId = tokenStore.selectedToken.id;
  try {
    hangUp.value.isExtending = true;
    const tasks = [];
    for (let i = 0; i < 4; i++) {
      tasks.push(
        new Promise((resolve) => {
          setTimeout(() => {
            tokenStore.sendMessage(tokenId, "system_mysharecallback", {
              isSkipShareCard: true,
              type: 2,
            });
            resolve(true);
          }, i * 300);
        }),
      );
    }
    await Promise.all(tasks);
    setTimeout(() => tokenStore.sendMessage(tokenId, "role_getroleinfo"), 1200);
    setTimeout(() => {
      hangUp.value.isExtending = false;
      message.success("加钟完成");
    }, 1800);
  } catch (error) {
    hangUp.value.isExtending = false;
    message.error("加钟失败");
  }
};

const claimHangUpReward = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  const tokenId = tokenStore.selectedToken.id;
  try {
    hangUp.value.isClaiming = true;
    tokenStore.sendMessage(tokenId, "system_mysharecallback");
    setTimeout(
      () => tokenStore.sendMessage(tokenId, "system_claimhangupreward"),
      200,
    );
    setTimeout(
      () =>
        tokenStore.sendMessage(tokenId, "system_mysharecallback", {
          isSkipShareCard: true,
          type: 2,
        }),
      400,
    );
    setTimeout(() => tokenStore.sendMessage(tokenId, "role_getroleinfo"), 600);
    setTimeout(() => {
      hangUp.value.isClaiming = false;
      message.success("挂机奖励已领取");
    }, 1200);
  } catch (error) {
    hangUp.value.isClaiming = false;
    message.error("领取失败");
  }
};
</script>

<style scoped lang="scss">
.automation-center {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 280px;
  padding: 18px;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background:
    linear-gradient(145deg, rgba(109, 90, 196, 0.24), rgba(31, 36, 70, 0.64)),
    radial-gradient(circle at 18% 12%, rgba(124, 108, 255, 0.16), transparent 30%),
    radial-gradient(circle at 84% 0%, rgba(96, 165, 250, 0.1), transparent 28%);
  background-blend-mode: screen, normal, screen;
  backdrop-filter: blur(18px) saturate(165%);
  -webkit-backdrop-filter: blur(18px) saturate(165%);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
  flex-shrink: 0;
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
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(248, 250, 252, 0.88);
}

.status-badge.active {
  border-color: rgba(110, 231, 183, 0.48);
  background: rgba(52, 211, 153, 0.18);
  color: #6ee7b7;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.automation-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 2px;
}

.automation-block {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.04)
  );
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.block-head h4 {
  margin: 0;
  font-size: 15px;
  color: rgba(248, 250, 252, 0.96);
}

.mini-badge {
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  color: rgba(248, 250, 252, 0.8);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.mini-badge.active {
  color: #6ee7b7;
  background: rgba(52, 211, 153, 0.18);
  border-color: rgba(110, 231, 183, 0.42);
}

.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.meta-row span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.meta-row strong {
  font-size: 20px;
  color: #d6f5ff;
  letter-spacing: 0.02em;
  text-shadow: 0 0 12px rgba(111, 231, 255, 0.35);
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
}

.block-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.block-actions.single {
  grid-template-columns: 1fr;
}

.action-button {
  height: 40px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 15px;
  font-weight: 700;
  color: #f8fbff;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
  position: relative;
  overflow: hidden;
}

.action-button.primary {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  border-color: rgba(167, 139, 250, 0.44);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.18);
}

.action-button.primary.blue {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  border-color: rgba(167, 139, 250, 0.44);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.18);
}

.action-button.secondary {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.84), rgba(99, 102, 241, 0.8));
  border-color: rgba(167, 139, 250, 0.4);
  color: #f8fbff;
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.14);
}

.action-button:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.04);
  box-shadow: 0 12px 22px rgba(99, 102, 241, 0.24);
}

.action-button:disabled {
  background: rgba(148, 163, 184, 0.26);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.65);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

@media (max-width: 768px) {
  .automation-center {
    padding: 14px;
  }

  .automation-grid {
    grid-template-columns: 1fr;
  }

  .meta-row strong {
    font-size: 18px;
  }
}
</style>
