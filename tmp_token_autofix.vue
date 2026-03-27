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
        <div class="hero-stats">
          <article class="stat-card stat-card-primary">
            <span class="stat-label">�?Token</span>
            <strong class="stat-value">{{ totalTokenCount }}</strong>
            <span class="stat-hint">当前库中所有账�</span>
          </article>
          <article class="stat-card">
            <span class="stat-label">在线连接</span>
            <strong class="stat-value">{{ connectedTokenCount }}</strong>
            <span class="stat-hint">{{ connectingTokenCount }} 个正在连�</span>
          </article>
          <article class="stat-card">
            <span class="stat-label">长期有效</span>
            <strong class="stat-value">{{ permanentTokenCount }}</strong>
            <span class="stat-hint">{{ temporaryTokenCount }} 个临时存�</span>
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

        <div class="hero-actions">
          <n-button type="primary" size="medium" @click="showImportForm = true">
            <template #icon>
              <n-icon>
                <Add />
              </n-icon>
            </template>
            添加Token
          </n-button>

          <n-button size="medium" secondary @click="goToDashboard">
            <template #icon>
              <n-icon>
                <List />
              </n-icon>
            </template>
            批量功能
          </n-button>

          <n-button ghost type="info" size="medium" @click="checkAppUpdate">
            <template #icon>
              <n-icon>
                <SyncCircle />
              </n-icon>
            </template>
            检查更�?          </n-button>
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
        width="40rem"
        :footer="false"
        :default-visible="!tokenStore.hasTokens"
      >
        <template #title>
          <h2>
            <n-icon>
              <Add />
            </n-icon>
            添加游戏Token
          </h2>
        </template>
        <div class="card-header">
          <!-- 导入方式选择 -->
          <n-radio-group
            v-model:value="importMethod"
            class="import-method-tabs"
            size="small"
          >
            <n-radio-button value="manual"> 鎵嬪姩杈撳叆 </n-radio-button>
            <n-radio-button value="url"> URL鑾峰彇 </n-radio-button>
            <n-radio-button value="wxQrcode"> 微信扫码获取 </n-radio-button>
            <n-radio-button value="bin"> BIN多角色获�?</n-radio-button>
            <n-radio-button value="singlebin"> BIN单角色获�?</n-radio-button>
          </n-radio-group>
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
            <span class="section-kicker">TOKEN 搴</span>
            <h2>我的Token列表 ({{ tokenStore.gameTokens.length }}涓?</h2>
            <p>拖拽可重新排序，点击卡片快速进入控制台或刷新连接�</p>
          </div>

          <div class="section-toolbar">
            <n-radio-group v-model:value="viewMode" size="small" class="view-switch">
              <n-radio-button value="list">列表</n-radio-button>
              <n-radio-button value="card">卡片</n-radio-button>
            </n-radio-group>

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
                服务�?{{ getSortIcon("server") }}
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
              最后使�?{{ getSortIcon("lastUsed") }}
              </n-button>
            </n-button-group>

            <n-dropdown :options="bulkOptions" @select="handleBulkAction">
              <n-button size="small" ghost class="toolbar-bulk-button">
                <template #icon>
                  <n-icon>
                    <Menu />
                  </n-icon>
                </template>
                鎵归噺鎿嶄綔
              </n-button>
            </n-dropdown>
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
                <!-- 连接状态文�?-->
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
                <span class="remark-label">备注�</span>
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
                <span class="remark-label">备注�</span>
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
                {{ token.sourceUrl ? "刷新" : "閲嶆柊鑾峰彇" }}
              </a-button>

              <div class="token-timestamps">
                <div class="timestamp-item">
                  <span class="timestamp-label">创建�</span>
                  <span class="timestamp-value">{{
                    formatTime(token.createdAt)
                  }}</span>
                </div>
                <div class="timestamp-item">
                  <span class="timestamp-label">浣跨敤锛</span>
                  <span class="timestamp-value">{{
                    formatTime(token.lastUsed)
                  }}</span>
                </div>
              </div>

              <!-- 瀛樺偍绫诲瀷淇℃伅 -->
              <div class="storage-info">
                <div class="storage-item">
                  <span class="storage-label">瀛樺偍绫诲瀷锛</span>
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
                        : "涓存椂瀛樺偍"
                    }}
                  </n-tag>
                </div>

                <!-- 升级选项（仅对临时存储的token鏄剧ず锛?-->
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
                    升级为长期有�?
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
                进入控制�?
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
                <!-- 连接状�?- 移动到最前端显示 -->
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

                <!-- Token鍩烘湰淇℃伅 -->
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
                <!-- 瀛樺偍绫诲瀷 -->
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
                      : "涓存椂"
                  }}
                </n-tag>

                <!-- 升级选项（仅对临时存储的token鏄剧ず锛?-->
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
                  控制�?
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

      <!-- 空状�?-->
      <section v-if="!tokenStore.hasTokens && !showImportForm" class="empty-panel">
        <div class="empty-visual">
          <img src="/icons/xiaoyugan.png" alt="XYZW" />
        </div>
        <h2>还没有导入任�?Token</h2>
        <p>先添加一个账号，或从 URL銆丅IN、微信扫码导入，再进入控制台开始管理�</p>
        <div class="empty-actions">
          <n-button type="primary" @click="showImportForm = true">
            添加 Token
          </n-button>
          <n-button ghost type="info" @click="checkAppUpdate">
            检查更�?          </n-button>
        </div>
      </section>
    </div>

    <!-- 编辑Token妯℃€佹 -->
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
        <n-form-item label="Token瀛楃涓?" path="token">
          <n-input
            v-model:value="editForm.token"
            type="textarea"
            :rows="3"
            placeholder="绮樿创Token瀛楃涓?.."
            clearable
          />
        </n-form-item>
        <n-form-item label="服务�?">
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
          <n-button type="primary" @click="saveEdit"> 淇濆瓨 </n-button>
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

