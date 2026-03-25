<template>
  <div
    class="game-status-container"
    :class="{
      'full-grid': activeSection === 'fightPvp',
      'full-page-mode': activeSection === 'saltFieldGroup' || activeSection === 'peachGroup' || activeSection === 'rankGroup',
      'club-mode': activeSection === 'club',
      'daily-mode': activeSection === 'daily'
    }"
  >
    <!-- 身份牌常驻（嵌入式，Tabs 上方） -->
    <IdentityCard embedded />

    <!-- 下方选卡分区切换（Tabs）：日常｜俱乐部｜活动 -->
    <n-tabs
      class="section-tabs"
      v-model:value="activeSection"
      type="line"
      animated
      size="small"
    >
      <n-tab-pane name="daily" tab="日常" />
      <n-tab-pane name="club" tab="俱乐部" />
      <n-tab-pane name="activity" tab="活动" />
      <n-tab-pane v-if="ENABLE_TOOLS_TAB" name="tools" tab="工具" />
      <n-tab-pane name="saltFieldGroup" tab="盐场" />
      <n-tab-pane name="peachGroup" tab="蟠桃园" />
      <n-tab-pane name="rankGroup" tab="排行榜" />
      <n-tab-pane name="fightPvp" tab="切磋" />
    </n-tabs>

    <!-- 阵容（仅日常） -->
    <TeamFormation v-show="activeSection === 'daily'" />

    <!-- 每日任务状态（仅日常） -->
    <DailyTaskStatus v-show="activeSection === 'daily'" />

    <!-- 咸将塔状态 -->
    <TowerStatus v-show="activeSection === 'daily' && isShowTowerStatus" />

    <!-- 怪异塔状态 -->
    <WeirdTowerStatus v-show="activeSection === 'daily'" />

    <!-- 自动化中心（挂机+机器人） -->
    <AutomationCenterCard v-show="activeSection === 'daily'" />

    <!-- 连接信息弱化条 -->

    <!-- 宝箱助手（提取组件） -->
    <BoxHelperCard v-show="activeSection === 'tools'" />

    <!-- 钓鱼助手（提取组件） -->
    <FishHelperCard v-show="activeSection === 'tools'" />

    <!-- 招募助手（提取组件） -->
    <RecruitHelperCard v-show="activeSection === 'tools'" />

    <!-- 升星助手（提取组件） -->
    <StarUpgradeCard v-if="activeSection === 'tools'" />

    <!-- 竞技场助手（提取组件） -->
    <FightHelperCard v-if="activeSection === 'tools'" />

    <!-- 梦境助手（提取组件） -->
    <DreamHelperCard v-if="activeSection === 'tools'" />

    <!-- 武将升级助手（提取组件） -->
    <HeroUpgradeCard v-if="activeSection === 'tools'" />


    <!-- 洗练助手（提取组件） -->
    <RefineHelperCard v-if="activeSection === 'tools'" />

    <!-- 消耗活动进度（提取组件） -->
    <ConsumptionProgressCard v-if="activeSection === 'tools'" />
    <!-- 咸王宝库（提取组件） -->
    <BossTower v-if="activeSection === 'tools'" />
    <!-- 俱乐部排位（暂时隐藏） -->
    <div
      class="status-card legion-match"
      v-if="ENABLE_LEGION_MATCH && activeSection === 'club'"
    >
      <div class="card-header">
        <img
          src="/icons/1733492491706152.png"
          alt="俱乐部图标"
          class="status-icon"
        />
        <div class="status-info">
          <h3>俱乐部排位</h3>
          <p>赛事状态</p>
        </div>
        <div class="status-badge" :class="{ active: legionMatch.isRegistered }">
          <div class="status-dot" />
          <span>{{ legionMatch.isRegistered ? "已报名" : "未报名" }}</span>
        </div>
      </div>
      <div class="card-content">
        <p class="description">
          每逢周三周四周五有比赛<br />
          立即报名参与精彩对决！
        </p>
        <button
          class="action-button"
          :disabled="legionMatch.isRegistered"
          @click="registerLegionMatch"
        >
          {{ legionMatch.isRegistered ? "已报名" : "立即报名" }}
        </button>
      </div>
    </div>

    <!-- 俱乐部赛车（合并自俱乐部赛车 + 疯狂赛车） -->

    <!-- 俱乐部签到（已迁移到俱乐部信息-概览，故隐藏原卡片） -->
    <div
      class="status-card legion-signin"
      v-if="ENABLE_LEGION_SIGNIN_CARD && activeSection === 'club'"
    >
      <div class="card-header">
        <img
          src="/icons/1733492491706148.png"
          alt="签到图标"
          class="status-icon"
        />
        <div class="status-info">
          <h3>俱乐部签到</h3>
          <p>每日签到状态</p>
        </div>
        <div class="status-badge" :class="{ active: legionSignin.isSignedIn }">
          <div class="status-dot" />
          <span>{{ legionSignin.isSignedIn ? "已签到" : "待签到" }}</span>
        </div>
      </div>
      <div class="card-content">
        <p v-if="legionSignin.clubName" class="club-name">
          当前俱乐部<br />
          <strong>{{ legionSignin.clubName }}</strong>
        </p>
        <p v-else class="description">尚未加入任何俱乐部</p>
        <div class="action-row">
          <button
            class="action-button"
            :disabled="legionSignin.isSignedIn"
            @click="signInLegion"
          >
            {{ legionSignin.isSignedIn ? "已签到" : "立即签到" }}
          </button>
        </div>
      </div>
    </div>

    <!-- 俱乐部信息与疯狂赛车（同级卡片，仅俱乐部分区） -->
    <ClubInfo v-if="activeSection === 'club'" />
    <ClubCarKing v-if="activeSection === 'club'" />

    <!-- 月度任务进度（提取组件） -->
    <MonthlyTasksCard v-show="activeSection === 'activity'" />

    <!-- 咸鱼大冲关（提取组件） -->
    <StudyChallengeCard v-show="activeSection === 'activity'" />

    <!-- 换皮闯关 -->
    <SkinChallengeCard v-show="activeSection === 'activity'" />

    <!-- 盐场分组（包含盐场、周战绩、月战绩） -->
    <div class="salt-field-group" v-if="activeSection === 'saltFieldGroup'">
      <div class="sub-nav">
        <n-tabs type="segment" animated v-model:value="saltFieldSubTab" size="small">
           <n-tab-pane name="warrank" tab="盐场" />
           <n-tab-pane name="weekBattle" tab="本周盐场战绩" />
           <n-tab-pane name="monthBattle" tab="本月盐场战绩" />
           <n-tab-pane name="legionWarMap" tab="盐场地图" />
           <n-tab-pane name="legionWarStatistics" tab="盐场战况" />
        </n-tabs>
      </div>

      <div class="warrank-full-container" v-if="saltFieldSubTab === 'weekBattle'">
        <ClubBattleRecords />
      </div>

      <div class="warrank-full-container" v-if="saltFieldSubTab === 'warrank'">
        <ClubWarrank />
      </div>

      <div class="warrank-full-container" v-if="saltFieldSubTab === 'monthBattle'">
        <ClubMonthBattleRecords />
      </div>

      <div class="warrank-full-container" v-if="saltFieldSubTab === 'legionWarMap'">
        <LegionWarMap />
      </div>
      <div class="warrank-full-container" v-if="saltFieldSubTab === 'legionWarStatistics'">
        <LegionWarStatistics />
      </div>
    </div>

    <!-- 蟠桃园分组 -->
    <div class="peach-group" v-if="activeSection === 'peachGroup'">
      <div class="sub-nav">
        <n-tabs type="segment" animated v-model:value="peachSubTab" size="small">
           <n-tab-pane name="peach" tab="蟠桃园信息" />
           <n-tab-pane name="peachBattle" tab="蟠桃园战绩" />
        </n-tabs>
      </div>

      <div class="warrank-full-container" v-if="peachSubTab === 'peachBattle'">
        <PeachBattleRecords />
      </div>

      <div class="warrank-full-container" v-if="peachSubTab === 'peach'">
        <PeachInfo />
      </div>
    </div>

    <!-- 排行榜分组 -->
    <div class="rank-group" v-if="activeSection === 'rankGroup'">
      <div class="sub-nav">
        <n-tabs type="segment" animated v-model:value="rankSubTab" size="small">
           <n-tab-pane name="serverrank" tab="区服榜" />
           <n-tab-pane name="toprank" tab="巅峰榜" />
           <n-tab-pane name="topclubrank" tab="俱乐部榜" />
           <n-tab-pane name="goldclubrank" tab="黄金积分榜" />
           <n-tab-pane name="greatRouteRank" tab="伟大航路积分榜" />
        </n-tabs>
      </div>

      <div class="warrank-full-container" v-if="rankSubTab === 'serverrank'">
        <ServerRankList />
      </div>

      <div class="warrank-full-container" v-if="rankSubTab === 'toprank'">
        <TopRankList />
      </div>

      <div class="warrank-full-container" v-if="rankSubTab === 'topclubrank'">
        <TopClubList />
      </div>

      <div class="warrank-full-container" v-if="rankSubTab === 'goldclubrank'">
        <GoldClubList />
      </div>

      <div class="warrank-full-container" v-if="rankSubTab === 'greatRouteRank'">
        <GreatRouteRankList />
      </div>
    </div>
    <!-- 切磋（提取组件） -->
    <FightPvp v-if="activeSection === 'fightPvp'" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { useMessage } from "naive-ui";
