<template>
  <div class="token-import-page">
    <div class="page-bg" aria-hidden="true">
      <span class="page-orb orb-a"></span>
      <span class="page-orb orb-b"></span>
      <span class="page-orb orb-c"></span>
      <span class="page-grid"></span>
    </div>

    <div class="container">
      <section class="hero-panel">

        <div class="hero-stats-shell">
          <div class="hero-stats">
            <article class="stat-card stat-card-primary">
              <span class="stat-label">总 Token</span>
              <strong class="stat-value">{{ totalTokenCount }}</strong>
              <span class="stat-hint">当前库中所有账号</span>
            </article>
            <article class="stat-card">
              <span class="stat-label">在线连接</span>
              <strong class="stat-value">{{ connectedTokenCount }}</strong>
              <span class="stat-hint">{{ connectingTokenCount }} 个正在连接</span>
            </article>
            <article class="stat-card">
              <span class="stat-label">长期有效</span>
              <strong class="stat-value">{{ permanentTokenCount }}</strong>
              <span class="stat-hint">{{ temporaryTokenCount }} 个临时存储</span>
            </article>
            <article class="stat-card stat-card-wide">
              <span class="stat-label">当前选中</span>
              <strong class="stat-value stat-value-name">
                {{ tokenStore.selectedToken?.name || "未选择 Token" }}
              </strong>
              <span class="stat-hint">
                {{ tokenStore.selectedToken?.server || "从下方列表选择一个角色进入控制台" }}
              </span>
            </article>
          </div>
        </div>

        <div class="hero-actions">
          <div class="hero-action-primary">
            <n-button type="primary" size="large" @click="showImportForm = true">
              <template #icon>
                <n-icon>
                  <Add />
                </n-icon>
              </template>
              添加Token
            </n-button>
          </div>

          <div class="hero-action-rail" role="toolbar" aria-label="页面操作">
            <n-button size="large" @click="goToDashboard">
              <template #icon>
                <n-icon>
                  <List />
                </n-icon>
              </template>
              批量功能
            </n-button>

            <n-button size="large" @click="checkAppUpdate">
              <template #icon>
                <n-icon>
                  <SyncCircle />
                </n-icon>
              </template>
              检查更新
            </n-button>

            <n-dropdown :options="bulkOptions" @select="handleBulkAction">
              <n-button size="large">
                <template #icon>
                  <n-icon>
                    <Menu />
                  </n-icon>
                </template>
                批量操作
              </n-button>
            </n-dropdown>
          </div>
        </div>
      </section>

      <!-- 限流等待提示 -->
      <n-alert
        v-if="rateLimitWaiting"
        type="warning"
        class="rate-limit-alert"
      >
        {{ rateLimitMessage }}
      </n-alert>

      <!-- Token导入区域 -->
      <a-modal
        class="token-import-modal"
        v-model:visible="showImportForm"
        width="min(94vw, 58rem)"
        :footer="false"
        :default-visible="!tokenStore.hasTokens"
      >
        <template #title>
          <div class="modal-title-block">
            <span class="modal-kicker">TOKEN IMPORT</span>
            <h2>
              <n-icon>
                <Add />
              </n-icon>
              添加游戏Token
            </h2>
            <p class="modal-note">
              先选择导入方式，再填写对应内容。支持手动、URL、微信扫码和 BIN 导入。
            </p>
          </div>
        </template>
        <div class="card-header">
          <div class="import-method-groups">
            <section class="import-method-group">
              <div class="import-method-group-head">
                <span class="import-method-group-kicker">快速导入</span>
                <p>适合手头已经有网页链接、账号信息或扫码入口的场景。</p>
              </div>
              <div
                class="import-method-tabs import-method-tabs--quick"
                role="tablist"
                aria-label="快速导入方式"
              >
                <n-button
                  v-for="method in quickImportMethods"
                  :key="method.value"
                  class="import-method-tab"
                  :class="{ active: importMethod === method.value }"
                  type="default"
                  size="small"
                  role="tab"
                  :aria-selected="importMethod === method.value"
                  @click="importMethod = method.value"
                >
                  {{ method.label }}
                </n-button>
              </div>
            </section>

            <section class="import-method-group import-method-group--advanced">
              <div class="import-method-group-head">
                <span class="import-method-group-kicker">高级导入</span>
                <p>适合从 BIN 文件恢复或做多角色迁移，适合批量处理。</p>
              </div>
              <div
                class="import-method-tabs import-method-tabs--advanced"
                role="tablist"
                aria-label="高级导入方式"
              >
                <n-button
                  v-for="method in advancedImportMethods"
                  :key="method.value"
                  class="import-method-tab"
                  :class="{ active: importMethod === method.value }"
                  type="default"
                  size="small"
                  role="tab"
                  :aria-selected="importMethod === method.value"
                  @click="importMethod = method.value"
                >
                  {{ method.label }}
                </n-button>
              </div>
            </section>
          </div>
        </div>
        <div class="card-body">
          <manual-token-form
            @cancel="() => (showImportForm = false)"
            @ok="() => (showImportForm = false)"
            v-if="importMethod === 'manual'"
          />
          <url-token-form
            @cancel="() => (showImportForm = false)"
            @ok="() => (showImportForm = false)"
            v-if="importMethod === 'url'"
          />
          <wx-qrcode-form
            @cancel="() => (showImportForm = false)"
            @ok="() => (showImportForm = false)"
            v-if="importMethod === 'wxQrcode'"
          />
          <bin-token-form
            @cancel="() => (showImportForm = false)"
            @ok="() => (showImportForm = false)"
            v-if="importMethod === 'bin'"
          />
          <single-bin-token-form
            @cancel="() => (showImportForm = false)"
            @ok="() => (showImportForm = false)"
            v-if="importMethod === 'singlebin'"
          />
        </div>
      </a-modal>

      <!-- Token列表 -->
      <div v-if="tokenStore.hasTokens" class="tokens-section">
        <div class="section-header">
          <div class="section-copy">
            <span class="section-kicker">TOKEN 库</span>
            <h2>我的Token列表 ({{ tokenStore.gameTokens.length }}个)</h2>
            <p>拖拽可重新排序，点击卡片快速进入控制台或刷新连接。</p>
          </div>

          <div class="section-toolbar">
            <div class="view-switch" role="tablist" aria-label="展示方式">
              <n-button
                size="small"
                class="view-switch-option"
                :class="{ active: viewMode === 'list' }"
                @click="viewMode = 'list'"
              >
                列表
              </n-button>
              <n-button
                size="small"
                class="view-switch-option"
                :class="{ active: viewMode === 'card' }"
                @click="viewMode = 'card'"
              >
                卡片
              </n-button>
            </div>

            <n-button-group size="small" class="sort-switch">
              <n-button
                @click="toggleSort('name')"
                :type="sortConfig.field === 'name' ? 'primary' : 'default'"
              >
                名称 {{ getSortIcon("name") }}
              </n-button>
              <n-button
                @click="toggleSort('server')"
                :type="sortConfig.field === 'server' ? 'primary' : 'default'"
              >
                服务器 {{ getSortIcon("server") }}
              </n-button>
              <n-button
                @click="toggleSort('createdAt')"
                :type="sortConfig.field === 'createdAt' ? 'primary' : 'default'"
              >
                创建时间 {{ getSortIcon("createdAt") }}
              </n-button>
              <n-button
                @click="toggleSort('lastUsed')"
                :type="sortConfig.field === 'lastUsed' ? 'primary' : 'default'"
              >
                最后使用 {{ getSortIcon("lastUsed") }}
              </n-button>
            </n-button-group>
          </div>
        </div>

        <div class="tokens-grid" v-if="viewMode === 'card'">
          <a-card
            v-for="(token, index) in sortedTokens"
            :key="token.id"
            draggable="true"
            @dragstart="handleDragStart(index, $event)"
            @dragover="handleDragOver($event)"
            @drop="handleDrop(index, $event)"
            :class="{
              'token-card': true,
              'token-card-shell': true,
              active: selectedTokenId === token.id,
            }"
            @click="selectToken(token)"
          >
            <template #title>
              <a-space class="token-name" align="center">
                <n-avatar
                  v-if="token.avatar"
                  :src="token.avatar"
                  round
                  size="small"
                  fallback-src="/icons/xiaoyugan.png"
                />
                {{ token.name }}
                <a-tag
                  :color="getServerTagColor(token.id)"
                  v-if="token.server"
                  >{{ token.server }}</a-tag
                >
                <!-- 连接状态指示器 -->
                <a-badge
                  :status="getTokenStyle(token.id)"
                  :text="getConnectionStatusText(token.id)"
                />
                <!-- 连接状态文字 -->
                <!-- <a-tag color="green">
                  {{ getConnectionStatusText(token.id) }}
                </a-tag> -->
              </a-space>
            </template>
            <template #extra>
              <n-dropdown
                :options="getTokenActions(token)"
                @select="(key) => handleTokenAction(key, token)"
              >
                <n-button text>
                  <template #icon>
                    <n-icon>
                      <EllipsisHorizontal />
                    </n-icon>
                  </template>
                </n-button>
              </n-dropdown>
            </template>

            <template #default>
              <div class="token-display">
                <span class="token-label">Token:</span>
                <code class="token-value">{{ maskToken(token.token) }}</code>
              </div>

              <!-- 备注信息 -->
              <div
                v-if="editingRemark === token.id"
                class="token-remark token-remark-edit"
                @click.stop
              >
                <span class="remark-label">备注：</span>
                <n-input
                  v-model:value="tempRemarks[token.id]"
                  type="textarea"
                  :rows="2"
                  placeholder="添加备注信息..."
                  @blur="saveRemark(token)"
                  @keyup.enter="saveRemark(token)"
                  @keyup.esc="cancelEditRemark()"
                  autofocus
                />
              </div>
              <div
                v-else
                class="token-remark"
                @click.stop="startEditRemark(token)"
              >
                <span class="remark-label">备注：</span>
                <span class="remark-value">{{
                  token.remark || "点击添加备注"
                }}</span>
                <n-icon style="margin-left: 4px; color: var(--text-tertiary)">
                  <Create />
                </n-icon>
              </div>

              <a-button
                :loading="refreshingTokens.has(token.id)"
                @click.stop="refreshToken(token)"
              >
                <template #icon>
                  <n-icon>
                    <Refresh />
                  </n-icon>
                </template>
                {{ token.sourceUrl ? "刷新" : "重新获取" }}
              </a-button>

              <div class="token-timestamps">
                <div class="timestamp-item">
                  <span class="timestamp-label">创建：</span>
                  <span class="timestamp-value">{{
                    formatTime(token.createdAt)
                  }}</span>
                </div>
                <div class="timestamp-item">
                  <span class="timestamp-label">使用：</span>
                  <span class="timestamp-value">{{
                    formatTime(token.lastUsed)
                  }}</span>
                </div>
              </div>

              <!-- 存储类型信息 -->
              <div class="storage-info">
                <div class="storage-item">
                  <span class="storage-label">存储类型：</span>
                  <n-tag
                    size="small"
                    :type="
                      token.importMethod === 'url' ||
                      token.importMethod === 'bin' ||
                      token.importMethod === 'wxQrcode' ||
                      token.upgradedToPermanent
                        ? 'success'
                        : 'warning'
                    "
                  >
                    {{
                      token.importMethod === "url" ||
                      token.importMethod === "bin" ||
                      token.importMethod === "wxQrcode" ||
                      token.upgradedToPermanent
                        ? "长期有效"
                        : "临时存储"
                    }}
                  </n-tag>
                </div>

                <!-- 升级选项（仅对临时存储的token显示） -->
                <div
                  v-if="
                    !(
                      token.importMethod === 'url' ||
                      token.importMethod === 'bin' ||
                      token.importMethod === 'wxQrcode' ||
                      token.upgradedToPermanent
                    )
                  "
                  class="storage-upgrade"
                >
                  <n-button
                    size="tiny"
                    type="success"
                    ghost
                    @click.stop="upgradeTokenToPermanent(token)"
                  >
                    <template #icon>
                      <n-icon>
                        <Star />
                      </n-icon>
                    </template>
                    升级为长期有效
                  </n-button>
                </div>
              </div>
            </template>
            <template #actions>
              <n-button
                type="primary"
                size="large"
                block
                :loading="connectingTokens.has(token.id)"
                @click="startTaskManagement(token)"
              >
                <template #icon>
                  <n-icon>
                    <Home />
                  </n-icon>
                </template>
                进入控制台
              </n-button>
            </template>
          </a-card>
        </div>

        <!-- List View -->
        <div class="tokens-list" v-else>
          <n-card
            v-for="(token, index) in sortedTokens"
            :key="token.id"
            draggable="true"
            @dragstart="handleDragStart(index, $event)"
            @dragover="handleDragOver($event)"
            @drop="handleDrop(index, $event)"
            size="small"
            style="margin-bottom: 8px"
            hoverable
            @click="selectToken(token)"
            :class="{ active: selectedTokenId === token.id, 'token-row-card': true }"
          >
            <n-space justify="space-between" align="center">
              <!-- Info -->
              <n-space align="center" :size="6">
                <!-- 连接状态 - 移动到最前端显示 -->
                <div style="min-width: 65px">
                  <a-badge
                    :status="getTokenStyle(token.id)"
                    :text="getConnectionStatusText(token.id)"
                  />
                </div>
                <!-- Avatar -->
                <n-avatar
                  v-if="token.avatar"
                  :src="token.avatar"
                  round
                  size="small"
                  fallback-src="/icons/xiaoyugan.png"
                />

                <!-- Token基本信息 -->
                <div style="min-width: 100px">
                  <div
                    style="
                      display: flex;
                      align-items: center;
                      flex-wrap: wrap;
                      gap: 2px;
                    "
                  >
                    <span style="font-weight: bold; font-size: 0.95em">{{
                      token.name
                    }}</span>
                    <n-tag
                      size="small"
                      :type="getServerTagType(token.id)"
                      v-if="token.server"
                      >{{ token.server }}</n-tag
                    >
                    <!-- 备注信息 - 显示在服务器信息后面 -->
                    <div
                      v-if="editingRemark === token.id"
                      style="
                        font-size: 0.75em;
                        display: flex;
                        align-items: center;
                        gap: 4px;
                      "
                      @click.stop
                    >
                      <i
                        class="i-mdi:note-outline"
                        style="margin-right: 1px"
                      ></i>
                      <n-input
                        v-model:value="tempRemarks[token.id]"
                        size="small"
                        placeholder="添加备注..."
                        @blur="saveRemark(token)"
                        @keyup.enter="saveRemark(token)"
                        @keyup.esc="cancelEditRemark()"
                        autofocus
                        style="width: 150px"
                      />
                    </div>
                    <div
                      v-else
                      style="
                        font-size: 0.75em;
                        color: var(--text-secondary);
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        gap: 4px;
                      "
                      @click.stop="startEditRemark(token)"
                    >
                      <i
                        class="i-mdi:note-outline"
                        style="margin-right: 1px"
                      ></i>
                      {{ token.remark || "点击添加备注" }}
                      <n-icon
                        style="font-size: 0.8em; color: var(--text-tertiary)"
                      >
                        <Create />
                      </n-icon>
                    </div>
                  </div>
                </div>
              </n-space>

              <!-- Actions -->
              <n-space>
                <!-- 存储类型 -->
                <n-tag
                  size="small"
                  :type="
                    token.importMethod === 'url' ||
                    token.importMethod === 'bin' ||
                    token.importMethod === 'wxQrcode' ||
                    token.upgradedToPermanent
                      ? 'success'
                      : 'warning'
                  "
                >
                  {{
                    token.importMethod === "url" ||
                    token.importMethod === "bin" ||
                    token.importMethod === "wxQrcode" ||
                    token.upgradedToPermanent
                      ? "长期"
                      : "临时"
                  }}
                </n-tag>

                <!-- 升级选项（仅对临时存储的token显示） -->
                <n-button
                  v-if="
                    !(
                      token.importMethod === 'url' ||
                      token.importMethod === 'bin' ||
                      token.importMethod === 'wxQrcode' ||
                      token.upgradedToPermanent
                    )
                  "
                  size="small"
                  type="success"
                  ghost
                  @click.stop="upgradeTokenToPermanent(token)"
                >
                  <template #icon>
                    <n-icon>
                      <Star />
                    </n-icon>
                  </template>
                  升级
                </n-button>

                <n-button
                  size="small"
                  type="primary"
                  :loading="connectingTokens.has(token.id)"
                  @click.stop="startTaskManagement(token)"
                >
                  <template #icon>
                    <n-icon>
                      <Home />
                    </n-icon>
                  </template>
                  控制台
                </n-button>
                <n-button
                  size="small"
                  @click.stop="refreshToken(token)"
                  :loading="refreshingTokens.has(token.id)"
                >
                  <template #icon>
                    <n-icon>
                      <Refresh />
                    </n-icon>
                  </template>
                  刷新
                </n-button>
                <n-dropdown
                  :options="getTokenActions(token)"
                  @select="(key) => handleTokenAction(key, token)"
                >
                  <n-button size="small" circle @click.stop>
                    <template #icon>
                      <n-icon>
                        <EllipsisHorizontal />
                      </n-icon>
                    </template>
                  </n-button>
                </n-dropdown>
              </n-space>
            </n-space>
          </n-card>
        </div>
      </div>

      <!-- 空状态 -->
      <section v-if="!tokenStore.hasTokens && !showImportForm" class="empty-panel">
        <div class="empty-visual">
          <img src="/icons/xiaoyugan.png" alt="XYZW" />
        </div>
        <h2>还没有导入任何 Token</h2>
        <p>先添加一个账号，或从 URL、BIN、微信扫码导入，再进入控制台开始管理。</p>
        <div class="empty-actions">
          <n-button type="primary" @click="showImportForm = true">
            添加 Token
          </n-button>
          <n-button ghost type="info" @click="checkAppUpdate">
            检查更新
          </n-button>
        </div>
      </section>
    </div>

    <!-- 编辑Token模态框 -->
    <n-modal
      v-model:show="showEditModal"
      preset="card"
      title="编辑Token"
      style="width: 500px"
    >
      <n-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-placement="left"
        label-width="80px"
      >
        <n-form-item label="名称" path="name">
          <n-input v-model:value="editForm.name" />
        </n-form-item>
        <n-form-item label="Token字符串" path="token">
          <n-input
            v-model:value="editForm.token"
            type="textarea"
            :rows="3"
            placeholder="粘贴Token字符串..."
            clearable
          />
        </n-form-item>
        <n-form-item label="服务器">
          <n-input v-model:value="editForm.server" />
        </n-form-item>
        <n-form-item label="WebSocket地址">
          <n-input v-model:value="editForm.wsUrl" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input
            v-model:value="editForm.remark"
            type="textarea"
            :rows="2"
            placeholder="添加备注信息..."
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-actions">
          <n-button @click="showEditModal = false"> 取消 </n-button>
          <n-button type="primary" @click="saveEdit"> 保存 </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import ManualTokenForm from "./manual.vue";
