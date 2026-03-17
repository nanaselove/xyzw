<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <nav class="navbar glass">
      <div class="container">
        <div class="nav-content">
          <div class="nav-brand">
            <img :alt="APP_NAME" src="/icons/xiaoyugan.png" class="brand-logo" />
            <span class="brand-text">{{ APP_NAME }}</span>
          </div>

          <div class="mobile-menu-button">
            <n-button text @click="isMobileMenuOpen = true">
              <n-icon>
                <Menu />
              </n-icon>
            </n-button>
          </div>

          <div class="nav-actions">
            <template v-if="!authStore.isAuthenticated">
              <n-button
                size="large"
                round
                class="nav-btn nav-login-btn"
                @click="router.push('/login')"
              >
                登录
              </n-button>
              <n-button
                size="large"
                round
                class="nav-btn nav-register-btn"
                @click="router.push('/register')"
              >
                注册
              </n-button>
            </template>
            <template v-else>
              <n-button
                size="large"
                round
                class="nav-btn nav-enter-btn"
                @click="router.push('/admin/dashboard')"
              >
                进入控制台
              </n-button>
            </template>
          </div>
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
          to="/"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Ribbon />
          </n-icon>
          <span>首页</span>
        </router-link>
        <router-link
          to="/admin/dashboard"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Settings />
          </n-icon>
          <span>控制台</span>
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
          to="/changelog"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon>
            <Ribbon />
          </n-icon>
          <span>更新日志</span>
        </router-link>
        <div class="drawer-actions">
          <n-button
            block
            round
            class="drawer-btn drawer-login-btn"
            @click="
              router.push('/login');
              isMobileMenuOpen = false;
            "
            >登录</n-button
          >
          <n-button
            block
            round
            class="drawer-btn drawer-register-btn"
            @click="
              router.push('/register');
              isMobileMenuOpen = false;
            "
            >注册</n-button
          >
        </div>
      </div>
    </n-drawer>

    <!-- 主要内容 -->
    <main class="main-content">
      <!-- 英雄区域 -->
      <section class="hero-section">
        <div class="container">
          <div class="hero-content">
            <div class="hero-text">
              <h1 class="hero-title">专业的游戏管理平台</h1>
              <p class="hero-subtitle">让游戏变得更简单，让管理变得更高效</p>
              <div class="hero-actions">
                <n-button
                  size="large"
                  round
                  class="hero-button hero-primary-button"
                  @click="
                    router.push(
                      authStore.isAuthenticated
                        ? '/admin/dashboard'
                        : '/register',
                    )
                  "
                >
                  {{ authStore.isAuthenticated ? "进入控制台" : "立即开始" }}
                </n-button>
                <n-button
                  size="large"
                  round
                  class="hero-button hero-secondary-button"
                  @click="scrollToFeatures"
                >
                  了解更多
                </n-button>
              </div>
            </div>

            <div class="hero-visual">
              <div class="feature-cards">
                <div
                  v-for="card in featureCards"
                  :key="card.id"
                  class="feature-card"
                >
                  <div class="card-icon">
                    <component :is="card.icon" />
                  </div>
                  <div class="card-content">
                    <h3>{{ card.title }}</h3>
                    <p>{{ card.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 功能特性 -->
      <section ref="featuresSection" class="features-section">
        <div class="container">
          <div class="section-header">
            <h2 class="section-title">核心功能</h2>
            <p class="section-subtitle">为您提供全方位的游戏管理解决方案</p>
          </div>

          <div class="features-grid">
            <div
              v-for="feature in features"
              :key="feature.id"
              class="feature-item"
            >
              <div class="feature-icon">
                <component :is="feature.icon" />
              </div>
              <h3 class="feature-title">
                {{ feature.title }}
              </h3>
              <p class="feature-description">
                {{ feature.description }}
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- 统计数据 -->
      <section class="stats-section">
        <div class="container">
          <div class="stats-grid">
            <div v-for="stat in stats" :key="stat.id" class="stat-item">
              <div class="stat-number">
                {{ stat.number }}
              </div>
              <div class="stat-label">
                {{ stat.label }}
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-brand">
            <img :alt="APP_NAME" src="/icons/xiaoyugan.png" class="footer-logo" />
            <span class="footer-text">{{ APP_NAME }}</span>
          </div>
          <div class="footer-links">
            <router-link to="/changelog" class="footer-link">
              更新日志
            </router-link>
            <a href="#" class="footer-link">关于我们</a>
            <a href="#" class="footer-link">隐私政策</a>
            <a href="#" class="footer-link">服务条款</a>
            <a href="#" class="footer-link">联系我们</a>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; xiao七濑 All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { PersonCircle, Cube, Ribbon, Settings, Menu } from "@vicons/ionicons5";
import { APP_NAME } from "@/constants/appMeta";

const router = useRouter();
const authStore = useAuthStore();
const featuresSection = ref(null);
const isMobileMenuOpen = ref(false);

// 功能卡片数据
const featureCards = ref([
  {
    id: 1,
    icon: markRaw(PersonCircle),
    title: "角色管理",
    description: "统一管理游戏角色",
  },
  {
    id: 2,
    icon: markRaw(Cube),
    title: "任务系统",
    description: "自动化日常任务",
  },
  {
    id: 3,
    icon: markRaw(Ribbon),
    title: "数据统计",
    description: "全面的数据分析",
  },
]);

// 功能特性数据
const features = ref([
  {
    id: 1,
    icon: markRaw(PersonCircle),
    title: "角色管理",
    description: "轻松管理多个游戏角色，统一查看角色信息、等级进度和装备状态",
  },
  {
    id: 2,
    icon: markRaw(Cube),
    title: "任务自动化",
    description: "智能日常任务系统，自动完成重复性任务，节省您的宝贵时间",
  },
  {
    id: 3,
    icon: markRaw(Ribbon),
    title: "数据分析",
    description: "详细的数据统计和分析报告，帮助您更好地了解游戏进度",
  },
  {
    id: 4,
    icon: markRaw(Settings),
    title: "个性化设置",
    description: "灵活的配置选项，根据您的需求定制最适合的管理方案",
  },
]);

// 统计数据
const stats = ref([
  { id: 1, number: "1000+", label: "活跃用户" },
  { id: 2, number: "50K+", label: "管理角色" },
  { id: 3, number: "100K+", label: "完成任务" },
  { id: 4, number: "99.9%", label: "系统稳定性" },
]);

// 滚动到功能区域
const scrollToFeatures = () => {
  if (featuresSection.value) {
    featuresSection.value.scrollIntoView({
      behavior: "smooth",
    });
  }
};

onMounted(() => {
  // 初始化认证状态
  authStore.initAuth();
});
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100dvh;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.12), transparent 45%),
    linear-gradient(
      165deg,
      rgba(2, 6, 23, 0.58) 0%,
      rgba(15, 23, 42, 0.66) 56%,
      rgba(3, 7, 18, 0.78) 100%
    ),
    url("https://t.alcy.cc/moez");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  position: relative;
  overflow-x: hidden;
  padding-bottom: calc(var(--spacing-md) + env(safe-area-inset-bottom));
}

