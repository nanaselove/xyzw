<template>
  <MyCard class="hang-up" :statusClass="{ active: hangUp.isActive }">
    <template #icon>
      <img src="/icons/174061875626614.png" alt="挂机图标" />
    </template>
    <template #title>
      <h3>挂机时间</h3>
      <p>已挂机：{{ formatTime(hangUp.elapsedTime) }}</p>
    </template>
    <template #badge>
      <span>{{ hangUp.isActive ? "挂机中" : "已完成" }}</span>
    </template>
    <template #default>
      <div class="time-display">
        {{ formatTime(hangUp.remainingTime) }}
      </div>
    </template>
    <template #action>
      <button
        class="action-button secondary"
        :disabled="hangUp.isExtending"
        @click="extendHangUp"
      >
        <span v-if="hangUp.isExtending" class="loading-text">
          <i class="line-md:loading-loop"></i> 加钟中...
        </span>
        <span v-else>加钟</span>
      </button>
      <button
        class="action-button primary"
        :disabled="hangUp.isClaiming"
        @click="claimHangUpReward"
      >
        <span v-if="hangUp.isClaiming" class="loading-text">
          <i class="line-md:loading-loop"></i> 领取中...
        </span>
        <span v-else>领取奖励</span>
      </button>
    </template>
  </MyCard>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useMessage } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";
import MyCard from "../Common/MyCard.vue";

const tokenStore = useTokenStore();
const message = useMessage();
const roleInfo = computed(() => tokenStore.gameData?.roleInfo || null);

const hangUp = ref({
  isActive: false,
  remainingTime: 0,
  elapsedTime: 0,
  lastTime: 0,
  hangUpTime: 0,
  isExtending: false,
  isClaiming: false,
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
  if (!role?.hangUp) return;
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
};

watch(roleInfo, () => syncFromRole(), { deep: true, immediate: true });

let timer = null;
onMounted(() => {
  timer = setInterval(() => {
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

const extendHangUp = async () => {
  if (!tokenStore.selectedToken) return message.warning("请先选择Token");
  const tokenId = tokenStore.selectedToken.id;
  try {
    hangUp.value.isExtending = true;
    message.info("正在加钟...");
    const tasks = [];
    for (let i = 0; i < 4; i++) {
      tasks.push(
        new Promise((resolve) => {
          setTimeout(() => {
            tokenStore.sendMessage(tokenId, "system_mysharecallback", {
              isSkipShareCard: true,
              type: 2,
            });
            resolve();
          }, i * 300);
        }),
      );
    }
    await Promise.all(tasks);
    setTimeout(() => tokenStore.sendMessage(tokenId, "role_getroleinfo"), 1500);
    setTimeout(() => {
      message.success("加钟操作已完成，请查看挂机剩余时间");
      hangUp.value.isExtending = false;
    }, 2500);
  } catch (e) {
    message.error("加钟操作失败: " + (e?.message || "未知错误"));
    hangUp.value.isExtending = false;
  }
};

const claimHangUpReward = async () => {
  if (!tokenStore.selectedToken) return message.warning("请先选择Token");
  const tokenId = tokenStore.selectedToken.id;
  try {
    hangUp.value.isClaiming = true;
    message.info("正在领取挂机奖励...");
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
      message.success("挂机奖励领取完成");
      hangUp.value.isClaiming = false;
    }, 1200);
  } catch (e) {
    message.error("领取挂机奖励失败: " + (e?.message || "未知错误"));
    hangUp.value.isClaiming = false;
  }
};
</script>

<style scoped lang="scss">
.hang-up :deep(.status-card) {
  display: flex;
  flex-direction: column;
  min-height: 240px;
  padding: 18px;
  gap: 12px;
}

.hang-up :deep(.card-header) {
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hang-up :deep(.status-title) {
  min-width: 0;
  flex: 1;
}

.hang-up :deep(.status-badge) {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(248, 250, 252, 0.88);
}

.hang-up :deep(.status-badge.active) {
  background: rgba(52, 211, 153, 0.18);
  border-color: rgba(110, 231, 183, 0.48);
  color: #6ee7b7;
}

.hang-up :deep(.card-content) {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 13, 32, 0.32);
  padding: 14px;
  margin-bottom: 2px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.hang-up :deep(.time-display) {
  width: 100%;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 0.02em;
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
  color: rgba(248, 250, 252, 0.96);
}

.hang-up :deep(.card-action) {
  margin-top: auto;
  padding-top: 2px;
  gap: 10px;
}

.hang-up :deep(.card-action > .action-button) {
  height: 46px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 17px;
  font-weight: 700;
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.hang-up :deep(.card-action > .action-button.secondary) {
  background: rgba(52, 211, 153, 0.18);
  border-color: rgba(52, 211, 153, 0.28);
  color: #d1fae5;
}

.hang-up :deep(.card-action > .action-button.primary) {
  background: linear-gradient(135deg, #34d399 0%, #22c55e 100%);
  color: #04210f;
}

.hang-up :deep(.card-action > .action-button:hover:not(:disabled)) {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.hang-up :deep(.card-action > .action-button.primary:hover:not(:disabled)) {
  box-shadow: 0 10px 18px rgba(34, 197, 94, 0.25);
}

.hang-up :deep(.card-action > .action-button:disabled) {
  background: rgba(148, 163, 184, 0.26);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.65);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.hang-up .loading-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .hang-up :deep(.status-card) {
    padding: 14px;
  }

  .hang-up :deep(.card-header) {
    margin-bottom: 10px;
    gap: 10px;
  }

  .hang-up :deep(.card-content) {
    padding: 12px;
  }

  .hang-up :deep(.time-display) {
    font-size: 30px;
  }

  .hang-up :deep(.card-action > .action-button) {
    height: 44px;
    font-size: 16px;
  }
}
</style>
