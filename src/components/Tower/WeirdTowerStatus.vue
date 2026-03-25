<template>
  <div class="status-card tower-status weird-tower">
    <div class="card-header">
      <img src="/icons/1733492491706152.png" alt="怪异塔图标" class="status-icon" />
      <div class="status-info">
        <h3>怪异塔</h3>
        <p>章节推进与道具操作</p>
      </div>
      <div class="energy-display">
        <img src="/icons/xiaoyugan.png" alt="小鱼干" class="energy-icon" />
        <span class="energy-count">{{ towerEnergy }}</span>
      </div>
    </div>

    <div class="status-pill" :class="{ running: canRun }">
      {{ weirdStatusText }}
    </div>

    <div class="card-content">
      <div class="tower-floor">
        <span class="label">当前层数</span>
        <span class="floor-number">{{ displayFloor }}</span>
      </div>
      <p class="floor-sub">{{ floorSubText }}</p>
    </div>

    <div class="card-actions">
      <button
        :class="[
          'climb-button',
          {
            active: canClimb,
            disabled: !canClimb,
          },
        ]"
        :disabled="!canClimb"
        @click="startTowerClimb"
      >
        {{ isClimbing ? "爬塔中..." : "开始爬塔" }}
      </button>

      <button v-if="isClimbing" class="stop-button" @click="stopClimbing">
        停止爬塔
      </button>

      <div v-if="!isClimbing && !isUsingItems && !isMerging" class="weird-secondary-actions">
        <button class="sub-action-button" @click="startUseItems">
          一键使用道具
        </button>

        <button class="sub-action-button" @click="autoMergeItems">
          {{ isMerging ? "合成中..." : "一键合成" }}
        </button>
      </div>

      <button v-if="isUsingItems" class="stop-button" @click="stopUsingItems">
        停止使用
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useTokenStore } from "@/stores/tokenStore";
import { useMessage } from "naive-ui";

const tokenStore = useTokenStore();
const message = useMessage();

let stopFlag = false;
let stopItemFlag = false;
let stopMergeFlag = false;

const isClimbing = ref(false);
const isUsingItems = ref(false);
const isMerging = ref(false);
const climbTimeout = ref(null);
const itemTimeout = ref(null);
const mergeTimeout = ref(null);

const evoTowerInfo = computed(() => tokenStore.gameData?.evoTowerInfo || null);
const weirdTowerData = computed(() => evoTowerInfo.value?.evoTower || null);
const currentTowerId = computed(() => Number(weirdTowerData.value?.towerId || 0));

const displayFloor = computed(() => {
  if (currentTowerId.value === 0) return "1-1";
  const chapter = Math.floor(currentTowerId.value / 10) + 1;
  const floor = (currentTowerId.value % 10) + 1;
  return `${chapter}-${floor}`;
});

const floorSubText = computed(() => {
  const chapter = Math.floor(currentTowerId.value / 10) + 1;
  const floor = (currentTowerId.value % 10) + 1;
  return `第 ${chapter} 章 · 第 ${floor} 层`;
});

const towerEnergy = computed(() => Number(weirdTowerData.value?.energy || 0));

const getCurrentActivityWeek = computed(() => {
  const now = new Date();
  const start = new Date("2025-12-12T12:00:00");
  const weekDuration = 7 * 24 * 60 * 60 * 1000;
  const cycleDuration = 3 * weekDuration;
  const elapsed = now - start;
  if (elapsed < 0) return null;
  const cyclePosition = elapsed % cycleDuration;
  if (cyclePosition < weekDuration) return "黑市周";
  if (cyclePosition < 2 * weekDuration) return "招募周";
  return "宝箱周";
});

const isWeirdTowerActivityOpen = computed(() => getCurrentActivityWeek.value === "黑市周");

const canClimb = computed(() => {
  const hasEnergy = towerEnergy.value > 0;
  const notBusy = !isClimbing.value && !isUsingItems.value && !isMerging.value;
  return hasEnergy && notBusy && isWeirdTowerActivityOpen.value;
});

const canRun = computed(
  () => isClimbing.value || (isWeirdTowerActivityOpen.value && towerEnergy.value > 0),
);

const weirdStatusText = computed(() => {
  if (isClimbing.value) return "🟢 进行中";
  if (!isWeirdTowerActivityOpen.value) return "🔴 未开始";
  if (towerEnergy.value > 0) return "🟢 可挑战";
  return "🔴 待推进";
});

const stopClimbing = () => {
  stopFlag = true;
  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
  message.info("已手动停止批量爬塔");
};

