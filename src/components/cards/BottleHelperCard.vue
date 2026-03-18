<template>
  <MyCard class="bottle-helper" :statusClass="{ active: state.isRunning }">
    <template #icon>
      <img src="/icons/173746572831736.png" alt="盐罐图标" />
    </template>
    <template #title>
      <h3>盐罐机器人</h3>
      <p>剩余时间</p>
    </template>
    <template #badge>
      <span>{{ state.isRunning ? "运行中" : "已停止" }}</span>
    </template>
    <template #default>
      <div class="time-display">
        {{ formatTime(state.remainingTime) }}
      </div>
    </template>
    <template #action>
      <button class="action-button primary" @click="handleBottleHelper">
        {{ state.isRunning ? "重启服务" : "启动服务" }}
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

const state = ref({
  isRunning: false,
  remainingTime: 0,
  stopTime: 0,
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
  if (!role?.bottleHelpers) return;
  const now = Date.now() / 1000;
  state.value.stopTime = role.bottleHelpers.helperStopTime;
  state.value.isRunning = role.bottleHelpers.helperStopTime > now;
  state.value.remainingTime = Math.max(
    0,
    Math.floor(role.bottleHelpers.helperStopTime - now),
  );
};

watch(roleInfo, () => syncFromRole(), { deep: true, immediate: true });

let timer = null;
onMounted(() => {
  timer = setInterval(() => {
    if (state.value.isRunning && state.value.remainingTime > 0) {
      state.value.remainingTime = Math.max(0, state.value.remainingTime - 1);
      if (state.value.remainingTime <= 0) state.value.isRunning = false;
    }
  }, 1000);
});
onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const handleBottleHelper = () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }
  const tokenId = tokenStore.selectedToken.id;
  tokenStore.sendMessage(tokenId, "bottlehelper_stop");
  setTimeout(() => {
    tokenStore.sendMessage(tokenId, "bottlehelper_start");
    tokenStore.sendMessage(tokenId, "role_getroleinfo");
  }, 500);
  message.info(state.value.isRunning ? "重启盐罐机器人" : "启动盐罐机器人");
};
</script>

<style scoped lang="scss">
.bottle-helper :deep(.status-card) {
  display: flex;
  flex-direction: column;
  min-height: 240px;
  padding: 18px;
  gap: 12px;
}

.bottle-helper :deep(.card-header) {
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.bottle-helper :deep(.status-title) {
  min-width: 0;
  flex: 1;
}

.bottle-helper :deep(.status-badge) {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(248, 250, 252, 0.88);
}

.bottle-helper :deep(.status-badge.active) {
  background: rgba(52, 211, 153, 0.18);
  border-color: rgba(110, 231, 183, 0.48);
  color: #6ee7b7;
}

.bottle-helper :deep(.card-content) {
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

.bottle-helper :deep(.time-display) {
  width: 100%;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 0.02em;
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
  color: rgba(248, 250, 252, 0.96);
}

.bottle-helper :deep(.card-action) {
  margin-top: auto;
  padding-top: 2px;
}

.bottle-helper :deep(.card-action > button) {
  width: 100%;
  height: 48px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #f8fbff;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease;
}

.bottle-helper :deep(.card-action > button:hover) {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.28);
}

@media (max-width: 768px) {
  .bottle-helper :deep(.status-card) {
    padding: 14px;
  }

  .bottle-helper :deep(.card-header) {
    margin-bottom: 10px;
    gap: 10px;
  }

  .bottle-helper :deep(.card-content) {
    padding: 12px;
  }

  .bottle-helper :deep(.time-display) {
    font-size: 30px;
  }

  .bottle-helper :deep(.card-action > button) {
    height: 46px;
    font-size: 17px;
  }
}
</style>