// 限流等待状�?
const rateLimitWaiting = ref(false);
const rateLimitMessage = ref("");

// 鍝嶅簲寮忔暟鎹?
const showImportForm = ref(false);
const isImporting = ref(false);
const showEditModal = ref(false);
const importFormRef = ref(null);
const urlFormRef = ref(null);
const editFormRef = ref(null);
const editingToken = ref(null);
const importMethod = ref("manual");
const refreshingTokens = ref(new Set());
const connectingTokens = ref(new Set());
// 从localStorage读取上次的视图模式，默认为列表视�?
const viewMode = ref(localStorage.getItem("tokenViewMode") || "list");
const dragIndex = ref(null);

// 备注编辑状态管�?
const editingRemark = ref(null); // 当前正在编辑备注的tokenId
const tempRemarks = ref({}); // 临时保存编辑中的备注内容

// 监听视图模式变化，保存到localStorage
watch(viewMode, (newViewMode) => {
  localStorage.setItem("tokenViewMode", newViewMode);
});

// 排序状态管�?- 从localStorage读取上次的排序设�?
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

    // 根据排序字段获取比较�?
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

    // 根据排序方向比较�?
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
  // 可以在这里设置拖拽时的预览图�?
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

  // 鏇存柊 store
  tokenStore.gameTokens = currentTokens;

  // 切换到手动排序模式，防止自动排序打乱顺序
  sortConfig.value.field = "manual";
  // 保存排序设置
  localStorage.setItem("tokenSortConfig", JSON.stringify(sortConfig.value));

  dragIndex.value = null;
  message.success("Token 順序已更新");
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
  token: [{ required: true, message: "请输入Token瀛楃涓?", trigger: "blur" }],
};

const bulkOptions = [
  { label: "刷新所有Token", key: "refreshAll" },
  { label: "鏇存柊token淇℃伅", key: "updateInfo" },
  { label: "导出所有Token", key: "export" },
  { label: "导入Token文件", key: "import" },
  { label: "清理过期Token", key: "clean" },
  { label: "断开所有连�?", key: "disconnect" },
  { label: "清除所有Token", key: "clear" },
];

/**
 * 鎵嬪姩鎵撳紑Token管理卡片
 */
const openshowImportForm = () => {
  showImportForm.value = true;
};

