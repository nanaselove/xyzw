<template>
  <div class="home-page">
    <div class="home-bg" />

    <nav class="topbar">
      <div class="topbar-inner container">
        <div class="brand-wrap">
          <div class="brand-badge">
            <img :alt="APP_NAME" src="/icons/xiaoyugan.png" class="brand-logo" />
          </div>
          <div class="brand-meta">
            <div class="brand-title brand-title-single">✨11068 濑帝俱乐部专属✨</div>
          </div>
        </div>

        <div class="topbar-right">
          <div class="desktop-actions">
            <template v-if="!authStore.isAuthenticated">
              <button class="top-btn ghost" @click="router.push('/login')">登录</button>
              <button class="top-btn solid" @click="router.push('/register')">注册</button>
            </template>
            <template v-else>
              <button class="top-btn solid" @click="router.push('/admin/dashboard')">
                进入控制台
              </button>
            </template>
          </div>

          <button class="menu-btn" @click="isMobileMenuOpen = true">
            <n-icon>
              <Menu />
            </n-icon>
          </button>
        </div>
      </div>
    </nav>

    <n-drawer v-model:show="isMobileMenuOpen" placement="right" style="width: 280px">
      <div class="drawer-menu">
        <router-link to="/" class="drawer-item" @click="isMobileMenuOpen = false">
          <n-icon><Home /></n-icon>
          <span>首页</span>
        </router-link>
        <router-link
          to="/admin/dashboard"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon><Grid /></n-icon>
          <span>控制台</span>
        </router-link>
        <router-link
          to="/admin/game-features"
          class="drawer-item"
          @click="isMobileMenuOpen = false"
        >
          <n-icon><Cube /></n-icon>
          <span>游戏功能</span>
        </router-link>
        <router-link to="/tokens" class="drawer-item" @click="isMobileMenuOpen = false">
          <n-icon><PersonCircle /></n-icon>
          <span>Token 管理</span>
        </router-link>
        <router-link to="/changelog" class="drawer-item" @click="isMobileMenuOpen = false">
          <n-icon><Ribbon /></n-icon>
          <span>更新日志</span>
        </router-link>

        <div class="drawer-actions">
          <template v-if="!authStore.isAuthenticated">
            <button
              class="drawer-btn ghost"
              @click="
                router.push('/login');
                isMobileMenuOpen = false;
              "
            >
              登录
            </button>
            <button
              class="drawer-btn solid"
              @click="
                router.push('/register');
                isMobileMenuOpen = false;
              "
            >
              注册
            </button>
          </template>
          <template v-else>
            <button
              class="drawer-btn solid"
              @click="
                router.push('/admin/dashboard');
                isMobileMenuOpen = false;
              "
            >
              进入控制台
            </button>
          </template>
        </div>
      </div>
    </n-drawer>

    <main class="main-content">
      <div class="content-shell">
        <section class="hero-section">
          <div class="glass-card hero-card">
            <div class="hero-glow glow-a" />
            <div class="hero-glow glow-b" />

            <div class="hero-layout">
              <div class="hero-main">
                <div class="hero-status">
                  <span class="status-dot" />
                  控制台已就绪
                </div>

                <h1 class="hero-title">
                  更顺手的
                  <span>游戏管理</span>
                </h1>

                <p class="hero-subtitle">
                  管理角色、自动任务、数据分析，给你的俱乐部一个更轻、更稳、更有质感的控制入口。
                </p>

                <div class="hero-actions">
                  <button class="hero-btn hero-btn-primary" @click="handleStart">
                    进入控制台
                  </button>
                  <button class="hero-btn hero-btn-secondary" @click="handleAddToken">
                    <n-icon>
                      <Add />
                    </n-icon>
                    添加 Token
                  </button>
                </div>
              </div>

              <div class="hero-aside">
                <div class="hero-aside-card">
                  <div class="hero-aside-label">模式</div>
                  <div class="hero-aside-value">Pro Console</div>
                  <div class="hero-aside-sub">Desktop Experience</div>
                </div>
                <div class="hero-aside-card accent">
                  <div class="hero-aside-label">体验关键词</div>
                  <div class="hero-aside-tags">
                    <span v-for="tag in heroKeywords" :key="tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="feature-section">
          <div class="feature-grid">
            <button
              v-for="item in primaryCards"
              :key="item.id"
              class="glass-card feature-card"
              :style="{ '--card-glow': item.glow }"
              @click="handlePrimaryAction(item.action)"
            >
              <span class="corner-line" />
              <div class="feature-top">
                <div class="feature-icon-wrap">
                  <component :is="item.icon" />
                </div>
                <n-icon class="feature-arrow">
                  <ChevronForward />
                </n-icon>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </button>
          </div>
        </section>

        <section class="ws-section">
          <div class="glass-card ws-card">
            <div class="ws-icon-wrap">
              <n-icon>
                <Wifi />
              </n-icon>
            </div>
            <div class="ws-content">
              <div class="ws-head">
                <h3>WebSocket 测试</h3>
                <span class="ws-tag">在线检测</span>
              </div>
              <p>更直观地检查连接状态、回包速度与指令通道是否正常。</p>
              <button class="ws-link" @click="router.push('/websocket-test')">
                进入测试
                <n-icon>
                  <ChevronForward />
                </n-icon>
              </button>
            </div>
          </div>
        </section>

        <footer class="footer">
          <div class="glass-card footer-shell">
            <div class="footer-brand">
              <img :alt="APP_NAME" src="/icons/xiaoyugan.png" class="footer-logo" />
              <div class="footer-brand-text">
                <span>{{ APP_NAME }}</span>
                <small>产品级控制台视觉重构</small>
              </div>
            </div>

            <div class="footer-links">
              <router-link to="/changelog" class="footer-link">更新日志</router-link>
              <a href="#" class="footer-link">关于我们</a>
              <a href="#" class="footer-link">隐私政策</a>
              <a href="#" class="footer-link">服务条款</a>
              <a href="#" class="footer-link">联系我们</a>
            </div>

            <span class="footer-copy">© {{ currentYear }} xiao七濑</span>
          </div>
        </footer>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import {
  Add,
  BarChart,
  ChevronForward,
  Cube,
  Grid,
  Home,
  Menu,
  PersonCircle,
  Ribbon,
  Settings,
  Wifi,
} from "@vicons/ionicons5";
import { APP_NAME } from "@/constants/appMeta";

