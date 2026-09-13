<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-tool-group">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-group-title">数据同步</h3>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn" @click="runTool('toolStatistics', '同步统计数据')">同步统计数据</button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn" @click="runTool('toolProductData', '同步商品向量索引')">同步商品向量索引</button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn" @click="runTool('toolRagData', '同步 RAG 知识库')">同步 RAG 知识库</button>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<section>` -->
    <section class="glass-card m-tool-group">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="m-group-title">订单与秒杀</h3>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn warn" @click="runTool('toolAddAllOrderToDelayQueue', '待付款订单加入延时队列')">
        <!-- [zh] 模板内容：`待付款订单加入延时队列` -->
        待付款订单加入延时队列
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn danger" @click="runTool('warmupRushStock', '预热全部秒杀券 Redis 库存')">
        <!-- [zh] 模板内容：`秒杀券 Redis 全量预热` -->
        秒杀券 Redis 全量预热
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="tool-btn danger" @click="runTool('reconcileRushStock', '对全部秒杀券执行 Redis/DB 对账')">
        <!-- [zh] 模板内容：`秒杀券 Redis 全量对账` -->
        秒杀券 Redis 全量对账
      <!-- [zh] 闭合标签 `</button>` -->
      </button>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="tool-hint">延时队列用于支付超时关单；秒杀操作请先在优惠券中配置秒杀券。</p>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const runTool = (apiKey, label, params = {}) => {
  proxy.Confirm({
    message: `确定执行「${label}」吗？`,
    okfun: async () => {
      const result = await proxy.Request({ url: proxy.Api[apiKey], params, showLoading: true })
      if (!result) return
      const d = result.data
      if (d && typeof d.synced === 'number') {
        proxy.Message.success(`同步 ${d.synced} 条，跳过 ${d.skipped ?? 0} 条`)
      } else {
        proxy.Message.success('操作成功')
      }
    },
  })
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-simple {` */
.m-simple {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-tool-group {` */
.m-tool-group {
  /* [zh] 样式规则 `padding: 14px;` */
  padding: 14px;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-group-title {` */
.m-group-title {
  /* [zh] 样式规则 `margin: 0 0 4px;` */
  margin: 0 0 4px;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tool-btn {` */
.tool-btn {
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.24);
  /* [zh] 样式规则 `border-radius: 13px;` */
  border-radius: 13px;
  /* [zh] 样式规则 `background: var(--m-ink);` */
  background: var(--m-ink);
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
  /* [zh] 样式规则 `transition: transform 0.15s, opacity 0.2` */
  transition: transform 0.15s, opacity 0.2s;

  /* [zh] 样式规则 `&:active {` */
  &:active {
    /* [zh] 样式规则 `transform: scale(0.98);` */
    transform: scale(0.98);
    /* [zh] 样式规则 `opacity: 0.9;` */
    opacity: 0.9;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.warn {` */
  &.warn {
    /* [zh] 样式规则 `background: rgba(255, 149, 0, 0.14);` */
    background: rgba(255, 149, 0, 0.14);
    /* [zh] 样式规则 `border-color: rgba(255, 149, 0, 0.4);` */
    border-color: rgba(255, 149, 0, 0.4);
    /* [zh] 样式规则 `color: #b56a00;` */
    color: #b56a00;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.danger {` */
  &.danger {
    /* [zh] 样式规则 `background: rgba(255, 59, 48, 0.1);` */
    background: rgba(255, 59, 48, 0.1);
    /* [zh] 样式规则 `border-color: rgba(255, 59, 48, 0.35);` */
    border-color: rgba(255, 59, 48, 0.35);
    /* [zh] 样式规则 `color: var(--m-danger);` */
    color: var(--m-danger);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.tool-hint {` */
.tool-hint {
  /* [zh] 样式规则 `margin: 4px 0 0;` */
  margin: 4px 0 0;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;
/* [zh] 样式规则 `}` */
}
</style>
