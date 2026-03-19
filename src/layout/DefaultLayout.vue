<template>
  <div class="default-layout">
    <!-- 顶部导航 -->
    <nav class="dashboard-nav">
      <div class="nav-container">
        <div class="nav-brand">
          <img :alt="APP_NAME" src="/icons/xiaoyugan.png" class="brand-logo" />
          <div class="brand-toggle" @click="isMobileMenuOpen = true">
            <n-icon>
              <Menu />
            </n-icon>
            <span class="brand-text">{{ APP_NAME }}</span>
          </div>
        </div>

        <div class="nav-menu">
          <router-link
            to="/admin/dashboard"
            class="nav-item"
            active-class="active"
          >
            <n-icon>
              <Home />
            </n-icon>
            <span>首页</span>
          </router-link>
          <router-link
            to="/admin/game-features"
            class="nav-item"
            active-class="active"
          >
            <n-icon>
              <Cube />
            </n-icon>
            <span>游戏功能</span>
          </router-link>
          <router-link to="/tokens" class="nav-item" active-class="active">
            <n-icon>
              <PersonCircle />
            </n-icon>
            <span>Token管理</span>
          </router-link>
          <router-link
            to="/admin/batch-daily-tasks"
            class="nav-item"
            active-class="active"
          >
            <n-icon>
              <Layers />
            </n-icon>
            <span>批量日常</span>
          </router-link>
          <router-link
            to="/admin/message-test"
            class="nav-item"
            active-class="active"
          >
            <n-icon>
              <ChatbubbleEllipsesSharp />
            </n-icon>
            <span>消息测试</span>
          </router-link>
          <router-link to="/admin/legion-war" class="nav-item" active-class="active"  v-if="isNowInLegionWarTime()" >
            <n-icon>
              <LockOpen />
            </n-icon>
            <span>实时盐场</span>
          </router-link>
        </div>

        <div class="nav-user">
          <!-- 主题切换按钮 -->
          <ThemeToggle />

          <n-dropdown :options="userMenuOptions" @select="handleUserAction">
            <div class="user-info">
              <n-avatar
                :src="selectedToken?.avatar || '/icons/xiaoyugan.png'"
                size="medium"
                fallback-src="/icons/xiaoyugan.png"
              />
              <span class="username">{{
                selectedToken?.name || "未选择Token"
              }}</span>
              <n-icon>
                <ChevronDown />
              </n-icon>
            </div>
          </n-dropdown>
        </div>
      </div>
    </nav>
    <n-drawer
      v-model:show="isMobileMenuOpen"
      placement="left"
      style="width: 260px"
    >
      <div class="drawer-menu">
        <router-link
          to="/admin/dashboard"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Home />
          </n-icon>
          <span>首页</span>
        </router-link>
        <router-link
          to="/admin/game-features"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Cube />
          </n-icon>
          <span>游戏功能</span>
        </router-link>
        <router-link
          to="/tokens"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <PersonCircle />
          </n-icon>
          <span>Token管理</span>
        </router-link>
        <router-link
          to="/admin/daily-tasks"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Settings />
          </n-icon>
          <span>任务管理</span>
        </router-link>
        <router-link
          to="/admin/batch-daily-tasks"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Layers />
          </n-icon>
          <span>批量日常</span>
        </router-link>
        <router-link
          to="/admin/message-test"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <ChatbubbleEllipsesSharp />
          </n-icon>
          <span>消息测试</span>
        </router-link>
          <router-link to="/admin/legion-war" class="nav-item" active-class="active"  v-if="isNowInLegionWarTime()" >
            <n-icon>
              <LockOpen />
            </n-icon>
            <span>实时盐场</span>
          </router-link>
        <router-link
          to="/admin/profile"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Settings />
          </n-icon>
          <span>个人设置</span>
        </router-link>
      </div>
    </n-drawer>
    <div class="main">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import {
  useTokenStore,
  selectedToken,
  selectedTokenId,
} from "@/stores/tokenStore";
import ThemeToggle from "@/components/Common/ThemeToggle.vue";
import {
  Home,
  PersonCircle,
  Cube,
  Settings,
  ChevronDown,
  ChatbubbleEllipsesSharp,
  LockClosedSharp,LockOpen,
  Menu,
  Layers,
} from "@vicons/ionicons5";

import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ref } from 'vue'
import { isNowInLegionWarTime } from '@/utils/clubBattleUtils'
import { APP_NAME } from '@/constants/appMeta'

const tokenStore = useTokenStore();
const router = useRouter();
const message = useMessage();

