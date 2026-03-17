<template>
  <div class="douyin-phone-import">
    <div class="login-flow-info">
      <h3>手机号验证码登录</h3>
      <ol class="flow-steps">
        <li>输入手机号并发送验证码</li>
        <li>输入收到的验证码并验证登录</li>
        <li>验证成功后可将返回的角色 Token 导入系统</li>
      </ol>
    </div>

    <n-form
      :model="phoneForm"
      label-placement="top"
      :show-label="true"
    >
      <n-form-item label="手机号" :show-label="true">
        <div class="phone-row">
          <n-input
            v-model:value="phoneForm.countryCode"
            class="country-code-input"
            placeholder="+86"
          />
          <n-input
            v-model:value="phoneForm.phone"
            class="phone-input"
            placeholder="请输入手机号"
            clearable
          />
          <n-button
            type="primary"
            :loading="isSendingCode"
            :disabled="sendCodeCooldown > 0"
            @click="sendSmsCode"
          >
            {{ sendCodeCooldown > 0 ? `${sendCodeCooldown}s后重试` : "发送验证码" }}
          </n-button>
        </div>
      </n-form-item>

      <n-form-item label="验证码" :show-label="true">
        <div class="sms-row">
          <n-input
            v-model:value="phoneForm.smsCode"
            placeholder="请输入验证码"
            clearable
          />
          <n-button type="success" :loading="isVerifying" @click="verifyCodeAndFetchToken">
            验证并获取Token
          </n-button>
        </div>
      </n-form-item>

      <n-form-item label="角色命名模板" :show-label="true">
        <n-input
          v-model:value="phoneForm.nameTemplate"
          placeholder="{name}-{id}"
        />
        <template #feedback>
          支持变量: {name}角色名, {id}角色ID, {index}序号, {server}服务器
        </template>
      </n-form-item>
    </n-form>

    <div class="status-line" :class="statusType">
      {{ statusMessage }}
    </div>

    <a-list v-if="roleList.length > 0">
      <a-list-item v-for="(role, index) in roleList" :key="role.id">
        <div class="role-item">
          <div>
            <strong>角色名称:</strong> {{ role.name }}<br />
            <strong>Token:</strong>
            <span style="word-break: break-all">{{ role.token }}</span><br />
            <strong>服务器:</strong> {{ role.server || "未指定" }}
          </div>
          <n-button type="error" size="small" @click="removeRole(index)">
            删除
          </n-button>
        </div>
      </a-list-item>
    </a-list>

    <div class="form-actions">
      <n-button
        type="primary"
        size="large"
        block
        :loading="isImporting"
        :disabled="roleList.length === 0"
        @click="handleImport"
      >
        <template #icon>
          <n-icon>
            <CloudUpload />
          </n-icon>
        </template>
        添加Token
      </n-button>

      <n-button block @click="$emit('cancel')" :disabled="isSendingCode || isVerifying">
        <template #icon>
          <n-icon>
            <Close />
          </n-icon>
        </template>
        取消
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from "vue";
import { Close, CloudUpload } from "@vicons/ionicons5";
import { NButton, NForm, NFormItem, NIcon, NInput, useMessage } from "naive-ui";
import { useTokenStore } from "@/stores/tokenStore";
import {
  buildAuthUrlCandidates,
  extractDouyinRoles,
  requestJsonWithFallback,
  type DouyinImportRole,
} from "./douyinAuth";

const emit = defineEmits(["cancel", "ok"]);
const message = useMessage();
const tokenStore = useTokenStore();

const phoneForm = reactive({
  countryCode: "+86",
  phone: "",
  smsCode: "",
  nameTemplate: "{name}-{id}",
});

const statusMessage = ref("请先发送验证码");
const statusType = ref("info");
const sendCodeCooldown = ref(0);
const isSendingCode = ref(false);
const isVerifying = ref(false);
const isImporting = ref(false);
const roleList = ref<DouyinImportRole[]>([]);

const sessionId = ref("");
const requestId = ref("");

let cooldownTimer: number | null = null;

const updateStatus = (messageText: string, type = "info") => {
  statusMessage.value = messageText;
  statusType.value = type;
};

const stopCooldown = () => {
  if (cooldownTimer !== null) {
    window.clearInterval(cooldownTimer);
    cooldownTimer = null;
  }
};

const startCooldown = (seconds: number) => {
  stopCooldown();
  sendCodeCooldown.value = seconds;
  cooldownTimer = window.setInterval(() => {
    sendCodeCooldown.value -= 1;
    if (sendCodeCooldown.value <= 0) {
      sendCodeCooldown.value = 0;
      stopCooldown();
    }
  }, 1000);
};

const validatePhone = () => {
  const plainPhone = phoneForm.phone.trim();
  return /^[0-9]{5,20}$/.test(plainPhone);
};

const mergeRoles = (incomingRoles: DouyinImportRole[]) => {
  for (const incomingRole of incomingRoles) {
    const existingIndex = roleList.value.findIndex(
      (role) => role.id === incomingRole.id,
    );
    if (existingIndex !== -1) {
      roleList.value[existingIndex] = incomingRole;
    } else {
      roleList.value.push(incomingRole);
    }
  }
};