const router = useRouter();
const authStore = useAuthStore();
const isMobileMenuOpen = ref(false);

const currentYear = computed(() => new Date().getFullYear());
const heroKeywords = ["玻璃质感", "微动效", "控制台", "轻拟态"];

const primaryCards = ref([
  {
    id: 1,
    title: "角色管理",
    desc: "统一查看角色信息、进度与装备状态",
    icon: markRaw(PersonCircle),
    action: "roles",
    glow:
      "radial-gradient(circle at 20% 18%, rgba(168, 85, 247, 0.36), transparent 40%), radial-gradient(circle at 80% 82%, rgba(34, 211, 238, 0.26), transparent 44%)",
  },
  {
    id: 2,
    title: "任务自动化",
    desc: "自动执行重复流程，减少手动操作",
    icon: markRaw(Cube),
    action: "tasks",
    glow:
      "radial-gradient(circle at 25% 20%, rgba(56, 189, 248, 0.34), transparent 42%), radial-gradient(circle at 78% 78%, rgba(129, 140, 248, 0.24), transparent 44%)",
  },
  {
    id: 3,
    title: "数据分析",
    desc: "追踪关键数据变化，快速了解运行状态",
    icon: markRaw(BarChart),
    action: "analysis",
    glow:
      "radial-gradient(circle at 22% 18%, rgba(16, 185, 129, 0.34), transparent 44%), radial-gradient(circle at 78% 80%, rgba(56, 189, 248, 0.24), transparent 44%)",
  },
  {
    id: 4,
    title: "个性设置",
    desc: "按习惯调整面板与功能偏好",
    icon: markRaw(Settings),
    action: "settings",
    glow:
      "radial-gradient(circle at 22% 18%, rgba(236, 72, 153, 0.3), transparent 44%), radial-gradient(circle at 78% 80%, rgba(56, 189, 248, 0.22), transparent 44%)",
  },
]);