import UrlTokenForm from "./url.vue";
import BinTokenForm from "./bin.vue";
import singleBinTokenForm from "./singlebin.vue";
import WxQrcodeForm from "./wxqrcode.vue";

import { useTokenStore, selectedTokenId } from "@/stores/tokenStore";
import {
  Add,
  Copy,
  Create,
  EllipsisHorizontal,
  Grid,
  List,
  Home,
  Key,
  Menu,
  Refresh,
  Star,
  SyncCircle,
  TrashBin,
} from "@vicons/ionicons5";
import { NIcon, NAlert, useDialog, useMessage } from "naive-ui";
import { computed, h, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { transformToken, scheduleAuthUserRequest } from "@/utils/token";
import { $emit } from "@/stores/events/index.ts";
import useIndexedDB from "@/hooks/useIndexedDB";
import { isAndroidWebView } from "@/utils/env";
const { getArrayBuffer, storeArrayBuffer, deleteArrayBuffer, clearAll } =
  useIndexedDB();
// 接收路由参数
const props = defineProps({
  token: String,
  name: String,
  server: String,
  wsUrl: String,
  api: String,
  auto: Boolean,
});

const router = useRouter();
const message = useMessage();
const dialog = useDialog();
const tokenStore = useTokenStore();

// 限流等待状态
const rateLimitWaiting = ref(false);
const rateLimitMessage = ref("");

// 响应式数据
const showImportForm = ref(false);
const isImporting = ref(false);
const showEditModal = ref(false);
const importFormRef = ref(null);
const urlFormRef = ref(null);
const editFormRef = ref(null);
const editingToken = ref(null);
const importMethod = ref("manual");
const quickImportMethods = [
  { value: "manual", label: "手动输入" },
  { value: "url", label: "URL获取" },
  { value: "wxQrcode", label: "微信扫码" },
];
const advancedImportMethods = [
  { value: "bin", label: "BIN多角色" },
  { value: "singlebin", label: "BIN单角色" },
];
const refreshingTokens = ref(new Set());
const connectingTokens = ref(new Set());
// 从localStorage读取上次的视图模式，默认为列表视图
const viewMode = ref(localStorage.getItem("tokenViewMode") || "list");
const dragIndex = ref(null);

// 备注编辑状态管理
const editingRemark = ref(null); // 当前正在编辑备注的tokenId
const tempRemarks = ref({}); // 临时保存编辑中的备注内容

// 监听视图模式变化，保存到localStorage
watch(viewMode, (newViewMode) => {
  localStorage.setItem("tokenViewMode", newViewMode);
});

// 排序状态管理 - 从localStorage读取上次的排序设置
const savedSortConfig = localStorage.getItem("tokenSortConfig");
const sortConfig = ref(
  savedSortConfig
    ? JSON.parse(savedSortConfig)
    : {
        field: "createdAt", // 排序字段：name, server, createdAt, lastUsed
        direction: "asc", // 排序方向：asc, desc
      },
);

// 排序后的游戏角色Token列表
const sortedTokens = computed(() => {
  if (sortConfig.value.field === "manual") {
    return tokenStore.gameTokens;
  }

  return [...tokenStore.gameTokens].sort((tokenA, tokenB) => {
    let valueA, valueB;

    // 根据排序字段获取比较值
    switch (sortConfig.value.field) {
      case "name":
        valueA = tokenA.name?.toLowerCase() || "";
        valueB = tokenB.name?.toLowerCase() || "";
        break;
      case "server":
        valueA = tokenA.server?.toLowerCase() || "";
        valueB = tokenB.server?.toLowerCase() || "";
        break;
      case "createdAt":
        valueA = new Date(tokenA.createdAt || 0).getTime();
        valueB = new Date(tokenB.createdAt || 0).getTime();
        break;
      case "lastUsed":
        valueA = new Date(tokenA.lastUsed || 0).getTime();
        valueB = new Date(tokenB.lastUsed || 0).getTime();
        break;
      default:
        valueA = tokenA.name?.toLowerCase() || "";
        valueB = tokenB.name?.toLowerCase() || "";
    }

    // 根据排序方向比较值
    if (valueA < valueB) {
      return sortConfig.value.direction === "asc" ? -1 : 1;
    }
    if (valueA > valueB) {
      return sortConfig.value.direction === "asc" ? 1 : -1;
    }
    return 0;
  });
});

// 切换排序
const toggleSort = (field) => {
  if (sortConfig.value.field === field) {
    // 如果点击的是当前排序字段，则切换排序方向
    sortConfig.value.direction =
      sortConfig.value.direction === "asc" ? "desc" : "asc";
  } else {
    // 如果点击的是新的排序字段，则默认升序
    sortConfig.value.field = field;
    sortConfig.value.direction = "asc";
  }

  // 保存排序设置到localStorage
  localStorage.setItem("tokenSortConfig", JSON.stringify(sortConfig.value));
};

// 获取排序图标
const getSortIcon = (field) => {
  if (sortConfig.value.field !== field) return null;
  return sortConfig.value.direction === "asc" ? "↑" : "↓";
};

const handleDragStart = (index, event) => {
  dragIndex.value = index;
  event.dataTransfer.effectAllowed = "move";
  // 可以在这里设置拖拽时的预览图等
};

const handleDragOver = (event) => {
  event.preventDefault(); // 允许放置
  event.dataTransfer.dropEffect = "move";
};

const handleDrop = (index, event) => {
  event.preventDefault();
  if (dragIndex.value === null || dragIndex.value === index) return;

  // 使用当前显示的列表（sortedTokens）来进行重新排序
  // 这样可以确保用户看到的顺序就是最终保存的顺序
  const currentTokens = [...sortedTokens.value];
  const draggedItem = currentTokens[dragIndex.value];

  // 移动元素
  currentTokens.splice(dragIndex.value, 1);
  currentTokens.splice(index, 0, draggedItem);

  // 更新 store
  tokenStore.gameTokens = currentTokens;

  // 切换到手动排序模式，防止自动排序打乱顺序
  sortConfig.value.field = "manual";
  // 保存排序设置
  localStorage.setItem("tokenSortConfig", JSON.stringify(sortConfig.value));

  dragIndex.value = null;
  message.success("Token 顺序已更新");
};

// 编辑表单
const editForm = reactive({
  name: "",
  token: "",
  server: "",
  wsUrl: "",
  remark: "",
});

const editRules = {
  name: [{ required: true, message: "请输入Token名称", trigger: "blur" }],
  token: [{ required: true, message: "请输入Token字符串", trigger: "blur" }],
};

const bulkOptions = [
  { label: "刷新所有Token", key: "refreshAll" },
  { label: "更新token信息", key: "updateInfo" },
  { label: "导出所有Token", key: "export" },
  { label: "导入Token文件", key: "import" },
  { label: "清理过期Token", key: "clean" },
  { label: "断开所有连接", key: "disconnect" },
  { label: "清除所有Token", key: "clear" },
];

/**
 * 手动打开Token管理卡片
 */
const openshowImportForm = () => {
  showImportForm.value = true;
};

// 刷新Token
const refreshToken = async (token) => {
  refreshingTokens.value.add(token.id);

  try {
    if (token.importMethod === "url") {
      // 有源URL的token - 从URL重新获取（使用限流）
      const data = await scheduleAuthUserRequest(async () => {
        let response;

        const isLocalUrl =
          token.sourceUrl.startsWith(window.location.origin) ||
          token.sourceUrl.startsWith("/") ||
          token.sourceUrl.startsWith("http://localhost") ||
          token.sourceUrl.startsWith("http://127.0.0.1");

        if (isLocalUrl) {
          response = await fetch(token.sourceUrl);
        } else {
          try {
            response = await fetch(token.sourceUrl, {
              method: "GET",
              headers: {
                Accept: "application/json",
              },
              mode: "cors",
            });
          } catch (corsError) {
            throw new Error(
              `跨域请求被阻止。请确保目标服务器支持CORS。错误详情: ${corsError.message}`,
            );
          }
        }

        if (!response.ok) {
          throw new Error(
            `请求失败: ${response.status} ${response.statusText}`,
          );
        }

        const result = await response.json();

        if (!result.token) {
          throw new Error("返回数据中未找到token字段");
        }

        return result;
      });

      // 更新token信息
      tokenStore.updateToken(token.id, {
        token: data.token,
        server: data.server || token.server,
        lastRefreshed: Date.now(),
      });

      message.success("Token刷新成功");
    } else if (
      token.importMethod === "wxQrcode" ||
      token.importMethod === "bin"
    ) {
      let userToken = await getArrayBuffer(token.id);
      let usedOldKey = false;
      if (!userToken) {
        userToken = await getArrayBuffer(token.name);
        usedOldKey = true;
      }
      if (userToken) {
        const newToken = await transformToken(userToken);
        tokenStore.updateToken(token.id, {
          token: newToken,
          lastRefreshed: Date.now(),
        });
        if (usedOldKey) {
          await storeArrayBuffer(token.id, userToken);
          await deleteArrayBuffer(token.name);
          console.log("已迁移IndexedDB数据:", token.name, "->", token.id);
        }
        message.success("Token刷新成功");
      }
    } else {
      dialog.info({
        title: "重新获取Token",
        content: `Token "${token.name}" 是通过微信扫码登录导入的，没有配置自动刷新地址。

请选择以下操作：
1. 重新手动导入新的Token
2. 尝试重新连接现有Token`,
        positiveText: "重新导入",
        negativeText: "重新连接",
        onPositiveClick: () => {
          showImportForm.value = true;
          importMethod.value = "manual";
          importForm.name = token.name;
          importForm.server = token.server;
          importForm.wsUrl = token.wsUrl;
        },
        onNegativeClick: () => {
          // 断开现有连接
          if (tokenStore.getWebSocketStatus(token.id) === "connected") {
            tokenStore.closeWebSocketConnection(token.id);
          }

          // 尝试重新连接
          setTimeout(() => {
            tokenStore.createWebSocketConnection(
              token.id,
              token.token,
              token.wsUrl,
            );
            message.info("正在尝试重新连接...");
          }, 500);
        },
      });
      return;
    }

    // 如果当前token有连接，需要重新连接
    if (tokenStore.getWebSocketStatus(token.id) === "connected") {
      tokenStore.closeWebSocketConnection(token.id);
      setTimeout(() => {
        tokenStore.createWebSocketConnection(
          token.id,
          token.token,
          token.wsUrl,
        );
      }, 500);
    }
  } catch (error) {
    console.error("刷新Token失败:", error);
    message.error(error.message || "Token刷新失败");
  } finally {
    refreshingTokens.value.delete(token.id);
    // 关闭限流等待提示
    rateLimitWaiting.value = false;
  }
};

// 升级Token为长期有效
const upgradeTokenToPermanent = (token) => {
  dialog.warning({
    title: "升级为长期有效",
    content: `确认要将Token "${token.name}" 升级为长期有效吗？升级后该Token将不会因24小时未使用而被自动清理。`,
    positiveText: "确认升级",
    negativeText: "取消",
    onPositiveClick: () => {
      const success = tokenStore.upgradeTokenToPermanent(token.id);
      if (success) {
        message.success(`Token "${token.name}" 已升级为长期有效！`);
      } else {
        message.error("升级失败，该Token可能已经是长期有效状态");
      }
    },
  });
};

const selectToken = (token, forceReconnect = false) => {
  // 如果有备注正在编辑，保存备注并取消编辑
  if (editingRemark.value) {
    saveCurrentRemark();
    return;
  }

  const isAlreadySelected = selectedTokenId.value === token.id;
  const connectionStatus = getConnectionStatus(token.id);

  // 降噪日志已移除

  // 如果已经选中且已连接，断开连接
  if (
    isAlreadySelected &&
    connectionStatus === "connected" &&
    !forceReconnect
  ) {
    // 断开连接
    tokenStore.closeWebSocketConnection(token.id);
    message.success(`已断开 ${token.name} 的连接`);
    return;
  }

  // 如果未选中但已连接，断开连接
  if (
    !isAlreadySelected &&
    connectionStatus === "connected" &&
    !forceReconnect
  ) {
    // 断开连接
    tokenStore.closeWebSocketConnection(token.id);
    message.success(`已断开 ${token.name} 的连接`);
    return;
  }

  // 如果已经选中但正在连接，也不执行操作
  if (
    isAlreadySelected &&
    connectionStatus === "connecting" &&
    !forceReconnect
  ) {
    message.info(`${token.name} 正在连接中...`);
    return;
  }

  // 选择token（带智能连接判断）
  const result = tokenStore.selectToken(token.id, forceReconnect);

  if (result) {
    if (forceReconnect) {
      message.success(`强制重连：${token.name}`);
    } else if (isAlreadySelected) {
      message.success(`重新连接：${token.name}`);
    } else {
      message.success(`已选择：${token.name}`);
    }
  } else {
    message.error(`选择Token失败：${token.name}`);
  }
};

const getConnectionStatus = (tokenId) => {
  return tokenStore.getWebSocketStatus(tokenId);
};

const getConnectionStatusText = (tokenId) => {
  const status = getConnectionStatus(tokenId);
  const statusMap = {
    connected: "已连接",
    connecting: "连接中...",
    disconnected: "已断开",
    error: "连接错误",
    disconnecting: "断开中...",
  };
  return statusMap[status] || "未连接";
};

const getTokenStyle = (tokenId) => {
  const status = getConnectionStatus(tokenId);
  const statusMap = {
    connected: "success",
    connecting: "warning",
    disconnected: "danger",
    error: "danger",
    disconnecting: "warning",
  };
  return statusMap[status] || "danger";
};

const getServerTagType = (tokenId) => {
  const status = getConnectionStatus(tokenId);
  // 连接成功时服务器标签使用绿色，其他状态保持红色
  return status === "connected" ? "success" : "error";
};

const getServerTagColor = (tokenId) => {
  const status = getConnectionStatus(tokenId);
  // 连接成功时服务器标签使用绿色，其他状态保持红色
  return status === "connected" ? "green" : "red";
};

const isPersistentToken = (token) =>
  token.importMethod === "url" ||
  token.importMethod === "bin" ||
  token.importMethod === "wxQrcode" ||
  token.upgradedToPermanent;

const totalTokenCount = computed(() => tokenStore.gameTokens.length);

const connectedTokenCount = computed(() =>
  tokenStore.gameTokens.filter(
    (token) => getConnectionStatus(token.id) === "connected",
  ).length,
);

const connectingTokenCount = computed(() =>
  tokenStore.gameTokens.filter(
    (token) => getConnectionStatus(token.id) === "connecting",
  ).length,
);

const permanentTokenCount = computed(() =>
  tokenStore.gameTokens.filter((token) => isPersistentToken(token)).length,
);

const temporaryTokenCount = computed(() =>
  Math.max(totalTokenCount.value - permanentTokenCount.value, 0),
);

const getTokenActions = (token) => {
  const actions = [
    {
      label: "编辑",
      key: "edit",
      icon: () => h(NIcon, null, { default: () => h(Create) }),
    },
    {
      label: "复制Token",
      key: "copy",
      icon: () => h(NIcon, null, { default: () => h(Copy) }),
    },
  ];

  // 根据Token类型添加刷新选项
  if (token.importMethod === "url" && token.sourceUrl) {
    actions.push({
      label: "从URL刷新",
      key: "refresh-url",
      icon: () => h(NIcon, null, { default: () => h(SyncCircle) }),
    });
  } else {
    actions.push({
      label: "重新获取",
      key: "refresh",
      icon: () => h(NIcon, null, { default: () => h(Refresh) }),
    });
  }

  actions.push(
    { type: "divider" },
    {
      label: "删除",
      key: "delete",
      icon: () => h(NIcon, null, { default: () => h(TrashBin) }),
      props: { style: { color: "#e74c3c" } },
    },
  );

  return actions;
};

const handleTokenAction = async (key, token) => {
  switch (key) {
    case "edit":
      editToken(token);
      break;
    case "copy":
      copyToken(token);
      break;
    case "refresh":
      // 重新获取Token
      refreshToken(token);
      break;
    case "refresh-url":
      // URL获取的Token刷新
      refreshToken(token);
      break;
    case "delete":
      deleteToken(token);
      break;
  }
};

const editToken = (token) => {
  editingToken.value = token;
  Object.assign(editForm, {
    name: token.name,
    token: token.token,
    server: token.server || "",
    wsUrl: token.wsUrl || "",
    remark: token.remark || "",
  });
  showEditModal.value = true;
};

const saveEdit = async () => {
  if (!editFormRef.value || !editingToken.value) return;

  try {
    await editFormRef.value.validate();

    tokenStore.updateToken(editingToken.value.id, {
      name: editForm.name,
      token: editForm.token,
      server: editForm.server,
      wsUrl: editForm.wsUrl,
      remark: editForm.remark,
    });

    message.success("Token信息已更新");
    showEditModal.value = false;
    editingToken.value = null;
  } catch (error) {
    // 验证失败
  }
};

const copyToken = async (token) => {
  try {
    await navigator.clipboard.writeText(token.token);
    message.success("Token已复制到剪贴板");
  } catch (error) {
    message.error("复制失败");
  }
};

// 快速编辑备注功能
const startEditRemark = (token) => {
  editingRemark.value = token.id;
  tempRemarks.value[token.id] = token.remark || "";
};

// 保存备注的通用函数
const saveCurrentRemark = () => {
  if (!editingRemark.value) return;

  const editingTokenId = editingRemark.value;
  const remark = tempRemarks.value[editingTokenId] || "";
  tokenStore.updateToken(editingTokenId, {
    remark: remark,
  });
  editingRemark.value = null;
  message.success("备注已保存");
};

const saveRemark = (token) => {
  saveCurrentRemark();
};

const cancelEditRemark = () => {
  editingRemark.value = null;
};

const deleteToken = (token) => {
  dialog.warning({
    title: "删除Token",
    content: `确定要删除Token "${token.name}" 吗？此操作无法恢复。`,
    positiveText: "确定删除",
    negativeText: "取消",
    onPositiveClick: async () => {
      await tokenStore.removeToken(token.id);
      message.success("Token已删除");
    },
  });
};

// 批量刷新所有URLToken
const refreshAllTokens = async () => {
  if (!tokenStore.gameTokens.length) {
    message.warning("没有可刷新的Token");
    return;
  }

  const tokensToRefresh = tokenStore.gameTokens.filter(
    (token) =>
      token.importMethod === "url" ||
      token.importMethod === "wxQrcode" ||
      token.importMethod === "bin",
  );
  const manualTokens = tokenStore.gameTokens.filter(
    (token) => token.importMethod === "manual",
  );

  if (tokensToRefresh.length === 0) {
    message.warning("没有支持自动刷新的Token");
    return;
  }

  // 显示确认对话框
  dialog.warning({
    title: "批量刷新Token",
    content: "确定要刷新所有支持自动刷新的Token吗?",
    positiveText: "开始刷新",
    negativeText: "取消",
    onPositiveClick: async () => {
      try {
        let successCount = 0;
        let failCount = 0;

        // 显示进度提示
        const loadingMessage = message.loading(
          `正在批量刷新Token (0/${tokensToRefresh.length})`,
          {
            duration: 0,
          },
        );

        for (let i = 0; i < tokensToRefresh.length; i++) {
          const token = tokensToRefresh[i];

          try {
            // 更新进度显示
            loadingMessage.content = `正在刷新Token (${i + 1}/${tokensToRefresh.length}): ${token.name}`;

            // 调用单个刷新函数（限流器会自动处理等待）
            await refreshToken(token);
            successCount++;
          } catch (error) {
            console.error(`刷新Token "${token.name}" 失败:`, error);
            failCount++;
          }
        }

        // 关闭进度提示
        loadingMessage.destroy();

        // 关闭限流等待提示
        rateLimitWaiting.value = false;

        // 显示结果
        if (failCount === 0) {
          message.success(`批量刷新完成！成功刷新 ${successCount} 个Token`);
        } else {
          message.warning(
            `批量刷新完成，成功 ${successCount} 个，失败 ${failCount} 个`,
          );
        }

        // 如果有手动导入的Token，提示用户
        if (manualTokens.length > 0) {
          message.info(`${manualTokens.length} 个手动导入的Token需要手动刷新`);
        }
      } catch (error) {
        message.error("批量刷新过程中发生错误: " + error.message);
      }
    },
  });
};

const handleBulkAction = (key) => {
  switch (key) {
    case "refreshAll":
      refreshAllTokens();
      break;
    case "updateInfo":
      updateAllTokenInfo();
      break;
    case "export":
      exportTokens();
      break;
    case "import":
      importTokenFile();
      break;
    case "clean":
      cleanExpiredTokens();
      break;
    case "disconnect":
      disconnectAll();
      break;
    case "clear":
      clearAllTokens();
      break;
  }
};

const exportTokens = () => {
  try {
    const data = tokenStore.exportTokens();
    const dataStr = JSON.stringify(data, null, 2);
    const dataBlob = new Blob([dataStr], { type: "application/json" });

    const link = document.createElement("a");
    link.href = URL.createObjectURL(dataBlob);
    link.download = `tokens_backup_${new Date().toISOString().split("T")[0]}.json`;
    link.click();

    message.success("Token数据已导出");
  } catch (error) {
    message.error("导出失败");
  }
};

const importTokenFile = () => {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".json";
  input.onchange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = JSON.parse(e.target.result);
          const result = tokenStore.importTokens(data);
          if (result.success) {
            message.success(result.message);
          } else {
            message.error(result.message);
          }
        } catch (error) {
          message.error("文件格式错误");
        }
      };
      reader.readAsText(file);
    }
  };
  input.click();
};

