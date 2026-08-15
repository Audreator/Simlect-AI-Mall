<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<footer>` -->
  <footer class="app-footer ignore">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="footer-inner">
      <!-- [zh] 开始标签 `<p>` -->
      <p class="copyright-line">
        <!-- [zh] 开始标签 `<a>` -->
        <a class="beian-link" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">闽ICP备2026020850号</a>
      <!-- [zh] 闭合标签 `</p>` -->
      </p>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="copyright-text">© 2026 简选 Simlect 版权所有</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</footer>` -->
  </footer>
</template>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.app-footer.ignore {` */
.app-footer.ignore {
  /* [zh] 样式规则 `background: $color-footer-bg;` */
  background: $color-footer-bg;
  /* [zh] 样式规则 `border-top: 1px solid $color-border-gray` */
  border-top: 1px solid $color-border-gray;
  /* [zh] 样式规则 `margin-top: 24px;` */
  margin-top: 24px;

  /* [zh] 样式规则 `.footer-inner {` */
  .footer-inner {
    /* [zh] 样式规则 `max-width: $content-width;` */
    max-width: $content-width;
    /* [zh] 样式规则 `margin: 0 auto;` */
    margin: 0 auto;
    /* [zh] 样式规则 `min-height: 48px;` */
    min-height: 48px;
    /* [zh] 样式规则 `padding: 8px 16px;` */
    padding: 8px 16px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `justify-content: center;` */
    justify-content: center;
    /* [zh] 样式规则 `gap: 12px;` */
    gap: 12px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `@media (max-width: $breakpoint-mobile) {` */
  @media (max-width: $breakpoint-mobile) {
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `border-top: none;` */
    border-top: none;
    /* [zh] 样式规则 `margin-top: 0;` */
    margin-top: 0;
    /* [zh] 样式规则 `padding-bottom: calc($mobile-tab-reserve` */
    padding-bottom: calc($mobile-tab-reserved + 56px);

    /* [zh] 样式规则 `.footer-inner {` */
    .footer-inner {
      /* [zh] 样式规则 `height: auto;` */
      height: auto;
      /* [zh] 样式规则 `padding: 12px 16px;` */
      padding: 12px 16px;
      /* [zh] 样式规则 `flex-direction: column;` */
      flex-direction: column;
      /* [zh] 样式规则 `gap: 4px;` */
      gap: 4px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .copyright-line,
  /* [zh] 样式规则 `.copyright-text {` */
  .copyright-text {
    /* [zh] 样式规则 `margin: 0;` */
    margin: 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `text-align: center;` */
    text-align: center;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.copyright-line {` */
  .copyright-line {
    /* [zh] 样式规则 `white-space: nowrap;` */
    white-space: nowrap;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.beian-link {` */
  .beian-link {
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `line-height: 1.4;` */
    line-height: 1.4;
    /* [zh] 样式规则 `color: $color-text-muted;` */
    color: $color-text-muted;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
    /* [zh] 样式规则 `transition: color $transition-fast;` */
    transition: color $transition-fast;

    /* [zh] 样式规则 `&:hover {` */
    &:hover {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `text-decoration: underline;` */
      text-decoration: underline;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