const stopUsingItems = () => {
  stopItemFlag = true;
  if (itemTimeout.value) {
    clearTimeout(itemTimeout.value);
    itemTimeout.value = null;
  }
  isUsingItems.value = false;
  message.info("已停止使用道具");
};

const startUseItems = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  if (isClimbing.value || isMerging.value) {
    message.warning("正在执行其他操作，请稍后");
    return;
  }

  isUsingItems.value = true;
  stopItemFlag = false;

  itemTimeout.value = setTimeout(() => {
    isUsingItems.value = false;
    itemTimeout.value = null;
    stopItemFlag = true;
    message.info("使用道具超时，已自动停止");
  }, 60000);

  try {
    const tokenId = tokenStore.selectedToken.id;
    const infoRes = await tokenStore.sendMessageWithPromise(
      tokenId,
      "mergebox_getinfo",
      { actType: 1 },
      5000,
    );
    const towerInfoRes = await tokenStore.sendMessageWithPromise(
      tokenId,
      "evotower_getinfo",
      {},
      5000,
    );

    if (!infoRes?.mergeBox) throw new Error("活动信息读取失败");

    let costTotalCnt = Number(infoRes.mergeBox.costTotalCnt || 0);
    let lotteryLeftCnt = Number(towerInfoRes?.evoTower?.lotteryLeftCnt || 0);

    if (lotteryLeftCnt <= 0) {
      message.info("没有剩余道具可使用");
      return;
    }

    let processedCount = 0;

    while (lotteryLeftCnt > 0 && !stopItemFlag) {
      let pos = {};
      if (costTotalCnt < 2) {
        pos = { gridX: 4, gridY: 5 };
      } else if (costTotalCnt < 102) {
        pos = { gridX: 7, gridY: 3 };
      } else {
        pos = { gridX: 6, gridY: 3 };
      }

      await tokenStore.sendMessageWithPromise(
        tokenId,
        "mergebox_openbox",
        {
          actType: 1,
          pos,
        },
        5000,
      );

      costTotalCnt++;
      lotteryLeftCnt--;
      processedCount++;

      await new Promise((res) => setTimeout(res, 500));
    }

    await tokenStore
      .sendMessageWithPromise(
        tokenId,
        "mergebox_claimcostprogress",
        { actType: 1 },
        5000,
      )
      .catch(() => {});

    message.success(`已使用道具 ${processedCount} 次`);
    await getTowerInfo();
  } catch (error) {
    message.error("使用道具失败: " + (error?.message || "未知错误"));
  } finally {
    if (itemTimeout.value) {
      clearTimeout(itemTimeout.value);
      itemTimeout.value = null;
    }
    isUsingItems.value = false;
  }
};

const autoMergeItems = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  if (isClimbing.value || isUsingItems.value) {
    message.warning("正在执行其他操作，请稍后");
    return;
  }

  isMerging.value = true;
  stopMergeFlag = false;

  mergeTimeout.value = setTimeout(() => {
    isMerging.value = false;
    mergeTimeout.value = null;
    stopMergeFlag = true;
    message.info("一键合成超时，已自动停止");
  }, 60000);

  try {
    const tokenId = tokenStore.selectedToken.id;
    let loopCount = 0;
    const MAX_LOOPS = 20;

    while (loopCount < MAX_LOOPS && !stopMergeFlag) {
      loopCount++;
      const infoRes = await tokenStore.sendMessageWithPromise(
        tokenId,
        "mergebox_getinfo",
        { actType: 1 },
        5000,
      );

      if (!infoRes?.mergeBox) throw new Error("合成数据读取失败");

      if (infoRes.mergeBox.taskMap) {
        const taskMap = infoRes.mergeBox.taskMap;
        const taskClaimMap = infoRes.mergeBox.taskClaimMap || {};
        for (const taskId in taskMap) {
          if (stopMergeFlag) break;
          if (taskMap[taskId] !== 0 && !taskClaimMap[taskId]) {
            await tokenStore
              .sendMessageWithPromise(
                tokenId,
                "mergebox_claimmergeprogress",
                { actType: 1, taskId: Number(taskId) },
                2000,
              )
              .catch(() => {});
            await new Promise((res) => setTimeout(res, 500));
          }
        }
      }

      const gridMap = infoRes.mergeBox.gridMap || {};
      const items = [];

      for (const xStr in gridMap) {
        for (const yStr in gridMap[xStr]) {
          const item = gridMap[xStr][yStr];
          if (item.gridConfId === 0 && item.gridItemId > 0 && !item.isLock) {
            items.push({
              x: Number(xStr),
              y: Number(yStr),
              id: item.gridItemId,
            });
          }
        }
      }

      const groupedItems = {};
      items.forEach((item) => {
        if (!groupedItems[item.id]) groupedItems[item.id] = [];
        groupedItems[item.id].push(item);
      });

      let hasPotentialMerge = false;
      for (const id in groupedItems) {
        if (groupedItems[id].length >= 2) {
          hasPotentialMerge = true;
          break;
        }
      }

      if (!hasPotentialMerge) break;

      const isLevel8OrAbove =
        infoRes.mergeBox.taskMap &&
        infoRes.mergeBox.taskMap["251212208"] &&
        infoRes.mergeBox.taskMap["251212208"] !== 0;

      if (isLevel8OrAbove) {
        await tokenStore.sendMessageWithPromise(
          tokenId,
          "mergebox_automergeitem",
          { actType: 1 },
          10000,
        );
        await new Promise((res) => setTimeout(res, 1500));
      } else {
        for (const id in groupedItems) {
          if (stopMergeFlag) break;
          const group = groupedItems[id];
          while (group.length >= 2) {
            if (stopMergeFlag) break;
            const source = group.shift();
            const target = group.shift();
            await tokenStore
              .sendMessageWithPromise(
                tokenId,
                "mergebox_mergeitem",
                {
                  actType: 1,
                  sourcePos: { gridX: source.x, gridY: source.y },
                  targetPos: { gridX: target.x, gridY: target.y },
                },
                1000,
              )
              .catch(() => {});
            await new Promise((res) => setTimeout(res, 300));
          }
        }
      }

      await new Promise((res) => setTimeout(res, 500));
    }

    message.success("一键合成完成");
    await getTowerInfo();
  } catch (error) {
    message.error("一键合成失败: " + (error?.message || "未知错误"));
  } finally {
    if (mergeTimeout.value) {
      clearTimeout(mergeTimeout.value);
      mergeTimeout.value = null;
    }
    isMerging.value = false;
  }
};