import {
  preloadQuestions,
  getQuestionCount,
} from "@/utils/studyQuestionsFromJSON.js";
import BoxHelperCard from "./cards/BoxHelperCard.vue";
import FishHelperCard from "./cards/FishHelperCard.vue";
import RecruitHelperCard from "./cards/RecruitHelperCard.vue";
import StarUpgradeCard from "./cards/StarUpgradeCard.vue";
import AutomationCenterCard from "./cards/AutomationCenterCard.vue";
import MonthlyTasksCard from "./cards/MonthlyTasksCard.vue";
import StudyChallengeCard from "./cards/StudyChallengeCard.vue";
import SkinChallengeCard from "./cards/SkinChallengeCard.vue";
import ClubWarrank from "./Club/ClubWarrank.vue";
import ClubMonthBattleRecords from "./Club/ClubMonthBattleRecords.vue";
import ClubBattleRecords from "./Club/ClubBattleRecords.vue";
import PeachBattleRecords from "./Club/PeachBattleRecords.vue";
import TopRankList from "./cards/TopRankListPageCard.vue";
import TopClubList from "./cards/TopClubListPageCard.vue";
import GreatRouteRankList from "./Club/GreatRouteRankListPageCard.vue";
import GoldClubList from "./cards/GoldRankListPageCard.vue";
import FightPvp from "./cards/FightPvp.vue";
import FightHelperCard from "./cards/FightHelperCard.vue";
import DreamHelperCard from "./cards/DreamHelperCard.vue";
import HeroUpgradeCard from "./cards/HeroUpgradeCard.vue";
import ConsumptionProgressCard from "./cards/ConsumptionProgressCard.vue";
import RefineHelperCard from "./cards/RefineHelperCard.vue";
import TowerStatus from "./Tower/TowerStatus.vue";
import WeirdTowerStatus from "./Tower/WeirdTowerStatus.vue";
import BossTower from "./Tower/BossTower.vue";
import PeachInfo from "./Club/PeachInfo.vue";
import ServerRankList from "./cards/ServerRankListPageCard.vue";
import LegionWarMap from "./Club/LegionWarMap.vue";
import LegionWarStatistics from "./Club/LegionWarStatistics.vue";