.drawer-menu {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-medium);
  color: var(--text-secondary);
  text-decoration: none;

  :deep(.n-icon) {
    font-size: 18px;
    flex: 0 0 auto;
  }
}

.drawer-item.router-link-active {
  background: var(--primary-color-light);
  color: var(--primary-color);
}

.drawer-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

// 导航栏
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-fixed);
  padding: var(--spacing-md) 0;
  background: rgba(6, 10, 24, 0.25);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mobile-menu-button {
  display: none;

  :deep(.n-button) {
    color: #f8fafc;
  }

  :deep(.n-icon) {
    font-size: 22px;
  }
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: var(--border-radius-small);
}

.brand-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: white;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.55);
}

.nav-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.nav-btn {
  min-width: 88px;
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.3px;
  border: 1px solid transparent;
  box-shadow: 0 8px 18px rgba(2, 6, 23, 0.24);
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast),
    filter var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(2, 6, 23, 0.32);
    filter: brightness(1.05);
  }
}

.nav-login-btn {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), rgba(226, 232, 240, 0.2));
  color: #f8fafc;
  border-color: rgba(255, 255, 255, 0.42);
}

.nav-register-btn,
.nav-enter-btn {
  background: linear-gradient(135deg, #5eead4 0%, #34d399 45%, #22c55e 100%);
  color: #052e16;
  border-color: rgba(255, 255, 255, 0.24);
}

.drawer-btn {
  border-radius: 999px;
  font-weight: 700;
  border: 1px solid transparent;
}

.drawer-login-btn {
  background: linear-gradient(135deg, rgba(148, 163, 184, 0.28), rgba(71, 85, 105, 0.32));
  color: #f8fafc;
  border-color: rgba(255, 255, 255, 0.22);
}

.drawer-register-btn {
  background: linear-gradient(135deg, #6ee7b7 0%, #34d399 50%, #22c55e 100%);
  color: #052e16;
  border-color: rgba(255, 255, 255, 0.24);
}

// 主要内容
.main-content {
  padding-top: 80px;
}

// 英雄区域
.hero-section {
  padding: var(--spacing-2xl) 0;
  min-height: 80vh;
  display: flex;
  align-items: center;
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-2xl);
  align-items: center;
}

.hero-text {
  color: white;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: var(--font-weight-bold);
  line-height: var(--line-height-tight);
  margin-bottom: var(--spacing-md);
  color: #f8fafc;
  text-shadow:
    0 2px 6px rgba(0, 0, 0, 0.55),
    0 12px 28px rgba(0, 0, 0, 0.35);
}

.hero-subtitle {
  font-size: var(--font-size-xl);
  color: rgba(255, 255, 255, 0.96);
  margin-bottom: var(--spacing-xl);
  line-height: var(--line-height-relaxed);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.hero-actions {
  display: flex;
  gap: var(--spacing-md);
}

.hero-button {
  padding: 0 var(--spacing-xl);
  height: 52px;
  font-size: var(--font-size-lg);
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.3px;
  border: 1px solid transparent;
  box-shadow: 0 10px 22px rgba(2, 6, 23, 0.25);
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast),
    filter var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 28px rgba(2, 6, 23, 0.32);
    filter: brightness(1.05);
  }
}

