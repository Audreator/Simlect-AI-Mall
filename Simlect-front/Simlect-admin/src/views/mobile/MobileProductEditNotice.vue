<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-notice">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card notice-card">
      <!-- [zh] 开始标签 `<span>` -->
      <span class="iconfont icon-edit notice-icon"></span>
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="notice-title">商品编辑建议在电脑版</h3>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="notice-text">
        <!-- [zh] 模板内容：`商品的发布与编辑涉及多规格 SKU、富文本详情与多图上传，移动端操作体验有限。` -->
        商品的发布与编辑涉及多规格 SKU、富文本详情与多图上传，移动端操作体验有限。
        <!-- [zh] 模板内容：`点击下方按钮可一键切换到电脑版进行编辑。` -->
        点击下方按钮可一键切换到电脑版进行编辑。
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="notice-btn" @click="toDesktop">切换到电脑版编辑</button>
      <!-- [zh] 开始标签 `<button>` -->
      <button type="button" class="notice-back" @click="back">返回商品列表</button>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { resolveDesktopPath, switchToDesktopView } from '@/utils/device'

const { proxy } = getCurrentInstance()
const router = useRouter()

const toDesktop = () => {
  const desktopPath = resolveDesktopPath(router.currentRoute.value.path)
  proxy.Confirm({
    message: '切换到电脑版将使用桌面布局进行商品编辑，确定继续吗？',
    okfun: () => switchToDesktopView(desktopPath),
  })
}

const back = () => router.replace('/m/product')
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-notice {` */
.m-notice {
  /* [zh] 样式规则 `padding-top: 24px;` */
  padding-top: 24px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-card {` */
.notice-card {
  /* [zh] 样式规则 `padding: 28px 20px;` */
  padding: 28px 20px;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-icon {` */
.notice-icon {
  /* [zh] 样式规则 `font-size: 40px;` */
  font-size: 40px;
  /* [zh] 样式规则 `color: var(--m-gold);` */
  color: var(--m-gold);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-title {` */
.notice-title {
  /* [zh] 样式规则 `margin: 14px 0 8px;` */
  margin: 14px 0 8px;
  /* [zh] 样式规则 `font-size: 17px;` */
  font-size: 17px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `color: var(--m-ink);` */
  color: var(--m-ink);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-text {` */
.notice-text {
  /* [zh] 样式规则 `margin: 0 0 20px;` */
  margin: 0 0 20px;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `line-height: 1.6;` */
  line-height: 1.6;
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-btn {` */
.notice-btn {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `border: none;` */
  border: none;
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `background: var(--m-ink);` */
  background: var(--m-ink);
  /* [zh] 样式规则 `color: #fff;` */
  color: #fff;
  /* [zh] 样式规则 `font-size: 15px;` */
  font-size: 15px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.notice-back {` */
.notice-back {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
  /* [zh] 样式规则 `height: 42px;` */
  height: 42px;
  /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
  border: 1px solid rgba(120, 120, 128, 0.24);
  /* [zh] 样式规则 `border-radius: 14px;` */
  border-radius: 14px;
  /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
  background: rgba(255, 255, 255, 0.5);
  /* [zh] 样式规则 `color: var(--m-ink-2);` */
  color: var(--m-ink-2);
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}
</style>