const startTowerClimb = async () => {
  if (!tokenStore.selectedToken) {
    message.warning("请先选择 Token");
    return;
  }
  if (!isWeirdTowerActivityOpen.value) {
    message.warning("怪异塔活动未开始或已结束");
    return;
  }
  if (!canClimb.value) {
    message.warning("体力不足或正在执行其他操作");
    return;
  }

  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }

  isClimbing.value = true;
  stopFlag = false;
  let climbCount = 0;

  climbTimeout.value = setTimeout(() => {
    isClimbing.value = false;
    climbTimeout.value = null;
    stopFlag = true;
    message.info("批量爬塔超时，已自动停止");
  }, 60000);

  try {
    const tokenId = tokenStore.selectedToken.id;
    const maxClimb = 100;

    for (let i = 0; i < maxClimb; i++) {
      if (stopFlag) break;

      await getTowerInfo();
      if (towerEnergy.value <= 0) break;

      await tokenStore.sendMessageWithPromise(tokenId, "evotower_readyfight", {}, 5000);
      const fightResult = await tokenStore.sendMessageWithPromise(
        tokenId,
        "evotower_fight",
        {
          battleNum: 1,
          winNum: 1,
        },
        10000,
      );

      climbCount++;
      await getTowerInfo();

      const towerData = evoTowerInfo.value?.evoTower;
      if (towerData?.taskClaimMap) {
        const now = new Date();
        const dateKey = `${now
          .getFullYear()
          .toString()
          .slice(2)}${(now.getMonth() + 1).toString().padStart(2, "0")}${now
          .getDate()
          .toString()
          .padStart(2, "0")}`;

        const dailyTasks = towerData.taskClaimMap[dateKey] || {};
        for (const taskId of [1, 2, 3]) {
          if (!dailyTasks[taskId]) {
            await tokenStore
              .sendMessageWithPromise(tokenId, "evotower_claimtask", { taskId }, 2000)
              .catch(() => {});
            await new Promise((r) => setTimeout(r, 200));
          }
        }
      }

      const towerId = currentTowerId.value;
      const floor = (towerId % 10) + 1;
      if (
        fightResult?.winList &&
        fightResult.winList[0] === true &&
        floor === 1
      ) {
        await tokenStore.sendMessageWithPromise(tokenId, "evotower_claimreward", {}, 5000);
      }

      await new Promise((res) => setTimeout(res, 400));
    }

    const freeEnergyResult = await tokenStore.sendMessageWithPromise(
      tokenStore.selectedToken.id,
      "mergebox_getinfo",
      { actType: 1 },
      5000,
    );

    if (freeEnergyResult?.mergeBox?.freeEnergy > 0) {
      await tokenStore.sendMessageWithPromise(
        tokenStore.selectedToken.id,
        "mergebox_claimfreeenergy",
        { actType: 1 },
        5000,
      );
    }

    message.success(`已自动爬塔 ${climbCount} 次`);
  } catch (error) {
    message.error("批量爬塔失败: " + (error?.message || "未知错误"));
  }

  if (climbTimeout.value) {
    clearTimeout(climbTimeout.value);
    climbTimeout.value = null;
  }
  isClimbing.value = false;
};