const isMobileMenuOpen = ref(false);

const userMenuOptions = [
  {
    label: "清除所有Token并退出",
    key: "logout",
  },
];

// 方法
const handleUserAction = async (key) => {
  switch (key) {
    case "logout":
      await tokenStore.clearAllTokens();
      message.success("已清除所有Token");
      router.push("/tokens");
      break;
  }
};
</script>

<style scoped lang="scss">
// 导航栏
.dashboard-nav {
  background: rgba(6, 10, 24, 0.28);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  padding: 0 var(--spacing-lg);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.nav-container {
  display: flex;
  align-items: center;
  height: 64px;
  max-width: 1400px;
  margin: 0 auto;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-right: var(--spacing-xl);
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--border-radius-small);
}

.brand-text {
  display: inline-block;
  min-width: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: #f8fafc;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-toggle {
  display: none;
  align-items: center;
  gap: var(--spacing-xs);
  cursor: pointer;
  font-size: var(--font-size-lg);
}

.brand-toggle .n-icon {
  font-size: inherit;
}

.nav-menu {
  display: flex;
  gap: var(--spacing-md);
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-medium);
  color: rgba(248, 250, 252, 0.88);
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(148, 163, 184, 0.28);
    color: #ffffff;
    transform: translateY(-1px);
    box-shadow: 0 8px 16px rgba(2, 6, 23, 0.22);
  }

  &.active {
    background: linear-gradient(135deg, #6ee7b7 0%, #34d399 50%, #22c55e 100%);
    color: #052e16;
    box-shadow: 0 8px 18px rgba(2, 6, 23, 0.22);
  }
}

.nav-item :deep(.n-icon) {
  font-size: 18px;
  flex: 0 0 auto;
}

.nav-user {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  border-radius: var(--border-radius-medium);
  cursor: pointer;
  transition: background var(--transition-fast);

  &:hover {
    background: rgba(148, 163, 184, 0.28);
    box-shadow: 0 6px 14px rgba(2, 6, 23, 0.2);
  }
}

.user-info :deep(.n-icon) {
  font-size: 14px;
}

.username {
  font-weight: var(--font-weight-medium);
  color: #f8fafc;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .dashboard-nav {
    padding: 0 var(--spacing-sm);
  }

  .nav-item span {
    display: none;
  }

  .nav-menu {
    display: none;
  }

  .nav-item {
    padding: var(--spacing-sm);
    flex: 0 0 auto;
  }

  .nav-container {
    height: 56px;
  }

  .nav-brand {
    margin-right: 0;
    flex: 1;
    min-width: 0;
  }

  .brand-logo {
    display: none;
  }

  .brand-toggle {
    display: inline-flex;
    align-items: center;
    width: 100%;
    min-width: 0;
    font-size: var(--font-size-md);
    max-width: 74vw;
    gap: 6px;
  }

  .brand-toggle .brand-text {
    flex: 1;
    min-width: 0;
  }

  .nav-user {
    margin-left: auto;
    justify-content: flex-end;
    gap: var(--spacing-xs);
    flex-shrink: 0;
  }

  .username {
    display: none;
  }

  .user-info {
    padding: var(--spacing-xs);
  }

  .nav-item :deep(.n-icon),
  .drawer-item :deep(.n-icon),
  .brand-toggle :deep(.n-icon) {
    font-size: 19px;
  }
}

.drawer-menu {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.08), transparent 45%),
    linear-gradient(
      165deg,
      rgba(2, 6, 23, 0.72) 0%,
      rgba(15, 23, 42, 0.78) 56%,
      rgba(3, 7, 18, 0.86) 100%
    );
  min-height: 100%;
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-medium);
  color: rgba(248, 250, 252, 0.88);
  text-decoration: none;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.drawer-item:hover {
  background: rgba(148, 163, 184, 0.24);
  color: #ffffff;
  transform: translateY(-1px);
}

.drawer-item :deep(.n-icon) {
  font-size: 18px;
  flex: 0 0 auto;
}

.drawer-item.router-link-active {
  background: linear-gradient(135deg, #6ee7b7 0%, #34d399 50%, #22c55e 100%);
  color: #052e16;
}

/* 禁用样式：灰化、鼠标禁止、无hover效果 */
.nav-item.disabled {
  background: #cccccc;
  color: #999999;
  cursor: not-allowed; /* 鼠标样式：禁止 */
  pointer-events: none; /* 可选：直接禁用所有鼠标事件（比阻止click更彻底） */
}
</style>