const handleStart = () => {
  if (authStore.isAuthenticated) {
    router.push("/admin/dashboard");
    return;
  }
  router.push("/register");
};

const handleAddToken = () => {
  router.push("/tokens");
};

const handlePrimaryAction = (action) => {
  switch (action) {
    case "roles":
    case "tasks":
    case "analysis":
      router.push("/admin/game-features");
      break;
    case "settings":
      router.push("/admin/profile");
      break;
    default:
      router.push("/admin/dashboard");
      break;
  }
};

onMounted(() => {
  authStore.initAuth();
});
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100dvh;
  position: relative;
  color: #f8fbff;
  overflow-x: hidden;
  background: #060a18;
}

.home-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(circle at 20% 18%, rgba(123, 97, 255, 0.24), transparent 30%),
    radial-gradient(circle at 80% 16%, rgba(90, 186, 255, 0.15), transparent 26%),
    radial-gradient(circle at 64% 55%, rgba(255, 120, 214, 0.1), transparent 30%),
    linear-gradient(180deg, rgba(5, 8, 18, 0.64) 0%, rgba(8, 12, 24, 0.58) 48%, rgba(6, 9, 18, 0.68) 100%),
    url("https://t.alcy.cc/moez");
  background-size: auto, auto, auto, auto, cover;
  background-position: center;
}

.home-bg::after {
  content: "";
  position: absolute;
  inset: 0;
  opacity: 0.06;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 28px 28px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(13, 18, 38, 0.52);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.topbar-inner {
  position: relative;
  z-index: 1;
  width: min(460px, calc(100% - 24px));
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.brand-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-badge {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.brand-logo {
  width: 30px;
  height: 30px;
}

.brand-meta {
  min-width: 0;
}

.brand-sub {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: rgba(255, 255, 255, 0.75);
}

.brand-title {
  margin-top: 2px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-title-single {
  margin-top: 0;
  display: block;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: 0.2px;
  color: #f8fbff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.26);
}

.brand-title :deep(.n-icon) {
  color: #fcd34d;
}

.topbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.desktop-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-btn {
  height: 40px;
  border-radius: 999px;
  padding: 0 16px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease;
}

.top-btn:hover {
  transform: translateY(-1px);
}

.top-btn.ghost {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.18);
}

.top-btn.solid {
  color: #0d1e11;
  background: linear-gradient(135deg, #72df8f 0%, #49c76b 100%);
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow: 0 10px 30px rgba(73, 199, 107, 0.35);
}

.menu-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.94);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.22s ease;
}

.menu-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-1px);
}

.menu-btn :deep(.n-icon) {
  font-size: 23px;
}

.drawer-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  min-height: 100%;
  background:
    radial-gradient(circle at 18% 16%, rgba(255, 255, 255, 0.08), transparent 42%),
    linear-gradient(165deg, rgba(12, 16, 32, 0.88), rgba(20, 24, 46, 0.94));
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 10px 12px;
  color: rgba(255, 255, 255, 0.86);
  text-decoration: none;
  border: 1px solid transparent;
}

.drawer-item:hover {
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
}

.drawer-item.router-link-active {
  border-color: rgba(125, 211, 252, 0.35);
  background: rgba(125, 211, 252, 0.16);
  color: #dbeafe;
}

.drawer-item :deep(.n-icon) {
  font-size: 18px;
}