const tokenStore = useTokenStore();
const message = useMessage();

const legionMatch = ref({
  isRegistered: false,
});

// 响应式数据
const showIdentity = ref(false);
const activeSection = ref("daily");
const saltFieldSubTab = ref("warrank");
const peachSubTab = ref("peach");
const rankSubTab = ref("serverrank");

// 活动开放时间：仅周一到周三可参与
const isActivityOpen = computed(() => {
  const day = new Date().getDay(); // 0=周日,1=周一,...,6=周六
  return day >= 1 && day <= 3;
});

const bottleHelper = ref({
  isRunning: false,
  remainingTime: 0,
  stopTime: 0,
});

const hangUp = ref({
  isActive: false,
  remainingTime: 0,
  elapsedTime: 0,
  lastTime: 0,
  hangUpTime: 0,
  isExtending: false, // 加钟状态
  isClaiming: false, // 领取奖励状态
});

const legionSignin = ref({
  isSignedIn: false,
  clubName: "",
});

// 使用 tokenStore 中的答题状态（仍用于 badge 状态等场景，如果仅在子组件中使用也可移除）
const study = computed(() => tokenStore.gameData.studyStatus);

// 计算属性
const roleInfo = computed(() => {
  return tokenStore.gameData?.roleInfo || null;
});
const isShowTowerStatus = computed(() => {
  const tower = roleInfo.value?.role?.tower;
  const towerId = tower?.id;
  const floor = Math.floor(towerId / 10) + 1;
  if (floor > 450) {
    return false;
  }
  return true;
});

// WebSocket连接状态
const isConnected = computed(() => {
  if (!tokenStore.selectedToken) return false;
  const status = tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
  return status === "connected";
});