const getTowerInfo = async () => {
  if (!tokenStore.selectedToken) return;

  try {
    const tokenId = tokenStore.selectedToken.id;
    const status = tokenStore.getWebSocketStatus(tokenId);
    if (status !== "connected") return;

    await tokenStore.sendMessageWithPromise(tokenId, "evotower_getinfo", {}, 5000);
    tokenStore.sendMessage(tokenId, "role_getroleinfo");
  } catch (error) {
    // ignore refresh failures
  }
};

const wsStatus = computed(() => {
  if (!tokenStore.selectedToken) return "disconnected";
  return tokenStore.getWebSocketStatus(tokenStore.selectedToken.id);
});

watch(wsStatus, (newStatus, oldStatus) => {
  if (newStatus === "connected" && oldStatus !== "connected") {
    setTimeout(() => {
      getTowerInfo();
    }, 1000);
  }
});

watch(
  () => tokenStore.selectedToken,
  (newToken, oldToken) => {
    if (newToken && newToken.id !== oldToken?.id) {
      const status = tokenStore.getWebSocketStatus(newToken.id);
      if (status === "connected") getTowerInfo();
    }
  },
);

onMounted(() => {
  if (tokenStore.selectedToken && wsStatus.value === "connected") {
    getTowerInfo();
  }
});
</script>

<style scoped lang="scss">
.weird-tower {
  display: flex;
  flex-direction: column;
  min-height: 260px;
  padding: 18px;
  gap: 12px;
}

.status-icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
  flex-shrink: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.status-info {
  min-width: 0;
  flex: 1;
}

.status-info h3 {
  margin: 0;
}

.status-info p {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.energy-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  margin-left: auto;
}

.energy-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.energy-count {
  font-size: 15px;
  font-weight: 700;
  color: rgba(248, 250, 252, 0.92);
}

.status-pill {
  align-self: flex-start;
  border-radius: 999px;
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.12);
  color: #fca5a5;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.running {
  border-color: rgba(74, 222, 128, 0.4);
  background: rgba(74, 222, 128, 0.14);
  color: #86efac;
}

.card-content {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 13, 32, 0.32);
  padding: 14px;
}

.tower-floor {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.tower-floor .label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.74);
}

.tower-floor .floor-number {
  font-size: 36px;
  font-weight: 800;
  color: #f8fbff;
  text-shadow: 0 0 14px rgba(124, 108, 255, 0.42);
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", monospace;
  letter-spacing: 0.02em;
}

.floor-sub {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.72);
  font-size: 13px;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
  padding-top: 2px;
}

.weird-secondary-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.climb-button {
  width: 100%;
  height: 48px;
  font-size: 18px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.climb-button.active {
  background: linear-gradient(135deg, #6b8dff 0%, #7c6cff 100%);
  color: #f8fbff;
}

.climb-button.active:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 10px 18px rgba(89, 102, 242, 0.28);
}

.climb-button.disabled {
  background: rgba(148, 163, 184, 0.26);
  border-color: rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.65);
  cursor: not-allowed;
}

.sub-action-button {
  width: 100%;
  height: 44px;
  font-size: 17px;
  font-weight: 700;
  border: 1px solid rgba(161, 140, 255, 0.5);
  border-radius: 12px;
  cursor: pointer;
  color: #eef2ff;
  background: linear-gradient(135deg, rgba(124, 108, 255, 0.92), rgba(99, 102, 241, 0.9));
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease;
}

.sub-action-button:hover {
  transform: translateY(-1px);
  filter: brightness(1.04);
  box-shadow: 0 10px 18px rgba(124, 108, 255, 0.28);
}

.stop-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(248, 250, 252, 0.9);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.stop-button:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(143, 227, 255, 0.55);
  color: #ffffff;
}

@media (max-width: 768px) {
  .weird-tower {
    padding: 14px;
  }

  .card-header {
    gap: 10px;
    margin-bottom: 6px;
  }

  .card-content {
    padding: 12px;
  }

  .tower-floor .floor-number {
    font-size: 32px;
  }

  .climb-button {
    height: 46px;
    font-size: 17px;
  }

  .weird-secondary-actions {
    gap: 8px;
  }

  .sub-action-button,
  .stop-button {
    height: 42px;
    font-size: 16px;
  }
}
</style>
