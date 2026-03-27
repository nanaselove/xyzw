<template>
  <div class="dashboard-page">
    <!-- 页面主体 -->
    <main class="dashboard-main">
      <div class="main-container">
        <!-- 欢迎区 -->
        <section class="welcome-section">
          <div class="welcome-content">
            <div class="welcome-text">
              <h1>欢迎回来，{{ displayName }}！</h1>
              <p>{{ welcomeSubtitle }}</p>
            </div>
            <div class="welcome-actions">
              <n-button
                type="primary"
                size="large"
                class="welcome-btn welcome-btn-primary"
                @click="router.push('/admin/game-features')"
              >
                进入控制台
              </n-button>
              <n-button
                size="large"
                class="welcome-btn welcome-btn-secondary"
                @click="handleManageTokens"
              >
                管理Token
              </n-button>
            </div>
          </div>
        </section>

        <!-- 快捷入口 -->
        <section class="quick-actions-section">
          <h2 class="section-title">快捷入口</h2>
          <p class="section-subtitle">常用模块一键直达，减少重复操作</p>
          <div class="actions-grid">
            <div
              v-for="action in quickActions"
              :key="action.id"
              class="action-card card-glass"
              @click="handleQuickAction(action)"
            >
              <div class="action-main">
                <div class="action-icon icon-wrap">
                  <component :is="action.icon" />
                </div>
                <div class="action-content">
                  <h3 class="title">{{ action.title }}</h3>
                  <p class="desc">{{ action.description }}</p>
                </div>
              </div>
              <div class="action-meta">
                <span class="status-dot" />
                <n-icon class="action-arrow arrow">
                  <ChevronForward />
                </n-icon>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useMessage } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";
import {
  Cube,
  CheckmarkCircle,
  Add,
  Cloud,
  ChevronForward,
} from "@vicons/ionicons5";

const router = useRouter();
const message = useMessage();
const tokenStore = useTokenStore();

// 当前日期
const currentDate = computed(() => {
  return new Date().toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  });
});

const displayName = computed(() => tokenStore.selectedToken?.name || "指挥官");

const welcomeSubtitle = computed(
  () => `今天是 ${currentDate.value}，控制台已就绪，开始今天的俱乐部管理。`,
);

const quickActions = ref([
  {
    id: 1,
    icon: Cube,
    title: "游戏功能",
    description: "进入核心功能面板，统一管理全部模块",
    action: "game-features",
  },
  {
    id: 2,
    icon: Add,
    title: "添加 Token",
    description: "快速绑定新的角色凭证，立即可用",
    action: "add-token",
  },
  {
    id: 3,
    icon: CheckmarkCircle,
    title: "批量任务",
    description: "一键执行批量日常，减少手动操作",
    action: "batch-daily-tasks",
  },
  {
    id: 4,
    icon: Cloud,
    title: "WebSocket 测试",
    description: "检查连接状态与指令回路，快速联调",
    action: "websocket-test",
  },
]);

const handleManageTokens = () => {
  try {
    router.push("/tokens");
  } catch (error) {
    console.error("导航失败:", error);
    message.error("跳转到 Token 管理页面失败");
  }
};

const handleQuickAction = (action) => {
  switch (action.action) {
    case "game-features":
      router.push("/admin/game-features");
      break;
    case "add-token":
      handleManageTokens();
      break;
    case "execute-tasks":
      router.push("/admin/game-features");
      break;
    case "websocket-test":
      router.push("/websocket-test");
      break;
    case "open-settings":
      router.push("/admin/profile");
      break;
    case "batch-daily-tasks":
      router.push("/admin/batch-daily-tasks");
      break;
  }
};

// 初始化
onMounted(async () => {
  // 初始化 Token Store
  tokenStore.initTokenStore();

  // 无 Token 时跳转至 Token 管理页
  if (!tokenStore.hasTokens) {
    router.replace("/tokens");
  }
});
</script>
<style scoped lang="scss">
.dashboard-page {
  --dashboard-nav-height: 64px;
  height: calc(100dvh - var(--dashboard-nav-height));
  min-height: calc(100dvh - var(--dashboard-nav-height));
  overflow: hidden;
  --panel-title-color: #ffffff;
  --panel-muted-color: rgba(255, 255, 255, 0.76);
  --glass-bg-start: rgba(255, 255, 255, 0.12);
  --glass-bg-end: rgba(255, 255, 255, 0.05);
  --glass-border: rgba(255, 255, 255, 0.18);
  --glass-shadow: 0 8px 28px rgba(19, 10, 46, 0.24);
  --glass-highlight: rgba(143, 227, 255, 0.18);
  --icon-grad-start: #8b7cff;
  --icon-grad-end: #6b8dff;
  --icon-shadow: 0 10px 24px rgba(60, 76, 206, 0.35);
  --arrow-color: rgba(255, 255, 255, 0.72);
  --dot-color: #8fe3ff;
  --corner-glow: rgba(143, 227, 255, 0.34);
  --card-border-grad-start: rgba(143, 227, 255, 0.66);
  --card-border-grad-mid: rgba(124, 108, 255, 0.52);
  --card-border-grad-end: rgba(255, 255, 255, 0.26);
  --cta-primary-start: #84e596;
  --cta-primary-end: #5fd47b;
  --cta-primary-text: #09160d;
  --cta-secondary-bg: rgba(255, 255, 255, 0.08);
  --cta-secondary-border: rgba(255, 255, 255, 0.24);
  --cta-secondary-text: #f8fbff;
}

