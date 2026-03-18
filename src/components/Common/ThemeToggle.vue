<template>
  <n-button
    circle
    size="medium"
    class="theme-toggle locked"
    title="主题已固定"
    @click.prevent="noop"
  >
    <template #icon>
      <n-icon v-if="isDark">
        <Sunny />
      </n-icon>
      <n-icon v-else>
        <Moon />
      </n-icon>
    </template>
  </n-button>
</template>

<script setup>
import { Moon, Sunny } from "@vicons/ionicons5";
import { useTheme } from "@/composables/useTheme";

const { isDark } = useTheme();
const noop = () => {};
</script>

<style scoped lang="scss">
.theme-toggle {
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.34);
  backdrop-filter: blur(6px);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px) scale(1.04);
    background: rgba(30, 41, 59, 0.78);
    border-color: rgba(110, 231, 183, 0.86);
    color: #dcfce7;
    box-shadow: 0 8px 18px rgba(2, 6, 23, 0.32);
  }
}

.theme-toggle.locked {
  cursor: default;
}

:global([data-theme="light"]) .theme-toggle {
  color: #0f172a;
  background: rgba(248, 250, 252, 0.92);
  border-color: rgba(15, 23, 42, 0.22);
}

:global([data-theme="light"]) .theme-toggle:hover {
  color: #052e16;
  background: rgba(236, 253, 245, 0.96);
  border-color: rgba(22, 163, 74, 0.72);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.16);
}
</style>