const cleanExpiredTokens = async () => {
  const count = await tokenStore.cleanExpiredTokens();
  message.success(`已清理 ${count} 个过期Token`);
};

const disconnectAll = () => {
  tokenStore.gameTokens.forEach((token) => {
    tokenStore.closeWebSocketConnection(token.id);
  });
  message.success("所有连接已断开");
};

const clearAllTokens = () => {
  dialog.error({
    title: "清除所有Token",
    content: "确定要清除所有Token吗？此操作无法恢复！",
    positiveText: "确定清除",
    negativeText: "取消",
    onPositiveClick: async () => {
      await tokenStore.clearAllTokens();
      message.success("所有Token已清除");
    },
  });
};

// 一键连接更新所有token信息
const updateAllTokenInfo = async () => {
  if (tokenStore.gameTokens.length === 0) {
    message.warning("没有可更新的Token");
    return;
  }

  dialog.warning({
    title: "更新所有Token信息",
    content:
      "此操作将逐个连接所有Token，获取最新的角色名称和服务器信息，完成后自动断开连接。\n\n预计耗时：约3-5秒/个Token",
    positiveText: "开始更新",
    negativeText: "取消",
    onPositiveClick: async () => {
      try {
        let successCount = 0;
        let failCount = 0;
        const totalTokens = tokenStore.gameTokens.length;

        // 显示进度提示
        const loadingMessage = message.loading(
          `正在更新Token信息 (0/${totalTokens})`,
          {
            duration: 0,
          },
        );

        // 顺序处理每个token
        for (let i = 0; i < tokenStore.gameTokens.length; i++) {
          const token = tokenStore.gameTokens[i];

          // 更新进度显示
          loadingMessage.content = `正在更新Token信息 (${i + 1}/${totalTokens}): ${token.name}`;

          try {
            // 连接token获取角色信息
            await tokenStore.selectToken(token.id);

            // 等待1秒确保角色信息已获取（可根据实际情况调整）
            await new Promise((resolve) => setTimeout(resolve, 1000));

            // 断开连接
            tokenStore.closeWebSocketConnection(token.id);

            successCount++;
            message.success(`Token "${token.name}" 信息更新成功`);
          } catch (error) {
            console.error(`更新Token "${token.name}" 失败:`, error);
            failCount++;
            message.error(`Token "${token.name}" 信息更新失败`);
          }

          // 添加短暂延迟，避免服务器压力过大
          if (i < tokenStore.gameTokens.length - 1) {
            await new Promise((resolve) => setTimeout(resolve, 500));
          }
        }

        // 关闭进度提示
        loadingMessage.destroy();

        // 显示结果
        if (failCount === 0) {
          message.success(
            `所有Token信息更新完成！成功更新 ${successCount} 个Token`,
          );
        } else {
          message.warning(
            `Token信息更新完成，成功 ${successCount} 个，失败 ${failCount} 个`,
          );
        }
      } catch (error) {
        message.error("更新过程中发生错误: " + error.message);
      }
    },
  });
};