// 刷新Token
const refreshToken = async (token) => {
  refreshingTokens.value.add(token.id);

  try {
    if (token.importMethod === "url") {
      // 有源URL的token - 从URL閲嶆柊鑾峰彇锛堜娇鐢ㄩ檺娴侊級
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
              `跨域请求被阻止。请确保目标服务器支持CORS銆傞敊璇鎯? ${corsError.message}`,
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
          throw new Error("返回数据中未找到token瀛楁");
        }

        return result;
      });

      // 鏇存柊token淇℃伅
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
          console.log("宸茶縼绉籌ndexedDB鏁版嵁:", token.name, "->", token.id);
        }
        message.success("Token刷新成功");
      }
    } else {
      dialog.info({
      title: "閲嶆柊鑾峰彇Token",
      content: `Token "${token.name}" 是通过微信扫码登录导入的，没有配置自动刷新地址�?

请选择以下操作�?
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

    // 如果当前token有连接，需要重新连�?
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

// 升级Token为长期有�?
const upgradeTokenToPermanent = (token) => {
  dialog.warning({
    title: "升级为长期有效",
    content: `确认要将Token "${token.name}" 升级为长期有效吗？升级后该Token将不会因为24小时未使用而被自动清理。`,
    positiveText: "确认升级",
    negativeText: "取消",
    onPositiveClick: () => {
      const success = tokenStore.upgradeTokenToPermanent(token.id);
      if (success) {
        message.success(`Token "${token.name}" 已升级为长期有效`);
      } else {
        message.error("升级失败，该Token可能已经是长期有效状态");
      }
    },
  });
};

const selectToken = (token, forceReconnect = false) => {
  // 如果有备注正在编辑，先保存备注
  if (editingRemark.value) {
    saveCurrentRemark();
    return;
  }

  const isAlreadySelected = selectedTokenId.value === token.id;
  const connectionStatus = getConnectionStatus(token.id);

  if (connectionStatus === "connected" && !forceReconnect) {
    tokenStore.closeWebSocketConnection(token.id);
    message.success(`已断开 ${token.name} 的连接`);
    return;
  }

  if (connectionStatus === "connecting" && !forceReconnect) {
    message.info(`${token.name} 正在连接中...`);
    return;
  }

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
  // 连接成功时服务器标签使用绿色，其他状态保持红�?
  return status === "connected" ? "success" : "error";
};

const getServerTagColor = (tokenId) => {
  const status = getConnectionStatus(tokenId);
  // 连接成功时服务器标签使用绿色，其他状态保持红�?  return status === "connected" ? "green" : "red";
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

  // 鏍规嵁Token类型添加刷新选项
  if (token.importMethod === "url" && token.sourceUrl) {
    actions.push({
      label: "从URL刷新",
      key: "refresh-url",
      icon: () => h(NIcon, null, { default: () => h(SyncCircle) }),
    });
  } else {
    actions.push({
      label: "閲嶆柊鑾峰彇",
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
      // 閲嶆柊鑾峰彇Token
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

// 蹇€熺紪杈戝娉ㄥ姛鑳?
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

  dialog.warning({
    title: "批量刷新Token",
    content: "确定要刷新所有支持自动刷新的Token吗？",
    positiveText: "开始刷新",
    negativeText: "取消",
    onPositiveClick: async () => {
      try {
        let successCount = 0;
        let failCount = 0;

        const loadingMessage = message.loading(
          `正在批量刷新Token (0/${tokensToRefresh.length})`,
          {
            duration: 0,
          },
        );

        for (let i = 0; i < tokensToRefresh.length; i++) {
          const token = tokensToRefresh[i];

          try {
            loadingMessage.content = `正在刷新Token (${i + 1}/${tokensToRefresh.length}): ${token.name}`;
            await refreshToken(token);
            successCount++;
          } catch (error) {
            console.error(`刷新Token "${token.name}" 失败:`, error);
            failCount++;
          }
        }

        loadingMessage.destroy();
        rateLimitWaiting.value = false;

        if (failCount === 0) {
          message.success(`批量刷新完成，成功刷新 ${successCount} 个Token`);
        } else {
          message.warning(`批量刷新完成，成功 ${successCount} 个，失败 ${failCount} 个`);
        }

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
  message.success(`宸叉竻鐞?${count} 个过期Token`);
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
    positiveText: "纭畾娓呴櫎",
    negativeText: "取消",
    onPositiveClick: async () => {
      await tokenStore.clearAllTokens();
      message.success("所有Token已清除");
    },
  });
};

// 一键连接更新所有token淇℃伅
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

        const loadingMessage = message.loading(
          `正在更新Token信息 (0/${totalTokens})`,
          {
            duration: 0,
          },
        );

        for (let i = 0; i < tokenStore.gameTokens.length; i++) {
          const token = tokenStore.gameTokens[i];

          loadingMessage.content = `正在更新Token信息 (${i + 1}/${totalTokens}): ${token.name}`;

          try {
            await tokenStore.selectToken(token.id);
            await new Promise((resolve) => setTimeout(resolve, 1000));
            tokenStore.closeWebSocketConnection(token.id);
            successCount++;
            message.success(`Token "${token.name}" 信息更新成功`);
          } catch (error) {
            console.error(`更新Token "${token.name}" 失败:`, error);
            failCount++;
            message.error(`Token "${token.name}" 信息更新失败`);
          }

          if (i < tokenStore.gameTokens.length - 1) {
            await new Promise((resolve) => setTimeout(resolve, 500));
          }
        }

        loadingMessage.destroy();

        if (failCount === 0) {
          message.success(`所有Token信息更新完成，成功更新 ${successCount} 个Token`);
        } else {
          message.warning(`Token信息更新完成，成功 ${successCount} 个，失败 ${failCount} 个`);
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

  message.info("璇峰湪 Android App 中使用该功能");
};

// 开始任务管�?- 直接跳转到控制台
const startTaskManagement = async (token) => {
  // 选择token
  tokenStore.selectToken(token.id);
  message.success(`正在进入 ${token.name} 的控制台`);

  try {
    await router.replace({ name: "Dashboard" });
  } catch (error) {
    console.error("进入控制台失�?", error);
  }

  // 鏌愪簺 Android WebView 环境下路由切换可能会被拦截，做一�?hash 兜底�?
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
  if (!(props.token || props.api)) {
    return;
  }

  try {
    isImporting.value = true;
    let tokenResult = null;

    if (props.api) {
      message.info("正在从 API 获取 token...");

      const response = await fetch(props.api, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        mode: "cors",
      });

      if (!response.ok) {
        throw new Error(
          `API 请求失败: ${response.status} ${response.statusText}`,
        );
      }

      const data = await response.json();

      if (!data.token) {
        throw new Error("API 返回数据中未找到 token 字段");
      }

      tokenResult = tokenStore.importBase64Token(
        props.name || data.name || "通过 API 导入的 Token",
        data.token,
        {
          server: props.server || data.server,
          wsUrl: props.wsUrl,
          sourceUrl: props.api,
          importMethod: "url",
        },
      );
    } else if (props.token) {
      message.info("正在导入 token...");

      tokenResult = tokenStore.importBase64Token(
        props.name || "通过 URL 导入的 Token",
        props.token,
        {
          server: props.server,
          wsUrl: props.wsUrl,
          importMethod: "url",
        },
      );
    }

    if (tokenResult && tokenResult.success) {
      message.success(`Token "${tokenResult.tokenName}" 导入成功`);

      if (props.auto && tokenResult.token) {
        tokenStore.selectToken(tokenResult.token.id);
        message.success("正在跳转到控制台...");
        setTimeout(() => {
          router.push("/admin/dashboard");
        }, 1500);
      } else {
        router.replace("/tokens");
      }
      return;
    }

    throw new Error(tokenResult?.message || "Token 导入失败");
  } catch (error) {
    console.error("URL 参数处理失败:", error);
    const errorMessage = error instanceof Error ? error.message : String(error);
    message.error(`导入失败: ${errorMessage}`);
    router.replace("/tokens");
  } finally {
    isImporting.value = false;
  }
};

// 监听路由参数变化
watch(() => [props.token, props.api], handleUrlParams, { immediate: false });

// 限流等待事件处理
const handleRateLimitWaiting = (data) => {
  rateLimitWaiting.value = true;
  rateLimitMessage.value = `Token 刷新限流等待中，预计等待 ${data.waitSeconds} 秒（队列: ${data.queueSize}）`;
};

// 生命周期
onMounted(async () => {
  tokenStore.initTokenStore();

  // 监听限流等待事件
  $emit.on("token:refresh:waiting", handleRateLimitWaiting);

  // 处理URL参数
  await handleUrlParams();

  // 濡傛灉娌℃湁token且没有URL参数，显示导入表�?
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
  border-radius: 24px;
  padding: clamp(12px, 2vw, 18px) clamp(12px, 2.4vw, 20px) 16px;
  background: linear-gradient(180deg, rgba(20, 26, 48, 0.72), rgba(12, 17, 34, 0.56));
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: none;
}

.hero-stats {
  margin-top: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 12px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 92px;
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

.stat-label {
  font-size: 0.74rem;
  color: var(--text-faint);
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: clamp(1.25rem, 2.6vw, 1.8rem);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.04em;
  position: relative;
  z-index: 1;
}

.stat-value-name {
  font-size: clamp(0.95rem, 2vw, 1.2rem);
}

.stat-hint {
  font-size: 0.74rem;
  line-height: 1.35;
  color: var(--text-soft);
  position: relative;
  z-index: 1;
}

.hero-actions {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-actions :deep(.n-button) {
  width: 100%;
  min-width: 0;
}

.hero-actions :deep(.n-button__content) {
  white-space: nowrap;
}

.toolbar-bulk-button {
  white-space: nowrap;
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
  flex-wrap: wrap;
}



.tokens-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.tokens-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.token-card,
.token-row-card {
  border-radius: 24px;
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
  width: min(92vw, 40rem) !important;
  max-width: calc(100vw - 24px);
  border-radius: 26px;
  overflow: hidden;
}

:global(.token-import-modal .arco-modal-body) {
  padding-top: 8px;
}

.card-header {
  margin-bottom: 14px;
}

.import-method-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.import-method-tabs :deep(.n-radio-button) {
  width: 100%;
  min-width: 0;
  text-align: center;
}

.import-method-tabs :deep(.n-radio-button__label) {
  white-space: nowrap;
  font-size: 0.82rem;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:global(.token-import-modal .form-actions) {
  margin-top: 20px !important;
  display: grid !important;
  gap: 10px !important;
}

:global(.token-import-modal .form-actions .n-button) {
  min-height: 50px;
  border-radius: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

:global(.token-import-modal .form-actions .n-button:hover) {
  transform: translateY(-1px);
}

:global(.token-import-modal .form-actions .n-button--primary-type) {
  background: linear-gradient(135deg, rgba(122, 241, 208, 0.98), rgba(124, 166, 255, 0.94)) !important;
  border-color: transparent !important;
  color: #08111f !important;
  box-shadow: 0 16px 28px rgba(122, 241, 208, 0.22);
}

:global(.token-import-modal .form-actions .n-button:not(.n-button--primary-type)) {
  background: rgba(255, 255, 255, 0.03) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: var(--text-main) !important;
}
:global([data-theme="dark"] .token-import-modal .arco-modal) {
  background: linear-gradient(180deg, rgba(21, 27, 50, 0.98), rgba(15, 20, 38, 0.98)) !important;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .n-form-item-label,
[data-theme="dark"] .n-form-item-label__text {
  color: #ffffff !important;
}

[data-theme="dark"] .n-input__input,
[data-theme="dark"] .n-input__textarea {
  color: #ffffff !important;
  background-color: rgba(255, 255, 255, 0.08) !important;
}

[data-theme="dark"] .n-input__placeholder {
  color: rgba(255, 255, 255, 0.42) !important;
}

[data-theme="dark"] .n-card {
  color: #ffffff !important;
}

[data-theme="dark"] .n-radio-button {
  color: #ffffff !important;
}

[data-theme="dark"] .n-radio-button--checked {
  background-color: rgba(122, 241, 208, 0.18) !important;
  color: #ffffff !important;
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
  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-header {
    grid-template-columns: 1fr;
  }

  .section-toolbar {
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .import-method-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .hero-actions :deep(.n-button) {
    width: 100%;
    min-width: 0;
  }

  .section-toolbar {
    width: 100%;
  }

  .sort-switch,
  .view-switch {
    width: 100%;
  }

  .sort-switch :deep(.n-button),
  .view-switch :deep(.n-radio-button) {
    flex: 1 1 auto;
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

@media (min-width: 769px) {
  .import-method-tabs {
    grid-template-columns: repeat(5, minmax(0, 1fr));
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

  .hero-stats {
    gap: 8px;
  }

  .stat-card {
    min-height: 84px;
    padding: 11px 12px;
  }

  .hero-actions {
    gap: 8px;
  }

  .token-display,
  .token-remark,
  .timestamp-item {
    border-radius: 14px;
  }

  .import-method-tabs :deep(.n-radio-button__label) {
    font-size: 0.76rem;
  }
}
</style>




















