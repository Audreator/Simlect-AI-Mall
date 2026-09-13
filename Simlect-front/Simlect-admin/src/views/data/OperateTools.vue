<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="operate-tools-page">
    <!-- [zh] 开始标签 `<el-card>` -->
    <el-card class="tool-card">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #header>
        <!-- [zh] 开始标签 `<span>` -->
        <span>数据同步</span>
      </template>
      <div class="tool-grid">
        <el-button type="primary" @click="runTool('toolStatistics')">同步统计数据</el-button>
        <el-button type="primary" @click="runTool('toolProductData')">同步商品向量索引</el-button>
        <el-button type="primary" @click="runTool('toolRagData')">同步 RAG 知识库</el-button>
      </div>
      <p class="tool-hint">将触发后台统计任务或 MQ 同步，数据量大时请耐心等待。</p>
    </el-card>

    <el-card class="tool-card">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #header>
        <!-- [zh] 开始标签 `<span>` -->
        <span>订单与秒杀</span>
      </template>
      <div class="tool-grid">
        <el-button type="warning" @click="runTool('toolAddAllOrderToDelayQueue')">
          待付款订单加入延时队列
        </el-button>
        <el-button type="danger" plain @click="runApi('warmupRushStock', {}, '预热全部秒杀券 Redis 库存？')">
          秒杀券 Redis 全量预热
        </el-button>
        <el-button type="danger" plain @click="runApi('reconcileRushStock', {}, '对全部秒杀券执行 Redis/DB 对账？')">
          秒杀券 Redis 全量对账
        </el-button>
      </div>
      <p class="tool-hint">延时队列用于支付超时关单；秒杀操作请先在「营销 → 优惠券管理」配置秒杀券。</p>
    </el-card>
  </div>
</template>

<script setup>
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const runTool = (apiKey) => {
  proxy.Confirm({
    message: '确定执行该操作吗？',
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api[apiKey],
        showLoading: true,
      })
      if (!result) return
      proxy.Message.success('操作成功')
    },
  })
}

const runApi = (apiKey, params, confirmMsg) => {
  proxy.Confirm({
    message: confirmMsg,
    okfun: async () => {
      const result = await proxy.Request({
        url: proxy.Api[apiKey],
        params,
        showLoading: true,
      })
      if (!result) return
      const d = result.data
      if (d && typeof d.synced === 'number') {
        proxy.Message.success(`同步 ${d.synced} 条，跳过 ${d.skipped ?? 0} 条（DB 共 ${d.totalInDb ?? 0} 条）`)
      } else {
        proxy.Message.success('操作成功')
      }
    },
  })
}
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `.operate-tools-page {` */
.operate-tools-page {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 16px;` */
  gap: 16px;
  /* [zh] 样式规则 `max-width: 960px;` */
  max-width: 960px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tool-card {` */
.tool-card {
  /* [zh] 样式规则 `border-radius: 12px;` */
  border-radius: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tool-grid {` */
.tool-grid {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-wrap: wrap;` */
  flex-wrap: wrap;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tool-hint {` */
.tool-hint {
  /* [zh] 样式规则 `margin: 12px 0 0;` */
  margin: 12px 0 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: #888;` */
  color: #888;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}
</style>