.drawer-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-btn {
  width: 100%;
  height: 40px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.drawer-btn.ghost {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.drawer-btn.solid {
  color: #0d1e11;
  background: linear-gradient(135deg, #72df8f 0%, #49c76b 100%);
  border-color: rgba(255, 255, 255, 0.22);
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 12px 0 30px;
}

.content-shell {
  width: min(460px, calc(100% - 24px));
  margin: 0 auto;
}

.glass-card {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg, rgba(55, 62, 86, 0.3), rgba(35, 40, 62, 0.22));
  box-shadow: 0 8px 24px rgba(8, 10, 30, 0.26);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.glass-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.01));
}

.hero-section {
  margin-top: 8px;
}

.hero-card {
  padding: 22px;
  color: #f8fbff;
}

.hero-layout {
  position: relative;
  z-index: 1;
}

.hero-main {
  min-width: 0;
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(36px);
}

.glow-a {
  width: 164px;
  height: 164px;
  top: -62px;
  right: -52px;
  background: rgba(167, 139, 250, 0.24);
  animation: pulseA 5s ease-in-out infinite;
}

.glow-b {
  width: 142px;
  height: 142px;
  bottom: -64px;
  left: -32px;
  background: rgba(103, 232, 249, 0.2);
  animation: pulseB 5.6s ease-in-out infinite;
}

.hero-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.78);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 12px rgba(74, 222, 128, 0.8);
}

.hero-title {
  margin: 16px 0 0;
  max-width: 12ch;
  font-size: clamp(2.45rem, 10.2vw, 3.9rem);
  line-height: 1.05;
  letter-spacing: -0.4px;
  font-weight: 900;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}

.hero-title span {
  display: block;
  background: linear-gradient(135deg, #fff 0%, #d5d9ff 45%, #9ce9ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-subtitle {
  margin: 14px 0 0;
  max-width: 58ch;
  font-size: 14px;
  line-height: 1.68;
  color: rgba(255, 255, 255, 0.76);
}

.hero-actions {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-aside {
  display: none;
}

.hero-aside-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.03);
  padding: 16px 14px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.2);
}

.hero-aside-card.accent {
  background: linear-gradient(135deg, rgba(88, 124, 255, 0.06), rgba(127, 232, 196, 0.04));
}

.hero-aside-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.56);
}

.hero-aside-value {
  margin-top: 8px;
  font-size: 32px;
  font-weight: 900;
  line-height: 1.06;
  letter-spacing: -0.3px;
}

.hero-aside-sub {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.hero-aside-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-aside-tags span {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  padding: 5px 10px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.72);
}

.hero-btn {
  height: 48px;
  border-radius: 17px;
  border: 1px solid transparent;
  font-size: 16px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.hero-btn:hover {
  transform: translateY(-2px);
}

.hero-btn-primary {
  color: #0b1b0e;
  background: linear-gradient(135deg, #72df8f 0%, #49c76b 100%);
  border-color: rgba(226, 252, 233, 0.3);
  box-shadow: 0 10px 30px rgba(73, 199, 107, 0.35);
}

.hero-btn-secondary {
  color: #fff;
  background: rgba(255, 255, 255, 0.045);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.feature-section {
  margin-top: 16px;
}

.section-head {
  margin-bottom: 12px;
  padding: 0 4px;
}

.section-head h2 {
  margin: 0;
  font-size: clamp(2.25rem, 11vw, 3.4rem);
  font-weight: 900;
  letter-spacing: -0.2px;
  line-height: 1.08;
}

.section-head p {
  margin: 4px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.62);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  position: relative;
  color: #f8fbff;
  text-align: left;
  cursor: pointer;
  padding: 16px 16px 15px;
  min-height: 202px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
}

.feature-card::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: var(--card-glow);
  opacity: 0.42;
}

.feature-card:hover {
  transform: translateY(-4px) scale(1.005);
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow: 0 16px 40px rgba(8, 10, 30, 0.42);
}

.corner-line {
  position: absolute;
  right: 16px;
  top: 14px;
  width: 42px;
  height: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.42);
  border-bottom: 1px solid rgba(156, 233, 255, 0.26);
  opacity: 0.7;
  transform: skewX(-28deg);
  pointer-events: none;
  z-index: 1;
}