const maskToken = (token) => {
  if (!token) return "";
  const len = token.length;
  if (len <= 8) return token;
  return token.substring(0, 4) + "***" + token.substring(len - 4);
};

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString("zh-CN");
};

const goToDashboard = () => {
  router.push("/admin/batch-daily-tasks");
};

const checkAppUpdate = () => {
  if (window.AndroidBridge?.checkApkUpdate) {
    window.AndroidBridge.checkApkUpdate();
    return;
  }

  message.info("请在 Android App 中使用该功能");
};

// 开始任务管理 - 直接跳转到控制台
const startTaskManagement = async (token) => {
  // 选择token
  tokenStore.selectToken(token.id);
  message.success(`正在进入 ${token.name} 的控制台`);

  try {
    await router.replace({ name: "Dashboard" });
  } catch (error) {
    console.error("进入控制台失败:", error);
  }

  // 某些 Android WebView 环境下路由切换可能会被拦截，做一次 hash 兜底。
  if (
    isAndroidWebView() &&
    router.currentRoute.value.name !== "Dashboard" &&
    window.location.hash !== "#/admin/dashboard"
  ) {
    window.location.hash = "#/admin/dashboard";
  }
};

// URL参数处理函数
const handleUrlParams = async () => {
  // 检查是否通过URL传递了token参数
  if (props.token || props.api) {
    try {
      isImporting.value = true;
      let tokenResult = null;

      if (props.api) {
        // 通过API获取token
        // 降噪
        message.info("正在从API获取token...");

        const response = await fetch(props.api, {
          method: "GET",
          headers: {
            Accept: "application/json",
          },
          mode: "cors",
        });

        if (!response.ok) {
          throw new Error(
            `API请求失败: ${response.status} ${response.statusText}`,
          );
        }

        const data = await response.json();

        if (!data.token) {
          throw new Error("API返回数据中未找到token字段");
        }

        // 使用API获取的token
        tokenResult = tokenStore.importBase64Token(
          props.name || data.name || "通过API导入的Token",
          data.token,
          {
            server: props.server || data.server,
            wsUrl: props.wsUrl,
            sourceUrl: props.api,
            importMethod: "url",
          },
        );
      } else if (props.token) {
        // 直接使用URL中的token
        // 降噪
        message.info("正在导入token...");

        tokenResult = tokenStore.importBase64Token(
          props.name || "通过URL导入的Token",
          props.token,
          {
            server: props.server,
            wsUrl: props.wsUrl,
            importMethod: "url",
          },
        );
      }

      if (tokenResult && tokenResult.success) {
        message.success(`Token "${tokenResult.tokenName}" 导入成功！`);

        // 如果auto=true，自动选择并跳转到控制台
        if (props.auto && tokenResult.token) {
          tokenStore.selectToken(tokenResult.token.id);
          message.success("正在跳转到控制台...");
          setTimeout(() => {
            router.push("/admin/dashboard");
          }, 1500);
        } else {
          // 清除URL参数，避免重复处理
          router.replace("/tokens");
        }
      } else {
        throw new Error(tokenResult?.message || "Token导入失败");
      }
    } catch (error) {
      console.error("URL参数处理失败:", error);
      message.error(`导入失败: ${error.message}`);
      // 清除URL参数
      router.replace("/tokens");
    } finally {
      isImporting.value = false;
    }
  }
};