[data-theme="light"] .dashboard-page {
  --panel-title-color: #1f2a44;
  --panel-muted-color: rgba(31, 42, 68, 0.76);
  --glass-bg-start: rgba(255, 255, 255, 0.92);
  --glass-bg-end: rgba(255, 255, 255, 0.82);
  --glass-border: rgba(148, 163, 184, 0.34);
  --glass-shadow: 0 10px 28px rgba(15, 23, 42, 0.14);
  --glass-highlight: rgba(124, 108, 255, 0.14);
  --icon-grad-start: #7c6cff;
  --icon-grad-end: #7d9aff;
  --icon-shadow: 0 8px 18px rgba(87, 96, 214, 0.28);
  --arrow-color: rgba(47, 60, 92, 0.66);
  --dot-color: #5f7dff;
  --corner-glow: rgba(124, 108, 255, 0.24);
  --card-border-grad-start: rgba(124, 108, 255, 0.56);
  --card-border-grad-mid: rgba(143, 227, 255, 0.45);
  --card-border-grad-end: rgba(148, 163, 184, 0.52);
}

.dashboard-main {
  height: 100%;
  overflow: hidden;
  padding: 16px 24px 18px;
}

.main-container {
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.welcome-section {
  border-radius: 30px;
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-xl);
  background: linear-gradient(135deg, var(--glass-bg-start), var(--glass-bg-end));
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--glass-shadow);
  position: relative;
  overflow: hidden;
}

.welcome-section::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 85% -10%, var(--glass-highlight), transparent 48%),
    linear-gradient(120deg, rgba(255, 255, 255, 0.14), transparent 35%);
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-lg);
}

.welcome-text h1 {
  font-size: clamp(2.05rem, 3.3vw, 2.9rem);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-sm);
  color: var(--panel-title-color);
  line-height: 1.2;
  text-wrap: balance;
  letter-spacing: -0.02em;
}

.welcome-text p {
  font-size: clamp(1rem, 1.22vw, 1.18rem);
  color: var(--panel-muted-color);
  margin: 0;
}

.welcome-actions {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.welcome-btn {
  min-width: 178px;
  height: 50px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-weight: 700;
  letter-spacing: 0.24px;
  box-shadow: 0 8px 18px rgba(13, 18, 39, 0.2);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    filter 0.22s ease;
}

.welcome-btn :deep(.n-button__content) {
  font-size: 18px;
  font-weight: 800;
}

.welcome-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.04);
  box-shadow: 0 12px 24px rgba(13, 18, 39, 0.26);
}

.welcome-btn-primary {
  background: linear-gradient(135deg, var(--cta-primary-start) 0%, var(--cta-primary-end) 100%);
  color: var(--cta-primary-text);
  border-color: rgba(255, 255, 255, 0.26);
  box-shadow: 0 14px 32px rgba(97, 212, 122, 0.24);
}

.welcome-btn-secondary {
  background: linear-gradient(135deg, var(--cta-secondary-bg), rgba(255, 255, 255, 0.04));
  color: var(--cta-secondary-text);
  border-color: var(--cta-secondary-border);
}

.quick-actions-section {
  margin-bottom: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--panel-title-color);
  margin-bottom: 6px;
  text-shadow: 0 1px 8px rgba(5, 8, 20, 0.22);
}

.section-subtitle {
  margin: 0 0 var(--spacing-lg);
  font-size: 15px;
  color: var(--panel-muted-color);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--spacing-lg);
  flex: 1;
  min-height: 0;
  align-content: start;
}

.card-glass {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 98px;
  padding: 18px;
  border-radius: 26px 28px 24px 30px;
  background:
    linear-gradient(135deg, var(--glass-bg-start), var(--glass-bg-end)) padding-box,
    linear-gradient(
      128deg,
      var(--card-border-grad-start) 0%,
      var(--card-border-grad-mid) 42%,
      var(--card-border-grad-end) 76%,
      var(--card-border-grad-start) 100%
    ) border-box;
  border: 1px solid transparent;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--glass-shadow);
  cursor: pointer;
  isolation: isolate;
  overflow: hidden;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.card-glass::before {
  content: "";
  position: absolute;
  width: 92px;
  height: 92px;
  right: -28px;
  top: -30px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, var(--corner-glow) 0%, rgba(124, 108, 255, 0.2) 40%, transparent 72%);
  z-index: 0;
}

.card-glass::after {
  content: "";
  position: absolute;
  top: 16px;
  right: 18px;
  width: 44px;
  height: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.46);
  border-bottom: 1px solid rgba(143, 227, 255, 0.34);
  opacity: 0.75;
  transform: skewX(-28deg);
  pointer-events: none;
  z-index: 1;
}