.feature-top,
.feature-card h3,
.feature-card p {
  position: relative;
  z-index: 2;
}

.feature-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.feature-icon-wrap {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(135deg, rgba(144, 255, 224, 0.18), rgba(121, 154, 255, 0.14));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.feature-icon-wrap :deep(svg) {
  width: 28px;
  height: 28px;
}

.feature-arrow {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.36);
  transition: all 0.24s ease;
}

.feature-card:hover .feature-arrow {
  transform: translateX(2px);
  color: rgba(255, 255, 255, 0.8);
}

.feature-card h3 {
  margin: auto 0 0;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: -0.1px;
  color: #f8fbff;
}

.feature-card p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.58;
  color: rgba(255, 255, 255, 0.72);
}

.ws-section {
  margin-top: 14px;
}

.ws-card {
  color: #f8fbff;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
}

.ws-icon-wrap {
  position: relative;
  z-index: 1;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(196, 181, 253, 0.24);
  background: linear-gradient(135deg, rgba(124, 108, 255, 0.28), rgba(93, 211, 255, 0.14));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.ws-icon-wrap :deep(.n-icon) {
  font-size: 28px;
}

.ws-content {
  position: relative;
  z-index: 1;
  min-width: 0;
  flex: 1;
}

.ws-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ws-head h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.2px;
}

.ws-tag {
  border-radius: 999px;
  border: 1px solid rgba(110, 231, 183, 0.28);
  background: rgba(110, 231, 183, 0.12);
  color: #bbf7d0;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  white-space: nowrap;
}

.ws-content p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.64;
  color: rgba(255, 255, 255, 0.72);
}

.ws-link {
  margin-top: 12px;
  border: 0;
  background: transparent;
  color: #bae6fd;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 0;
}

.ws-link :deep(.n-icon) {
  font-size: 14px;
}

.footer {
  margin-top: 14px;
  padding-bottom: 24px;
}

.footer-shell {
  color: #f8fbff;
  padding: 14px 16px;
  border-radius: 22px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
}

.footer-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  gap: 2px;
}

.footer-brand-text small {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.48);
}

.footer-logo {
  width: 22px;
  height: 22px;
}

.footer-copy {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.56);
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-link {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
  text-decoration: none;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(0, 0, 0, 0.22);
  padding: 6px 12px;
}

.footer-link:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.22);
}