// 监听路由参数变化
watch(() => [props.token, props.api], handleUrlParams, { immediate: false });

// 限流等待事件处理
const handleRateLimitWaiting = (data) => {
  rateLimitWaiting.value = true;
  rateLimitMessage.value = `Token刷新限流等待中，预计等待 ${data.waitSeconds} 秒（队列: ${data.queueSize}）`;
};

// 生命周期
onMounted(async () => {
  tokenStore.initTokenStore();

  // 监听限流等待事件
  $emit.on("token:refresh:waiting", handleRateLimitWaiting);

  // 处理URL参数
  await handleUrlParams();

  // 如果没有token且没有URL参数，显示导入表单
  if (!tokenStore.hasTokens && !props.token && !props.api) {
    showImportForm.value = true;
  }
});

onUnmounted(() => {
  // 移除限流等待事件监听
  $emit.off("token:refresh:waiting", handleRateLimitWaiting);
});
</script>

<style scoped lang="scss">
.token-import-page {
  --page-bg-start: #0c1021;
  --page-bg-end: #181d36;
  --panel-bg: rgba(15, 20, 38, 0.82);
  --panel-bg-strong: rgba(18, 24, 46, 0.96);
  --panel-border: rgba(255, 255, 255, 0.1);
  --panel-shadow: 0 24px 60px rgba(2, 6, 23, 0.34);
  --text-main: #f7fbff;
  --text-soft: rgba(236, 244, 255, 0.78);
  --text-faint: rgba(236, 244, 255, 0.54);
  --accent: #7af1d0;
  --accent-strong: #8f7bff;
  --accent-soft: rgba(122, 241, 208, 0.14);
  --danger-soft: rgba(255, 102, 138, 0.14);
  --section-tab-surface: linear-gradient(180deg, rgba(92, 77, 214, 0.22), rgba(18, 22, 44, 0.72));
  --section-tab-border: rgba(180, 166, 255, 0.36);
  --section-tab-text: rgba(248, 251, 255, 0.96);
  --section-tab-hover-bg: rgba(124, 108, 255, 0.26);
  --section-tab-hover-text: #f8fbff;
  --section-tab-active-bg: linear-gradient(135deg, #8b5cf6 0%, #7c6cff 100%);
  --section-tab-active-text: #ffffff;
  --section-tab-active-border: rgba(193, 178, 255, 0.78);
  --section-tab-active-shadow: 0 12px 22px rgba(124, 108, 255, 0.42);
  --section-tab-shadow: 0 12px 30px rgba(6, 10, 24, 0.3);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 10%, rgba(122, 241, 208, 0.14), transparent 28%),
    radial-gradient(circle at 85% 0%, rgba(143, 123, 255, 0.16), transparent 24%),
    linear-gradient(160deg, var(--page-bg-start), var(--page-bg-end));
  color: var(--text-main);
}