.hero-primary-button {
  background: linear-gradient(135deg, #34d399 0%, #22c55e 50%, #16a34a 100%);
  color: #052e16;
  border-color: rgba(255, 255, 255, 0.22);
}

.hero-secondary-button {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.5), rgba(30, 41, 59, 0.55));
  color: #f8fafc;
  border-color: rgba(255, 255, 255, 0.42);
}

// 功能卡片
.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-md);
}

.feature-card {
  background: rgba(7, 13, 30, 0.38);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 24px;
  padding: var(--spacing-lg);
  transition: all var(--transition-normal);
  box-shadow: 0 12px 28px rgba(2, 6, 23, 0.28);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 36px rgba(2, 6, 23, 0.34);
  }
}

.card-icon {
  width: 48px;
  height: 48px;
  color: #fff;
  margin-bottom: var(--spacing-md);

  :deep(svg) {
    width: 100%;
    height: 100%;
  }
}

.card-content h3 {
  color: white;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--spacing-sm);
}

.card-content p {
  color: rgba(255, 255, 255, 0.8);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
}

// 功能特性区域
.features-section {
  position: relative;
  padding: var(--spacing-2xl) 0;
  background: rgba(7, 13, 30, 0.26);
  backdrop-filter: blur(8px);
  overflow: hidden;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.features-section::before,
.features-section::after {
  content: "";
  position: absolute;
  pointer-events: none;
}

.features-section::before {
  width: 460px;
  height: 460px;
  top: -190px;
  left: -130px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(94, 234, 212, 0.16), transparent 70%);
}

.features-section::after {
  width: 520px;
  height: 520px;
  right: -220px;
  bottom: -260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18), transparent 68%);
}

.features-section > .container {
  position: relative;
  z-index: 1;
}

.section-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.section-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: #f8fafc;
  margin-bottom: var(--spacing-md);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.section-subtitle {
  font-size: var(--font-size-lg);
  color: rgba(255, 255, 255, 0.9);
  max-width: 600px;
  margin: 0 auto;
  line-height: var(--line-height-relaxed);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-xl);
  align-items: stretch;
}

.feature-item {
  position: relative;
  overflow: hidden;
  text-align: center;
  padding: var(--spacing-xl);
  background: linear-gradient(145deg, rgba(9, 16, 35, 0.52), rgba(15, 23, 42, 0.4));
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(12px);
  border-radius: 28px;
  box-shadow: 0 14px 30px rgba(2, 6, 23, 0.22);
  transition: all var(--transition-normal);

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(110, 231, 183, 0.5);
    box-shadow: 0 20px 38px rgba(2, 6, 23, 0.34);
  }
}

.feature-item::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    130deg,
    rgba(255, 255, 255, 0.12),
    rgba(255, 255, 255, 0.02) 34%,
    transparent 60%
  );
  pointer-events: none;
}

.feature-item > * {
  position: relative;
  z-index: 1;
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: 0 auto var(--spacing-lg);
  background: linear-gradient(145deg, rgba(94, 234, 212, 0.28), rgba(52, 211, 153, 0.22));
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25);
  color: rgba(255, 255, 255, 0.95);

  :deep(svg) {
    width: 36px;
    height: 36px;
  }
}

.feature-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: #f8fafc;
  margin-bottom: var(--spacing-md);
}