const sendSmsCode = async () => {
  if (!validatePhone()) {
    message.error("请输入正确的手机号");
    return;
  }

  try {
    isSendingCode.value = true;
    updateStatus("正在发送验证码...", "info");

    const payload = await requestJsonWithFallback(
      buildAuthUrlCandidates([
        "/phone/send-code",
        "/phone/sendCode",
        "/sms/send-code",
        "/sms/sendCode",
        "/mobile/send-code",
        "/mobile/sendCode",
      ]),
      {
        method: "POST",
        body: JSON.stringify({
          countryCode: phoneForm.countryCode.trim() || "+86",
          phone: phoneForm.phone.trim(),
        }),
      },
    );
    const data = (payload as any)?.data ?? payload ?? {};

    sessionId.value = data.sessionId || data.session_id || sessionId.value;
    requestId.value = data.requestId || data.request_id || requestId.value;

    const cooldownSeconds = Number(data.cooldown || data.cooldownSeconds || 60);
    startCooldown(Number.isNaN(cooldownSeconds) ? 60 : cooldownSeconds);

    updateStatus("验证码已发送，请注意查收", "success");
    message.success("验证码发送成功");
  } catch (error: any) {
    console.error("发送手机号验证码失败:", error);
    updateStatus(`验证码发送失败: ${error.message || error}`, "error");
    message.error(error.message || "验证码发送失败");
  } finally {
    isSendingCode.value = false;
  }
};

const verifyCodeAndFetchToken = async () => {
  if (!validatePhone()) {
    message.error("请输入正确的手机号");
    return;
  }
  if (!phoneForm.smsCode.trim()) {
    message.error("请输入验证码");
    return;
  }

  try {
    isVerifying.value = true;
    updateStatus("正在验证验证码并拉取Token...", "info");

    const payload = await requestJsonWithFallback(
      buildAuthUrlCandidates([
        "/phone/verify",
        "/phone/login",
        "/sms/verify",
        "/sms/login",
        "/mobile/verify",
        "/mobile/login",
      ]),
      {
        method: "POST",
        body: JSON.stringify({
          countryCode: phoneForm.countryCode.trim() || "+86",
          phone: phoneForm.phone.trim(),
          smsCode: phoneForm.smsCode.trim(),
          code: phoneForm.smsCode.trim(),
          sessionId: sessionId.value || undefined,
          requestId: requestId.value || undefined,
        }),
      },
    );

    const roles = extractDouyinRoles(payload, {
      importMethod: "dyPhone",
      nameTemplate: phoneForm.nameTemplate,
      defaultNamePrefix: "手机号角色",
    });

    if (roles.length === 0) {
      throw new Error("未获取到可导入Token，请检查验证码或后端返回");
    }

    mergeRoles(roles);
    updateStatus(`验证成功，已获取 ${roles.length} 个角色Token`, "success");
    message.success(`已获取 ${roles.length} 个角色Token`);
  } catch (error: any) {
    console.error("手机号验证码登录失败:", error);
    updateStatus(`验证失败: ${error.message || error}`, "error");
    message.error(error.message || "验证失败");
  } finally {
    isVerifying.value = false;
  }
};

const removeRole = (index: number) => {
  roleList.value.splice(index, 1);
};

const handleImport = () => {
  if (roleList.value.length === 0) {
    message.error("请先完成验证码登录并获取Token");
    return;
  }

  isImporting.value = true;
  try {
    for (const role of roleList.value) {
      const existingToken = tokenStore.gameTokens.find((token) => token.id === role.id);
      if (existingToken) {
        tokenStore.updateToken(role.id, {
          ...role,
        });
      } else {
        tokenStore.addToken({
          ...role,
        });
      }
    }

    message.success(`Token添加成功，共导入 ${roleList.value.length} 个角色`);
    roleList.value = [];
    emit("ok");
  } catch (error: any) {
    console.error("手机号方式导入Token失败:", error);
    message.error(error.message || "导入失败");
  } finally {
    isImporting.value = false;
  }
};

onUnmounted(() => {
  stopCooldown();
});
</script>

<style scoped lang="scss">
.douyin-phone-import {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg) 0;
}

.login-flow-info {
  background: var(--bg-tertiary);
  border-radius: var(--border-radius-medium);
  padding: var(--spacing-md);

  h3 {
    margin: 0 0 var(--spacing-sm) 0;
    color: var(--text-primary);
    font-size: var(--font-size-md);
  }

  .flow-steps {
    margin: 0;
    padding-left: var(--spacing-lg);
    color: var(--text-secondary);

    li {
      margin-bottom: var(--spacing-xs);
      font-size: var(--font-size-sm);
    }
  }
}

.phone-row,
.sms-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.country-code-input {
  max-width: 90px;
}

.phone-input {
  flex: 1;
}

.sms-row :deep(.n-input) {
  flex: 1;
}

.status-line {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-small);

  &.info {
    color: var(--text-secondary);
    background: var(--bg-tertiary);
  }

  &.success {
    color: var(--success-color);
    background: rgba(16, 185, 129, 0.1);
  }

  &.error {
    color: var(--error-color);
    background: rgba(239, 68, 68, 0.1);
  }
}

.role-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: var(--spacing-md);
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

@media (max-width: 768px) {
  .phone-row,
  .sms-row {
    flex-direction: column;
    align-items: stretch;
  }

  .country-code-input {
    max-width: none;
  }
}
</style>