.card-glass:hover {
  transform: translateY(-3px) scale(1.006);
  box-shadow:
    0 18px 34px rgba(19, 10, 46, 0.38),
    0 0 0 1px rgba(143, 227, 255, 0.16) inset;
}

.action-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.icon-wrap {
  position: relative;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 26%, rgba(255, 255, 255, 0.38), transparent 44%),
    linear-gradient(145deg, rgba(139, 124, 255, 0.68), rgba(107, 141, 255, 0.52));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.36);
  box-shadow: var(--icon-shadow);
  overflow: hidden;
}

.icon-wrap::before {
  content: "";
  position: absolute;
  left: 10px;
  top: 8px;
  width: 30px;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.08));
  filter: blur(0.2px);
  opacity: 0.7;
}

.action-icon :deep(svg) {
  width: 26px;
  height: 26px;
}

.action-content {
  min-width: 0;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: var(--panel-title-color);
  margin: 0;
  line-height: 1.2;
  letter-spacing: 0.2px;
}

.desc {
  margin-top: 4px;
  margin-bottom: 0;
  font-size: 14px;
  color: var(--panel-muted-color);
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-meta {
  position: relative;
  z-index: 1;
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dot-color);
  box-shadow:
    0 0 0 6px color-mix(in srgb, var(--dot-color) 16%, transparent),
    0 0 10px color-mix(in srgb, var(--dot-color) 62%, transparent);
}

.action-arrow {
  color: var(--arrow-color);
  font-size: 20px;
  transition: transform 0.24s ease;
}

.card-glass:hover .action-arrow {
  transform: translateX(2px);
}

@media (max-width: 1024px) {
  .welcome-content {
    flex-direction: column;
    text-align: center;
  }

  .welcome-actions {
    justify-content: center;
  }
}

@media (min-width: 1200px) {
  .dashboard-main {
    padding: 24px 32px 30px;
  }

  .main-container {
    max-width: 1520px;
  }

  .welcome-section {
    padding: 40px 46px;
    border-radius: 32px;
  }

  .welcome-content {
    gap: 26px;
  }

  .welcome-text h1 {
    font-size: clamp(2.5rem, 3vw, 3.35rem);
  }

  .welcome-text p {
    font-size: clamp(1.06rem, 1.14vw, 1.24rem);
  }

  .welcome-actions {
    gap: 14px;
  }

  .welcome-btn {
    min-width: 212px;
    height: 56px;
    padding: 0 24px;
  }

  .welcome-btn :deep(.n-button__content) {
    font-size: 22px;
  }

  .section-title {
    font-size: clamp(2rem, 2.2vw, 2.5rem);
  }

  .section-subtitle {
    font-size: 16px;
  }

  .actions-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .card-glass {
    min-height: 112px;
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    --dashboard-nav-height: 56px;
  }

  .dashboard-main {
    padding: 8px 10px 10px;
  }

  .welcome-section {
    border-radius: 18px;
    padding: 12px;
    margin-bottom: 10px;
  }

  .welcome-text h1 {
    font-size: clamp(1.45rem, 5.6vw, 1.9rem);
    margin-bottom: 4px;
  }

  .welcome-text p {
    font-size: 0.9rem;
  }

  .welcome-actions {
    margin-top: 8px;
    width: 100%;
    flex-direction: row;
    gap: 10px;
  }

  .welcome-btn {
    flex: 1;
    width: auto;
    min-width: 0;
    height: 50px;
    border-radius: 14px;
    padding: 0 14px;
  }

  .welcome-btn :deep(.n-button__content) {
    font-size: 20px;
    font-weight: 800;
  }

  .section-title {
    font-size: 1.7rem;
    margin-bottom: 4px;
  }

  .section-subtitle {
    font-size: 13px;
    margin-bottom: 10px;
  }

  .actions-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .card-glass {
    min-height: 92px;
    padding: 12px;
    border-radius: 18px;
  }

  .icon-wrap {
    width: 44px;
    height: 44px;
  }

  .action-icon :deep(svg) {
    width: 22px;
    height: 22px;
  }

  .title {
    font-size: 20px;
  }

  .desc {
    font-size: 14px;
    line-height: 1.3;
  }
}

@media (max-width: 420px) {
  .dashboard-main {
    padding: 6px 8px 8px;
  }

  .welcome-section {
    padding: 10px;
    margin-bottom: 8px;
  }

  .welcome-actions {
    gap: 8px;
  }

  .welcome-btn {
    height: 48px;
    padding: 0 12px;
  }

  .welcome-btn :deep(.n-button__content) {
    font-size: 18px;
  }

  .card-glass {
    min-height: 86px;
    padding: 10px;
  }

  .action-meta {
    gap: 6px;
  }

  .status-dot {
    width: 6px;
    height: 6px;
  }

  .action-arrow {
    font-size: 16px;
  }

  .title {
    font-size: 18px;
  }

  .desc {
    font-size: 13px;
    display: block;
  }
}
</style>
