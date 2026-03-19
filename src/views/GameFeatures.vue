<template>
  <div class="game-features-page">
    <div class="page-header">
      <div class="container">
        <div class="status-topbar">
          <div class="status-left">
            <span class="status-dot" :class="connectionStatus" />
            <span class="status-text">{{ connectionStatusText }}</span>
            <span class="status-token">
              Token: {{ tokenStore.selectedToken?.name || "未选择" }}
            </span>
            <span class="status-latency">延迟: {{ latencyText }}</span>
          </div>

          <n-button text class="status-link" @click="toggleConnection">
            {{ isConnected ? "断开" : "重连" }}
          </n-button>
        </div>
      </div>
    </div>

    <div class="features-grid-section">
      <div class="container">
        <GameStatus />
      </div>
    </div>

    <div class="ws-status-section">
      <div class="container">
        <div class="ws-status-strip">
          <span class="strip-item">
            WebSocket：<strong :class="connectionClass">{{ connectionStatusText }}</strong>
          </span>
          <span class="strip-item" v-if="tokenStore.selectedToken">
            当前 Token：{{ tokenStore.selectedToken.name }}
          </span>
          <span class="strip-item" v-if="lastActivity">最近活动：{{ lastActivity }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useMessage } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";

const router = useRouter();
const message = useMessage();
const tokenStore = useTokenStore();

const connectionStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  const status = tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
  return status === "connected" ? "connected" : "disconnected";
});

const connectionStatusText = computed(() => {
  if (!tokenStore.selectedToken) return "未选择 Token";
  return connectionStatus.value === "connected" ? "已连接" : "未连接";
});

const connectionClass = computed(() =>
  connectionStatus.value === "connected" ? "status-connected" : "status-disconnected",
);

const isConnected = computed(() => connectionStatus.value === "connected");

const lastActivity = computed(() => {
  if (!tokenStore.selectedToken) return "";
  const conn = tokenStore.wsConnections?.[tokenStore.selectedToken.id];
  const ts = conn?.lastMessage?.timestamp;
  if (!ts) return "";
  return new Date(ts).toLocaleTimeString();
});

const latencyText = computed(() => {
  if (!tokenStore.selectedToken) return "--";
  const conn = tokenStore.wsConnections?.[tokenStore.selectedToken.id];
  const ts = conn?.lastMessage?.timestamp || conn?.connectedAt;
  if (!ts) return "--";
  const diff = Math.max(1, Date.now() - new Date(ts).getTime());
  return `${diff}ms`;
});

const connectWebSocket = () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择一个 Token");
    router.push("/tokens");
    return;
  }

  try {
    const tokenId = tokenStore.selectedToken.id;
    tokenStore.createWebSocketConnection(tokenId, tokenStore.selectedToken.token);
    message.info("正在建立 WebSocket 连接...");

    setTimeout(async () => {
      const status = tokenStore.getWebSocketStatus(tokenId);
      if (status === "connected") {
        message.success("WebSocket 连接成功");
        await initializeGameData();
      }
    }, 2000);
  } catch (error) {
    message.error("WebSocket 连接失败");
  }
};

const disconnectWebSocket = () => {
  if (!tokenStore.selectedToken) return;
  tokenStore.closeWebSocketConnection(tokenStore.selectedToken.id);
  message.info("WebSocket 已断开");
};

const toggleConnection = () => {
  if (isConnected.value) {
    disconnectWebSocket();
  } else {
    connectWebSocket();
  }
};

const initializeGameData = async () => {
  if (!tokenStore.selectedToken) return;
  try {
    const tokenId = tokenStore.selectedToken.id;
    tokenStore.sendMessage(tokenId, "role_getroleinfo");
    tokenStore.sendMessage(tokenId, "tower_getinfo");
    tokenStore.sendMessage(tokenId, "evotower_getinfo");
    tokenStore.sendMessage(tokenId, "presetteam_getinfo");
    const res = await tokenStore.sendMessageWithPromise(tokenId, "fight_startlevel");
    tokenStore.setBattleVersion(res?.battleData?.version);
  } catch {
    // silent init
  }
};

onMounted(() => {
  if (!tokenStore.selectedToken) return;
  const status = tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
  if (status !== "connected") {
    connectWebSocket();
  } else {
    initializeGameData();
  }
});

watch(
  () => {
    if (!tokenStore.selectedToken) return { status: "disconnected", lastError: null };
    const conn = tokenStore.wsConnections[tokenStore.selectedToken.id];
    return { status: conn?.status, lastError: conn?.lastError };
  },
  (cur) => {
    if (!cur || cur.status !== "error" || !cur.lastError) return;

    const err = String(cur.lastError.error || "").toLowerCase();
    if (err.includes("token") && err.includes("expired")) {
      const importMethod = tokenStore.selectedToken?.importMethod;
      if (importMethod === "url" || importMethod === "bin" || importMethod === "wxQrcode") {
        message.warning("Token 已过期，正在尝试自动刷新...");
        return;
      }
      message.error("当前 Token 已过期，请重新导入");
      router.push("/tokens");
    }
  },
  { deep: true },
);
</script>

<style scoped lang="scss">
.game-features-page {
  min-height: 100dvh;
  background: linear-gradient(135deg, #0f172a 0%, #1f2937 100%);
  padding-bottom: calc(var(--spacing-md) + env(safe-area-inset-bottom));
}

.page-header {
  position: sticky;
  top: 56px;
  z-index: 8;
  padding: 10px 0;
  background: linear-gradient(180deg, rgba(6, 10, 24, 0.74), rgba(6, 10, 24, 0.46));
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
}

.status-topbar {
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(15, 23, 42, 0.58);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 12px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}

.status-dot.connected {
  background: #4ade80;
  box-shadow: 0 0 12px rgba(74, 222, 128, 0.8);
}

.status-dot.disconnected {
  background: #f87171;
  box-shadow: 0 0 12px rgba(248, 113, 113, 0.72);
}

.status-text,
.status-token,
.status-latency {
  color: rgba(241, 245, 249, 0.86);
  font-size: 13px;
  white-space: nowrap;
}

.status-token {
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-link {
  color: rgba(191, 219, 254, 0.95);
  font-weight: 700;
}

.features-grid-section {
  padding: 14px 0;
}

.ws-status-section {
  padding: 0 0 16px;
}

.ws-status-strip {
  min-height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding: 8px 12px;
  color: rgba(226, 232, 240, 0.76);
}

.strip-item {
  font-size: 12px;
  min-width: 0;
  white-space: normal;
  line-height: 1.35;
  word-break: break-word;
}

.status-connected {
  color: #6ee7b7;
}

.status-disconnected {
  color: #fda4af;
}

@media (max-width: 768px) {
  .container {
    padding: 0 var(--spacing-md);
  }

  .page-header {
    top: 56px;
    padding: 8px 0;
  }

  .status-topbar {
    min-height: 40px;
    padding: 0 10px;
  }

  .status-left {
    gap: 8px;
  }

  .status-text,
  .status-token,
  .status-latency,
  .strip-item {
    font-size: 12px;
  }

  .ws-status-strip {
    display: grid;
    grid-template-columns: 1fr;
    gap: 6px;
    align-items: start;
    justify-items: start;
    overflow-x: hidden;
  }
}
</style>