.page-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.page-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(12px);
  opacity: 0.6;
  animation: drift 18s ease-in-out infinite alternate;
}

.orb-a {
  width: 26rem;
  height: 26rem;
  top: -10rem;
  right: -6rem;
  background: radial-gradient(circle, rgba(143, 123, 255, 0.32), transparent 68%);
}

.orb-b {
  width: 20rem;
  height: 20rem;
  bottom: -8rem;
  left: -5rem;
  background: radial-gradient(circle, rgba(122, 241, 208, 0.24), transparent 68%);
  animation-delay: -7s;
}

.orb-c {
  width: 16rem;
  height: 16rem;
  top: 48%;
  left: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08), transparent 68%);
  animation-delay: -12s;
}

.page-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.28), transparent 78%);
}

.container {
  position: relative;
  z-index: 1;
  max-width: 1280px;
  margin: 0 auto;
  padding: clamp(16px, 3vw, 28px);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-panel,
.tokens-section,
.empty-panel {
  background: linear-gradient(180deg, rgba(20, 26, 48, 0.86), rgba(12, 17, 34, 0.8));
  border: 1px solid var(--panel-border);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.hero-panel {
  border-radius: 26px;
  padding: clamp(16px, 2.5vw, 24px);
}

.hero-top,
.hero-brand,
.hero-copy,
.hero-tools {
  display: none !important;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-logo {
  width: 68px;
  height: 68px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.28);
  object-fit: cover;
  flex-shrink: 0;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: rgba(122, 241, 208, 0.86);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(1.75rem, 4.5vw, 3rem);
  line-height: 1;
  letter-spacing: -0.04em;
}

.hero-desc {
  margin: 8px 0 0;
  max-width: 48rem;
  font-size: 0.95rem;
  line-height: 1.65;
  color: var(--text-soft);
}

.hero-tools {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.hero-stats {
  margin-top: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-stats-shell {
  margin-top: 0;
  padding: 12px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.015)),
    rgba(255, 255, 255, 0.02);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.stat-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 14px 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 98px;
}

.stat-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(143, 123, 255, 0.16), transparent 45%),
    radial-gradient(circle at bottom left, rgba(122, 241, 208, 0.12), transparent 40%);
  pointer-events: none;
}

.stat-card-primary {
  background: linear-gradient(160deg, rgba(122, 241, 208, 0.18), rgba(143, 123, 255, 0.12));
  border-color: rgba(122, 241, 208, 0.18);
}

.stat-card-wide {
  grid-column: span 1;
}

.stat-label {
  font-size: 0.82rem;
  color: var(--text-faint);
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: clamp(1.45rem, 2.8vw, 2rem);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.04em;
  position: relative;
  z-index: 1;
}

