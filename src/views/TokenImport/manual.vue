<template>
  <!-- 手动输入表单 -->
  <n-form
    ref="importFormRef"
    :model="importForm"
    :rules="importRules"
    :label-placement="'top'"
    :size="'large'"
    :show-label="true"
  >
    <n-form-item :label="'游戏角色名称'" :path="'name'" :show-label="true">
      <n-input
        v-model:value="importForm.name"
        placeholder="例如：主号战士"
        clearable
      />
    </n-form-item>

    <n-form-item
      :label="'Token字符串'"
      :path="'base64Token'"
      :show-label="true"
    >
      <n-input
        v-model:value="importForm.base64Token"
        type="textarea"
        :rows="3"
        placeholder="粘贴Token字符串..."
        clearable
      >
        <template #suffix>
          <n-popover placement="right" trigger="hover">
            <template #trigger>
              <n-icon :depth="1">
                <AlertCircleOutline />
              </n-icon>
            </template>
            <div class="large-text">
              输入格式为：{"roleToken":"****","sessId":***,"connId":***,"isRestore":***}
            </div>
          </n-popover>
        </template>
      </n-input>
    </n-form-item>

    <!-- 角色详情 -->
    <n-collapse>
      <n-collapse-item title="角色详情 (可选)" name="optional">
        <div class="optional-fields">
          <n-form-item label="服务器">
            <n-input
              v-model:value="importForm.server"
              placeholder="服务器名称"
            />
          </n-form-item>

          <n-form-item label="自定义连接地址">
            <n-input
              v-model:value="importForm.wsUrl"
              placeholder="留空使用默认连接"
            />
          </n-form-item>
        </div>
      </n-collapse-item>
    </n-collapse>

    <div class="form-actions">
      <n-button
        type="default"
        class="import-action import-action--primary"
        size="large"
        block
        :loading="isImporting"
        @click="handleImport"
      >
        <template #icon>
          <n-icon>
            <CloudUpload />
          </n-icon>
        </template>
        添加Token
      </n-button>

      <n-button
        v-if="tokenStore.hasTokens"
        type="default"
        class="import-action import-action--secondary"
        size="large"
        block
        @click="cancel"
      >
        取消
      </n-button>
    </div>
  </n-form>
</template>
<script lang="ts" setup>
import { useTokenStore } from "@/stores/tokenStore";
import { CloudUpload, AlertCircleOutline } from "@vicons/ionicons5";
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  useMessage,
} from "naive-ui";
import { reactive, ref } from "vue";

const $emit = defineEmits(["cancel", "ok"]);

const cancel = () => {
  $emit("cancel");
};

const tokenStore = useTokenStore();
const message = useMessage();
const importFormRef = ref();
const isImporting = ref(false);
const importForm = reactive({
  name: "",
  base64Token: "",
  server: "",
  wsUrl: "",
});
const importRules = {
  name: [
    { required: true, message: "请输入角色名称", trigger: "blur" },
    {
      min: 1,
      max: 50,
      message: "名称长度应在1到50个字符之间",
      trigger: "blur",
    },
  ],
  base64Token: [
    { required: true, message: "请输入Token字符串", trigger: "blur" },
    { min: 20, message: "Token字符串长度应至少20个字符", trigger: "blur" },
  ],
};
const handleImport = () => {
  isImporting.value = true;
  try {
    tokenStore.addToken({
      name: importForm.name,
      token: importForm.base64Token,
      server: importForm.server,
      wsUrl: importForm.wsUrl,
    });
    message.success("Token添加成功");
    importForm.name = "";
    importForm.base64Token = "";
    importForm.server = "";
    importForm.wsUrl = "";
    $emit("ok");
  } catch (error: any) {
    message.error(`添加Token失败: ${error.message || error}`);
  } finally {
    isImporting.value = false;
  }
};
</script>
<style lang="scss" scoped>
.optional-fields {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;

  n-form-item {
    flex: 1 1 45%;
    min-width: 200px;
  }
}

.form-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-actions :deep(button.import-action),
.form-actions :deep(.n-button.import-action) {
  width: 100%;
  min-height: 48px;
  border-radius: 999px;
  font-weight: 800;
  letter-spacing: 0.01em;
  color: var(--section-tab-text) !important;
  background-color: rgba(124, 108, 255, 0.14) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.18), rgba(18, 22, 44, 0.74)) !important;
  border: 1px solid var(--section-tab-border) !important;
  box-shadow: var(--section-tab-shadow) !important;
}

.form-actions :deep(button.import-action--primary),
.form-actions :deep(.n-button.import-action--primary) {
  color: #ffffff !important;
  background-color: #7c6cff !important;
  background-image: linear-gradient(135deg, #8b5cf6 0%, #7c6cff 100%) !important;
  border-color: var(--section-tab-active-border) !important;
  box-shadow: var(--section-tab-active-shadow) !important;
}

.form-actions :deep(button.import-action--secondary),
.form-actions :deep(.n-button.import-action--secondary) {
  color: #f8fbff !important;
  --n-text-color: #f8fbff;
  --n-text-color-hover: #ffffff;
  --n-text-color-pressed: #ffffff;
  background-color: rgba(124, 108, 255, 0.16) !important;
  background-image: linear-gradient(180deg, rgba(124, 108, 255, 0.16), rgba(18, 22, 44, 0.72)) !important;
  border-color: rgba(180, 166, 255, 0.34) !important;
}

.form-tips {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #888;
}

.cors-tip {
  color: #e67e22;
}
</style>