// 格式化时间 - 确保显示到秒
const formatTime = (seconds) => {
  // 确保传入值为数字，并向下取整到秒
  const totalSeconds = Math.floor(Number(seconds) || 0);

  if (totalSeconds <= 0) return "00:00:00";

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;

  return `${hours.toString().padStart(2, "0")}:${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
};

// 更新数据
const updateGameStatus = () => {
  if (!roleInfo.value) return;

  const role = roleInfo.value.role;

  // 更新盐罐机器人状态
  if (role.bottleHelpers) {
    const now = Date.now() / 1000;
    bottleHelper.value.stopTime = role.bottleHelpers.helperStopTime;
    bottleHelper.value.isRunning = role.bottleHelpers.helperStopTime > now;
    // 确保剩余时间为整数秒
    bottleHelper.value.remainingTime = Math.max(
      0,
      Math.floor(role.bottleHelpers.helperStopTime - now),
    );
    // 控制台精简，避免频繁刷屏
  }

  // 更新挂机状态
  if (role.hangUp) {
    const now = Date.now() / 1000;
    hangUp.value.lastTime = role.hangUp.lastTime;
    hangUp.value.hangUpTime = role.hangUp.hangUpTime;

    const elapsed = now - hangUp.value.lastTime;
    if (elapsed <= hangUp.value.hangUpTime) {
      // 确保剩余时间为整数秒
      hangUp.value.remainingTime = Math.floor(
        hangUp.value.hangUpTime - elapsed,
      );
      hangUp.value.isActive = true;
    } else {
      hangUp.value.remainingTime = 0;
      hangUp.value.isActive = false;
    }
    // 确保已挂机时间为整数秒
    hangUp.value.elapsedTime = Math.floor(
      hangUp.value.hangUpTime - hangUp.value.remainingTime,
    );
    // 控制台精简
  }

  // 更新俱乐部排位状态
  if (role.statistics) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayTimestamp = today.getTime() / 1000;

    legionMatch.value.isRegistered =
      Number(role.statistics["last:legion:match:sign:up:time"]) >
      todayTimestamp;
  }

  // 更新俱乐部签到状态
  if (role.statisticsTime) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayTimestamp = today.getTime() / 1000;

    legionSignin.value.isSignedIn =
      role.statisticsTime["legion:sign:in"] > todayTimestamp;
  }
};

// 定时器更新
let timer = null;
const startTimer = () => {
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    // 更新盐罐机器人剩余时间
    if (bottleHelper.value.isRunning && bottleHelper.value.remainingTime > 0) {
      bottleHelper.value.remainingTime = Math.max(
        0,
        bottleHelper.value.remainingTime - 1,
      );
      if (bottleHelper.value.remainingTime <= 0) {
        bottleHelper.value.isRunning = false;
      }
    }

    // 更新挂机剩余时间
    if (hangUp.value.isActive && hangUp.value.remainingTime > 0) {
      hangUp.value.remainingTime = Math.max(0, hangUp.value.remainingTime - 1);
      hangUp.value.elapsedTime = hangUp.value.elapsedTime + 1;
      if (hangUp.value.remainingTime <= 0) {
        hangUp.value.isActive = false;
      }
    }
  }, 1000);
};

// 盐罐机器人操作
const handleBottleHelper = () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }

  const tokenId = tokenStore.selectedToken.id;

  // 停止后重启
  tokenStore.sendMessage(tokenId, "bottlehelper_stop");
  setTimeout(() => {
    tokenStore.sendMessage(tokenId, "bottlehelper_start");
    tokenStore.sendMessage(tokenId, "role_getroleinfo");
  }, 500);

  message.info(
    bottleHelper.value.isRunning ? "重启盐罐机器人" : "启动盐罐机器人",
  );
};

// 挂机操作 - 参考HangUpStatus逻辑优化
const extendHangUp = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }

  const tokenId = tokenStore.selectedToken.id;

  try {
    // 降噪
    hangUp.value.isExtending = true;
    message.info("正在加钟...");

    // 按照参考代码的逻辑，发送4次分享回调请求
    const promises = [];
    for (let i = 0; i < 4; i++) {
      const promise = new Promise((resolve) => {
        setTimeout(() => {
          // 降噪
          const result = tokenStore.sendMessage(
            tokenId,
            "system_mysharecallback",
            {
              isSkipShareCard: true,
              type: 2,
            },
          );
          resolve(result);
        }, i * 300); // 增加间隔时间确保稳定性
      });
      promises.push(promise);
    }

    // 等待所有请求完成
    await Promise.all(promises);

    // 降噪

    // 延迟获取最新角色信息
    setTimeout(() => {
      // 降噪
      tokenStore.sendMessage(tokenId, "role_getroleinfo");
    }, 1500);

    // 延迟显示完成消息和重置状态
    setTimeout(() => {
      message.success("加钟操作已完成，请查看挂机剩余时间");
      hangUp.value.isExtending = false;
    }, 2500);
  } catch (error) {
    console.error("🕐 加钟操作失败:", error);
    message.error("加钟操作失败: " + (error.message || "未知错误"));
    hangUp.value.isExtending = false;
  }
};

const claimHangUpReward = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择Token");
    return;
  }

  const tokenId = tokenStore.selectedToken.id;

  try {
    // 降噪
    hangUp.value.isClaiming = true;
    message.info("正在领取挂机奖励...");

    // 参考HangUpStatus的S函数逻辑
    // 1. 发送初始分享回调
    tokenStore.sendMessage(tokenId, "system_mysharecallback");

    // 2. 领取挂机奖励
    setTimeout(() => {
      tokenStore.sendMessage(tokenId, "system_claimhangupreward");
    }, 200);

    // 3. 发送跳过分享卡片的回调
    setTimeout(() => {
      tokenStore.sendMessage(tokenId, "system_mysharecallback", {
        isSkipShareCard: true,
        type: 2,
      });
    }, 400);

    // 4. 获取最新角色信息
    setTimeout(() => {
      tokenStore.sendMessage(tokenId, "role_getroleinfo");
    }, 600);

    // 5. 显示完成消息并重置状态
    setTimeout(() => {
      message.success("挂机奖励领取完成");
      hangUp.value.isClaiming = false;
    }, 1200);

    // 降噪
  } catch (error) {
    console.error("🎁 领取挂机奖励失败:", error);
    message.error("领取挂机奖励失败: " + (error.message || "未知错误"));
    hangUp.value.isClaiming = false;
  }
};

// 功能开关：暂时隐藏俱乐部排位与旧签到卡片
const ENABLE_LEGION_MATCH = false;
const ENABLE_LEGION_SIGNIN_CARD = false;
const ENABLE_TOOLS_TAB = true; // 工具分区开关

// 盐场战绩入口已移动至俱乐部信息模块

// 学习答题逻辑已移动到 StudyChallengeCard 组件

// 监听角色信息变化
watch(
  roleInfo,
  (newValue) => {
    if (newValue) {
      updateGameStatus();
    }
  },
  { deep: true, immediate: true },
);

// 监听 WebSocket 连接状态（俱乐部信息）
const hasFetchedLegionOnce = ref(false);
watch(
  () =>
    tokenStore.selectedToken
      ? tokenStore.getWebSocketStatus(tokenStore.selectedToken.id)
      : "disconnected",
  (status) => {
    if (status === "connected") {
      if (!hasFetchedLegionOnce.value && tokenStore.selectedToken) {
        hasFetchedLegionOnce.value = true;
        const tokenId = tokenStore.selectedToken.id;
        tokenStore.sendMessage(tokenId, "legion_getinfo");
      }
    }
  },
);

// 战绩加载逻辑现由俱乐部信息模块负责

// 生命周期
onMounted(() => {
  updateGameStatus();
  startTimer();
  // 获取俱乐部信息
  if (
    tokenStore.selectedToken &&
    tokenStore.getWebSocketStatus(tokenStore.selectedToken.id) === "connected"
  ) {
    const tokenId = tokenStore.selectedToken.id;
    tokenStore.sendMessage(tokenId, "legion_getinfo");
    hasFetchedLegionOnce.value = true;
  }
});

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped lang="scss">
.game-status-container {
  // 固定游戏状态区的视觉变量，避免切换全局明暗主题时出现白底白字
  --text-primary: #f8fbff;
  --text-secondary: rgba(226, 232, 240, 0.84);
  --text-tertiary: rgba(148, 163, 184, 0.9);
  --bg-primary: rgba(15, 23, 42, 0.56);
  --bg-secondary: rgba(30, 41, 59, 0.44);
  --bg-tertiary: rgba(100, 116, 139, 0.32);
  --border-light: rgba(255, 255, 255, 0.18);
  --border-medium: rgba(255, 255, 255, 0.28);
  --border-dark: rgba(255, 255, 255, 0.36);
  --section-tab-surface: rgba(255, 255, 255, 0.08);
  --section-tab-border: rgba(255, 255, 255, 0.2);
  --section-tab-text: rgba(255, 255, 255, 0.88);
  --section-tab-hover-bg: rgba(255, 255, 255, 0.14);
  --section-tab-hover-text: #f8fbff;
  --section-tab-active-bg: linear-gradient(135deg, #7c6cff 0%, #8b5cf6 100%);
  --section-tab-active-text: #ffffff;
  --section-tab-active-border: rgba(124, 108, 255, 0.72);
  --section-tab-active-shadow: 0 10px 18px rgba(67, 69, 173, 0.38);
  --section-tab-shadow: 0 10px 26px rgba(10, 13, 31, 0.22);
  --card-surface: linear-gradient(
    165deg,
    rgba(15, 23, 42, 0.78) 0%,
    rgba(30, 41, 59, 0.64) 100%
  );
  --card-border: rgba(255, 255, 255, 0.16);
  --card-border-strong: rgba(14, 165, 233, 0.42);
  --card-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
  --card-hover-shadow: 0 20px 38px rgba(15, 23, 42, 0.18);
  --card-chip-bg: rgba(51, 65, 85, 0.38);
  --card-chip-border: rgba(255, 255, 255, 0.16);

  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);

  // 在大屏幕上限制最大列数以确保卡片有足够宽度
  @media (min-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
    max-width: 1400px;
    margin: 0 auto;
  }

  // 在中等屏幕上确保有足够空间
  @media (max-width: 1200px) {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }

  // 在较小屏幕上使用单列布局
  @media (max-width: 900px) {
    grid-template-columns: 1fr;
    gap: var(--spacing-md);
  }

  @media (max-width: 768px) {
    grid-template-columns: minmax(0, 1fr);
    padding: var(--spacing-sm);
    gap: var(--spacing-md);
  }
}

.game-status-container :deep(.status-card),
.game-status-container :deep(.team-status-card),
.game-status-container :deep(.team-formation-card) {
  background: var(--card-surface);
  border: 1px solid var(--card-border);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  box-shadow: var(--card-shadow);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease,
    background-color 0.22s ease;
}

.game-status-container :deep(.status-card:hover),
.game-status-container :deep(.team-status-card:hover),
.game-status-container :deep(.team-formation-card:hover) {
  transform: translateY(-2px);
  border-color: var(--card-border-strong);
  box-shadow: var(--card-hover-shadow);
}

.game-status-container :deep(.identity-card.embedded) {
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease;
}

.game-status-container :deep(.identity-card.embedded:hover) {
  transform: translateY(-2px);
}

.game-status-container :deep(.identity-card.embedded .res-item.detail-item) {
  background: var(--card-chip-bg);
  border: 1px solid var(--card-chip-border);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.game-status-container.daily-mode :deep(.status-card),
.game-status-container.daily-mode :deep(.team-status-card),
.game-status-container.daily-mode :deep(.team-formation-card) {
  background:
    linear-gradient(145deg, rgba(93, 105, 193, 0.06), rgba(27, 34, 66, 0.18)),
    radial-gradient(circle at 18% 14%, rgba(124, 108, 255, 0.03), transparent 28%),
    radial-gradient(circle at 84% 10%, rgba(96, 165, 250, 0.03), transparent 26%);
  background-blend-mode: screen, normal, screen;
  background-position: 0 0, 18% 16%, 84% 10%;
  background-size: 100% 100%, 220% 220%, 220% 220%;
  backdrop-filter: blur(8px) saturate(128%);
  -webkit-backdrop-filter: blur(8px) saturate(128%);
  border-color: rgba(255, 255, 255, 0.05);
  filter: saturate(1);
  animation: dailyGlowFloat 10s ease-in-out infinite;
}

.game-status-container.daily-mode :deep(.status-card:hover),
.game-status-container.daily-mode :deep(.team-status-card:hover),
.game-status-container.daily-mode :deep(.team-formation-card:hover) {
  transform: translateY(-3px) scale(1.004);
  border-color: rgba(168, 180, 255, 0.42);
  box-shadow:
    0 24px 50px rgba(10, 13, 31, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 0 34px rgba(124, 108, 255, 0.14);
}

.game-status-container.daily-mode :deep(.status-card:active),
.game-status-container.daily-mode :deep(.team-status-card:active),
.game-status-container.daily-mode :deep(.team-formation-card:active) {
  transform: translateY(-1px) scale(0.998);
}

.game-status-container.daily-mode :deep(.status-card:not(.team-formation-card) button),
.game-status-container.daily-mode :deep(.team-status-card button) {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  -webkit-tap-highlight-color: transparent;
}

.game-status-container.daily-mode :deep(.status-card:not(.team-formation-card) button::before),
.game-status-container.daily-mode :deep(.team-status-card button::before) {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(
    circle at center,
    rgba(255, 255, 255, 0.28) 0%,
    rgba(255, 255, 255, 0.14) 28%,
    transparent 68%
  );
  opacity: 0;
  transform: scale(0.7);
  transition:
    opacity 0.24s ease,
    transform 0.32s ease;
  pointer-events: none;
}

.game-status-container.daily-mode :deep(.status-card:not(.team-formation-card) button:hover::before),
.game-status-container.daily-mode :deep(.team-status-card button:hover::before) {
  opacity: 0.12;
  transform: scale(1);
}

.game-status-container.daily-mode :deep(.status-card:not(.team-formation-card) button:active::before),
.game-status-container.daily-mode :deep(.team-status-card button:active::before) {
  opacity: 0.24;
  transform: scale(1.08);
}

.full-grid {
  grid-template-columns: repeat(1, 1fr);
}

.game-status-container.full-page-mode {
  max-width: 100% !important;
  grid-template-columns: 1fr;
  padding: var(--spacing-sm);
  
  @media (min-width: 1400px) {
    max-width: 100% !important;
  }
}

.game-status-container.club-mode {
  --text-color: #f7fbff;
  --text-secondary: rgba(225, 237, 255, 0.84);
  --text-tertiary: rgba(176, 194, 226, 0.9);
  --bg-color: rgba(8, 15, 35, 0.72);
  --bg-primary: rgba(11, 21, 44, 0.68);
  --bg-secondary: rgba(16, 28, 54, 0.58);
  --bg-tertiary: rgba(28, 42, 72, 0.48);
  --border-color: rgba(124, 108, 255, 0.18);
  --border-light: rgba(124, 108, 255, 0.16);
  --border-medium: rgba(124, 108, 255, 0.24);
  --border-dark: rgba(124, 108, 255, 0.34);
  --primary-color: #7c6cff;
  --primary-color-hover: #8b5cf6;
  --primary-color-light: rgba(124, 108, 255, 0.16);
  --section-tab-surface: rgba(8, 15, 35, 0.58);
  --section-tab-border: rgba(124, 108, 255, 0.2);
  --section-tab-text: rgba(237, 246, 255, 0.9);
  --section-tab-hover-bg: rgba(124, 108, 255, 0.12);
  --section-tab-hover-text: #f8fbff;
  --section-tab-active-bg: linear-gradient(135deg, #7c6cff 0%, #8b5cf6 100%);
  --section-tab-active-text: #ffffff;
  --section-tab-active-border: rgba(124, 108, 255, 0.72);
  --section-tab-active-shadow: 0 10px 18px rgba(124, 108, 255, 0.38);
  --section-tab-shadow: 0 10px 26px rgba(8, 15, 35, 0.28);
  --card-surface:
    linear-gradient(165deg, rgba(7, 18, 39, 0.84) 0%, rgba(16, 31, 59, 0.68) 100%);
  --card-border: rgba(124, 108, 255, 0.14);
  --card-border-strong: rgba(124, 108, 255, 0.42);
  --card-shadow: 0 18px 34px rgba(8, 15, 35, 0.18);
  --card-hover-shadow: 0 22px 42px rgba(124, 108, 255, 0.24);
  --card-chip-bg: rgba(15, 23, 42, 0.42);
  --card-chip-border: rgba(124, 108, 255, 0.16);

  @media (min-width: 1400px) {
    grid-template-columns: repeat(2, 1fr);
    max-width: 100% !important;
  }

  :deep(.status-card),
  :deep(.team-status-card),
  :deep(.team-formation-card) {
    background:
      linear-gradient(145deg, rgba(8, 18, 40, 0.08), rgba(13, 25, 49, 0.2)),
      radial-gradient(circle at 18% 14%, rgba(124, 108, 255, 0.08), transparent 28%),
      radial-gradient(circle at 84% 10%, rgba(139, 92, 246, 0.08), transparent 26%);
    background-blend-mode: screen, normal, screen;
    background-position: 0 0, 18% 16%, 84% 10%;
    background-size: 100% 100%, 220% 220%, 220% 220%;
    backdrop-filter: blur(8px) saturate(128%);
    -webkit-backdrop-filter: blur(8px) saturate(128%);
    border-color: rgba(124, 108, 255, 0.08);
    filter: saturate(1);
    animation: dailyGlowFloat 10s ease-in-out infinite;
  }

  :deep(.status-card:hover),
  :deep(.team-status-card:hover),
  :deep(.team-formation-card:hover) {
    transform: translateY(-3px) scale(1.004);
    border-color: rgba(124, 108, 255, 0.38);
    box-shadow:
      0 24px 50px rgba(8, 15, 35, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.12),
      0 0 34px rgba(124, 108, 255, 0.16);
  }

  :deep(.status-card:active),
  :deep(.team-status-card:active),
  :deep(.team-formation-card:active) {
    transform: translateY(-1px) scale(0.998);
  }

  :deep(.status-card .status-badge),
  :deep(.status-card .card-action > button) {
    border: 1px solid rgba(124, 108, 255, 0.14);
    background: rgba(8, 15, 35, 0.34);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  }

  :deep(.status-card .status-badge.active),
  :deep(.status-card .card-action.active > button) {
    background: linear-gradient(135deg, #7c6cff 0%, #8b5cf6 100%);
    border-color: rgba(124, 108, 255, 0.4);
    color: #ffffff;
    box-shadow: 0 10px 18px rgba(124, 108, 255, 0.24);
  }

  :deep(.status-card:not(.team-formation-card) button),
  :deep(.team-status-card button) {
    position: relative;
    overflow: hidden;
    isolation: isolate;
    -webkit-tap-highlight-color: transparent;
  }

  :deep(.status-card:not(.team-formation-card) button::before),
  :deep(.team-status-card button::before) {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: radial-gradient(
      circle at center,
      rgba(255, 255, 255, 0.24) 0%,
      rgba(255, 255, 255, 0.12) 28%,
      transparent 68%
    );
    opacity: 0;
    transform: scale(0.7);
    transition:
      opacity 0.24s ease,
      transform 0.32s ease;
    pointer-events: none;
  }

  :deep(.status-card:not(.team-formation-card) button:hover::before),
  :deep(.team-status-card button:hover::before) {
    opacity: 0.14;
    transform: scale(1);
  }

  :deep(.status-card:not(.team-formation-card) button:active::before),
  :deep(.team-status-card button:active::before) {
    opacity: 0.26;
    transform: scale(1.08);
  }

  :deep(.status-card .card-action > button) {
    min-height: 40px;
    border-radius: 12px;
    color: var(--text-color);
    background: linear-gradient(135deg, rgba(8, 15, 35, 0.78), rgba(13, 25, 49, 0.86));
    border-color: rgba(124, 108, 255, 0.18);
    font-weight: 700;
    letter-spacing: 0.02em;
    box-shadow: 0 10px 20px rgba(8, 15, 35, 0.14);
  }

  :deep(.status-card .card-action > button:hover:not(:disabled)) {
    transform: translateY(-1px);
    filter: brightness(1.05);
    background: linear-gradient(135deg, rgba(124, 108, 255, 0.26), rgba(139, 92, 246, 0.22));
    border-color: rgba(124, 108, 255, 0.34);
    box-shadow: 0 12px 22px rgba(124, 108, 255, 0.18);
  }

  :deep(.status-card .card-action > button:disabled) {
    background: rgba(148, 163, 184, 0.24);
    border-color: rgba(148, 163, 184, 0.22);
    color: rgba(226, 232, 240, 0.56);
    box-shadow: none;
    transform: none;
  }

  :deep(.club-info .n-card),
  :deep(.club-car-king),
  :deep(.club-car-king .car-card-item) {
    background:
      linear-gradient(165deg, rgba(7, 18, 39, 0.84) 0%, rgba(16, 31, 59, 0.68) 100%);
    border: 1px solid rgba(124, 108, 255, 0.14);
    backdrop-filter: blur(8px) saturate(126%);
    -webkit-backdrop-filter: blur(8px) saturate(126%);
    box-shadow: 0 16px 32px rgba(8, 15, 35, 0.14);
  }

  :deep(.club-info .status-badge.active) {
    border-color: rgba(110, 231, 183, 0.48);
    background: rgba(52, 211, 153, 0.18);
    color: #6ee7b7;
    box-shadow: none;
  }

  :deep(.club-info .n-button),
  :deep(.club-car-king .n-button) {
    border-radius: 12px;
    border: 1px solid rgba(124, 108, 255, 0.18);
    color: var(--text-color);
    background: rgba(8, 15, 35, 0.34);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
    transition:
      transform 0.2s ease,
      filter 0.2s ease,
      box-shadow 0.2s ease,
      background-color 0.2s ease,
      border-color 0.2s ease;
  }

  :deep(.club-info .n-button:hover:not(.n-button--disabled)),
  :deep(.club-car-king .n-button:hover:not(.n-button--disabled)) {
    transform: translateY(-1px);
    filter: brightness(1.05);
    background: rgba(124, 108, 255, 0.16);
    border-color: rgba(124, 108, 255, 0.34);
    box-shadow: 0 12px 22px rgba(124, 108, 255, 0.16);
  }

  :deep(.club-info .n-button--primary-type),
  :deep(.club-car-king .n-button--primary-type) {
    background: linear-gradient(135deg, #7c6cff 0%, #8b5cf6 100%);
    border-color: rgba(124, 108, 255, 0.36);
    color: #ffffff;
    box-shadow: 0 10px 18px rgba(124, 108, 255, 0.22);
  }

  :deep(.club-info .n-button--secondary-type),
  :deep(.club-car-king .n-button--secondary-type) {
    background: rgba(8, 15, 35, 0.34);
    color: var(--text-color);
  }

  :deep(.club-car-king .car-brand-icon),
  :deep(.club-car-king .status-item),
  :deep(.club-car-king .reward-tag) {
    background: rgba(8, 15, 35, 0.34);
    border-color: rgba(124, 108, 255, 0.16);
  }

  :deep(.club-info-tabs .n-tabs-nav) {
    background: transparent !important;
  }

  :deep(.club-info-tabs .n-tabs-nav-scroll-wrapper),
  :deep(.club-info-tabs .n-tabs-nav-scroll-content) {
    background: transparent !important;
  }

  :deep(.club-info-tabs .n-tabs-bar) {
    display: none;
  }

  :deep(.club-info-tabs .n-tabs-tab) {
    border: 1px solid transparent;
    border-radius: 999px;
    min-height: 32px;
    padding-inline: 14px;
    color: var(--section-tab-text) !important;
    font-weight: 700;
    transition:
      color 0.2s ease,
      background-color 0.2s ease,
      transform 0.2s ease,
      box-shadow 0.2s ease,
      border-color 0.2s ease;
  }

  :deep(.club-info-tabs .n-tabs-tab:hover) {
    color: var(--section-tab-hover-text) !important;
    background: var(--section-tab-hover-bg);
    border-color: var(--section-tab-border);
    transform: translateY(-1px);
  }

  :deep(.club-info-tabs .n-tabs-tab.n-tabs-tab--active) {
    color: var(--section-tab-active-text) !important;
    background: var(--section-tab-active-bg);
    border-color: var(--section-tab-active-border);
    box-shadow: var(--section-tab-active-shadow);
    transform: translateY(0);
  }

  :deep(.club-info-tabs .n-tabs-tab .n-tabs-tab__label) {
    color: inherit !important;
  }
}

.section-header {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px var(--spacing-sm);
}

.identity-toggle {
  padding: 6px 12px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  background: var(--bg-primary);
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.section-tabs {
  margin: 0 var(--spacing-sm) var(--spacing-md);
  grid-column: 1 / -1;
  border: 1px solid var(--section-tab-border);
  border-radius: 999px;
  padding: 4px 6px;
  background: var(--section-tab-surface);
  backdrop-filter: blur(14px);
  box-shadow: var(--section-tab-shadow);
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.section-tabs::-webkit-scrollbar {
  display: none;
}

.section-tabs :deep(.n-tabs-pane-wrapper) {
  display: none;
}

.section-tabs :deep(.n-tabs-nav) {
  background: transparent !important;
  width: max-content;
  min-width: 100%;
}

.section-tabs :deep(.n-tabs-wrapper),
.section-tabs :deep(.n-tabs-nav-scroll-wrapper),
.section-tabs :deep(.n-tabs-nav-scroll-content) {
  background: transparent !important;
}

.section-tabs :deep(.n-tabs-nav-scroll-content) {
  display: inline-flex;
  white-space: nowrap;
  min-width: max-content;
}

.section-tabs :deep(.n-tabs-tab) {
  border: 1px solid rgba(255, 255, 255, 0);
  border-radius: 999px;
  min-height: 38px;
  padding-inline: 16px;
  color: var(--section-tab-text) !important;
  font-weight: 700;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.section-tabs :deep(.n-tabs-tab-wrapper) {
  border-radius: 999px;
  padding: 0;
  flex: 0 0 auto;
}

.section-tabs :deep(.n-tabs-tab:hover) {
  color: var(--section-tab-hover-text) !important;
  background: var(--section-tab-hover-bg);
  border-color: var(--section-tab-border);
  transform: translateY(-1px);
}

.section-tabs :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: var(--section-tab-active-text) !important;
  background: var(--section-tab-active-bg);
  border-color: var(--section-tab-active-border);
  box-shadow: var(--section-tab-active-shadow);
  transform: translateY(0);
}

.section-tabs :deep(.n-tabs-tab .n-tabs-tab__label) {
  color: inherit !important;
}

.section-tabs :deep(.n-tabs-bar) {
  display: none;
}

.sub-nav {
  padding: 8px;
  background: var(--section-tab-surface);
  border: 1px solid var(--section-tab-border);
  border-radius: 12px;
  backdrop-filter: blur(8px);
  display: block;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.sub-nav::-webkit-scrollbar {
  display: none;
}

.sub-nav :deep(.n-tabs) {
  display: block;
  width: max-content;
  min-width: 100%;
  flex: none;
}

.sub-nav :deep(.n-tabs-nav) {
  width: max-content;
  min-width: 100%;
  background: transparent;
  border-radius: var(--border-radius-sm);
}

.sub-nav :deep(.n-tabs--segment-type .n-tabs-rail) {
  display: inline-flex;
  width: max-content;
  min-width: max-content;
  flex-wrap: nowrap;
}

.sub-nav :deep(.n-tabs--segment-type .n-tabs-tab-wrapper) {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 2px;
}

.sub-nav :deep(.n-tabs--segment-type .n-tabs-tab) {
  width: auto;
  border-radius: 999px;
  min-height: 32px;
  padding-inline: 14px;
}

.sub-nav :deep(.v-x-scroll) {
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}

.sub-nav :deep(.n-tabs-nav-scroll-content) {
  display: inline-flex;
  white-space: nowrap;
  min-width: max-content;
}

.sub-nav :deep(.n-tabs-tab) {
  flex: 0 0 auto;
  white-space: nowrap;
  border: 1px solid transparent;
  border-radius: 999px;
  color: var(--section-tab-text) !important;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.sub-nav :deep(.n-tabs-tab:hover) {
  color: var(--section-tab-hover-text) !important;
  background: var(--section-tab-hover-bg);
  border-color: var(--section-tab-border);
}

.sub-nav :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: var(--section-tab-active-text) !important;
  background: var(--section-tab-active-bg);
  border-color: var(--section-tab-active-border);
  box-shadow: var(--section-tab-active-shadow);
}

@media (min-width: 769px) {
  .sub-nav {
    overflow-x: hidden;
  }

  .sub-nav :deep(.n-tabs),
  .sub-nav :deep(.n-tabs-nav),
  .sub-nav :deep(.v-x-scroll),
  .sub-nav :deep(.n-tabs-nav-scroll-wrapper),
  .sub-nav :deep(.n-tabs-nav-scroll-content),
  .sub-nav :deep(.n-tabs--segment-type .n-tabs-rail) {
    width: 100% !important;
    min-width: 0 !important;
  }

  .sub-nav :deep(.n-tabs-nav-scroll-content),
  .sub-nav :deep(.n-tabs--segment-type .n-tabs-rail) {
    display: flex !important;
  }

  .sub-nav :deep(.n-tabs--segment-type .n-tabs-tab-wrapper) {
    flex: 1 1 0 !important;
    min-width: 0 !important;
  }

  .sub-nav :deep(.n-tabs--segment-type .n-tabs-tab) {
    width: 100% !important;
    justify-content: center;
    white-space: nowrap;
  }
}

.warrank-full-container {
  grid-column: 1 / -1;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: calc(100vh - 200px);
  min-height: 600px;
  overflow: auto;
  -webkit-overflow-scrolling: touch;

  @media (max-width: 768px) {
    height: auto;
    min-height: 0;
    max-width: 100%;
    min-width: 0;
    overflow: auto;
  }
}

.warrank-full-container > * {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.salt-field-group,
.peach-group,
.rank-group {
  grid-column: 1 / -1;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.monthly-tasks .description.muted {
  color: var(--text-tertiary);
  margin-top: var(--spacing-sm);
}

.monthly-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-xs);
  font-size: var(--font-size-sm);
}

.status-dot {
  &.completed {
    background: var(--success-color);
    box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
  }
}

.energy-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.card-content {
  .time-display {
    font-size: 1.5rem;
    /* text-2xl */
    font-weight: 700;
    /* font-bold */
    color: var(--text-primary);
    text-align: center;
    margin-bottom: var(--spacing-md);
    font-family:
      "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", "Consolas", monospace;
    letter-spacing: 0.1em;
    text-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
    background: var(--bg-tertiary);
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    border: 1px solid var(--border-light);
    box-shadow:
      0 1px 3px rgba(0, 0, 0, 0.1),
      0 1px 2px rgba(0, 0, 0, 0.06);
    transition: all 0.2s ease-in-out;

    &:hover {
      transform: translateY(-1px);
      box-shadow:
        0 4px 6px rgba(0, 0, 0, 0.1),
        0 2px 4px rgba(0, 0, 0, 0.06);
    }
  }

  .description {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    line-height: 1.5;
    margin-bottom: var(--spacing-lg);
  }

  .club-name {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    margin-bottom: var(--spacing-lg);

    strong {
      color: var(--text-primary);
      font-weight: var(--font-weight-medium);
    }
  }

  .tower-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--spacing-lg);

    .label {
      color: var(--text-secondary);
      font-size: var(--font-size-sm);
    }

    .tower-level {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-bold);
      color: var(--text-primary);
    }
  }
}

.action-row {
  display: flex;
  gap: var(--spacing-sm);

  .action-button {
    flex: 1;
  }
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

.loading-icon {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@keyframes dailyGlowFloat {
  0%,
  100% {
    background-position: 0 0, 18% 16%, 84% 10%;
  }

  50% {
    background-position: 0 0, 24% 10%, 78% 16%;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .game-status-container {
    grid-template-columns: 1fr;
    padding: var(--spacing-sm);
  }

  .sub-nav :deep(.n-tabs-tab) {
    min-width: 96px;
    justify-content: center;
  }

  .status-card {
    padding: var(--spacing-md);
  }
  .card-header {
    flex-wrap: wrap;
    gap: var(--spacing-sm);

    .status-info {
      flex: 1;
      min-width: 120px;
    }

    .status-badge {
      margin-left: auto;
    }
  }
}
</style>
