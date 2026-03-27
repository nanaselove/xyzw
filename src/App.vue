<template>
  <n-config-provider :theme="naiveTheme">
    <n-message-provider>
      <n-loading-bar-provider>
        <n-notification-provider>
          <n-dialog-provider>
            <div id="app">
              <router-view />
            </div>
          </n-dialog-provider>
        </n-notification-provider>
      </n-loading-bar-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from "vue";
import { darkTheme } from "naive-ui";
import { useTheme } from "@/composables/useTheme";

const { isDark, initTheme, setupSystemThemeListener, updateReactiveState } =
  useTheme();

// Naive UI theme
const naiveTheme = computed(() => {
  return isDark.value ? darkTheme : null;
});

// Listen for theme change event
const handleThemeChange = () => {
  // Keep reactive state in sync
  updateReactiveState();
  // Force another sync after DOM updates
  setTimeout(() => {
    updateReactiveState();
  }, 50);
};

onMounted(() => {
  initTheme();
  setupSystemThemeListener();

  // Listen for custom theme event
  window.addEventListener("theme-change", handleThemeChange);

  // Initial sync
  updateReactiveState();
});

onUnmounted(() => {
  window.removeEventListener("theme-change", handleThemeChange);
});
</script>

<style>
/* Theme variables */
:root {
  --app-background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.12), transparent 45%),
    linear-gradient(
      165deg,
      rgba(2, 6, 23, 0.58) 0%,
      rgba(15, 23, 42, 0.66) 56%,
      rgba(3, 7, 18, 0.78) 100%
    ),
    url("https://t.alcy.cc/moez");
  --text-color: #333;
  --text-secondary: #666;
  --text-tertiary: #999;
  --bg-color: #ffffff;
  --border-color: #e0e0e0;
}

/* Dark theme variables */
.dark {
  --app-background:
    radial-gradient(circle at 20% 18%, rgba(148, 163, 184, 0.08), transparent 45%),
    linear-gradient(
      165deg,
      rgba(2, 6, 23, 0.72) 0%,
      rgba(15, 23, 42, 0.78) 56%,
      rgba(3, 7, 18, 0.86) 100%
    ),
    url("https://t.alcy.cc/moez");
  --text-color: #e2e8f0;
  --text-secondary: #cbd5e0;
  --text-tertiary: #94a3b8;
  --bg-color: #1a202c;
  --border-color: #4a5568;
}

html.dark,
html[data-theme="dark"] {
  color-scheme: dark;
}

/* Keep portal layer text readable, avoid forcing all page text to white */
html.dark .n-modal,
html.dark .n-drawer,
html.dark .n-popover,
html.dark .n-dropdown,
html.dark .n-tooltip,
html.dark .n-dialog,
html[data-theme="dark"] .n-modal,
html[data-theme="dark"] .n-drawer,
html[data-theme="dark"] .n-popover,
html[data-theme="dark"] .n-dropdown,
html[data-theme="dark"] .n-tooltip,
html[data-theme="dark"] .n-dialog {
  color: var(--text-primary, #f8fafc);
}

html.dark .n-input__placeholder,
html.dark ::placeholder,
html[data-theme="dark"] .n-input__placeholder,
html[data-theme="dark"] ::placeholder {
  color: rgba(148, 163, 184, 0.85) !important;
}

body.dark .n-modal-container,
body.dark .n-drawer-container,
body.dark .n-popover-container,
body[data-theme="dark"] .n-modal-container,
body[data-theme="dark"] .n-drawer-container,
body[data-theme="dark"] .n-popover-container {
  color: var(--text-primary, #f8fafc) !important;
}

#app {
  min-height: 100vh;
  background: transparent;
  transition:
    background 0.3s ease,
    color 0.3s ease;
  position: relative;
  z-index: 0;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  font-family:
    "SF Pro Display",
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    "PingFang SC",
    "Hiragino Sans GB",
    "Microsoft YaHei",
    "Helvetica Neue",
    Helvetica,
    Arial,
    sans-serif;
  transition: color 0.3s ease;
  background: transparent !important;
  scrollbar-gutter: stable;
}

body {
  position: relative;
  overflow-y: scroll;
}

body::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -2;
  background: var(--app-background);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

body::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    linear-gradient(
      to bottom,
      rgba(6, 10, 24, 0.38) 0%,
      rgba(6, 10, 24, 0.24) 35%,
      rgba(6, 10, 24, 0.36) 100%
    );
}

@media (max-width: 768px) {
  body::before {
    background-attachment: scroll;
    background-position: center top;
  }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>