.stat-value-name {
  font-size: clamp(1rem, 2vw, 1.4rem);
}

.stat-hint {
  font-size: 0.82rem;
  color: var(--text-soft);
  position: relative;
  z-index: 1;
}

.hero-actions {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-action-primary :deep(.n-button) {
  width: 100%;
  min-height: 48px;
  border-radius: 999px;
  font-size: 0.98rem;
  font-weight: 800;
  color: #ffffff !important;
  background: var(--section-tab-active-bg) !important;
  border: 1px solid var(--section-tab-active-border) !important;
  box-shadow: var(--section-tab-active-shadow) !important;
}

.hero-action-rail {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 0.95fr) minmax(0, 1.1fr);
  gap: 8px;
  width: 100%;
  padding: 6px;
  border-radius: 999px;
  border: 1px solid var(--section-tab-border);
  background: var(--section-tab-surface);
  box-shadow: var(--section-tab-shadow);
  backdrop-filter: blur(14px);
}

.hero-action-rail :deep(.n-button) {
  width: 100%;
  min-width: 0;
  min-height: 38px;
  border-radius: 999px;
  padding-inline: 8px;
  font-size: 0.74rem;
  font-weight: 700;
  color: var(--section-tab-text) !important;
  background: rgba(124, 108, 255, 0.14) !important;
  border: 1px solid rgba(180, 166, 255, 0.34) !important;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-action-rail :deep(.n-button__content) {
  color: #f8fbff !important;
}

.hero-action-rail :deep(.n-dropdown) {
  display: block;
  width: 100%;
}

.rate-limit-alert {
  border-radius: 20px;
  border: 1px solid rgba(245, 158, 11, 0.24);
  background: rgba(245, 158, 11, 0.1);
  color: #fff;
}

.tokens-section {
  border-radius: 30px;
  padding: clamp(18px, 3vw, 28px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
}

.section-copy h2,
.empty-panel h2 {
  margin: 0;
  font-size: clamp(1.35rem, 2.2vw, 1.9rem);
  line-height: 1.1;
}

.section-copy p {
  margin: 8px 0 0;
  color: var(--text-soft);
  line-height: 1.6;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: rgba(122, 241, 208, 0.9);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.section-toolbar {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.view-switch,
.sort-switch {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--section-tab-border);
  border-radius: 999px;
  background: var(--section-tab-surface);
  backdrop-filter: blur(14px);
  box-shadow: var(--section-tab-shadow);
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.view-switch::-webkit-scrollbar,
.sort-switch::-webkit-scrollbar {
  display: none;
}

.view-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: stretch;
  width: 100%;
  overflow: hidden;
}

.view-switch-option,
.sort-switch :deep(.n-button) {
  flex: 0 0 auto;
  white-space: nowrap;
  border-radius: 999px;
  min-height: 36px;
  padding-inline: 14px;
  color: var(--section-tab-text) !important;
  border: 1px solid transparent;
  background: transparent;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.view-switch-option {
  width: 100%;
  min-width: 0;
  border-color: rgba(180, 166, 255, 0.34) !important;
  background: rgba(124, 108, 255, 0.14) !important;
  color: var(--section-tab-text) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.view-switch-option :deep(.n-button__content) {
  color: #f8fbff !important;
  font-weight: 800;
}

.view-switch-option:hover,
.sort-switch :deep(.n-button:hover) {
  color: var(--section-tab-hover-text) !important;
  background: var(--section-tab-hover-bg);
  border-color: var(--section-tab-border);
  transform: translateY(-1px);
}

.view-switch-option.active,
.view-switch-option.active:hover {
  color: var(--section-tab-active-text) !important;
  background: var(--section-tab-active-bg);
  border-color: var(--section-tab-active-border);
  box-shadow: var(--section-tab-active-shadow);
}

.sort-switch :deep(.n-button--primary-type) {
  color: var(--section-tab-active-text) !important;
  background: var(--section-tab-active-bg) !important;
  border-color: var(--section-tab-active-border) !important;
  box-shadow: var(--section-tab-active-shadow);
}

.sort-switch :deep(.n-button) {
  padding-inline: 16px;
}

.tokens-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.tokens-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.token-card,
.token-row-card {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(30, 35, 62, 0.88), rgba(18, 22, 44, 0.9)),
    linear-gradient(135deg, rgba(122, 241, 208, 0.06), rgba(143, 123, 255, 0.04));
  box-shadow: 0 16px 36px rgba(2, 6, 23, 0.2);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
  overflow: hidden;
}

.token-card-shell {
  position: relative;
}

.token-card-shell::before,
.token-row-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(143, 123, 255, 0.16), transparent 40%),
    radial-gradient(circle at bottom left, rgba(122, 241, 208, 0.12), transparent 35%);
  pointer-events: none;
}

.token-card-shell:hover,
.token-row-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 44px rgba(2, 6, 23, 0.26);
  border-color: rgba(122, 241, 208, 0.16);
}

.token-card-shell.active,
.token-row-card.active {
  border-color: rgba(122, 241, 208, 0.48);
  box-shadow: 0 0 0 1px rgba(122, 241, 208, 0.28), 0 22px 48px rgba(2, 6, 23, 0.32);
}

.token-card-shell :deep(.arco-card-header),
.token-card-shell :deep(.arco-card-body),
.token-card-shell :deep(.arco-card-actions),
.token-row-card :deep(.n-card__header),
.token-row-card :deep(.n-card__content),
.token-row-card :deep(.n-card__footer) {
  background: transparent;
  border: 0;
}

.token-card-shell :deep(.arco-card-body),
.token-row-card :deep(.n-card__content) {
  padding-top: 0;
}

.token-row-card :deep(.n-button),
.token-card-shell :deep(.arco-btn) {
  min-height: 38px;
  border-radius: 999px !important;
  padding-inline: 14px;
  font-weight: 800;
  color: #f8fbff !important;
  background-color: rgba(124, 108, 255, 0.16) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.22), rgba(18, 22, 44, 0.76)) !important;
  border: 1px solid rgba(180, 166, 255, 0.36) !important;
  box-shadow: 0 10px 18px rgba(124, 108, 255, 0.14);
}

.token-row-card :deep(.n-button:hover),
.token-card-shell :deep(.arco-btn:hover) {
  color: #ffffff !important;
  background-color: rgba(124, 108, 255, 0.26) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.28), rgba(18, 22, 44, 0.82)) !important;
  border-color: rgba(193, 178, 255, 0.56) !important;
}

.token-row-card :deep(.n-button--primary-type),
.token-card-shell :deep(.arco-btn-primary) {
  color: #ffffff !important;
  background-color: #7c6cff !important;
  background-image: linear-gradient(135deg, #8b5cf6 0%, #7c6cff 100%) !important;
  border-color: rgba(193, 178, 255, 0.68) !important;
  box-shadow: var(--section-tab-active-shadow) !important;
}

.token-row-card :deep(.n-button__content),
.token-card-shell :deep(.arco-btn-text),
.token-card-shell :deep(.arco-btn-content) {
  color: inherit !important;
  font-weight: 800;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.18);
}

.token-row-card :deep(.n-button--circle) {
  width: 38px;
  min-width: 38px;
  padding: 0 !important;
}

.token-row-card :deep(.n-button--circle .n-button__content) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.token-name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: var(--text-main);
  font-size: 1.02rem;
  font-weight: 800;
}

.token-display {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
}

.token-label {
  color: var(--text-faint);
  font-size: 0.82rem;
  font-weight: 600;
}

.token-value {
  flex: 1;
  min-width: 0;
  font-family:
    "SFMono-Regular",
    Consolas,
    "Liberation Mono",
    monospace;
  font-size: 0.86rem;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
}

.token-remark {
  margin: 10px 0;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--text-soft);
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease;
}

.token-remark:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(122, 241, 208, 0.16);
}

.token-remark-edit {
  cursor: default;
}

.remark-label {
  font-weight: 700;
  color: var(--text-main);
  flex-shrink: 0;
}

.remark-value {
  flex: 1;
  min-width: 0;
  color: var(--text-soft);
  word-break: break-all;
}