@media (min-width: 901px) {
  .menu-btn {
    display: none;
  }

  .topbar-inner {
    width: min(1240px, calc(100% - 56px));
    padding: 12px 0;
  }

  .content-shell {
    width: min(1240px, calc(100% - 56px));
  }

  .brand-wrap {
    gap: 12px;
  }

  .brand-badge {
    width: 54px;
    height: 54px;
    border-radius: 16px;
  }

  .brand-logo {
    width: 36px;
    height: 36px;
  }

  .brand-sub {
    font-size: 13px;
  }

  .brand-title {
    font-size: 22px;
  }

  .brand-title-single {
    font-size: clamp(24px, 2.2vw, 40px);
    line-height: 1.02;
    letter-spacing: 0;
  }

  .top-btn {
    height: 42px;
    border-radius: 14px;
    padding: 0 22px;
    font-size: 15px;
  }

  .main-content {
    padding-top: 16px;
    padding-bottom: 40px;
  }

  .hero-card {
    min-height: 300px;
    padding: 32px 34px;
    border-radius: 34px;
  }

  .hero-layout {
    display: flex;
    align-items: flex-end;
    gap: 26px;
  }

  .hero-main {
    flex: 1;
    max-width: 760px;
  }

  .hero-status {
    font-size: 13px;
    padding: 7px 14px;
  }

  .hero-title {
    margin-top: 14px;
    font-size: clamp(3.2rem, 4.8vw, 4.9rem);
    max-width: none;
    white-space: nowrap;
    line-height: 1.03;
    letter-spacing: -0.05em;
  }

  .hero-title span {
    display: inline;
    margin-left: 0.14em;
  }

  .hero-subtitle {
    max-width: none;
    font-size: 18px;
    line-height: 1.72;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: rgba(255, 255, 255, 0.7);
  }

  .hero-actions {
    margin-top: 24px;
    max-width: 640px;
    display: flex;
    gap: 16px;
  }

  .hero-btn {
    height: 60px;
    min-width: 260px;
    padding: 0 24px;
    font-size: 18px;
    border-radius: 16px;
  }

  .hero-aside {
    width: 292px;
    flex-shrink: 0;
    display: grid;
    gap: 12px;
  }

  .hero-aside-card {
    border-radius: 22px;
    padding: 16px;
  }

  .hero-aside-value {
    font-size: 31px;
  }

  .hero-aside-sub {
    font-size: 12px;
  }

  .hero-aside-tags span {
    font-size: 11px;
    padding: 4px 10px;
  }

  .feature-section {
    margin-top: 20px;
  }

  .section-head {
    margin-bottom: 16px;
  }

  .section-head h2 {
    font-size: 56px;
    line-height: 1;
    letter-spacing: -0.06em;
  }

  .section-head p {
    margin-top: 8px;
    font-size: 17px;
  }

  .feature-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .feature-card {
    min-height: 220px;
    padding: 20px 18px;
    border-radius: 26px;
  }

  .feature-icon-wrap {
    width: 56px;
    height: 56px;
  }

  .feature-card h3 {
    font-size: 22px;
  }

  .feature-card p {
    font-size: 14px;
    line-height: 1.66;
  }

  .ws-section {
    margin-top: 18px;
  }

  .ws-card {
    padding: 24px 26px;
    align-items: center;
    border-radius: 30px;
    gap: 18px;
  }

  .ws-head h3 {
    font-size: 30px;
  }

  .ws-content p {
    font-size: 14px;
  }

  .footer {
    margin-top: 18px;
    padding-bottom: 32px;
  }

  .footer-shell {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 14px 18px;
  }

  .footer-brand {
    min-width: 210px;
  }

  .footer-links {
    justify-content: center;
    flex: 1;
  }

  .footer-copy {
    white-space: nowrap;
  }

}

@media (max-width: 900px) {
  .desktop-actions {
    display: none;
  }

  .content-shell {
    width: min(460px, calc(100% - 20px));
  }
}

@media (max-width: 680px) {
  .topbar-inner {
    width: min(460px, calc(100% - 20px));
    padding: 10px 0;
  }

  .brand-badge,
  .menu-btn {
    width: 40px;
    height: 40px;
    border-radius: 12px;
  }

  .brand-logo {
    width: 28px;
    height: 28px;
  }

  .brand-sub {
    font-size: 11px;
  }

  .brand-title {
    font-size: 14px;
  }

  .main-content {
    padding-top: 10px;
  }

  .hero-card {
    padding: 18px;
    border-radius: 24px;
  }

  .hero-btn {
    height: 46px;
    font-size: 15px;
  }

  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-card {
    min-height: 180px;
    border-radius: 22px;
  }

  .ws-icon-wrap {
    width: 50px;
    height: 50px;
    border-radius: 16px;
  }

  .ws-head h3 {
    font-size: 19px;
  }

  .footer-shell {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 420px) {
  .content-shell {
    width: calc(100% - 14px);
  }

  .topbar-inner {
    width: calc(100% - 14px);
  }

  .hero-title {
    font-size: clamp(2.1rem, 9.8vw, 2.7rem);
  }

  .feature-card h3 {
    font-size: 19px;
  }

  .feature-card p {
    font-size: 12.5px;
  }
}

@media (max-width: 360px) {
  .hero-actions {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@keyframes pulseA {
  0%,
  100% {
    opacity: 0.55;
    transform: scale(1);
  }
  50% {
    opacity: 0.95;
    transform: scale(1.08);
  }
}

@keyframes pulseB {
  0%,
  100% {
    opacity: 0.42;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}
</style>