.feature-description {
  color: rgba(255, 255, 255, 0.88);
  line-height: var(--line-height-relaxed);
  max-width: 320px;
  margin: 0 auto;
}

// 统计区域
.stats-section {
  position: relative;
  padding: var(--spacing-2xl) 0;
  background: rgba(7, 13, 30, 0.2);
  backdrop-filter: blur(6px);
  overflow: hidden;
}

/* 深色主题下统计区背景 */
[data-theme="dark"] .stats-section {
  background: rgba(7, 13, 30, 0.3);
}

.stats-section::before {
  content: "";
  position: absolute;
  width: 420px;
  height: 420px;
  right: -130px;
  top: -150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(110, 231, 183, 0.14), transparent 68%);
  pointer-events: none;
}

.stats-section > .container {
  position: relative;
  z-index: 1;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-xl);
}

.stat-item {
  position: relative;
  overflow: hidden;
  text-align: center;
  color: white;
  padding: var(--spacing-lg);
  border-radius: 26px;
  background: linear-gradient(145deg, rgba(9, 16, 35, 0.46), rgba(15, 23, 42, 0.34));
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  box-shadow: 0 12px 26px rgba(2, 6, 23, 0.22);
  transition: all var(--transition-normal);
}

.stat-item::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.02) 38%,
    transparent 65%
  );
  pointer-events: none;
}

.stat-item > * {
  position: relative;
  z-index: 1;
}

.stat-item:hover {
  transform: translateY(-3px);
  border-color: rgba(110, 231, 183, 0.48);
  box-shadow: 0 18px 32px rgba(2, 6, 23, 0.3);
}

.stat-number {
  font-size: 3rem;
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-sm);
  color: #86efac;
  text-shadow: 0 4px 14px rgba(0, 0, 0, 0.28);
}

.stat-label {
  font-size: var(--font-size-lg);
  color: rgba(255, 255, 255, 0.92);
}

// 页脚
.footer {
  background: rgba(0, 0, 0, 0.76);
  color: white;
  padding: var(--spacing-xl) 0;
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.footer-logo {
  width: 24px;
  height: 24px;
}

.footer-text {
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.2px;
}

.footer-links {
  display: flex;
  gap: var(--spacing-lg);
}

.footer-link {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: rgba(148, 163, 184, 0.08);
  color: rgba(255, 255, 255, 0.8);
  transition: all var(--transition-fast);

  &:hover {
    color: white;
    border-color: rgba(255, 255, 255, 0.2);
    background: rgba(255, 255, 255, 0.14);
    transform: translateY(-1px);
  }
}

.footer-bottom {
  text-align: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

// 响应式设计
@media (max-width: 768px) {
  .home-page {
    background-attachment: scroll;
    background-position: center top;
  }

  .navbar {
    padding: var(--spacing-sm) 0;
  }

  .main-content {
    padding-top: 72px;
  }

  .hero-content {
    grid-template-columns: 1fr;
    text-align: center;
    gap: var(--spacing-lg);
  }

  .features-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .feature-item,
  .stat-item {
    padding: var(--spacing-lg);
  }

  .mobile-menu-button {
    display: inline-flex;
    margin-left: auto;
  }

  .brand-text {
    font-size: var(--font-size-md);
  }

  .nav-actions {
    display: none;
  }

  .hero-title {
    font-size: clamp(1.9rem, 8vw, 2.35rem);
  }

  .hero-subtitle {
    font-size: var(--font-size-md);
  }

  .hero-button {
    height: 46px;
    padding: 0 var(--spacing-lg);
    font-size: var(--font-size-md);
  }

  .card-icon {
    width: 40px;
    height: 40px;
  }

  .feature-icon {
    width: 54px;
    height: 54px;
    border-radius: 14px;
    margin-bottom: var(--spacing-md);

    :deep(svg) {
      width: 30px;
      height: 30px;
    }
  }

  .stat-number {
    font-size: 2.25rem;
  }

  .hero-actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .footer-content {
    flex-direction: column;
    gap: var(--spacing-lg);
  }

  .nav-actions {
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .footer-links {
    justify-content: center;
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }
}

@media (max-width: 480px) {
  .hero-section {
    min-height: auto;
    padding: var(--spacing-xl) 0;
  }

  .brand-logo {
    width: 28px;
    height: 28px;
  }

  .brand-text {
    font-size: var(--font-size-sm);
  }

  .feature-card,
  .feature-item,
  .stat-item {
    border-radius: 18px;
  }

  .drawer-item {
    padding: var(--spacing-sm);

    :deep(.n-icon) {
      font-size: 17px;
    }
  }
}
</style>