.token-timestamps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.timestamp-item {
  padding: 10px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.timestamp-label {
  display: block;
  font-size: 0.76rem;
  color: var(--text-faint);
  margin-bottom: 4px;
}

.timestamp-value {
  display: block;
  font-size: 0.82rem;
  color: var(--text-main);
}

.storage-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.storage-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.storage-label {
  font-size: 0.82rem;
  color: var(--text-faint);
  font-weight: 600;
}

.storage-upgrade {
  margin-top: 6px;
}

.empty-panel {
  border-radius: 30px;
  padding: clamp(28px, 4vw, 42px);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 14px;
}

.empty-visual {
  width: 88px;
  height: 88px;
  border-radius: 28px;
  display: grid;
  place-items: center;
  background: linear-gradient(160deg, rgba(122, 241, 208, 0.18), rgba(143, 123, 255, 0.16));
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 18px 36px rgba(2, 6, 23, 0.18);
}

.empty-visual img {
  width: 56px;
  height: 56px;
  object-fit: cover;
}

.empty-panel p {
  max-width: 34rem;
  margin: 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

:global(.token-import-modal .arco-modal) {
  width: min(94vw, 58rem) !important;
  max-width: calc(100vw - 24px);
  border-radius: 28px;
  overflow: hidden;
}

:global(.token-import-modal .arco-modal-body) {
  max-height: calc(100vh - 120px);
  padding: 18px 20px 22px;
  overflow-y: auto;
}

.card-header {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.modal-title-block h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  line-height: 1.15;
  color: #f8fbff;
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.18);
}

.modal-note {
  margin: 0;
  color: rgba(248, 251, 255, 0.92);
  font-size: 0.92rem;
  line-height: 1.6;
}

.import-method-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.import-method-group {
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(180, 166, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(124, 108, 255, 0.18), rgba(17, 21, 40, 0.9)),
    rgba(255, 255, 255, 0.03);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 12px 26px rgba(2, 6, 23, 0.18);
}

.import-method-group-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.import-method-group-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c6cff 100%);
  border: 1px solid rgba(193, 178, 255, 0.66);
  box-shadow: 0 10px 18px rgba(124, 108, 255, 0.32);
}

.import-method-group-head p {
  margin: 0;
  color: rgba(248, 251, 255, 0.92);
  font-size: 0.84rem;
  line-height: 1.45;
  text-align: right;
}

.import-method-tabs {
  display: grid;
  grid-auto-flow: column;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px;
  border-radius: 999px;
  border: 1px solid var(--section-tab-border);
  background: var(--section-tab-surface);
  backdrop-filter: blur(14px);
  box-shadow: var(--section-tab-shadow);
  overflow: hidden;
}

.import-method-tabs--quick {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.import-method-tabs--advanced {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.import-method-tab {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.01em;
  white-space: nowrap;
  line-height: 1.15;
  padding: 8px 12px;
  color: var(--section-tab-text) !important;
  border: 1px solid rgba(180, 166, 255, 0.34) !important;
  background: rgba(124, 108, 255, 0.14) !important;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.import-method-tab :deep(.n-button__content) {
  color: #f8fbff !important;
  font-weight: 800;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.18);
}

.import-method-tab.active {
  color: var(--section-tab-active-text) !important;
  background: var(--section-tab-active-bg) !important;
  border-color: var(--section-tab-active-border) !important;
  box-shadow: var(--section-tab-active-shadow) !important;
}

.import-method-tab:hover {
  color: var(--section-tab-hover-text) !important;
  background: var(--section-tab-hover-bg) !important;
  border-color: var(--section-tab-border) !important;
  transform: translateY(-1px);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 14px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(124, 108, 255, 0.08), rgba(12, 17, 34, 0.96)),
    rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(180, 166, 255, 0.18);
}

.token-import-modal :deep(.form-actions) {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.token-import-modal :deep(.form-actions button.import-action),
.token-import-modal :deep(.form-actions .n-button.import-action),
.token-import-modal :deep(.form-actions .n-button),
.token-import-modal :deep(.form-actions .arco-btn) {
  width: 100%;
  min-height: 44px;
  border-radius: 999px;
  padding-inline: 18px;
  font-weight: 800;
  color: var(--section-tab-text) !important;
  background-color: rgba(124, 108, 255, 0.14) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.18), rgba(18, 22, 44, 0.74)) !important;
  border: 1px solid rgba(180, 166, 255, 0.36) !important;
  box-shadow: var(--section-tab-shadow);
}

.token-import-modal :deep(.form-actions button.import-action:hover),
.token-import-modal :deep(.form-actions .n-button.import-action:hover),
.token-import-modal :deep(.form-actions .n-button:hover),
.token-import-modal :deep(.form-actions .arco-btn:hover) {
  color: var(--section-tab-hover-text) !important;
  background-color: rgba(124, 108, 255, 0.24) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.24), rgba(18, 22, 44, 0.78)) !important;
  border-color: var(--section-tab-border) !important;
}

.token-import-modal :deep(.form-actions button.import-action--primary),
.token-import-modal :deep(.form-actions .n-button.import-action--primary),
.token-import-modal :deep(.form-actions .n-button--primary-type),
.token-import-modal :deep(.form-actions .arco-btn-primary) {
  color: #ffffff !important;
  background-color: #7c6cff !important;
  background-image: linear-gradient(135deg, #8b5cf6 0%, #7c6cff 100%) !important;
  border-color: var(--section-tab-active-border) !important;
  box-shadow: var(--section-tab-active-shadow) !important;
}

.token-import-modal :deep(.form-actions button.import-action--secondary),
.token-import-modal :deep(.form-actions .n-button.import-action--secondary) {
  color: #f8fbff !important;
  --n-text-color: #f8fbff;
  --n-text-color-hover: #ffffff;
  --n-text-color-pressed: #ffffff;
  background-color: rgba(124, 108, 255, 0.16) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.16), rgba(18, 22, 44, 0.72)) !important;
  border-color: rgba(180, 166, 255, 0.3) !important;
}

.token-import-modal :deep(.form-actions .n-button__content),
.token-import-modal :deep(.form-actions .arco-btn-text) {
  color: inherit !important;
  font-weight: 800;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.18);
}

:global([data-theme="dark"] .token-import-modal .arco-modal) {
  background: linear-gradient(180deg, rgba(19, 24, 46, 0.98), rgba(11, 15, 31, 0.98)) !important;
  border: 1px solid rgba(180, 166, 255, 0.16);
}

:global(.token-import-modal .n-form-item-label),
:global(.token-import-modal .n-form-item-label__text),
:global(.token-import-modal .arco-form-item-label),
:global(.token-import-modal .arco-form-item-label-col) {
  color: rgba(248, 251, 255, 0.96) !important;
  font-weight: 700;
}

:global(.token-import-modal .n-input),
:global(.token-import-modal .n-input__input),
:global(.token-import-modal .n-input__textarea),
:global(.token-import-modal .arco-input),
:global(.token-import-modal .arco-textarea) {
  color: #f8fbff !important;
  background-color: rgba(124, 108, 255, 0.12) !important;
}

:global(.token-import-modal .n-input__placeholder),
:global(.token-import-modal .arco-input::placeholder),
:global(.token-import-modal .arco-textarea::placeholder),
:global(.token-import-modal .n-input__placeholder span) {
  color: rgba(248, 251, 255, 0.48) !important;
}

@keyframes drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }

  to {
    transform: translate3d(0, 18px, 0) scale(1.06);
  }
}

@media (max-width: 1024px) {
  .section-header {
    grid-template-columns: 1fr;
  }

  .section-toolbar {
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .hero-top,
  .hero-brand,
  .hero-copy,
  .hero-tools {
    display: none !important;
  }

  .hero-top {
    flex-direction: column;
  }

  .hero-brand {
    align-items: flex-start;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-action-primary :deep(.n-button) {
    width: 100%;
  }

  .hero-action-rail {
    gap: 6px;
    grid-template-columns: minmax(0, 0.92fr) minmax(0, 0.92fr) minmax(0, 1.16fr);
  }

  .hero-action-rail :deep(.n-button) {
    min-height: 36px;
    padding-inline: 6px;
    font-size: 0.7rem;
  }

  .section-toolbar {
    width: 100%;
  }

  .sort-switch,
  .view-switch {
    width: 100%;
  }

  .import-method-tab {
    font-size: 0.74rem;
  }

  .tokens-grid {
    grid-template-columns: 1fr;
  }

  .token-timestamps {
    grid-template-columns: 1fr;
  }

  .modal-actions {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 12px;
  }

  .hero-panel,
  .tokens-section,
  .empty-panel {
    border-radius: 24px;
  }

  .brand-logo {
    width: 64px;
    height: 64px;
  }

  .token-display,
  .token-remark,
  .timestamp-item {
    border-radius: 14px;
  }

  :global(.token-import-modal .arco-modal-body) {
    padding: 14px 12px 16px;
  }

  .modal-title-block h2 {
    font-size: 1.05rem;
  }

  .modal-note {
    font-size: 0.82rem;
  }

  .modal-intro {
    padding: 12px 14px;
  }

  .import-method-group {
    padding: 12px;
    border-radius: 16px;
  }

  .import-method-group-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .import-method-group-head p {
    text-align: left;
    font-size: 0.78rem;
  }

  .import-method-tab {
    font-size: 0.74rem;
    padding: 8px 10px;
  }

  .card-body {
    padding: 12px;
  }
}
</style>
